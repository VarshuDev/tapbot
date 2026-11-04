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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TelegramApiAudioTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `sendAudio with stream URL serializes all fields correctly`() = runTest {
        var recordedUrl: String? = null
        var recordedBody: String? = null

        val interceptor = Interceptor { chain ->
            val request = chain.request()
            recordedUrl = request.url.toString()
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            recordedBody = buffer.readUtf8()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"ok":true,"result":{"message_id":555}}""".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val telegramApi = TelegramApiClient(client)

        val result = telegramApi.sendAudio(
            token = "TEST_TOKEN_123",
            chatId = 987654321L,
            audioUrl = "https://cdn.tapbot.internal/stream/chill_hop.m4a",
            title = "Chill Study Beats",
            performer = "Lofi Artist",
            duration = 180,
            caption = "🎧 Now playing live",
            parseMode = "Markdown"
        )

        assertTrue(result.isSuccess)
        assertEquals("https://api.telegram.org/botTEST_TOKEN_123/sendAudio", recordedUrl)
        assertTrue(recordedBody!!.contains("987654321"))
        assertTrue(recordedBody!!.contains("https://cdn.tapbot.internal/stream/chill_hop.m4a"))
        assertTrue(recordedBody!!.contains("Chill Study Beats"))
        assertTrue(recordedBody!!.contains("Lofi Artist"))
        assertTrue(recordedBody!!.contains("180"))
        assertTrue(recordedBody!!.contains("Now playing live"))
    }

    @Test
    fun `sendAudio with local file uses multipart upload and includes metadata`() = runTest {
        val testFile = tempFolder.newFile("sample_track.mp3")
        testFile.writeText("MOCK_MP3_AUDIO_STREAM_BYTES")

        var recordedContentType: String? = null
        var recordedBodyText: String? = null

        val interceptor = Interceptor { chain ->
            val request = chain.request()
            recordedContentType = request.body?.contentType()?.toString()
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            recordedBodyText = buffer.readUtf8()

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"ok":true,"result":{"message_id":556}}""".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val telegramApi = TelegramApiClient(client)

        val result = telegramApi.sendAudio(
            token = "TEST_TOKEN_FILE",
            chatId = 11223344L,
            file = testFile,
            title = "Retro Synthwave",
            performer = "Neon Driver",
            duration = 210,
            caption = "🚀 High energy synth",
            parseMode = "Markdown"
        )

        assertTrue(result.isSuccess)
        assertTrue(recordedContentType!!.startsWith("multipart/form-data"))
        assertTrue(recordedBodyText!!.contains("name=\"chat_id\""))
        assertTrue(recordedBodyText!!.contains("11223344"))
        assertTrue(recordedBodyText!!.contains("filename=\"sample_track.mp3\""))
        assertTrue(recordedBodyText!!.contains("MOCK_MP3_AUDIO_STREAM_BYTES"))
        assertTrue(recordedBodyText!!.contains("Retro Synthwave"))
        assertTrue(recordedBodyText!!.contains("Neon Driver"))
        assertTrue(recordedBodyText!!.contains("210"))
    }

    @Test
    fun `sendAudio handles API failure response with descriptive error`() = runTest {
        val interceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"ok":false,"error_code":400,"description":"Bad Request: wrong file identifier/HTTP URL specified"}""".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val telegramApi = TelegramApiClient(client)

        val result = telegramApi.sendAudio(
            token = "INVALID_TOKEN",
            chatId = 12345L,
            audioUrl = "ftp://invalid-url"
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("wrong file identifier") == true)
    }
}
