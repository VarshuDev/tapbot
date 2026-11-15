package com.tapbot.core.runner.runtime

import com.tapbot.core.runner.ServiceLocator

/**
 * Factory for instantiating bot runtimes based on bot ID and runtime metadata.
 */
object BotRuntimeFactory {

    fun createRuntime(botId: String, botName: String? = null): BotRuntime {
        val customDef = ServiceLocator.customBotRepository?.getDefinition(botId)
        if (customDef != null || botId.startsWith("bot_custom_") || botId.startsWith("custom_wf_")) {
            return CustomWorkflowBotRuntime(botId)
        }

        val normalized = (botId + " " + (botName ?: "")).lowercase().replace("-", "_")
        val words = normalized.split("_", " ", "-").filter { it.isNotBlank() }.toSet()

        return when {
            normalized.contains("custom_workflow") -> CustomWorkflowBotRuntime(botId)

            // Exon Group Sentinel
            normalized.contains("exon") || botId.startsWith("bot_1790084493920") ||
                botId == "bot_exon" -> ExonBotRuntime(botId)

            // Flagship Hardware Commander
            normalized.contains("commander") ||
                normalized.contains("companion") || normalized.contains("controller") ||
                normalized.contains("fleet") || botId == "bot_commander" ||
                botId == "tapbot_commander" || botId.startsWith("bot_commander_") -> CommanderBotRuntime(botId)

            // Flagship Music & Streamer
            normalized.contains("music") || normalized.contains("stream") ||
                normalized.contains("tapstream") || botId == "bot_music_streamer" ||
                botId == "tapstream_music" -> MusicBotRuntime(botId)

            // Flagship RSS & News Broadcaster
            normalized.contains("rss") || normalized.contains("feed") ||
                (normalized.contains("notifier") && !normalized.contains("git")) -> RssNotifierBotRuntime(botId)

            // Daily Lifestyle: Habits, Reminders, Fitness (checked before generic matching)
            normalized.contains("habit") || normalized.contains("remind") ||
                normalized.contains("fit") || botId in listOf("bot_habit_forge", "bot_remind_me", "bot_fit_track") ->
                DailyLifestyleBotRuntime(botId)

            // Flagship Gemini AI Assistant (word-boundary safe check)
            words.contains("ai") || normalized.contains("gemini") || botId.startsWith("bot_ai") -> AIBotRuntime(botId)

            // Personal Finance & Market Radar
            normalized.contains("penny") || normalized.contains("expense") ||
                normalized.contains("budget") || normalized.contains("coin") ||
                normalized.contains("crypto") || normalized.contains("stock") ||
                botId in listOf("bot_penny_wise", "bot_coin_radar") ->
                PersonalFinanceBotRuntime(botId)

            // Productivity & Task Sprint
            normalized.contains("pomo") || normalized.contains("focus") ||
                normalized.contains("note") || normalized.contains("scratchpad") ||
                botId in listOf("bot_pomo_focus", "bot_tap_note") ->
                ProductivityBotRuntime(botId)

            // Developer Tools: Git Notifier
            normalized.contains("git") || botId == "bot_git_notifier" ->
                DeveloperBotRuntime(botId)

            // Daily Utilities: Weather, Link Vault, Translator, Parcel Track, Camera Sentry, WhisperBox, Trivia
            normalized.contains("sky") || normalized.contains("weather") ||
                normalized.contains("link") || normalized.contains("polyglot") ||
                normalized.contains("translat") || normalized.contains("parcel") ||
                normalized.contains("package") || normalized.contains("cam") ||
                normalized.contains("sentry") || normalized.contains("whisper") ||
                normalized.contains("trivia") || normalized.contains("quiz") ||
                botId in listOf(
                    "bot_sky_pulse", "bot_link_drop", "bot_poly_glot", "bot_parcel_track",
                    "bot_cam_sentry", "bot_whisper_box", "bot_trivia_master"
                ) -> DailyUtilityBotRuntime(botId)

            // System Diagnostics & Echo Starter
            normalized.contains("util") || normalized.contains("diagnostic") ||
                normalized.contains("echo") -> UtilityBotRuntime(botId)

            // Fallback
            else -> PingPongBotRuntime(botId)
        }
    }
}
