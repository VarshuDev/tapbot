package com.tapbot.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

sealed class DownloadState {
    data class Progress(val percentage: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Success(val destinationFile: File, val calculatedSha256: String) : DownloadState()
    data class Failed(val error: Throwable) : DownloadState()
}

/**
 * Downloads bot packages from R2/Cloudflare and verifies cryptographic integrity via SHA-256.
 */
class PackageDownloader(
    private val client: OkHttpClient = OkHttpClient()
) {
    fun downloadPackage(
        url: String,
        destinationFile: File,
        expectedSha256: String
    ): Flow<DownloadState> = flow {
        try {
            val cleanExpected = expectedSha256.trim()
            if (!Sha256PackageVerifier.SHA256_HEX_REGEX.matches(cleanExpected)) {
                emit(DownloadState.Failed(SecurityException("Valid expected SHA-256 checksum is required")))
                return@flow
            }
            UrlSecurityValidator.validateUrlSyntax(
                url,
                UrlSecurityValidator.ValidationOptions(requireHttps = true, allowLoopbackHosts = false)
            ).getOrThrow()

            destinationFile.parentFile?.mkdirs()
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Failed(IOException("Download failed with HTTP ${response.code}")))
                    return@flow
                }

                val body = response.body ?: throw IOException("Empty response body")
                val totalBytes = body.contentLength()
                var bytesDownloaded = 0L
                val digest = MessageDigest.getInstance("SHA-256")

                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            bytesDownloaded += read

                            val progress = if (totalBytes > 0) {
                                ((bytesDownloaded * 100) / totalBytes).toInt()
                            } else {
                                -1
                            }
                            emit(DownloadState.Progress(progress, bytesDownloaded, totalBytes))
                        }
                    }
                }

                val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
                if (!calculatedHash.equals(cleanExpected, ignoreCase = true)) {
                    destinationFile.delete()
                    emit(DownloadState.Failed(SecurityException("SHA-256 checksum mismatch! Expected: $cleanExpected, Calculated: $calculatedHash")))
                } else {
                    emit(DownloadState.Success(destinationFile, calculatedHash))
                }
            }
        } catch (e: Exception) {
            destinationFile.delete()
            emit(DownloadState.Failed(e))
        }
    }.flowOn(Dispatchers.IO)
}
