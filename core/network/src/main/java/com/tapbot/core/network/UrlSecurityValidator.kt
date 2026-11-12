package com.tapbot.core.network

import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Security validator & safe OkHttp factory preventing Server-Side Request Forgery (SSRF),
 * DNS rebinding, unsafe URL schemes, cross-scheme redirects, and unbounded body OOM reads.
 */
object UrlSecurityValidator {

    data class ValidationOptions(
        val requireHttps: Boolean = true,
        val allowLoopbackHosts: Boolean = false
    )

    /**
     * Validates the URL syntax, scheme, embedded credentials, and host literal rules
     * without hitting the network yet.
     */
    fun validateUrlSyntax(
        rawUrl: String,
        options: ValidationOptions = ValidationOptions()
    ): Result<HttpUrl> = runCatching {
        val trimmed = rawUrl.trim()
        require(trimmed.isNotBlank()) { "URL cannot be blank" }

        val parsed = trimmed.toHttpUrlOrNull()
            ?: throw SecurityException("Malformed URL: only valid HTTP(S) URLs are allowed")

        val scheme = parsed.scheme.lowercase()
        if (options.requireHttps && scheme != "https") {
            if (!(options.allowLoopbackHosts && scheme == "http" && isExplicitLoopbackHostname(parsed.host))) {
                throw SecurityException("Insecure URL scheme '$scheme': HTTPS is required")
            }
        } else if (scheme != "https" && scheme != "http") {
            throw SecurityException("Disallowed URL scheme '$scheme'")
        }

        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) {
            throw SecurityException("Embedded credentials in URLs are prohibited")
        }

        val host = parsed.host.lowercase()
        require(host.isNotBlank()) { "URL host cannot be empty" }

        if (!options.allowLoopbackHosts && isBlockedHostname(host)) {
            throw SecurityException("Blocked local or metadata host: '$host'")
        }

        // Reject literal private/loopback IPv4/IPv6 addresses immediately when disallowed
        if (isIpLiteral(host) && !(options.allowLoopbackHosts && isExplicitLoopbackHostname(host))) {
            val addr = InetAddress.getByName(host)
            if (!isPublicRoutableAddress(addr, options.allowLoopbackHosts)) {
                throw SecurityException("Access to private, loopback, or link-local IP addresses is blocked")
            }
        }

        parsed
    }

    /**
     * Validates that [rawUrl] is syntactically valid and resolves solely to safe public IP addresses.
     */
    fun validateUrlAndResolve(
        rawUrl: String,
        options: ValidationOptions = ValidationOptions()
    ): Result<Pair<HttpUrl, List<InetAddress>>> = runCatching {
        val url = validateUrlSyntax(rawUrl, options).getOrThrow()
        val resolved = resolveSafeAddresses(url.host, options.allowLoopbackHosts)
        url to resolved
    }

    fun resolveSafeAddresses(
        hostname: String,
        allowLoopbackHosts: Boolean = false
    ): List<InetAddress> {
        val normalized = hostname.trim().lowercase()
        if (!allowLoopbackHosts && isBlockedHostname(normalized)) {
            throw SecurityException("Blocked local hostname '$normalized'")
        }

        val addresses = InetAddress.getAllByName(hostname).toList()
        if (addresses.isEmpty()) {
            throw IOException("DNS resolution returned no addresses for '$hostname'")
        }
        val allowEmulatorAlias = allowLoopbackHosts && isExplicitLoopbackHostname(normalized)
        for (addr in addresses) {
            if (!allowEmulatorAlias && !isPublicRoutableAddress(addr, allowLoopbackHosts)) {
                throw SecurityException(
                    "Blocked unsafe target IP ${addr.hostAddress} resolved for '$hostname'"
                )
            }
        }
        return addresses
    }

    /**
     * Checks whether [address] is safe to connect to (public unicast IP).
     */
    fun isPublicRoutableAddress(
        address: InetAddress,
        allowLoopback: Boolean = false
    ): Boolean {
        if (allowLoopback && address.isLoopbackAddress) {
            return true
        }
        if (address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return false
        }

        val raw = address.address
        if (address is Inet4Address && raw.size == 4) {
            return isPublicIpv4Bytes(raw)
        }
        if (address is Inet6Address && raw.size == 16) {
            val b0 = raw[0].toInt() and 0xFF
            // IPv6 ULA fc00::/7
            if ((b0 and 0xFE) == 0xFC) return false
            // Unspecified ::/128 or loopback ::1/128 (already caught above, extra defense)
            // IPv4-compatible / IPv4-mapped / 6to4 checks
            if (isIpv4MappedIpv6(raw)) {
                val ipv4 = raw.copyOfRange(12, 16)
                return isPublicIpv4Bytes(ipv4)
            }
        }
        return true
    }

    private fun isPublicIpv4Bytes(raw: ByteArray): Boolean {
        val a = raw[0].toInt() and 0xFF
        val b = raw[1].toInt() and 0xFF
        val c = raw[2].toInt() and 0xFF

        if (a == 0) return false // 0.0.0.0/8 "this network"
        if (a == 10) return false // 10.0.0.0/8 private
        if (a == 100 && (b in 64..127)) return false // 100.64.0.0/10 CGNAT
        if (a == 127) return false // 127.0.0.0/8 loopback
        if (a == 169 && b == 254) return false // 169.254.0.0/16 link-local / cloud metadata
        if (a == 172 && (b in 16..31)) return false // 172.16.0.0/12 private
        if (a == 192 && b == 0 && c == 0) return false // 192.0.0.0/24 IETF protocol
        if (a == 192 && b == 0 && c == 2) return false // TEST-NET-1
        if (a == 192 && b == 168) return false // 192.168.0.0/16 private
        if (a == 198 && (b == 18 || b == 19)) return false // 198.18.0.0/15 benchmark
        if (a == 198 && b == 51 && c == 100) return false // TEST-NET-2
        if (a == 203 && b == 0 && c == 113) return false // TEST-NET-3
        if (a >= 224) return false // Multicast & Reserved (224.0.0.0/4+)
        return true
    }

    private fun isIpv4MappedIpv6(raw: ByteArray): Boolean {
        for (i in 0 until 10) {
            if (raw[i] != 0.toByte()) return false
        }
        return (raw[10] == 0xFF.toByte() && raw[11] == 0xFF.toByte()) ||
            (raw[10] == 0.toByte() && raw[11] == 0.toByte())
    }

    private fun isExplicitLoopbackHostname(host: String): Boolean {
        val h = host.lowercase()
        return h == "localhost" || h == "127.0.0.1" || h == "::1" || h == "10.0.2.2"
    }

    private fun isBlockedHostname(host: String): Boolean {
        return host == "localhost" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host == "metadata.google.internal" ||
            host == "169.254.169.254" ||
            host == "10.0.2.2"
    }

    private fun isIpLiteral(host: String): Boolean {
        if (host.contains(":")) return true
        val parts = host.split('.')
        return parts.size == 4 && parts.all { p -> p.toIntOrNull()?.let { it in 0..255 } == true }
    }

    /**
     * OkHttp [Dns] implementation that validates every resolved IP at connect-time to prevent DNS rebinding.
     */
    class SafePublicDns(
        private val allowLoopbackHosts: Boolean = false
    ) : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return resolveSafeAddresses(hostname, allowLoopbackHosts)
        }
    }

    /**
     * Network interceptor validating the active request URL & target socket address before transmission,
     * ensuring HTTP redirects cannot smuggle internal/non-HTTPS endpoints.
     */
    class SafeOutboundInterceptor(
        private val options: ValidationOptions = ValidationOptions()
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val request = chain.request()
            validateUrlSyntax(request.url.toString(), options).getOrThrow()

            val socketAddr = chain.connection()?.socket()?.inetAddress
            if (socketAddr != null && !isPublicRoutableAddress(socketAddr, options.allowLoopbackHosts)) {
                throw SecurityException("Connection refused to unsafe resolved IP address")
            }
            return chain.proceed(request)
        }
    }

    /**
     * Builds an SSRF-hardened [OkHttpClient].
     */
    fun createSafeHttpClient(
        options: ValidationOptions = ValidationOptions(),
        connectTimeoutSeconds: Long = 15,
        readTimeoutSeconds: Long = 20,
        followRedirects: Boolean = false
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(SafePublicDns(allowLoopbackHosts = options.allowLoopbackHosts))
            .addNetworkInterceptor(SafeOutboundInterceptor(options))
            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)
            .build()
    }

    /**
     * Safely reads an HTTP response body up to [maxBytes], throwing [IOException] if exceeded
     * so unbounded or adversarial payloads cannot trigger OutOfMemoryError.
     */
    fun readResponseBodyCapped(body: ResponseBody?, maxBytes: Long): String {
        if (body == null) return ""
        val declaredLength = body.contentLength()
        if (declaredLength > maxBytes) {
            throw IOException("Response payload exceeds maximum allowed size ($maxBytes bytes)")
        }

        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0L
        body.byteStream().use { input ->
            while (true) {
                val read = input.read(buf)
                if (read == -1) break
                total += read
                if (total > maxBytes) {
                    throw IOException("Response payload exceeded maximum allowed size ($maxBytes bytes)")
                }
                out.write(buf, 0, read)
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
