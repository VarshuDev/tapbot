package com.tapbot.core.runner.moderation

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ExonChatState(
    val warns: Map<Long, Int> = emptyMap(),
    val rules: String? = null,
    val locks: Set<String> = emptySet(),
    val welcomeMessage: String? = null,
    val welcomeEnabled: Boolean = false,
    val antifloodLimit: Int = 0
)

@Serializable
private data class ExonModerationSnapshot(
    val chats: Map<Long, ExonChatState> = emptyMap()
)

/**
 * Persistent per-chat moderation store for [com.tapbot.core.runner.runtime.ExonBotRuntime].
 * When [storageFile] is null (e.g. unit tests), operates purely in-memory for clean isolation.
 */
class ExonModerationStore(
    private val storageFile: File? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val chats = mutableMapOf<Long, ExonChatState>()

    init {
        loadFromDisk()
    }

    @Synchronized
    fun getChatState(chatId: Long): ExonChatState {
        return chats[chatId] ?: ExonChatState()
    }

    @Synchronized
    fun incrementWarn(chatId: Long, userId: Long): Int {
        val current = chats[chatId] ?: ExonChatState()
        val nextCount = (current.warns[userId] ?: 0) + 1
        chats[chatId] = current.copy(warns = current.warns + (userId to nextCount))
        saveToDisk()
        return nextCount
    }

    @Synchronized
    fun getWarns(chatId: Long, userId: Long): Int {
        return chats[chatId]?.warns?.get(userId) ?: 0
    }

    @Synchronized
    fun resetWarns(chatId: Long, userId: Long) {
        val current = chats[chatId] ?: return
        if (!current.warns.containsKey(userId)) return
        chats[chatId] = current.copy(warns = current.warns - userId)
        saveToDisk()
    }

    @Synchronized
    fun getRules(chatId: Long): String? = chats[chatId]?.rules

    @Synchronized
    fun setRules(chatId: Long, rules: String) {
        val current = chats[chatId] ?: ExonChatState()
        chats[chatId] = current.copy(rules = rules)
        saveToDisk()
    }

    @Synchronized
    fun clearRules(chatId: Long) {
        val current = chats[chatId] ?: return
        if (current.rules == null) return
        chats[chatId] = current.copy(rules = null)
        saveToDisk()
    }

    @Synchronized
    fun addLock(chatId: Long, lockType: String) {
        val normalized = normalizeLockType(lockType)
        val current = chats[chatId] ?: ExonChatState()
        chats[chatId] = current.copy(locks = current.locks + normalized)
        saveToDisk()
    }

    @Synchronized
    fun removeLock(chatId: Long, lockType: String) {
        val normalized = normalizeLockType(lockType)
        val current = chats[chatId] ?: return
        chats[chatId] = current.copy(locks = current.locks - normalized - lockType.lowercase())
        saveToDisk()
    }

    @Synchronized
    fun getLocks(chatId: Long): Set<String> = chats[chatId]?.locks ?: emptySet()

    @Synchronized
    fun setWelcomeMessage(chatId: Long, message: String) {
        val current = chats[chatId] ?: ExonChatState()
        chats[chatId] = current.copy(welcomeMessage = message, welcomeEnabled = true)
        saveToDisk()
    }

    @Synchronized
    fun setWelcomeEnabled(chatId: Long, enabled: Boolean) {
        val current = chats[chatId] ?: ExonChatState()
        chats[chatId] = current.copy(welcomeEnabled = enabled)
        saveToDisk()
    }

    @Synchronized
    fun clearWelcome(chatId: Long) {
        val current = chats[chatId] ?: return
        chats[chatId] = current.copy(welcomeMessage = null, welcomeEnabled = false)
        saveToDisk()
    }

    @Synchronized
    fun getWelcomeConfig(chatId: Long): Pair<Boolean, String?> {
        val current = chats[chatId] ?: ExonChatState()
        return current.welcomeEnabled to current.welcomeMessage
    }

    @Synchronized
    fun setAntifloodLimit(chatId: Long, limit: Int) {
        val current = chats[chatId] ?: ExonChatState()
        chats[chatId] = current.copy(antifloodLimit = limit.coerceAtLeast(0))
        saveToDisk()
    }

    @Synchronized
    fun getAntifloodLimit(chatId: Long): Int = chats[chatId]?.antifloodLimit ?: 0

    @Synchronized
    fun getTrackedChatCount(): Int = chats.size

    @Synchronized
    fun getTotalActiveLocks(): Int = chats.values.sumOf { it.locks.size }

    @Synchronized
    fun getTotalWarnCount(): Int = chats.values.sumOf { it.warns.values.sum() }

    private fun normalizeLockType(raw: String): String {
        return when (val low = raw.trim().lowercase()) {
            "url", "urls", "link" -> "links"
            "stickers" -> "sticker"
            "photos", "image", "images" -> "photo"
            "doc", "docs", "file", "files", "documents" -> "document"
            "forward" -> "forwards"
            "bot" -> "bots"
            else -> low
        }
    }

    private fun loadFromDisk() {
        if (storageFile == null || !storageFile.exists() || storageFile.length() == 0L) return
        try {
            val raw = storageFile.readText()
            val snapshot = json.decodeFromString<ExonModerationSnapshot>(raw)
            chats.clear()
            chats.putAll(snapshot.chats)
        } catch (_: Exception) {
            // Ignore corrupted moderation state rather than crashing runtime
        }
    }

    private fun saveToDisk() {
        val file = storageFile ?: return
        try {
            file.parentFile?.mkdirs()
            val encoded = json.encodeToString(ExonModerationSnapshot(chats = chats.toMap()))
            val tmpFile = File(file.parentFile ?: File("."), "${file.name}.tmp")
            tmpFile.writeText(encoded)
            if (!tmpFile.renameTo(file)) {
                file.writeText(encoded)
                tmpFile.delete()
            }
        } catch (_: Exception) {
            // Avoid failing command execution due to transient storage errors
        }
    }
}
