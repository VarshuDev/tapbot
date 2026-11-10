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
        return when {
            normalized.contains("custom_workflow") -> CustomWorkflowBotRuntime(botId)
            normalized.contains("commander") || normalized.contains("sentinel") ||
                normalized.contains("companion") || normalized.contains("controller") ||
                normalized.contains("fleet") || botId == "bot_commander" ||
                botId == "tapbot_commander" || botId.startsWith("bot_commander_") -> CommanderBotRuntime(botId)
            normalized.contains("music") || normalized.contains("stream") ||
                normalized.contains("tapstream") || botId == "bot_music_streamer" ||
                botId == "tapstream_music" -> MusicBotRuntime(botId)
            normalized.contains("rss") || normalized.contains("feed") ||
                normalized.contains("notifier") -> RssNotifierBotRuntime(botId)
            normalized.contains("ai") || normalized.contains("gemini") -> AIBotRuntime(botId)
            normalized.contains("exon") || botId.startsWith("bot_1790084493920") -> ExonBotRuntime(botId)
            normalized.contains("util") || normalized.contains("diagnostic") -> UtilityBotRuntime(botId)
            normalized.contains("echo") -> UtilityBotRuntime(botId)
            else -> PingPongBotRuntime(botId)
        }
    }
}
