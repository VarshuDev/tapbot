package com.tapbot.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiClientTest {

    @Test
    fun `generateContent sends multi-turn messages and systemInstruction and parses response`() = runTest {
        var recordedUrl: String? = null
        var recordedApiKeyHeader: String? = null
        var recordedBody: String? = null

        val interceptor = Interceptor { chain ->
            val request = chain.request()
            recordedUrl = request.url.toString()
            recordedApiKeyHeader = request.header("x-goog-api-key")
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            recordedBody = buffer.readUtf8()

            val responseJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "role": "model",
                        "parts": [
                          { "text": "Structured concurrency keeps child coroutines scoped cleanly." }
                        ]
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
            """.trimIndent()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val gemini = GeminiApiClient(client = client, baseUrl = "https://mock.gemini.local")

        val result = gemini.generateContent(
            apiKey = "AIzaSy-test-key-123",
            model = "gemini-2.5-flash",
            messages = listOf(
                GeminiChatMessage("user", "Hi Gemini"),
                GeminiChatMessage("model", "Hello! How can I help?"),
                GeminiChatMessage("user", "Explain structured concurrency")
            ),
            systemPrompt = "Be concise and technical."
        )

        assertTrue(result.isSuccess)
        assertEquals(
            "https://mock.gemini.local/v1beta/models/gemini-2.5-flash:generateContent",
            recordedUrl
        )
        assertEquals("AIzaSy-test-key-123", recordedApiKeyHeader)
        assertFalse("API key must stay in header, not query string", recordedUrl!!.contains("AIzaSy"))
        assertTrue(recordedBody!!.contains("Explain structured concurrency"))
        assertTrue(recordedBody!!.contains("Be concise and technical."))
        assertEquals(
            "Structured concurrency keeps child coroutines scoped cleanly.",
            result.getOrThrow()
        )
    }

    @Test
    fun `generateContent fails gracefully when api key is missing`() = runTest {
        val gemini = GeminiApiClient(baseUrl = "https://mock.gemini.local")
        val result = gemini.generateContent(
            apiKey = "   ",
            messages = listOf(GeminiChatMessage("user", "Hello"))
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key") == true)
    }

    @Test
    fun `generateContent surfaces API error description cleanly`() = runTest {
        val interceptor = Interceptor { chain ->
            val errJson = """
                {
                  "error": {
                    "code": 400,
                    "message": "API key not valid. Please pass a valid API key.",
                    "status": "INVALID_ARGUMENT"
                  }
                }
            """.trimIndent()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(400)
                .message("Bad Request")
                .body(errJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val gemini = GeminiApiClient(client = client, baseUrl = "https://mock.gemini.local")

        val result = gemini.generateContent(
            apiKey = "bad-key",
            messages = listOf(GeminiChatMessage("user", "Test"))
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key not valid") == true)
    }
}
