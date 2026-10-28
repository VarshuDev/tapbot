package com.tapbot.core.runner

import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.runner.telemetry.BatteryTelemetry
import com.tapbot.core.runner.watchdog.DeviceWatchdog
import com.tapbot.core.runner.watchdog.WatchdogAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceWatchdogTest {

    private lateinit var watchdog: DeviceWatchdog

    @Before
    fun setUp() {
        watchdog = DeviceWatchdog(debounceMillis = 30 * 60 * 1000L) // 30 minutes
    }

    @Test
    fun `checkBattery triggers alert when below threshold and discharging`() {
        val lowBattery = BatteryTelemetry(
            percentage = 12,
            isCharging = false,
            powerSource = "Battery Power 🔋",
            temperatureCelsius = 31.0,
            voltageMv = 3600,
            health = "Good ✅"
        )

        val alert = watchdog.checkBattery(lowBattery, threshold = 15, currentTimeMillis = 1000L)
        assertNotNull(alert)
        assertEquals(12, alert?.percentage)
        assertEquals(15, alert?.threshold)

        val formatted = watchdog.formatBatteryAlert(alert!!)
        assertTrue(formatted.contains("12%"))
        assertTrue(formatted.contains("Threshold: 15%"))
    }

    @Test
    fun `checkBattery does not trigger when charging or above threshold`() {
        // Low battery but charging
        val chargingBattery = BatteryTelemetry(
            percentage = 10,
            isCharging = true,
            powerSource = "AC Wall Charger ⚡",
            temperatureCelsius = 32.0,
            voltageMv = 3800,
            health = "Good ✅"
        )
        assertNull(watchdog.checkBattery(chargingBattery, threshold = 15, currentTimeMillis = 1000L))

        // Normal battery (above threshold)
        val normalBattery = BatteryTelemetry(
            percentage = 50,
            isCharging = false,
            powerSource = "Battery Power 🔋",
            temperatureCelsius = 30.0,
            voltageMv = 3900,
            health = "Good ✅"
        )
        assertNull(watchdog.checkBattery(normalBattery, threshold = 15, currentTimeMillis = 1000L))
    }

    @Test
    fun `checkBattery debounces alerts for 30 minutes`() {
        val lowBattery = BatteryTelemetry(
            percentage = 9,
            isCharging = false,
            powerSource = "Battery Power 🔋",
            temperatureCelsius = 31.0,
            voltageMv = 3550,
            health = "Good ✅"
        )

        val t0 = 1_000_000L
        // 1st alert triggers
        assertNotNull(watchdog.checkBattery(lowBattery, threshold = 15, currentTimeMillis = t0))

        // 10 minutes later (600,000 ms) -> suppressed
        assertNull(watchdog.checkBattery(lowBattery, threshold = 15, currentTimeMillis = t0 + 600_000L))

        // 25 minutes later (1,500,000 ms) -> suppressed
        assertNull(watchdog.checkBattery(lowBattery, threshold = 15, currentTimeMillis = t0 + 1_500_000L))

        // 30 minutes later (1,800,000 ms) -> triggers again
        assertNotNull(watchdog.checkBattery(lowBattery, threshold = 15, currentTimeMillis = t0 + 1_800_000L))
    }

    @Test
    fun `checkCrashes detects newly crashed bots and deduplicates consecutive checks`() {
        val runningBot = BotInstance(
            installationId = "inst_1",
            botId = "bot_echo",
            name = "Echo Bot",
            version = "1.0.0",
            status = BotInstanceStatus.Running(pollCount = 50, messageCount = 5)
        )
        val crashedBot = BotInstance(
            installationId = "inst_2",
            botId = "bot_crypto",
            name = "Crypto Bot",
            version = "2.0.0",
            status = BotInstanceStatus.Crashed(reason = "NullPointerException in main loop")
        )

        // 1st check: Detects crashedBot
        val alerts1 = watchdog.checkCrashes(listOf(runningBot, crashedBot))
        assertEquals(1, alerts1.size)
        assertEquals("bot_crypto", alerts1[0].botId)
        assertEquals("Crypto Bot", alerts1[0].botName)
        assertEquals("NullPointerException in main loop", alerts1[0].reason)

        val formatted = watchdog.formatCrashAlert(alerts1[0])
        assertTrue(formatted.contains("Crypto Bot"))
        assertTrue(formatted.contains("NullPointerException in main loop"))
        assertTrue(formatted.contains("/restartbot bot_crypto"))

        // 2nd check: Same crash state -> deduplicated (empty alerts)
        val alerts2 = watchdog.checkCrashes(listOf(runningBot, crashedBot))
        assertTrue(alerts2.isEmpty())
    }
}
