package com.tapbot.core.runner

import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramChat
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.manager.BotInstanceManager
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.BotRuntimeState
import com.tapbot.core.runner.runtime.CommanderBotRuntime
import com.tapbot.core.runner.runtime.RuntimeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class CommanderBotRuntimeTest {

    private class FakeTelegramApiClient : TelegramApiClient() {
        val sentMessages = mutableListOf<Triple<Long, String, String?>>()
        val sentDocuments = mutableListOf<Pair<Long, String>>()
        val sentChatActions = mutableListOf<Pair<Long, String>>()
        val updateQueue = mutableListOf<List<TelegramUpdate>>()
        val pollCount = AtomicInteger(0)

        override suspend fun getMe(token: String): Result<TelegramUser> {
            return Result.success(
                TelegramUser(
                    id = 111222333,
                    isBot = true,
                    firstName = "TapBot Commander",
                    username = "TapBotCommanderBot"
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
            sentMessages.add(Triple(chatId, text, parseMode))
            return Result.success(true)
        }

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?,
            replyToMessageId: Long?
        ): Result<Boolean> {
            sentMessages.add(Triple(chatId, text, parseMode))
            return Result.success(true)
        }

        override suspend fun sendDocument(
            token: String,
            chatId: Long,
            file: File,
            caption: String?,
            parseMode: String?
        ): Result<Boolean> {
            sentDocuments.add(chatId to (caption ?: file.name))
            return Result.success(true)
        }

        override suspend fun sendChatAction(
            token: String,
            chatId: Long,
            action: String
        ): Result<Boolean> {
            sentChatActions.add(chatId to action)
            return Result.success(true)
        }
    }

    private class FakeBotInstanceManager : BotInstanceManager {
        private val _instances = MutableStateFlow<List<BotInstance>>(emptyList())
        override val instances: StateFlow<List<BotInstance>> = _instances.asStateFlow()
        override val updateProgress: StateFlow<Map<String, com.tapbot.core.model.BotUpdateProgress>> = MutableStateFlow(emptyMap())
        override val activeState: StateFlow<BotRuntimeState> = MutableStateFlow(BotRuntimeState.Stopped)

        val startedIds = mutableListOf<String>()
        val stoppedIds = mutableListOf<String>()
        val restartedIds = mutableListOf<String>()

        fun setTestInstances(list: List<BotInstance>) {
            _instances.value = list
        }

        override suspend fun validateToken(token: String): Result<TelegramUser> = Result.success(TelegramUser(1, true, "Bot", "bot"))
        override suspend fun start(): Result<Unit> = Result.success(Unit)
        override suspend fun stop(): Result<Unit> = Result.success(Unit)
        override suspend fun restart(): Result<Unit> = Result.success(Unit)

        override suspend fun install(bot: com.tapbot.core.model.BotMetadata, packageFile: File): Result<BotInstance> =
            throw UnsupportedOperationException()
        override suspend fun uninstall(installationId: String): Result<Unit> = Result.success(Unit)

        override suspend fun start(installationId: String): Result<Unit> {
            startedIds.add(installationId)
            return Result.success(Unit)
        }

        override suspend fun stop(installationId: String): Result<Unit> {
            stoppedIds.add(installationId)
            return Result.success(Unit)
        }

        override suspend fun restart(installationId: String): Result<Unit> {
            restartedIds.add(installationId)
            return Result.success(Unit)
        }

        override suspend fun update(installationId: String, newVersion: String, packageFile: File): Result<BotInstance> =
            throw UnsupportedOperationException()
        override suspend fun updateWithRollback(
            installationId: String,
            targetVersion: com.tapbot.core.model.BotVersion,
            packageDownloader: com.tapbot.core.network.BotPackageDownloader?,
            manifestValidator: com.tapbot.core.network.ManifestValidator?,
            onProgress: ((com.tapbot.core.model.BotUpdateProgress) -> Unit)?
        ): Result<BotInstance> = throw UnsupportedOperationException()

        override fun getInstance(installationId: String): BotInstance? =
            _instances.value.firstOrNull { it.installationId == installationId }

        override fun getInstanceByBotId(botId: String): BotInstance? =
            _instances.value.firstOrNull { it.botId == botId }

        override fun observeInstance(installationId: String) = kotlinx.coroutines.flow.flowOf(getInstance(installationId))
        override fun updateInstanceStatus(installationId: String, status: BotInstanceStatus) {}
        override fun recordCrash(installationId: String, reason: String) {}
    }

    @Test
    fun `factory creates CommanderBotRuntime for bot_commander and keywords`() {
        val runtime1 = BotRuntimeFactory.createRuntime("bot_commander")
        assertTrue(runtime1 is CommanderBotRuntime)

        val runtime2 = BotRuntimeFactory.createRuntime("tapbot_sentinel", "TapBot Fleet Commander")
        assertTrue(runtime2 is CommanderBotRuntime)

        val runtime3 = BotRuntimeFactory.createRuntime("custom_id", "Android Device Controller")
        assertTrue(runtime3 is CommanderBotRuntime)
    }

    @Test
    fun `lifecycle initializes, authenticates, and reaches Running state`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val runtime = CommanderBotRuntime("bot_commander")

        val ctx = RuntimeContext(
            botId = "bot_commander",
            token = "fake_test_token_12345",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = this
        )

        runtime.initialize(ctx)
        runtime.start()

        delay(50)
        assertTrue(runtime.state.value is BotRuntimeState.Running)
        assertEquals("TapBotCommanderBot", (runtime.state.value as BotRuntimeState.Running).botUsername)

        runtime.stop()
        delay(20)
        assertTrue(runtime.state.value is BotRuntimeState.Stopped)
    }

    @Test
    fun `responds to start, help, and about commands`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val runtime = CommanderBotRuntime("bot_commander")

        fakeApi.updateQueue.add(
            listOf(
                TelegramUpdate(
                    updateId = 101,
                    message = TelegramMessage(
                        messageId = 1,
                        from = TelegramUser(id = 777, isBot = false, firstName = "Alex", username = "alex"),
                        chat = TelegramChat(id = 99999, type = "private"),
                        date = 1700000000L,
                        text = "/start"
                    )
                ),
                TelegramUpdate(
                    updateId = 102,
                    message = TelegramMessage(
                        messageId = 2,
                        from = TelegramUser(id = 777, isBot = false, firstName = "Alex", username = "alex"),
                        chat = TelegramChat(id = 99999, type = "private"),
                        date = 1700000000L,
                        text = "/help"
                    )
                ),
                TelegramUpdate(
                    updateId = 103,
                    message = TelegramMessage(
                        messageId = 3,
                        from = TelegramUser(id = 777, isBot = false, firstName = "Alex", username = "alex"),
                        chat = TelegramChat(id = 99999, type = "private"),
                        date = 1700000000L,
                        text = "/about"
                    )
                )
            )
        )

        val ctx = RuntimeContext(
            botId = "bot_commander",
            token = "fake_token",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = this
        )

        runtime.initialize(ctx)
        runtime.start()
        delay(150)
        runtime.stop()

        assertTrue(fakeApi.sentMessages.size >= 3)
        assertTrue(fakeApi.sentMessages.any { it.second.contains("Welcome to TapBot Commander") })
        assertTrue(fakeApi.sentMessages.any { it.second.contains("TapBot Commander — Command Menu") })
        assertTrue(fakeApi.sentMessages.any { it.second.contains("Decentralized on-device Telegram bot runner") || it.second.contains("About TapBot") })
    }

    @Test
    fun `enforces owner security for sensitive commands`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val runtime = CommanderBotRuntime("bot_commander")

        // Configure owner_id as 999 (Authorized)
        val ctx = RuntimeContext(
            botId = "bot_commander",
            token = "fake_token",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = this,
            credentials = mapOf("owner_id" to "999")
        )

        // Attacker with ID 666 tries sensitive command /status and /ring
        fakeApi.updateQueue.add(
            listOf(
                TelegramUpdate(
                    updateId = 201,
                    message = TelegramMessage(
                        messageId = 10,
                        from = TelegramUser(id = 666, isBot = false, firstName = "Hacker", username = "hacker"),
                        chat = TelegramChat(id = 8888, type = "private"),
                        date = 1700000000L,
                        text = "/status"
                    )
                ),
                TelegramUpdate(
                    updateId = 202,
                    message = TelegramMessage(
                        messageId = 11,
                        from = TelegramUser(id = 666, isBot = false, firstName = "Hacker", username = "hacker"),
                        chat = TelegramChat(id = 8888, type = "private"),
                        date = 1700000000L,
                        text = "/ring"
                    )
                )
            )
        )

        runtime.initialize(ctx)
        runtime.start()
        delay(120)
        runtime.stop()

        assertTrue(fakeApi.sentMessages.size >= 2)
        assertTrue(fakeApi.sentMessages.all { it.second.contains("Access Denied") })
    }

    @Test
    fun `authorized owner executes telemetry and fleet commands`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val fakeManager = FakeBotInstanceManager()
        ServiceLocator.botInstanceManager = fakeManager

        val testBot = BotInstance(
            installationId = "inst_exon_1",
            botId = "bot_exon",
            name = "Exon Robot",
            version = "1.0.0",
            status = BotInstanceStatus.Running(pollCount = 42, messageCount = 5)
        )
        fakeManager.setTestInstances(listOf(testBot))

        val runtime = CommanderBotRuntime("bot_commander")

        val ctx = RuntimeContext(
            botId = "bot_commander",
            token = "fake_token",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = this,
            credentials = mapOf("owner_id" to "12345")
        )

        fakeApi.updateQueue.add(
            listOf(
                TelegramUpdate(
                    updateId = 301,
                    message = TelegramMessage(
                        messageId = 20,
                        from = TelegramUser(id = 12345, isBot = false, firstName = "Owner", username = "owner"),
                        chat = TelegramChat(id = 5555, type = "private"),
                        date = 1700000000L,
                        text = "/status"
                    )
                ),
                TelegramUpdate(
                    updateId = 302,
                    message = TelegramMessage(
                        messageId = 21,
                        from = TelegramUser(id = 12345, isBot = false, firstName = "Owner", username = "owner"),
                        chat = TelegramChat(id = 5555, type = "private"),
                        date = 1700000000L,
                        text = "/bots"
                    )
                ),
                TelegramUpdate(
                    updateId = 303,
                    message = TelegramMessage(
                        messageId = 22,
                        from = TelegramUser(id = 12345, isBot = false, firstName = "Owner", username = "owner"),
                        chat = TelegramChat(id = 5555, type = "private"),
                        date = 1700000000L,
                        text = "/stopbot bot_exon"
                    )
                ),
                TelegramUpdate(
                    updateId = 304,
                    message = TelegramMessage(
                        messageId = 23,
                        from = TelegramUser(id = 12345, isBot = false, firstName = "Owner", username = "owner"),
                        chat = TelegramChat(id = 5555, type = "private"),
                        date = 1700000000L,
                        text = "/startbot bot_exon"
                    )
                )
            )
        )

        runtime.initialize(ctx)
        runtime.start()
        delay(300)
        runtime.stop()

        assertTrue(fakeApi.sentMessages.any { it.second.contains("TapBot Host System Dashboard") })
        assertTrue(fakeApi.sentMessages.any { it.second.contains("TapBot Installed Fleet") && it.second.contains("Exon Robot") })
        assertTrue(fakeManager.stoppedIds.contains("inst_exon_1"))
        assertTrue(fakeManager.startedIds.contains("inst_exon_1"))
    }

    @Test
    fun `handles apk delivery and store search`() = runBlocking {
        val fakeApi = FakeTelegramApiClient()
        val runtime = CommanderBotRuntime("bot_commander")

        fakeApi.updateQueue.add(
            listOf(
                TelegramUpdate(
                    updateId = 401,
                    message = TelegramMessage(
                        messageId = 30,
                        from = TelegramUser(id = 100, isBot = false, firstName = "User", username = "user"),
                        chat = TelegramChat(id = 4444, type = "private"),
                        date = 1700000000L,
                        text = "/apk"
                    )
                ),
                TelegramUpdate(
                    updateId = 402,
                    message = TelegramMessage(
                        messageId = 31,
                        from = TelegramUser(id = 100, isBot = false, firstName = "User", username = "user"),
                        chat = TelegramChat(id = 4444, type = "private"),
                        date = 1700000000L,
                        text = "/search exon"
                    )
                ),
                TelegramUpdate(
                    updateId = 403,
                    message = TelegramMessage(
                        messageId = 32,
                        from = TelegramUser(id = 100, isBot = false, firstName = "User", username = "user"),
                        chat = TelegramChat(id = 4444, type = "private"),
                        date = 1700000000L,
                        text = "/batteryguide"
                    )
                )
            )
        )

        val ctx = RuntimeContext(
            botId = "bot_commander",
            token = "fake_token",
            telegramApi = fakeApi,
            log = { _, _, _ -> },
            scope = this
        )

        runtime.initialize(ctx)
        runtime.start()
        delay(150)
        runtime.stop()

        assertTrue(fakeApi.sentMessages.any { it.second.contains("TapBot APK") || it.second.contains("TapBot APK Information") })
        assertTrue(fakeApi.sentMessages.any { it.second.contains("Exon Robot") && it.second.contains("Search Results") })
        assertTrue(fakeApi.sentMessages.any { it.second.contains("Samsung One UI") && it.second.contains("Never sleeping apps") })
    }
}
