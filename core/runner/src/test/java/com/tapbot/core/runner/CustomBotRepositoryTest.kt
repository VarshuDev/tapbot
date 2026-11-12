package com.tapbot.core.runner

import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.core.runner.custom.CustomBotRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CustomBotRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `saves updates reloads and deletes custom bot definitions`() {
        val file = tempFolder.newFile("custom_bots.json")
        file.delete()

        val repo = CustomBotRepository(file)
        val definition = CustomBotDefinition(
            id = "custom_deploy_1",
            name = "Deploy Hook Bot",
            summary = "Triggers deployment pipelines",
            welcomeMessage = "Welcome {user}",
            rules = listOf(
                CommandRule(
                    id = "r1",
                    command = "/deploy",
                    description = "Trigger deploy",
                    responseTemplate = "Result: {webhook_response}",
                    webhookUrl = "https://deploy.example.com/run",
                    webhookMethod = CustomHttpMethod.POST,
                    buttonLabel = "Re-run"
                )
            )
        )

        repo.saveDefinition(definition)
        assertEquals(1, repo.getAll().size)

        val reloaded = CustomBotRepository(file)
        val stored = reloaded.getDefinition("custom_deploy_1")
        assertNotNull(stored)
        assertEquals("Deploy Hook Bot", stored!!.name)
        assertEquals(1, stored.rules.size)
        assertEquals(CustomHttpMethod.POST, stored.rules[0].webhookMethod)

        val meta = stored.toBotMetadata()
        assertEquals("Custom", meta.category)
        assertEquals("built-in", meta.packageInfo.runtimeType)
        assertTrue(meta.requiredCredentials.any { it.key == "bot_token" })

        // Update existing
        reloaded.saveDefinition(stored.copy(summary = "Updated pipeline bot"))
        assertEquals(1, reloaded.getAll().size)
        assertEquals("Updated pipeline bot", reloaded.getDefinition("custom_deploy_1")?.summary)

        assertTrue(reloaded.deleteDefinition("custom_deploy_1"))
        assertFalse(reloaded.deleteDefinition("custom_deploy_1"))
        assertNull(CustomBotRepository(file).getDefinition("custom_deploy_1"))
    }

    @Test
    fun `registers and unregisters sensitive webhook header secrets with SecretRedactor`() {
        com.tapbot.core.security.SecretRedactor.clearRegisteredSecrets()
        val file = tempFolder.newFile("custom_bots_secret.json")
        val repo = CustomBotRepository(file)
        val secretToken = "wh_super_secret_token_98765"
        val def = CustomBotDefinition(
            id = "custom_sec_hdr",
            name = "Header Bot",
            summary = "Secret header test",
            rules = listOf(
                CommandRule(
                    id = "r1",
                    command = "/ping",
                    responseTemplate = "{webhook_response}",
                    webhookUrl = "https://hooks.example.com/run",
                    webhookHeaders = mapOf(
                        "Authorization" to "Bearer $secretToken",
                        "X-Api-Key" to "custom_header_secret_12345",
                        "Content-Type" to "application/json"
                    )
                )
            )
        )

        repo.saveDefinition(def)
        val redacted = com.tapbot.core.security.SecretRedactor.redact("Failed calling with Bearer $secretToken")
        assertFalse(redacted.contains(secretToken))
        assertTrue(redacted.contains("[REDACTED_"))
        val redactedApiKey = com.tapbot.core.security.SecretRedactor.redact("Header custom_header_secret_12345 leaked")
        assertFalse(redactedApiKey.contains("custom_header_secret_12345"))
        assertTrue(redactedApiKey.contains("[REDACTED_"))

        repo.deleteDefinition("custom_sec_hdr")
        val afterDelete = com.tapbot.core.security.SecretRedactor.redact("Header custom_header_secret_12345 cleared")
        assertEquals("Header custom_header_secret_12345 cleared", afterDelete)
        com.tapbot.core.security.SecretRedactor.clearRegisteredSecrets()
    }
}
