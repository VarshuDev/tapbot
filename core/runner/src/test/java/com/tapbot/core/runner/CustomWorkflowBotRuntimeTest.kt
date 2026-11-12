package com.tapbot.core.runner

import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.core.model.InlineKeyboardMarkup
import com.tapbot.core.model.TelegramCallbackQuery
import com.tapbot.core.model.TelegramChat
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.CustomWorkflowBotRuntime
import com.tapbot.core.runner.runtime.RuntimeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomWorkflowBotRuntimeTest {

    private data class RecordedReply(
        val chatId: Long,
        val text: String,
        val markup: InlineKeyboardMarkup? = null
    )

    private class FakeTelegramApiClient : TelegramApiClient() {
        val replies = mutableListOf<RecordedReply>()
        val answeredCallbacks = mutableListOf<Pair<String, String?>>()
        val updatesQueue = mutableListOf<List<TelegramUpdate>>()

        override suspend fun getMe(token: String): Result<TelegramUser> {
            return Result.success(TelegramUser(id = 777L, isBot = true, firstName = "WorkflowBot", username = "workflow_bot"))
        }

        override suspend fun getUpdates(
            token: String,
            offset: Long?,
            timeoutSeconds: Int
        ): Result<List<TelegramUpdate>> {
            return if (updatesQueue.isNotEmpty()) {
                Result.success(updatesQueue.removeAt(0))
            } else {
                delay(25)
                Result.success(emptyList())
            }
        }

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?,
            replyToMessageId: Long?
        ): Result<Boolean> {
            replies.add(RecordedReply(chatId, text, null))
            return Result.success(true)
        }

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?,
            replyToMessageId: Long?,
            replyMarkup: InlineKeyboardMarkup?
        ): Result<Boolean> {
            replies.add(RecordedReply(chatId, text, replyMarkup))
            return Result.success(true)
        }

        override suspend fun answerCallbackQuery(
            token: String,
            callbackQueryId: String,
            text: String?,
            showAlert: Boolean
        ): Result<Boolean> {
            answeredCallbacks.add(callbackQueryId to text)
            return Result.success(true)
        }
    }

    @After
    fun tearDown() {
        ServiceLocator.customBotRepository = null
    }

    @Test
    fun `factory routes custom bot IDs to CustomWorkflowBotRuntime`() {
        val customRepo = CustomBotRepository(storageFile = null).apply {
            saveDefinition(
                CustomBotDefinition(
                    id = "custom_music_alert",
                    name = "Music Alert Custom Bot",
                    summary = "Custom workflow even with music keyword",
                    rules = listOf(CommandRule(id = "r1", command = "/ping", responseTemplate = "pong"))
                )
            )
        }
        ServiceLocator.customBotRepository = customRepo

        assertTrue(BotRuntimeFactory.createRuntime("custom_music_alert", "Music Alert Custom Bot") is CustomWorkflowBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("bot_custom_12345") is CustomWorkflowBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("my_custom_workflow") is CustomWorkflowBotRuntime)
    }

    @Test
    fun `runtime executes rules variables GET and POST webhooks and inline button callbacks`() = runBlocking {
        val capturedRequests = mutableListOf<Triple<String, String, String?>>()
        val fakeHttpClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val bodyText = req.body?.let { body ->
                    val buf = Buffer()
                    body.writeTo(buf)
                    buf.readUtf8()
                }
                capturedRequests.add(Triple(req.method, req.url.toString(), bodyText))

                val responseText = if (req.method == "POST") {
                    "DEPLOYED_OK"
                } else {
                    "WEATHER_SUNNY"
                }
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseText.toResponseBody("text/plain".toMediaType()))
                    .build()
            })
            .build()

        val definition = CustomBotDefinition(
            id = "custom_wf_1",
            name = "Automation Buddy",
            summary = "Test custom bot",
            welcomeMessage = "Welcome {user} ({username}) to {chat_id}!",
            rules = listOf(
                CommandRule(
                    id = "r_greet",
                    command = "/greet",
                    description = "Greet args",
                    responseTemplate = "Hello {user} [id={user_id}] on {date} -> args={args}",
                    buttonLabel = "Refresh Greeting"
                ),
                CommandRule(
                    id = "r_weather",
                    command = "/weather",
                    description = "Fetch GET hook",
                    responseTemplate = "Weather for {args}: {webhook_response}",
                    webhookUrl = "https://api.example.com/weather?city={args}&uid={user_id}",
                    webhookMethod = CustomHttpMethod.GET
                ),
                CommandRule(
                    id = "r_deploy",
                    command = "/deploy",
                    description = "Trigger POST hook",
                    responseTemplate = "Deploy {args}: {webhook_response}",
                    webhookUrl = "https://hooks.example.com/deploy",
                    webhookMethod = CustomHttpMethod.POST,
                    webhookBodyTemplate = "{\"env\":\"{args}\",\"by\":\"{user_id}\"}"
                )
            )
        )

        val tgApi = FakeTelegramApiClient()
        val caller = TelegramUser(id = 4242L, isBot = false, firstName = "Varshan", username = "varshan_dev")
        val chat = TelegramChat(id = 9001L, type = "private")

        tgApi.updatesQueue.add(
            listOf(
                TelegramUpdate(
                    updateId = 1L,
                    message = TelegramMessage(10L, caller, chat, 1000L, "/start")
                ),
                TelegramUpdate(
                    updateId = 2L,
                    message = TelegramMessage(11L, caller, chat, 1001L, "/help")
                ),
                TelegramUpdate(
                    updateId = 3L,
                    message = TelegramMessage(12L, caller, chat, 1002L, "/greet good morning")
                ),
                TelegramUpdate(
                    updateId = 4L,
                    message = TelegramMessage(13L, caller, chat, 1003L, "/weather New York")
                ),
                TelegramUpdate(
                    updateId = 5L,
                    message = TelegramMessage(14L, caller, chat, 1004L, "/deploy production")
                ),
                TelegramUpdate(
                    updateId = 6L,
                    callbackQuery = TelegramCallbackQuery(
                        id = "cb_99",
                        from = caller,
                        message = TelegramMessage(12L, caller, chat, 1002L, "/greet"),
                        data = "cw:btn:r_greet"
                    )
                )
            )
        )

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val runtime = CustomWorkflowBotRuntime(
            botId = "custom_wf_1",
            definitionOverride = definition,
            httpClient = fakeHttpClient,
            dateProvider = { "2026-11-10 12:00:00 UTC" }
        )

        runtime.initialize(
            RuntimeContext(
                botId = "custom_wf_1",
                token = "123456:TESTTOKEN",
                telegramApi = tgApi,
                log = { _, _, _ -> },
                scope = scope
            )
        )
        runtime.start()

        var waited = 0
        while (tgApi.replies.size < 6 && waited < 3000) {
            delay(40)
            waited += 40
        }
        runtime.stop()
        scope.cancel()

        assertEquals(6, tgApi.replies.size)

        // 1. /start
        assertTrue(tgApi.replies[0].text.contains("Welcome Varshan (@varshan_dev) to 9001!"))
        assertTrue(tgApi.replies[0].text.contains("/greet — Greet args"))

        // 2. /help
        assertTrue(tgApi.replies[1].text.contains("Automation Buddy — Commands:"))
        assertTrue(tgApi.replies[1].text.contains("/deploy — Trigger POST hook"))

        // 3. /greet with inline button
        assertEquals("Hello Varshan [id=4242] on 2026-11-10 12:00:00 UTC -> args=good morning", tgApi.replies[2].text)
        assertNotNull(tgApi.replies[2].markup)
        assertEquals("Refresh Greeting", tgApi.replies[2].markup?.inlineKeyboard?.first()?.first()?.text)

        // 4. /weather GET webhook
        assertEquals("Weather for New York: WEATHER_SUNNY", tgApi.replies[3].text)
        assertEquals("GET", capturedRequests[0].first)
        assertEquals("https://api.example.com/weather?city=New%20York&uid=4242", capturedRequests[0].second)

        // 5. /deploy POST webhook
        assertEquals("Deploy production: DEPLOYED_OK", tgApi.replies[4].text)
        assertEquals("POST", capturedRequests[1].first)
        assertEquals("{\"env\":\"production\",\"by\":\"4242\"}", capturedRequests[1].third)

        // 6. Inline button callback execution
        assertEquals(1, tgApi.answeredCallbacks.size)
        assertEquals("cb_99", tgApi.answeredCallbacks[0].first)
        assertTrue(tgApi.replies[5].text.contains("Hello Varshan [id=4242]"))
    }

    @Test
    fun `blocks SSRF metadata webhook URLs and escapes JSON body template variables`() = runBlocking {
        val capturedBodies = mutableListOf<String>()
        var networkCalls = 0
        val fakeClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                networkCalls++
                val req = chain.request()
                val buf = Buffer()
                req.body?.writeTo(buf)
                capturedBodies.add(buf.readUtf8())
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("OK".toResponseBody("text/plain".toMediaType()))
                    .build()
            }
            .build()

        val definition = CustomBotDefinition(
            id = "custom_sec",
            name = "SecBot",
            summary = "Security test",
            rules = listOf(
                CommandRule(
                    id = "r_ssrf",
                    command = "/ssrf",
                    responseTemplate = "Result: {webhook_response}",
                    webhookUrl = "http://169.254.169.254/latest/meta-data/",
                    webhookMethod = CustomHttpMethod.GET
                ),
                CommandRule(
                    id = "r_json",
                    command = "/post",
                    responseTemplate = "Posted: {webhook_response}",
                    webhookUrl = "https://hooks.example.com/ingest",
                    webhookMethod = CustomHttpMethod.POST,
                    webhookBodyTemplate = """{"user":"{user}","args":"{args}"}"""
                )
            )
        )

        val tgApi = FakeTelegramApiClient()
        val user = TelegramUser(id = 77L, isBot = false, firstName = "A\"b")
        val chat = TelegramChat(id = 88L, type = "private")
        tgApi.updatesQueue.add(
            listOf(
                TelegramUpdate(1L, TelegramMessage(1L, user, chat, 100L, "/ssrf")),
                TelegramUpdate(2L, TelegramMessage(2L, user, chat, 101L, "/post hello \",\"injected\":true"))
            )
        )

        val runtime = CustomWorkflowBotRuntime(
            botId = "custom_sec",
            definitionOverride = definition,
            httpClient = fakeClient
        )
        runtime.initialize(RuntimeContext("custom_sec", "TOKEN", tgApi, { _, _, _ -> }, this))
        runtime.start()
        delay(200)
        runtime.stop()

        assertTrue(tgApi.replies.any { it.text.contains("Blocked webhook URL") })
        assertEquals(1, networkCalls)
        assertEquals(1, capturedBodies.size)
        assertEquals(
            """{"user":"A\"b","args":"hello \",\"injected\":true"}""",
            capturedBodies.single()
        )
    }
}
