package com.tapbot.core.runner

import com.tapbot.core.runner.security.CommanderPairingManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommanderPairingManagerTest {

    @After
    fun tearDown() {
        CommanderPairingManager.revoke("bot_test")
    }

    @Test
    fun `issuePairingCode verifies valid code once and invalidates after consumption`() {
        val clock = 1_000_000L
        val code = CommanderPairingManager.issuePairingCode("bot_test", nowMs = clock).code
        assertEquals(6, code.length)
        assertNotNull(CommanderPairingManager.getActiveCode("bot_test", nowMs = clock))

        val ok = CommanderPairingManager.verifyAndConsume("bot_test", code.lowercase(), nowMs = clock + 5_000L)
        assertEquals(CommanderPairingManager.PairingVerificationResult.Success, ok)

        assertNull(CommanderPairingManager.getActiveCode("bot_test", nowMs = clock + 5_000L))
        val replay = CommanderPairingManager.verifyAndConsume("bot_test", code, nowMs = clock + 6_000L)
        assertEquals(CommanderPairingManager.PairingVerificationResult.NoActiveChallenge, replay)
    }

    @Test
    fun `expired pairing code returns Expired`() {
        val start = 5_000_000L
        val code = CommanderPairingManager.issuePairingCode("bot_test", nowMs = start).code
        val expiredRes = CommanderPairingManager.verifyAndConsume(
            "bot_test",
            code,
            nowMs = start + 11 * 60_000L
        )
        assertEquals(CommanderPairingManager.PairingVerificationResult.Expired, expiredRes)
        assertNull(CommanderPairingManager.getActiveCode("bot_test", nowMs = start + 11 * 60_000L))
    }

    @Test
    fun `locks out after 5 invalid attempts even if valid code sent on 6th`() {
        val now = 2_000_000L
        val code = CommanderPairingManager.issuePairingCode("bot_test", nowMs = now).code

        repeat(4) { attempt ->
            val res = CommanderPairingManager.verifyAndConsume("bot_test", "WRONG1", nowMs = now + 100L)
            assertTrue(res is CommanderPairingManager.PairingVerificationResult.InvalidCode)
            assertEquals(4 - attempt, (res as CommanderPairingManager.PairingVerificationResult.InvalidCode).remainingAttempts)
        }

        val fifth = CommanderPairingManager.verifyAndConsume("bot_test", "WRONG2", nowMs = now + 200L)
        assertEquals(CommanderPairingManager.PairingVerificationResult.LockedOut, fifth)

        val sixthWithReal = CommanderPairingManager.verifyAndConsume("bot_test", code, nowMs = now + 300L)
        assertEquals(CommanderPairingManager.PairingVerificationResult.NoActiveChallenge, sixthWithReal)
    }
}
