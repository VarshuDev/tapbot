package com.tapbot.core.security

import java.util.concurrent.CopyOnWriteArraySet

/**
 * High-performance secret redaction engine.
 * Ensures user Telegram Bot Tokens, OpenAI API keys, Bearer tokens, and sensitive credentials
 * are NEVER printed in logs, console outputs, error traces, crash reports, or network debug statements.
 */
object SecretRedactor {

    // Matches standard Telegram Bot Tokens: e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ_12345
    private val TELEGRAM_TOKEN_REGEX = Regex(
        """\b(\d{6,12}):([a-zA-Z0-9_-]{30,50})\b"""
    )

    // Matches Telegram bot URLs: e.g. /bot123456789:ABC.../
    private val TELEGRAM_URL_BOT_REGEX = Regex(
        """/bot(\d{6,12}:[a-zA-Z0-9_-]{30,50})/"""
    )

    // Matches OpenAI API Keys: e.g. sk-..., sk-proj-..., sk-svcacct-...
    private val OPENAI_API_KEY_REGEX = Regex(
        """\bsk-(?:proj-|svcacct-)?[a-zA-Z0-9_-]{20,90}\b"""
    )

    // Matches Anthropic API Keys: e.g. sk-ant-...
    private val ANTHROPIC_API_KEY_REGEX = Regex(
        """\bsk-ant-[a-zA-Z0-9_-]{20,90}\b"""
    )

    // Matches Google / Gemini API Keys: e.g. AIzaSy...
    private val GEMINI_API_KEY_REGEX = Regex(
        """\bAIza[0-9A-Za-z_-]{33,39}\b"""
    )

    // Matches Bearer authorization headers: e.g. Bearer eyJhbGciOi...
    private val BEARER_TOKEN_REGEX = Regex(
        """(?i)\b(Bearer\s+)[a-zA-Z0-9_\-\.=]{16,}\b"""
    )

    // Matches HTTP header credentials (Authorization, x-goog-api-key, x-api-key, etc.)
    private val HTTP_HEADER_SECRET_REGEX = Regex(
        """(?i)((?:x-goog-api-key|x-api-key|x-auth-token|x-webhook-secret)\s*:\s*)(?!\[REDACTED)([^\r\n,;"]+)"""
    )

    // Matches URL query string credentials (?key=..., &token=..., &api_key=...)
    private val URL_QUERY_SECRET_REGEX = Regex(
        """(?i)([?&](?:api[_-]?key|key|token|bot_token|access_token|secret|password|auth)=)([^&\s#]+)"""
    )

    // Matches JSON / key-value credential fields: e.g. "bot_token": "secret123"
    private val KEY_VALUE_CREDENTIAL_REGEX = Regex(
        """(?i)("?(?:bot_token|access_token|webhook_secret|auth_token|token|api_?key|secret|password)"?\s*[:=]\s*")([^"]{6,})(")"""
    )

    // Set of user-configured dynamic secrets registered at runtime
    private val registeredSecrets = CopyOnWriteArraySet<String>()

    /**
     * Dynamically registers a secret string currently in use by the application.
     * Any occurrence of this string in logs, crash dumps, or error messages will be masked.
     */
    fun registerSecret(secret: String?) {
        if (!secret.isNullOrBlank() && secret.trim().length >= 4) {
            registeredSecrets.add(secret.trim())
        }
    }

    /**
     * Removes a previously registered secret string from the in-memory set.
     */
    fun unregisterSecret(secret: String?) {
        if (!secret.isNullOrBlank()) {
            registeredSecrets.remove(secret.trim())
        }
    }

    /**
     * Clears all registered dynamic secrets.
     */
    fun clearRegisteredSecrets() {
        registeredSecrets.clear()
    }

    /**
     * Sanitizes any throwable message safely.
     */
    fun redactThrowable(throwable: Throwable?, fallback: String = "Operation failed"): String {
        val raw = throwable?.message?.takeIf { it.isNotBlank() } ?: fallback
        return redact(raw)
    }

    /**
     * Sanitizes any text string by replacing detected Telegram bot tokens, API keys,
     * bearer tokens, URL query secrets, headers, and registered secrets with safe redacted masks.
     */
    fun redact(text: String?): String {
        if (text == null || text.isEmpty()) return ""

        var sanitized: String = text

        // 1. Redact dynamically registered secrets first (longest first to avoid partial match leaks)
        if (registeredSecrets.isNotEmpty()) {
            for (secret in registeredSecrets.sortedByDescending { it.length }) {
                if (sanitized.contains(secret)) {
                    sanitized = sanitized.replace(secret, "[REDACTED_SECRET]")
                }
            }
        }

        // 2. Redact in Telegram API URLs
        sanitized = TELEGRAM_URL_BOT_REGEX.replace(sanitized) { matchResult ->
            val fullToken = matchResult.groupValues[1]
            val masked = maskTelegramToken(fullToken)
            "/bot$masked/"
        }

        // 3. Redact raw Telegram tokens in text
        sanitized = TELEGRAM_TOKEN_REGEX.replace(sanitized) { matchResult ->
            val fullToken = matchResult.value
            maskTelegramToken(fullToken)
        }

        // 4. Redact Anthropic keys (before generic sk- regex)
        sanitized = ANTHROPIC_API_KEY_REGEX.replace(sanitized) { matchResult ->
            val key = matchResult.value
            val prefix = key.take(7)
            "${prefix}****:[REDACTED_API_KEY]"
        }

        // 5. Redact OpenAI keys
        sanitized = OPENAI_API_KEY_REGEX.replace(sanitized) { matchResult ->
            val key = matchResult.value
            val prefix = key.take(7)
            "${prefix}****:[REDACTED_API_KEY]"
        }

        // 6. Redact Google / Gemini keys
        sanitized = GEMINI_API_KEY_REGEX.replace(sanitized) { matchResult ->
            val key = matchResult.value
            val prefix = key.take(6)
            "${prefix}****:[REDACTED_API_KEY]"
        }

        // 7. Redact Bearer tokens
        sanitized = BEARER_TOKEN_REGEX.replace(sanitized) { matchResult ->
            val prefix = matchResult.groupValues[1]
            "${prefix}[REDACTED_BEARER_TOKEN]"
        }

        // 8. Redact HTTP header credentials
        sanitized = HTTP_HEADER_SECRET_REGEX.replace(sanitized) { matchResult ->
            val prefix = matchResult.groupValues[1]
            "${prefix}[REDACTED_SECRET]"
        }

        // 9. Redact URL query parameter secrets
        sanitized = URL_QUERY_SECRET_REGEX.replace(sanitized) { matchResult ->
            val prefix = matchResult.groupValues[1]
            "${prefix}[REDACTED_CREDENTIAL]"
        }

        // 10. Redact JSON key-value credentials
        sanitized = KEY_VALUE_CREDENTIAL_REGEX.replace(sanitized) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val suffix = matchResult.groupValues[3]
            "${prefix}[REDACTED_CREDENTIAL]${suffix}"
        }

        return sanitized
    }

    private fun maskTelegramToken(token: String): String {
        val parts = token.split(":")
        return if (parts.size == 2) {
            val botIdPrefix = parts[0].take(4)
            "${botIdPrefix}****:[REDACTED_TELEGRAM_TOKEN]"
        } else {
            "[REDACTED_TELEGRAM_TOKEN]"
        }
    }
}
