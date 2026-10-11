package com.tapbot.core.runner

import android.content.Context
import android.content.Intent
import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotPackageInfo
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

    @Before
    fun setup() {
        mockInstanceManager = FakeInstanceManager()
        ServiceLocator.botInstanceManager = mockInstanceManager
    }

    @After
    fun tearDown() {
        ServiceLocator.botInstanceManager = null
    }

    @Test
    fun `receiver ignores unrecognized intent actions`() {
        val receiver = BootCompletedReceiver()
        val intent = Intent("android.intent.action.BATTERY_LOW")
        // Should return cleanly without throwing
        receiver.onReceive(FakeContext(), intent)
        assertEquals(0, mockInstanceManager.restartCallCount)
    }

    @Test
    fun `receiver ignores null intent`() {
        val receiver = BootCompletedReceiver()
        receiver.onReceive(FakeContext(), null)
        assertEquals(0, mockInstanceManager.restartCallCount)
    }

    private class FakeContext : android.content.ContextWrapper(null)

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
