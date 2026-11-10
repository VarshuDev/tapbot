package com.tapbot.core.ui.catalog

import com.tapbot.core.model.CustomBotDefinition

data class BotCommandEntry(
    val command: String,
    val summary: String,
    val category: String
)

/**
 * Single-source-of-truth command registry derived directly from implemented on-device runtimes.
 */
object BotCommandCatalog {

    private val commanderCommands = listOf(
        BotCommandEntry("/start", "Show authorized owner welcome & quick menu", "General"),
        BotCommandEntry("/help", "List every Commander telemetry & control command", "General"),
        BotCommandEntry("/status", "Full host HUD: battery, RAM, disk, network & uptime", "Telemetry"),
        BotCommandEntry("/battery", "Battery charge, temperature, voltage & Doze status", "Telemetry"),
        BotCommandEntry("/ram", "Physical memory & ART heap breakdown", "Telemetry"),
        BotCommandEntry("/storage", "Internal storage free & used capacity", "Telemetry"),
        BotCommandEntry("/network", "Active Wi-Fi / 5G transport details", "Telemetry"),
        BotCommandEntry("/bots", "Live status of every installed TapBot node", "Fleet"),
        BotCommandEntry("/startbot <id>", "Start an installed bot by ID or name", "Fleet"),
        BotCommandEntry("/stopbot <id>", "Stop a running bot safely", "Fleet"),
        BotCommandEntry("/restartbot <id>", "Restart a bot runtime cleanly", "Fleet"),
        BotCommandEntry("/logs <id> [lines]", "Read recent redacted execution logs in chat", "Fleet"),
        BotCommandEntry("/ring [sec]", "Play phone finder alarm tone remotely", "Device"),
        BotCommandEntry("/stopring", "Silence active phone finder alarm", "Device"),
        BotCommandEntry("/torch [on|off]", "Toggle phone camera LED flashlight", "Device"),
        BotCommandEntry("/tts <text>", "Speak text out loud via Android TextToSpeech", "Device"),
        BotCommandEntry("/notify <msg>", "Post a local Android notification banner", "Device"),
        BotCommandEntry("/schedule <delay> <msg>", "Schedule a delayed Telegram alert", "Device"),
        BotCommandEntry("/apk", "Send installed TapBot APK directly in chat", "Delivery")
    )

    private val musicCommands = listOf(
        BotCommandEntry("/start", "Show TapStream audio guide & controls", "General"),
        BotCommandEntry("/play <query|url>", "Resolve song/stream and play via speaker or chat", "Playback"),
        BotCommandEntry("/radio [lofi|synthwave|jazz]", "Tune into curated 24/7 internet radio streams", "Playback"),
        BotCommandEntry("/pause", "Pause active phone speaker playback", "Playback"),
        BotCommandEntry("/resume", "Resume paused audio playback", "Playback"),
        BotCommandEntry("/skip", "Skip to the next queued track", "Queue"),
        BotCommandEntry("/queue", "View active playlist queue", "Queue"),
        BotCommandEntry("/np", "Interactive Now Playing card with inline buttons", "Queue"),
        BotCommandEntry("/volume <0-100>", "Adjust host phone media volume percentage", "Audio"),
        BotCommandEntry("/mode [speaker|chat|both]", "Route audio to speaker, chat, or both", "Audio"),
        BotCommandEntry("/loop [off|one|all]", "Cycle queue repeat behavior", "Queue"),
        BotCommandEntry("/shuffle", "Shuffle upcoming tracks in queue", "Queue"),
        BotCommandEntry("/seek <mm:ss|sec>", "Jump to timestamp inside current track", "Playback"),
        BotCommandEntry("/stop", "Stop audio playback and clear queue", "Playback")
    )

    private val exonCommands = listOf(
        BotCommandEntry("/start", "Launch group guard greeting & inline help", "General"),
        BotCommandEntry("/help", "Open interactive category command menus", "General"),
        BotCommandEntry("/ban", "Ban replied/target user from group", "Moderation"),
        BotCommandEntry("/tban <10m|2h|1d>", "Temporarily ban a user for a duration", "Moderation"),
        BotCommandEntry("/mute", "Restrict user from sending messages", "Moderation"),
        BotCommandEntry("/tmute <10m|2h|1d>", "Temporarily mute a user", "Moderation"),
        BotCommandEntry("/warn", "Issue strike warning (auto-ban at limit)", "Moderation"),
        BotCommandEntry("/lock <links|sticker|bots|all>", "Enforce automatic message deletion locks", "Protection"),
        BotCommandEntry("/unlock <type>", "Remove a previously enabled group lock", "Protection"),
        BotCommandEntry("/antiflood <count|off>", "Auto-mute flooders exceeding messages per 10s", "Protection"),
        BotCommandEntry("/setwelcome <text>", "Customize group welcome template ({first}, {chat})", "Group"),
        BotCommandEntry("/rules", "Show persisted group rules card", "Group"),
        BotCommandEntry("/promote", "Promote user to chat administrator", "Admin"),
        BotCommandEntry("/pin", "Pin the replied message in the group", "Admin")
    )

    private val aiCommands = listOf(
        BotCommandEntry("/start", "Show active Gemini model & AI capabilities", "General"),
        BotCommandEntry("/ask <prompt>", "Ask Gemini anything with per-chat memory", "AI"),
        BotCommandEntry("/summarize [text]", "Summarize text or a replied message", "AI"),
        BotCommandEntry("/code <language & task>", "Generate clean code snippets & explanation", "AI"),
        BotCommandEntry("/model [name]", "Inspect or switch Gemini model per chat", "Config"),
        BotCommandEntry("/system [prompt|clear]", "Set or clear persistent system instruction", "Config"),
        BotCommandEntry("/clear", "Reset sliding-window conversation history", "Config"),
        BotCommandEntry("/status", "View API key health, memory count & uptime", "Diagnostics")
    )

    private val rssCommands = listOf(
        BotCommandEntry("/start", "Show RSS / Atom broadcaster command guide", "General"),
        BotCommandEntry("/feeds", "List all subscribed RSS/Atom feed URLs", "Feeds"),
        BotCommandEntry("/addfeed <url>", "Subscribe to a new RSS 2.0 or Atom feed", "Feeds"),
        BotCommandEntry("/removefeed <url|#>", "Unsubscribe from a feed by URL or index", "Feeds"),
        BotCommandEntry("/checknow", "Poll all configured feeds immediately", "Broadcast"),
        BotCommandEntry("/interval [minutes]", "Configure scan cadence (1–1440 minutes)", "Broadcast"),
        BotCommandEntry("/settarget [chat_id]", "Bind broadcast output to channel/chat ID", "Broadcast"),
        BotCommandEntry("/status", "View active feed count & seen GUID stats", "Diagnostics")
    )

    private val utilityCommands = listOf(
        BotCommandEntry("/start", "Welcome menu & diagnostics overview", "General"),
        BotCommandEntry("/ping", "Verify Android ART runner roundtrip latency", "Diagnostics"),
        BotCommandEntry("/uptime", "Report active foreground runner uptime", "Diagnostics"),
        BotCommandEntry("/sysinfo", "Inspect ART JVM heap & active threads", "Diagnostics"),
        BotCommandEntry("/echo <text>", "Echo payload cleanly back to chat", "Tools"),
        BotCommandEntry("/crash", "Trigger controlled crash test for recovery verification", "Resilience")
    )

    private val pingPongCommands = listOf(
        BotCommandEntry("/start", "Verify local PingPong runtime greeting", "General"),
        BotCommandEntry("ping", "Returns immediate 'pong' health check reply", "Diagnostics")
    )

    fun getCommandsForBot(
        botId: String,
        customDefinition: CustomBotDefinition? = null,
        category: String = "",
        name: String = ""
    ): List<BotCommandEntry> {
        if (customDefinition != null || botId.startsWith("bot_custom_") || botId.startsWith("custom_wf_")) {
            val dynamicRules = customDefinition?.rules.orEmpty().mapNotNull { rule ->
                val cmd = rule.command.trim()
                if (cmd.isBlank()) return@mapNotNull null
                val webhookNote = if (!rule.webhookUrl.isNullOrBlank()) " (${rule.webhookMethod.name} webhook)" else ""
                val summary = rule.description.trim().ifBlank {
                    "Replied: ${rule.responseTemplate.trim().take(48)}"
                } + webhookNote
                BotCommandEntry(
                    command = cmd,
                    summary = summary,
                    category = if (!rule.webhookUrl.isNullOrBlank()) "Webhook" else "Rule"
                )
            }
            val builtIns = listOf(
                BotCommandEntry("/start", "Send welcome template & list rules", "Built-in"),
                BotCommandEntry("/help", "Display active custom workflow commands", "Built-in")
            )
            val seen = HashSet<String>()
            val merged = mutableListOf<BotCommandEntry>()
            for (entry in dynamicRules + builtIns) {
                if (seen.add(entry.command.lowercase())) {
                    merged.add(entry)
                }
            }
            return merged
        }

        val normalized = "$botId $category $name".lowercase().replace("-", "_")
        return when {
            normalized.contains("commander") || normalized.contains("sentinel") || normalized.contains("fleet") -> commanderCommands
            normalized.contains("music") || normalized.contains("tapstream") || normalized.contains("stream") -> musicCommands
            normalized.contains("exon") || botId.startsWith("bot_1790084493920") -> exonCommands
            normalized.contains("rss") || normalized.contains("notifier") -> rssCommands
            normalized.contains("ai") || normalized.contains("gemini") -> aiCommands
            normalized.contains("pingpong") -> pingPongCommands
            normalized.contains("util") || normalized.contains("echo") -> utilityCommands
            else -> utilityCommands
        }
    }

    fun getPreviewCommands(
        botId: String,
        customDefinition: CustomBotDefinition? = null,
        category: String = "",
        name: String = "",
        limit: Int = 3
    ): List<String> {
        val entries = getCommandsForBot(botId, customDefinition, category, name)
        val nonGeneric = entries.filter { !it.command.equals("/start", ignoreCase = true) && !it.command.equals("/help", ignoreCase = true) }
        val pool = nonGeneric.ifEmpty { entries }
        return pool.take(limit).map { entry ->
            entry.command.substringBefore(" ").trim()
        }
    }
}
