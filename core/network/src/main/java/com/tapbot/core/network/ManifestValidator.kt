package com.tapbot.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.ZipFile

/**
 * Result of manifest validation check.
 */
data class ManifestValidationResult(
    val isValid: Boolean,
    val botId: String,
    val version: String,
    val runtime: String = "native_art",
    val minimumAppVersion: Int = 1,
    val details: String = "Manifest valid"
)

/**
 * Validates the manifest and structure of a downloaded `.botpkg` archive.
 * Verifies manifest existence, syntax, bot ID, version matching, and runtime requirements.
 */
class ManifestValidator(
    private val appVersionCode: Int = 1,
    private val supportedRuntimes: Set<String> = setOf("native_art", "built-in", "dex")
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Validates [packageFile] archive against [expectedBotId] and [expectedVersion].
     */
    fun validatePackage(
        packageFile: File,
        expectedBotId: String,
        expectedVersion: String
    ): Result<ManifestValidationResult> = runCatching {
        if (!packageFile.exists() || packageFile.length() == 0L) {
            throw IllegalArgumentException("Package file does not exist or is empty: ${packageFile.name}")
        }

        // Enforce strict ZIP archive validation with Zip-Slip and Zip-Bomb guards
        ZipFile(packageFile).use { zip ->
            var entryCount = 0
            var totalUncompressed = 0L
            val canonicalRoot = File("/tapbot_pkg_root").canonicalFile
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                entryCount++
                if (entryCount > MAX_ZIP_ENTRIES) {
                    throw SecurityException("Package archive exceeds maximum entry count ($MAX_ZIP_ENTRIES)")
                }

                val entryName = entry.name
                if (entryName.isBlank() ||
                    entryName.contains('\u0000') ||
                    entryName.startsWith("/") ||
                    entryName.startsWith("\\") ||
                    entryName.contains("..")
                ) {
                    throw SecurityException("Unsafe Zip-Slip path detected in archive entry: '$entryName'")
                }
                val resolved = File(canonicalRoot, entryName).canonicalFile
                if (!resolved.path.startsWith(canonicalRoot.path + File.separator) && resolved != canonicalRoot) {
                    throw SecurityException("Archive entry escapes package root: '$entryName'")
                }

                val entrySize = entry.size
                if (entrySize > 0L) {
                    totalUncompressed += entrySize
                    if (totalUncompressed > MAX_UNCOMPRESSED_BYTES) {
                        throw SecurityException("Package archive exceeds maximum uncompressed size ($MAX_UNCOMPRESSED_BYTES bytes)")
                    }
                }
            }

            val manifestEntry = zip.getEntry("manifest.json") ?: zip.getEntry("bot/manifest.json")
                ?: throw IllegalArgumentException("Package archive is missing required manifest.json")
            if (manifestEntry.size > MAX_MANIFEST_BYTES) {
                throw SecurityException("manifest.json exceeds maximum allowed size ($MAX_MANIFEST_BYTES bytes)")
            }

            val content = zip.getInputStream(manifestEntry).use { input ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > MAX_MANIFEST_BYTES) {
                        throw SecurityException("manifest.json stream exceeds maximum allowed size")
                    }
                    out.write(buffer, 0, read)
                }
                out.toString(Charsets.UTF_8.name())
            }
            validateManifestContent(content, expectedBotId, expectedVersion)
        }
    }

    companion object {
        private const val MAX_ZIP_ENTRIES = 500
        private const val MAX_UNCOMPRESSED_BYTES = 50L * 1024 * 1024
        private const val MAX_MANIFEST_BYTES = 256 * 1024
    }

    /**
     * Validates the JSON string content of a manifest.json.
     */
    fun validateManifestContent(
        manifestJson: String,
        expectedBotId: String,
        expectedVersion: String
    ): ManifestValidationResult {
        val root = json.parseToJsonElement(manifestJson).jsonObject

        val id = root["id"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("manifest.json missing required 'id' field")
        val version = root["version"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("manifest.json missing required 'version' field")
        val runtime = root["runtime"]?.jsonPrimitive?.content ?: "native_art"
        val minAppVer = root["minimumAppVersion"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1

        if (!isMatchingBotId(id, expectedBotId)) {
            throw SecurityException("Manifest bot id mismatch: expected '$expectedBotId', found '$id'")
        }

        if (version != expectedVersion) {
            throw SecurityException("Manifest version mismatch: expected '$expectedVersion', found '$version'")
        }

        if (minAppVer > appVersionCode) {
            throw IllegalStateException("Bot requires app version $minAppVer, but current app version is $appVersionCode")
        }

        if (!supportedRuntimes.contains(runtime.lowercase())) {
            throw IllegalArgumentException("Unsupported runtime in manifest: '$runtime'. Supported: $supportedRuntimes")
        }

        return ManifestValidationResult(
            isValid = true,
            botId = id,
            version = version,
            runtime = runtime,
            minimumAppVersion = minAppVer,
            details = "Manifest successfully validated for $id v$version"
        )
    }

    private fun isMatchingBotId(manifestId: String, expectedBotId: String): Boolean {
        if (manifestId == expectedBotId) return true
        val canonicalAliases = mapOf(
            "bot_commander" to setOf("tapbot-commander", "bot_commander_flagship"),
            "bot_commander_flagship" to setOf("tapbot-commander", "bot_commander"),
            "tapbot-commander" to setOf("bot_commander", "bot_commander_flagship"),
            "bot_music_streamer" to setOf("tapstream-music"),
            "tapstream-music" to setOf("bot_music_streamer")
        )
        return canonicalAliases[expectedBotId]?.contains(manifestId) == true
    }
}
