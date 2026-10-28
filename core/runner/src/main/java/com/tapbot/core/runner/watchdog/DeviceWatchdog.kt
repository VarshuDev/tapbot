package com.tapbot.core.runner.watchdog

import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.runner.telemetry.BatteryTelemetry

sealed class WatchdogAlert {
    data class LowBattery(val percentage: Int, val threshold: Int) : WatchdogAlert()
    data class BotCrash(val botId: String, val botName: String, val reason: String) : WatchdogAlert()
}

/**
 * Autonomous background watchdog monitoring hardware health (battery levels) and bot fleet lifecycle.
 * Debounces battery alerts (defaults to 30 min) and deduplicates crash alerts per instance failure.
 */
class DeviceWatchdog(
    private val debounceMillis: Long = 30 * 60 * 1000L // 30 minutes
) {
    private var lastLowBatteryAlertTime: Long = 0L
    private val alertedCrashIds = mutableSetOf<String>()

    /**
     * Evaluates current battery telemetry and returns an alert if battery <= threshold, discharging,
     * and debouncing interval has elapsed.
     */
    fun checkBattery(
        battery: BatteryTelemetry,
        threshold: Int = 15,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): WatchdogAlert.LowBattery? {
        if (battery.percentage > 0 && battery.percentage <= threshold && !battery.isCharging) {
            if (lastLowBatteryAlertTime == 0L || currentTimeMillis - lastLowBatteryAlertTime >= debounceMillis) {
                lastLowBatteryAlertTime = currentTimeMillis
                return WatchdogAlert.LowBattery(battery.percentage, threshold)
            }
        }
        return null
    }

    /**
     * Checks installed bot instances for newly crashed bots.
     * Returns a list of new crash alerts. Each crash reason per bot installation is alerted once.
     */
    fun checkCrashes(instances: List<BotInstance>): List<WatchdogAlert.BotCrash> {
        val alerts = mutableListOf<WatchdogAlert.BotCrash>()
        for (inst in instances) {
            if (inst.status is BotInstanceStatus.Crashed) {
                val reason = (inst.status as BotInstanceStatus.Crashed).reason
                val crashKey = "${inst.installationId}:${reason}"
                if (alertedCrashIds.add(crashKey)) {
                    alerts.add(WatchdogAlert.BotCrash(inst.botId, inst.name, reason))
                }
            }
        }
        return alerts
    }

    fun formatBatteryAlert(alert: WatchdogAlert.LowBattery): String {
        return """
            ⚠️ *LOW BATTERY WARNING!*
            ━━━━━━━━━━━━━━━━━━━━━━
            The host phone running your TapBot fleet is at *${alert.percentage}%* and discharging!
            (Threshold: ${alert.threshold}%)
            
            Please connect a charger to prevent Android from shutting down your bots.
        """.trimIndent()
    }

    fun formatCrashAlert(alert: WatchdogAlert.BotCrash): String {
        return """
            🚨 *BOT CRASH DETECTED!*
            ━━━━━━━━━━━━━━━━━━━━━━
            Bot: *${escapeMarkdown(alert.botName)}* (`${escapeMarkdown(alert.botId)}`)
            Reason: `${escapeMarkdown(alert.reason)}`
            
            Action required:
            • Run `/logs ${alert.botId}` to inspect crash stack.
            • Run `/restartbot ${alert.botId}` to recover.
        """.trimIndent()
    }

    fun resetCrashAlerts() {
        alertedCrashIds.clear()
    }

    fun resetBatteryDebounce() {
        lastLowBatteryAlertTime = 0L
    }

    private fun escapeMarkdown(text: String): String {
        return text.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`")
    }
}
