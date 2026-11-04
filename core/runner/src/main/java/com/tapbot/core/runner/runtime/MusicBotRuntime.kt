package com.tapbot.core.runner.runtime

import com.tapbot.core.model.LogLevel
import com.tapbot.core.runner.ServiceLocator
import com.tapbot.core.runner.audio.AudioSourceType
import com.tapbot.core.runner.audio.AudioStreamResolver
import com.tapbot.core.runner.audio.AudioTrack
import com.tapbot.core.runner.audio.DeviceAudioPlayer
import com.tapbot.core.runner.audio.PlaybackStatus
import com.tapbot.core.runner.util.AdaptiveBackoff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class PlaybackRoutingMode {
    SPEAKER,  // Plays through phone speaker / Bluetooth audio
    CHAT,     // Sends playable audio track directly into Telegram chat
    BOTH      // Plays out loud on speaker AND sends to chat
}

/**
 * Flagship on-device Music Streaming Bot runtime (TapStream).
 *
 * Features:
 * • Audio search and resolution using yt-dlp API proxy, direct links, and curated radio streams
 * • Physical Android speaker/Bluetooth audio streaming via DeviceAudioPlayer (MediaPlayer)
 * • Telegram native in-chat audio player delivery via sendAudio
 * • Interactive playback controls: /play, /pause, /resume, /skip, /stop, /queue, /nowplaying, /volume, /send, /radio, /mode
 */
class MusicBotRuntime(
    override val botId: String = "bot_music",
    resolver: AudioStreamResolver? = null,
    player: DeviceAudioPlayer? = null
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null

    // Audio services
    private val streamResolver = resolver ?: AudioStreamResolver()
    private val audioPlayer = player ?: DeviceAudioPlayer(
        context = ServiceLocator.appContext,
        onTrackCompleted = { handleTrackFinished() }
    )

    // Queue & Playback state
    private val queue = mutableListOf<AudioTrack>()
    private var currentTrackIndex: Int = -1
    private var routingMode = PlaybackRoutingMode.BOTH

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
        val initialVol = context.credentials["default_volume"]?.toIntOrNull() ?: 80
        audioPlayer.setVolume(initialVol)

        val modeSetting = context.credentials["default_playback_mode"]?.lowercase()
        routingMode = when (modeSetting) {
            "speaker", "device" -> PlaybackRoutingMode.SPEAKER
            "chat", "telegram" -> PlaybackRoutingMode.CHAT
            else -> PlaybackRoutingMode.BOTH
        }
    }

    override suspend fun start() {
        val ctx = context ?: throw IllegalStateException("Runtime not initialized")
        val token = ctx.token

        if (token.isBlank()) {
            val err = "Cannot start Music Bot: Empty Telegram Bot Token."
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        _state.value = BotRuntimeState.Starting
        ctx.log(LogLevel.INFO, TAG, "Starting TapStream Music Bot runtime on Android...")

        val meResult = ctx.telegramApi.getMe(token)
        if (meResult.isFailure) {
            val err = "Telegram authentication failed: ${meResult.exceptionOrNull()?.message}"
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        val botUser = meResult.getOrThrow()
        val username = botUser.username ?: "TapStreamBot"
        ctx.log(LogLevel.INFO, TAG, "TapStream authenticated as @$username")

        _state.value = BotRuntimeState.Connected(username, botUser.firstName)

        val startedAt = System.currentTimeMillis()
        var pollCounter = 0L
        var messageCounter = 0L

        pollJob = ctx.scope.launch {
            _state.value = BotRuntimeState.Running(
                botUsername = username,
                startedAt = startedAt,
                pollCount = 0L,
                messageCount = 0L
            )
            ctx.log(LogLevel.INFO, TAG, "TapStream polling active.")
            val backoff = AdaptiveBackoff()

            while (isActive) {
                try {
                    if (!ctx.isNetworkAvailable()) {
                        backoff.delayWithBackoff(ctx.networkState)
                        continue
                    }

                    val offset = lastUpdateId?.plus(1)
                    val updatesResult = ctx.telegramApi.getUpdates(
                        token = token,
                        offset = offset,
                        timeoutSeconds = 25
                    )

                    pollCounter++

                    if (updatesResult.isSuccess) {
                        backoff.recordSuccess()
                        val updates = updatesResult.getOrThrow()
                        for (update in updates) {
                            lastUpdateId = update.updateId
                            val message = update.message ?: continue
                            val text = message.text?.trim().orEmpty()
                            val chatId = message.chat.id
                            messageCounter++

                            handleCommand(ctx, token, chatId, text)
                        }

                        _state.value = BotRuntimeState.Running(
                            botUsername = username,
                            startedAt = startedAt,
                            pollCount = pollCounter,
                            messageCount = messageCounter,
                            lastActivityAt = System.currentTimeMillis()
                        )
                    } else {
                        val error = updatesResult.exceptionOrNull()
                        val delayMs = backoff.recordFailure()
                        ctx.log(LogLevel.WARN, TAG, "TapStream getUpdates API failure: ${error?.message}")
                        backoff.delayWithBackoff(ctx.networkState)
                    }
                } catch (c: CancellationException) {
                    break
                } catch (e: Exception) {
                    val delayMs = backoff.recordFailure()
                    ctx.log(LogLevel.WARN, TAG, "TapStream polling transient error: ${e.message}")
                    backoff.delayWithBackoff(ctx.networkState)
                }
            }
        }
    }

    suspend fun handleCommand(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        text: String
    ): String {
        val parts = text.split("\\s+".toRegex())
        val command = parts.firstOrNull()?.lowercase(Locale.ROOT)
            ?.removeSuffix("@${(state.value as? BotRuntimeState.Running)?.botUsername ?: ""}")
            .orEmpty()
        val query = parts.drop(1).joinToString(" ").trim()

        val reply = when (command) {
            "/start", "/help" -> formatHelpMessage()

            "/play" -> handlePlayCommand(ctx, token, chatId, query, forceChatOnly = false)

            "/send", "/download" -> handlePlayCommand(ctx, token, chatId, query, forceChatOnly = true)

            "/pause" -> {
                if (audioPlayer.pause()) {
                    "⏸ *Playback paused.* Send `/resume` to continue."
                } else {
                    "⚠️ Nothing is currently playing on the device."
                }
            }

            "/resume" -> {
                if (audioPlayer.resume()) {
                    val track = audioPlayer.playbackState.value.currentTrack
                    "▶️ *Resumed:* ${escapeMarkdown(track?.title ?: "Track")}"
                } else {
                    "⚠️ No paused playback to resume. Use `/play <song>`."
                }
            }

            "/stop" -> {
                audioPlayer.stop()
                queue.clear()
                currentTrackIndex = -1
                "⏹ *Stopped playback and cleared queue.*"
            }

            "/skip" -> handleSkipCommand(ctx, token, chatId)

            "/queue", "/playlist" -> formatQueueMessage()

            "/nowplaying", "/np", "/status" -> formatNowPlayingMessage()

            "/volume", "/vol" -> {
                val volArg = query.toIntOrNull()
                if (volArg != null) {
                    val newVol = audioPlayer.setVolume(volArg)
                    "🔊 Device speaker volume set to *$newVol%*"
                } else {
                    val curVol = audioPlayer.playbackState.value.volumePct
                    "🔊 Current device volume: *$curVol%*\nUse `/volume <0-100>` to change."
                }
            }

            "/radio" -> handleRadioCommand(ctx, token, chatId, query)

            "/mode" -> {
                val modeArg = query.lowercase(Locale.ROOT)
                when (modeArg) {
                    "speaker", "phone" -> {
                        routingMode = PlaybackRoutingMode.SPEAKER
                        "🔊 Playback Mode: *DEVICE SPEAKER ONLY*\nAudio plays out loud on your Android phone."
                    }
                    "chat", "telegram" -> {
                        routingMode = PlaybackRoutingMode.CHAT
                        "💬 Playback Mode: *TELEGRAM CHAT ONLY*\nAudio files/streams sent directly to Telegram chat."
                    }
                    "both", "all" -> {
                        routingMode = PlaybackRoutingMode.BOTH
                        "✨ Playback Mode: *BOTH (SPEAKER + CHAT)*\nPlays out loud on phone AND delivers audio to chat."
                    }
                    else -> {
                        "🎛 *Active Playback Mode:* `${routingMode.name}`\n" +
                                "Options:\n" +
                                "• `/mode speaker` — Stream on host phone speaker/Bluetooth\n" +
                                "• `/mode chat` — Send audio directly to Telegram chat\n" +
                                "• `/mode both` — Both speaker and in-chat audio"
                    }
                }
            }

            else -> "🎵 TapStream received: `${escapeMarkdown(text)}`\nSend `/help` for music commands."
        }

        ctx.telegramApi.sendMessage(token, chatId, reply, "Markdown")
        return reply
    }

    private suspend fun handlePlayCommand(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        query: String,
        forceChatOnly: Boolean
    ): String {
        if (query.isBlank()) {
            val cur = audioPlayer.playbackState.value
            return if (cur.status == PlaybackStatus.PAUSED) {
                audioPlayer.resume()
                "▶️ *Resumed:* ${escapeMarkdown(cur.currentTrack?.title ?: "Current Track")}"
            } else {
                "Usage: `/play <song title, artist, YouTube link, or stream URL>`\n" +
                        "Example: `/play never gonna give you up` or `/play lofi`"
            }
        }

        ctx.telegramApi.sendChatAction(token, chatId, "typing")
        val customResolver = ctx.credentials["stream_resolver_url"] ?: ctx.credentials["ytdlp_api_url"]
        val resolveRes = streamResolver.resolveTrack(query, customResolver)

        if (resolveRes.isFailure) {
            val err = resolveRes.exceptionOrNull()?.message ?: "Could not resolve audio stream"
            return "❌ Resolution failed: `${escapeMarkdown(err)}`"
        }

        val track = resolveRes.getOrThrow()
        queue.add(track)

        val shouldSendToChat = forceChatOnly || routingMode == PlaybackRoutingMode.CHAT || routingMode == PlaybackRoutingMode.BOTH
        val shouldPlayOnDevice = !forceChatOnly && (routingMode == PlaybackRoutingMode.SPEAKER || routingMode == PlaybackRoutingMode.BOTH)

        // If nothing is playing on device, start immediately
        if (shouldPlayOnDevice && !audioPlayer.playbackState.value.isPlaying) {
            currentTrackIndex = queue.size - 1
            audioPlayer.playTrack(track)
        }

        // Send audio directly to Telegram chat
        if (shouldSendToChat) {
            ctx.telegramApi.sendChatAction(token, chatId, "upload_document")
            ctx.telegramApi.sendAudio(
                token = token,
                chatId = chatId,
                audioUrl = track.streamUrl,
                title = track.title,
                performer = track.artist,
                duration = if (track.durationSeconds > 0) track.durationSeconds else null,
                caption = "🎧 *${escapeMarkdown(track.title)}* by *${escapeMarkdown(track.artist)}*",
                parseMode = "Markdown"
            )
        }

        val sourceBadge = when (track.source) {
            AudioSourceType.DIRECT_URL -> "🌐 Direct Stream"
            AudioSourceType.YTDLP_API -> "⚡ yt-dlp API"
            AudioSourceType.STREAM_RESOLVER -> "🔎 Web Audio Stream"
            AudioSourceType.RADIO_STATION -> "📻 24/7 Radio Station"
            AudioSourceType.LOCAL_CACHE -> "💾 Local Cache"
        }

        val position = queue.size
        val outputLabel = if (shouldPlayOnDevice && shouldSendToChat) {
            "Phone Speaker + Telegram Audio"
        } else if (shouldPlayOnDevice) {
            "Phone Speaker"
        } else {
            "Telegram Audio"
        }

        return if (position == 1 || currentTrackIndex == position - 1) {
            "▶️ *Now Playing:* ${escapeMarkdown(track.title)}\n" +
                    "👤 Artist: *${escapeMarkdown(track.artist)}*\n" +
                    "⏱ Duration: `${track.durationFormatted}` | $sourceBadge\n" +
                    "🔊 Output: `$outputLabel`"
        } else {
            "📋 *Queued Track (#$position):* ${escapeMarkdown(track.title)}\n" +
                    "👤 Artist: *${escapeMarkdown(track.artist)}*\n" +
                    "⏱ Duration: `${track.durationFormatted}`"
        }
    }

    private suspend fun handleSkipCommand(ctx: RuntimeContext, token: String, chatId: Long): String {
        if (queue.isEmpty() || currentTrackIndex >= queue.size - 1) {
            audioPlayer.stop()
            return "⏭️ End of queue reached. Use `/play <song>` to queue more."
        }

        currentTrackIndex++
        val nextTrack = queue[currentTrackIndex]
        audioPlayer.playTrack(nextTrack)

        if (routingMode == PlaybackRoutingMode.CHAT || routingMode == PlaybackRoutingMode.BOTH) {
            ctx.telegramApi.sendAudio(
                token = token,
                chatId = chatId,
                audioUrl = nextTrack.streamUrl,
                title = nextTrack.title,
                performer = nextTrack.artist,
                duration = if (nextTrack.durationSeconds > 0) nextTrack.durationSeconds else null,
                caption = "⏭️ *Now Playing:* ${escapeMarkdown(nextTrack.title)}",
                parseMode = "Markdown"
            )
        }

        return "⏭️ *Skipped! Now playing:* ${escapeMarkdown(nextTrack.title)} by *${escapeMarkdown(nextTrack.artist)}*"
    }

    private fun handleRadioCommand(ctx: RuntimeContext, token: String, chatId: Long, genre: String): String {
        val stations = streamResolver.getCuratedRadioStations()
        if (genre.isBlank()) {
            val sb = StringBuilder("📻 *24/7 Curated Internet Radio Stations*\n━━━━━━━━━━━━━━━━━━━━━━\n")
            for (st in stations) {
                sb.append("• `/radio ${st.genre}` — *${st.title}* (${st.artist})\n")
            }
            sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
            sb.append("Tune in instantly with `/radio <genre>` (e.g. `/radio lofi`)")
            return sb.toString()
        }

        val match = streamResolver.getRadioStationByGenre(genre)
            ?: return "⚠️ Radio genre not found. Available: `lofi`, `synthwave`, `chillhop`, `rock`, `jazz`, `ambient`"

        queue.add(match)
        currentTrackIndex = queue.size - 1
        audioPlayer.playTrack(match)

        return "📻 *Tuned into:* ${escapeMarkdown(match.title)}\n" +
                "📡 Stream: *${escapeMarkdown(match.artist)}* | Status: `LIVE 🔴`\n" +
                "🔊 Output: Host Phone Speaker (Volume: ${audioPlayer.playbackState.value.volumePct}%)"
    }

    private fun formatNowPlayingMessage(): String {
        val state = audioPlayer.playbackState.value
        val track = state.currentTrack
        if (track == null || state.status == PlaybackStatus.IDLE || state.status == PlaybackStatus.STOPPED) {
            return "🎧 *TapStream Player Status*\n━━━━━━━━━━━━━━━━━━━━━━\nNothing is currently playing.\nUse `/play <song>` or `/radio` to start."
        }

        val progressBar = state.formatProgressBar()
        val statusText = when (state.status) {
            PlaybackStatus.PLAYING -> "🟢 PLAYING"
            PlaybackStatus.PAUSED -> "🟡 PAUSED"
            PlaybackStatus.BUFFERING -> "⏳ BUFFERING"
            PlaybackStatus.STOPPED -> "⚪ STOPPED"
            PlaybackStatus.ERROR -> "🔴 ERROR: ${state.errorMessage}"
            PlaybackStatus.IDLE -> "IDLE"
        }

        return """
            🎧 *Now Playing on TapStream*
            ━━━━━━━━━━━━━━━━━━━━━━
            🎵 *Track:* ${escapeMarkdown(track.title)}
            👤 *Artist:* ${escapeMarkdown(track.artist)}
            🎛 *Status:* $statusText
            
            `$progressBar`
            
            🔊 *Volume:* `${state.volumePct}%` | 🎚 *Mode:* `${routingMode.name}`
            ━━━━━━━━━━━━━━━━━━━━━━
            Controls: /pause, /resume, /skip, /stop, /volume `<0-100>`
        """.trimIndent()
    }

    private fun formatQueueMessage(): String {
        if (queue.isEmpty()) {
            return "🎶 *Playback Queue is Empty.*\nQueue tracks using `/play <song>` or `/radio <genre>`."
        }

        val sb = StringBuilder("🎶 *TapStream Playback Queue (${queue.size} tracks)*\n━━━━━━━━━━━━━━━━━━━━━━\n")
        queue.forEachIndexed { i, track ->
            val isCurrent = (i == currentTrackIndex && audioPlayer.playbackState.value.isPlaying)
            val prefix = if (isCurrent) "▶️" else "${i + 1}."
            sb.append("$prefix *${escapeMarkdown(track.title)}* — ${escapeMarkdown(track.artist)} (`${track.durationFormatted}`)\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Manage: /skip, /stop, /np")
        return sb.toString()
    }

    private fun formatHelpMessage(): String {
        return """
            🎵 *TapStream Music & Audio Bot* 🎧
            
            Stream music directly on your Android phone's speakers/Bluetooth or download tracks in Telegram!
            
            *Commands:*
            • `/play <query|url>` — Resolve & stream audio track or playlist
            • `/radio [genre]` — 24/7 internet radio (lofi, synthwave, jazz, rock)
            • `/pause` — Pause current audio stream
            • `/resume` — Resume playback
            • `/skip` — Play next queued track
            • `/stop` — Stop audio and clear queue
            • `/queue` — View active playlist queue
            • `/np` or `/nowplaying` — Live playback time scrubber
            • `/volume <0-100>` — Adjust device physical speaker volume
            • `/send <query|url>` — Send audio track directly into Telegram chat
            • `/mode [speaker|chat|both]` — Route audio to speaker, chat, or both
        """.trimIndent()
    }

    private fun handleTrackFinished() {
        if (currentTrackIndex < queue.size - 1) {
            currentTrackIndex++
            val next = queue[currentTrackIndex]
            audioPlayer.playTrack(next)
        }
    }

    private fun escapeMarkdown(text: String): String {
        return text.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`")
    }

    override suspend fun stop() {
        audioPlayer.stop()
        queue.clear()
        currentTrackIndex = -1
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
    }

    companion object {
        private const val TAG = "MusicBotRuntime"
    }
}
