package com.tapbot.core.runner

import android.content.ComponentName
import android.content.Intent
import com.tapbot.core.logging.InMemoryRingBufferLogRepository
import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.network.AppPreferencesRepository
import com.tapbot.core.runner.manager.BotInstanceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class BootCompletedReceiverTest {

    private lateinit var mockInstanceManager: FakeInstanceManager
    private lateinit var logRepository: InMemoryRingBufferLogRepository
    private lateinit var prefsRepository: AppPreferencesRepository

    @Before
    fun setup() {
        mockInstanceManager = FakeInstanceManager()
        logRepository = InMemoryRingBufferLogRepository()
        prefsRepository = AppPreferencesRepository(storageFile = null)
        ServiceLocator.botInstanceManager = mockInstanceManager
        ServiceLocator.logRepository = logRepository
        ServiceLocator.appPreferencesRepository = prefsRepository
    }

    @After
    fun tearDown() {
        ServiceLocator.botInstanceManager = null
        ServiceLocator.logRepository = null
        ServiceLocator.appPreferencesRepository = null
    }

    @Test
    fun `receiver ignores unrecognized intent actions`() {
        val receiver = BootCompletedReceiver()
        val fakeCtx = FakeContext()
        val intent = Intent("android.intent.action.BATTERY_LOW")
        receiver.onReceive(fakeCtx, intent)
        receiver.handleBootAction(fakeCtx, "android.intent.action.BATTERY_LOW")
        assertEquals(0, fakeCtx.serviceStartCount)
    }

    @Test
    fun `receiver ignores null intent`() {
        val receiver = BootCompletedReceiver()
        val fakeCtx = FakeContext()
        receiver.onReceive(fakeCtx, null)
        assertEquals(0, fakeCtx.serviceStartCount)
    }

    @Test
    fun `boot recovery dispatches all active running bots`() {
        mockInstanceManager.setRunningInstances(
            listOf(
                BotInstance(
                    installationId = "inst_bot_commander",
                    botId = "bot_commander",
                    name = "Commander",
                    version = "1.0.0",
                    status = BotInstanceStatus.Running(pollCount = 12, messageCount = 3)
                ),
                BotInstance(
                    installationId = "inst_bot_music",
                    botId = "bot_music_streamer",
                    name = "TapStream",
                    version = "1.0.0",
                    status = BotInstanceStatus.Stopped
                ),
                BotInstance(
                    installationId = "inst_custom_hook",
                    botId = "custom_hook_1",
                    name = "Deploy Hook",
                    version = "1.0.0",
                    status = BotInstanceStatus.Running(pollCount = 4, messageCount = 1)
                )
            )
        )

        val receiver = BootCompletedReceiver()
        val fakeCtx = FakeContext()
        val recoveredIds = mutableListOf<String>()

        receiver.handleBootAction(fakeCtx, Intent.ACTION_BOOT_COMPLETED) { ids ->
            recoveredIds.addAll(ids)
        }

        assertEquals(listOf("inst_bot_commander", "inst_custom_hook"), recoveredIds)

        // Also test default OS Context dispatch path on MY_PACKAGE_REPLACED
        receiver.handleBootAction(fakeCtx, Intent.ACTION_MY_PACKAGE_REPLACED)
        assertEquals(1, fakeCtx.serviceStartCount)

        val logs = logRepository.getLogStream("bot_commander").value
        assertTrue(logs.any { it.message.contains("Auto-recovering 2 bot(s)") })
    }

    @Test
    fun `boot recovery skips startup when autoStartOnBoot preference is false`() {
        prefsRepository.setAutoStartOnBoot(false)
        mockInstanceManager.setRunningInstances(
            listOf(
                BotInstance(
                    installationId = "inst_bot_commander",
                    botId = "bot_commander",
                    name = "Commander",
                    version = "1.0.0",
                    status = BotInstanceStatus.Running(pollCount = 5, messageCount = 1)
                )
            )
        )

        val receiver = BootCompletedReceiver()
        val fakeCtx = FakeContext()
        val recoveredIds = mutableListOf<String>()

        receiver.handleBootAction(fakeCtx, Intent.ACTION_BOOT_COMPLETED) { ids ->
            recoveredIds.addAll(ids)
        }
        receiver.handleBootAction(fakeCtx, Intent.ACTION_BOOT_COMPLETED)

        assertTrue("Expected zero bots recovered when autoStartOnBoot=false", recoveredIds.isEmpty())
        assertEquals(0, fakeCtx.serviceStartCount)

        val sysLogs = logRepository.getLogStream("system").value
        assertTrue(sysLogs.any { it.message.contains("Auto-start on boot disabled") })
    }

    private class FakeContext : android.content.ContextWrapper(null) {
        var serviceStartCount = 0

        override fun startService(service: Intent?): ComponentName? {
            serviceStartCount++
            return null
        }

        override fun startForegroundService(service: Intent?): ComponentName? {
            serviceStartCount++
            return null
        }
    }

    private class FakeInstanceManager : BotInstanceManager {
        var restartCallCount = 0
        private val _instances = MutableStateFlow<List<BotInstance>>(emptyList())
        override val instances: StateFlow<List<BotInstance>> = _instances
        override val updateProgress: StateFlow<Map<String, com.tapbot.core.model.BotUpdateProgress>> = MutableStateFlow(emptyMap())
        override val activeState: StateFlow<com.tapbot.core.runner.runtime.BotRuntimeState> = MutableStateFlow(com.tapbot.core.runner.runtime.BotRuntimeState.Stopped)

        fun setRunningInstances(list: List<BotInstance>) {
            _instances.value = list
        }

        override suspend fun validateToken(token: String) = Result.success(com.tapbot.core.model.TelegramUser(1L, true, "bot", null, null))
        override suspend fun start() = Result.success(Unit)
        override suspend fun stop() = Result.success(Unit)
        override suspend fun restart(): Result<Unit> {
            restartCallCount++
            return Result.success(Unit)
        }
        override suspend fun install(bot: BotMetadata, packageFile: File) = Result.failure<BotInstance>(NotImplementedError())
        override suspend fun uninstall(installationId: String) = Result.success(Unit)
        override suspend fun start(installationId: String) = Result.success(Unit)
        override suspend fun stop(installationId: String) = Result.success(Unit)
        override suspend fun restart(installationId: String) = Result.success(Unit)
        override suspend fun update(installationId: String, newVersion: String, packageFile: File) = Result.failure<BotInstance>(NotImplementedError())
        override suspend fun updateWithRollback(installationId: String, targetVersion: com.tapbot.core.model.BotVersion, packageDownloader: com.tapbot.core.network.BotPackageDownloader?, manifestValidator: com.tapbot.core.network.ManifestValidator?, onProgress: ((com.tapbot.core.model.BotUpdateProgress) -> Unit)?) = Result.failure<BotInstance>(NotImplementedError())
        override fun getInstance(installationId: String): BotInstance? = null
        override fun getInstanceByBotId(botId: String): BotInstance? = null
        override fun observeInstance(installationId: String) = MutableStateFlow<BotInstance?>(null)
        override fun updateInstanceStatus(installationId: String, status: BotInstanceStatus) {}
        override fun recordCrash(installationId: String, reason: String) {}
    }
}
