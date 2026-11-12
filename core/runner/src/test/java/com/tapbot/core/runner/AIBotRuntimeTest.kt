package com.tapbot.core.runner

import com.tapbot.core.model.TelegramChat
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.GeminiApiClient
import com.tapbot.core.network.GeminiChatMessage
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.runtime.AIBotRuntime
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.RuntimeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AIBotRuntimeTest {

    private class FakeGeminiClient : GeminiApiClient() {
        val capturedCalls = mutableListOf<Triple<String, List<GeminiChatMessage>, String?>>()
        var nextReply: String = "Gemini default test reply"

        override suspend fun generateContent(
            apiKey: String,
            model: String,
            messages: List<GeminiChatMessage>,
            systemPrompt: String?,
            temperature: Double,
            maxOutputTokens: Int
        ): Result<String> {
            capturedCalls.add(Triple(model, messages.toList(), systemPrompt))
            return Result.success(nextReply)
        }
    }

    private class FakeTelegramApi : TelegramApiClient() {
        val sentMessages = mutableListOf<Pair<Long, String>>()

        override suspend fun getMe(token: String) = Result.success(
            TelegramUser(id = 700L, isBot = true, firstName = "Gemini AI", username = "TapBotGemini")
        )

        override suspend fun getUpdates(token: String, offset: Long?, timeoutSeconds: Int) =
            Result.success(emptyList<TelegramUpdate>())

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?
        ): Result<Boolean> {
            sentMessages.add(chatId to text)
            return Result.success(true)
        }

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?,
            replyToMessageId: Long?
        ): Result<Boolean> {
            sentMessages.add(chatId to text)
            return Result.success(true)
        }

        override suspend fun sendChatAction(token: String, chatId: Long, action: String) =
            Result.success(true)
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Test
    fun `factory creates AIBotRuntime for bot_ai_assistant and bot_ai`() {
        assertTrue(BotRuntimeFactory.createRuntime("bot_ai_assistant") is AIBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("bot_ai") is AIBotRuntime)
    }

    @Test
    fun `ask command calls Gemini and preserves multi-turn per-chat memory until cleared`() = runBlocking {
        val fakeGemini = FakeGeminiClient()
        val fakeTg = FakeTelegramApi()
        val runtime = AIBotRuntime("bot_ai_assistant", geminiClient = fakeGemini)
        val ctx = RuntimeContext(
            botId = "bot_ai_assistant",
            token = "tg-token",
            telegramApi = fakeTg,
            log = { _, _, _ -> },
            scope = scope,
            credentials = mapOf(
                "gemini_api_key" to "AIzaSy-secret",
                "gemini_model" to "gemini-2.5-flash",
                "system_prompt" to "Answer like a senior Kotlin engineer."
            )
        )
        runtime.initialize(ctx)

        fakeGemini.nextReply = "Coroutines use cooperative cancellation."
        val msg1 = TelegramMessage(
            messageId = 1,
            from = TelegramUser(10, false, "Varshan"),
            chat = TelegramChat(42L, "private"),
            date = 1000L,
            text = "/ask What are coroutines?"
        )
        runtime.handleMessage(ctx, "tg-token", msg1)
        assertEquals(1, fakeGemini.capturedCalls.size)
        assertEquals("gemini-2.5-flash", fakeGemini.capturedCalls[0].first)
        assertEquals("Answer like a senior Kotlin engineer.", fakeGemini.capturedCalls[0].third)
        assertEquals(1, fakeGemini.capturedCalls[0].second.size)

        // Second message is natural PM chat without '/'
        fakeGemini.nextReply = "Yes, Structured Concurrency enforces it."
        val msg2 = TelegramMessage(
            messageId = 2,
            from = TelegramUser(10, false, "Varshan"),
            chat = TelegramChat(42L, "private"),
            date = 1001L,
            text = "How does cancellation work cleanly?"
        )
        runtime.handleMessage(ctx, "tg-token", msg2)

        assertEquals(2, fakeGemini.capturedCalls.size)
        val turn2History = fakeGemini.capturedCalls[1].second
        assertEquals(3, turn2History.size) // user + model + user
        assertEquals("What are coroutines?", turn2History[0].text)
        assertEquals("Coroutines use cooperative cancellation.", turn2History[1].text)
        assertEquals("How does cancellation work cleanly?", turn2History[2].text)

        // /clear resets history
        val clearMsg = TelegramMessage(
            messageId = 3,
            from = TelegramUser(10, false, "Varshan"),
            chat = TelegramChat(42L, "private"),
            date = 1002L,
            text = "/clear"
        )
        runtime.handleMessage(ctx, "tg-token", clearMsg)

        val msg3 = TelegramMessage(
            messageId = 4,
            from = TelegramUser(10, false, "Varshan"),
            chat = TelegramChat(42L, "private"),
            date = 1003L,
            text = "/ask Fresh question"
        )
        runtime.handleMessage(ctx, "tg-token", msg3)
        assertEquals(1, fakeGemini.capturedCalls[2].second.size)
    }

    @Test
    fun `summarize uses replied message text and long outputs split cleanly into Telegram chunks`() = runBlocking {
        val fakeGemini = FakeGeminiClient()
        val fakeTg = FakeTelegramApi()
        val runtime = AIBotRuntime("bot_ai_assistant", geminiClient = fakeGemini)
        val ctx = RuntimeContext(
            botId = "bot_ai_assistant",
            token = "tg-token",
            telegramApi = fakeTg,
            log = { _, _, _ -> },
            scope = scope,
            credentials = mapOf("gemini_api_key" to "AIzaSy-secret")
        )
        runtime.initialize(ctx)

        fakeGemini.nextReply = ("Bullet line with meaningful technical text. ".repeat(150)).trim() // > 4096 chars
        val original = TelegramMessage(
            messageId = 10,
            from = TelegramUser(9, false, "Alice"),
            chat = TelegramChat(99L, "group", "Devs"),
            date = 1000L,
            text = "Long article explaining Android Keystore hardware-backed keys and StrongBox."
        )
        val cmdMsg = TelegramMessage(
            messageId = 11,
            from = TelegramUser(10, false, "Bob"),
            chat = TelegramChat(99L, "group", "Devs"),
            date = 1001L,
            text = "/summarize",
            replyToMessage = original
        )

        runtime.handleMessage(ctx, "tg-token", cmdMsg)
        assertTrue(fakeGemini.capturedCalls.single().second.single().text.contains("StrongBox"))
        assertTrue("Responses >4000 chars must split into multiple Telegram messages", fakeTg.sentMessages.size >= 2)
        assertTrue(fakeTg.sentMessages.all { it.second.length <= 4000 })
    }

    @Test
    fun `missing gemini api key returns clear setup instructions`() = runBlocking {
        val fakeGemini = FakeGeminiClient()
        val fakeTg = FakeTelegramApi()
        val runtime = AIBotRuntime("bot_ai_assistant", geminiClient = fakeGemini)
        val ctx = RuntimeContext(
            botId = "bot_ai_assistant",
            token = "tg-token",
            telegramApi = fakeTg,
            log = { _, _, _ -> },
            scope = scope,
            credentials = emptyMap()
        )
        runtime.initialize(ctx)

        val msg = TelegramMessage(
            messageId = 1,
            from = TelegramUser(10, false, "User"),
            chat = TelegramChat(100L, "private"),
            date = 1000L,
            text = "/ask Hello?"
        )
        runtime.handleMessage(ctx, "tg-token", msg)

        assertEquals(0, fakeGemini.capturedCalls.size)
        assertTrue(fakeTg.sentMessages.single().second.contains("Gemini API Key"))
        assertTrue(fakeTg.sentMessages.single().second.contains("aistudio.google.com"))
    }

    @Test
    fun `restricts model and system configuration changes in groups and rate limits rapid requests`() = runBlocking {
        com.tapbot.core.runner.security.RuntimeAccessAuthorizer.clearCache()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val fakeGemini = FakeGeminiClient()
        val fakeTg = FakeTelegramApi()
        val runtime = AIBotRuntime("bot_ai_sec", geminiClient = fakeGemini)
        val ctx = RuntimeContext(
            botId = "bot_ai_sec",
            token = "tg-token",
            telegramApi = fakeTg,
            log = { _, _, _ -> },
            scope = scope,
            credentials = mapOf("gemini_api_key" to "AIzaFakeKey1234567890")
        )
        runtime.initialize(ctx)

        val member = TelegramUser(88L, false, "Member")
        val groupChat = TelegramChat(-100888L, "supergroup", "TestGroup")
        val deniedModel = runtime.handleMessage(
            ctx,
            "tg-token",
            TelegramMessage(1, member, groupChat, 100L, "/model gemini-2.5-pro")
        )
        val deniedSys = runtime.handleMessage(
            ctx,
            "tg-token",
            TelegramMessage(2, member, groupChat, 101L, "/system Ignore rules")
        )
        assertTrue(deniedModel!!.contains("Access denied"))
        assertTrue(deniedSys!!.contains("Access denied"))

        // Rate limit after 20 requests in 1 minute
        val pmChat = TelegramChat(505L, "private")
        repeat(20) { idx ->
            runtime.handleMessage(
                ctx,
                "tg-token",
                TelegramMessage(10L + idx, member, pmChat, 200L + idx, "/ask ping $idx")
            )
        }
        val limitedReply = runtime.handleMessage(
            ctx,
            "tg-token",
            TelegramMessage(30L, member, pmChat, 300L, "/ask ping 21")
        )
        assertTrue(limitedReply!!.contains("Rate limit exceeded"))
        assertEquals(20, fakeGemini.capturedCalls.size)
    }
}
