package com.tapbot.feature.botdetail.simulator

import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.ui.catalog.BotCommandCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Engine for simulating on-device Telegram bot runtime interactions directly inside TapBot.
 * Generates contextual responses for standard commands (/start, /help, /status)
 * and domain-specific commands across all 20 flagship bots and custom bots.
 */
object BotSimulatorEngine {

    fun generateWelcome(botName: String): String {
        return """
            👋 **Welcome to $botName!**
            
            This is an in-app interactive sandbox running locally on Android ART coroutines.
            Commands sent here are dispatched locally to verify bot behavior without hitting Telegram servers.
            
            Type **/help** or tap a quick chip below to explore available commands.
        """.trimIndent()
    }

    fun generateHelp(
        botId: String,
        botName: String,
        category: String,
        customDefinition: CustomBotDefinition? = null
    ): String {
        val commands = BotCommandCatalog.getCommandsForBot(botId, customDefinition, category, botName)
        val sb = StringBuilder()
        sb.append("📋 **$botName — Command Directory**\n\n")
        sb.append("The following commands are registered in this bot's on-device runtime:\n\n")

        val grouped = commands.groupBy { it.category.ifBlank { "General" } }
        for ((group, cmds) in grouped) {
            sb.append("• **$group:**\n")
            for (c in cmds) {
                sb.append("  `${c.command}` — ${c.summary}\n")
            }
            sb.append("\n")
        }
        sb.append("💡 *Tip: Tap any quick chip or type a command to simulate its execution.*")
        return sb.toString().trim()
    }

    fun dispatchCommand(
        input: String,
        botId: String,
        botName: String,
        category: String,
        customDefinition: CustomBotDefinition? = null
    ): String {
        val trimmed = input.trim()
        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        val command = parts.firstOrNull()?.lowercase() ?: ""
        val args = if (parts.size > 1) parts[1].trim() else ""

        val normalizedBot = "$botId $category $botName".lowercase().replace("-", "_")
        val nowTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        return when (command) {
            "/start" -> generateWelcome(botName)

            "/help" -> generateHelp(botId, botName, category, customDefinition)

            "/status", "/ping" -> """
                🟢 **$botName — Live Sandbox Status**
                • **Host Engine:** Android ART Local Coroutine Sandbox
                • **Uptime:** Active Sandbox Session ($nowTime)
                • **Simulated Latency:** 12ms (in-process loopback)
                • **Status:** Healthy & Responsive
            """.trimIndent()

            "/battery" -> """
                🔋 **Battery Diagnostics**
                • Level: 87% • Status: Discharging
                • Health: Good • Temperature: 28.5°C
                • Doze Mode: Standby Whitelisted
            """.trimIndent()

            "/ram", "/memory" -> """
                🧠 **Memory HUD**
                • ART Heap Used: 42 MB / 256 MB
                • Device RAM: 3.8 GB Free / 8.0 GB Total
                • Garbage Collection: Nominal
            """.trimIndent()

            "/storage" -> """
                💾 **Storage Telemetry**
                • Internal Storage: 64.2 GB Free / 128.0 GB Total
                • App Sandbox Cache: 8.4 MB
            """.trimIndent()

            "/network" -> """
                📡 **Network Transport**
                • Interface: Wi-Fi (WLAN0)
                • Bandwidth: 150 Mbps Full Duplex
                • IPv4: 192.168.1.105
            """.trimIndent()

            "/bots" -> """
                🤖 **Installed Fleet Overview**
                • Total Installed: 20 Flagship Nodes
                • Simulator Target: $botName (`$botId`)
                • Mode: In-App ART Coroutine Execution
            """.trimIndent()

            "/quote", "/inspire" -> """
                💡 **Daily Inspiration**
                *"The secret of getting ahead is getting started. The secret of getting started is breaking your complex overwhelming tasks into small manageable tasks, and starting on the first one."*
                — Mark Twain
            """.trimIndent()

            "/habit", "/loghabit" -> """
                ✅ **Habit Tracked!**
                Logged habit: **${if (args.isNotBlank()) args else "Drink Water & Hydrate"}**
                🔥 Current Streak: **5 days**
                Keep up the momentum!
            """.trimIndent()

            "/remind", "/remindme" -> """
                ⏰ **Reminder Created!**
                Reminder set for: **${if (args.isNotBlank()) args else "30 minutes from now"}**
                You will receive a notification alert when due.
            """.trimIndent()

            "/fit", "/workout" -> """
                🏋️ **Workout Logged!**
                Activity: **${if (args.isNotBlank()) args else "30 min Morning Cardio"}**
                Estimated Calories: ~240 kcal
                Total Weekly Workouts: 4 sessions
            """.trimIndent()

            "/expense", "/spend" -> """
                💸 **Expense Recorded!**
                Entry: **${if (args.isNotBlank()) args else "$12.50 Coffee & Snack"}**
                📊 Today's Total: $12.50
                💼 Remaining Monthly Budget: $987.50
            """.trimIndent()

            "/crypto", "/coin" -> """
                🪙 **Market Radar: ${if (args.isNotBlank()) args.uppercase() else "BTC"}**
                • Price: **$91,450.00 USD** (+3.42% 24h)
                • 24h Volume: $42.8B
                • Market Trend: Bullish Momentum
            """.trimIndent()

            "/timer", "/pomo", "/pomodoro" -> """
                🍅 **Pomodoro Sprint Started!**
                Sprint Duration: **${if (args.isNotBlank()) args else "25 minutes"}**
                🎯 Focus goal: Deep work session. Background timer active!
            """.trimIndent()

            "/note", "/scratch" -> """
                📝 **Scratchpad Note Saved!**
                Note: "${if (args.isNotBlank()) args else "Quick task item"}"
                Saved locally to encrypted on-device database.
            """.trimIndent()

            "/weather", "/sky" -> """
                🌤️ **Weather Report for ${if (args.isNotBlank()) args else "Local Coordinates"}**
                • Condition: Mostly Sunny
                • Temperature: 23°C (73°F)
                • Humidity: 48% • Wind: 9 km/h
                • Forecast: Clear skies throughout the evening
            """.trimIndent()

            "/play", "/radio", "/music" -> """
                🎵 **TapStream Playback Simulated**
                Track: **${if (args.isNotBlank()) args else "Lofi Chill Beats Stream"}**
                Routing: Host Speaker & Audio Engine
                Volume: 75% • Status: Playing
            """.trimIndent()

            "/git", "/repo" -> """
                🐙 **GitHub Watcher Active**
                Repository: `${if (args.isNotBlank()) args else "VarshuDev/tapbot"}`
                Status: Webhook listening on active port. 0 pending PRs.
            """.trimIndent()

            "/news", "/pulse" -> """
                📰 **NewsPulse Headlines**
                1. 🚀 Mobile runtime performance hits new high on Android 15
                2. 🛡️ On-device AI assistants expand privacy-first local features
                3. ⚡ Modern Kotlin compilers reduce APK binary overhead
            """.trimIndent()

            "/translate", "/polyglot" -> """
                🌐 **PolyGlot Translation**
                Original: "${if (args.isNotBlank()) args else "Hello world"}"
                Translated (ES): *"¡Hola Mundo!"*
            """.trimIndent()

            "/parcel", "/track" -> """
                📦 **Parcel Tracker**
                Tracking Number: `${if (args.isNotBlank()) args else "TAP109824X"}`
                Status: **Out for Delivery** (ETA: Today by 4:00 PM)
            """.trimIndent()

            "/trivia", "/quiz" -> """
                🧠 **Trivia Challenge!**
                **Question:** What is the fastest land animal?
                A) Cheetah
                B) Pronghorn
                C) Lion
                D) Thompson's Gazelle
                *Reply with your answer to score points!*
            """.trimIndent()

            else -> {
                if (trimmed.startsWith("/")) {
                    val available = BotCommandCatalog.getCommandsForBot(botId, customDefinition, category, botName)
                    val matching = available.find { it.command.removePrefix("/").substringBefore(" ").equals(command.removePrefix("/"), ignoreCase = true) }
                    if (matching != null) {
                        """
                            ⚡ **Simulated Command: `${matching.command}`**
                            ${matching.summary}
                            
                            *Command payload received: "${if (args.isNotBlank()) args else "(no arguments)"}"*
                            Execution completed successfully with exit code 0.
                        """.trimIndent()
                    } else {
                        """
                            ❓ **Unknown Command: `$command`**
                            This command is not recognized by $botName.
                            Type **/help** to list all available commands.
                        """.trimIndent()
                    }
                } else {
                    """
                        💬 **Message Received:** "$trimmed"
                        
                        *Local runner received text message. Type **/help** to see interactive slash commands.*
                    """.trimIndent()
                }
            }
        }
    }
}
