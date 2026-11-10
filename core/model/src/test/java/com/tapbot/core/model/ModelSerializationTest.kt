package com.tapbot.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serialize and deserialize BotMetadata`() {
        val bot = BotMetadata(
            id = "bot_sample",
            name = "Sample Bot",
            summary = "Sample summary",
            description = "Full description",
            author = "Architect",
            version = "1.0.0",
            iconUrl = "https://example.com/icon.png",
            category = "Utilities",
            tags = listOf("sample", "test"),
            requiredCredentials = listOf(BotCredentialSpec.TELEGRAM_BOT_TOKEN),
            packageInfo = BotPackageInfo(
                packageUrl = "https://example.com/bot.botpkg",
                sha256Checksum = "deadbeef"
            )
        )

        val serialized = json.encodeToString(bot)
        val deserialized = json.decodeFromString<BotMetadata>(serialized)

        assertEquals("bot_sample", deserialized.id)
        assertEquals("Sample Bot", deserialized.name)
        assertEquals(1, deserialized.requiredCredentials.size)
        assertEquals("bot_token", deserialized.requiredCredentials.first().key)
    }

    @Test
    fun `deserialize TelegramApiResponse with update`() {
        val rawJson = """
            {
                "ok": true,
                "result": [
                    {
                        "update_id": 998877,
                        "message": {
                            "message_id": 42,
                            "date": 1726740000,
                            "chat": {
                                "id": 1234567,
                                "type": "private",
                                "first_name": "Alice"
                            },
                            "from": {
                                "id": 1234567,
                                "is_bot": false,
                                "first_name": "Alice",
                                "username": "alice_dev"
                            },
                            "text": "/start"
                        }
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<TelegramApiResponse<List<TelegramUpdate>>>(rawJson)
        assertTrue(response.ok)
        assertNotNull(response.result)
        assertEquals(1, response.result?.size)

        val update = response.result?.first()
        assertEquals(998877L, update?.updateId)
        assertEquals("/start", update?.message?.text)
        assertEquals("alice_dev", update?.message?.from?.username)
    }

    @Test
    fun `deserialize callback_query and inline keyboard payloads cleanly`() {
        val rawUpdateJson = """
            {
              "update_id": 10055,
              "callback_query": {
                "id": "cb_987",
                "from": { "id": 42, "is_bot": false, "first_name": "Alex" },
                "message": {
                  "message_id": 77,
                  "date": 1726740000,
                  "chat": { "id": -100123, "type": "supergroup", "title": "Devs" },
                  "text": "Menu"
                },
                "data": "cmd:status"
              }
            }
        """.trimIndent()

        val update = json.decodeFromString<TelegramUpdate>(rawUpdateJson)
        assertEquals(10055L, update.updateId)
        assertEquals("cb_987", update.callbackQuery?.id)
        assertEquals("cmd:status", update.callbackQuery?.data)
        assertEquals(-100123L, update.callbackQuery?.message?.chat?.id)

        val markup = InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(InlineKeyboardButton(text = "Refresh", callbackData = "cmd:status"))
            )
        )
        val sendJson = json.encodeToString(
            SendMessagePayload(chatId = 1L, text = "HUD", replyMarkup = markup)
        )
        assertTrue(sendJson.contains("inline_keyboard"))
        assertTrue(sendJson.contains("callback_data"))
    }
}
