package com.tapbot.core.runner.device

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

sealed class DeviceActionResult {
    data class Success(val message: String) : DeviceActionResult()
    data class Failure(val errorMessage: String) : DeviceActionResult()
}

/**
 * Controller for hardware actions on the Android device (Ringtone, Flashlight, Vibrator, Notifications, Clipboard).
 * Enforces safety timeouts and owner authorization verification.
 */
class DeviceActionController(
    private val context: Context? = null,
    private val handler: Handler? = try { Handler(Looper.getMainLooper()) } catch (_: Throwable) { null }
) {
    private val isTorchOn = AtomicBoolean(false)
    private var activeRingtone: Ringtone? = null
    private var ringtoneStopRunnable: Runnable? = null
    private var isSimulatedRinging: Boolean = false
    private var inMemoryClipboard: String? = null

    /**
     * Checks if a sender Telegram user ID matches the configured owner_id.
     * If no owner_id is configured, allows initial execution for onboarding.
     */
    fun isOwnerAuthorized(configuredOwnerId: String?, senderUserId: Long?): Boolean {
        val trimmed = configuredOwnerId?.trim()
        if (trimmed.isNullOrBlank()) return true // Unconfigured owner allows initial setup
        if (senderUserId == null) return false
        return senderUserId.toString() == trimmed
    }

    /**
     * Rings the device at maximum volume to locate it.
     * Automatically silences after [durationSec] seconds (defaults to 15s, clamped between 5s and 60s).
     */
    fun ringPhone(durationSec: Int = 15, onAutoStop: (() -> Unit)? = null): DeviceActionResult {
        val clampedSec = durationSec.coerceIn(5, 60)
        stopRing()

        if (context == null) {
            isSimulatedRinging = true
            return DeviceActionResult.Success(
                "🔔 *Ringing phone at full volume for ${clampedSec} seconds!*\nSend /stopring to silence immediately."
            )
        }

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                try {
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
                } catch (_: Exception) {}
            }

            handler?.post {
                try {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    val ringtone = RingtoneManager.getRingtone(context, alertUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    ringtone.play()
                    activeRingtone = ringtone

                    ringtoneStopRunnable = Runnable {
                        stopRing()
                        onAutoStop?.invoke()
                    }
                    handler.postDelayed(ringtoneStopRunnable!!, clampedSec * 1000L)
                } catch (e: Exception) {
                    activeRingtone = null
                }
            }

            isSimulatedRinging = true
            DeviceActionResult.Success(
                "🔔 *Ringing phone at full volume for ${clampedSec} seconds!*\nSend /stopring to silence immediately."
            )
        } catch (e: Exception) {
            DeviceActionResult.Failure("Failed to ring phone: ${e.message}")
        }
    }

    /**
     * Immediately silences the active ringing alarm.
     */
    fun stopRing(): DeviceActionResult {
        isSimulatedRinging = false
        handler?.post {
            try {
                ringtoneStopRunnable?.let { handler.removeCallbacks(it) }
                ringtoneStopRunnable = null
                activeRingtone?.stop()
                activeRingtone = null
            } catch (_: Exception) {}
        }
        return DeviceActionResult.Success("🔕 Phone ringtone stopped.")
    }

    /**
     * Checks if the device is currently ringing.
     */
    fun isRinging(): Boolean {
        return isSimulatedRinging || (activeRingtone?.isPlaying == true)
    }

    /**
     * Toggles or sets the camera flashlight torch mode.
     */
    fun toggleTorch(stateArg: String? = null): DeviceActionResult {
        val targetState = when (stateArg?.lowercase()) {
            "on", "1", "true" -> true
            "off", "0", "false" -> false
            else -> !isTorchOn.get()
        }

        if (context == null) {
            isTorchOn.set(targetState)
            val msg = if (targetState) "🔦 Flashlight turned *ON*." else "🔦 Flashlight turned *OFF*."
            return DeviceActionResult.Success(msg)
        }

        val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (camManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return DeviceActionResult.Failure("Flashlight is not supported on this device/runtime.")
        }

        return try {
            val cameraId = camManager.cameraIdList.firstOrNull()
                ?: return DeviceActionResult.Failure("No camera flash unit found.")

            camManager.setTorchMode(cameraId, targetState)
            isTorchOn.set(targetState)
            val statusText = if (targetState) "🔦 Flashlight turned *ON*." else "🔦 Flashlight turned *OFF*."
            DeviceActionResult.Success(statusText)
        } catch (e: Exception) {
            DeviceActionResult.Failure("Torch error: ${e.message}")
        }
    }

    /**
     * Returns whether the torch is currently toggled on.
     */
    fun isTorchActive(): Boolean = isTorchOn.get()

    /**
     * Triggers a haptic vibration pulse on the host device.
     */
    fun vibrate(durationMs: Long = 500): DeviceActionResult {
        if (context == null) {
            return DeviceActionResult.Success("📳 Phone vibrated (${durationMs}ms).")
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
            DeviceActionResult.Success("📳 Phone vibrated (${durationMs}ms).")
        } catch (e: Exception) {
            DeviceActionResult.Failure("Vibrate failed: ${e.message}")
        }
    }

    /**
     * Posts a high-priority heads-up notification on the host phone screen.
     */
    fun postNotification(
        title: String = "Telegram Commander Alert",
        text: String
    ): DeviceActionResult {
        if (text.isBlank()) {
            return DeviceActionResult.Failure("Notification message text cannot be empty.")
        }

        if (context == null) {
            return DeviceActionResult.Success("🔔 Notification posted to Android device:\n\"$text\"")
        }

        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "tapbot_commander_notifications"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Commander Remote Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts pushed remotely from Telegram"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val pendingIntent = if (launchIntent != null) {
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            nm.notify(1099, notif)
            DeviceActionResult.Success("🔔 Notification posted to Android device:\n\"$text\"")
        } catch (e: Exception) {
            DeviceActionResult.Failure("Notification failed: ${e.message}")
        }
    }

    /**
     * Retrieves the text currently copied on the Android system clipboard.
     */
    fun getClipboard(): String? {
        if (context == null) return inMemoryClipboard
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0)?.text?.toString()
            } else null
        } catch (_: Throwable) {
            inMemoryClipboard
        }
    }

    /**
     * Copies text to the Android system clipboard.
     */
    fun setClipboard(text: String, label: String = "Telegram Commander"): Boolean {
        inMemoryClipboard = text
        if (context == null) return true
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            cm?.setPrimaryClip(clip)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
