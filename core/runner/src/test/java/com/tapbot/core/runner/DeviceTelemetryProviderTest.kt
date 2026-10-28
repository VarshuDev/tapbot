package com.tapbot.core.runner

import com.tapbot.core.runner.telemetry.BatteryTelemetry
import com.tapbot.core.runner.telemetry.DeviceTelemetryProvider
import com.tapbot.core.runner.telemetry.NetworkTelemetry
import com.tapbot.core.runner.telemetry.RamTelemetry
import com.tapbot.core.runner.telemetry.StorageTelemetry
import com.tapbot.core.runner.telemetry.SystemHudTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTelemetryProviderTest {

    @Test
    fun `default provider without context returns sensible fallback telemetry`() {
        val provider = DeviceTelemetryProvider(null)

        val battery = provider.getBatteryTelemetry()
        assertTrue(battery.percentage in 0..100)
        assertTrue(battery.voltageMv > 0)
        assertNotNull(battery.health)

        val ram = provider.getRamTelemetry()
        assertTrue(ram.totalMemMB > 0)
        assertTrue(ram.usedMemMB >= 0)

        val storage = provider.getStorageTelemetry()
        assertTrue(storage.totalGB > 0)
        assertTrue(storage.progressBar.contains("█") || storage.progressBar.contains("░"))

        val network = provider.getNetworkTelemetry()
        assertNotNull(network.transport)

        val hud = provider.getSystemHudTelemetry()
        assertNotNull(hud.deviceModel)
        assertNotNull(hud.uptimeFormatted)
    }

    @Test
    fun `formatStatusHud produces valid markdown with telemetry values`() {
        val provider = DeviceTelemetryProvider(null)

        val hud = SystemHudTelemetry(
            battery = BatteryTelemetry(
                percentage = 74,
                isCharging = false,
                powerSource = "Battery Power 🔋",
                temperatureCelsius = 31.2,
                voltageMv = 3980,
                health = "Good ✅",
                isDozeWhitelisted = true
            ),
            ram = RamTelemetry(
                totalMemMB = 8192,
                availMemMB = 3500,
                usedMemMB = 4692,
                usedPct = 57,
                jvmTotalMB = 128,
                jvmUsedMB = 48,
                jvmMaxMB = 512,
                isLowMem = false
            ),
            storage = StorageTelemetry(
                totalGB = 128.0,
                freeGB = 45.0,
                usedGB = 83.0,
                usedPct = 64,
                progressBar = "██████░░░░"
            ),
            network = NetworkTelemetry(
                transport = "Wi-Fi 📶",
                isConnected = true,
                details = "Connected • 5 GHz"
            ),
            uptimeFormatted = "2d 4h",
            deviceModel = "Google Pixel 8",
            androidVersion = "Android 14 (API 34)"
        )

        val formatted = provider.formatStatusHud(hud, botFleetSummary = "• Exon Robot: Running 🟢")

        assertTrue(formatted.contains("TapBot Host System Dashboard"))
        assertTrue(formatted.contains("Google Pixel 8"))
        assertTrue(formatted.contains("74%"))
        assertTrue(formatted.contains("Discharging"))
        assertTrue(formatted.contains("4692 MB / 8192 MB"))
        assertTrue(formatted.contains("██████░░░░"))
        assertTrue(formatted.contains("Wi-Fi 📶"))
        assertTrue(formatted.contains("Exon Robot: Running 🟢"))
    }

    @Test
    fun `formatBatteryReport includes critical warning when battery is low and discharging`() {
        val provider = DeviceTelemetryProvider(null)

        val lowBattery = BatteryTelemetry(
            percentage = 12,
            isCharging = false,
            powerSource = "Battery Power 🔋",
            temperatureCelsius = 34.0,
            voltageMv = 3650,
            health = "Good ✅",
            isDozeWhitelisted = true
        )

        val report = provider.formatBatteryReport(lowBattery)

        assertTrue(report.contains("Detailed Battery Diagnostics"))
        assertTrue(report.contains("12%"))
        assertTrue(report.contains("CRITICAL") || report.contains("WARNING") || report.contains("critically low"))
    }

    @Test
    fun `formatBatteryReport includes batteryguide hint when not doze whitelisted`() {
        val provider = DeviceTelemetryProvider(null)

        val nonWhitelisted = BatteryTelemetry(
            percentage = 90,
            isCharging = true,
            powerSource = "AC Wall Charger ⚡",
            temperatureCelsius = 28.0,
            voltageMv = 4250,
            health = "Good ✅",
            isDozeWhitelisted = false
        )

        val report = provider.formatBatteryReport(nonWhitelisted)

        assertTrue(report.contains("Restricted by OS") || report.contains("/batteryguide"))
    }
}
