package com.tapbot.core.runner.custom

import android.content.Context
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.security.EncryptedFileHelper
import com.tapbot.core.security.SecretRedactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale

@Serializable
private data class CustomBotSnapshot(
    val bots: List<CustomBotDefinition> = emptyList()
)

/**
 * Persistent local repository for user-built no-code [CustomBotDefinition] workflows.
 * Uses Android Keystore AES-256-GCM [EncryptedFileHelper] when [androidContext] is provided
 * (migrating legacy plaintext JSON once on read), or atomic temp-file writes in headless tests.
 */
class CustomBotRepository(
    private val storageFile: File? = null,
    private val androidContext: Context? = null
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
        val previous = if (idx >= 0) mutable[idx] else null
        if (idx >= 0) {
            mutable[idx] = updatedDef
        } else {
            mutable.add(0, updatedDef)
        }
        if (previous != null) {
            unregisterSecretsFor(previous, excludeIn = updatedDef)
        }
        registerSecretsFor(updatedDef)
        _definitions.value = mutable
        saveToDisk(mutable)
        return updatedDef
    }

    @Synchronized
    fun deleteDefinition(botId: String): Boolean {
        val current = _definitions.value
        val removed = current.firstOrNull { it.id == botId } ?: return false
        val next = current.filterNot { it.id == botId }
        unregisterSecretsFor(removed, excludeList = next)
        _definitions.value = next
        saveToDisk(next)
        return true
    }

    private fun loadFromDisk() {
        val file = storageFile ?: return
        if (!file.exists() || file.length() == 0L) return
        try {
            val raw = if (androidContext != null) {
                EncryptedFileHelper.readUtf8WithMigration(androidContext, file)
            } else {
                file.readText(Charsets.UTF_8)
            }
            if (!raw.isNullOrBlank()) {
                val snapshot = json.decodeFromString<CustomBotSnapshot>(raw)
                _definitions.value = snapshot.bots
                snapshot.bots.forEach { registerSecretsFor(it) }
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
            if (androidContext != null) {
                EncryptedFileHelper.writeEncryptedUtf8(androidContext, file, encoded)
            } else {
                val tmpFile = File(file.parentFile ?: File("."), "${file.name}.tmp")
                tmpFile.writeText(encoded, Charsets.UTF_8)
                if (!tmpFile.renameTo(file)) {
                    file.writeText(encoded, Charsets.UTF_8)
                    tmpFile.delete()
                }
            }
        } catch (_: Exception) {
            // Avoid crashing UI on disk write errors
        }
    }

    private fun registerSecretsFor(definition: CustomBotDefinition) {
        extractHeaderSecrets(definition).forEach { SecretRedactor.registerSecret(it) }
    }

    private fun unregisterSecretsFor(
        definition: CustomBotDefinition,
        excludeIn: CustomBotDefinition? = null,
        excludeList: List<CustomBotDefinition> = emptyList()
    ) {
        val keepSecrets = buildSet {
            if (excludeIn != null) addAll(extractHeaderSecrets(excludeIn))
            excludeList.forEach { addAll(extractHeaderSecrets(it)) }
        }
        extractHeaderSecrets(definition)
            .filterNot { it in keepSecrets }
            .forEach { SecretRedactor.unregisterSecret(it) }
    }

    private fun extractHeaderSecrets(definition: CustomBotDefinition): List<String> {
        return definition.rules.flatMap { rule ->
            rule.webhookHeaders.entries.mapNotNull { (headerName, headerValue) ->
                val trimmed = headerValue.trim()
                if (trimmed.length >= 4 && isSensitiveHeaderName(headerName)) trimmed else null
            }
        }
    }

    private fun isSensitiveHeaderName(name: String): Boolean {
        val lower = name.lowercase(Locale.US)
        return SENSITIVE_HEADER_KEYWORDS.any { lower.contains(it) }
    }

    private companion object {
        val SENSITIVE_HEADER_KEYWORDS = listOf(
            "auth", "token", "secret", "key", "pass", "cookie", "signature", "bearer"
        )
    }
}
