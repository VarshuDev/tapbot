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

    // 1. TapBot Commander (19 commands)
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

    // 2. TapStream Music (14 commands)
    private val musicCommands = listOf(
        BotCommandEntry("/start", "Show TapStream audio guide & controls", "General"),
        BotCommandEntry("/play <query|url>", "Resolve song/stream and play via speaker or chat", "Playback"),
        BotCommandEntry("/radio [station]", "Tune into curated 24/7 internet radio streams", "Playback"),
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

    // 3. Exon Robot Sentinel (14 commands)
    private val exonCommands = listOf(
        BotCommandEntry("/start", "Launch group guard greeting & inline help", "General"),
        BotCommandEntry("/help", "Open interactive category command menus", "General"),
        BotCommandEntry("/ban", "Ban replied/target user from group", "Moderation"),
        BotCommandEntry("/tban <10m|2h|1d>", "Temporarily ban a user for a duration", "Moderation"),
        BotCommandEntry("/mute", "Restrict user from sending messages", "Moderation"),
        BotCommandEntry("/tmute <10m|2h|1d>", "Temporarily mute a user", "Moderation"),
        BotCommandEntry("/warn", "Issue strike warning (auto-ban at limit)", "Moderation"),
        BotCommandEntry("/lock <type>", "Enforce automatic message deletion locks", "Protection"),
        BotCommandEntry("/unlock <type>", "Remove a previously enabled group lock", "Protection"),
        BotCommandEntry("/antiflood <count|off>", "Auto-mute flooders exceeding messages per 10s", "Protection"),
        BotCommandEntry("/setwelcome <text>", "Customize group welcome template", "Group"),
        BotCommandEntry("/rules", "Show persisted group rules card", "Group"),
        BotCommandEntry("/promote", "Promote user to chat administrator", "Admin"),
        BotCommandEntry("/pin", "Pin the replied message in the group", "Admin")
    )

    // 4. Gemini Smart Assistant (8 commands)
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

    // 5. HabitForge Daily Tracker (8 commands)
    private val habitForgeCommands = listOf(
        BotCommandEntry("/start", "Welcome guide & daily habit check-in dashboard", "General"),
        BotCommandEntry("/done <habit>", "Mark a habit as completed for today", "Check-in"),
        BotCommandEntry("/streak [habit]", "View current & longest streak analytics", "Analytics"),
        BotCommandEntry("/today", "Interactive checklist of remaining habits for today", "Check-in"),
        BotCommandEntry("/habits", "List all active tracked habits with streaks", "Habits"),
        BotCommandEntry("/addhabit <name>", "Register a new recurring habit to track", "Manage"),
        BotCommandEntry("/reset <habit>", "Reset or archive a tracked habit", "Manage"),
        BotCommandEntry("/stats", "Weekly completion percentage and momentum score", "Analytics")
    )

    // 6. PennyWise Expense Tracker (8 commands)
    private val pennyWiseCommands = listOf(
        BotCommandEntry("/start", "Overview of balance, monthly budget, and quick spend menu", "General"),
        BotCommandEntry("/spend <amt> <cat> [note]", "Log a new expense with category tag", "Expenses"),
        BotCommandEntry("/balance", "Show remaining monthly budget allowance", "Budget"),
        BotCommandEntry("/budget <amt>", "Set or update target monthly budget limit", "Budget"),
        BotCommandEntry("/stats [week|month]", "Category breakdown chart & average burn rate", "Analytics"),
        BotCommandEntry("/history [count]", "List recent transactions with timestamps", "Expenses"),
        BotCommandEntry("/categories", "Manage spending category tags", "Config"),
        BotCommandEntry("/export", "Export transaction ledger as CSV in chat", "Export")
    )

    // 7. PomoFocus Deep Work Timer (8 commands)
    private val pomoFocusCommands = listOf(
        BotCommandEntry("/start", "Launch Pomodoro HUD & current interval status", "General"),
        BotCommandEntry("/pomodoro [mins]", "Start a focus work sprint (default 25m)", "Focus"),
        BotCommandEntry("/shortbreak", "Start a 5-minute restorative rest break", "Break"),
        BotCommandEntry("/longbreak", "Start a 15-minute extended break after 4 sprints", "Break"),
        BotCommandEntry("/stop", "Cancel active timer interval", "Timer"),
        BotCommandEntry("/status", "Check remaining time in active interval", "Timer"),
        BotCommandEntry("/stats", "Daily & weekly completed focus sprint counts", "Analytics"),
        BotCommandEntry("/task <name>", "Tag current focus session with a project name", "Task")
    )

    // 8. TapNote Scratchpad (8 commands)
    private val tapNoteCommands = listOf(
        BotCommandEntry("/start", "Open scratchpad menu and active todo count", "General"),
        BotCommandEntry("/note <text>", "Quick capture an unorganized note or thought", "Notes"),
        BotCommandEntry("/todo <task>", "Add a pending checklist action item", "Todos"),
        BotCommandEntry("/todos", "View active pending checklist items", "Todos"),
        BotCommandEntry("/check <id>", "Mark a todo item completed by index", "Todos"),
        BotCommandEntry("/journal", "Review today's captured thoughts & micro-journal", "Journal"),
        BotCommandEntry("/search <kw>", "Search stored notes and todos by keyword", "Search"),
        BotCommandEntry("/export", "Export all notes as a Markdown document in chat", "Export")
    )

    // 9. SkyPulse Weather Radar (8 commands)
    private val skyPulseCommands = listOf(
        BotCommandEntry("/start", "Current weather summary & briefing status", "General"),
        BotCommandEntry("/weather [city]", "Instant conditions, temperature, humidity & wind", "Conditions"),
        BotCommandEntry("/forecast [days]", "Multi-day high/low temperature & precipitation outlook", "Forecast"),
        BotCommandEntry("/radar", "Rain likelihood, precipitation intensity & cloud cover", "Radar"),
        BotCommandEntry("/alerts", "Severe weather warnings & high UV advisories", "Alerts"),
        BotCommandEntry("/setcity <city>", "Set default home city for daily automated briefings", "Config"),
        BotCommandEntry("/morningbrief [on|off]", "Toggle 7:00 AM daily weather briefing", "Schedule"),
        BotCommandEntry("/uv", "Current UV index rating and sun safety guidelines", "Conditions")
    )

    // 10. RemindMe Health & Pill Alarms (8 commands)
    private val remindMeCommands = listOf(
        BotCommandEntry("/start", "Active alarms overview & scheduling guide", "General"),
        BotCommandEntry("/remind <time> <msg>", "Schedule a reminder (e.g. 20m Call mom)", "Reminders"),
        BotCommandEntry("/reminders", "View all active scheduled alarms and countdowns", "Reminders"),
        BotCommandEntry("/water [interval]", "Start periodic hydration reminder (e.g. 60m)", "Health"),
        BotCommandEntry("/pill <name> <time>", "Set daily recurring medication alarm", "Health"),
        BotCommandEntry("/snooze <id> [mins]", "Snooze an active alarm (default 10m)", "Actions"),
        BotCommandEntry("/cancel <id>", "Delete an upcoming scheduled reminder", "Manage"),
        BotCommandEntry("/clear", "Cancel all upcoming reminders and timers", "Manage")
    )

    // 11. LinkDrop Link & Clipboard Vault (7 commands)
    private val linkDropCommands = listOf(
        BotCommandEntry("/start", "Clipboard bridge status & recent saved links", "General"),
        BotCommandEntry("/save <url> [tag]", "Save a bookmark with optional topic tag", "Vault"),
        BotCommandEntry("/links [tag]", "List recent saved URLs, filterable by tag", "Vault"),
        BotCommandEntry("/search <query>", "Search bookmarks by title, URL or tag", "Search"),
        BotCommandEntry("/copy <id>", "Copy saved text or URL to phone's Android clipboard", "Device"),
        BotCommandEntry("/archive <id>", "Mark a link as read/archived", "Manage"),
        BotCommandEntry("/tags", "List all bookmark categories and counts", "Vault")
    )

    // 12. PolyGlot Instant Translator (7 commands)
    private val polyGlotCommands = listOf(
        BotCommandEntry("/start", "Supported languages and quick translation instructions", "General"),
        BotCommandEntry("/tr <lang> <text>", "Translate text to target language code", "Translate"),
        BotCommandEntry("/detect <text>", "Detect source language and confidence score", "Detect"),
        BotCommandEntry("/languages", "List all supported ISO language codes", "Help"),
        BotCommandEntry("/word", "Daily vocabulary flashcard with definition and usage", "Learning"),
        BotCommandEntry("/quiz", "Quick 1-question vocabulary challenge", "Learning"),
        BotCommandEntry("/history", "View your recent translations", "History")
    )

    // 13. CoinRadar Crypto & Stock Watcher (8 commands)
    private val coinRadarCommands = listOf(
        BotCommandEntry("/start", "Market overview, top gainers, and portfolio summary", "General"),
        BotCommandEntry("/p <ticker>", "Instant live quote, 24h change & volume", "Quotes"),
        BotCommandEntry("/chart <ticker>", "7-day ASCII sparkline trend and price range", "Charts"),
        BotCommandEntry("/portfolio", "View held assets, total valuation & P&L percentage", "Portfolio"),
        BotCommandEntry("/add <ticker> <qty>", "Add asset holding to local portfolio", "Portfolio"),
        BotCommandEntry("/alert <ticker> <price>", "Set high/low price trigger Telegram notification", "Alerts"),
        BotCommandEntry("/alerts", "List active price alerts", "Alerts"),
        BotCommandEntry("/market", "Top 10 crypto assets by market capitalization", "Market")
    )

    // 14. FitTrack Hydration & Workouts (7 commands)
    private val fitTrackCommands = listOf(
        BotCommandEntry("/start", "Today's fitness rings (water, workout, calories)", "General"),
        BotCommandEntry("/water [amt_ml]", "Log water intake (default 250ml)", "Hydration"),
        BotCommandEntry("/workout <type> <mins>", "Log training (run, gym, bike, yoga, walk)", "Workouts"),
        BotCommandEntry("/calories", "View total active calories burned today", "Stats"),
        BotCommandEntry("/weight <kg|lbs>", "Log body weight check-in", "Body"),
        BotCommandEntry("/summary [week]", "Weekly activity report with total exercise minutes", "Stats"),
        BotCommandEntry("/target <calories>", "Set daily active calorie target", "Config")
    )

    // 15. ParcelTrack Delivery Watcher (7 commands)
    private val parcelTrackCommands = listOf(
        BotCommandEntry("/start", "Active shipments in transit and delivery overview", "General"),
        BotCommandEntry("/track <number> [label]", "Add a shipment tracking number with custom label", "Tracking"),
        BotCommandEntry("/packages", "List all active inbound parcels and latest status", "Shipments"),
        BotCommandEntry("/status <id>", "Detailed milestone timeline for a package", "Shipments"),
        BotCommandEntry("/delivered", "View history of completed deliveries", "Shipments"),
        BotCommandEntry("/carriers", "Supported courier services and format patterns", "Help"),
        BotCommandEntry("/remove <id>", "Stop tracking a package", "Manage")
    )

    // 16. NewsPulse RSS Broadcaster (8 commands)
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

    // 17. GitNotifier Repo Watcher (8 commands)
    private val gitNotifierCommands = listOf(
        BotCommandEntry("/start", "Active watched repository status & event feed", "General"),
        BotCommandEntry("/watch <owner/repo>", "Watch a GitHub or GitLab repository for events", "Watch"),
        BotCommandEntry("/unwatch <owner/repo>", "Remove repository from active monitoring", "Watch"),
        BotCommandEntry("/repos", "List all currently monitored repositories", "Watch"),
        BotCommandEntry("/issues [repo]", "View open issues and recent discussions", "Issues"),
        BotCommandEntry("/pulls [repo]", "View pending pull requests needing review", "Pulls"),
        BotCommandEntry("/ci [repo]", "Check latest GitHub Actions or GitLab CI workflow status", "CI"),
        BotCommandEntry("/stars [repo]", "Check repository star count & fork growth", "Stats")
    )

    // 18. CamSentry Hardware Security (7 commands)
    private val camSentryCommands = listOf(
        BotCommandEntry("/start", "Sentry status, battery guard, and security controls", "General"),
        BotCommandEntry("/snap [front|back]", "Capture photo from specified camera and send in chat", "Camera"),
        BotCommandEntry("/flashsnap", "Turn on LED flash and capture clear photo in darkness", "Camera"),
        BotCommandEntry("/status", "Device battery level, charging state, temperature, storage", "Telemetry"),
        BotCommandEntry("/sentry [on|off]", "Arm or disarm automated watchdog motion sentry", "Sentry"),
        BotCommandEntry("/motion", "Review detected motion triggers and snapshot timestamps", "Sentry"),
        BotCommandEntry("/history", "Recent capture timestamps and security logs", "Logs")
    )

    // 19. WhisperBox Anonymous Mailbox (7 commands)
    private val whisperBoxCommands = listOf(
        BotCommandEntry("/start", "Open mailbox overview & shareable submission link", "General"),
        BotCommandEntry("/link", "Get your unique public anonymous ask link", "Sharing"),
        BotCommandEntry("/inbox", "View list of unread anonymous questions & feedback", "Inbox"),
        BotCommandEntry("/read <id>", "Read full text of an incoming anonymous message", "Inbox"),
        BotCommandEntry("/reply <id> <text>", "Send an anonymous response back to the sender", "Reply"),
        BotCommandEntry("/block <id>", "Block abusive sender from sending future questions", "Moderation"),
        BotCommandEntry("/settings", "Configure auto-filtering for spam and profanity", "Config")
    )

    // 20. TriviaMaster Daily Brain Quiz (7 commands)
    private val triviaMasterCommands = listOf(
        BotCommandEntry("/start", "Today's quiz status and streak counter", "General"),
        BotCommandEntry("/quiz", "Start a 5-question interactive multiple-choice quiz", "Game"),
        BotCommandEntry("/category [name]", "Choose topic (Science, History, Tech, Geography)", "Config"),
        BotCommandEntry("/daily", "Play today's official Daily Brain Challenge question", "Daily"),
        BotCommandEntry("/score", "View personal total score, correct answer %, rank", "Stats"),
        BotCommandEntry("/streak", "Daily quiz participation streak and badge progress", "Stats"),
        BotCommandEntry("/leaderboard", "Top scores in group or personal ranking", "Leaderboard")
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

    private val exactBotMap: Map<String, List<BotCommandEntry>> = mapOf(
        // 1. Commander
        "bot_commander" to commanderCommands,
        "bot_commander_flagship" to commanderCommands,
        "tapbot-commander" to commanderCommands,
        "tapbot_commander" to commanderCommands,

        // 2. Music Streamer
        "bot_music_streamer" to musicCommands,
        "tapstream-music" to musicCommands,
        "tapstream_music" to musicCommands,
        "bot_music" to musicCommands,

        // 3. Exon
        "bot_exon" to exonCommands,
        "bot_1790084493920_n4l8lj" to exonCommands,
        "bot_1790084493920" to exonCommands,
        "exon" to exonCommands,

        // 4. Gemini AI
        "bot_ai_assistant" to aiCommands,
        "bot_gemini" to aiCommands,
        "bot_ai" to aiCommands,

        // 5. HabitForge
        "bot_habit_forge" to habitForgeCommands,
        "habit_forge" to habitForgeCommands,
        "habitforge" to habitForgeCommands,

        // 6. PennyWise
        "bot_penny_wise" to pennyWiseCommands,
        "penny_wise" to pennyWiseCommands,
        "pennywise" to pennyWiseCommands,

        // 7. PomoFocus
        "bot_pomo_focus" to pomoFocusCommands,
        "pomo_focus" to pomoFocusCommands,
        "pomofocus" to pomoFocusCommands,

        // 8. TapNote
        "bot_tap_note" to tapNoteCommands,
        "tap_note" to tapNoteCommands,
        "tapnote" to tapNoteCommands,

        // 9. SkyPulse
        "bot_sky_pulse" to skyPulseCommands,
        "sky_pulse" to skyPulseCommands,
        "skypulse" to skyPulseCommands,

        // 10. RemindMe
        "bot_remind_me" to remindMeCommands,
        "remind_me" to remindMeCommands,
        "remindme" to remindMeCommands,

        // 11. LinkDrop
        "bot_link_drop" to linkDropCommands,
        "link_drop" to linkDropCommands,
        "linkdrop" to linkDropCommands,

        // 12. PolyGlot
        "bot_poly_glot" to polyGlotCommands,
        "poly_glot" to polyGlotCommands,
        "polyglot" to polyGlotCommands,

        // 13. CoinRadar
        "bot_coin_radar" to coinRadarCommands,
        "coin_radar" to coinRadarCommands,
        "coinradar" to coinRadarCommands,

        // 14. FitTrack
        "bot_fit_track" to fitTrackCommands,
        "fit_track" to fitTrackCommands,
        "fittrack" to fitTrackCommands,

        // 15. ParcelTrack
        "bot_parcel_track" to parcelTrackCommands,
        "parcel_track" to parcelTrackCommands,
        "parceltrack" to parcelTrackCommands,

        // 16. RSS Broadcaster
        "bot_rss_notifier" to rssCommands,
        "bot_rss" to rssCommands,
        "rss_notifier" to rssCommands,
        "newspulse" to rssCommands,

        // 17. GitNotifier
        "bot_git_notifier" to gitNotifierCommands,
        "git_notifier" to gitNotifierCommands,
        "gitnotifier" to gitNotifierCommands,

        // 18. CamSentry
        "bot_cam_sentry" to camSentryCommands,
        "cam_sentry" to camSentryCommands,
        "camsentry" to camSentryCommands,

        // 19. WhisperBox
        "bot_whisper_box" to whisperBoxCommands,
        "whisper_box" to whisperBoxCommands,
        "whisperbox" to whisperBoxCommands,

        // 20. TriviaMaster
        "bot_trivia_master" to triviaMasterCommands,
        "trivia_master" to triviaMasterCommands,
        "triviamaster" to triviaMasterCommands,

        // Existing utilities & tests
        "bot_ping_pong" to pingPongCommands,
        "ping_pong" to pingPongCommands,
        "pingpong" to pingPongCommands,
        "bot_echo" to utilityCommands,
        "bot_utility" to utilityCommands
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

        val cleanId = botId.trim().lowercase().replace("-", "_")
        exactBotMap[cleanId]?.let { return it }
        exactBotMap[cleanId.removePrefix("bot_")]?.let { return it }

        val normalized = "$botId $category $name".lowercase().replace("-", "_")
        return when {
            normalized.contains("git_notifier") || normalized.contains("gitnotifier") -> gitNotifierCommands
            normalized.contains("cam_sentry") || normalized.contains("camsentry") -> camSentryCommands
            normalized.contains("whisper_box") || normalized.contains("whisperbox") || normalized.contains("mailbox") -> whisperBoxCommands
            normalized.contains("trivia_master") || normalized.contains("triviamaster") || normalized.contains("trivia") -> triviaMasterCommands
            normalized.contains("habit_forge") || normalized.contains("habitforge") || normalized.contains("habit") -> habitForgeCommands
            normalized.contains("penny_wise") || normalized.contains("pennywise") || normalized.contains("expense") -> pennyWiseCommands
            normalized.contains("pomo_focus") || normalized.contains("pomofocus") || normalized.contains("pomodoro") -> pomoFocusCommands
            normalized.contains("tap_note") || normalized.contains("tapnote") || normalized.contains("scratchpad") -> tapNoteCommands
            normalized.contains("sky_pulse") || normalized.contains("skypulse") || normalized.contains("weather") -> skyPulseCommands
            normalized.contains("remind_me") || normalized.contains("remindme") || normalized.contains("alarm") -> remindMeCommands
            normalized.contains("link_drop") || normalized.contains("linkdrop") || normalized.contains("clipboard") -> linkDropCommands
            normalized.contains("poly_glot") || normalized.contains("polyglot") || normalized.contains("translator") -> polyGlotCommands
            normalized.contains("coin_radar") || normalized.contains("coinradar") || normalized.contains("crypto") -> coinRadarCommands
            normalized.contains("fit_track") || normalized.contains("fittrack") || normalized.contains("workout") -> fitTrackCommands
            normalized.contains("parcel_track") || normalized.contains("parceltrack") || normalized.contains("delivery") -> parcelTrackCommands
            normalized.contains("commander") || normalized.contains("fleet") -> commanderCommands
            normalized.contains("music") || normalized.contains("tapstream") || normalized.contains("stream") -> musicCommands
            normalized.contains("exon") || cleanId.startsWith("bot_1790084493920") -> exonCommands
            normalized.contains("rss") || normalized.contains("newspulse") -> rssCommands
            normalized.contains("gemini") || normalized.contains("assistant") || cleanId == "bot_ai" || cleanId == "bot_ai_assistant" -> aiCommands
            normalized.contains("pingpong") || normalized.contains("ping_pong") -> pingPongCommands
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
        val nonGeneric = entries.filter { entry ->
            val verb = entry.command.trim().substringBefore(" ").lowercase()
            verb != "/start" && verb != "/help"
        }
        val pool = nonGeneric.ifEmpty { entries }
        return pool.take(limit).map { entry ->
            entry.command.trim().substringBefore(" ").trim()
        }
    }
}
