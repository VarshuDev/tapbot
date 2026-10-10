package com.tapbot.core.runner.runtime

/**
 * Factory for instantiating bot runtimes based on bot ID and runtime metadata.
 */
object BotRuntimeFactory {

    fun createRuntime(botId: String, botName: String? = null): BotRuntime {
        val normalized = (botId + " " + (botName ?: "")).lowercase().replace("-", "_")
        return when {
            normalized.contains("music") -> MusicBotRuntime(botId)
            normalized.contains("ai") || normalized.contains("gemini") -> AIBotRuntime(botId)
            normalized.contains("exon") || botId.startsWith("bot_1790084493920") -> ExonBotRuntime(botId)
            normalized.contains("util") || normalized.contains("diagnostic") -> UtilityBotRuntime(botId)
            normalized.contains("echo") -> UtilityBotRuntime(botId)
            else -> PingPongBotRuntime(botId)
        }
    }
}
