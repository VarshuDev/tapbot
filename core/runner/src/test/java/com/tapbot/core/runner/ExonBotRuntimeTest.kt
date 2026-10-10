package com.tapbot.core.runner

import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramChat
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.BotRuntimeState
import com.tapbot.core.runner.runtime.ExonBotRuntime
import com.tapbot.core.runner.runtime.RuntimeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ExonBotRuntimeTest {

    private class FakeTelegramApiClient : TelegramApiClient() {
        var sentMessages = mutableListOf<Pair<Long, String>>()
        var updateQueue = mutableListOf<List<TelegramUpdate>>()
        val pollCount = AtomicInteger(0)
        var bannedUsers = mutableListOf<Pair<Long, Long>>()

        override suspend fun getMe(token: String): Result<TelegramUser> {
            return Result.success(
                TelegramUser(
                    id = 999888777,
                    isBot = true,
                    firstName = "Exon Robot",
                    username = "ExonRobot"
                )
            )
        }

        override suspend fun getUpdates(
            token: String,
            offset: Long?,
            timeoutSeconds: Int
        ): Result<List<TelegramUpdate>> {
            pollCount.incrementAndGet()
            return if (updateQueue.isNotEmpty()) {
                Result.success(updateQueue.removeAt(0))
            } else {
                delay(50)
                Result.success(emptyList())
            }
        }

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

        override suspend fun banChatMember(
            token: String,
            chatId: Long,
            userId: Long
        ): Result<Boolean> {
            bannedUsers.add(chatId to userId)
            return Result.success(true)
        }
    }

    @Test
    fun `factory creates ExonBotRuntime for bot_exon`() {
        val runtime = BotRuntimeFactory.createRuntime("bot_exon")
        assertTrue(runtime is ExonBotRuntime)
    }

    @Test
    fun `factory creates ExonBotRuntime for published catalog ID and Name`() {
        val runtimeById = BotRuntimeFactory.createRuntime("bot_1790084493920_n4l8lj")
        assertTrue(runtimeById is ExonBotRuntime)

        val runtimeByName = BotRuntimeFactory.createRuntime("random_id", "Exon Robot")
        assertTrue(runtimeByName is ExonBotRuntime)
    }

    @Test
    fun `exon bot handles start command with signature styling`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val update = TelegramUpdate(
            updateId = 1,
            message = TelegramMessage(
                messageId = 101,
                from = TelegramUser(id = 42, isBot = false, firstName = "Abishnoi", username = "Abishnoi1M"),
                chat = TelegramChat(id = 10001, type = "private"),
                date = 1726740000,
                text = "/start"
            )
        )
        fakeApi.updateQueue.add(listOf(update))

        val runtime = ExonBotRuntime("bot_exon")
        val scope = CoroutineScope(Dispatchers.Default)

        val context = RuntimeContext(
            botId = "bot_exon",
            token = "dummy_token_exon",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = scope,
            networkState = kotlinx.coroutines.flow.MutableStateFlow(true)
        )

        runtime.initialize(context)
        val job = scope.launch { runtime.start() }

        // Allow execution tick
        delay(300)

        runtime.stop()
        job.cancel()

        assertTrue(fakeApi.sentMessages.isNotEmpty())
        val reply = fakeApi.sentMessages.first().second
        assertTrue("Start message should contain Exon signature styling", reply.contains("Exon Robot"))
        assertTrue("Start message should mention /help", reply.contains("/help"))
    }

    @Test
    fun `exon bot handles ping command with pong and latency`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val update = TelegramUpdate(
            updateId = 2,
            message = TelegramMessage(
                messageId = 102,
                from = TelegramUser(id = 42, isBot = false, firstName = "Alice"),
                chat = TelegramChat(id = 10001, type = "group", title = "Test Group"),
                date = 1726740000,
                text = "/ping"
            )
        )
        fakeApi.updateQueue.add(listOf(update))

        val runtime = ExonBotRuntime("bot_exon")
        val scope = CoroutineScope(Dispatchers.Default)

        val context = RuntimeContext(
            botId = "bot_exon",
            token = "dummy_token_exon",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = scope,
            networkState = kotlinx.coroutines.flow.MutableStateFlow(true)
        )

        runtime.initialize(context)
        val job = scope.launch { runtime.start() }

        delay(300)

        runtime.stop()
        job.cancel()

        assertTrue(fakeApi.sentMessages.isNotEmpty())
        val reply = fakeApi.sentMessages.first().second
        assertTrue("Ping response should contain PONG 🥀!!", reply.contains("𝗣𝗢𝗡𝗚 🥀!!"))
        assertTrue("Ping response should contain uptime", reply.contains("sᴇʀᴠɪᴄᴇ ᴜᴘᴛɪᴍᴇ"))
    }

    @Test
    fun `exon bot handles warn and auto-bans on 3 warnings`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val targetUser = TelegramUser(id = 888, isBot = false, firstName = "Spammer")
        val repliedMessage = TelegramMessage(
            messageId = 200,
            from = targetUser,
            chat = TelegramChat(id = 5555, type = "supergroup"),
            date = 1726740000,
            text = "Buy crypto"
        )

        val update1 = TelegramUpdate(1, TelegramMessage(201, TelegramUser(1, false, "Admin"), TelegramChat(5555, "supergroup"), 1726740001, "/warn", repliedMessage))
        val update2 = TelegramUpdate(2, TelegramMessage(202, TelegramUser(1, false, "Admin"), TelegramChat(5555, "supergroup"), 1726740002, "/warn", repliedMessage))
        val update3 = TelegramUpdate(3, TelegramMessage(203, TelegramUser(1, false, "Admin"), TelegramChat(5555, "supergroup"), 1726740003, "/warn", repliedMessage))

        fakeApi.updateQueue.add(listOf(update1, update2, update3))

        val runtime = ExonBotRuntime("bot_exon")
        val scope = CoroutineScope(Dispatchers.Default)

        val context = RuntimeContext(
            botId = "bot_exon",
            token = "dummy_token_exon",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = scope,
            networkState = kotlinx.coroutines.flow.MutableStateFlow(true)
        )

        runtime.initialize(context)
        val job = scope.launch { runtime.start() }

        delay(400)

        runtime.stop()
        job.cancel()

        assertEquals(3, fakeApi.sentMessages.size)
        assertTrue(fakeApi.sentMessages[0].second.contains("1/3") && fakeApi.sentMessages[0].second.contains("warnings"))
        assertTrue(fakeApi.sentMessages[1].second.contains("2/3") && fakeApi.sentMessages[1].second.contains("warnings"))
        assertTrue(fakeApi.sentMessages[2].second.contains("3/3"))
        assertEquals(1, fakeApi.bannedUsers.size)
        assertEquals(5555L to 888L, fakeApi.bannedUsers.first())
    }
}
