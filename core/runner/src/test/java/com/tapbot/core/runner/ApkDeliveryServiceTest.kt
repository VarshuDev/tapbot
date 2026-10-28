package com.tapbot.core.runner

import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.delivery.ApkDeliveryResult
import com.tapbot.core.runner.delivery.ApkDeliveryService
import com.tapbot.core.runner.delivery.ApkFileInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ApkDeliveryServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var deliveryService: ApkDeliveryService

    private class TestTelegramApiClient : TelegramApiClient() {
        val sentDocuments = mutableListOf<Triple<Long, String, String?>>()
        val sentMessages = mutableListOf<Triple<Long, String, String?>>()
        val sentChatActions = mutableListOf<Pair<Long, String>>()

        override suspend fun sendDocument(
            token: String,
            chatId: Long,
            file: File,
            caption: String?,
            parseMode: String?
        ): Result<Boolean> {
            sentDocuments.add(Triple(chatId, file.name, caption))
            return Result.success(true)
        }

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?
        ): Result<Boolean> {
            sentMessages.add(Triple(chatId, text, parseMode))
            return Result.success(true)
        }

        override suspend fun sendChatAction(
            token: String,
            chatId: Long,
            action: String
        ): Result<Boolean> {
            sentChatActions.add(chatId to action)
            return Result.success(true)
        }
    }

    @Before
    fun setUp() {
        deliveryService = ApkDeliveryService(context = null)
    }

    @Test
    fun `computeSha256 computes deterministic hash of file`() {
        val file = tempFolder.newFile("test.apk")
        file.writeText("TapBot Test APK Payload")

        val hash = deliveryService.computeSha256(file)
        assertNotNull(hash)
        assertEquals(64, hash.length) // 64 hex characters for SHA-256

        // Computing again yields identical hash
        val hash2 = deliveryService.computeSha256(file)
        assertEquals(hash, hash2)
    }

    @Test
    fun `getApkInfo reads size, generates checksum, and checks limit`() {
        val file = tempFolder.newFile("sample.apk")
        file.writeBytes(ByteArray(1024 * 1024)) // 1 MB file

        val info = deliveryService.getApkInfo(file)
        assertEquals(1024 * 1024L, info.sizeBytes)
        assertEquals(1.0, info.sizeMb, 0.05)
        assertEquals(64, info.sha256Checksum.length)
        assertFalse(info.exceedsTelegramLimit)

        val caption = deliveryService.formatApkCaption(info)
        assertTrue(caption.contains("1.00 MB"))
        assertTrue(caption.contains(info.sha256Checksum))
        assertTrue(caption.contains("TapBot APK"))
    }

    @Test
    fun `flags files exceeding 50MB upload limit`() {
        val customService = ApkDeliveryService(context = null, maxUploadBytes = 1000L)
        val file = tempFolder.newFile("large.apk")
        file.writeBytes(ByteArray(2000))

        val info = customService.getApkInfo(file)
        assertTrue(info.exceedsTelegramLimit)

        val limitMsg = customService.formatLimitExceededMessage(info)
        assertTrue(limitMsg.contains("Exceeds Telegram 50MB bot upload limit"))
    }

    @Test
    fun `deliverApk uploads document and sends chat action`() = runBlocking {
        val fakeApi = TestTelegramApiClient()

        // Create a fake test APK in the releases folder for fallback resolution
        val releasesDir = File("releases")
        releasesDir.mkdirs()
        val testApk = File(releasesDir, "test-deliverable.apk")
        testApk.writeText("Mock Android Package Data")

        try {
            val result = deliveryService.deliverApk(fakeApi, "fake_token", 12345L)
            assertTrue(result is ApkDeliveryResult.Sent)
            assertEquals(1, fakeApi.sentChatActions.size)
            assertEquals("upload_document", fakeApi.sentChatActions[0].second)
            assertEquals(1, fakeApi.sentDocuments.size)
            assertEquals(12345L, fakeApi.sentDocuments[0].first)
            assertTrue(fakeApi.sentDocuments[0].third?.contains("TapBot APK") == true)
        } finally {
            testApk.delete()
        }
    }
}
