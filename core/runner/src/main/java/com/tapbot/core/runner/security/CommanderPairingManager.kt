package com.tapbot.core.runner.security

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Cryptographic one-time pairing token manager for binding TapBot Commander `owner_id`.
 * Ensures a newly installed Commander bot fails closed until the host owner either:
 * 1) Configures their numeric Telegram ID in encrypted credentials, OR
 * 2) Consumes an in-app one-time pairing code via `/pair <code>` in a private DM.
 */
object CommanderPairingManager {

    data class PairingChallenge(
        val code: String,
        val expiresAtMs: Long,
        val remainingAttempts: Int
    )

    sealed class PairingVerificationResult {
        data object Success : PairingVerificationResult()
        data object NoActiveChallenge : PairingVerificationResult()
        data object Expired : PairingVerificationResult()
        data class InvalidCode(val remainingAttempts: Int) : PairingVerificationResult()
        data object LockedOut : PairingVerificationResult()
    }

    private data class InternalChallenge(
        val code: String,
        val expiresAtMs: Long,
        var failedAttempts: Int = 0
    )

    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val CODE_LENGTH = 6
    private const val DEFAULT_TTL_MS = 10L * 60L * 1000L // 10 minutes
    private const val MAX_ATTEMPTS = 5

    private val random = SecureRandom()
    private val challenges = ConcurrentHashMap<String, InternalChallenge>()

    fun issuePairingCode(
        botId: String,
        ttlMs: Long = DEFAULT_TTL_MS,
        nowMs: Long = System.currentTimeMillis()
    ): PairingChallenge {
        val codeChars = CharArray(CODE_LENGTH) {
            CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]
        }
        val code = String(codeChars)
        val challenge = InternalChallenge(
            code = code,
            expiresAtMs = nowMs + ttlMs
        )
        challenges[botId] = challenge
        return PairingChallenge(
            code = code,
            expiresAtMs = challenge.expiresAtMs,
            remainingAttempts = MAX_ATTEMPTS
        )
    }

    fun getActiveCode(
        botId: String,
        nowMs: Long = System.currentTimeMillis()
    ): PairingChallenge? {
        val challenge = challenges[botId] ?: return null
        if (nowMs > challenge.expiresAtMs || challenge.failedAttempts >= MAX_ATTEMPTS) {
            challenges.remove(botId, challenge)
            return null
        }
        return PairingChallenge(
            code = challenge.code,
            expiresAtMs = challenge.expiresAtMs,
            remainingAttempts = MAX_ATTEMPTS - challenge.failedAttempts
        )
    }

    fun verifyAndConsume(
        botId: String,
        candidateCode: String,
        nowMs: Long = System.currentTimeMillis()
    ): PairingVerificationResult {
        val challenge = challenges[botId] ?: return PairingVerificationResult.NoActiveChallenge
        synchronized(challenge) {
            if (nowMs > challenge.expiresAtMs) {
                challenges.remove(botId, challenge)
                return PairingVerificationResult.Expired
            }
            if (challenge.failedAttempts >= MAX_ATTEMPTS) {
                challenges.remove(botId, challenge)
                return PairingVerificationResult.LockedOut
            }

            val normalized = candidateCode.trim().uppercase()
            val expectedBytes = challenge.code.toByteArray(StandardCharsets.UTF_8)
            val actualBytes = normalized.toByteArray(StandardCharsets.UTF_8)
            val matched = expectedBytes.size == actualBytes.size &&
                MessageDigest.isEqual(expectedBytes, actualBytes)

            return if (matched) {
                challenges.remove(botId, challenge)
                PairingVerificationResult.Success
            } else {
                challenge.failedAttempts++
                val remaining = MAX_ATTEMPTS - challenge.failedAttempts
                if (remaining <= 0) {
                    challenges.remove(botId, challenge)
                    PairingVerificationResult.LockedOut
                } else {
                    PairingVerificationResult.InvalidCode(remaining)
                }
            }
        }
    }

    fun revoke(botId: String) {
        challenges.remove(botId)
    }
}
