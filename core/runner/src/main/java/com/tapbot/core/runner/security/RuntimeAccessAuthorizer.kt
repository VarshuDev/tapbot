package com.tapbot.core.runner.security

import com.tapbot.core.model.TelegramChatMember
import com.tapbot.core.runner.runtime.RuntimeContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared authorization helper enforcing fail-closed owner checks, chat allowlists,
 * and cached Telegram group administrator verification across bot runtimes.
 */
object RuntimeAccessAuthorizer {

    private data class CachedMember(
        val member: TelegramChatMember?,
        val fetchedAtMs: Long
    )

    private const val DEFAULT_CACHE_TTL_MS = 60_000L
    private val adminCache = ConcurrentHashMap<Pair<Long, Long>, CachedMember>()

    fun configuredOwnerId(ctx: RuntimeContext): String? {
        return ctx.credentials["owner_id"]?.trim()?.takeIf { it.isNotBlank() }
    }

    fun isConfiguredOwner(ctx: RuntimeContext, senderId: Long?): Boolean {
        val owner = configuredOwnerId(ctx) ?: return false
        if (senderId == null) return false
        return senderId.toString() == owner
    }

    /**
     * Enforces optional `allowed_chat_ids` CSV credential when configured.
     */
    fun isChatAllowed(ctx: RuntimeContext, chatId: Long): Boolean {
        val raw = ctx.credentials["allowed_chat_ids"]?.trim().orEmpty()
        if (raw.isBlank()) return true
        val allowed = raw.split(',', ';', ' ')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
        return allowed.isEmpty() || chatId in allowed
    }

    /**
     * Checks whether [senderId] holds `creator` or `administrator` role in [chatId],
     * using short-lived caching and failing closed if Telegram API fails.
     */
    suspend fun getVerifiedGroupAdmin(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        senderId: Long?,
        nowMs: Long = System.currentTimeMillis(),
        ttlMs: Long = DEFAULT_CACHE_TTL_MS
    ): TelegramChatMember? {
        if (senderId == null || senderId <= 0L) return null

        val key = chatId to senderId
        val cached = adminCache[key]
        if (cached != null && (nowMs - cached.fetchedAtMs) <= ttlMs) {
            return cached.member
        }

        val memberRes = ctx.telegramApi.getChatMember(token, chatId, senderId)
        val member = memberRes.getOrNull()?.takeIf {
            it.status == "creator" || it.status == "administrator"
        }
        if (memberRes.isSuccess) {
            adminCache[key] = CachedMember(member, nowMs)
        }
        return member
    }

    suspend fun isAuthorizedForPrivilegedAction(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        chatType: String,
        senderId: Long?,
        requireOwnerEvenInPrivate: Boolean = false
    ): Boolean {
        if (!isChatAllowed(ctx, chatId)) return false

        val owner = configuredOwnerId(ctx)
        if (owner != null) {
            return senderId != null && senderId.toString() == owner
        }

        if (requireOwnerEvenInPrivate) return false

        if (chatType.equals("private", ignoreCase = true)) {
            return true
        }

        return getVerifiedGroupAdmin(ctx, token, chatId, senderId) != null
    }

    fun clearCache() {
        adminCache.clear()
    }
}
