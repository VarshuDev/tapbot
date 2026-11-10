package com.tapbot.feature.botdetail

import com.tapbot.core.logging.BotLogRepository
import com.tapbot.core.model.BotCredentialSpec
import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotPackageInfo
import com.tapbot.core.model.BotRunState
import com.tapbot.core.model.LogLevel
import com.tapbot.core.network.BotPackageDownloader
import com.tapbot.core.network.CatalogRepository
import com.tapbot.core.network.DefaultLocalBotInstallationManager
import com.tapbot.core.network.InstalledBotRecord
import com.tapbot.core.network.LocalBotInstallationManager
import com.tapbot.core.runner.BotServiceController
import com.tapbot.core.security.SecureCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class BotInstallFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleBot = BotMetadata(
        id = "ai-summarizer-bot",
        name = "AI Summarizer Bot",
        summary = "Summarizes long channel posts locally.",
        description = "Runs local processing on Android to summarize messages.",
        author = "TapBot Publishing",
        version = "2.1.0",
        iconUrl = "https://cdn.tapbot.dev/summarizer.png",
        category = "Productivity",
        tags = listOf("productivity", "ai"),
        requiredCredentials = listOf(
            BotCredentialSpec.TELEGRAM_BOT_TOKEN,
            BotCredentialSpec(
                key = "api_key",
                label = "Summarizer API Key",
                description = "Key for external processing",
                isSecret = true,
                isRequired = true
            )
        ),
        packageInfo = BotPackageInfo(
            packageUrl = "https://r2.tapbot.dev/packages/summarizer_2.1.0.botpkg",
            sha256Checksum = "feedbeefcafebabedeadbeefcafebabefeedbeefcafebabedeadbeefcafebabe",
            runtimeType = "native_art",
            packageSizeBytes = 8192L
        ),
        permissionsRequired = listOf("INTERNET"),
        releaseNotes = "Added multi-language support",
        minimumAppVersion = 1,
        minimumRuntimeVersion = "1.0.0",
        updatedAt = "2026-09-19T00:00:00Z",
        isFeatured = false
    )

    private val fakeCatalogRepo = object : CatalogRepository {
        override suspend fun getBots(forceRefresh: Boolean): Result<List<BotMetadata>> =
            Result.success(listOf(sampleBot))
        override suspend fun getFeaturedBots(): Result<List<BotMetadata>> =
            Result.success(listOf(sampleBot))
        override suspend fun getCategories(): Result<List<String>> =
            Result.success(listOf("All", "Productivity"))
        override suspend fun getBotDetails(botId: String, forceRefresh: Boolean): Result<BotMetadata> =
            if (botId == sampleBot.id) Result.success(sampleBot) else Result.failure(NoSuchElementException(botId))
        override suspend fun getBotVersions(botId: String): Result<List<com.tapbot.core.model.BotVersion>> = Result.success(emptyList())
        override suspend fun getLatestVersion(botId: String): Result<com.tapbot.core.model.BotVersion?> = Result.success(null)
        override suspend fun checkForUpdate(botId: String, currentVersion: String): Result<com.tapbot.core.model.BotVersion?> = Result.success(null)
    }

    private val fakeCredentialStore = object : SecureCredentialStore {
        val storage = mutableMapOf<String, MutableMap<String, String>>()
        override suspend fun saveCredential(botId: String, key: String, secretValue: String) {
            storage.getOrPut(botId) { mutableMapOf() }[key] = secretValue
        }
        override suspend fun getCredential(botId: String, key: String): String? = storage[botId]?.get(key)
        override suspend fun deleteCredential(botId: String, key: String) { storage[botId]?.remove(key) }
        override suspend fun hasCredential(botId: String, key: String): Boolean = storage[botId]?.containsKey(key) == true
        override suspend fun getAllCredentials(botId: String): Map<String, String> = storage[botId]?.toMap() ?: emptyMap()
        override suspend fun deleteCredentials(botId: String) { storage.remove(botId) }
        override suspend fun hasRequiredCredentials(botId: String, requiredKeys: List<String>): Boolean =
            requiredKeys.all { storage[botId]?.containsKey(it) == true }
    }

    private val fakeController = object : BotServiceController {
        var startCount = 0
        var stopCount = 0
        private val state = MutableStateFlow<BotRunState>(BotRunState.Stopped)
        override fun startBot(botId: String) {
            startCount++
            state.value = BotRunState.Running(1, 0)
        }
        override fun stopBot(botId: String) {
            stopCount++
            state.value = BotRunState.Stopped
        }
        override fun stopAll() { state.value = BotRunState.Stopped }
        override fun getBotRunState(botId: String): StateFlow<BotRunState> = state.asStateFlow()
    }

    private val fakeLogRepo = object : BotLogRepository {
        private val logs = MutableStateFlow<List<BotLogEntry>>(emptyList())
        override fun appendLog(botId: String, level: LogLevel, tag: String, message: String) {}
        override fun getLogStream(botId: String): StateFlow<List<BotLogEntry>> = logs.asStateFlow()
        override fun clearLogs(botId: String) {}
    }

    private class FakeDownloader(
        var failChecksum: Boolean = false
    ) : BotPackageDownloader {
        var downloadCalls = 0
        override suspend fun downloadPackage(
            packageUrl: String,
            expectedSha256: String,
            targetFile: File,
            onProgress: (Float) -> Unit
        ): Result<File> {
            downloadCalls++
            onProgress(0.25f)
            onProgress(0.75f)
            onProgress(1.0f)
            return if (failChecksum) {
                Result.failure(SecurityException("SHA-256 verification failed"))
            } else {
                targetFile.writeText("VALID_PACKAGE_PAYLOAD")
                Result.success(targetFile)
            }
        }
    }

    private class FakeInstallationManager : LocalBotInstallationManager {
        val installedBots = mutableMapOf<String, String>() // botId -> version
        var installCalls = 0

        override fun isInstalled(botId: String): Boolean = installedBots.containsKey(botId)
        override fun getInstalledVersion(botId: String): String? = installedBots[botId]
        override fun installBot(bot: BotMetadata, packageFile: File): Result<Unit> {
            installCalls++
            installedBots[bot.id] = bot.version
            return Result.success(Unit)
        }
        override fun uninstallBot(botId: String): Result<Unit> {
            installedBots.remove(botId)
            return Result.success(Unit)
        }
        override fun getInstalledBots(): List<InstalledBotRecord> = emptyList()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun installFlow_endToEnd_downloadsVerifiesAndShowsConfigurationWithoutAutoStarting() = runTest(testDispatcher) {
        val downloader = FakeDownloader()
        val installer = FakeInstallationManager()

        val viewModel = BotDetailViewModel(
            botId = sampleBot.id,
            catalogRepository = fakeCatalogRepo,
            credentialStore = fakeCredentialStore,
            botServiceController = fakeController,
            logRepository = fakeLogRepo,
            packageDownloader = downloader,
            installationManager = installer,
            appVersionCode = 1
        )

        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Initial State: Uninstalled, bot not running
        val initialState = viewModel.uiState.value as BotDetailUiState.Success
        assertFalse("Bot should not be installed initially", initialState.isInstalled)
        assertFalse("Bot should not be installing initially", initialState.isInstalling)
        assertEquals(0, fakeController.startCount)

        // 2. Trigger "Add Bot" (Step 1-7)
        viewModel.installBot()
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Verify Download & Installation
        assertEquals(1, downloader.downloadCalls)
        assertEquals(1, installer.installCalls)
        assertTrue(installer.isInstalled(sampleBot.id))
        assertEquals("2.1.0", installer.getInstalledVersion(sampleBot.id))

        // 4. Verify State transitions to Installed and reveals configuration
        val installedState = viewModel.uiState.value as BotDetailUiState.Success
        assertTrue("Bot should now be marked installed", installedState.isInstalled)
        assertFalse("Installing flag should be reset", installedState.isInstalling)
        assertEquals("2.1.0", installedState.installedVersion)

        // 5. CRITICAL: Verify bot is NOT automatically started!
        assertEquals("Bot must NOT be started automatically upon install", 0, fakeController.startCount)

        // 6. User enters required credentials
        viewModel.updateCredential("bot_token", "123456:ABC-TOKEN")
        viewModel.updateCredential("api_key", "secret-key-xyz")

        // 7. User taps "START BOT" (Final action)
        viewModel.startBot()
        testDispatcher.scheduler.advanceUntilIdle()

        // 8. Verify credentials saved and bot service started
        assertEquals(1, fakeController.startCount)
        assertEquals("123456:ABC-TOKEN", fakeCredentialStore.getCredential(sampleBot.id, "bot_token"))
        assertEquals("secret-key-xyz", fakeCredentialStore.getCredential(sampleBot.id, "api_key"))
    }

    @Test
    fun installFlow_corruptedSha256_reportsErrorAndDoesNotInstall() = runTest(testDispatcher) {
        val downloader = FakeDownloader(failChecksum = true)
        val installer = FakeInstallationManager()

        val viewModel = BotDetailViewModel(
            botId = sampleBot.id,
            catalogRepository = fakeCatalogRepo,
            credentialStore = fakeCredentialStore,
            botServiceController = fakeController,
            logRepository = fakeLogRepo,
            packageDownloader = downloader,
            installationManager = installer,
            appVersionCode = 1
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installBot()
        testDispatcher.scheduler.advanceUntilIdle()

        // Installation must fail
        assertEquals(1, downloader.downloadCalls)
        assertEquals(0, installer.installCalls)
        assertFalse(installer.isInstalled(sampleBot.id))

        val state = viewModel.uiState.value as BotDetailUiState.Success
        assertFalse(state.isInstalled)
        assertFalse(state.isInstalling)
        assertNotNull(state.installError)
        assertTrue(state.installError!!.contains("SHA-256"))
    }

    @Test
    fun installFlow_incompatibleAppVersion_blocksInstallation() = runTest(testDispatcher) {
        val downloader = FakeDownloader()
        val installer = FakeInstallationManager()

        // Bot requires app version 5, but running app is version 1
        val futureBot = sampleBot.copy(minimumAppVersion = 5)
        val repoWithFutureBot = object : CatalogRepository by fakeCatalogRepo {
            override suspend fun getBotDetails(botId: String, forceRefresh: Boolean) = Result.success(futureBot)
        }

        val viewModel = BotDetailViewModel(
            botId = futureBot.id,
            catalogRepository = repoWithFutureBot,
            credentialStore = fakeCredentialStore,
            botServiceController = fakeController,
            logRepository = fakeLogRepo,
            packageDownloader = downloader,
            installationManager = installer,
            appVersionCode = 1
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installBot()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should block without calling downloader
        assertEquals(0, downloader.downloadCalls)
        assertEquals(0, installer.installCalls)

        val state = viewModel.uiState.value as BotDetailUiState.Success
        assertFalse(state.isInstalled)
        assertNotNull(state.installError)
        assertTrue(state.installError!!.contains("App update required"))
    }

    @Test
    fun installFlow_builtInRuntime_installsSuccessfullyAndSyncsWithBotInstanceManager() = runTest(testDispatcher) {
        val builtInBot = sampleBot.copy(
            id = "bot_ai_assistant",
            name = "Gemini Smart Assistant",
            packageInfo = sampleBot.packageInfo.copy(
                packageUrl = "https://assets.tapbot.internal/packages/bot_ai_assistant_2.1.0.botpkg",
                runtimeType = "built-in"
            )
        )
        val repoWithBuiltIn = object : CatalogRepository by fakeCatalogRepo {
            override suspend fun getBotDetails(botId: String, forceRefresh: Boolean) = Result.success(builtInBot)
        }
        val downloader = FakeDownloader()
        val installer = FakeInstallationManager()

        val installedInstances = kotlinx.coroutines.flow.MutableStateFlow<List<com.tapbot.core.model.BotInstance>>(emptyList())
        var managerStartedId: String? = null
        var managerStoppedId: String? = null
        val fakeInstanceManager = object : com.tapbot.core.runner.manager.BotInstanceManager {
            override val instances = installedInstances
            override val updateProgress = kotlinx.coroutines.flow.MutableStateFlow<Map<String, com.tapbot.core.model.BotUpdateProgress>>(emptyMap())
            override val activeState = kotlinx.coroutines.flow.MutableStateFlow<com.tapbot.core.runner.runtime.BotRuntimeState>(
                com.tapbot.core.runner.runtime.BotRuntimeState.Stopped
            )
            override suspend fun validateToken(token: String) =
                Result.success(com.tapbot.core.model.TelegramUser(1L, true, "Bot", username = "bot"))
            override suspend fun start() = Result.success(Unit)
            override suspend fun stop() = Result.success(Unit)
            override suspend fun restart() = Result.success(Unit)
            override suspend fun install(bot: BotMetadata, packageFile: File): Result<com.tapbot.core.model.BotInstance> {
                val inst = com.tapbot.core.model.BotInstance(
                    installationId = "inst_${bot.id}",
                    botId = bot.id,
                    name = bot.name,
                    version = bot.version,
                    runtimeType = bot.packageInfo.runtimeType,
                    status = com.tapbot.core.model.BotInstanceStatus.Stopped
                )
                installedInstances.value = installedInstances.value + inst
                return Result.success(inst)
            }
            override suspend fun uninstall(installationId: String): Result<Unit> {
                installedInstances.value = installedInstances.value.filterNot { it.installationId == installationId }
                return Result.success(Unit)
            }
            override suspend fun start(installationId: String): Result<Unit> {
                managerStartedId = installationId
                return Result.success(Unit)
            }
            override suspend fun stop(installationId: String): Result<Unit> {
                managerStoppedId = installationId
                return Result.success(Unit)
            }
            override suspend fun restart(installationId: String) = Result.success(Unit)
            override suspend fun update(installationId: String, newVersion: String, packageFile: File) =
                Result.failure<com.tapbot.core.model.BotInstance>(UnsupportedOperationException())
            override suspend fun updateWithRollback(
                installationId: String,
                targetVersion: com.tapbot.core.model.BotVersion,
                packageDownloader: BotPackageDownloader?,
                manifestValidator: com.tapbot.core.network.ManifestValidator?,
                onProgress: ((com.tapbot.core.model.BotUpdateProgress) -> Unit)?
            ) = Result.failure<com.tapbot.core.model.BotInstance>(UnsupportedOperationException())
            override fun getInstance(installationId: String) =
                installedInstances.value.find { it.installationId == installationId }
            override fun getInstanceByBotId(botId: String) =
                installedInstances.value.find { it.botId == botId }
            override fun observeInstance(installationId: String) =
                kotlinx.coroutines.flow.flowOf(getInstance(installationId))
            override fun updateInstanceStatus(installationId: String, status: com.tapbot.core.model.BotInstanceStatus) {}
            override fun recordCrash(installationId: String, reason: String) {}
        }

        val viewModel = BotDetailViewModel(
            botId = builtInBot.id,
            catalogRepository = repoWithBuiltIn,
            credentialStore = fakeCredentialStore,
            botServiceController = fakeController,
            logRepository = fakeLogRepo,
            packageDownloader = downloader,
            installationManager = installer,
            appVersionCode = 1,
            botInstanceManager = fakeInstanceManager
        )

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.installBot()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as BotDetailUiState.Success
        assertTrue("built-in runtime must install cleanly", state.isInstalled)
        assertTrue(installer.isInstalled(builtInBot.id))
        assertNotNull("BotInstanceManager must reflect newly installed bot", fakeInstanceManager.getInstanceByBotId(builtInBot.id))

        viewModel.updateCredential("bot_token", "123456:ABC-TOKEN")
        viewModel.updateCredential("api_key", "AIzaSy-test")
        viewModel.startBot()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("inst_bot_ai_assistant", managerStartedId)
        assertEquals(1, fakeController.startCount)

        viewModel.stopBot()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("inst_bot_ai_assistant", managerStoppedId)
    }
}
