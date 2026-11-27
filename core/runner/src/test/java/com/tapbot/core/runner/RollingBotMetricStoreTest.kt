package com.tapbot.core.runner

import com.tapbot.core.runner.telemetry.RollingBotMetricStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RollingBotMetricStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `recordMessage increments message and command counts`() = runBlocking {
        val store = RollingBotMetricStore(storageFile = null, scope = CoroutineScope(Dispatchers.Unconfined))

        store.recordMessage(botId = "bot_commander", isCommand = false)
        store.recordMessage(botId = "bot_commander", isCommand = true)
        store.recordMessage(botId = "bot_habit", isCommand = true)

        val cmdSnap = store.getSnapshot("bot_commander")
        assertEquals(2L, cmdSnap.totalMessages)
        assertEquals(2L, cmdSnap.messagesToday)
        assertEquals(1L, cmdSnap.totalCommands)
        assertEquals(1L, cmdSnap.commandsToday)

        val habitSnap = store.getSnapshot("bot_habit")
        assertEquals(1L, habitSnap.totalMessages)
        assertEquals(1L, habitSnap.totalCommands)

        val summary = store.getFleetSummary(runningCount = 2, totalCount = 5)
        assertEquals(3L, summary.totalMessagesProcessed)
        assertEquals(3L, summary.messagesProcessedToday)
        assertEquals(2L, summary.totalCommandsExecuted)
        assertEquals(2L, summary.commandsExecutedToday)
        assertEquals(2, summary.runningBotsCount)
        assertEquals(5, summary.totalBotsCount)
    }

    @Test
    fun `recordError tracks errors and computes uptime percentage`() = runBlocking {
        val store = RollingBotMetricStore(storageFile = null, scope = CoroutineScope(Dispatchers.Unconfined))

        store.recordMessage("bot_test", isCommand = true)
        assertEquals(99.9, store.getSnapshot("bot_test").estimatedUptimePercentage, 0.1)

        store.recordError("bot_test", "Network timeout")
        val snap = store.getSnapshot("bot_test")
        assertEquals(1L, snap.errorCount)
        assertTrue(snap.estimatedUptimePercentage < 99.0)
    }

    @Test
    fun `bot started and stopped records session uptime`() = runBlocking {
        val store = RollingBotMetricStore(storageFile = null, scope = CoroutineScope(Dispatchers.Unconfined))

        store.recordBotStarted("bot_music")
        assertTrue(store.getSnapshot("bot_music").isCurrentlyRunning)

        store.recordBotStopped("bot_music")
        assertFalse(store.getSnapshot("bot_music").isCurrentlyRunning)
    }

    @Test
    fun `metrics persist to disk and reload successfully`() = runBlocking {
        val file = tempFolder.newFile("test_metrics.json")
        file.delete()

        val store1 = RollingBotMetricStore(storageFile = file, scope = CoroutineScope(Dispatchers.Unconfined))
        store1.recordMessage("bot_persistent", isCommand = true)
        store1.recordMessage("bot_persistent", isCommand = false)
        store1.flush()

        // Reload from same file
        val store2 = RollingBotMetricStore(storageFile = file, scope = CoroutineScope(Dispatchers.Unconfined))
        val snap = store2.getSnapshot("bot_persistent")
        assertEquals(2L, snap.totalMessages)
        assertEquals(1L, snap.totalCommands)
    }
}
