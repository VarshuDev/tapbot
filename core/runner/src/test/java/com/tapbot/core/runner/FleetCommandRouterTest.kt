package com.tapbot.core.runner

import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotUpdateProgress
import com.tapbot.core.model.BotVersion
import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.BotPackageDownloader
import com.tapbot.core.network.ManifestValidator
import com.tapbot.core.runner.fleet.FleetCommandRouter
import com.tapbot.core.runner.manager.BotInstanceManager
import com.tapbot.core.runner.runtime.BotRuntimeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FleetCommandRouterTest {

    private class TestBotInstanceManager : BotInstanceManager {
        private val _instances = MutableStateFlow<List<BotInstance>>(emptyList())
        override val instances: StateFlow<List<BotInstance>> = _instances
        override val updateProgress: StateFlow<Map<String, BotUpdateProgress>> = MutableStateFlow(emptyMap())
        override val activeState: StateFlow<BotRuntimeState> = MutableStateFlow(BotRuntimeState.Stopped)

        val startedIds = mutableListOf<String>()
        val stoppedIds = mutableListOf<String>()
        val restartedIds = mutableListOf<String>()

        fun setInstances(list: List<BotInstance>) {
            _instances.value = list
        }

        override suspend fun validateToken(token: String): Result<TelegramUser> = Result.success(TelegramUser(1, true, "Bot"))
        override suspend fun start(): Result<Unit> = Result.success(Unit)
        override suspend fun stop(): Result<Unit> = Result.success(Unit)
        override suspend fun restart(): Result<Unit> = Result.success(Unit)

        override suspend fun install(bot: BotMetadata, packageFile: File): Result<BotInstance> =
            Result.failure(NotImplementedError())

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
            Result.failure(NotImplementedError())

        override suspend fun updateWithRollback(
            installationId: String,
            targetVersion: BotVersion,
            packageDownloader: BotPackageDownloader?,
            manifestValidator: ManifestValidator?,
            onProgress: ((BotUpdateProgress) -> Unit)?
        ): Result<BotInstance> = Result.failure(NotImplementedError())

        override fun getInstance(installationId: String): BotInstance? =
            _instances.value.firstOrNull { it.installationId == installationId }

        override fun getInstanceByBotId(botId: String): BotInstance? =
            _instances.value.firstOrNull { it.botId == botId }

        override fun observeInstance(installationId: String): Flow<BotInstance?> = emptyFlow()
        override fun updateInstanceStatus(installationId: String, status: BotInstanceStatus) {}
        override fun recordCrash(installationId: String, reason: String) {}
    }

    private lateinit var fakeManager: TestBotInstanceManager
    private lateinit var router: FleetCommandRouter

    @Before
    fun setUp() {
        fakeManager = TestBotInstanceManager()
        router = FleetCommandRouter(fakeManager)
    }

    @Test
    fun `formatFleetStatus handles empty and populated bot list`() {
        val emptyMsg = router.formatFleetStatus(emptyList())
        assertTrue(emptyMsg.contains("No bots installed yet"))

        val bot1 = BotInstance(
            installationId = "inst_1",
            botId = "bot_echo",
            name = "Echo Bot",
            version = "1.0.0",
            status = BotInstanceStatus.Running(pollCount = 100, messageCount = 12)
        )
        val bot2 = BotInstance(
            installationId = "inst_2",
            botId = "bot_crypto",
            name = "Crypto Bot",
            version = "2.1.0",
            status = BotInstanceStatus.Crashed(reason = "OOM killer")
        )

        val populatedMsg = router.formatFleetStatus(listOf(bot1, bot2))
        assertTrue(populatedMsg.contains("TapBot Installed Fleet (2)"))
        assertTrue(populatedMsg.contains("Echo Bot"))
        assertTrue(populatedMsg.contains("🟢 RUNNING"))
        assertTrue(populatedMsg.contains("Polls: `100`"))
        assertTrue(populatedMsg.contains("Crypto Bot"))
        assertTrue(populatedMsg.contains("🔴 CRASHED"))
        assertTrue(populatedMsg.contains("OOM killer"))
    }

    @Test
    fun `startBot, stopBot, and restartBot dispatch correctly by botId`() = runBlocking {
        val bot = BotInstance(
            installationId = "inst_alpha",
            botId = "bot_alpha",
            name = "Alpha",
            version = "1.0.0"
        )
        fakeManager.setInstances(listOf(bot))

        val startResult = router.startBot("bot_alpha")
        assertTrue(startResult.isSuccess)
        assertTrue(fakeManager.startedIds.contains("inst_alpha"))

        val stopResult = router.stopBot("bot_alpha")
        assertTrue(stopResult.isSuccess)
        assertTrue(fakeManager.stoppedIds.contains("inst_alpha"))

        val restartResult = router.restartBot("bot_alpha")
        assertTrue(restartResult.isSuccess)
        assertTrue(fakeManager.restartedIds.contains("inst_alpha"))

        // Non-existent bot
        val missingResult = router.startBot("non_existent_bot")
        assertFalse(missingResult.isSuccess)
    }

    @Test
    fun `formatLogs sanitizes bot tokens and limits line count`() {
        val entries = listOf(
            BotLogEntry(
                botId = "bot_test",
                timestamp = 1700000000000L,
                level = LogLevel.INFO,
                tag = "Network",
                message = "Connecting to Telegram using token 123456789:ABCdefGHIjklMNOpqrSTUvwxYZ123456789 to fetch updates"
            ),
            BotLogEntry(
                botId = "bot_test",
                timestamp = 1700000001000L,
                level = LogLevel.DEBUG,
                tag = "Poller",
                message = "Received 2 new messages"
            )
        )

        val logOutput = router.formatLogs("bot_test", entries, lineCount = 10)
        assertTrue(logOutput.contains("Live Logs for `bot\\_test`"))
        assertTrue(logOutput.contains("[REDACTED_BOT_TOKEN]"))
        assertFalse(logOutput.contains("123456789:ABCdefGHIjklMNOpqrSTUvwxYZ123456789"))
        assertTrue(logOutput.contains("Received 2 new messages"))

        val emptyOutput = router.formatLogs("bot_test", emptyList())
        assertTrue(emptyOutput.contains("No recent log entries"))
    }
}
