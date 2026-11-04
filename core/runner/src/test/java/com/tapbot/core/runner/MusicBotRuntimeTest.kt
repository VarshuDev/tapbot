package com.tapbot.core.runner

import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.audio.AudioSourceType
import com.tapbot.core.runner.audio.AudioStreamResolver
import com.tapbot.core.runner.audio.AudioTrack
import com.tapbot.core.runner.audio.DeviceAudioPlayer
import com.tapbot.core.runner.runtime.BotRuntimeState
import com.tapbot.core.runner.runtime.MusicBotRuntime
import com.tapbot.core.runner.runtime.RuntimeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class MusicBotRuntimeTest {

    private class MockTelegramApiClient : TelegramApiClient() {
        val sentMessages = mutableListOf<Triple<Long, String, String?>>()
        val sentAudios = mutableListOf<Triple<Long, String, String?>>() // chatId, audioUrl, title
        val updatesQueue = mutableListOf<List<TelegramUpdate>>()

        override suspend fun getMe(token: String): Result<TelegramUser> {
            return Result.success(TelegramUser(id = 888, isBot = true, firstName = "TapStream", username = "TapStreamBot"))
        }

        override suspend fun getUpdates(token: String, offset: Long?, timeoutSeconds: Int): Result<List<TelegramUpdate>> {
            return if (updatesQueue.isNotEmpty()) {
                Result.success(updatesQueue.removeAt(0))
            } else {
                delay(30)
                Result.success(emptyList())
            }
        }

        override suspend fun sendMessage(token: String, chatId: Long, text: String, parseMode: String?): Result<Boolean> {
            sentMessages.add(Triple(chatId, text, parseMode))
            return Result.success(true)
        }

        override suspend fun sendAudio(
            token: String,
            chatId: Long,
            audioUrl: String,
            title: String?,
            performer: String?,
            duration: Int?,
            caption: String?,
            parseMode: String?,
            replyToMessageId: Long?
        ): Result<Boolean> {
            sentAudios.add(Triple(chatId, audioUrl, title))
            return Result.success(true)
        }

        override suspend fun sendChatAction(token: String, chatId: Long, action: String): Result<Boolean> {
            return Result.success(true)
        }
    }

    private lateinit var fakeApi: MockTelegramApiClient
    private lateinit var audioPlayer: DeviceAudioPlayer
    private lateinit var streamResolver: AudioStreamResolver
    private lateinit var musicRuntime: MusicBotRuntime
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private fun createCtx(credentials: Map<String, String> = emptyMap()) = RuntimeContext(
        botId = "bot_music",
        token = "VALID_MUSIC_TOKEN",
        telegramApi = fakeApi,
        log = { _, _, _ -> },
        scope = scope,
        credentials = credentials
    )

    @Before
    fun setUp() {
        fakeApi = MockTelegramApiClient()
        audioPlayer = DeviceAudioPlayer(context = null)
        streamResolver = AudioStreamResolver()
        musicRuntime = MusicBotRuntime(
            botId = "bot_music",
            resolver = streamResolver,
            player = audioPlayer
        )
    }

    @Test
    fun `initializes with custom volume and playback mode credentials`() = runBlocking {
        val ctx = createCtx(mapOf("default_volume" to "90", "default_playback_mode" to "chat"))
        musicRuntime.initialize(ctx)

        assertEquals(90, audioPlayer.playbackState.value.volumePct)
    }

    @Test
    fun `responds to start and help with full command menu`() = runBlocking {
        val ctx = createCtx()
        musicRuntime.initialize(ctx)

        val reply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/help")
        assertTrue(reply.contains("TapStream Music & Audio Bot"))
        assertTrue(reply.contains("/play"))
        assertTrue(reply.contains("/radio"))
        assertTrue(reply.contains("/nowplaying"))
        assertTrue(reply.contains("/mode"))
    }

    @Test
    fun `play command queues and streams audio on device and chat`() = runBlocking {
        val ctx = createCtx(mapOf("default_playback_mode" to "both"))
        musicRuntime.initialize(ctx)

        val reply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/play lofi")
        assertTrue(reply.contains("Now Playing"))
        assertTrue(reply.contains("Lofi Girl Study Beats"))

        // Device player starts playing
        assertTrue(audioPlayer.playbackState.value.isPlaying)
        assertEquals("Lofi Girl Study Beats", audioPlayer.playbackState.value.currentTrack?.title)

        // Telegram sendAudio was called
        assertEquals(1, fakeApi.sentAudios.size)
        assertEquals(12345L, fakeApi.sentAudios[0].first)
        assertEquals("Lofi Girl Study Beats", fakeApi.sentAudios[0].third)
    }

    @Test
    fun `playback controls pause resume and stop update player state`() = runBlocking {
        val ctx = createCtx()
        musicRuntime.initialize(ctx)

        musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/play synthwave")
        assertTrue(audioPlayer.playbackState.value.isPlaying)

        // Pause
        val pauseReply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/pause")
        assertTrue(pauseReply.contains("Playback paused"))
        assertFalse(audioPlayer.playbackState.value.isPlaying)

        // Resume
        val resumeReply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/resume")
        assertTrue(resumeReply.contains("Resumed"))
        assertTrue(audioPlayer.playbackState.value.isPlaying)

        // Stop
        val stopReply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/stop")
        assertTrue(stopReply.contains("Stopped playback and cleared queue"))
        assertFalse(audioPlayer.playbackState.value.isPlaying)
    }

    @Test
    fun `nowplaying displays visual progress bar and track info`() = runBlocking {
        val ctx = createCtx()
        musicRuntime.initialize(ctx)

        musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/play jazz")
        val npReply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/nowplaying")

        assertTrue(npReply.contains("Now Playing on TapStream"))
        assertTrue(npReply.contains("Jazz Cafe & Smooth Bossa"))
        assertTrue(npReply.contains("PLAYING"))
    }

    @Test
    fun `volume command adjusts device audio volume`() = runBlocking {
        val ctx = createCtx()
        musicRuntime.initialize(ctx)

        val volReply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/volume 65")
        assertTrue(volReply.contains("65%"))
        assertEquals(65, audioPlayer.playbackState.value.volumePct)
    }

    @Test
    fun `radio command lists genres when blank and tunes in when specified`() = runBlocking {
        val ctx = createCtx()
        musicRuntime.initialize(ctx)

        // List genres
        val listReply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/radio")
        assertTrue(listReply.contains("24/7 Curated Internet Radio Stations"))
        assertTrue(listReply.contains("/radio lofi"))
        assertTrue(listReply.contains("/radio synthwave"))
        assertTrue(listReply.contains("/radio rock"))

        // Tune in
        val tuneReply = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/radio rock")
        assertTrue(tuneReply.contains("Tuned into"))
        assertTrue(tuneReply.contains("Classic Rock Live FM"))
        assertTrue(audioPlayer.playbackState.value.isPlaying)
    }

    @Test
    fun `mode command switches routing modes`() = runBlocking {
        val ctx = createCtx()
        musicRuntime.initialize(ctx)

        val speakerMode = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/mode speaker")
        assertTrue(speakerMode.contains("DEVICE SPEAKER ONLY"))

        val chatMode = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/mode chat")
        assertTrue(chatMode.contains("TELEGRAM CHAT ONLY"))

        val bothMode = musicRuntime.handleCommand(ctx, "TOKEN", 12345L, "/mode both")
        assertTrue(bothMode.contains("BOTH (SPEAKER + CHAT)"))
    }
}
