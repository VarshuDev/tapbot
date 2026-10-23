package com.tapbot.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudflareCatalogApiTest {

    private val sampleJson = """
        {
          "success": true,
          "data": [
            {
              "id": "bot_ping_pong",
              "slug": "ping-pong-bot",
              "name": "Ping Pong Runner",
              "description": "Ultra-lightweight Telegram bot testing on-device latency.",
              "longDescription": "Full description of ping pong runner.",
              "iconUrl": "https://example.com/icon.png",
              "category": "utilities",
              "runtime": "native_art",
              "currentVersion": "1.0.0",
              "status": "published",
              "createdAt": "2026-09-19T00:00:00Z",
              "updatedAt": "2026-09-19T00:00:00Z",
              "credentials": [
                {
                  "key": "bot_token",
                  "displayName": "Telegram Bot Token",
                  "description": "From @BotFather",
                  "required": true,
                  "secret": true,
                  "inputType": "password"
                }
              ],
              "currentVersionInfo": {
                "id": "ver_pp_100",
                "version": "1.0.0",
                "packageKey": "packages/bot_ping_pong_1.0.0.botpkg",
                "packageSize": 15420,
                "sha256": "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
                "releaseNotes": "Initial release",
                "minimumAppVersion": 1,
                "publishedAt": "2026-09-19T00:00:00Z",
                "downloadUrl": "https://api.tapbot.internal/api/v1/packages/packages%2Fbot_ping_pong_1.0.0.botpkg"
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun getBots_parsesCloudflareResponseCorrectly() = runBlocking {
        val mockInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(sampleJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val api = CloudflareCatalogApi(
            baseUrl = "https://mock.api",
            client = client,
            fallback = null
        )

        val result = api.getBots()
        assertTrue(result.isSuccess)

        val bots = result.getOrThrow()
        assertEquals(1, bots.size)

        val bot = bots[0]
        assertEquals("bot_ping_pong", bot.id)
        assertEquals("Ping Pong Runner", bot.name)
        assertEquals("1.0.0", bot.version)
        assertEquals("Utilities", bot.category)
        assertEquals(1, bot.requiredCredentials.size)
        assertEquals("bot_token", bot.requiredCredentials[0].key)
        assertEquals(true, bot.requiredCredentials[0].isSecret)
        assertEquals("https://api.tapbot.internal/api/v1/packages/packages%2Fbot_ping_pong_1.0.0.botpkg", bot.packageInfo.packageUrl)
        assertEquals("7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069", bot.packageInfo.sha256Checksum)
    }

    @Test
    fun getBots_fallsBackWhenNetworkFails() = runBlocking {
        val failingInterceptor = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(failingInterceptor)
            .build()

        val fallback = MockCatalogApi()
        val api = CloudflareCatalogApi(
            baseUrl = "https://failing.api",
            client = client,
            fallback = fallback
        )

        val result = api.getBots()
        assertTrue(result.isSuccess)
        val bots = result.getOrThrow()
        assertTrue(bots.isNotEmpty())
    }

    @Test
    fun mockCatalogApi_includesTapBotCommanderAsFeatured() = runBlocking {
        val mockApi = MockCatalogApi()
        val bots = mockApi.getBots().getOrThrow()
        val commander = bots.find { it.id == "bot_commander" }
        assertNotNull("TapBot Commander must be present in sampleBots", commander)
        assertEquals("TapBot Commander", commander!!.name)
        assertTrue("TapBot Commander must be marked featured", commander.isFeatured)
        assertEquals("Automation", commander.category)
        assertEquals("native_art", commander.packageInfo.runtimeType)

        // Check details by id or alias
        val details = mockApi.getBotDetails("bot_commander").getOrThrow()
        assertEquals("TapBot Commander", details.name)
        val aliasDetails = mockApi.getBotDetails("bot_commander_flagship").getOrThrow()
        assertEquals("TapBot Commander", aliasDetails.name)

        // Check version
        val versions = mockApi.getBotVersions("bot_commander").getOrThrow()
        assertTrue(versions.isNotEmpty())
        assertEquals("1.0.0", versions.first().version)

        val latest = mockApi.getLatestVersion("bot_commander").getOrThrow()
        assertNotNull(latest)
        assertEquals("1.0.0", latest!!.version)
    }

    @Test
    fun liveCatalogApi_fetchesBotsFromHostedBackend() = runBlocking {
        val api = CloudflareCatalogApi(
            baseUrl = CloudflareCatalogApi.DEFAULT_BASE_URL,
            client = OkHttpClient(),
            fallback = null
        )
        val result = api.getBots()
        assertTrue("Live API call to hosted backend must succeed", result.isSuccess)
        val bots = result.getOrThrow()
        assertTrue("Catalog must return at least 5 published bots", bots.size >= 5)
        val commander = bots.find { it.name.contains("Commander") }
        assertNotNull("TapBot Commander must be returned in live catalog", commander)
    }
}
