package com.tapbot.core.logging

import com.tapbot.core.model.LogLevel
import com.tapbot.core.security.SecretRedactor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RingBufferLogRepositoryTest {

    @After
    fun tearDown() {
        SecretRedactor.clearRegisteredSecrets()
    }

    @Test
    fun `appendLog respects maximum capacity ring buffer boundary`() {
        val repo = InMemoryRingBufferLogRepository(maxCapacityPerBot = 5)
        val botId = "test_bot"

        for (i in 1..10) {
            repo.appendLog(botId, LogLevel.INFO, "TestTag", "Message $i")
        }

        val logs = repo.getLogStream(botId).value
        assertEquals(5, logs.size)
        assertEquals("Message 6", logs.first().message)
        assertEquals("Message 10", logs.last().message)
    }

    @Test
    fun `clearLogs empties buffer`() {
        val repo = InMemoryRingBufferLogRepository(maxCapacityPerBot = 10)
        val botId = "test_bot"

        repo.appendLog(botId, LogLevel.INFO, "TestTag", "Hello")
        repo.appendLog(botId, LogLevel.ERROR, "TestTag", "World")
        assertEquals(2, repo.getLogStream(botId).value.size)

        repo.clearLogs(botId)
        assertEquals(0, repo.getLogStream(botId).value.size)
    }

    @Test
    fun `appendLog redacts API keys bearer tokens and registered secrets`() {
        val repo = InMemoryRingBufferLogRepository(maxCapacityPerBot = 10)
        val openAiKey = listOf("sk", "proj", "unitTestFakeKeyForLoggingRedaction12345").joinToString("-")
        val geminiKey = "AIza" + "SyUnitTestLoggingRedactionKey0123456789"
        val customSecret = "very-sensitive-webhook-signing-token"
        SecretRedactor.registerSecret(customSecret)

        repo.appendLog(
            "bot1",
            LogLevel.WARN,
            "Tag:$geminiKey",
            "Auth Bearer abcdefghij1234567890 key=$openAiKey custom=$customSecret"
        )

        val entry = repo.getLogStream("bot1").value.single()
        assertFalse(entry.tag.contains(geminiKey))
        assertFalse(entry.message.contains(openAiKey))
        assertFalse(entry.message.contains("abcdefghij1234567890"))
        assertFalse(entry.message.contains(customSecret))
        assertTrue(entry.message.contains("[REDACTED_SECRET]"))
    }
}
