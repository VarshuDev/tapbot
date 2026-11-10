package com.tapbot.feature.botdetail

import com.tapbot.core.logging.BotLogRepository
import com.tapbot.core.model.BotCredentialSpec
import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotPackageInfo
import com.tapbot.core.model.BotRunState
import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.model.BotVersion
import com.tapbot.core.network.CatalogRepository
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.BotServiceController
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.security.SecretRedactor
import com.tapbot.core.security.SecureCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BotDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val testBot = BotMetadata(
        id = "bot_commander",
        name = "TapBot Commander",
        summary = "Host telemetry watchdog",
        description = "Controls phone & bot fleet",
        author = "TapBot",
        version = "2.3.0",
        iconUrl = "",
        category = "Automation",
        requiredCredentials = listOf(BotCredentialSpec.TELEGRAM_BOT_TOKEN),
        packageInfo = BotPackageInfo("", "", "native_art", packageSizeBytes = 1024L)
    )

    private class FakeSecureCredentialStore : SecureCredentialStore {
        private val map = mutableMapOf<String, String>()
        override suspend fun saveCredential(botId: String, key: String, secretValue: String) {
            map["$botId:$key"] = secretValue
        }
        override suspend fun getCredential(botId: String, key: String): String? = map["$botId:$key"]
        override suspend fun deleteCredential(botId: String, key: String) {
            map.remove("$botId:$key")
        }
        override suspend fun hasCredential(botId: String, key: String): Boolean =
            !map["$botId:$key"].isNullOrBlank()
        override suspend fun getAllCredentials(botId: String): Map<String, String> =
            map.filterKeys { it.startsWith("$botId:") }.mapKeys { it.key.removePrefix("$botId:") }
        override suspend fun deleteCredentials(botId: String) {
            map.keys.removeAll { it.startsWith("$botId:") }
        }
        override suspend fun hasRequiredCredentials(botId: String, requiredKeys: List<String>): Boolean =
            requiredKeys.all { hasCredential(botId, it) }
    }

    private class FakeCatalogRepository(private val bots: List<BotMetadata>) : CatalogRepository {
        override suspend fun getBots(forceRefresh: Boolean) = Result.success(bots)
        override suspend fun getFeaturedBots() = Result.success(bots.filter { it.isFeatured })
        override suspend fun getCategories() = Result.success(listOf("All", "Automation"))
        override suspend fun getBotDetails(botId: String, forceRefresh: Boolean): Result<BotMetadata> {
            val found = bots.firstOrNull { it.id == botId }
            return if (found != null) Result.success(found)
            else Result.failure(NoSuchElementException(botId))
        }
        override suspend fun getBotVersions(botId: String): Result<List<com.tapbot.core.model.BotVersion>> = Result.success(emptyList())
        override suspend fun getLatestVersion(botId: String): Result<com.tapbot.core.model.BotVersion?> = Result.success(null)
        override suspend fun checkForUpdate(botId: String, currentVersion: String): Result<com.tapbot.core.model.BotVersion?> = Result.success(null)
    }

    private class FakeServiceController : BotServiceController {
        private val state = MutableStateFlow<BotRunState>(BotRunState.Stopped)
        override fun startBot(botId: String) { state.value = BotRunState.Running(1, 0) }
        override fun stopBot(botId: String) { state.value = BotRunState.Stopped }
        override fun stopAll() { state.value = BotRunState.Stopped }
        override fun getBotRunState(botId: String): StateFlow<BotRunState> = state.asStateFlow()
    }

    private class FakeLogRepository : BotLogRepository {
        private val stream = MutableStateFlow<List<BotLogEntry>>(emptyList())
        override fun appendLog(botId: String, level: LogLevel, tag: String, message: String) {}
        override fun getLogStream(botId: String): StateFlow<List<BotLogEntry>> = stream.asStateFlow()
        override fun clearLogs(botId: String) { stream.value = emptyList() }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        SecretRedactor.clearRegisteredSecrets()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        SecretRedactor.clearRegisteredSecrets()
    }

    @Test
    fun `verifyToken succeeds with TelegramApiClient and populates verifiedBotUsername`() = runTest {
        val fakeTelegram = object : TelegramApiClient() {
            override suspend fun getMe(token: String): Result<TelegramUser> {
                return Result.success(
                    TelegramUser(
                        id = 1001L,
                        isBot = true,
                        firstName = "Commander",
                        username = "@commander_test_bot"
                    )
                )
            }
        }

        val vm = BotDetailViewModel(
            botId = testBot.id,
            catalogRepository = FakeCatalogRepository(listOf(testBot)),
            credentialStore = FakeSecureCredentialStore(),
            botServiceController = FakeServiceController(),
            logRepository = FakeLogRepository(),
            telegramApiClient = fakeTelegram
        )
        advanceUntilIdle()

        vm.updateCredential("bot_token", "123456789:ValidBotTokenSecret_001")
        vm.verifyToken()
        advanceUntilIdle()

        val state = vm.uiState.value as BotDetailUiState.Success
        assertFalse(state.isVerifyingToken)
        assertNull(state.tokenVerifyError)
        assertEquals("commander_test_bot", state.verifiedBotUsername)

        // Modifying token clears previously verified username
        vm.updateCredential("bot_token", "123456789:AnotherTokenSecret_002")
        val updatedState = vm.uiState.value as BotDetailUiState.Success
        assertNull(updatedState.verifiedBotUsername)
    }

    @Test
    fun `verifyToken failure redacts token secret from error message and clears on delete`() = runTest {
        val rawSecretToken = "555123456:SuperSensitiveTokenPayload_XYZ123"
        val failingTelegram = object : TelegramApiClient() {
            override suspend fun getMe(token: String): Result<TelegramUser> {
                return Result.failure(IOException("HTTP 401 failed calling /bot$token/getMe"))
            }
        }

        val vm = BotDetailViewModel(
            botId = testBot.id,
            catalogRepository = FakeCatalogRepository(listOf(testBot)),
            credentialStore = FakeSecureCredentialStore(),
            botServiceController = FakeServiceController(),
            logRepository = FakeLogRepository(),
            telegramApiClient = failingTelegram
        )
        advanceUntilIdle()

        vm.updateCredential("bot_token", rawSecretToken)
        vm.verifyToken()
        advanceUntilIdle()

        val errState = vm.uiState.value as BotDetailUiState.Success
        assertNull(errState.verifiedBotUsername)
        assertNotNull(errState.tokenVerifyError)
        assertFalse(errState.tokenVerifyError!!.contains(rawSecretToken))
        assertTrue(
            errState.tokenVerifyError!!.contains("[REDACTED")
        )

        vm.deleteCredentials()
        advanceUntilIdle()
        val clearedState = vm.uiState.value as BotDetailUiState.Success
        assertNull(clearedState.tokenVerifyError)
        assertNull(clearedState.verifiedBotUsername)
    }

    @Test
    fun `live customDefinition observation updates bot metadata and rules in detail state`() = runTest {
        val customRepo = CustomBotRepository(storageFile = null)
        val initialDef = CustomBotDefinition(
            id = "bot_custom_ops_1",
            name = "Ops v1",
            summary = "Initial summary",
            rules = listOf(CommandRule(id = "r1", command = "/ping", responseTemplate = "pong"))
        )
        customRepo.saveDefinition(initialDef)

        val vm = BotDetailViewModel(
            botId = initialDef.id,
            catalogRepository = FakeCatalogRepository(emptyList()),
            credentialStore = FakeSecureCredentialStore(),
            botServiceController = FakeServiceController(),
            logRepository = FakeLogRepository(),
            customBotRepository = customRepo
        )
        advanceUntilIdle()

        val loaded = vm.uiState.value as BotDetailUiState.Success
        assertEquals("Ops v1", loaded.bot.name)
        assertEquals(1, loaded.customDefinition?.rules?.size)

        customRepo.saveDefinition(
            initialDef.copy(
                name = "Ops v2 Updated",
                rules = initialDef.rules + CommandRule(id = "r2", command = "/deploy", responseTemplate = "ok")
            )
        )
        advanceUntilIdle()

        val refreshed = vm.uiState.value as BotDetailUiState.Success
        assertEquals("Ops v2 Updated", refreshed.bot.name)
        assertEquals(2, refreshed.customDefinition?.rules?.size)
    }
}
