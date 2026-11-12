package com.tapbot.core.runner.runtime

import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.core.model.InlineKeyboardButton
import com.tapbot.core.model.InlineKeyboardMarkup
import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramCallbackQuery
import com.tapbot.core.model.TelegramChat
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.UrlSecurityValidator
import com.tapbot.core.runner.ServiceLocator
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.runner.security.RuntimeAccessAuthorizer
import com.tapbot.core.runner.util.AdaptiveBackoff
import com.tapbot.core.security.SecretRedactor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong

/**
 * No-code custom command & webhook bot runtime executing user-authored [CustomBotDefinition] workflows.
 *
 * Supports:
 * - Exact / slash-command and prefix trigger matching
 * - Template variables: {user}, {username}, {user_id}, {chat_id}, {args}, {date}, {webhook_response}
 * - Live HTTP GET/POST webhook execution via OkHttp
 * - Optional inline keyboard button attachment and callback_query handling
 * - Built-in /start and /help command summaries when not overridden by custom rules
 */
class CustomWorkflowBotRuntime(
    override val botId: String,
    private val definitionOverride: CustomBotDefinition? = null,
    private val repositoryProvider: () -> CustomBotRepository? = { ServiceLocator.customBotRepository },
    private val httpClient: OkHttpClient = UrlSecurityValidator.createSafeHttpClient(
        options = UrlSecurityValidator.ValidationOptions(requireHttps = true),
        connectTimeoutSeconds = 10,
        readTimeoutSeconds = 15,
        followRedirects = false
    ),
    private val dateProvider: () -> String = {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null

    private val pollCounter = AtomicLong(0L)
    private val messageCounter = AtomicLong(0L)

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
    }

    override suspend fun start() {
        val ctx = context ?: throw IllegalStateException("Runtime not initialized")
        val token = ctx.token

        if (token.isBlank()) {
            val err = "Cannot start Custom Workflow Bot: Empty Telegram Bot Token."
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        _state.value = BotRuntimeState.Starting
        pollCounter.set(0L)
        messageCounter.set(0L)
        ctx.log(LogLevel.INFO, TAG, "Starting CustomWorkflowBotRuntime for '$botId'...")

        val meResult = ctx.telegramApi.getMe(token)
        if (meResult.isFailure) {
            val err = "Telegram authentication failed: ${meResult.exceptionOrNull()?.message}"
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        val botUser = meResult.getOrThrow()
        val username = botUser.username ?: botUser.firstName
        ctx.log(LogLevel.INFO, TAG, "Custom workflow bot authenticated as @$username (ID: ${botUser.id})")

        _state.value = BotRuntimeState.Connected(username, botUser.firstName)
        val startedAt = System.currentTimeMillis()

        pollJob = ctx.scope.launch {
            _state.value = BotRuntimeState.Running(
                botUsername = username,
                startedAt = startedAt,
                pollCount = 0L,
                messageCount = 0L
            )
            val backoff = AdaptiveBackoff()

            while (isActive) {
                try {
                    if (!ctx.isNetworkAvailable()) {
                        backoff.delayWithBackoff(ctx.networkState)
                        continue
                    }

                    val offset = lastUpdateId?.plus(1)
                    val updatesResult = ctx.telegramApi.getUpdates(
                        token = token,
                        offset = offset,
                        timeoutSeconds = 25
                    )
                    val polls = pollCounter.incrementAndGet()

                    if (updatesResult.isSuccess) {
                        backoff.recordSuccess()
                        val updates = updatesResult.getOrThrow()
                        for (update in updates) {
                            lastUpdateId = update.updateId

                            val callback = update.callbackQuery
                            if (callback != null) {
                                val msgs = messageCounter.incrementAndGet()
                                handleCallbackQuery(ctx, token, botUser, callback)
                                _state.value = BotRuntimeState.Running(
                                    botUsername = username,
                                    startedAt = startedAt,
                                    pollCount = polls,
                                    messageCount = msgs,
                                    lastActivityAt = System.currentTimeMillis()
                                )
                                continue
                            }

                            val message = update.message ?: continue
                            val text = message.text?.trim() ?: continue
                            if (message.from?.id == botUser.id) continue

                            val msgs = messageCounter.incrementAndGet()
                            handleTextMessage(ctx, token, botUser, message, text)
                            _state.value = BotRuntimeState.Running(
                                botUsername = username,
                                startedAt = startedAt,
                                pollCount = polls,
                                messageCount = msgs,
                                lastActivityAt = System.currentTimeMillis()
                            )
                        }
                    } else {
                        backoff.recordFailure()
                    }

                    backoff.delayWithBackoff(ctx.networkState)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ctx.log(LogLevel.ERROR, TAG, "Polling loop error: ${e.message}")
                    backoff.recordFailure()
                    backoff.delayWithBackoff(ctx.networkState)
                }
            }
        }
    }

    override suspend fun stop() {
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
        context?.log(LogLevel.INFO, TAG, "Custom workflow bot '$botId' stopped.")
    }

    private fun resolveDefinition(): CustomBotDefinition? {
        return definitionOverride ?: repositoryProvider()?.getDefinition(botId)
    }

    private suspend fun handleCallbackQuery(
        ctx: RuntimeContext,
        token: String,
        botUser: TelegramUser,
        callback: TelegramCallbackQuery
    ) {
        val data = callback.data.orEmpty()
        val def = resolveDefinition()
        val msg = callback.message
        val chat = msg?.chat
        if (chat != null && !isSenderAndChatPermitted(ctx, chat.id, callback.from.id)) {
            ctx.telegramApi.answerCallbackQuery(token, callback.id, "⛔ Unauthorized chat or user.", showAlert = true)
            return
        }

        val matchedRule = def?.rules?.firstOrNull { rule ->
            (!rule.buttonCallbackData.isNullOrBlank() && rule.buttonCallbackData == data) ||
                data == "cw:cmd:${rule.command.trim()}" ||
                (rule.id.isNotBlank() && data == "cw:btn:${rule.id}")
        }

        if (matchedRule != null && chat != null) {
            ctx.telegramApi.answerCallbackQuery(token, callback.id, "Executed ${matchedRule.command}")
            executeRule(
                ctx = ctx,
                token = token,
                rule = matchedRule,
                from = callback.from,
                chat = chat,
                replyToMessageId = msg.messageId,
                args = ""
            )
            return
        }

        if (data.startsWith("cw:cmd:") && chat != null) {
            val cmd = data.removePrefix("cw:cmd:")
            ctx.telegramApi.answerCallbackQuery(token, callback.id, "Running $cmd")
            val synthetic = TelegramMessage(
                messageId = msg.messageId,
                from = callback.from,
                chat = chat,
                date = msg.date,
                text = cmd
            )
            handleTextMessage(ctx, token, botUser, synthetic, cmd)
            return
        }

        ctx.telegramApi.answerCallbackQuery(token, callback.id, "Triggered")
    }

    private suspend fun handleTextMessage(
        ctx: RuntimeContext,
        token: String,
        botUser: TelegramUser,
        message: TelegramMessage,
        text: String
    ) {
        if (!isSenderAndChatPermitted(ctx, message.chat.id, message.from?.id)) {
            return
        }
        val definition = resolveDefinition()
        val trimmed = text.trim()
        val parts = trimmed.split("\\s+".toRegex(), limit = 2)
        val rawFirstToken = parts.firstOrNull().orEmpty()
        val cmdToken = rawFirstToken.substringBefore("@")
        val trailingArgs = if (parts.size > 1) parts[1].trim() else ""

        // 1. Check configured user workflow rules first
        val rules = definition?.rules.orEmpty()
        for (rule in rules) {
            val matchedArgs = matchRule(rule, trimmed, cmdToken, trailingArgs)
            if (matchedArgs != null) {
                executeRule(
                    ctx = ctx,
                    token = token,
                    rule = rule,
                    from = message.from,
                    chat = message.chat,
                    replyToMessageId = message.messageId,
                    args = matchedArgs
                )
                return
            }
        }

        // 2. Built-in /start and /help if not overridden by user rule
        if (cmdToken.equals("/start", ignoreCase = true)) {
            val welcomeTpl = definition?.welcomeMessage
                ?: "Hello {user}! Custom workflow bot is online on TapBot."
            val renderedWelcome = substituteVariables(
                template = welcomeTpl,
                from = message.from,
                chat = message.chat,
                args = trailingArgs,
                webhookResponse = ""
            )
            val helpSuffix = if (rules.isNotEmpty()) {
                val cmdList = rules.joinToString("\n") { r ->
                    val desc = if (r.description.isNotBlank()) " — ${r.description}" else ""
                    "• ${r.command.trim()}$desc"
                }
                "\n\nAvailable commands:\n$cmdList"
            } else {
                ""
            }
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = message.chat.id,
                text = renderedWelcome + helpSuffix,
                parseMode = null,
                replyToMessageId = message.messageId
            )
            return
        }

        if (cmdToken.equals("/help", ignoreCase = true)) {
            val botName = definition?.name ?: "Custom Workflow Bot"
            val lines = if (rules.isEmpty()) {
                "No custom command rules configured yet."
            } else {
                rules.joinToString("\n") { r ->
                    val desc = if (r.description.isNotBlank()) " — ${r.description}" else ""
                    "• ${r.command.trim()}$desc"
                }
            }
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = message.chat.id,
                text = "$botName — Commands:\n$lines",
                parseMode = null,
                replyToMessageId = message.messageId
            )
            return
        }

        // 3. Optional fallback reply
        val fallback = definition?.fallbackMessage?.takeIf { it.isNotBlank() }
        if (fallback != null && message.chat.id > 0) {
            val renderedFallback = substituteVariables(
                template = fallback,
                from = message.from,
                chat = message.chat,
                args = trimmed,
                webhookResponse = ""
            )
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = message.chat.id,
                text = renderedFallback,
                parseMode = null,
                replyToMessageId = message.messageId
            )
        }
    }

    /**
     * Returns extracted `{args}` if [rule] matches input text, or null if no match.
     */
    private fun matchRule(
        rule: CommandRule,
        fullText: String,
        cmdToken: String,
        trailingArgs: String
    ): String? {
        val ruleCmd = rule.command.trim()
        if (ruleCmd.isBlank()) return null

        return if (ruleCmd.startsWith("/")) {
            if (!cmdToken.equals(ruleCmd, ignoreCase = true)) {
                null
            } else if (!rule.matchPrefix && trailingArgs.isNotBlank()) {
                null
            } else {
                trailingArgs
            }
        } else {
            val normalizedRule = ruleCmd.lowercase(Locale.ROOT)
            val normalizedFull = fullText.lowercase(Locale.ROOT)
            when {
                normalizedFull == normalizedRule -> ""
                rule.matchPrefix && normalizedFull.startsWith("$normalizedRule ") -> {
                    fullText.substring(ruleCmd.length).trim()
                }
                else -> null
            }
        }
    }

    private suspend fun executeRule(
        ctx: RuntimeContext,
        token: String,
        rule: CommandRule,
        from: TelegramUser?,
        chat: TelegramChat,
        replyToMessageId: Long?,
        args: String
    ) {
        val webhookResponse = if (!rule.webhookUrl.isNullOrBlank()) {
            callWebhook(rule, from, chat, args)
        } else {
            ""
        }

        val rendered = substituteVariables(
            template = rule.responseTemplate,
            from = from,
            chat = chat,
            args = args,
            webhookResponse = webhookResponse
        )

        val btnLabel = rule.buttonLabel?.trim()?.takeIf { it.isNotEmpty() }
        if (btnLabel != null) {
            val cbData = rule.buttonCallbackData?.trim()?.takeIf { it.isNotEmpty() }
                ?: if (rule.id.isNotBlank()) "cw:btn:${rule.id}" else "cw:cmd:${rule.command.trim()}"
            val markup = InlineKeyboardMarkup(
                inlineKeyboard = listOf(
                    listOf(InlineKeyboardButton(text = btnLabel, callbackData = cbData))
                )
            )
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = chat.id,
                text = rendered,
                parseMode = null,
                replyToMessageId = replyToMessageId,
                replyMarkup = markup
            )
        } else {
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = chat.id,
                text = rendered,
                parseMode = null,
                replyToMessageId = replyToMessageId
            )
        }

        ctx.log(LogLevel.INFO, TAG, "Executed rule '${rule.command}' for chat ${chat.id}")
    }

    private suspend fun callWebhook(
        rule: CommandRule,
        from: TelegramUser?,
        chat: TelegramChat,
        args: String
    ): String = withContext(Dispatchers.IO) {
        val rawUrl = rule.webhookUrl?.trim().orEmpty()
        val preCheck = UrlSecurityValidator.validateUrlSyntax(
            rawUrl,
            UrlSecurityValidator.ValidationOptions(requireHttps = true)
        )
        if (preCheck.isFailure) {
            return@withContext "Blocked webhook URL: ${preCheck.exceptionOrNull()?.message ?: "HTTPS public host required"}"
        }

        val resolvedUrl = substituteUrlVariables(rawUrl, from, chat, args)
        val postCheck = UrlSecurityValidator.validateUrlSyntax(
            resolvedUrl,
            UrlSecurityValidator.ValidationOptions(requireHttps = true)
        )
        if (postCheck.isFailure) {
            return@withContext "Blocked resolved webhook URL: ${postCheck.exceptionOrNull()?.message ?: "Invalid target"}"
        }

        val reqBuilder = Request.Builder().url(resolvedUrl)
        for ((k, v) in rule.webhookHeaders) {
            if (k.isNotBlank()) {
                reqBuilder.header(k, v)
            }
        }

        when (rule.webhookMethod) {
            CustomHttpMethod.GET -> reqBuilder.get()
            CustomHttpMethod.POST -> {
                val bodyTemplate = rule.webhookBodyTemplate?.takeIf { it.isNotBlank() }
                val payload = if (bodyTemplate != null) {
                    substituteVariables(
                        template = bodyTemplate,
                        from = from,
                        chat = chat,
                        args = args,
                        webhookResponse = "",
                        escapeJson = true
                    )
                } else {
                    buildJsonObject {
                        put("botId", botId)
                        put("command", rule.command)
                        put("userId", from?.id ?: 0L)
                        put("user", from?.firstName ?: from?.username ?: "User")
                        put("chatId", chat.id)
                        put("args", args)
                        put("date", dateProvider())
                    }.toString()
                }
                reqBuilder.post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            }
        }

        try {
            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val body = UrlSecurityValidator.readResponseBodyCapped(
                    response.body,
                    MAX_WEBHOOK_RESPONSE_BYTES
                ).trim()
                val safeBody = SecretRedactor.redact(body)
                if (response.isSuccessful) {
                    safeBody.ifBlank { "HTTP ${response.code} OK" }
                } else {
                    "Webhook HTTP ${response.code}: ${safeBody.ifBlank { response.message }}"
                }
            }
        } catch (e: Exception) {
            "Webhook error: ${SecretRedactor.redactThrowable(e, "Request failed")}"
        }
    }

    private fun substituteUrlVariables(
        rawUrl: String,
        from: TelegramUser?,
        chat: TelegramChat,
        args: String
    ): String {
        val qIdx = rawUrl.indexOf('?')
        val user = from?.firstName ?: from?.username ?: "User"
        val username = from?.username?.let { "@$it" } ?: user
        val userId = (from?.id ?: 0L).toString()
        val chatId = chat.id.toString()
        val date = dateProvider()

        return if (qIdx >= 0) {
            val pathPart = rawUrl.substring(0, qIdx)
                .replace("{user_id}", userId)
                .replace("{chat_id}", chatId)
                .replace("{args}", encodeUrl(args))
                .replace("{user}", encodeUrl(user))
                .replace("{username}", encodeUrl(username))
                .replace("{date}", encodeUrl(date))
            val queryPart = rawUrl.substring(qIdx)
                .replace("{user_id}", userId)
                .replace("{chat_id}", chatId)
                .replace("{args}", encodeUrl(args))
                .replace("{user}", encodeUrl(user))
                .replace("{username}", encodeUrl(username))
                .replace("{date}", encodeUrl(date))
            pathPart + queryPart
        } else {
            rawUrl
                .replace("{user_id}", userId)
                .replace("{chat_id}", chatId)
                .replace("{args}", encodeUrl(args))
                .replace("{user}", encodeUrl(user))
                .replace("{username}", encodeUrl(username))
                .replace("{date}", encodeUrl(date))
        }
    }

    private fun encodeUrl(value: String): String {
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    private fun substituteVariables(
        template: String,
        from: TelegramUser?,
        chat: TelegramChat,
        args: String,
        webhookResponse: String,
        escapeJson: Boolean = false
    ): String {
        val rawUser = from?.firstName ?: from?.username ?: "User"
        val rawUsername = from?.username?.let { "@$it" } ?: rawUser
        val userId = (from?.id ?: 0L).toString()
        val chatId = chat.id.toString()
        val rawDate = dateProvider()

        val user = if (escapeJson) escapeJsonStringContent(rawUser) else rawUser
        val username = if (escapeJson) escapeJsonStringContent(rawUsername) else rawUsername
        val safeArgs = if (escapeJson) escapeJsonStringContent(args) else args
        val date = if (escapeJson) escapeJsonStringContent(rawDate) else rawDate
        val resp = if (escapeJson) escapeJsonStringContent(webhookResponse) else webhookResponse

        return template
            .replace("{user}", user)
            .replace("{username}", username)
            .replace("{user_id}", userId)
            .replace("{chat_id}", chatId)
            .replace("{args}", safeArgs)
            .replace("{date}", date)
            .replace("{webhook_response}", resp)
    }

    private fun escapeJsonStringContent(raw: String): String {
        val serialized = JsonPrimitive(raw).toString()
        return if (serialized.length >= 2 && serialized.startsWith("\"") && serialized.endsWith("\"")) {
            serialized.substring(1, serialized.length - 1)
        } else {
            serialized
        }
    }

    private fun isSenderAndChatPermitted(ctx: RuntimeContext, chatId: Long, senderId: Long?): Boolean {
        if (!RuntimeAccessAuthorizer.isChatAllowed(ctx, chatId)) return false
        if (RuntimeAccessAuthorizer.configuredOwnerId(ctx) != null && !RuntimeAccessAuthorizer.isConfiguredOwner(ctx, senderId)) {
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "CustomWorkflowBot"
        private const val MAX_WEBHOOK_RESPONSE_BYTES = 1L * 1024L * 1024L
    }
}
