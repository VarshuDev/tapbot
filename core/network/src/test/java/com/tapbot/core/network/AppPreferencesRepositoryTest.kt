package com.tapbot.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `defaults are sensible when no disk file exists`() {
        val repo = AppPreferencesRepository(storageFile = null)
        val prefs = repo.preferences.value

        assertTrue(prefs.autoStartOnBoot)
        assertEquals("", prefs.customCatalogUrl)
        assertFalse(prefs.wifiOnlyDownloads)
        assertTrue(prefs.diagnosticsExportEnabled)
        assertEquals(
            CloudflareCatalogApi.DEFAULT_BASE_URL,
            repo.effectiveCatalogUrl(CloudflareCatalogApi.DEFAULT_BASE_URL)
        )
    }

    @Test
    fun `preferences persist across repository reloads`() {
        val prefsFile = tempFolder.newFile("app_preferences.json")
        prefsFile.delete()

        val repo = AppPreferencesRepository(storageFile = prefsFile)
        repo.setAutoStartOnBoot(false)
        repo.setWifiOnlyDownloads(true)
        repo.setDiagnosticsExportEnabled(false)
        repo.setCustomCatalogUrl("https://custom.worker.dev/catalog///")

        val reloaded = AppPreferencesRepository(storageFile = prefsFile)
        val prefs = reloaded.preferences.value

        assertFalse(prefs.autoStartOnBoot)
        assertTrue(prefs.wifiOnlyDownloads)
        assertFalse(prefs.diagnosticsExportEnabled)
        assertEquals("https://custom.worker.dev/catalog", prefs.customCatalogUrl)
        assertEquals(
            "https://custom.worker.dev/catalog",
            reloaded.effectiveCatalogUrl("https://default.worker.dev")
        )
    }

    @Test
    fun `effectiveCatalogUrl rejects invalid schemes and resetToDefaults restores defaults`() {
        val prefsFile = tempFolder.newFile("app_preferences_reset.json")
        val repo = AppPreferencesRepository(storageFile = prefsFile)

        repo.setCustomCatalogUrl("ftp://invalid-endpoint.local")
        assertEquals(
            "https://default.worker.dev",
            repo.effectiveCatalogUrl("https://default.worker.dev")
        )

        repo.setAutoStartOnBoot(false)
        repo.setCustomCatalogUrl("https://edge.example.com")
        repo.resetToDefaults()

        val reloaded = AppPreferencesRepository(storageFile = prefsFile)
        assertTrue(reloaded.preferences.value.autoStartOnBoot)
        assertEquals("", reloaded.preferences.value.customCatalogUrl)
    }
}
