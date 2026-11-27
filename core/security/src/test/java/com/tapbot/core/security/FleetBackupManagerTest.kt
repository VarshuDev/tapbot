package com.tapbot.core.security

import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.CustomBotDefinition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FleetBackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class TestCredentialStore : CredentialStore {
        val map = mutableMapOf<String, MutableMap<String, String>>()
        var globalToken: String? = null

        override suspend fun saveCredential(botId: String, key: String, secretValue: String) {
            map.getOrPut(botId) { mutableMapOf() }[key] = secretValue
        }

        override suspend fun getCredential(botId: String, key: String): String? = map[botId]?.get(key)
        override suspend fun deleteCredential(botId: String, key: String) { map[botId]?.remove(key) }
        override suspend fun hasCredential(botId: String, key: String): Boolean = map[botId]?.containsKey(key) == true
        override suspend fun getAllCredentials(botId: String): Map<String, String> = map[botId]?.toMap() ?: emptyMap()
        override suspend fun deleteCredentials(botId: String) { map.remove(botId) }
        override suspend fun hasRequiredCredentials(botId: String, requiredKeys: List<String>): Boolean =
            requiredKeys.all { hasCredential(botId, it) }

        override suspend fun saveToken(token: String) { globalToken = token }
        override suspend fun getToken(): String? = globalToken
        override suspend fun clearToken() { globalToken = null }
        override suspend fun hasToken(): Boolean = !globalToken.isNullOrBlank()
    }

    @Test
    fun `export and restore successfully preserves instances and credentials`() = runBlocking {
        val credStore = TestCredentialStore()
        credStore.saveCredential("bot_1", "telegram_bot_token", "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11")
        credStore.saveCredential("bot_1", "api_key", "secret_api_key_xyz")
        credStore.saveToken("global_token_secret")

        val instances = listOf(
            BotInstance(
                installationId = "inst_bot_1",
                botId = "bot_1",
                name = "Commander Bot",
                version = "1.0.0",
                status = BotInstanceStatus.Stopped
            )
        )
        val customBots = listOf(
            CustomBotDefinition(
                id = "bot_custom_1",
                name = "My Custom Bot",
                summary = "Custom Workflow Bot"
            )
        )

        val manager = FleetBackupManager()
        val archiveResult = manager.exportArchive(
            passphrase = "MasterSecretPassword123!",
            instances = instances,
            customBots = customBots,
            credentialStore = credStore
        )
        assertTrue(archiveResult.isSuccess)
        val archive = archiveResult.getOrThrow()
        assertEquals(1, archive.instances.size)
        assertEquals(1, archive.customBots.size)
        assertTrue(archive.saltHex.isNotBlank())
        assertTrue(archive.ivHex.isNotBlank())
        assertTrue(archive.encryptedPayloadBase64.isNotBlank())

        // Now restore on a brand new empty credential store
        val freshCredStore = TestCredentialStore()
        val restoreResult = manager.restoreFromArchive(
            passphrase = "MasterSecretPassword123!",
            archive = archive,
            credentialStore = freshCredStore
        )
        assertTrue(restoreResult.isSuccess)
        assertEquals("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11", freshCredStore.getCredential("bot_1", "telegram_bot_token"))
        assertEquals("secret_api_key_xyz", freshCredStore.getCredential("bot_1", "api_key"))
        assertEquals("global_token_secret", freshCredStore.getToken())
    }

    @Test
    fun `restore rejects wrong passphrase`() = runBlocking {
        val credStore = TestCredentialStore()
        credStore.saveCredential("bot_test", "token", "token123")

        val manager = FleetBackupManager()
        val archive = manager.exportArchive(
            passphrase = "CorrectPassword123",
            instances = emptyList(),
            customBots = emptyList(),
            credentialStore = credStore
        ).getOrThrow()

        val freshStore = TestCredentialStore()
        val result = manager.restoreFromArchive(
            passphrase = "WrongPassword456",
            archive = archive,
            credentialStore = freshStore
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `restore rejects tampered ciphertext`() = runBlocking {
        val credStore = TestCredentialStore()
        credStore.saveCredential("bot_test", "token", "token123")

        val manager = FleetBackupManager()
        val archive = manager.exportArchive(
            passphrase = "Password123",
            instances = emptyList(),
            customBots = emptyList(),
            credentialStore = credStore
        ).getOrThrow()

        val tamperedArchive = archive.copy(
            encryptedPayloadBase64 = "dGFtcGVyZWRkYXRh"
        )

        val freshStore = TestCredentialStore()
        val result = manager.restoreFromArchive(
            passphrase = "Password123",
            archive = tamperedArchive,
            credentialStore = freshStore
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `exportToFile and restoreFromFile round trip`() = runBlocking {
        val credStore = TestCredentialStore()
        credStore.saveCredential("bot_file", "token", "super_secret_file_token")

        val file = tempFolder.newFile("backup.tapbotbackup")
        file.delete()

        val instances = listOf(
            BotInstance(
                installationId = "inst_bot_file",
                botId = "bot_file",
                name = "File Bot",
                version = "1.0.0",
                status = BotInstanceStatus.Stopped
            )
        )

        val manager = FleetBackupManager()
        val exportRes = manager.exportToFile(
            passphrase = "PassphraseFile123",
            instances = instances,
            customBots = emptyList(),
            credentialStore = credStore,
            targetFile = file
        )
        assertTrue(exportRes.isSuccess)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        val freshStore = TestCredentialStore()
        val restoreRes = manager.restoreFromFile(
            passphrase = "PassphraseFile123",
            sourceFile = file,
            credentialStore = freshStore
        )
        assertTrue(restoreRes.isSuccess)
        assertEquals("super_secret_file_token", freshStore.getCredential("bot_file", "token"))
    }
}
