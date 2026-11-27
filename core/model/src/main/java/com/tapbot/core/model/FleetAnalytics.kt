package com.tapbot.core.model

import kotlinx.serialization.Serializable

/**
 * Snapshot of operational metrics for an individual bot instance.
 */
@Serializable
data class BotMetricSnapshot(
    val botId: String,
    val totalMessages: Long = 0L,
    val messagesToday: Long = 0L,
    val totalCommands: Long = 0L,
    val commandsToday: Long = 0L,
    val errorCount: Long = 0L,
    val lastActiveTimestampMs: Long = 0L,
    val uptimeStartMs: Long = 0L,
    val totalUptimeSeconds: Long = 0L,
    val isCurrentlyRunning: Boolean = false
) {
    val estimatedUptimePercentage: Double
        get() = if (errorCount == 0L) 99.9 else {
            val total = (totalMessages + totalCommands).coerceAtLeast(1L)
            val successRate = (total - errorCount.coerceAtMost(total)).toDouble() / total * 100.0
            (successRate * 10.0).toLong() / 10.0
        }
}

/**
 * Aggregated telemetry across the entire hosted bot fleet.
 */
@Serializable
data class FleetAnalyticsSummary(
    val totalMessagesProcessed: Long = 0L,
    val messagesProcessedToday: Long = 0L,
    val totalCommandsExecuted: Long = 0L,
    val commandsExecutedToday: Long = 0L,
    val totalErrors: Long = 0L,
    val runningBotsCount: Int = 0,
    val totalBotsCount: Int = 0,
    val uptimePercentage: Double = 99.9,
    val botSnapshots: Map<String, BotMetricSnapshot> = emptyMap(),
    val hourlyMessageThroughput: List<Int> = List(24) { 0 },
    val lastUpdatedTimestampMs: Long = 0L
) {
    fun getSnapshotFor(botId: String): BotMetricSnapshot {
        return botSnapshots[botId] ?: BotMetricSnapshot(botId = botId)
    }
}
