package com.tapbot.feature.catalog

import com.tapbot.core.network.DefaultLocalBotInstallationManager
import com.tapbot.core.network.LocalCatalogCache
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.runner.manager.DefaultBotInstanceManager
import com.tapbot.core.security.CredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomBotBuilderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeCredentialStore : CredentialStore {
        val creds = mutableMapOf<String, String>()

        override suspend fun saveCredential(botId: String, key: String, secretValue: String) {
            creds["$botId:$key"] = secretValue
        }

        override suspend fun getCredential(botId: String, key: String): String? = creds["$botId:$key"]

        override suspend fun deleteCredential(botId: String, key: String) {
            creds.remove("$botId:$key")
        }

        override suspend fun hasCredential(botId: String, key: String): Boolean =
            !creds["$botId:$key"].isNullOrBlank()

        override suspend fun getAllCredentials(botId: String): Map<String, String> =
            creds.filterKeys { it.startsWith("$botId:") }.mapKeys { it.key.removePrefix("$botId:") }

        override suspend fun deleteCredentials(botId: String) {
            creds.keys.removeAll { it.startsWith("$botId:") }
        }

        override suspend fun hasRequiredCredentials(botId: String, requiredKeys: List<String>): Boolean =
            requiredKeys.all { hasCredential(botId, it) }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `validates name token and webhook URL before saving`() = runTest(testDispatcher) {
        val customRepo = CustomBotRepository(storageFile = null)
        val vm = CustomBotBuilderViewModel(customBotRepository = customRepo)

        // Blank name
        vm.updateTelegramToken("12345678:ValidTokenString123")
        vm.saveCustomBot()
        assertEquals("Bot Name is required.", vm.uiState.value.errorMessage)

        // Invalid token
        vm.updateBotName("Alert Bot")
        vm.updateTelegramToken("invalid_token")
        vm.saveCustomBot()
        assertTrue(vm.uiState.value.errorMessage?.contains("valid Telegram Bot Token") == true)

        // Invalid webhook URL
        vm.updateTelegramToken("12345678:ValidTokenString123")
        vm.updateRule("rule_1") { it.copy(webhookUrl = "ftp://bad-scheme.example.com") }
        vm.saveCustomBot()
        assertTrue(vm.uiState.value.errorMessage?.contains("http:// or https://") == true)
        assertNull(vm.uiState.value.savedBotId)
    }

    @Test
    fun `saveCustomBot stores definition keystore token catalog cache and fleet instance`() = runTest(testDispatcher) {
        val customRepo = CustomBotRepository(storageFile = null)
        val credStore = FakeCredentialStore()
        val cache = LocalCatalogCache(cacheFile = null)
        val installMgr = DefaultLocalBotInstallationManager(storageFile = null, installDir = null)
        val instanceMgr = DefaultBotInstanceManager(
            context = null,
            storageFile = null,
            installDir = null,
            credentialStore = credStore,
            telegramApi = TelegramApiClient(),
            serviceLauncher = { _, _ -> }
        )

        val vm = CustomBotBuilderViewModel(
            customBotRepository = customRepo,
            botInstanceManager = instanceMgr,
            credentialStore = credStore,
            localCatalogCache = cache,
            localInstallationManager = installMgr
        )

        vm.updateBotName("Status Notifier")
        vm.updateSummary("Checks API status via command")
        vm.updateTelegramToken("987654321:ValidTelegramSecretToken")
        vm.addRule()
        vm.saveCustomBot()

        testDispatcher.scheduler.advanceUntilIdle()

        val savedId = vm.uiState.value.savedBotId
        assertNotNull(savedId)
        assertTrue(savedId!!.startsWith("bot_custom_status_notifier_"))
        assertNull(vm.uiState.value.errorMessage)

        val def = customRepo.getDefinition(savedId)
        assertNotNull(def)
        assertEquals(2, def!!.rules.size)
        assertEquals("987654321:ValidTelegramSecretToken", credStore.getCredential(savedId, "bot_token"))
        assertEquals("Status Notifier", cache.getCachedBot(savedId)?.name)
        assertTrue(installMgr.isInstalled(savedId))
        assertNotNull(instanceMgr.getInstanceByBotId(savedId))
    }

    @Test
    fun `applyBlueprint populates template rules and loadBotForEditing preserves id and createdAt`() = runTest(testDispatcher) {
        val customRepo = CustomBotRepository(storageFile = null)
        val credStore = FakeCredentialStore()
        val cache = LocalCatalogCache(cacheFile = null)
        val installMgr = DefaultLocalBotInstallationManager(storageFile = null, installDir = null)

        val creatorVm = CustomBotBuilderViewModel(
            customBotRepository = customRepo,
            credentialStore = credStore,
            localCatalogCache = cache,
            localInstallationManager = installMgr
        )

        creatorVm.applyBlueprint("webhook_monitor")
        val afterBlueprint = creatorVm.uiState.value
        assertEquals("webhook_monitor", afterBlueprint.selectedBlueprintId)
        assertEquals("Webhook Relay Bot", afterBlueprint.botName)
        assertEquals(2, afterBlueprint.rules.size)
        assertTrue(afterBlueprint.rules.any { it.command == "/webhook" })

        creatorVm.updateTelegramToken("111222333:BlueprintTokenSecret_999")
        creatorVm.saveCustomBot()
        testDispatcher.scheduler.advanceUntilIdle()

        val createdId = creatorVm.uiState.value.savedBotId!!
        val originalDef = customRepo.getDefinition(createdId)!!

        // Now open an editor ViewModel for the existing custom bot
        val editorVm = CustomBotBuilderViewModel(
            customBotRepository = customRepo,
            credentialStore = credStore,
            localCatalogCache = cache,
            localInstallationManager = installMgr
        )
        editorVm.loadBotForEditing(createdId)
        testDispatcher.scheduler.advanceUntilIdle()

        val editState = editorVm.uiState.value
        assertTrue(editState.isEditing)
        assertEquals(createdId, editState.editingBotId)
        assertEquals(originalDef.createdAt, editState.originalCreatedAt)
        assertEquals("111222333:BlueprintTokenSecret_999", editState.telegramToken)

        editorVm.updateBotName("Webhook Relay Pro")
        editorVm.addRule()
        editorVm.saveCustomBot()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(createdId, editorVm.uiState.value.savedBotId)
        val updatedDef = customRepo.getDefinition(createdId)!!
        assertEquals("Webhook Relay Pro", updatedDef.name)
        assertEquals(originalDef.createdAt, updatedDef.createdAt)
        assertEquals(3, updatedDef.rules.size)
        assertEquals("Webhook Relay Pro", cache.getCachedBot(createdId)?.name)
    }
}
