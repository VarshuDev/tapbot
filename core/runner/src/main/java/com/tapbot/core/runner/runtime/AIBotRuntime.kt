package com.tapbot.core.runner.runtime

import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.network.GeminiApiClient
import com.tapbot.core.network.GeminiChatMessage
import com.tapbot.core.runner.util.AdaptiveBackoff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device runtime for AI Bot (`bot_ai` / `bot_ai_assistant`).
 * Connects directly from Android to Google Gemini (`generateContent`) with:
 * - Multi-turn per-chat conversation memory (sliding window)
 * - /ask, /summarize (inline or replied message), /code, /model, /system, /clear, /status
 * - Natural private-message conversation (non-slash messages in PM chats)
 * - Automatic 4096-character chunk splitting for Telegram message limits
 */
class AIBotRuntime(
    override val botId: String = "bot_ai",
    private val geminiClient: GeminiApiClient = GeminiApiClient()
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null
    private var startedAtTimestamp: Long = 0L

    private var apiKey: String = ""
    private var defaultModel: String = GeminiApiClient.DEFAULT_MODEL
    private var defaultSystemPrompt: String? = null

    private val chatHistories = ConcurrentHashMap<Long, MutableList<GeminiChatMessage>>()
    private val chatModels = ConcurrentHashMap<Long, String>()
    private val chatSystemPrompts = ConcurrentHashMap<Long, String>()

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
        this.apiKey = context.credentials["gemini_api_key"]?.trim().orEmpty()
        this.defaultModel = context.credentials["gemini_model"]?.trim()?.takeIf { it.isNotBlank() }
            ?: GeminiApiClient.DEFAULT_MODEL
        this.defaultSystemPrompt = context.credentials["system_prompt"]?.trim()?.takeIf { it.isNotBlank() }
    }

    override suspend fun start() {
        val ctx = context ?: throw IllegalStateException("Runtime not initialized")
        val token = ctx.token

        if (token.isBlank()) {
            val err = "Cannot start AI Bot: Empty Telegram Bot Token."
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        _state.value = BotRuntimeState.Starting
        startedAtTimestamp = System.currentTimeMillis()
        ctx.log(LogLevel.INFO, TAG, "Starting Gemini AI Bot runtime (model=$defaultModel)...")

        val meResult = ctx.telegramApi.getMe(token)
        if (meResult.isFailure) {
            val err = "Telegram authentication failed: ${meResult.exceptionOrNull()?.message}"
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        val botUser = meResult.getOrThrow()
        val username = botUser.username ?: "AIBot"
        ctx.log(LogLevel.INFO, TAG, "AI Bot authenticated as @$username")

        _state.value = BotRuntimeState.Connected(username, botUser.firstName)

        val startedAt = System.currentTimeMillis()
        var pollCounter = 0L
        var messageCounter = 0L

        pollJob = ctx.scope.launch {
            _state.value = BotRuntimeState.Running(
                botUsername = username,
                startedAt = startedAt,
                pollCount = 0L,
                messageCount = 0L
            )
            ctx.log(LogLevel.INFO, TAG, "AI Bot polling active.")
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

                    pollCounter++

                    if (updatesResult.isSuccess) {
                        backoff.recordSuccess()
                        val updates = updatesResult.getOrThrow()
                        for (update in updates) {
                            lastUpdateId = update.updateId
                            val message = update.message ?: continue
                            val text = message.text?.trim().orEmpty()
                            if (text.isBlank()) continue
                            messageCounter++

                            handleMessage(ctx, token, message, username)
                        }

                        _state.value = BotRuntimeState.Running(
                            botUsername = username,
                            startedAt = startedAt,
                            pollCount = pollCounter,
                            messageCount = messageCounter,
                            lastActivityAt = System.currentTimeMillis()
                        )
                    } else {
                        val error = updatesResult.exceptionOrNull()
                        val delayMs = backoff.recordFailure()
                        ctx.log(LogLevel.WARN, TAG, "AI Bot getUpdates API failure: ${error?.message}. Backing off for ${delayMs}ms.")
                        backoff.delayWithBackoff(ctx.networkState)
                    }
                } catch (c: CancellationException) {
                    break
                } catch (e: Exception) {
                    val delayMs = backoff.recordFailure()
                    ctx.log(LogLevel.WARN, TAG, "AI Bot polling transient error: ${e.message}. Backing off for ${delayMs}ms.")
                    backoff.delayWithBackoff(ctx.networkState)
                }
            }
        }
    }

    suspend fun handleMessage(
        ctx: RuntimeContext,
        token: String,
        message: TelegramMessage,
        botUsername: String = "AIBot"
    ): String? {
        val rawText = message.text?.trim().orEmpty()
        if (rawText.isBlank()) return null

        val chatId = message.chat.id
        val isPrivate = message.chat.type.equals("private", ignoreCase = true)

        if (rawText.startsWith("/")) {
            val parts = rawText.split("\\s+".toRegex(), limit = 2)
            val cmdToken = parts[0]
            if (cmdToken.contains("@")) {
                val target = cmdToken.substringAfter("@")
                if (!target.equals(botUsername, ignoreCase = true)) {
                    return null
                }
            }
            val cmd = cmdToken.removePrefix("/").substringBefore("@").lowercase(Locale.ROOT)
            val arg = parts.getOrNull(1)?.trim().orEmpty()

            val reply = when (cmd) {
                "start", "help" -> formatHelpMessage(chatId)
                "ask" -> {
                    if (arg.isBlank()) {
                        "Usage: `/ask <question>`\nTip: In private chat you can also message me directly without `/ask`."
                    } else {
                        queryGeminiWithMemory(ctx, token, chatId, arg)
                    }
                }
                "summarize", "sum" -> {
                    val targetText = arg.ifBlank {
                        message.replyToMessage?.text?.trim()
                            ?: message.replyToMessage?.caption?.trim()
                            ?: ""
                    }
                    if (targetText.isBlank()) {
                        "Please provide text (`/summarize <text>`) or reply `/summarize` to a message."
                    } else {
                        queryGeminiOneShot(
                            ctx = ctx,
                            token = token,
                            chatId = chatId,
                            prompt = "Summarize the following content into concise, clear key bullet points:\n\n$targetText"
                        )
                    }
                }
                "code" -> {
                    if (arg.isBlank()) {
                        "Usage: `/code <language & task>`\nExample: `/code kotlin debounce flow extension`"
                    } else {
                        queryGeminiOneShot(
                            ctx = ctx,
                            token = token,
                            chatId = chatId,
                            prompt = "Provide clean, well-structured code with a short explanation for:\n\n$arg"
                        )
                    }
                }
                "model" -> handleModelCommand(chatId, arg)
                "system" -> handleSystemCommand(chatId, arg)
                "clear", "reset" -> {
                    chatHistories.remove(chatId)
                    "🧹 Cleared conversation history for this chat."
                }
                "status" -> formatStatusMessage(chatId)
                else -> "Unrecognized command `/$cmd`. Send /help to view AI commands, or type any message directly in PM."
            }

            val replyToId = if (isPrivate) null else message.messageId
            sendChunkedReply(ctx, token, chatId, reply, replyToId)
            ctx.log(LogLevel.INFO, TAG, "Handled AI command '/$cmd' -> reply sent.")
            return reply
        }

        // Natural conversation in private chats, or when @mentioned in groups
        val mentionTag = "@$botUsername"
        val isMentioned = rawText.contains(mentionTag, ignoreCase = true)
        if (isPrivate || isMentioned) {
            val prompt = if (isMentioned) {
                rawText.replace(mentionTag, "", ignoreCase = true).trim()
            } else {
                rawText
            }
            if (prompt.isBlank()) return null

            val reply = queryGeminiWithMemory(ctx, token, chatId, prompt)
            val replyToId = if (isPrivate) null else message.messageId
            sendChunkedReply(ctx, token, chatId, reply, replyToId)
            ctx.log(LogLevel.INFO, TAG, "Handled natural AI prompt -> reply sent.")
            return reply
        }

        return null
    }

    private suspend fun queryGeminiWithMemory(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        userPrompt: String
    ): String {
        if (apiKey.isBlank()) {
            return missingKeyMessage()
        }

        runCatching { ctx.telegramApi.sendChatAction(token, chatId, "typing") }

        val history = chatHistories.computeIfAbsent(chatId) { mutableListOf() }
        val snapshot: List<GeminiChatMessage> = synchronized(history) {
            history.add(GeminiChatMessage(role = "user", text = userPrompt))
            while (history.size > MAX_HISTORY_MESSAGES) {
                history.removeAt(0)
            }
            history.toList()
        }

        val model = chatModels[chatId] ?: defaultModel
        val sysPrompt = chatSystemPrompts[chatId] ?: defaultSystemPrompt

        val res = geminiClient.generateContent(
            apiKey = apiKey,
            model = model,
            messages = snapshot,
            systemPrompt = sysPrompt
        )

        return res.fold(
            onSuccess = { replyText ->
                synchronized(history) {
                    history.add(GeminiChatMessage(role = "model", text = replyText))
                    while (history.size > MAX_HISTORY_MESSAGES) {
                        history.removeAt(0)
                    }
                }
                replyText
            },
            onFailure = { err ->
                // Roll back the failed user turn so history stays alternating/clean
                synchronized(history) {
                    if (history.lastOrNull()?.role == "user" && history.lastOrNull()?.text == userPrompt) {
                        history.removeAt(history.size - 1)
                    }
                }
                "⚠️ Gemini request failed: ${err.message ?: "Unknown network/API error"}"
            }
        )
    }

    private suspend fun queryGeminiOneShot(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        prompt: String
    ): String {
        if (apiKey.isBlank()) {
            return missingKeyMessage()
        }
        runCatching { ctx.telegramApi.sendChatAction(token, chatId, "typing") }

        val model = chatModels[chatId] ?: defaultModel
        val sysPrompt = chatSystemPrompts[chatId] ?: defaultSystemPrompt
        val res = geminiClient.generateContent(
            apiKey = apiKey,
            model = model,
            messages = listOf(GeminiChatMessage(role = "user", text = prompt)),
            systemPrompt = sysPrompt
        )

        return res.getOrElse { err ->
            "⚠️ Gemini request failed: ${err.message ?: "Unknown network/API error"}"
        }
    }

    private fun handleModelCommand(chatId: Long, arg: String): String {
        val current = chatModels[chatId] ?: defaultModel
        if (arg.isBlank()) {
            return "🧠 Active Gemini Model: $current\n" +
                "Recommended models:\n" +
                "• gemini-2.5-flash (fast, default)\n" +
                "• gemini-2.5-pro (deep reasoning)\n" +
                "• gemini-2.0-flash\n\n" +
                "Switch anytime with: /model <model_name>"
        }
        val cleaned = arg.split("\\s+".toRegex()).first()
        chatModels[chatId] = cleaned
        return "✅ Switched active Gemini model for this chat to: $cleaned"
    }

    private fun handleSystemCommand(chatId: Long, arg: String): String {
        val current = chatSystemPrompts[chatId] ?: defaultSystemPrompt
        return when {
            arg.isBlank() -> {
                if (current.isNullOrBlank()) {
                    "🧭 No system instruction set.\nSet one with: /system <instruction>"
                } else {
                    "🧭 Current System Instruction:\n$current\n\nClear with: /system clear"
                }
            }
            arg.equals("clear", ignoreCase = true) || arg.equals("reset", ignoreCase = true) -> {
                chatSystemPrompts.remove(chatId)
                defaultSystemPrompt = null
                "🧹 Cleared custom system instruction."
            }
            else -> {
                chatSystemPrompts[chatId] = arg
                "🧭 Updated system instruction for this chat."
            }
        }
    }

    private fun formatHelpMessage(chatId: Long): String {
        val model = chatModels[chatId] ?: defaultModel
        val keyStatus = if (apiKey.isNotBlank()) "Configured" else "Missing Gemini API Key"
        return """
            🤖 TapBot Gemini AI Assistant ($model)
            Status: $keyStatus

            Commands:
            • /ask <prompt> — Ask anything (remembers chat context)
            • /summarize [text] — Summarize text or a replied message
            • /code <task> — Generate code with explanation
            • /model [name] — View or change Gemini model
            • /system [prompt|clear] — View or set AI persona/instruction
            • /clear — Clear conversation history for this chat
            • /status — Show model, memory turns & uptime

            Tip: In private messages, just type naturally without any command prefix!
        """.trimIndent()
    }

    private fun formatStatusMessage(chatId: Long): String {
        val model = chatModels[chatId] ?: defaultModel
        val historyCount = chatHistories[chatId]?.let { synchronized(it) { it.size } } ?: 0
        val uptimeSec = if (startedAtTimestamp > 0L) (System.currentTimeMillis() - startedAtTimestamp) / 1000L else 0L
        val keyBadge = if (apiKey.isNotBlank()) "Configured ✅" else "Missing ⚠️"
        val customSys = if ((chatSystemPrompts[chatId] ?: defaultSystemPrompt).isNullOrBlank()) "None" else "Custom"
        return """
            🤖 Gemini AI Engine Status
            • Model: $model
            • API Key: $keyBadge
            • System Prompt: $customSys
            • Context Memory: $historyCount / $MAX_HISTORY_MESSAGES messages
            • Uptime: ${uptimeSec}s
        """.trimIndent()
    }

    private fun missingKeyMessage(): String {
        return "🔑 Gemini API Key is not configured.\n\n" +
            "To activate AI responses:\n" +
            "1. Open TapBot -> Bot Details -> Credentials\n" +
            "2. Get a free API key at https://aistudio.google.com\n" +
            "3. Paste it into Gemini API Key, tap Save, and restart the bot."
    }

    private suspend fun sendChunkedReply(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        text: String,
        replyToMessageId: Long?
    ) {
        val chunks = splitTelegramChunks(text, TELEGRAM_CHUNK_LIMIT)
        chunks.forEachIndexed { index, chunk ->
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = chatId,
                text = chunk,
                parseMode = null,
                replyToMessageId = if (index == 0) replyToMessageId else null
            )
        }
    }

    override suspend fun stop() {
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
    }

    companion object {
        private const val TAG = "AIBotRuntime"
        private const val MAX_HISTORY_MESSAGES = 20
        private const val TELEGRAM_CHUNK_LIMIT = 4000

        internal fun splitTelegramChunks(text: String, maxLen: Int = TELEGRAM_CHUNK_LIMIT): List<String> {
            if (text.length <= maxLen) return listOf(text)
            val result = mutableListOf<String>()
            var remaining = text
            while (remaining.length > maxLen) {
                var splitAt = remaining.lastIndexOf('\n', maxLen)
                if (splitAt < maxLen / 2) {
                    splitAt = remaining.lastIndexOf(' ', maxLen)
                }
                if (splitAt <= 0) {
                    splitAt = maxLen
                }
                result.add(remaining.substring(0, splitAt).trimEnd())
                remaining = remaining.substring(splitAt).trimStart('\n', ' ')
            }
            if (remaining.isNotEmpty()) {
                result.add(remaining)
            }
            return result
        }
    }
}
