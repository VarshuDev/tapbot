package com.tapbot.core.runner.custom

import com.tapbot.core.model.CustomBotDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class CustomBotSnapshot(
    val bots: List<CustomBotDefinition> = emptyList()
)

/**
 * Persistent local repository for user-built no-code [CustomBotDefinition] workflows.
 * Stored as atomic JSON in `custom_bots.json`; operates purely in-memory when [storageFile] is null.
 */
class CustomBotRepository(
    private val storageFile: File? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _definitions = MutableStateFlow<List<CustomBotDefinition>>(emptyList())
    val definitions: StateFlow<List<CustomBotDefinition>> = _definitions.asStateFlow()

    init {
        loadFromDisk()
    }

    @Synchronized
    fun getAll(): List<CustomBotDefinition> = _definitions.value

    @Synchronized
    fun getDefinition(botId: String): CustomBotDefinition? {
        return _definitions.value.firstOrNull { it.id == botId }
    }

    @Synchronized
    fun saveDefinition(definition: CustomBotDefinition): CustomBotDefinition {
        val now = System.currentTimeMillis()
        val updatedDef = definition.copy(updatedAt = now)
        val mutable = _definitions.value.toMutableList()
        val idx = mutable.indexOfFirst { it.id == updatedDef.id }
        if (idx >= 0) {
            mutable[idx] = updatedDef
        } else {
            mutable.add(0, updatedDef)
        }
        _definitions.value = mutable
        saveToDisk(mutable)
        return updatedDef
    }

    @Synchronized
    fun deleteDefinition(botId: String): Boolean {
        val current = _definitions.value
        val next = current.filterNot { it.id == botId }
        if (next.size == current.size) return false
        _definitions.value = next
        saveToDisk(next)
        return true
    }

    private fun loadFromDisk() {
        val file = storageFile ?: return
        if (!file.exists() || file.length() == 0L) return
        try {
            val raw = file.readText()
            if (raw.isNotBlank()) {
                val snapshot = json.decodeFromString<CustomBotSnapshot>(raw)
                _definitions.value = snapshot.bots
            }
        } catch (_: Exception) {
            // Preserve empty list on corrupted custom_bots.json
        }
    }

    private fun saveToDisk(list: List<CustomBotDefinition>) {
        val file = storageFile ?: return
        try {
            file.parentFile?.mkdirs()
            val encoded = json.encodeToString(CustomBotSnapshot(bots = list))
            val tmpFile = File(file.parentFile ?: File("."), "${file.name}.tmp")
            tmpFile.writeText(encoded)
            if (!tmpFile.renameTo(file)) {
                file.writeText(encoded)
                tmpFile.delete()
            }
        } catch (_: Exception) {
            // Avoid crashing UI on disk write errors
        }
    }
}
