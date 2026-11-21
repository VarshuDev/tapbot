package com.tapbot.core.network

import com.tapbot.core.model.AppUpdateManifest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Result of checking for app updates against remote manifest.
 */
sealed class AppUpdateCheckResult {
    /** App is currently running the latest version. */
    data class UpToDate(val manifest: AppUpdateManifest) : AppUpdateCheckResult()

    /** Newer version is available. */
    data class UpdateAvailable(
        val manifest: AppUpdateManifest,
        val isForceUpdate: Boolean
    ) : AppUpdateCheckResult()
}

/**
 * Client responsible for querying remote version control manifests on GitHub / CDN
 * to detect new updates and enforce mandatory update policies.
 */
class AppUpdateChecker(
    private val client: OkHttpClient = UrlSecurityValidator.createSafeHttpClient(),
    private val defaultEndpoints: List<String> = DEFAULT_ENDPOINTS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        val DEFAULT_ENDPOINTS = listOf(
            "https://raw.githubusercontent.com/VarshuDev/tapbot/main/version.json",
            "https://varshudev.github.io/tapbot/version.json"
        )
    }

    /**
     * Parse raw JSON string into [AppUpdateManifest].
     */
    fun parseManifest(jsonString: String): AppUpdateManifest {
        return json.decodeFromString(AppUpdateManifest.serializer(), jsonString)
    }

    /**
     * Queries remote endpoints sequentially until a valid manifest is retrieved.
     * Evaluates [currentVersionCode] to determine if an update is available or required.
     */
    suspend fun check(
        currentVersionCode: Long,
        customUrl: String? = null
    ): Result<AppUpdateCheckResult> = withContext(ioDispatcher) {
        val candidateUrls = buildList {
            if (!customUrl.isNullOrBlank()) {
                add(customUrl.trim())
            }
            addAll(defaultEndpoints)
        }

        var lastException: Exception? = null

        for (endpoint in candidateUrls) {
            try {
                val manifest = fetchManifestFromUrl(endpoint)
                val isAvailable = manifest.isUpdateAvailable(currentVersionCode)
                val isForce = manifest.isForceUpdate(currentVersionCode)

                val result = if (isAvailable) {
                    AppUpdateCheckResult.UpdateAvailable(
                        manifest = manifest,
                        isForceUpdate = isForce
                    )
                } else {
                    AppUpdateCheckResult.UpToDate(manifest = manifest)
                }
                return@withContext Result.success(result)
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: IOException("Failed to fetch version manifest from all endpoints"))
    }

    private fun fetchManifestFromUrl(url: String): AppUpdateManifest {
        val separator = if (url.contains("?")) "&" else "?"
        val cacheBustedUrl = "$url${separator}_ts=${System.currentTimeMillis()}"

        val request = Request.Builder()
            .url(cacheBustedUrl)
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected HTTP code ${response.code} from $url")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body from $url")
            return parseManifest(body)
        }
    }
}
