package com.tapbot.core.security

import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.EncryptedCredentialsPayload
import com.tapbot.core.model.FleetBackupArchive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manager handling authenticated encryption and decryption of TapBot fleet configuration,
 * custom no-code bot definitions, and sensitive hardware credentials into portable `.tapbotbackup` archives.
 *
 * Security architecture:
 * - Key derivation: PBKDF2WithHmacSHA256 with 10,000 iterations and 16-byte random salt.
 * - Cipher: AES-256 in GCM mode with 12-byte random IV and 128-bit authentication tag.
 * - Authenticated decryption rejects wrong passphrases or tampered payload with [SecurityException].
 */
class FleetBackupManager(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }
) {
    private val secureRandom = SecureRandom()

    suspend fun exportArchive(
        passphrase: String,
        instances: List<BotInstance>,
        customBots: List<CustomBotDefinition>,
        credentialStore: CredentialStore
    ): Result<FleetBackupArchive> = runCatching {
        if (passphrase.length < 4) {
            throw IllegalArgumentException("Backup password must be at least 4 characters")
        }

        // 1. Gather all credentials from CredentialStore
        val credsMap = mutableMapOf<String, Map<String, String>>()
        for (instance in instances) {
            val botCreds = credentialStore.getAllCredentials(instance.botId)
            if (botCreds.isNotEmpty()) {
                credsMap[instance.botId] = botCreds
            }
        }
        val globalToken = credentialStore.getToken()

        val payload = EncryptedCredentialsPayload(
            credentialsByBotId = credsMap,
            globalToken = globalToken
        )

        // 2. Encrypt credentials payload with PBKDF2 + AES-256-GCM
        val (saltHex, ivHex, encryptedBase64) = encryptPayload(passphrase, payload)

        // 3. Assemble self-contained archive
        FleetBackupArchive(
            formatVersion = 1,
            appVersion = "2.7.0",
            createdAtTimestampMs = System.currentTimeMillis(),
            instances = instances,
            customBots = customBots,
            saltHex = saltHex,
            ivHex = ivHex,
            encryptedPayloadBase64 = encryptedBase64
        )
    }

    suspend fun exportToFile(
        passphrase: String,
        instances: List<BotInstance>,
        customBots: List<CustomBotDefinition>,
        credentialStore: CredentialStore,
        targetFile: File
    ): Result<FleetBackupArchive> = runCatching {
        val archive = exportArchive(passphrase, instances, customBots, credentialStore).getOrThrow()
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(json.encodeToString(archive))
        archive
    }

    suspend fun restoreFromArchive(
        passphrase: String,
        archive: FleetBackupArchive,
        credentialStore: CredentialStore
    ): Result<FleetBackupArchive> = runCatching {
        if (passphrase.isBlank()) {
            throw IllegalArgumentException("Password cannot be blank")
        }

        // 1. Decrypt and verify credentials payload
        val payload = decryptPayload(
            passphrase = passphrase,
            saltHex = archive.saltHex,
            ivHex = archive.ivHex,
            ciphertextBase64 = archive.encryptedPayloadBase64
        )

        // 2. Restore credentials back into CredentialStore
        for ((botId, creds) in payload.credentialsByBotId) {
            for ((key, secret) in creds) {
                credentialStore.saveCredential(botId, key, secret)
            }
        }
        payload.globalToken?.takeIf { it.isNotBlank() }?.let {
            credentialStore.saveToken(it)
        }

        archive
    }

    suspend fun restoreFromFile(
        passphrase: String,
        sourceFile: File,
        credentialStore: CredentialStore
    ): Result<FleetBackupArchive> = runCatching {
        if (!sourceFile.exists()) {
            throw IllegalArgumentException("Backup file does not exist: ${sourceFile.absolutePath}")
        }
        val text = sourceFile.readText()
        val archive = json.decodeFromString<FleetBackupArchive>(text)
        restoreFromArchive(passphrase, archive, credentialStore).getOrThrow()
    }

    fun encryptPayload(passphrase: String, payload: EncryptedCredentialsPayload): Triple<String, String, String> {
        val salt = ByteArray(16).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(12).apply { secureRandom.nextBytes(this) }
        val secretKey = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val plaintextJson = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintextJson)

        val saltHex = salt.joinToString("") { "%02x".format(it) }
        val ivHex = iv.joinToString("") { "%02x".format(it) }
        val ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext)

        return Triple(saltHex, ivHex, ciphertextBase64)
    }

    fun decryptPayload(
        passphrase: String,
        saltHex: String,
        ivHex: String,
        ciphertextBase64: String
    ): EncryptedCredentialsPayload {
        return try {
            val salt = hexToBytes(saltHex)
            val iv = hexToBytes(ivHex)
            val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
            val secretKey = deriveKey(passphrase, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            json.decodeFromString<EncryptedCredentialsPayload>(jsonString)
        } catch (e: Exception) {
            throw SecurityException("Failed to decrypt backup: Invalid passphrase or corrupted archive", e)
        }
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 10000, 256)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
