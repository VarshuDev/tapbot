package com.tapbot.core.model

import kotlinx.serialization.Serializable

/**
 * Sensitive credentials payload encrypted with AES-256-GCM using a user-supplied passphrase.
 */
@Serializable
data class EncryptedCredentialsPayload(
    val credentialsByBotId: Map<String, Map<String, String>> = emptyMap(),
    val globalToken: String? = null
)

/**
 * Self-contained encrypted backup archive format (.tapbotbackup) for full fleet migration.
 * Contains instance configurations, no-code custom bot definitions, and an AES-256-GCM encrypted
 * credentials vault protected by PBKDF2WithHmacSHA256 key derivation.
 */
@Serializable
data class FleetBackupArchive(
    val formatVersion: Int = 1,
    val appVersion: String = "2.7.0",
    val createdAtTimestampMs: Long = System.currentTimeMillis(),
    val instances: List<BotInstance> = emptyList(),
    val customBots: List<CustomBotDefinition> = emptyList(),
    val saltHex: String,
    val ivHex: String,
    val encryptedPayloadBase64: String
)
