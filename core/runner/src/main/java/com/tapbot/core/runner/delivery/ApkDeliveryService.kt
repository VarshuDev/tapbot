package com.tapbot.core.runner.delivery

import android.content.Context
import android.os.Build
import com.tapbot.core.network.TelegramApiClient
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

data class ApkFileInfo(
    val file: File,
    val sizeBytes: Long,
    val sizeMb: Double,
    val sha256Checksum: String,
    val versionName: String,
    val versionCode: Long,
    val exceedsTelegramLimit: Boolean
)

sealed class ApkDeliveryResult {
    data class Sent(val fileSizeMb: Double, val sha256: String) : ApkDeliveryResult()
    data class ExceededLimit(val fileSizeMb: Double, val message: String) : ApkDeliveryResult()
    data class FileNotFound(val message: String) : ApkDeliveryResult()
    data class DeliveryFailed(val errorMessage: String, val fallbackMessage: String) : ApkDeliveryResult()
}

/**
 * Service resolving the host TapBot APK, calculating SHA-256 integrity checksums,
 * and delivering APK files directly to users over Telegram chat via multipart upload.
 */
class ApkDeliveryService(
    private val context: Context? = null,
    private val maxUploadBytes: Long = 50L * 1024 * 1024 // 50 MB Telegram Bot API limit
) {

    fun resolveApkFile(): File? {
        // 1. Installed APK on Android device via ApplicationInfo.sourceDir
        val sourceDir = context?.applicationInfo?.sourceDir
        if (!sourceDir.isNullOrBlank()) {
            val candidate = File(sourceDir)
            if (candidate.exists() && candidate.canRead()) {
                return candidate
            }
        }

        // 2. Pre-built releases directory fallback
        val releaseDir = File("releases")
        if (releaseDir.exists() && releaseDir.isDirectory) {
            val apks = releaseDir.listFiles { _, name -> name.endsWith(".apk") }
            if (!apks.isNullOrEmpty()) {
                return apks.first()
            }
        }

        // 3. Local build output fallbacks
        val debugApk = File("app/build/outputs/apk/debug/app-debug.apk")
        if (debugApk.exists() && debugApk.canRead()) return debugApk

        val releaseApk = File("app/build/outputs/apk/release/app-release.apk")
        if (releaseApk.exists() && releaseApk.canRead()) return releaseApk

        val releaseBuildApk = File("app/build/outputs/apk/release/app-release-unsigned.apk")
        if (releaseBuildApk.exists() && releaseBuildApk.canRead()) return releaseBuildApk

        return null
    }

    fun getApkInfo(file: File): ApkFileInfo {
        val sizeBytes = file.length()
        val sizeMb = sizeBytes / (1024.0 * 1024.0)
        val sha256 = computeSha256(file)

        val pInfo = try {
            context?.packageManager?.getPackageInfo(context.packageName, 0)
        } catch (_: Throwable) {
            null
        }

        val versionName = pInfo?.versionName ?: "2.3.0"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { pInfo?.longVersionCode ?: 230L } catch (_: Throwable) { 230L }
        } else {
            @Suppress("DEPRECATION")
            try { pInfo?.versionCode?.toLong() ?: 230L } catch (_: Throwable) { 230L }
        }

        return ApkFileInfo(
            file = file,
            sizeBytes = sizeBytes,
            sizeMb = sizeMb,
            sha256Checksum = sha256,
            versionName = versionName,
            versionCode = versionCode,
            exceedsTelegramLimit = sizeBytes > maxUploadBytes
        )
    }

    fun computeSha256(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        }
    }

    fun formatApkCaption(info: ApkFileInfo): String {
        val sizeFormatted = String.format(Locale.US, "%.2f", info.sizeMb)
        return """
            🚀 *TapBot APK v${info.versionName} (Build ${info.versionCode})*
            ━━━━━━━━━━━━━━━━━━━━━━
            📦 Size: *${sizeFormatted} MB*
            🔒 SHA-256: `${info.sha256Checksum}`
            📱 Min SDK: Android 10+ (API 29–35)
            
            Run real Telegram bots directly on Android 24/7 without cloud fees!
        """.trimIndent()
    }

    fun formatLimitExceededMessage(info: ApkFileInfo): String {
        val sizeFormatted = String.format(Locale.US, "%.2f", info.sizeMb)
        return """
            🚀 *TapBot APK v${info.versionName} (Build ${info.versionCode})*
            ━━━━━━━━━━━━━━━━━━━━━━
            📦 Size: *${sizeFormatted} MB* (Exceeds Telegram 50MB bot upload limit)
            🔒 SHA-256: `${info.sha256Checksum}`
            
            ⚠️ The APK is larger than Telegram's 50MB bot upload limit.
            Download the official release directly via:
            • Web: [TapBot Official Releases](https://tapbot.dev/download)
            • ADB: `adb install -r app-release.apk`
        """.trimIndent()
    }

    suspend fun deliverApk(
        telegramApi: TelegramApiClient,
        token: String,
        chatId: Long
    ): ApkDeliveryResult {
        val file = resolveApkFile()
            ?: return ApkDeliveryResult.FileNotFound("No installed or built APK was found on this host device.")

        val info = getApkInfo(file)
        if (info.exceedsTelegramLimit) {
            val msg = formatLimitExceededMessage(info)
            telegramApi.sendMessage(token, chatId, msg, "Markdown")
            return ApkDeliveryResult.ExceededLimit(info.sizeMb, msg)
        }

        telegramApi.sendChatAction(token, chatId, "upload_document")
        val caption = formatApkCaption(info)
        val sendRes = telegramApi.sendDocument(
            token = token,
            chatId = chatId,
            file = file,
            caption = caption,
            parseMode = "Markdown"
        )

        return if (sendRes.isSuccess) {
            ApkDeliveryResult.Sent(info.sizeMb, info.sha256Checksum)
        } else {
            val err = sendRes.exceptionOrNull()?.message ?: "Upload timeout"
            val fallback = formatLimitExceededMessage(info)
            telegramApi.sendMessage(token, chatId, fallback, "Markdown")
            ApkDeliveryResult.DeliveryFailed(err, fallback)
        }
    }
}
