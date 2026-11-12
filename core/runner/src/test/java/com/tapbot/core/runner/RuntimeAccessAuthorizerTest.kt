package com.tapbot.core.runner

import com.tapbot.core.model.TelegramChatMember
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.runtime.RuntimeContext
import com.tapbot.core.runner.security.RuntimeAccessAuthorizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeAccessAuthorizerTest {

    @After
    fun tearDown() {
        RuntimeAccessAuthorizer.clearCache()
    }

    private fun makeCtx(
        creds: Map<String, String> = emptyMap(),
        api: TelegramApiClient = TelegramApiClient()
    ): RuntimeContext {
        return RuntimeContext(
            botId = "test_bot",
            token = "tok",
            credentials = creds,
            telegramApi = api,
            log = { _, _, _ -> },
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `isConfiguredOwner fails closed when owner_id missing or mismatched`() {
        assertFalse(RuntimeAccessAuthorizer.isConfiguredOwner(makeCtx(), 100L))
        assertFalse(RuntimeAccessAuthorizer.isConfiguredOwner(makeCtx(mapOf("owner_id" to "  ")), 100L))
        assertFalse(RuntimeAccessAuthorizer.isConfiguredOwner(makeCtx(mapOf("owner_id" to "100")), 200L))
        assertFalse(RuntimeAccessAuthorizer.isConfiguredOwner(makeCtx(mapOf("owner_id" to "100")), null))
        assertTrue(RuntimeAccessAuthorizer.isConfiguredOwner(makeCtx(mapOf("owner_id" to "100")), 100L))
    }

    @Test
    fun `isChatAllowed enforces csv allowlist when configured`() {
        assertTrue(RuntimeAccessAuthorizer.isChatAllowed(makeCtx(), 999L))
        val restricted = makeCtx(mapOf("allowed_chat_ids" to "100, -10012345 ; 42"))
        assertTrue(RuntimeAccessAuthorizer.isChatAllowed(restricted, 100L))
        assertTrue(RuntimeAccessAuthorizer.isChatAllowed(restricted, -10012345L))
        assertTrue(RuntimeAccessAuthorizer.isChatAllowed(restricted, 42L))
        assertFalse(RuntimeAccessAuthorizer.isChatAllowed(restricted, 999L))
    }

    @Test
    fun `getVerifiedGroupAdmin caches admin lookup within TTL and fails closed on API failure`() = runBlocking {
        var calls = 0
        val fakeApi = object : TelegramApiClient() {
            override suspend fun getChatMember(token: String, chatId: Long, userId: Long): Result<TelegramChatMember> {
                calls++
                return when (userId) {
                    1L -> Result.success(
                        TelegramChatMember(
                            user = TelegramUser(1L, false, "Admin"),
                            status = "administrator",
                            canRestrictMembers = true
                        )
                    )
                    2L -> Result.success(
                        TelegramChatMember(
                            user = TelegramUser(2L, false, "Member"),
                            status = "member"
                        )
                    )
                    else -> Result.failure(IllegalStateException("Network error"))
                }
            }
        }
        val ctx = makeCtx(api = fakeApi)

        val adminFirst = RuntimeAccessAuthorizer.getVerifiedGroupAdmin(ctx, "tok", -100L, 1L, nowMs = 1000L, ttlMs = 60_000L)
        assertNotNull(adminFirst)
        val adminCached = RuntimeAccessAuthorizer.getVerifiedGroupAdmin(ctx, "tok", -100L, 1L, nowMs = 5000L, ttlMs = 60_000L)
        assertNotNull(adminCached)
        assertEquals(1, calls)

        assertNull(RuntimeAccessAuthorizer.getVerifiedGroupAdmin(ctx, "tok", -100L, 2L, nowMs = 5000L))
        assertNull(RuntimeAccessAuthorizer.getVerifiedGroupAdmin(ctx, "tok", -100L, 3L, nowMs = 5000L))
    }
}
