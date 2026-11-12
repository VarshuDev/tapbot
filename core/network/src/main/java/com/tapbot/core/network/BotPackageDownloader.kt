package com.tapbot.core.network

import com.tapbot.core.security.SecretRedactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Contract for downloading and verifying bot package (.botpkg) archives.
 */
interface BotPackageDownloader {
    /**
     * Downloads a package from [packageUrl], calculates and validates its SHA-256 checksum against [expectedSha256],
     * and writes it to [targetFile].
     */
    suspend fun downloadPackage(
        packageUrl: String,
        expectedSha256: String,
        targetFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File>

    /**
     * Signature-aware overload supporting cryptographic Ed25519 signature verification alongside SHA-256.
     */
    suspend fun downloadPackage(
        packageUrl: String,
        expectedSha256: String,
        targetFile: File,
        signature: String? = null,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = downloadPackage(packageUrl, expectedSha256, targetFile, onProgress)
}

/**
 * Production implementation using OkHttp with chunked streaming, HTTPS enforcement,
 * size bounding, and SHA-256 / Ed25519 verification.
 */
class OkHttpBotPackageDownloader(
    private val client: OkHttpClient = OkHttpClient(),
    private val cacheDir: File? = null,
    private val packageVerifier: PackageVerifier = Sha256PackageVerifier(),
    private val allowLoopbackHttp: Boolean = false
) : BotPackageDownloader {

    override suspend fun downloadPackage(
        packageUrl: String,
        expectedSha256: String,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Result<File> = downloadPackage(packageUrl, expectedSha256, targetFile, signature = null, onProgress = onProgress)

    override suspend fun downloadPackage(
        packageUrl: String,
        expectedSha256: String,
        targetFile: File,
        signature: String?,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanSha = expectedSha256.trim()
            if (!Sha256PackageVerifier.SHA256_HEX_REGEX.matches(cleanSha)) {
                throw SecurityException("Missing or invalid expected SHA-256 checksum")
            }

            // 1. Check local cache to avoid duplicate downloads; always verify integrity & signature!
            val cachedArchive = resolveSafeCachedArchive(packageUrl, cleanSha)
            if (cachedArchive != null && cachedArchive.exists() && cachedArchive.length() > 0) {
                val cacheVerification = packageVerifier.verifyPackage(cachedArchive, cleanSha, signature)
                if (cacheVerification.isSuccess) {
                    if (cachedArchive.canonicalPath != targetFile.canonicalPath) {
                        targetFile.parentFile?.mkdirs()
                        cachedArchive.copyTo(targetFile, overwrite = true)
                    }
                    onProgress(1.0f)
                    return@runCatching targetFile
                } else {
                    cachedArchive.delete()
                }
            }

            // 2. Non-HTTP bundled package URLs must exist in local cache; never bypass missing URLs silently
            if (!packageUrl.startsWith("http://", ignoreCase = true) && !packageUrl.startsWith("https://", ignoreCase = true)) {
                if (targetFile.exists() && targetFile.length() > 0 &&
                    packageVerifier.verifyPackage(targetFile, cleanSha, signature).isSuccess
                ) {
                    onProgress(1.0f)
                    return@runCatching targetFile
                }
                throw IOException("Package '$packageUrl' not found or failed integrity check in local cache")
            }

            // Enforce HTTPS & safe public routing rules for remote package downloads
            UrlSecurityValidator.validateUrlSyntax(
                packageUrl,
                UrlSecurityValidator.ValidationOptions(
                    requireHttps = true,
                    allowLoopbackHosts = allowLoopbackHttp
                )
            ).getOrThrow()

            // 3. Ensure target directories exist and verify disk space
            targetFile.parentFile?.mkdirs()
            val freeBytes = targetFile.parentFile?.usableSpace ?: Long.MAX_VALUE
            if (freeBytes < 10 * 1024 * 1024) {
                throw IOException("Insufficient storage space: only ${freeBytes / (1024 * 1024)} MB free on device (minimum 10 MB required).")
            }
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp_${System.currentTimeMillis()}")

            val request = Request.Builder()
                .url(packageUrl)
                .header("Accept", "application/octet-stream")
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed to download package: HTTP ${response.code} ${response.message}")
                    }

                    val responseBody = response.body ?: throw IOException("Empty response body from package server")
                    val totalBytes = responseBody.contentLength()
                    if (totalBytes > MAX_PACKAGE_BYTES) {
                        throw SecurityException("Remote package exceeds maximum allowed size ($MAX_PACKAGE_BYTES bytes)")
                    }
                    var bytesReadTotal = 0L

                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                bytesReadTotal += read
                                if (bytesReadTotal > MAX_PACKAGE_BYTES) {
                                    throw SecurityException("Downloaded package exceeded maximum allowed size ($MAX_PACKAGE_BYTES bytes)")
                                }
                                outputStream.write(buffer, 0, read)

                                if (totalBytes > 0) {
                                    val progress = (bytesReadTotal.toFloat() / totalBytes.toFloat()).coerceIn(0.0f, 0.99f)
                                    onProgress(progress)
                                }
                            }
                            outputStream.flush()
                        }
                    }
                }

                // 4. Verify Package Integrity & Signature using PackageVerifier
                packageVerifier.verifyPackage(tempFile, cleanSha, signature).getOrThrow()

                // 5. Move temp file to final destination
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                // 6. Populate cache safely
                val safeCacheTarget = cacheDir?.let { dir ->
                    dir.mkdirs()
                    resolveChildSafely(dir, "$cleanSha.botpkg")
                }
                if (safeCacheTarget != null && safeCacheTarget.canonicalPath != targetFile.canonicalPath) {
                    targetFile.copyTo(safeCacheTarget, overwrite = true)
                }

                onProgress(1.0f)
                targetFile
            } catch (e: Exception) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                val safeMsg = SecretRedactor.redact(e.message ?: "Package download failed")
                when (e) {
                    is SecurityException -> throw SecurityException(safeMsg)
                    is IOException -> throw IOException(safeMsg)
                    else -> throw IOException(safeMsg)
                }
            }
        }
    }

    private fun resolveSafeCachedArchive(packageUrl: String, cleanSha: String): File? {
        val dir = cacheDir ?: return null
        val bySha = resolveChildSafely(dir, "$cleanSha.botpkg")
        if (bySha != null && bySha.exists() && bySha.length() > 0) return bySha

        val rawSegment = packageUrl.substringBefore('?').substringBefore('#').substringAfterLast('/')
        val byName = resolveChildSafely(dir, rawSegment)
        return if (byName != null && byName.exists() && byName.length() > 0) byName else null
    }

    private fun resolveChildSafely(parent: File, childName: String): File? {
        if (childName.isBlank() || childName == "." || childName == ".." ||
            !SAFE_FILENAME_REGEX.matches(childName)
        ) {
            return null
        }
        val candidate = File(parent, childName)
        return if (candidate.canonicalPath.startsWith(parent.canonicalPath + File.separator)) {
            candidate
        } else {
            null
        }
    }

    companion object {
        private const val MAX_PACKAGE_BYTES = 100L * 1024 * 1024
        private val SAFE_FILENAME_REGEX = Regex("^[a-zA-Z0-9._-]+$")
    }
}
