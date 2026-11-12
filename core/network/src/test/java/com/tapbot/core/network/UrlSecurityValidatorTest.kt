package com.tapbot.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress

class UrlSecurityValidatorTest {

    @Test
    fun `validateUrlSyntax allows clean public HTTPS URLs`() {
        val res = UrlSecurityValidator.validateUrlSyntax("https://api.example.com/hooks/telegram")
        assertTrue(res.isSuccess)
    }

    @Test
    fun `validateUrlSyntax rejects http when https required and blocks embedded credentials`() {
        assertTrue(UrlSecurityValidator.validateUrlSyntax("http://webhook.example.com/hook").isFailure)
        assertTrue(UrlSecurityValidator.validateUrlSyntax("https://user:pass@webhook.example.com/hook").isFailure)
        assertTrue(UrlSecurityValidator.validateUrlSyntax("file:///etc/passwd").isFailure)
    }

    @Test
    fun `validateUrlSyntax blocks loopback private site-local and metadata IPs`() {
        val blocked = listOf(
            "https://localhost/admin",
            "https://127.0.0.1/api",
            "https://169.254.169.254/latest/meta-data/",
            "https://metadata.google.internal/computeMetadata/v1/",
            "https://10.0.0.1/internal",
            "https://172.16.5.4/internal",
            "https://192.168.1.1/router",
            "https://[::1]/local",
            "https://0.0.0.0/any"
        )
        for (url in blocked) {
            val res = UrlSecurityValidator.validateUrlSyntax(url)
            assertTrue("Expected SSRF URL '$url' to be blocked", res.isFailure)
        }
    }

    @Test
    fun `isPublicRoutableAddress blocks RFC1918 CGNAT link-local ULA and permits public IPv4`() {
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("10.1.2.3")))
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("100.64.0.1")))
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("169.254.169.254")))
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("172.20.0.1")))
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("192.168.0.1")))
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("fc00::1")))
        assertFalse(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("::ffff:127.0.0.1")))
        assertTrue(UrlSecurityValidator.isPublicRoutableAddress(InetAddress.getByName("8.8.8.8")))
    }

    @Test
    fun `loopback override allows local dev server while still rejecting metadata and RFC1918`() {
        val devOpts = UrlSecurityValidator.ValidationOptions(requireHttps = true, allowLoopbackHosts = true)
        assertTrue(UrlSecurityValidator.validateUrlSyntax("http://localhost:8787/api", devOpts).isSuccess)
        assertTrue(UrlSecurityValidator.validateUrlSyntax("http://10.0.2.2:8787/api", devOpts).isSuccess)
        assertTrue(UrlSecurityValidator.validateUrlSyntax("https://169.254.169.254/latest", devOpts).isFailure)
        assertTrue(UrlSecurityValidator.validateUrlSyntax("http://192.168.1.10/api", devOpts).isFailure)
    }

    @Test(expected = IOException::class)
    fun `readResponseBodyCapped aborts oversized response streams`() {
        val oversized = "A".repeat(2048).toResponseBody("text/plain".toMediaType())
        UrlSecurityValidator.readResponseBodyCapped(oversized, maxBytes = 1024L)
    }

    @Test
    fun `readResponseBodyCapped reads bounded payload accurately`() {
        val body = "hello-secure-world".toResponseBody("text/plain".toMediaType())
        assertEquals("hello-secure-world", UrlSecurityValidator.readResponseBodyCapped(body, maxBytes = 1024L))
    }
}
