package com.tapbot.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppPreferences(
    val autoStartOnBoot: Boolean = true,
    val customCatalogUrl: String = "",
    val wifiOnlyDownloads: Boolean = false,
    val diagnosticsExportEnabled: Boolean = true
)

/**
 * Persistent preferences repository stored as atomic JSON in app filesDir.
 * Operates purely in-memory when [storageFile] is null for isolated JVM unit tests.
 */
class AppPreferencesRepository(
    private val storageFile: File? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _preferences = MutableStateFlow(AppPreferences())
    val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    init {
        loadFromDisk()
    }

    @Synchronized
    fun updatePreferences(transform: (AppPreferences) -> AppPreferences): AppPreferences {
        val next = transform(_preferences.value)
        _preferences.value = next
        saveToDisk(next)
        return next
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        updatePreferences { it.copy(autoStartOnBoot = enabled) }
    }

    fun setCustomCatalogUrl(url: String) {
        val cleaned = url.trim().trimEnd('/')
        updatePreferences { it.copy(customCatalogUrl = cleaned) }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        updatePreferences { it.copy(wifiOnlyDownloads = enabled) }
    }

    fun setDiagnosticsExportEnabled(enabled: Boolean) {
        updatePreferences { it.copy(diagnosticsExportEnabled = enabled) }
    }

    fun resetToDefaults() {
        updatePreferences { AppPreferences() }
    }

    fun effectiveCatalogUrl(
        defaultUrl: String = CloudflareCatalogApi.DEFAULT_BASE_URL,
        allowLocalhostHttp: Boolean = false
    ): String {
        val custom = _preferences.value.customCatalogUrl.trim().trimEnd('/')
        if (custom.isBlank()) return defaultUrl
        val urlValidation = UrlSecurityValidator.validateUrlSyntax(
            custom,
            UrlSecurityValidator.ValidationOptions(
                requireHttps = true,
                allowLoopbackHosts = allowLocalhostHttp
            )
        )
        return if (urlValidation.isSuccess) custom else defaultUrl
    }

    private fun loadFromDisk() {
        val file = storageFile ?: return
        if (!file.exists() || file.length() == 0L) return
        try {
            val raw = file.readText()
            if (raw.isNotBlank()) {
                _preferences.value = json.decodeFromString<AppPreferences>(raw)
            }
        } catch (_: Exception) {
            // Preserve defaults if disk JSON is unreadable
        }
    }

    private fun saveToDisk(prefs: AppPreferences) {
        val file = storageFile ?: return
        try {
            file.parentFile?.mkdirs()
            val encoded = json.encodeToString(prefs)
            val tmpFile = File(file.parentFile ?: File("."), "${file.name}.tmp")
            tmpFile.writeText(encoded)
            if (!tmpFile.renameTo(file)) {
                file.writeText(encoded)
                tmpFile.delete()
            }
        } catch (_: Exception) {
            // Avoid crashing settings UI on transient disk error
        }
    }
}
