package com.tapbot.core.runner

import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.runner.runtime.AIBotRuntime
import com.tapbot.core.runner.runtime.BotRuntime
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.BotRuntimeState
import com.tapbot.core.runner.runtime.CommanderBotRuntime
import com.tapbot.core.runner.runtime.CustomWorkflowBotRuntime
import com.tapbot.core.runner.runtime.ExonBotRuntime
import com.tapbot.core.runner.runtime.MusicBotRuntime
import com.tapbot.core.runner.runtime.PingPongBotRuntime
import com.tapbot.core.runner.runtime.RssNotifierBotRuntime
import com.tapbot.core.runner.runtime.UtilityBotRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotRuntimeFactoryTest {

    private val all20FlagshipBots = listOf(
        "bot_commander" to "TapBot Commander",
        "bot_music_streamer" to "TapStream Music",
        "bot_exon" to "Exon Robot Sentinel",
        "bot_ai_assistant" to "Gemini Smart Assistant",
        "bot_habit_forge" to "HabitForge Daily Tracker",
        "bot_penny_wise" to "PennyWise Expense Tracker",
        "bot_pomo_focus" to "PomoFocus Deep Work Timer",
        "bot_tap_note" to "TapNote Scratchpad",
        "bot_sky_pulse" to "SkyPulse Weather Radar",
        "bot_remind_me" to "RemindMe Health & Pill Alarms",
        "bot_link_drop" to "LinkDrop Link & Clipboard Vault",
        "bot_poly_glot" to "PolyGlot Instant Translator",
        "bot_coin_radar" to "CoinRadar Crypto & Stock Watcher",
        "bot_fit_track" to "FitTrack Hydration & Workouts",
        "bot_parcel_track" to "ParcelTrack Delivery Watcher",
        "bot_rss_notifier" to "NewsPulse RSS Broadcaster",
        "bot_git_notifier" to "GitNotifier Repo Watcher",
        "bot_cam_sentry" to "CamSentry Hardware Security",
        "bot_whisper_box" to "WhisperBox Anonymous Mailbox",
        "bot_trivia_master" to "TriviaMaster Daily Brain Quiz"
    )

    @After
    fun tearDown() {
        ServiceLocator.customBotRepository = null
    }

    @Test
    fun `every flagship bot resolves to a valid BotRuntime with matching botId and Stopped state`() {
        assertEquals("Must test all 20 flagship bots", 20, all20FlagshipBots.size)

        for ((id, name) in all20FlagshipBots) {
            val runtime = BotRuntimeFactory.createRuntime(id, name)
            assertNotNull("Runtime for '$id' must not be null", runtime)
            assertEquals("botId in runtime must match requested ID for '$id'", id, runtime.botId)
            assertEquals("Initial state for '$id' must be Stopped", BotRuntimeState.Stopped, runtime.state.value)
        }
    }

    @Test
    fun `flagship runtimes resolve to dedicated runtime classes`() {
        // Commander
        val commander = BotRuntimeFactory.createRuntime("bot_commander", "TapBot Commander")
        assertTrue("bot_commander must resolve to CommanderBotRuntime", commander is CommanderBotRuntime)

        // Music Streamer
        val music = BotRuntimeFactory.createRuntime("bot_music_streamer", "TapStream Music")
        assertTrue("bot_music_streamer must resolve to MusicBotRuntime", music is MusicBotRuntime)

        // Exon Sentinel
        val exon = BotRuntimeFactory.createRuntime("bot_exon", "Exon Robot Sentinel")
        assertTrue("bot_exon must resolve to ExonBotRuntime", exon is ExonBotRuntime)

        // Gemini AI Assistant
        val ai = BotRuntimeFactory.createRuntime("bot_ai_assistant", "Gemini Smart Assistant")
        assertTrue("bot_ai_assistant must resolve to AIBotRuntime", ai is AIBotRuntime)

        // RSS Notifier
        val rss = BotRuntimeFactory.createRuntime("bot_rss_notifier", "NewsPulse RSS Broadcaster")
        assertTrue("bot_rss_notifier must resolve to RssNotifierBotRuntime", rss is RssNotifierBotRuntime)
    }

    @Test
    fun `domain runtimes or graceful fallback for new 15 flagship bots`() {
        // Test resolution of all 15 new flagship bots
        val new15Bots = all20FlagshipBots.filterNot { (id, _) ->
            id in listOf("bot_commander", "bot_music_streamer", "bot_exon", "bot_ai_assistant", "bot_rss_notifier")
        }
        assertEquals(15, new15Bots.size)

        // Check if domain runtime classes are present on classpath (M2)
        val hasDailyLifestyle = try {
            Class.forName("com.tapbot.core.runner.runtime.DailyLifestyleBotRuntime") != null
        } catch (e: ClassNotFoundException) {
            false
        }

        for ((id, name) in new15Bots) {
            val runtime = BotRuntimeFactory.createRuntime(id, name)
            assertNotNull("Runtime for new flagship '$id' must not be null", runtime)
            assertEquals("botId must match requested '$id'", id, runtime.botId)
            assertEquals("Initial state must be Stopped for '$id'", BotRuntimeState.Stopped, runtime.state.value)

            if (hasDailyLifestyle && id in listOf("bot_habit_forge", "bot_remind_me", "bot_fit_track")) {
                assertEquals("DailyLifestyleBotRuntime", runtime::class.java.simpleName)
            }
        }
    }

    @Test
    fun `diagnostic and ping-pong built-in bots resolve properly`() {
        val util = BotRuntimeFactory.createRuntime("bot_utility", "Utility Bot")
        assertTrue("bot_utility must resolve to UtilityBotRuntime", util is UtilityBotRuntime)

        val echo = BotRuntimeFactory.createRuntime("bot_echo", "Echo Bot")
        assertTrue("bot_echo must resolve to UtilityBotRuntime", echo is UtilityBotRuntime)

        val pingPong = BotRuntimeFactory.createRuntime("bot_ping_pong", "Ping Pong")
        assertTrue("bot_ping_pong must resolve to PingPongBotRuntime", pingPong is PingPongBotRuntime)

        val unknown = BotRuntimeFactory.createRuntime("unknown_random_bot_xyz_123")
        assertTrue("unknown bots must fallback to PingPongBotRuntime", unknown is PingPongBotRuntime)
        assertEquals("unknown_random_bot_xyz_123", unknown.botId)
    }

    @Test
    fun `canonical aliases resolve to matching runtime classes`() {
        // Commander aliases
        assertTrue(BotRuntimeFactory.createRuntime("tapbot-commander") is CommanderBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("tapbot_commander") is CommanderBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("bot_commander_flagship") is CommanderBotRuntime)

        // Music aliases
        assertTrue(BotRuntimeFactory.createRuntime("tapstream-music") is MusicBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("tapstream_music") is MusicBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("bot_music") is MusicBotRuntime)

        // Exon aliases
        assertTrue(BotRuntimeFactory.createRuntime("bot_1790084493920_n4l8lj") is ExonBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("bot_1790084493920") is ExonBotRuntime)

        // AI aliases
        assertTrue(BotRuntimeFactory.createRuntime("bot_gemini") is AIBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("bot_ai") is AIBotRuntime)

        // RSS aliases
        assertTrue(BotRuntimeFactory.createRuntime("bot_rss") is RssNotifierBotRuntime)
    }

    @Test
    fun `handles edge case bot IDs and name formatting`() {
        // Uppercase / mixed-case
        assertTrue(BotRuntimeFactory.createRuntime("BOT_COMMANDER") is CommanderBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("Bot_Music_Streamer") is MusicBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("BOT_EXON") is ExonBotRuntime)

        // Name-based resolution
        val nameResolved = BotRuntimeFactory.createRuntime("custom_id_123", "Commander Fleet Sentinel")
        assertTrue("Name hint with commander should resolve to CommanderBotRuntime", nameResolved is CommanderBotRuntime)

        // Empty / whitespace ID
        val emptyRuntime = BotRuntimeFactory.createRuntime("")
        assertNotNull("Empty botId must not throw exception", emptyRuntime)
        assertTrue(emptyRuntime is PingPongBotRuntime)

        val blankRuntime = BotRuntimeFactory.createRuntime("   ")
        assertNotNull("Blank botId must not throw exception", blankRuntime)
        assertTrue(blankRuntime is PingPongBotRuntime)

        // Special characters in ID
        val specialRuntime = BotRuntimeFactory.createRuntime("bot_test#special!@123")
        assertNotNull("Special character ID must resolve safely", specialRuntime)
    }

    @Test
    fun `custom workflow definitions take strict precedence over built-in keywords`() {
        val customRepo = CustomBotRepository(storageFile = null).apply {
            saveDefinition(
                CustomBotDefinition(
                    id = "custom_commander_override",
                    name = "Commander Custom Flow",
                    summary = "Override built-in keyword",
                    rules = listOf(CommandRule(id = "r1", command = "/start", responseTemplate = "Hello custom"))
                )
            )
            saveDefinition(
                CustomBotDefinition(
                    id = "custom_music_stream_bot",
                    name = "Music Stream Custom Flow",
                    summary = "Override music keyword",
                    rules = listOf(CommandRule(id = "r2", command = "/play", responseTemplate = "Playing custom"))
                )
            )
        }
        ServiceLocator.customBotRepository = customRepo

        // Must resolve to CustomWorkflowBotRuntime even though ID/name contain "commander" / "music"
        val customCommander = BotRuntimeFactory.createRuntime("custom_commander_override", "Commander Custom Flow")
        assertTrue("Custom definition must take precedence over CommanderBotRuntime", customCommander is CustomWorkflowBotRuntime)

        val customMusic = BotRuntimeFactory.createRuntime("custom_music_stream_bot", "Music Stream Custom Flow")
        assertTrue("Custom definition must take precedence over MusicBotRuntime", customMusic is CustomWorkflowBotRuntime)

        // Prefix routing
        assertTrue(BotRuntimeFactory.createRuntime("bot_custom_9999") is CustomWorkflowBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("custom_wf_8888") is CustomWorkflowBotRuntime)
    }

    @Test
    fun `runtime instances have isolated states and do not share state flows`() {
        val instanceA = BotRuntimeFactory.createRuntime("bot_commander", "Commander A")
        val instanceB = BotRuntimeFactory.createRuntime("bot_commander", "Commander B")

        // Instances must be separate objects
        assertTrue("Each factory call must create a distinct instance", instanceA !== instanceB)
        assertTrue("StateFlow must not be shared between instances", instanceA.state !== instanceB.state)
        assertEquals(instanceA.state.value, instanceB.state.value)
    }

    @Test
    fun `concurrent fleet resolution across 20 parallel coroutines`() = runBlocking(Dispatchers.Default) {
        val deferredList = all20FlagshipBots.map { (id, name) ->
            async {
                val runtime = BotRuntimeFactory.createRuntime(id, name)
                id to runtime
            }
        }

        val results = deferredList.awaitAll()
        assertEquals(20, results.size)

        for ((expectedId, runtime) in results) {
            assertNotNull("Runtime for '$expectedId' must not be null under concurrent load", runtime)
            assertEquals("botId must match under concurrent load", expectedId, runtime.botId)
            assertEquals(BotRuntimeState.Stopped, runtime.state.value)
        }
    }

    @Test
    fun `multi-bot lifecycle readiness verification`() {
        val fleet = listOf(
            "bot_commander",
            "bot_music_streamer",
            "bot_exon",
            "bot_ai_assistant",
            "bot_habit_forge",
            "bot_penny_wise",
            "bot_sky_pulse"
        ).map { BotRuntimeFactory.createRuntime(it) }

        for (bot in fleet) {
            assertEquals("State must be Stopped before initialization", BotRuntimeState.Stopped, bot.state.value)
            assertTrue("botId must not be blank", bot.botId.isNotBlank())
        }
    }
}
