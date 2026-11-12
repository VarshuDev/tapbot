package com.tapbot.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class PackageVerifierTest {

    private fun sha256Of(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun verifyPackage_matchingSha256_succeeds() = runTest {
        val tempDir = File.createTempFile("verifier_test", "").apply { delete(); mkdirs() }
        val testFile = File(tempDir, "test.botpkg")
        val content = "GENUINE_PACKAGE_CONTENT_12345".toByteArray()
        testFile.writeBytes(content)
        val expectedHash = sha256Of(content)

        val verifier = Sha256PackageVerifier()
        val result = verifier.verifyPackage(testFile, expectedHash)

        assertTrue(result.isSuccess)
        val verification = result.getOrThrow()
        assertTrue(verification.isValid)
        assertEquals(expectedHash, verification.computedSha256)
        assertTrue(testFile.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun verifyPackage_tamperedFile_failsAndDeletesFile() = runTest {
        val tempDir = File.createTempFile("verifier_tamper_test", "").apply { delete(); mkdirs() }
        val testFile = File(tempDir, "tampered.botpkg")
        testFile.writeText("MALICIOUS_TAMPERED_CONTENT")
        val legitHash = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

        val verifier = Sha256PackageVerifier()
        val result = verifier.verifyPackage(testFile, legitHash)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertFalse("Tampered file must be deleted upon failed verification", testFile.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun downgradeAttackChecker_correctlyDetectsRollbacks() {
        // Downgrades
        assertTrue(DowngradeAttackChecker.isDowngrade("1.2.0", "1.1.9"))
        assertTrue(DowngradeAttackChecker.isDowngrade("2.0.0", "1.9.9"))
        assertTrue(DowngradeAttackChecker.isDowngrade("1.5.3", "1.5.2"))

        // Upgrades
        assertFalse(DowngradeAttackChecker.isDowngrade("1.0.0", "1.0.1"))
        assertFalse(DowngradeAttackChecker.isDowngrade("1.0.0", "1.1.0"))
        assertFalse(DowngradeAttackChecker.isDowngrade("1.9.0", "2.0.0"))

        // Same version
        assertFalse(DowngradeAttackChecker.isDowngrade("1.0.0", "1.0.0"))

        // Fresh install (no current version installed)
        assertFalse(DowngradeAttackChecker.isDowngrade(null, "1.0.0"))
    }

    @Test
    fun verifyPackage_rejectsBlankSha256() = runTest {
        val tempDir = File.createTempFile("verifier_blank_sha", "").apply { delete(); mkdirs() }
        val testFile = File(tempDir, "test.botpkg").apply { writeText("PAYLOAD") }

        val verifier = Sha256PackageVerifier()
        val result = verifier.verifyPackage(testFile, "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        tempDir.deleteRecursively()
    }

    @Test
    fun verifyPackage_ed25519SignatureVerification_passesValidAndRejectsTampered() = runTest {
        val tempDir = File.createTempFile("verifier_ed25519", "").apply { delete(); mkdirs() }
        val testFile = File(tempDir, "signed.botpkg")
        val content = "SIGNED_BOT_ARCHIVE_BYTES_V2_4_0".toByteArray()
        testFile.writeBytes(content)
        val sha256 = sha256Of(content)

        // Generate real Ed25519 keypair using JVM KeyPairGenerator and sign package bytes
        val kpg = java.security.KeyPairGenerator.getInstance("Ed25519")
        val keyPair = kpg.generateKeyPair()
        val signer = java.security.Signature.getInstance("Ed25519")
        signer.initSign(keyPair.private)
        signer.update(content)
        val signatureBase64 = java.util.Base64.getEncoder().encodeToString(signer.sign())
        val pubKeyBase64 = java.util.Base64.getEncoder().encodeToString(keyPair.public.encoded)
        val pemPublicKey = "-----BEGIN PUBLIC KEY-----\n$pubKeyBase64\n-----END PUBLIC KEY-----"

        val verifier = Sha256PackageVerifier(
            signatureVerifier = Ed25519SignatureVerifier(),
            trustedPublicKeyPem = pemPublicKey,
            requireSignature = true
        )

        // 1. Valid signature + valid SHA-256 passes
        val okResult = verifier.verifyPackage(testFile, sha256, signatureBase64)
        assertTrue("Valid Ed25519 signature must succeed: ${okResult.exceptionOrNull()?.message}", okResult.isSuccess)
        assertTrue(okResult.getOrThrow().isSignatureVerified)

        // 2. Missing required signature fails and deletes package
        val missingSig = verifier.verifyPackage(testFile, sha256, null)
        assertTrue(missingSig.isFailure)
        assertFalse(testFile.exists())

        // 3. Forged/wrong signature fails and deletes package
        testFile.writeBytes(content)
        val forgedSig = java.util.Base64.getEncoder().encodeToString(ByteArray(64) { 0x42 })
        val badSigRes = verifier.verifyPackage(testFile, sha256, forgedSig)
        assertTrue(badSigRes.isFailure)
        assertFalse(testFile.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun ed25519SignatureVerifier_passesOfficialRfc8032Vector1() {
        val pubHex = "d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a"
        val sigHex = "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e06522490155" +
            "5fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b"

        val result = Ed25519SignatureVerifier().verifyBytes(ByteArray(0), sigHex, pubHex)
        assertTrue(result.isSuccess)
        assertTrue("RFC 8032 test vector #1 must verify true", result.getOrThrow())
    }
}
