package com.tapbot.core.network

import com.tapbot.core.model.InlineKeyboardButton
import com.tapbot.core.model.InlineKeyboardMarkup
import com.tapbot.core.model.PromoteChatMemberPayload
import com.tapbot.core.model.RestrictChatPermissions
import com.tapbot.core.model.TelegramBotCommand
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramApiClientTest {

    @Test
    fun `sendMessage with InlineKeyboardMarkup, answerCallbackQuery, editMessageText, timed ban, and promoteChatMember serialize properly`() = runBlocking {
        val requests = mutableListOf<Pair<String, String>>()

        val interceptor = Interceptor { chain ->
            val request = chain.request()
            val buf = Buffer()
            request.body?.writeTo(buf)
            requests.add(request.url.encodedPath to buf.readUtf8())

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("""{"ok":true,"result":true}""".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val api = TelegramApiClient(client)

        val markup = InlineKeyboardMarkup(
            listOf(listOf(InlineKeyboardButton(text = "Pause", callbackData = "music:pause")))
        )

        assertTrue(api.sendMessage("T", 10L, "Player", "Markdown", null, markup).isSuccess)
        assertTrue(api.answerCallbackQuery("T", "cb_1", "Paused", showAlert = false).isSuccess)
        assertTrue(api.editMessageText("T", 10L, 55L, "Updated", "HTML", markup).isSuccess)
        assertTrue(api.banChatMember("T", -100L, 99L, untilDate = 1800000000L).isSuccess)
        assertTrue(api.restrictChatMember("T", -100L, 99L, RestrictChatPermissions(), untilDate = 1800000600L).isSuccess)
        assertTrue(api.promoteChatMember("T", PromoteChatMemberPayload.promote(-100L, 99L)).isSuccess)

        assertTrue(requests[0].first.endsWith("/sendMessage"))
        assertTrue(requests[0].second.contains("\"reply_markup\""))
        assertTrue(requests[0].second.contains("music:pause"))

        assertTrue(requests[1].first.endsWith("/answerCallbackQuery"))
        assertTrue(requests[1].second.contains("\"callback_query_id\":\"cb_1\""))

        assertTrue(requests[2].first.endsWith("/editMessageText"))
        assertTrue(requests[2].second.contains("\"message_id\":55"))

        assertTrue(requests[3].first.endsWith("/banChatMember"))
        assertTrue(requests[3].second.contains("\"until_date\":1800000000"))

        assertTrue(requests[4].first.endsWith("/restrictChatMember"))
        assertTrue(requests[4].second.contains("\"until_date\":1800000600"))

        assertTrue(requests[5].first.endsWith("/promoteChatMember"))
        assertTrue(requests[5].second.contains("\"can_delete_messages\":true"))
    }

    @Test
    fun `setMyCommands and getMyCommands serialize and parse commands correctly`() = runBlocking {
        var lastPath = ""
        var lastRequestBody = ""

        val interceptor = Interceptor { chain ->
            val request = chain.request()
            val buf = Buffer()
            request.body?.writeTo(buf)
            lastPath = request.url.encodedPath
            lastRequestBody = buf.readUtf8()

            val jsonResponse = if (lastPath.endsWith("/setMyCommands")) {
                """{"ok":true,"result":true}"""
            } else {
                """{"ok":true,"result":[{"command":"start","description":"Start bot"},{"command":"help","description":"Help menu"}]}"""
            }

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        val api = TelegramApiClient(client)

        val commands = listOf(
            TelegramBotCommand("start", "Start bot"),
            TelegramBotCommand("help", "Help menu"),
            TelegramBotCommand("status", "Show status")
        )

        val setResult = api.setMyCommands("TEST_TOKEN", commands)
        assertTrue(setResult.isSuccess)
        assertTrue(setResult.getOrThrow())
        assertTrue(lastPath.endsWith("/setMyCommands"))
        assertTrue(lastRequestBody.contains("\"command\":\"start\""))
        assertTrue(lastRequestBody.contains("\"description\":\"Start bot\""))
        assertTrue(lastRequestBody.contains("\"command\":\"status\""))

        val getResult = api.getMyCommands("TEST_TOKEN")
        assertTrue(getResult.isSuccess)
        val fetched = getResult.getOrThrow()
        assertEquals(2, fetched.size)
        assertEquals("start", fetched[0].command)
        assertEquals("Start bot", fetched[0].description)
        assertEquals("help", fetched[1].command)
    }
}
