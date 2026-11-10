package com.tapbot.core.runner.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
 * Supports Android audio focus negotiation and a live elapsed progress ticker.
 */
class DeviceAudioPlayer(
    private val context: Context? = null,
    private val onTrackCompleted: (() -> Unit)? = null,
    private val tickIntervalMs: Long = 1000L
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var pausedByFocusLoss: Boolean = false
    private var preDuckVolumePct: Int? = null

    private val tickerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedByFocusLoss = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_playbackState.value.status == PlaybackStatus.PLAYING) {
                    pausedByFocusLoss = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (preDuckVolumePct == null) {
                    preDuckVolumePct = _playbackState.value.volumePct
                    applyPlayerVolumeOnly(25)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                preDuckVolumePct?.let { original ->
                    preDuckVolumePct = null
                    applyPlayerVolumeOnly(original)
                }
                if (pausedByFocusLoss && _playbackState.value.status == PlaybackStatus.PAUSED) {
                    pausedByFocusLoss = false
                    resume()
                }
            }
        }
    }

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
        stopTicker()
        pausedByFocusLoss = false
        _playbackState.value = _playbackState.value.copy(
            status = PlaybackStatus.BUFFERING,
            currentTrack = track,
            elapsedMs = 0L,
            durationMs = (track.durationSeconds * 1000L).coerceAtLeast(0L),
            errorMessage = null
        )

        if (context == null) {
            _playbackState.value = _playbackState.value.copy(
                status = PlaybackStatus.PLAYING,
                durationMs = if (track.durationSeconds > 0) track.durationSeconds * 1000L else 0L
            )
            startTicker()
            return true
        }

        return try {
            requestAudioFocus()
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
                startTicker()
            }

            mp.setOnCompletionListener {
                stopTicker()
                abandonAudioFocus()
                _playbackState.value = _playbackState.value.copy(
                    status = PlaybackStatus.STOPPED,
                    elapsedMs = _playbackState.value.durationMs
                )
                onTrackCompleted?.invoke()
            }

            mp.setOnErrorListener { _, what, extra ->
                stopTicker()
                abandonAudioFocus()
                _playbackState.value = _playbackState.value.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = "Playback error (code: $what, extra: $extra)"
                )
                true
            }

            mp.prepareAsync()
            true
        } catch (e: Exception) {
            stopTicker()
            abandonAudioFocus()
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

        stopTicker()
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

        requestAudioFocus()
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}

        _playbackState.value = cur.copy(status = PlaybackStatus.PLAYING)
        startTicker()
        return true
    }

    /**
     * Stops playback and resets player state.
     */
    fun stop(): Boolean {
        stopTicker()
        pausedByFocusLoss = false
        abandonAudioFocus()
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
     * Stops playback and cancels internal ticker coroutines permanently.
     */
    fun release() {
        stop()
        tickerScope.cancel()
    }

    /**
     * Adjusts playback volume (0 to 100).
     */
    fun setVolume(percentage: Int): Int {
        val clamped = percentage.coerceIn(0, 100)
        preDuckVolumePct = null
        _playbackState.value = _playbackState.value.copy(volumePct = clamped)

        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (am != null) {
            try {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (clamped * maxVol / 100.0).toInt().coerceIn(0, maxVol)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (_: Exception) {}
        }

        applyPlayerVolumeOnly(clamped)
        return clamped
    }

    /**
     * Seeks playback position in milliseconds.
     */
    fun seekTo(positionMs: Long): Boolean {
        val duration = _playbackState.value.durationMs
        val clamped = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        _playbackState.value = _playbackState.value.copy(elapsedMs = clamped)
        if (context == null || mediaPlayer == null) {
            return true
        }
        return try {
            mediaPlayer?.seekTo(clamped.toInt())
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun startTicker() {
        stopTicker()
        val interval = tickIntervalMs.coerceAtLeast(10L)
        tickerJob = tickerScope.launch {
            while (isActive) {
                delay(interval)
                val cur = _playbackState.value
                if (cur.status != PlaybackStatus.PLAYING) break

                val nextElapsed = if (mediaPlayer != null && context != null) {
                    try {
                        mediaPlayer?.currentPosition?.toLong()?.coerceAtLeast(0L) ?: (cur.elapsedMs + interval)
                    } catch (_: Throwable) {
                        cur.elapsedMs + interval
                    }
                } else {
                    val raw = cur.elapsedMs + interval
                    if (cur.durationMs > 0L) raw.coerceAtMost(cur.durationMs) else raw
                }
                _playbackState.value = _playbackState.value.copy(elapsedMs = nextElapsed)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun applyPlayerVolumeOnly(percentage: Int) {
        val floatVol = percentage.coerceIn(0, 100) / 100f
        try {
            mediaPlayer?.setVolume(floatVol, floatVol)
        } catch (_: Exception) {}
    }

    private fun requestAudioFocus() {
        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                    .also { audioFocusRequest = it }
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (_: Throwable) {}
    }

    private fun abandonAudioFocus() {
        val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusChangeListener)
            }
        } catch (_: Throwable) {}
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
