package com.tapbot.core.runner

import com.tapbot.core.runner.audio.AudioSourceType
import com.tapbot.core.runner.audio.AudioTrack
import com.tapbot.core.runner.audio.DeviceAudioPlayer
import com.tapbot.core.runner.audio.PlaybackState
import com.tapbot.core.runner.audio.PlaybackStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceAudioPlayerTest {

    private lateinit var player: DeviceAudioPlayer

    private val sampleTrack = AudioTrack(
        id = "test_track_1",
        title = "Midnight Drive",
        artist = "Synthwave Boy",
        streamUrl = "https://cdn.example.com/synth.mp3",
        durationSeconds = 240, // 4 mins
        source = AudioSourceType.DIRECT_URL
    )

    private val liveStream = AudioTrack(
        id = "radio_lofi",
        title = "Lofi Girl 24/7",
        artist = "Lofi Records",
        streamUrl = "https://stream.example.com/live",
        durationSeconds = 0,
        source = AudioSourceType.RADIO_STATION,
        genre = "lofi"
    )

    @Before
    fun setUp() {
        player = DeviceAudioPlayer(context = null, tickIntervalMs = 25L)
    }

    @After
    fun tearDown() {
        player.release()
    }

    @Test
    fun `initial state is IDLE with default volume 80`() {
        val state = player.playbackState.value
        assertEquals(PlaybackStatus.IDLE, state.status)
        assertEquals(80, state.volumePct)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `playTrack transitions status to PLAYING and records track duration`() {
        val success = player.playTrack(sampleTrack)
        assertTrue(success)

        val state = player.playbackState.value
        assertEquals(PlaybackStatus.PLAYING, state.status)
        assertTrue(state.isPlaying)
        assertEquals("Midnight Drive", state.currentTrack?.title)
        assertEquals(240_000L, state.durationMs)
    }

    @Test
    fun `pause and resume update status sequentially`() {
        player.playTrack(sampleTrack)
        assertTrue(player.playbackState.value.isPlaying)

        // Pause
        val pauseResult = player.pause()
        assertTrue(pauseResult)
        assertEquals(PlaybackStatus.PAUSED, player.playbackState.value.status)
        assertFalse(player.playbackState.value.isPlaying)

        // Resume
        val resumeResult = player.resume()
        assertTrue(resumeResult)
        assertEquals(PlaybackStatus.PLAYING, player.playbackState.value.status)
        assertTrue(player.playbackState.value.isPlaying)
    }

    @Test
    fun `stop halts playback and transitions to STOPPED`() {
        player.playTrack(sampleTrack)
        val stopped = player.stop()
        assertTrue(stopped)
        assertEquals(PlaybackStatus.STOPPED, player.playbackState.value.status)
        assertFalse(player.playbackState.value.isPlaying)
    }

    @Test
    fun `setVolume clamps volume percentage between 0 and 100`() {
        assertEquals(50, player.setVolume(50))
        assertEquals(50, player.playbackState.value.volumePct)

        // Clamping upper
        assertEquals(100, player.setVolume(150))
        assertEquals(100, player.playbackState.value.volumePct)

        // Clamping lower
        assertEquals(0, player.setVolume(-20))
        assertEquals(0, player.playbackState.value.volumePct)
    }

    @Test
    fun `seekTo clamps to track duration bounds`() {
        player.playTrack(sampleTrack)

        player.seekTo(60_000L) // 1 minute in
        assertEquals(60_000L, player.playbackState.value.elapsedMs)

        // Seek past end of track
        player.seekTo(999_999L)
        assertEquals(240_000L, player.playbackState.value.elapsedMs)
    }

    @Test
    fun `formatProgressBar formats visual indicator for tracks and live streams`() {
        val trackState = PlaybackState(
            status = PlaybackStatus.PLAYING,
            currentTrack = sampleTrack,
            elapsedMs = 60_000L, // 1 min
            durationMs = 240_000L, // 4 mins
            volumePct = 75
        )
        val bar = trackState.formatProgressBar(12)
        assertTrue(bar.contains("▶"))
        assertTrue(bar.contains("01:00"))
        assertTrue(bar.contains("04:00"))
        assertTrue(bar.contains("●"))

        val liveState = PlaybackState(
            status = PlaybackStatus.PLAYING,
            currentTrack = liveStream,
            durationMs = 0L
        )
        val liveBar = liveState.formatProgressBar(12)
        assertTrue(liveBar.contains("LIVE STREAM 🔴"))
        assertTrue(liveBar.contains("[lofi]"))
    }

    @Test
    fun `ticker advances elapsedMs while playing and pauses when paused or stopped`() = runBlocking {
        player.playTrack(sampleTrack)
        delay(90)
        val elapsedWhilePlaying = player.playbackState.value.elapsedMs
        assertTrue("Expected elapsedMs ticker to advance while playing, was $elapsedWhilePlaying", elapsedWhilePlaying >= 25L)

        player.pause()
        val pausedElapsed = player.playbackState.value.elapsedMs
        delay(70)
        assertEquals("Ticker should stop advancing once paused", pausedElapsed, player.playbackState.value.elapsedMs)

        player.stop()
        assertEquals(0L, player.playbackState.value.elapsedMs)
    }
}
