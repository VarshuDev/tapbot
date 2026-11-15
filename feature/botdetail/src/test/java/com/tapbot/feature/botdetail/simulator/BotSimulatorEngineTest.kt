package com.tapbot.feature.botdetail.simulator

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotSimulatorEngineTest {

    @Test
    fun `generateWelcome contains bot name and interactive sandbox instructions`() {
        val welcome = BotSimulatorEngine.generateWelcome("HabitForge Daily Tracker")
        assertTrue(welcome.contains("HabitForge Daily Tracker"))
        assertTrue(welcome.contains("interactive sandbox"))
        assertTrue(welcome.contains("/help"))
    }

    @Test
    fun `generateHelp returns grouped command catalog for bot`() {
        val help = BotSimulatorEngine.generateHelp(
            botId = "bot_habit_forge",
            botName = "HabitForge Daily Tracker",
            category = "Daily Lifestyle"
        )
        assertTrue(help.contains("Command Directory"))
        assertTrue(help.contains("/start"))
        assertTrue(help.contains("/done"))
        assertTrue(help.contains("/habits"))
    }

    @Test
    fun `dispatchCommand handles core and domain specific commands correctly`() {
        val statusReply = BotSimulatorEngine.dispatchCommand(
            input = "/status",
            botId = "bot_commander",
            botName = "TapBot Commander",
            category = "System & Automation"
        )
        assertTrue(statusReply.contains("Live Sandbox Status"))
        assertTrue(statusReply.contains("Healthy & Responsive"))

        val quoteReply = BotSimulatorEngine.dispatchCommand(
            input = "/quote",
            botId = "bot_habit_forge",
            botName = "HabitForge Daily Tracker",
            category = "Daily Lifestyle"
        )
        assertTrue(quoteReply.contains("Daily Inspiration"))

        val weatherReply = BotSimulatorEngine.dispatchCommand(
            input = "/weather Tokyo",
            botId = "bot_sky_pulse",
            botName = "SkyPulse Weather HUD",
            category = "Daily Utilities"
        )
        assertTrue(weatherReply.contains("Weather Report for Tokyo"))

        val cryptoReply = BotSimulatorEngine.dispatchCommand(
            input = "/crypto eth",
            botId = "bot_coin_radar",
            botName = "CoinRadar Market Tracker",
            category = "Personal Finance"
        )
        assertTrue(cryptoReply.contains("Market Radar: ETH"))

        val timerReply = BotSimulatorEngine.dispatchCommand(
            input = "/timer 25",
            botId = "bot_pomo_focus",
            botName = "PomoFocus Sprint Timer",
            category = "Productivity"
        )
        assertTrue(timerReply.contains("Pomodoro Sprint Started!"))
    }

    @Test
    fun `dispatchCommand handles unknown and raw text gracefully`() {
        val unknown = BotSimulatorEngine.dispatchCommand(
            input = "/nonexistent_command_xyz",
            botId = "bot_commander",
            botName = "TapBot Commander",
            category = "System"
        )
        assertTrue(unknown.contains("Unknown Command") || unknown.contains("/help"))

        val plainText = BotSimulatorEngine.dispatchCommand(
            input = "Hello bot",
            botId = "bot_commander",
            botName = "TapBot Commander",
            category = "System"
        )
        assertTrue(plainText.contains("Message Received") && plainText.contains("Hello bot"))
    }
}
