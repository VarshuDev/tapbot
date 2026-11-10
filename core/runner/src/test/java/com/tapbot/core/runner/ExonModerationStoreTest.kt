package com.tapbot.core.runner

import com.tapbot.core.runner.moderation.ExonModerationStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExonModerationStoreTest {

    @Test
    fun `persists warns, rules, locks, welcome, and antiflood across store reloads`() {
        val tempFile = File.createTempFile("exon_store_test", ".json")
        tempFile.delete()

        try {
            val store1 = ExonModerationStore(tempFile)
            val chatId = -100123456L

            assertEquals(1, store1.incrementWarn(chatId, 42L))
            assertEquals(2, store1.incrementWarn(chatId, 42L))
            store1.setRules(chatId, "Be respectful and no spam.")
            store1.addLock(chatId, "url")
            store1.addLock(chatId, "sticker")
            store1.setWelcomeMessage(chatId, "Welcome {first} to {chat}!")
            store1.setAntifloodLimit(chatId, 4)

            // Reload from disk in fresh instance
            val store2 = ExonModerationStore(tempFile)
            assertEquals(2, store2.getWarns(chatId, 42L))
            assertEquals("Be respectful and no spam.", store2.getRules(chatId))
            assertTrue(store2.getLocks(chatId).contains("links"))
            assertTrue(store2.getLocks(chatId).contains("sticker"))
            val (enabled, welcomeMsg) = store2.getWelcomeConfig(chatId)
            assertTrue(enabled)
            assertEquals("Welcome {first} to {chat}!", welcomeMsg)
            assertEquals(4, store2.getAntifloodLimit(chatId))

            store2.resetWarns(chatId, 42L)
            store2.clearRules(chatId)
            store2.removeLock(chatId, "links")
            store2.clearWelcome(chatId)

            val store3 = ExonModerationStore(tempFile)
            assertEquals(0, store3.getWarns(chatId, 42L))
            assertNull(store3.getRules(chatId))
            assertFalse(store3.getLocks(chatId).contains("links"))
            assertTrue(store3.getLocks(chatId).contains("sticker"))
            val (enabledAfterClear, msgAfterClear) = store3.getWelcomeConfig(chatId)
            assertFalse(enabledAfterClear)
            assertNull(msgAfterClear)
        } finally {
            tempFile.delete()
        }
    }
}
