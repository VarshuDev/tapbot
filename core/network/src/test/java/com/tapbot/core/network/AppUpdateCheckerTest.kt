package com.tapbot.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateCheckerTest {

    private val camelCaseJson = """
        {
            "latestVersionCode": 260,
            "latestVersionName": "2.6.0",
            "minRequiredVersionCode": 250,
            "forceUpdate": false,
            "title": "TapBot v2.6.0 Upgrade",
            "releaseNotes": "In-App Force Update OTA System",
            "apkUrl": "https://example.com/tapbot-v2.6.0.apk",
            "downloadUrl": "https://example.com/download",
            "sha256": "4895583b..."
        }
    """.trimIndent()

    private val snakeCaseForceUpdateJson = """
        {
            "latest_version_code": 260,
            "latest_version_name": "2.6.0",
            "min_required_version_code": 260,
            "force_update": true,
            "title": "Mandatory Security Upgrade",
            "release_notes": "Critical security and fleet update required",
            "apk_url": "https://example.com/tapbot-v2.6.0.apk",
            "download_url": "https://example.com/download"
        }
    """.trimIndent()

    @Test
    fun `parseManifest correctly parses camelCase JSON`() {
        val checker = AppUpdateChecker()
        val manifest = checker.parseManifest(camelCaseJson)

        assertEquals(260L, manifest.effectiveLatestVersionCode)
        assertEquals("2.6.0", manifest.effectiveLatestVersionName)
        assertEquals(250L, manifest.effectiveMinRequiredVersionCode)
        assertFalse(manifest.effectiveForceUpdate)
        assertEquals("TapBot v2.6.0 Upgrade", manifest.title)
        assertEquals("https://example.com/tapbot-v2.6.0.apk", manifest.effectiveApkUrl)
    }

    @Test
    fun `parseManifest correctly parses snake_case JSON and identifies force update`() {
        val checker = AppUpdateChecker()
        val manifest = checker.parseManifest(snakeCaseForceUpdateJson)

        assertEquals(260L, manifest.effectiveLatestVersionCode)
        assertEquals("2.6.0", manifest.effectiveLatestVersionName)
        assertEquals(260L, manifest.effectiveMinRequiredVersionCode)
        assertTrue(manifest.effectiveForceUpdate)
        assertEquals("Mandatory Security Upgrade", manifest.title)

        // If current is 250, update is available and is a force update
        assertTrue(manifest.isUpdateAvailable(250L))
        assertTrue(manifest.isForceUpdate(250L))

        // If current is 260, already up to date
        assertFalse(manifest.isUpdateAvailable(260L))
        assertFalse(manifest.isForceUpdate(260L))
    }

    @Test
    fun `isForceUpdate triggers when current version is below minRequiredVersionCode`() {
        val checker = AppUpdateChecker()
        val manifest = checker.parseManifest(camelCaseJson) // forceUpdate = false, minRequired = 250

        // If current is 240 (< 250), it is forced even though forceUpdate is false
        assertTrue(manifest.isForceUpdate(240L))

        // If current is 250, update is available (260 > 250) but NOT forced (250 >= 250 and forceUpdate=false)
        assertTrue(manifest.isUpdateAvailable(250L))
        assertFalse(manifest.isForceUpdate(250L))
    }

    @Test
    fun `mock network fetch returns UpdateAvailable with force flag`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(snakeCaseForceUpdateJson.toResponseBody())
                    .build()
            }
            .build()

        val checker = AppUpdateChecker(
            client = mockClient,
            defaultEndpoints = listOf("https://mock.example.com/version.json"),
            ioDispatcher = testDispatcher
        )

        val result = checker.check(currentVersionCode = 240L)
        assertTrue(result.isSuccess)
        val update = result.getOrThrow() as AppUpdateCheckResult.UpdateAvailable
        assertEquals(260L, update.manifest.effectiveLatestVersionCode)
        assertTrue(update.isForceUpdate)
    }

    @Test
    fun `AppUpdateManager manages state and prevents dismissing force update`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(snakeCaseForceUpdateJson.toResponseBody())
                    .build()
            }
            .build()

        val checker = AppUpdateChecker(
            client = mockClient,
            defaultEndpoints = listOf("https://mock.example.com/version.json"),
            ioDispatcher = testDispatcher
        )

        val manager = AppUpdateManager(checker = checker, scope = testScope)
        assertEquals(AppUpdateUiState.Idle, manager.updateState.value)

        manager.checkForUpdates(currentVersionCode = 250L, isManualCheck = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = manager.updateState.value
        assertTrue(state is AppUpdateUiState.UpdateAvailable)
        val available = state as AppUpdateUiState.UpdateAvailable
        assertTrue(available.isForceUpdate)
        assertTrue(available.isManualCheck)

        // Attempting to dismiss force update should do nothing
        manager.dismissUpdate()
        assertTrue(manager.updateState.value is AppUpdateUiState.UpdateAvailable)
    }
}
