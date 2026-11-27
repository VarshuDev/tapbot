package com.tapbot.core.runner.telemetry

import com.tapbot.core.model.BotMetricSnapshot
import com.tapbot.core.model.FleetAnalyticsSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

@Serializable
internal data class PersistedMetricData(
    val dayOfYear: Int = 0,
    val year: Int = 0,
    val snapshots: Map<String, BotMetricSnapshot> = emptyMap(),
    val hourlyThroughput: List<Int> = List(24) { 0 }
)

/**
 * Thread-safe atomic rolling metric store that tracks per-bot and fleet-wide telemetry,
 * manages daily counter rollovers, and persists telemetry state to local storage.
 */
class RollingBotMetricStore(
    private val storageFile: File? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }
) {
    private val snapshots = ConcurrentHashMap<String, BotMetricSnapshot>()
    private val hourlyThroughput = IntArray(24) { 0 }
    private val lock = Any()

    private var currentDayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    private val _fleetAnalyticsStream = MutableStateFlow(computeSummary(0, 0))
    val fleetAnalyticsStream: StateFlow<FleetAnalyticsSummary> = _fleetAnalyticsStream.asStateFlow()
    val fleetAnalytics: StateFlow<FleetAnalyticsSummary> get() = fleetAnalyticsStream

    init {
        loadFromDisk()
    }

    fun updateFleetCounts(runningCount: Int, totalCount: Int) {
        synchronized(lock) {
            _fleetAnalyticsStream.value = computeSummary(runningCount, totalCount)
        }
    }

    fun recordMessage(botId: String, isCommand: Boolean = false) {
        synchronized(lock) {
            checkDayRollover()
            val now = System.currentTimeMillis()
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            hourlyThroughput[hour] = hourlyThroughput[hour] + 1

            val current = snapshots[botId] ?: BotMetricSnapshot(botId = botId)
            val updated = current.copy(
                totalMessages = current.totalMessages + 1,
                messagesToday = current.messagesToday + 1,
                totalCommands = if (isCommand) current.totalCommands + 1 else current.totalCommands,
                commandsToday = if (isCommand) current.commandsToday + 1 else current.commandsToday,
                lastActiveTimestampMs = now
            )
            snapshots[botId] = updated
            refreshStream()
        }
        scheduleSave()
    }

    fun recordError(botId: String, error: String? = null) {
        synchronized(lock) {
            checkDayRollover()
            val current = snapshots[botId] ?: BotMetricSnapshot(botId = botId)
            snapshots[botId] = current.copy(
                errorCount = current.errorCount + 1,
                lastActiveTimestampMs = System.currentTimeMillis()
            )
            refreshStream()
        }
        scheduleSave()
    }

    fun recordBotStarted(botId: String) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val current = snapshots[botId] ?: BotMetricSnapshot(botId = botId)
            snapshots[botId] = current.copy(
                uptimeStartMs = now,
                isCurrentlyRunning = true,
                lastActiveTimestampMs = now
            )
            refreshStream()
        }
        scheduleSave()
    }

    fun recordBotStopped(botId: String) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val current = snapshots[botId] ?: BotMetricSnapshot(botId = botId)
            val sessionUptime = if (current.uptimeStartMs > 0L) {
                ((now - current.uptimeStartMs) / 1000L).coerceAtLeast(0L)
            } else 0L

            snapshots[botId] = current.copy(
                isCurrentlyRunning = false,
                uptimeStartMs = 0L,
                totalUptimeSeconds = current.totalUptimeSeconds + sessionUptime,
                lastActiveTimestampMs = now
            )
            refreshStream()
        }
        scheduleSave()
    }

    fun getSnapshot(botId: String): BotMetricSnapshot {
        return snapshots[botId] ?: BotMetricSnapshot(botId = botId)
    }

    fun getFleetSummary(runningCount: Int = 0, totalCount: Int = 0): FleetAnalyticsSummary {
        synchronized(lock) {
            return computeSummary(runningCount, totalCount)
        }
    }

    private fun computeSummary(runningCount: Int, totalCount: Int): FleetAnalyticsSummary {
        var totalMsgs = 0L
        var msgsToday = 0L
        var totalCmds = 0L
        var cmdsToday = 0L
        var totalErr = 0L

        for (snap in snapshots.values) {
            totalMsgs += snap.totalMessages
            msgsToday += snap.messagesToday
            totalCmds += snap.totalCommands
            cmdsToday += snap.commandsToday
            totalErr += snap.errorCount
        }

        val totalOps = (totalMsgs + totalCmds).coerceAtLeast(1L)
        val successOps = (totalOps - totalErr.coerceAtMost(totalOps)).toDouble()
        val calculatedUptime = if (totalErr == 0L) 99.9 else ((successOps / totalOps * 1000.0).toLong() / 10.0)

        return FleetAnalyticsSummary(
            totalMessagesProcessed = totalMsgs,
            messagesProcessedToday = msgsToday,
            totalCommandsExecuted = totalCmds,
            commandsExecutedToday = cmdsToday,
            totalErrors = totalErr,
            runningBotsCount = runningCount,
            totalBotsCount = totalCount.coerceAtLeast(runningCount),
            uptimePercentage = calculatedUptime,
            botSnapshots = snapshots.toMap(),
            hourlyMessageThroughput = hourlyThroughput.toList(),
            lastUpdatedTimestampMs = System.currentTimeMillis()
        )
    }

    private fun checkDayRollover() {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        if (day != currentDayOfYear || year != currentYear) {
            currentDayOfYear = day
            currentYear = year
            for ((key, snap) in snapshots) {
                snapshots[key] = snap.copy(messagesToday = 0L, commandsToday = 0L)
            }
            hourlyThroughput.fill(0)
        }
    }

    private fun refreshStream() {
        _fleetAnalyticsStream.value = computeSummary(
            runningCount = snapshots.values.count { it.isCurrentlyRunning },
            totalCount = snapshots.size
        )
    }

    private fun loadFromDisk() {
        if (storageFile == null || !storageFile.exists()) return
        try {
            val text = storageFile.readText()
            if (text.isNotBlank()) {
                val data = json.decodeFromString<PersistedMetricData>(text)
                currentDayOfYear = data.dayOfYear
                currentYear = data.year
                snapshots.putAll(data.snapshots)
                for (i in 0 until minOf(24, data.hourlyThroughput.size)) {
                    hourlyThroughput[i] = data.hourlyThroughput[i]
                }
                checkDayRollover()
                refreshStream()
            }
        } catch (_: Exception) {
            // Ignore parse errors on corrupted files
        }
    }

    fun flush() {
        saveInternal()
    }

    private fun scheduleSave() {
        if (storageFile == null) return
        scope.launch {
            saveInternal()
        }
    }

    private fun saveInternal() {
        if (storageFile == null) return
        try {
            val data = synchronized(lock) {
                PersistedMetricData(
                    dayOfYear = currentDayOfYear,
                    year = currentYear,
                    snapshots = snapshots.toMap(),
                    hourlyThroughput = hourlyThroughput.toList()
                )
            }
            storageFile.parentFile?.mkdirs()
            val temp = File(storageFile.parentFile, "${storageFile.name}.tmp")
            temp.writeText(json.encodeToString(PersistedMetricData.serializer(), data))
            if (temp.exists()) {
                try {
                    java.nio.file.Files.move(
                        temp.toPath(),
                        storageFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (_: Exception) {
                    if (storageFile.exists()) storageFile.delete()
                    temp.renameTo(storageFile)
                }
            }
        } catch (_: Exception) {}
    }
}
