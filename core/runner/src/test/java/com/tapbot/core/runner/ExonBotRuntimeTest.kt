package com.tapbot.core.runner

import com.tapbot.core.model.InlineKeyboardMarkup
import com.tapbot.core.model.PromoteChatMemberPayload
import com.tapbot.core.model.RestrictChatPermissions
import com.tapbot.core.model.TelegramCallbackQuery
import com.tapbot.core.model.TelegramChat
import com.tapbot.core.model.TelegramFileRef
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramMessageEntity
import com.tapbot.core.model.TelegramPhotoSize
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.moderation.ExonModerationStore
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.ExonBotRuntime
import com.tapbot.core.runner.runtime.RuntimeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class ExonBotRuntimeTest {

    private data class SentRichMessage(
        val chatId: Long,
        val text: String,
        val replyMarkup: InlineKeyboardMarkup? = null
    )

    private data class EditedMessage(
        val chatId: Long,
        val messageId: Long,
        val text: String,
        val replyMarkup: InlineKeyboardMarkup?
    )

    private class FakeTelegramApiClient : TelegramApiClient() {
        var sentMessages = mutableListOf<Pair<Long, String>>()
        val sentRichMessages = mutableListOf<SentRichMessage>()
        val editedMessages = mutableListOf<EditedMessage>()
        val answeredCallbacks = mutableListOf<Pair<String, String?>>()
        var updateQueue = mutableListOf<List<TelegramUpdate>>()
        val pollCount = AtomicInteger(0)
        var bannedUsers = mutableListOf<Pair<Long, Long>>()
        val timedBans = mutableListOf<Triple<Long, Long, Long?>>()
        val restrictedUsers = mutableListOf<Triple<Long, Long, Long?>>()
        val promotedPayloads = mutableListOf<PromoteChatMemberPayload>()
        val deletedMessages = mutableListOf<Pair<Long, Long>>()

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
                delay(30)
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
            sentRichMessages.add(SentRichMessage(chatId, text, null))
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
            sentRichMessages.add(SentRichMessage(chatId, text, null))
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
            sentMessages.add(chatId to text)
            sentRichMessages.add(SentRichMessage(chatId, text, replyMarkup))
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

        override suspend fun editMessageText(
            token: String,
            chatId: Long,
            messageId: Long,
            text: String,
            parseMode: String?,
            replyMarkup: InlineKeyboardMarkup?
        ): Result<Boolean> {
            editedMessages.add(EditedMessage(chatId, messageId, text, replyMarkup))
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

        override suspend fun banChatMember(
            token: String,
            chatId: Long,
            userId: Long,
            untilDate: Long?
        ): Result<Boolean> {
            bannedUsers.add(chatId to userId)
            timedBans.add(Triple(chatId, userId, untilDate))
            return Result.success(true)
        }

        override suspend fun restrictChatMember(
            token: String,
            chatId: Long,
            userId: Long,
            permissions: RestrictChatPermissions
        ): Result<Boolean> {
            restrictedUsers.add(Triple(chatId, userId, null))
            return Result.success(true)
        }

        override suspend fun restrictChatMember(
            token: String,
            chatId: Long,
            userId: Long,
            permissions: RestrictChatPermissions,
            untilDate: Long?
        ): Result<Boolean> {
            restrictedUsers.add(Triple(chatId, userId, untilDate))
            return Result.success(true)
        }

        override suspend fun promoteChatMember(
            token: String,
            payload: PromoteChatMemberPayload
        ): Result<Boolean> {
            promotedPayloads.add(payload)
            return Result.success(true)
        }

        override suspend fun deleteMessage(
            token: String,
            chatId: Long,
            messageId: Long
        ): Result<Boolean> {
            deletedMessages.add(chatId to messageId)
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
    fun `parseDurationSeconds parses valid units and rejects invalid inputs`() {
        assertEquals(30L, ExonBotRuntime.parseDurationSeconds("30s"))
        assertEquals(600L, ExonBotRuntime.parseDurationSeconds("10m"))
        assertEquals(7200L, ExonBotRuntime.parseDurationSeconds("2h"))
        assertEquals(86400L, ExonBotRuntime.parseDurationSeconds("1d"))
        assertEquals(604800L, ExonBotRuntime.parseDurationSeconds("1W"))
        assertNull(ExonBotRuntime.parseDurationSeconds("0m"))
        assertNull(ExonBotRuntime.parseDurationSeconds("abc"))
        assertNull(ExonBotRuntime.parseDurationSeconds(null))
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
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

        delay(250)

        runtime.stop()
        job.cancel()
        scope.cancel()

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
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

        delay(250)

        runtime.stop()
        job.cancel()
        scope.cancel()

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
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
        scope.cancel()

        assertEquals(3, fakeApi.sentMessages.size)
        assertTrue(fakeApi.sentMessages[0].second.contains("1/3") && fakeApi.sentMessages[0].second.contains("warnings"))
        assertTrue(fakeApi.sentMessages[1].second.contains("2/3") && fakeApi.sentMessages[1].second.contains("warnings"))
        assertTrue(fakeApi.sentMessages[2].second.contains("3/3"))
        assertEquals(1, fakeApi.bannedUsers.size)
        assertEquals(5555L to 888L, fakeApi.bannedUsers.first())
    }

    @Test
    fun `exon bot supports timed mute, timed ban, promote, and demote`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val admin = TelegramUser(id = 1, isBot = false, firstName = "Admin")
        val target = TelegramUser(id = 777, isBot = false, firstName = "Member")
        val chat = TelegramChat(id = -100777, type = "supergroup", title = "Moderators")
        val repliedMsg = TelegramMessage(300, target, chat, 1726740000, "hi")

        fakeApi.updateQueue.add(
            listOf(
                TelegramUpdate(1, TelegramMessage(301, admin, chat, 1726740001, "/tmute 10m", repliedMsg)),
                TelegramUpdate(2, TelegramMessage(302, admin, chat, 1726740002, "/tban 2h", repliedMsg)),
                TelegramUpdate(3, TelegramMessage(303, admin, chat, 1726740003, "/promote", repliedMsg)),
                TelegramUpdate(4, TelegramMessage(304, admin, chat, 1726740004, "/demote", repliedMsg))
            )
        )

        val runtime = ExonBotRuntime("bot_exon")
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        runtime.initialize(RuntimeContext("bot_exon", "token", fakeApi, { _, _, _ -> }, scope))
        runtime.start()
        delay(250)
        runtime.stop()
        scope.cancel()

        assertEquals(1, fakeApi.restrictedUsers.size)
        assertEquals(777L, fakeApi.restrictedUsers[0].second)
        assertNotNull(fakeApi.restrictedUsers[0].third)

        assertEquals(1, fakeApi.timedBans.size)
        assertEquals(777L, fakeApi.timedBans[0].second)
        assertNotNull(fakeApi.timedBans[0].third)

        assertEquals(2, fakeApi.promotedPayloads.size)
        assertTrue(fakeApi.promotedPayloads[0].canManageChat)
        assertTrue(!fakeApi.promotedPayloads[1].canManageChat)
    }

    @Test
    fun `exon bot sends welcome greetings and enforces active locks and antiflood`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val admin = TelegramUser(id = 1, isBot = false, firstName = "Admin")
        val newcomer = TelegramUser(id = 202, isBot = false, firstName = "Bob", username = "bob_dev")
        val spammer = TelegramUser(id = 303, isBot = false, firstName = "Flooder")
        val chat = TelegramChat(id = -100999, type = "supergroup", title = "TapBot Club")

        fakeApi.updateQueue.add(
            listOf(
                // Configure welcome, locks, and antiflood
                TelegramUpdate(1, TelegramMessage(401, admin, chat, 1000L, "/setwelcome Hello {first} in {chat} ({username})!")),
                TelegramUpdate(2, TelegramMessage(402, admin, chat, 1001L, "/lock links")),
                TelegramUpdate(3, TelegramMessage(403, admin, chat, 1002L, "/lock sticker")),
                TelegramUpdate(4, TelegramMessage(404, admin, chat, 1003L, "/lock photo")),
                TelegramUpdate(5, TelegramMessage(405, admin, chat, 1004L, "/antiflood 2")),
                // New member joins
                TelegramUpdate(6, TelegramMessage(406, admin, chat, 1005L, null, newChatMembers = listOf(newcomer))),
                // Violating messages: link, sticker, photo
                TelegramUpdate(
                    7,
                    TelegramMessage(
                        messageId = 407,
                        from = spammer,
                        chat = chat,
                        date = 1006L,
                        text = "visit https://spam.example.com",
                        entities = listOf(TelegramMessageEntity(type = "url", offset = 6, length = 24))
                    )
                ),
                TelegramUpdate(8, TelegramMessage(408, spammer, chat, 1007L, text = null, sticker = TelegramFileRef("stk_1"))),
                TelegramUpdate(9, TelegramMessage(409, spammer, chat, 1008L, text = null, photo = listOf(TelegramPhotoSize("ph_1", width = 100, height = 100)))),
                // Antiflood burst (> 2 messages in 10s from user 404)
                TelegramUpdate(10, TelegramMessage(410, TelegramUser(404, false, "Burst"), chat, 1009L, "msg 1")),
                TelegramUpdate(11, TelegramMessage(411, TelegramUser(404, false, "Burst"), chat, 1010L, "msg 2")),
                TelegramUpdate(12, TelegramMessage(412, TelegramUser(404, false, "Burst"), chat, 1011L, "msg 3"))
            )
        )

        val runtime = ExonBotRuntime("bot_exon")
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        runtime.initialize(RuntimeContext("bot_exon", "token", fakeApi, { _, _, _ -> }, scope))
        runtime.start()
        delay(350)
        runtime.stop()
        scope.cancel()

        assertTrue(fakeApi.sentMessages.any { it.second.contains("Hello Bob in TapBot Club (@bob_dev)!") })
        assertTrue(fakeApi.deletedMessages.contains(-100999L to 407L))
        assertTrue(fakeApi.deletedMessages.contains(-100999L to 408L))
        assertTrue(fakeApi.deletedMessages.contains(-100999L to 409L))
        assertTrue(fakeApi.restrictedUsers.any { it.second == 404L && it.third != null })
        assertTrue(fakeApi.sentMessages.any { it.second.contains("ᴀɴᴛɪғʟᴏᴏᴅ") })
    }

    @Test
    fun `exon bot attaches inline help keyboard and handles callback queries and persists state`() = runBlocking {
        val tempFile = File.createTempFile("exon_runtime_persist", ".json")
        tempFile.delete()
        try {
            val fakeApi = FakeTelegramApiClient()
            val user = TelegramUser(id = 1, isBot = false, firstName = "Admin")
            val chat = TelegramChat(id = -100555, type = "supergroup", title = "Persist Chat")

            fakeApi.updateQueue.add(
                listOf(
                    TelegramUpdate(1, TelegramMessage(501, user, chat, 100L, "/help")),
                    TelegramUpdate(2, TelegramMessage(502, user, chat, 101L, "/setrules No spamming allowed")),
                    TelegramUpdate(
                        3,
                        callbackQuery = TelegramCallbackQuery(
                            id = "cb_exon_1",
                            from = user,
                            message = TelegramMessage(501, user, chat, 100L, "help"),
                            data = "exon:help:admin"
                        )
                    )
                )
            )

            val runtime1 = ExonBotRuntime("bot_exon", ExonModerationStore(tempFile))
            val scope1 = CoroutineScope(Dispatchers.Default + SupervisorJob())
            runtime1.initialize(RuntimeContext("bot_exon", "token", fakeApi, { _, _, _ -> }, scope1))
            runtime1.start()
            delay(250)
            runtime1.stop()
            scope1.cancel()

            val helpMsg = fakeApi.sentRichMessages.firstOrNull { it.replyMarkup != null }
            assertNotNull("/help should attach inline keyboard", helpMsg)
            assertEquals("cb_exon_1", fakeApi.answeredCallbacks.firstOrNull()?.first)
            assertTrue(fakeApi.editedMessages.any { it.text.contains("/promote") })

            // Restart a fresh ExonBotRuntime pointing at the same store file and verify /rules persisted
            val fakeApi2 = FakeTelegramApiClient()
            fakeApi2.updateQueue.add(
                listOf(TelegramUpdate(4, TelegramMessage(503, user, chat, 102L, "/rules")))
            )
            val runtime2 = ExonBotRuntime("bot_exon", ExonModerationStore(tempFile))
            val scope2 = CoroutineScope(Dispatchers.Default + SupervisorJob())
            runtime2.initialize(RuntimeContext("bot_exon", "token", fakeApi2, { _, _, _ -> }, scope2))
            runtime2.start()
            delay(200)
            runtime2.stop()
            scope2.cancel()

            assertTrue(fakeApi2.sentMessages.any { it.second.contains("No spamming allowed") })
        } finally {
            tempFile.delete()
        }
    }
}
