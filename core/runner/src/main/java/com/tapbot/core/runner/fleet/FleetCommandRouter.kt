package com.tapbot.core.runner.fleet

import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.runner.manager.BotInstanceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Router handling multi-bot fleet operations and formatting for Telegram Commander commands.
 * Handles /bots, /startbot, /stopbot, /restartbot, and /logs with log sanitization.
 */
class FleetCommandRouter(
    private val manager: BotInstanceManager? = null
) {

    fun formatFleetStatus(instances: List<BotInstance>): String {
        if (instances.isEmpty()) {
            return "🤖 *No bots installed yet.*\nVisit the TapBot app or run /store to explore available bots."
        }

        val sb = StringBuilder("🤖 *TapBot Installed Fleet (${instances.size})*\n━━━━━━━━━━━━━━━━━━━━━━\n")
        for (inst in instances) {
            val statusBadge = when (inst.status) {
                is BotInstanceStatus.Running -> "🟢 RUNNING"
                is BotInstanceStatus.Starting -> "🟡 STARTING"
                is BotInstanceStatus.Stopping -> "🟠 STOPPING"
                is BotInstanceStatus.Stopped -> "⚪ STOPPED"
                is BotInstanceStatus.Crashed -> "🔴 CRASHED"
            }

            sb.append("• *${escapeMarkdown(inst.name)}* (`${escapeMarkdown(inst.botId)}`)\n")
            sb.append("   Version: v${inst.version} | Status: *${statusBadge}*\n")

            if (inst.status is BotInstanceStatus.Running) {
                val r = inst.status as BotInstanceStatus.Running
                sb.append("   Polls: `${r.pollCount}` | Messages: `${r.messageCount}`\n")
            } else if (inst.status is BotInstanceStatus.Crashed) {
                val c = inst.status as BotInstanceStatus.Crashed
                sb.append("   Reason: `${escapeMarkdown(c.reason)}`\n")
            }
            sb.append("\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Manage: /startbot `<id>`, /stopbot `<id>`, /logs `<id>`")
        return sb.toString()
    }

    suspend fun startBot(botId: String): Result<String> {
        val mgr = manager ?: return Result.failure(IllegalStateException("BotInstanceManager is unavailable"))
        val inst = mgr.getInstanceByBotId(botId) ?: mgr.getInstance(botId)
            ?: return Result.failure(IllegalArgumentException("Bot with ID `${escapeMarkdown(botId)}` not found."))

        val res = mgr.start(inst.installationId)
        return if (res.isSuccess) {
            Result.success("✅ Started bot *${escapeMarkdown(inst.name)}* successfully.")
        } else {
            val err = res.exceptionOrNull()?.message ?: "Unknown error"
            Result.failure(Exception("Failed to start bot: `${escapeMarkdown(err)}`"))
        }
    }

    suspend fun stopBot(botId: String): Result<String> {
        val mgr = manager ?: return Result.failure(IllegalStateException("BotInstanceManager is unavailable"))
        val inst = mgr.getInstanceByBotId(botId) ?: mgr.getInstance(botId)
            ?: return Result.failure(IllegalArgumentException("Bot with ID `${escapeMarkdown(botId)}` not found."))

        val res = mgr.stop(inst.installationId)
        return if (res.isSuccess) {
            Result.success("⏹ Stopped bot *${escapeMarkdown(inst.name)}* successfully.")
        } else {
            val err = res.exceptionOrNull()?.message ?: "Unknown error"
            Result.failure(Exception("Failed to stop bot: `${escapeMarkdown(err)}`"))
        }
    }

    suspend fun restartBot(botId: String): Result<String> {
        val mgr = manager ?: return Result.failure(IllegalStateException("BotInstanceManager is unavailable"))
        val inst = mgr.getInstanceByBotId(botId) ?: mgr.getInstance(botId)
            ?: return Result.failure(IllegalArgumentException("Bot with ID `${escapeMarkdown(botId)}` not found."))

        val res = mgr.restart(inst.installationId)
        return if (res.isSuccess) {
            Result.success("🔄 Restarted bot *${escapeMarkdown(inst.name)}* successfully.")
        } else {
            val err = res.exceptionOrNull()?.message ?: "Unknown error"
            Result.failure(Exception("Failed to restart bot: `${escapeMarkdown(err)}`"))
        }
    }

    fun formatLogs(botId: String, entries: List<BotLogEntry>, lineCount: Int = 15): String {
        val recent = entries.takeLast(lineCount)
        if (recent.isEmpty()) {
            return "📄 No recent log entries for `${escapeMarkdown(botId)}`."
        }

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        val sb = StringBuilder("📄 *Live Logs for `${escapeMarkdown(botId)}` (last ${recent.size})*\n```\n")
        for (e in recent) {
            val time = timeFormat.format(Date(e.timestamp))
            val sanitized = sanitizeLogLine(e.message)
            sb.append("[$time] [${e.level.name}] [${e.tag}] $sanitized\n")
        }
        sb.append("```")
        return sb.toString()
    }

    fun sanitizeLogLine(message: String): String {
        val tokenRegex = """\b\d{8,10}:[A-Za-z0-9_-]{35}\b""".toRegex()
        return message.replace(tokenRegex, "[REDACTED_BOT_TOKEN]")
    }

    private fun escapeMarkdown(text: String): String {
        return text.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("`", "\\`")
    }
}
