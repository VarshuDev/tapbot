package com.tapbot.core.runner.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    STOPPED,
    ERROR
}

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentTrack: AudioTrack? = null,
    val elapsedMs: Long = 0L,
    val durationMs: Long = 0L,
    val volumePct: Int = 80,
    val errorMessage: String? = null
) {
    val isPlaying: Boolean get() = status == PlaybackStatus.PLAYING

    fun formatProgressBar(barLength: Int = 12): String {
        if (durationMs <= 0L) {
            val icon = if (isPlaying) "▶" else "⏸"
            return "$icon LIVE STREAM 🔴 [${currentTrack?.genre ?: "Audio"}]"
        }
        val progress = (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        val head = (progress * barLength).toInt().coerceIn(0, barLength - 1)
        val bar = StringBuilder()
        for (i in 0 until barLength) {
            if (i == head) bar.append("●")
            else if (i < head) bar.append("━")
            else bar.append("─")
        }
        val elapsedSec = (elapsedMs / 1000).toInt()
        val totalSec = (durationMs / 1000).toInt()
        val elapsedStr = "%02d:%02d".format(Locale.US, elapsedSec / 60, elapsedSec % 60)
        val totalStr = "%02d:%02d".format(Locale.US, totalSec / 60, totalSec % 60)
        val icon = if (isPlaying) "▶" else "⏸"
        return "$icon $elapsedStr $bar $totalStr"
    }
}

/**
 * On-device hardware audio player wrapping Android MediaPlayer and AudioManager.
 * Enables Android device speakers/Bluetooth output to be controlled remotely via Telegram.
 */
class DeviceAudioPlayer(
    private val context: Context? = null,
    private val onTrackCompleted: (() -> Unit)? = null
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null

    init {
        try {
            handler = Handler(Looper.getMainLooper())
        } catch (_: Throwable) {
            // JVM unit test environment
        }
    }

    /**
     * Starts streaming and playing the specified audio track.
     */
    fun playTrack(track: AudioTrack): Boolean {
        _playbackState.value = _playbackState.value.copy(
            status = PlaybackStatus.BUFFERING,
            currentTrack = track,
            elapsedMs = 0L,
            durationMs = (track.durationSeconds * 1000L).coerceAtLeast(0L),
            errorMessage = null
        )

        if (context == null) {
            // Simulated playback for local test environments
            _playbackState.value = _playbackState.value.copy(
                status = PlaybackStatus.PLAYING,
                durationMs = if (track.durationSeconds > 0) track.durationSeconds * 1000L else 0L
            )
            return true
        }

        return try {
            releaseMediaPlayer()
            val mp = MediaPlayer()
            mediaPlayer = mp

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }

            mp.setDataSource(track.streamUrl)
            mp.setOnPreparedListener { player ->
                player.start()
                val realDuration = try { player.duration.toLong().coerceAtLeast(0L) } catch (_: Throwable) { 0L }
                _playbackState.value = _playbackState.value.copy(
                    status = PlaybackStatus.PLAYING,
                    durationMs = if (realDuration > 0) realDuration else (track.durationSeconds * 1000L)
                )
            }

            mp.setOnCompletionListener {
                _playbackState.value = _playbackState.value.copy(
                    status = PlaybackStatus.STOPPED,
                    elapsedMs = _playbackState.value.durationMs
                )
                onTrackCompleted?.invoke()
            }

            mp.setOnErrorListener { _, what, extra ->
                _playbackState.value = _playbackState.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = "Playback error (code: $what, extra: $extra)"
                )
                true
            }

            mp.prepareAsync()
            true
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = e.message ?: "Failed to start audio playback"
            )
            false
        }
    }

    /**
     * Pauses current playback.
     */
    fun pause(): Boolean {
        val cur = _playbackState.value
        if (cur.status != PlaybackStatus.PLAYING) return false

        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}

        _playbackState.value = cur.copy(status = PlaybackStatus.PAUSED)
        return true
    }

    /**
     * Resumes paused playback.
     */
    fun resume(): Boolean {
        val cur = _playbackState.value
        if (cur.status != PlaybackStatus.PAUSED) return false

        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}

        _playbackState.value = cur.copy(status = PlaybackStatus.PLAYING)
        return true
    }

    /**
     * Stops playback and resets player state.
     */
    fun stop(): Boolean {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        releaseMediaPlayer()

        _playbackState.value = _playbackState.value.copy(
            status = PlaybackStatus.STOPPED,
            elapsedMs = 0L
        )
        return true
    }

    /**
     * Adjusts playback volume (0 to 100).
     */
    fun setVolume(percentage: Int): Int {
        val clamped = percentage.coerceIn(0, 100)
        _playbackState.value = _playbackState.value.copy(volumePct = clamped)

        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            try {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (clamped * maxVol / 100.0).toInt().coerceIn(0, maxVol)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (_: Exception) {}
        }

        val floatVol = clamped / 100f
        try {
            mediaPlayer?.setVolume(floatVol, floatVol)
        } catch (_: Exception) {}

        return clamped
    }

    /**
     * Seeks playback position in milliseconds.
     */
    fun seekTo(positionMs: Long): Boolean {
        val clamped = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(0L))
        _playbackState.value = _playbackState.value.copy(elapsedMs = clamped)
        try {
            mediaPlayer?.seekTo(clamped.toInt())
            return true
        } catch (_: Exception) {
            return false
        }
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
