package com.tapbot.core.network

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Base64

/**
 * Pure-Kotlin RFC 8032 Ed25519 signature verifier compatible with Android API 26+ and JVM unit tests
 * with zero external cryptography dependencies.
 *
 * Accepts:
 * - Public keys in Base64 SPKI/X.509 PEM (`-----BEGIN PUBLIC KEY-----`), Base64 raw 32-byte, or 64-char hex.
 * - Signatures in Base64 or 128-char hex (standard 64-byte RFC 8032 `(R, S)` encoding).
 */
class Ed25519SignatureVerifier : DigitalSignatureVerifier {

    override fun verifySignature(
        packageFile: File,
        signature: String,
        publicKeyPem: String
    ): Result<Boolean> = runCatching {
        require(packageFile.exists() && packageFile.isFile) {
            "Package file does not exist: ${packageFile.name}"
        }
        val keyBytes = parsePublicKey(publicKeyPem)
        val sigBytes = parseSignature(signature)
        val messageBytes = packageFile.readBytes()
        verifyRaw(keyBytes, messageBytes, sigBytes)
    }

    fun verifyBytes(
        payload: ByteArray,
        signature: String,
        publicKeyPem: String
    ): Result<Boolean> = runCatching {
        val keyBytes = parsePublicKey(publicKeyPem)
        val sigBytes = parseSignature(signature)
        verifyRaw(keyBytes, payload, sigBytes)
    }

    internal fun verifyRaw(
        publicKey: ByteArray,
        message: ByteArray,
        signature: ByteArray
    ): Boolean {
        if (publicKey.size != 32 || signature.size != 64) return false

        val aPoint = decodePoint(publicKey) ?: return false
        val rBytes = signature.copyOfRange(0, 32)
        val rPoint = decodePoint(rBytes) ?: return false

        val sBytes = signature.copyOfRange(32, 64)
        val s = decodeLittleEndian(sBytes)
        if (s >= L || s < BigInteger.ZERO) return false

        val digest = MessageDigest.getInstance("SHA-512")
        digest.update(rBytes)
        digest.update(publicKey)
        digest.update(message)
        val k = decodeLittleEndian(digest.digest()).mod(L)

        val lhs = scalarMul(BASE_POINT, s)
        val rhs = pointAdd(rPoint, scalarMul(aPoint, k))
        return pointsEqual(lhs, rhs)
    }

    internal fun parsePublicKey(rawKey: String): ByteArray {
        val cleaned = rawKey.trim()
        require(cleaned.isNotBlank()) { "Ed25519 public key cannot be blank" }

        // 64-char hex raw 32-byte key
        if (cleaned.length == 64 && cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return hexToBytes(cleaned)
        }

        val base64Body = cleaned
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")

        val decoded = Base64.getDecoder().decode(base64Body)
        return when {
            decoded.size == 32 -> decoded
            // X.509 SPKI Ed25519 wrapper: 30 2a 30 05 06 03 2b 65 70 03 21 00 <32-byte key>
            decoded.size == 44 && decoded.copyOfRange(0, 12).contentEquals(SPKI_ED25519_PREFIX) -> {
                decoded.copyOfRange(12, 44)
            }
            else -> throw IllegalArgumentException("Unsupported Ed25519 public key format (${decoded.size} bytes)")
        }
    }

    internal fun parseSignature(signature: String): ByteArray {
        val cleaned = signature.trim()
        require(cleaned.isNotBlank()) { "Signature cannot be blank" }
        if (cleaned.length == 128 && cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return hexToBytes(cleaned)
        }
        val decoded = Base64.getDecoder().decode(cleaned.replace("\\s+".toRegex(), ""))
        if (decoded.size != 64) {
            throw IllegalArgumentException("Invalid Ed25519 signature length: ${decoded.size} bytes")
        }
        return decoded
    }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            out[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }

    private data class ExtPoint(
        val x: BigInteger,
        val y: BigInteger,
        val z: BigInteger,
        val t: BigInteger
    )

    private fun decodeLittleEndian(bytes: ByteArray): BigInteger {
        val reversed = bytes.reversedArray()
        return BigInteger(1, reversed)
    }

    private fun decodePoint(encoded: ByteArray): ExtPoint? {
        if (encoded.size != 32) return null
        val sign = ((encoded[31].toInt() and 0x80) ushr 7)
        val yBytes = encoded.clone()
        yBytes[31] = (yBytes[31].toInt() and 0x7F).toByte()
        val y = decodeLittleEndian(yBytes)
        if (y >= P) return null

        val y2 = y.multiply(y).mod(P)
        val u = y2.subtract(BigInteger.ONE).mod(P)
        val v = D.multiply(y2).add(BigInteger.ONE).mod(P)

        val v3 = v.pow(3).mod(P)
        val v7 = v3.multiply(v3).multiply(v).mod(P)
        val uv3 = u.multiply(v3).mod(P)
        val uv7 = u.multiply(v7).mod(P)

        var x = uv3.multiply(uv7.modPow(P_MINUS_5_DIV_8, P)).mod(P)
        val vx2 = v.multiply(x).multiply(x).mod(P)
        if (vx2 != u) {
            if (vx2 == P.subtract(u).mod(P)) {
                x = x.multiply(MOD_P_SQRT_MINUS_1).mod(P)
            } else {
                return null
            }
        }
        if (x == BigInteger.ZERO && sign == 1) return null
        if (x.testBit(0) != (sign == 1)) {
            x = P.subtract(x).mod(P)
        }
        val t = x.multiply(y).mod(P)
        return ExtPoint(x, y, BigInteger.ONE, t)
    }

    private fun pointAdd(p1: ExtPoint, p2: ExtPoint): ExtPoint {
        val a = p1.y.subtract(p1.x).multiply(p2.y.subtract(p2.x)).mod(P)
        val b = p1.y.add(p1.x).multiply(p2.y.add(p2.x)).mod(P)
        val c = p1.t.multiply(TWO_D).multiply(p2.t).mod(P)
        val d = TWO.multiply(p1.z).multiply(p2.z).mod(P)
        val e = b.subtract(a).mod(P)
        val f = d.subtract(c).mod(P)
        val g = d.add(c).mod(P)
        val h = b.add(a).mod(P)
        return ExtPoint(
            x = e.multiply(f).mod(P),
            y = g.multiply(h).mod(P),
            z = f.multiply(g).mod(P),
            t = e.multiply(h).mod(P)
        )
    }

    private fun scalarMul(point: ExtPoint, scalar: BigInteger): ExtPoint {
        var r0 = NEUTRAL_POINT
        var r1 = point
        val bitLen = scalar.bitLength()
        for (i in 0 until bitLen) {
            if (scalar.testBit(i)) {
                r0 = pointAdd(r0, r1)
            }
            r1 = pointAdd(r1, r1)
        }
        return r0
    }

    private fun pointsEqual(p1: ExtPoint, p2: ExtPoint): Boolean {
        val xEq = p1.x.multiply(p2.z).mod(P) == p2.x.multiply(p1.z).mod(P)
        val yEq = p1.y.multiply(p2.z).mod(P) == p2.y.multiply(p1.z).mod(P)
        return xEq && yEq
    }

    companion object {
        private val TWO = BigInteger.valueOf(2)
        private val P = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19))
        private val L = BigInteger.ONE.shiftLeft(252)
            .add(BigInteger("27742317777372353535851937790883648493"))
        private val D = BigInteger.valueOf(-121665)
            .multiply(BigInteger.valueOf(121666).modInverse(P))
            .mod(P)
        private val TWO_D = TWO.multiply(D).mod(P)
        private val P_MINUS_5_DIV_8 = P.subtract(BigInteger.valueOf(5)).shiftRight(3)
        private val MOD_P_SQRT_MINUS_1 = TWO.modPow(P.subtract(BigInteger.ONE).shiftRight(2), P)

        private val BASE_Y = BigInteger.valueOf(4)
            .multiply(BigInteger.valueOf(5).modInverse(P))
            .mod(P)
        private val BASE_X = run {
            val y2 = BASE_Y.multiply(BASE_Y).mod(P)
            val u = y2.subtract(BigInteger.ONE).mod(P)
            val v = D.multiply(y2).add(BigInteger.ONE).mod(P)
            var x = u.multiply(v.modInverse(P)).modPow(P.add(BigInteger.valueOf(3)).shiftRight(3), P)
            if (v.multiply(x).multiply(x).mod(P) != u) {
                x = x.multiply(MOD_P_SQRT_MINUS_1).mod(P)
            }
            if (x.testBit(0)) x = P.subtract(x).mod(P)
            x
        }
        private val BASE_POINT = ExtPoint(
            x = BASE_X,
            y = BASE_Y,
            z = BigInteger.ONE,
            t = BASE_X.multiply(BASE_Y).mod(P)
        )
        private val NEUTRAL_POINT = ExtPoint(BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO)

        private val SPKI_ED25519_PREFIX = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
        )
    }
}
