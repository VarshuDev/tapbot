package com.tapbot.core.runner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tapbot.core.model.LogLevel

/**
 * BroadcastReceiver triggered by the Android system after device reboot or app package update.
 *
 * Supported Actions:
 * - android.intent.action.BOOT_COMPLETED (Device reboot)
 * - android.intent.action.QUICKBOOT_POWERON (Fastboot / quickboot on select OEM devices)
 * - android.intent.action.MY_PACKAGE_REPLACED (TapBot app update installed)
 *
 * Scenarios Addressed:
 * - Scenario 12: Device reboot (automatic service recovery)
 * - Scenario 15: App update (recovery across package upgrade)
 *
 * Note: Under Android 12+ (API 31+), ACTION_BOOT_COMPLETED and ACTION_MY_PACKAGE_REPLACED are
 * explicit system-exempted triggers that permit starting a foreground service from the background.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        try {
            val manager = ServiceLocator.botInstanceManager
            val runningInstances = manager?.instances?.value?.filter { it.status.isRunning } ?: emptyList()

            // If bots were running prior to reboot / update, auto-recover them
            if (runningInstances.isNotEmpty()) {
                val serviceIntent = Intent(context, BotForegroundService::class.java).apply {
                    this.action = BotForegroundService.ACTION_RESTART_BOT
                    putExtra(BotForegroundService.EXTRA_INSTALLATION_ID, runningInstances.first().installationId)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                ServiceLocator.logRepository?.appendLog(
                    botId = runningInstances.first().botId,
                    level = LogLevel.INFO,
                    tag = TAG,
                    message = "Auto-recovering ${runningInstances.size} bot(s) after system event: $action"
                )
            }
        } catch (e: Exception) {
            ServiceLocator.logRepository?.appendLog(
                botId = "system",
                level = LogLevel.WARN,
                tag = TAG,
                message = "Failed to auto-recover bots on boot: ${e.message}"
            )
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
