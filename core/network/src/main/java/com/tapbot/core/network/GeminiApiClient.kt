package com.tapbot.core.network

import com.tapbot.core.security.SecretRedactor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GeminiChatMessage(
    val role: String,
    val text: String
)

@Serializable
data class GeminiPart(
    val text: String = ""
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart> = emptyList()
)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null
)

@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    @SerialName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerialName("finishReason") val finishReason: String? = null
)

@Serializable
data class GeminiErrorDetail(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val error: GeminiErrorDetail? = null
)

/**
 * Direct on-device HTTP client for Google's Gemini REST API (`generateContent`).
 * Supports multi-turn conversation history, custom system instructions, and configurable
 * base URLs for local testing.
 */
open class GeminiApiClient(
    private val client: OkHttpClient = sharedClient,
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    open suspend fun generateContent(
        apiKey: String,
        model: String = DEFAULT_MODEL,
        messages: List<GeminiChatMessage>,
        systemPrompt: String? = null,
        temperature: Double = 0.7,
        maxOutputTokens: Int = 2048
    ): Result<String> = runCatching {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key is required.")
        }
        if (messages.isEmpty()) {
            throw IllegalArgumentException("At least one message is required.")
        }

        val effectiveModel = model.trim().ifBlank { DEFAULT_MODEL }
        val normalizedBase = baseUrl.trimEnd('/')
        val url = "$normalizedBase/v1beta/models/$effectiveModel:generateContent"

        val requestPayload = GeminiGenerateContentRequest(
            contents = messages.map { msg ->
                GeminiContent(
                    role = msg.role.ifBlank { "user" },
                    parts = listOf(GeminiPart(text = msg.text))
                )
            },
            systemInstruction = systemPrompt?.trim()?.takeIf { it.isNotBlank() }?.let { prompt ->
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            },
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxOutputTokens
            )
        )

        val requestBody = json.encodeToString(requestPayload).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", cleanKey)
            .header("Content-Type", "application/json")
            .post(requestBody)
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val rawBody = UrlSecurityValidator.readResponseBodyCapped(response.body, MAX_RESPONSE_BYTES)
                val parsed = if (rawBody.isNotBlank()) {
                    try {
                        json.decodeFromString<GeminiGenerateContentResponse>(rawBody)
                    } catch (_: Exception) {
                        null
                    }
                } else null

                if (!response.isSuccessful) {
                    val apiMessage = SecretRedactor.redact(
                        parsed?.error?.message ?: response.message.ifBlank { "HTTP ${response.code}" }
                    )
                    throw IOException("Gemini API error (${response.code}): $apiMessage")
                }

                if (parsed?.error != null) {
                    val errMsg = SecretRedactor.redact(parsed.error.message ?: "Unknown error")
                    throw IOException("Gemini API error: $errMsg")
                }

                val firstCandidate = parsed?.candidates?.firstOrNull()
                val text = firstCandidate?.content?.parts
                    ?.joinToString("") { it.text }
                    ?.trim()
                    .orEmpty()

                if (text.isBlank()) {
                    val reason = firstCandidate?.finishReason ?: "EMPTY_RESPONSE"
                    throw IOException("Gemini returned an empty response (reason: $reason)")
                }

                text
            }
        }
    }.recoverCatching { err ->
        if (err is CancellationException) throw err
        val redacted = SecretRedactor.redactThrowable(err, "Gemini API request failed")
        when (err) {
            is IllegalArgumentException -> throw IllegalArgumentException(redacted)
            is SecurityException -> throw SecurityException(redacted)
            else -> throw IOException(redacted)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        private const val MAX_RESPONSE_BYTES = 2L * 1024 * 1024

        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
