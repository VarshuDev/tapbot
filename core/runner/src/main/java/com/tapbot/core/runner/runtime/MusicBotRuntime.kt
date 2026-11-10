package com.tapbot.core.runner.runtime

import com.tapbot.core.model.InlineKeyboardButton
import com.tapbot.core.model.InlineKeyboardMarkup
import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramCallbackQuery
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

enum class LoopMode {
    OFF,
    ONE,
    ALL
}

/**
 * Flagship on-device Music Streaming Bot runtime (TapStream Audio 2.0).
 *
 * Features:
 * • Audio search and resolution using yt-dlp API proxy, direct links, and curated radio streams
 * • Physical Android speaker/Bluetooth audio streaming via DeviceAudioPlayer (MediaPlayer + AudioFocus + live ticker)
 * • Telegram native in-chat audio player delivery via sendAudio + interactive inline player buttons
 * • Full queue & playback controls: /play, /pause, /resume, /skip, /stop, /queue, /nowplaying, /volume, /send, /radio, /mode, /shuffle, /loop, /seek, /remove
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
    private var loopMode = LoopMode.OFF

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
        ServiceLocator.setActiveAudioPlayer(audioPlayer)
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

                            val callback = update.callbackQuery
                            if (callback != null) {
                                messageCounter++
                                handleCallbackQuery(ctx, token, callback)
                                continue
                            }

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
                        backoff.recordFailure()
                        ctx.log(LogLevel.WARN, TAG, "TapStream getUpdates API failure: ${error?.message}")
                        backoff.delayWithBackoff(ctx.networkState)
                    }
                } catch (c: CancellationException) {
                    break
                } catch (e: Exception) {
                    backoff.recordFailure()
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

        var attachControls = false

        val reply = when (command) {
            "/start", "/help" -> formatHelpMessage()

            "/play" -> {
                val res = handlePlayCommand(ctx, token, chatId, query, forceChatOnly = false)
                attachControls = audioPlayer.playbackState.value.currentTrack != null
                res
            }

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
                    attachControls = true
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

            "/nowplaying", "/np", "/status" -> {
                val track = audioPlayer.playbackState.value.currentTrack
                val st = audioPlayer.playbackState.value.status
                attachControls = track != null && st != PlaybackStatus.IDLE && st != PlaybackStatus.STOPPED
                formatNowPlayingMessage()
            }

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

            "/radio" -> {
                val res = handleRadioCommand(query)
                attachControls = query.isNotBlank() && audioPlayer.playbackState.value.currentTrack != null
                res
            }

            "/shuffle" -> handleShuffleCommand()

            "/loop", "/repeat" -> handleLoopCommand(query)

            "/seek" -> handleSeekCommand(query)

            "/remove", "/rm" -> handleRemoveCommand(query)

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

        if (attachControls) {
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = chatId,
                text = reply,
                parseMode = "Markdown",
                replyToMessageId = null,
                replyMarkup = buildPlayerKeyboard()
            )
        } else {
            ctx.telegramApi.sendMessage(token, chatId, reply, "Markdown")
        }
        return reply
    }

    private suspend fun handleCallbackQuery(
        ctx: RuntimeContext,
        token: String,
        callback: TelegramCallbackQuery
    ) {
        val data = callback.data.orEmpty()
        val msg = callback.message
        val chatId = msg?.chat?.id
        val messageId = msg?.messageId

        var toast = "Updated"
        when (data) {
            "music:toggle" -> {
                val cur = audioPlayer.playbackState.value
                toast = when (cur.status) {
                    PlaybackStatus.PLAYING -> {
                        audioPlayer.pause()
                        "⏸ Paused"
                    }
                    PlaybackStatus.PAUSED -> {
                        audioPlayer.resume()
                        "▶️ Resumed"
                    }
                    else -> {
                        val first = queue.getOrNull(currentTrackIndex.coerceAtLeast(0))
                        if (first != null) {
                            currentTrackIndex = currentTrackIndex.coerceAtLeast(0)
                            audioPlayer.playTrack(first)
                            "▶️ Playing"
                        } else {
                            "Queue is empty"
                        }
                    }
                }
            }
            "music:skip" -> {
                handleSkipCommand(ctx, token, chatId ?: 0L)
                toast = "⏭️ Next track"
            }
            "music:prev" -> {
                if (currentTrackIndex > 0) {
                    currentTrackIndex--
                    audioPlayer.playTrack(queue[currentTrackIndex])
                    toast = "⏮️ Previous track"
                } else if (audioPlayer.playbackState.value.currentTrack != null) {
                    audioPlayer.seekTo(0L)
                    toast = "⏮️ Restarted track"
                } else {
                    toast = "Nothing playing"
                }
            }
            "music:voldown" -> {
                val newVol = audioPlayer.setVolume(audioPlayer.playbackState.value.volumePct - 15)
                toast = "🔉 Volume: $newVol%"
            }
            "music:volup" -> {
                val newVol = audioPlayer.setVolume(audioPlayer.playbackState.value.volumePct + 15)
                toast = "🔊 Volume: $newVol%"
            }
            "music:stop" -> {
                audioPlayer.stop()
                queue.clear()
                currentTrackIndex = -1
                toast = "⏹ Stopped"
            }
            "music:loop" -> {
                loopMode = when (loopMode) {
                    LoopMode.OFF -> LoopMode.ONE
                    LoopMode.ONE -> LoopMode.ALL
                    LoopMode.ALL -> LoopMode.OFF
                }
                toast = "🔁 Loop: ${loopMode.name}"
            }
            "music:shuffle" -> {
                shuffleUpcoming()
                toast = "🔀 Queue shuffled"
            }
        }

        ctx.telegramApi.answerCallbackQuery(token, callback.id, toast)

        if (chatId != null && messageId != null) {
            val markup = if (audioPlayer.playbackState.value.status == PlaybackStatus.STOPPED) null else buildPlayerKeyboard()
            ctx.telegramApi.editMessageText(
                token = token,
                chatId = chatId,
                messageId = messageId,
                text = formatNowPlayingMessage(),
                parseMode = "Markdown",
                replyMarkup = markup
            )
        }
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
        if (queue.isEmpty()) {
            audioPlayer.stop()
            return "⏭️ End of queue reached. Use `/play <song>` to queue more."
        }

        if (currentTrackIndex >= queue.size - 1) {
            if (loopMode == LoopMode.ALL) {
                currentTrackIndex = 0
            } else {
                audioPlayer.stop()
                return "⏭️ End of queue reached. Use `/play <song>` to queue more."
            }
        } else {
            currentTrackIndex++
        }

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

    private fun handleRadioCommand(genre: String): String {
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

    private fun handleShuffleCommand(): String {
        if (queue.size <= 1) {
            return "🔀 Queue needs at least 2 tracks to shuffle."
        }
        shuffleUpcoming()
        return "🔀 *Shuffled playback queue!* (${queue.size} tracks ordered)"
    }

    private fun shuffleUpcoming() {
        if (queue.size <= 1) return
        val current = queue.getOrNull(currentTrackIndex)
        if (current != null) {
            val others = queue.filterIndexed { idx, _ -> idx != currentTrackIndex }.shuffled()
            queue.clear()
            queue.add(current)
            queue.addAll(others)
            currentTrackIndex = 0
        } else {
            queue.shuffle()
        }
    }

    private fun handleLoopCommand(arg: String): String {
        val normalized = arg.lowercase(Locale.ROOT)
        loopMode = when (normalized) {
            "off", "none", "disable" -> LoopMode.OFF
            "one", "track", "single" -> LoopMode.ONE
            "all", "queue" -> LoopMode.ALL
            "" -> when (loopMode) {
                LoopMode.OFF -> LoopMode.ONE
                LoopMode.ONE -> LoopMode.ALL
                LoopMode.ALL -> LoopMode.OFF
            }
            else -> return "Usage: `/loop [off|one|all]`\nCurrent Loop Mode: `${loopMode.name}`"
        }
        val desc = when (loopMode) {
            LoopMode.OFF -> "Looping disabled."
            LoopMode.ONE -> "Repeating current track."
            LoopMode.ALL -> "Repeating entire queue."
        }
        return "🔁 *Loop Mode set to:* `${loopMode.name}`\n$desc"
    }

    private fun handleSeekCommand(arg: String): String {
        val cur = audioPlayer.playbackState.value
        val track = cur.currentTrack
            ?: return "⚠️ No track is loaded to seek. Use `/play <song>` first."

        val targetMs = parseSeekMilliseconds(arg)
            ?: return "Usage: `/seek <mm:ss | seconds>` (e.g. `/seek 1:30` or `/seek 45`)"

        audioPlayer.seekTo(targetMs)
        val updated = audioPlayer.playbackState.value
        val bar = updated.formatProgressBar()
        return "⏩ *Seeked ${escapeMarkdown(track.title)}*\n`$bar`"
    }

    private fun handleRemoveCommand(arg: String): String {
        if (queue.isEmpty()) {
            return "🎶 Queue is currently empty."
        }
        val pos = arg.toIntOrNull()
        if (pos == null || pos !in 1..queue.size) {
            return "Usage: `/remove <1..${queue.size}>`\nSend `/queue` to view track numbers."
        }
        val idx = pos - 1
        val removed = queue.removeAt(idx)
        when {
            queue.isEmpty() -> {
                currentTrackIndex = -1
                audioPlayer.stop()
            }
            idx < currentTrackIndex -> {
                currentTrackIndex--
            }
            idx == currentTrackIndex -> {
                currentTrackIndex = currentTrackIndex.coerceAtMost(queue.size - 1)
                audioPlayer.playTrack(queue[currentTrackIndex])
            }
        }
        return "🗑️ *Removed (#$pos):* ${escapeMarkdown(removed.title)}"
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
            
            🔊 *Volume:* `${state.volumePct}%` | 🎚 *Mode:* `${routingMode.name}` | 🔁 *Loop:* `${loopMode.name}`
            ━━━━━━━━━━━━━━━━━━━━━━
            Controls: /pause, /resume, /skip, /seek, /loop, /shuffle, /stop
        """.trimIndent()
    }

    private fun formatQueueMessage(): String {
        if (queue.isEmpty()) {
            return "🎶 *Playback Queue is Empty.*\nQueue tracks using `/play <song>` or `/radio <genre>`."
        }

        val sb = StringBuilder("🎶 *TapStream Playback Queue (${queue.size} tracks | Loop: ${loopMode.name})*\n━━━━━━━━━━━━━━━━━━━━━━\n")
        queue.forEachIndexed { i, track ->
            val isCurrent = (i == currentTrackIndex && audioPlayer.playbackState.value.isPlaying)
            val prefix = if (isCurrent) "▶️ ${i + 1}." else "${i + 1}."
            sb.append("$prefix *${escapeMarkdown(track.title)}* — ${escapeMarkdown(track.artist)} (`${track.durationFormatted}`)\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Manage: /skip, /shuffle, /loop, /remove `<#>`, /stop, /np")
        return sb.toString()
    }

    private fun formatHelpMessage(): String {
        return """
            🎵 *TapStream Music & Audio Bot* 🎧
            
            Stream music directly on your Android phone's speakers/Bluetooth or download tracks in Telegram!
            
            *Commands:*
            • `/play <query|url>` — Resolve & stream audio track or playlist
            • `/radio [genre]` — 24/7 internet radio (lofi, synthwave, jazz, rock)
            • `/pause` & `/resume` — Pause or resume current audio stream
            • `/skip` & `/stop` — Skip track or stop audio & clear queue
            • `/queue` — View active playlist queue
            • `/shuffle` & `/loop [off|one|all]` — Shuffle queue & set repeat mode
            • `/seek <mm:ss|sec>` & `/remove <#>` — Scrub position or remove track
            • `/np` or `/nowplaying` — Live playback time scrubber & controls
            • `/volume <0-100>` — Adjust device physical speaker volume
            • `/send <query|url>` — Send audio track directly into Telegram chat
            • `/mode [speaker|chat|both]` — Route audio to speaker, chat, or both
        """.trimIndent()
    }

    private fun buildPlayerKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("⏮ Prev", callbackData = "music:prev"),
                    InlineKeyboardButton("⏯ Play/Pause", callbackData = "music:toggle"),
                    InlineKeyboardButton("⏭ Skip", callbackData = "music:skip")
                ),
                listOf(
                    InlineKeyboardButton("🔉 Vol-", callbackData = "music:voldown"),
                    InlineKeyboardButton("🔊 Vol+", callbackData = "music:volup"),
                    InlineKeyboardButton("⏹ Stop", callbackData = "music:stop")
                ),
                listOf(
                    InlineKeyboardButton("🔁 Loop: ${loopMode.name}", callbackData = "music:loop"),
                    InlineKeyboardButton("🔀 Shuffle", callbackData = "music:shuffle")
                )
            )
        )
    }

    internal fun handleTrackFinished() {
        when (loopMode) {
            LoopMode.ONE -> {
                val track = queue.getOrNull(currentTrackIndex) ?: return
                audioPlayer.playTrack(track)
            }
            LoopMode.ALL -> {
                if (queue.isEmpty()) return
                currentTrackIndex = (currentTrackIndex + 1) % queue.size
                audioPlayer.playTrack(queue[currentTrackIndex])
            }
            LoopMode.OFF -> {
                if (currentTrackIndex < queue.size - 1) {
                    currentTrackIndex++
                    val next = queue[currentTrackIndex]
                    audioPlayer.playTrack(next)
                }
            }
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
        audioPlayer.release()
        if (ServiceLocator.activeAudioPlayer.value === audioPlayer) {
            ServiceLocator.setActiveAudioPlayer(null)
        }
        queue.clear()
        currentTrackIndex = -1
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
    }

    companion object {
        private const val TAG = "MusicBotRuntime"

        fun parseSeekMilliseconds(raw: String): Long? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":")
                if (parts.size == 2) {
                    val mins = parts[0].toLongOrNull() ?: return null
                    val secs = parts[1].toLongOrNull() ?: return null
                    if (mins < 0L || secs < 0L || secs >= 60L) return null
                    return (mins * 60L + secs) * 1000L
                }
                return null
            }
            val sec = trimmed.removeSuffix("s").removeSuffix("S").toLongOrNull() ?: return null
            if (sec < 0L) return null
            return sec * 1000L
        }
    }
}
