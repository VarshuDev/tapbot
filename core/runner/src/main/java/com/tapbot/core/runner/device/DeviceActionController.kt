package com.tapbot.core.runner.device

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

sealed class DeviceActionResult {
    data class Success(val message: String) : DeviceActionResult()
    data class Failure(val errorMessage: String) : DeviceActionResult()
}

/**
 * Controller for hardware actions on the Android device
 * (Ringtone, Flashlight, Vibrator, Notifications, Clipboard, Text-to-Speech, Wi-Fi Info, Location).
 * Enforces safety timeouts and owner authorization verification.
 */
class DeviceActionController(
    private val context: Context? = null,
    private val handler: Handler? = try { Handler(Looper.getMainLooper()) } catch (_: Throwable) { null },
    private val hasLocationPermissionOverride: Boolean? = null
) {
    private val isTorchOn = AtomicBoolean(false)
    private var activeRingtone: Ringtone? = null
    private var ringtoneStopRunnable: Runnable? = null
    private var isSimulatedRinging: Boolean = false
    private var inMemoryClipboard: String? = null

    private val ttsLock = Any()
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var pendingTtsUtterance: String? = null
    private var lastSpokenText: String? = null

    /**
     * Checks if a sender Telegram user ID matches the configured owner_id.
     * If no owner_id is configured, allows initial execution for onboarding.
     */
    fun isOwnerAuthorized(configuredOwnerId: String?, senderUserId: Long?): Boolean {
        val trimmed = configuredOwnerId?.trim()
        if (trimmed.isNullOrBlank()) return false
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
                } catch (_: Exception) {
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
     * Speaks text aloud through the Android device speaker via TextToSpeech.
     */
    fun speakText(text: String): DeviceActionResult {
        val cleanText = text.trim().take(500)
        if (cleanText.isBlank()) {
            return DeviceActionResult.Failure("Text-to-speech message cannot be empty.")
        }
        lastSpokenText = cleanText

        if (context == null) {
            return DeviceActionResult.Success("🗣️ *Speaking on phone speaker:*\n\"$cleanText\"")
        }

        return try {
            val initAndSpeak = {
                synchronized(ttsLock) {
                    if (textToSpeech == null) {
                        pendingTtsUtterance = cleanText
                        ttsReady = false
                        textToSpeech = TextToSpeech(context.applicationContext) { status ->
                            synchronized(ttsLock) {
                                if (status == TextToSpeech.SUCCESS) {
                                    ttsReady = true
                                    try {
                                        textToSpeech?.language = Locale.getDefault()
                                    } catch (_: Throwable) {}
                                    val queued = pendingTtsUtterance
                                    pendingTtsUtterance = null
                                    if (!queued.isNullOrBlank()) {
                                        textToSpeech?.speak(
                                            queued,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            "tapbot_tts_${System.currentTimeMillis()}"
                                        )
                                    }
                                }
                            }
                        }
                    } else if (ttsReady) {
                        textToSpeech?.speak(
                            cleanText,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "tapbot_tts_${System.currentTimeMillis()}"
                        )
                    } else {
                        pendingTtsUtterance = cleanText
                    }
                }
            }

            if (handler != null) {
                handler.post {
                    try {
                        initAndSpeak()
                    } catch (_: Throwable) {}
                }
            } else {
                initAndSpeak()
            }

            DeviceActionResult.Success("🗣️ *Speaking on phone speaker:*\n\"$cleanText\"")
        } catch (e: Throwable) {
            DeviceActionResult.Failure("Text-to-speech failed: ${e.message}")
        }
    }

    fun getLastSpokenText(): String? = lastSpokenText

    fun shutdownTts() {
        synchronized(ttsLock) {
            try {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
            } catch (_: Throwable) {}
            textToSpeech = null
            ttsReady = false
            pendingTtsUtterance = null
        }
    }

    /**
     * Reads current Wi-Fi and IP network interface details from the device.
     */
    @Suppress("DEPRECATION")
    fun getWifiDetails(): DeviceActionResult {
        if (context == null) {
            val localIp = resolveLocalIpv4Address() ?: "192.168.1.42"
            return DeviceActionResult.Success(
                "📶 *Wi-Fi & Network Details*\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "• Status: *Connected (Simulated Test Mode)*\n" +
                    "• SSID: `TapBot-Test-WiFi`\n" +
                    "• Local IP: `$localIp`\n" +
                    "• Link Speed: `433 Mbps` | Band: `5180 MHz`\n" +
                    "• Signal Strength: `-48 dBm`"
            )
        }

        return try {
            val hasWifiPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
            val linkProps = activeNetwork?.let { cm.getLinkProperties(it) }
            val linkIp = linkProps?.linkAddresses
                ?.firstOrNull { !it.address.isLoopbackAddress && it.address is Inet4Address }
                ?.address
                ?.hostAddress
                ?: resolveLocalIpv4Address()
                ?: "Unavailable"

            val transportLabel = when {
                caps == null -> "Offline"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connected"
            }

            if (!hasWifiPerm) {
                return DeviceActionResult.Success(
                    "📶 *Network Details*\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Transport: *$transportLabel*\n" +
                        "• Local IP: `$linkIp`\n" +
                        "• Wi-Fi Hardware Details: _ACCESS_WIFI_STATE permission unavailable_"
                )
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val isWifiEnabled = wifiManager?.isWifiEnabled == true
            val info = wifiManager?.connectionInfo

            val rawSsid = info?.ssid?.trim()?.removeSurrounding("\"")
            val ssidDisplay = when {
                !isWifiEnabled -> "Wi-Fi Radio Off"
                rawSsid.isNullOrBlank() || rawSsid == "<unknown ssid>" -> "Connected (Grant Location for SSID)"
                else -> rawSsid
            }

            val rssi = info?.rssi ?: -127
            val speed = info?.linkSpeed ?: -1
            val freq = info?.frequency ?: -1

            val ipInt = info?.ipAddress ?: 0
            val wifiIp = if (ipInt != 0) {
                String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            } else {
                linkIp
            }

            DeviceActionResult.Success(
                "📶 *Wi-Fi & Network Details*\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "• Transport: *$transportLabel*\n" +
                    "• Wi-Fi Enabled: *${if (isWifiEnabled) "Yes ✅" else "No ⚠️"}*\n" +
                    "• SSID: `$ssidDisplay`\n" +
                    "• Local IP: `$wifiIp`\n" +
                    (if (speed > 0) "• Link Speed: `${speed} Mbps`\n" else "") +
                    (if (freq > 0) "• Frequency: `${freq} MHz`\n" else "") +
                    (if (rssi > -120) "• Signal: `${rssi} dBm`\n" else "") +
                    "━━━━━━━━━━━━━━━━━━━━━━"
            )
        } catch (e: Exception) {
            DeviceActionResult.Failure("Failed to inspect Wi-Fi details: ${e.message}")
        }
    }

    /**
     * Retrieves the device's most recent cached GPS/Network location when authorized.
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): DeviceActionResult {
        if (context == null) {
            if (hasLocationPermissionOverride == false) {
                return DeviceActionResult.Failure(
                    "Location permission not granted. Enable Fine/Coarse Location in Android App Settings to use /location."
                )
            }
            val lat = "37.421998"
            val lon = "-122.084000"
            return DeviceActionResult.Success(
                "📍 *Last Known Device Location*\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "• Provider: `gps (Simulated)`\n" +
                    "• Coordinates: `$lat, $lon`\n" +
                    "• Accuracy: `±12m`\n" +
                    "• Map: [Open in Google Maps](https://maps.google.com/?q=$lat,$lon)"
            )
        }

        val hasFine = hasLocationPermissionOverride
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        val hasCoarse = hasLocationPermissionOverride
            ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)

        if (!hasFine && !hasCoarse) {
            return DeviceActionResult.Failure(
                "Location permission not granted. Open Android Settings -> Apps -> TapBot -> Permissions -> Location to authorize /location."
            )
        }

        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return DeviceActionResult.Failure("LocationManager service is unavailable on this device.")

            val providers = mutableListOf<String>()
            if (hasFine) {
                providers.add(LocationManager.GPS_PROVIDER)
            }
            providers.add(LocationManager.NETWORK_PROVIDER)
            providers.add(LocationManager.PASSIVE_PROVIDER)

            var bestLocation: Location? = null
            for (provider in providers) {
                val candidate = try {
                    lm.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (candidate != null && (bestLocation == null || candidate.time > bestLocation.time)) {
                    bestLocation = candidate
                }
            }

            if (bestLocation == null) {
                return DeviceActionResult.Failure(
                    "No recent location fix cached on device. Make sure system Location is turned on."
                )
            }

            val lat = String.format(Locale.US, "%.6f", bestLocation.latitude)
            val lon = String.format(Locale.US, "%.6f", bestLocation.longitude)
            val accuracy = if (bestLocation.hasAccuracy()) "±${bestLocation.accuracy.toInt()}m" else "unknown"
            val providerName = bestLocation.provider ?: "unknown"
            val ageMin = ((System.currentTimeMillis() - bestLocation.time).coerceAtLeast(0L)) / 60_000L

            DeviceActionResult.Success(
                "📍 *Last Known Device Location*\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━\n" +
                    "• Provider: `$providerName`\n" +
                    "• Coordinates: `$lat, $lon`\n" +
                    "• Accuracy: `$accuracy` | Age: `${ageMin}m ago`\n" +
                    "• Map: [Open in Google Maps](https://maps.google.com/?q=$lat,$lon)"
            )
        } catch (e: Exception) {
            DeviceActionResult.Failure("Failed to query location: ${e.message}")
        }
    }

    private fun resolveLocalIpv4Address(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
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
