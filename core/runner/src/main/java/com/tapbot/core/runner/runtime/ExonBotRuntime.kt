package com.tapbot.core.runner.runtime

import com.tapbot.core.model.InlineKeyboardButton
import com.tapbot.core.model.InlineKeyboardMarkup
import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.PromoteChatMemberPayload
import com.tapbot.core.model.RestrictChatPermissions
import com.tapbot.core.model.TelegramCallbackQuery
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.runner.ServiceLocator
import com.tapbot.core.runner.moderation.ExonModerationStore
import com.tapbot.core.runner.security.RuntimeAccessAuthorizer
import com.tapbot.core.runner.util.AdaptiveBackoff
import com.tapbot.core.security.SecretRedactor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * On-device runtime for Exon Robot 2.0 (ported & extended from VarshuDev/ExonRobot).
 * Provides persistent Telegram group management, timed moderation, auto-moderation, and interactive menus:
 * - Greetings & Help: /start, /help (with interactive category buttons), /alive
 * - Diagnostics: /ping, /stats
 * - Information: /id, /info
 * - Chat Administration: /promote, /demote, /adminlist, /pin, /unpin, /invitelink
 * - Moderation: /ban, /sban, /tban, /unban, /kick
 * - Muting: /mute, /tmute, /unmute
 * - Warning System: /warn, /warns, /resetwarn (Auto-ban on 3 warnings, persisted to disk)
 * - Lock Manager: /lock, /unlock, /locks (Actively enforces & deletes violating messages)
 * - Welcome & Antiflood: /setwelcome, /welcome <on|off>, /clearwelcome, /antiflood <count|off>
 * - Group Rules: /rules, /setrules, /clearrules
 */
class ExonBotRuntime(
    override val botId: String = "bot_exon",
    private val customStore: ExonModerationStore? = null
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null
    private var startTime: Long = 0L

    private var store: ExonModerationStore = customStore ?: ExonModerationStore(null)

    private val pollCounter = AtomicLong(0L)
    private val messageCounter = AtomicLong(0L)

    // Antiflood 10-second sliding windows keyed by (chatId, userId)
    private val floodWindows = ConcurrentHashMap<Pair<Long, Long>, ArrayDeque<Long>>()

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
        if (customStore == null) {
            val aCtx = context.androidContext ?: ServiceLocator.appContext
            store = if (aCtx != null) {
                val safeId = botId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                ExonModerationStore(File(aCtx.filesDir, "exon_moderation_${safeId}.json"))
            } else {
                ExonModerationStore(null)
            }
        } else {
            store = customStore
        }
    }

    override suspend fun start() {
        val ctx = context ?: throw IllegalStateException("Runtime not initialized")
        val token = ctx.token

        if (token.isBlank()) {
            val err = "Cannot start Exon Robot: Empty Telegram Bot Token."
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        _state.value = BotRuntimeState.Starting
        startTime = System.currentTimeMillis()
        pollCounter.set(0L)
        messageCounter.set(0L)
        ctx.log(LogLevel.INFO, TAG, "Starting Exon Robot 2.0 group management runtime on Android ART...")

        val meResult = ctx.telegramApi.getMe(token)
        if (meResult.isFailure) {
            val err = "Telegram authentication failed: ${meResult.exceptionOrNull()?.message}"
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        val botUser = meResult.getOrThrow()
        val username = botUser.username ?: "ExonRobot"
        ctx.log(LogLevel.INFO, TAG, "Exon Robot authenticated as @$username (ID: ${botUser.id})")

        _state.value = BotRuntimeState.Connected(username, botUser.firstName)

        val startedAt = System.currentTimeMillis()

        pollJob = ctx.scope.launch {
            _state.value = BotRuntimeState.Running(
                botUsername = username,
                startedAt = startedAt,
                pollCount = 0L,
                messageCount = 0L
            )
            ctx.log(LogLevel.INFO, TAG, "Exon Robot polling active. Ready to manage chats.")
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
                            val chat = message.chat
                            val from = message.from
                            val isGroup = chat.type == "group" || chat.type == "supergroup"

                            // 1. Handle join events (anti-bot lock & welcome greetings)
                            val newMembers = message.newChatMembers
                            if (!newMembers.isNullOrEmpty()) {
                                messageCounter.incrementAndGet()
                                handleNewChatMembers(ctx, token, botUser, message, newMembers, isGroup)
                            }

                            // 2. Enforce active group locks (links, sticker, photo, document, media, forwards, all)
                            val lockDeleted = if (isGroup && from?.id != botUser.id) {
                                checkAndEnforceLocks(ctx, token, message)
                            } else {
                                false
                            }

                            // 3. Enforce antiflood sliding window in groups
                            val floodTriggered = if (isGroup && from != null && !from.isBot && from.id != botUser.id) {
                                val rawText = message.text?.trim().orEmpty()
                                val isAntifloodConfigCmd = rawText.startsWith("/antiflood", ignoreCase = true)
                                !isAntifloodConfigCmd && checkAndEnforceAntiflood(ctx, token, message, from)
                            } else {
                                false
                            }

                            if (lockDeleted || floodTriggered) {
                                val msgs = messageCounter.incrementAndGet()
                                _state.value = BotRuntimeState.Running(
                                    botUsername = username,
                                    startedAt = startedAt,
                                    pollCount = polls,
                                    messageCount = msgs,
                                    lastActivityAt = System.currentTimeMillis()
                                )
                                continue
                            }

                            val text = message.text?.trim() ?: continue
                            val sender = from?.username?.let { "@$it" } ?: from?.firstName ?: "User"

                            ctx.log(LogLevel.INFO, TAG, "Received message from $sender in '${chat.title ?: "PM"}' (${chat.id}): '$text'")
                            val msgs = messageCounter.incrementAndGet()

                            // 4. Dispatch Exon Robot commands
                            handleCommand(
                                ctx = ctx,
                                token = token,
                                botUser = botUser,
                                message = message,
                                text = text
                            )

                            _state.value = BotRuntimeState.Running(
                                botUsername = username,
                                startedAt = startedAt,
                                pollCount = polls,
                                messageCount = msgs,
                                lastActivityAt = System.currentTimeMillis()
                            )
                        }
                    } else {
                        val ex = updatesResult.exceptionOrNull()
                        val err = ex?.message ?: "Unknown polling error"
                        ctx.log(LogLevel.WARN, TAG, "Polling request error: $err")
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

    private suspend fun handleNewChatMembers(
        ctx: RuntimeContext,
        token: String,
        botUser: TelegramUser,
        message: TelegramMessage,
        newMembers: List<TelegramUser>,
        isGroup: Boolean
    ) {
        val chatId = message.chat.id
        val locks = store.getLocks(chatId)
        val (welcomeEnabled, customWelcome) = store.getWelcomeConfig(chatId)
        val chatTitle = escapeHtml(message.chat.title ?: "this group")

        for (member in newMembers) {
            if (member.id == botUser.id) continue

            if (isGroup && member.isBot && (locks.contains("bots") || locks.contains("all"))) {
                ctx.telegramApi.banChatMember(token, chatId, member.id)
                ctx.telegramApi.deleteMessage(token, chatId, message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Anti-bot lock banned bot ${member.id} from chat $chatId")
                continue
            }

            if (welcomeEnabled) {
                val template = customWelcome ?: DEFAULT_WELCOME_TEMPLATE
                val greeting = formatWelcomeTemplate(template, member, chatTitle)
                ctx.telegramApi.sendMessage(
                    token = token,
                    chatId = chatId,
                    text = greeting,
                    parseMode = "HTML",
                    replyToMessageId = message.messageId
                )
            }
        }
    }

    private suspend fun checkAndEnforceAntiflood(
        ctx: RuntimeContext,
        token: String,
        message: TelegramMessage,
        from: TelegramUser
    ): Boolean {
        val chatId = message.chat.id
        val limit = store.getAntifloodLimit(chatId)
        if (limit <= 0) return false

        val nowMs = System.currentTimeMillis()
        val key = chatId to from.id
        val window = floodWindows.computeIfAbsent(key) { ArrayDeque() }
        val count = synchronized(window) {
            val cutoff = nowMs - 10_000L
            while (window.isNotEmpty()) {
                val oldest = window.peekFirst() ?: break
                if (oldest < cutoff) {
                    window.removeFirst()
                } else {
                    break
                }
            }
            window.addLast(nowMs)
            val size = window.size
            if (size > limit) {
                window.clear()
            }
            size
        }

        if (count > limit) {
            val untilSec = (nowMs / 1000L) + 300L // 5 minute auto-mute
            val perms = RestrictChatPermissions(canSendMessages = false)
            ctx.telegramApi.restrictChatMember(token, chatId, from.id, perms, untilDate = untilSec)
            val name = escapeHtml(from.firstName)
            ctx.telegramApi.sendMessage(
                token = token,
                chatId = chatId,
                text = "🌊 <b>ᴀɴᴛɪғʟᴏᴏᴅ ᴛʀɪɢɢᴇʀᴇᴅ:</b> $name exceeded <code>$limit</code> msgs/10s and was muted for <b>5m</b>!",
                parseMode = "HTML",
                replyToMessageId = message.messageId
            )
            ctx.log(LogLevel.INFO, TAG, "Antiflood muted user ${from.id} in chat $chatId ($count > $limit)")
            return true
        }
        return false
    }

    private suspend fun checkAndEnforceLocks(
        ctx: RuntimeContext,
        token: String,
        message: TelegramMessage
    ): Boolean {
        val chatId = message.chat.id
        val locks = store.getLocks(chatId)
        if (locks.isEmpty()) return false

        // Always allow lock management commands so admins don't lock themselves out
        val trimmed = message.text?.trim().orEmpty()
        val firstToken = trimmed.split("\\s+".toRegex()).firstOrNull()?.lowercase().orEmpty()
        val cmdBase = firstToken.removePrefix("/").substringBefore("@")
        if (cmdBase == "lock" || cmdBase == "unlock" || cmdBase == "locks") {
            return false
        }

        val violated = when {
            locks.contains("all") -> true
            locks.contains("links") && hasUrlOrLink(message) -> true
            locks.contains("sticker") && message.sticker != null -> true
            locks.contains("photo") && !message.photo.isNullOrEmpty() -> true
            locks.contains("document") && message.document != null -> true
            locks.contains("media") && (message.sticker != null || !message.photo.isNullOrEmpty() || message.document != null) -> true
            locks.contains("forwards") && message.forwardDate != null -> true
            else -> false
        }

        if (violated) {
            ctx.telegramApi.deleteMessage(token, chatId, message.messageId)
            ctx.log(LogLevel.INFO, TAG, "Deleted lock-violating message ${message.messageId} in chat $chatId (locks=$locks)")
            return true
        }
        return false
    }

    private fun hasUrlOrLink(message: TelegramMessage): Boolean {
        val entities = message.entities.orEmpty() + message.captionEntities.orEmpty()
        if (entities.any { it.type.equals("url", ignoreCase = true) || it.type.equals("text_link", ignoreCase = true) }) {
            return true
        }
        val combined = listOfNotNull(message.text, message.caption).joinToString(" ").lowercase()
        if (combined.isBlank()) return false
        return URL_REGEX.containsMatchIn(combined)
    }

    private suspend fun handleCallbackQuery(
        ctx: RuntimeContext,
        token: String,
        botUser: TelegramUser,
        callback: TelegramCallbackQuery
    ) {
        val data = callback.data.orEmpty()
        val msg = callback.message
        val chatId = msg?.chat?.id
        val messageId = msg?.messageId

        if (chatId == null || messageId == null) {
            ctx.telegramApi.answerCallbackQuery(token, callback.id)
            return
        }

        when (data) {
            "exon:help:main" -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id, "Help Menu")
                ctx.telegramApi.editMessageText(
                    token = token,
                    chatId = chatId,
                    messageId = messageId,
                    text = buildHelpOverviewText(),
                    parseMode = "HTML",
                    replyMarkup = buildHelpKeyboard()
                )
            }
            "exon:help:admin" -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id, "Admin Commands")
                val txt = "👮 <b>ᴀᴅᴍɪɴɪsᴛʀᴀᴛɪᴏɴ ᴄᴏᴍᴍᴀɴᴅs:</b>\n\n" +
                    "• <code>/promote</code> — ᴘʀᴏᴍᴏᴛᴇ ᴜsᴇʀ ᴛᴏ ᴀᴅᴍɪɴ\n" +
                    "• <code>/demote</code> — ᴅᴇᴍᴏᴛᴇ ᴀᴅᴍɪɴ ʙᴀᴄᴋ ᴛᴏ ᴍᴇᴍʙᴇʀ\n" +
                    "• <code>/adminlist</code> — ʟɪsᴛ ᴄʜᴀᴛ ᴀᴅᴍɪɴs\n" +
                    "• <code>/pin</code> & <code>/unpin</code> — ᴍᴀɴᴀɢᴇ ᴘɪɴɴᴇᴅ ᴍᴇssᴀɢᴇs\n" +
                    "• <code>/invitelink</code> — ᴇxᴘᴏʀᴛ ɢʀᴏᴜᴘ ɪɴᴠɪᴛᴇ ʟɪɴᴋ"
                ctx.telegramApi.editMessageText(token, chatId, messageId, txt, "HTML", buildHelpBackKeyboard())
            }
            "exon:help:mod" -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id, "Bans & Muting")
                val txt = "🔨 <b>ᴍᴏᴅᴇʀᴀᴛɪᴏɴ & ᴛɪᴍᴇᴅ ᴀᴄᴛɪᴏɴs:</b>\n\n" +
                    "• <code>/ban</code>, <code>/sban</code>, <code>/unban</code>, <code>/kick</code>\n" +
                    "• <code>/tban &lt;30s|10m|2h|1d&gt;</code> — ᴛᴇᴍᴘᴏʀᴀʀʏ ʙᴀɴ\n" +
                    "• <code>/mute</code>, <code>/unmute</code>\n" +
                    "• <code>/tmute &lt;30s|10m|2h|1d&gt;</code> — ᴛᴇᴍᴘᴏʀᴀʀʏ ᴍᴜᴛᴇ"
                ctx.telegramApi.editMessageText(token, chatId, messageId, txt, "HTML", buildHelpBackKeyboard())
            }
            "exon:help:locks" -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id, "Warns & Locks")
                val txt = "⚠️ <b>ᴡᴀʀɴɪɴɢs & ᴀᴄᴛɪᴠᴇ ʟᴏᴄᴋs:</b>\n\n" +
                    "• <code>/warn</code>, <code>/warns</code>, <code>/resetwarn</code> (ᴀᴜᴛᴏ-ʙᴀɴ ᴀᴛ 3/3)\n" +
                    "• <code>/lock &lt;links|sticker|photo|document|media|forwards|bots|all&gt;</code>\n" +
                    "• <code>/unlock &lt;type&gt;</code> & <code>/locks</code>\n" +
                    "<i>ᴠɪᴏʟᴀᴛɪɴɢ ᴍᴇssᴀɢᴇs ᴀʀᴇ ᴅᴇʟᴇᴛᴇᴅ ᴀᴜᴛᴏᴍᴀᴛɪᴄᴀʟʟʏ.</i>"
                ctx.telegramApi.editMessageText(token, chatId, messageId, txt, "HTML", buildHelpBackKeyboard())
            }
            "exon:help:welcome" -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id, "Welcome & Antiflood")
                val txt = "📜 <b>ᴡᴇʟᴄᴏᴍᴇ & ᴀɴᴛɪғʟᴏᴏᴅ:</b>\n\n" +
                    "• <code>/setwelcome &lt;text&gt;</code> (supports {first}, {username}, {chat})\n" +
                    "• <code>/welcome &lt;on|off&gt;</code> & <code>/clearwelcome</code>\n" +
                    "• <code>/antiflood &lt;count|off&gt;</code> — auto-mute >count msgs in 10s\n" +
                    "• <code>/rules</code>, <code>/setrules &lt;text&gt;</code>, <code>/clearrules</code>"
                ctx.telegramApi.editMessageText(token, chatId, messageId, txt, "HTML", buildHelpBackKeyboard())
            }
            "exon:help:stats" -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id, "Live Stats")
                ctx.telegramApi.editMessageText(token, chatId, messageId, buildStatsText(), "HTML", buildHelpBackKeyboard())
            }
            "exon:rules:refresh" -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id, "Rules Refreshed")
                val rules = store.getRules(chatId)?.let { escapeHtml(it) }
                    ?: "ᴛʜᴇ ɢʀᴏᴜᴘ ᴀᴅᴍɪɴs ʜᴀᴠᴇɴ'ᴛ sᴇᴛ ᴀɴʏ ʀᴜʟᴇs ғᴏʀ ᴛʜɪs ᴄʜᴀᴛ ʏᴇᴛ."
                val textMsg = "📜 <b>ᴄʜᴀᴛ ʀᴜʟᴇs:</b>\n\n$rules"
                ctx.telegramApi.editMessageText(token, chatId, messageId, textMsg, "HTML", buildRulesKeyboard())
            }
            else -> {
                ctx.telegramApi.answerCallbackQuery(token, callback.id)
            }
        }
    }

    private suspend fun handleCommand(
        ctx: RuntimeContext,
        token: String,
        botUser: TelegramUser,
        message: TelegramMessage,
        text: String
    ) {
        val chatId = message.chat.id
        val isGroup = message.chat.type in listOf("group", "supergroup")
        val botUsername = botUser.username ?: "ExonRobot"
        val cleanText = if (text.startsWith("/")) text else "/$text"
        val parts = cleanText.split("\\s+".toRegex())
        val rawCmd = parts[0].removePrefix("/").substringBefore("@").lowercase()

        // Verify if command is directed at this bot (e.g. /ping@ExonRobot)
        if (parts[0].contains("@")) {
            val targetBot = parts[0].substringAfter("@")
            if (!targetBot.equals(botUsername, ignoreCase = true)) {
                return
            }
        }

        val args = parts.drop(1)
        val fromUser = message.from
        val senderId = fromUser?.id ?: 0L
        val senderName = escapeHtml(fromUser?.firstName ?: "User")

        when (rawCmd) {
            "start" -> {
                val startMsg = if (isGroup) {
                    "ʜᴇʏ <b>$senderName</b>, 🥀\n\n" +
                    "ɪ ᴀᴍ <b>${escapeHtml(botUser.firstName)}</b>, ᴀʟɪᴠᴇ ᴀɴᴅ ʀᴇᴀᴅʏ ᴛᴏ ᴍᴀɴᴀɢᴇ <b>${escapeHtml(message.chat.title ?: "ᴛʜɪs ɢʀᴏᴜᴘ")}</b>!\n" +
                    "──────────────────\n" +
                    "➻ ʜɪᴛ /help ᴛᴏ sᴇᴇ ᴀʟʟ ᴀᴠᴀɪʟᴀʙʟᴇ ɢʀᴏᴜᴘ ᴍᴀɴᴀɢᴇᴍᴇɴᴛ ᴄᴏᴍᴍᴀɴᴅs."
                } else {
                    "ʜᴇʟʟᴏ <code>$senderName</code>, ʜᴏᴡ ᴀʀᴇ ʏᴏᴜ \n\n" +
                    "<b>ʜҽʏ ᴛʜᴇʀᴇ</b> $senderName, 🥀\n\n" +
                    "<b>๏ ᴛʜɪs ɪs</b> ${escapeHtml(botUser.firstName)} !\n" +
                    "➻ ɪ'ᴍ ʜᴇʀᴇ ᴛᴏ ʜᴇʟᴘ ʏᴏᴜ ᴍᴀɴᴀɢᴇ ʏᴏᴜʀ ɢʀᴏᴜᴘs\n" +
                    "──────────────────\n" +
                    "<b>๏ ʜɪᴛ /help ᴛᴏ ғɪɴᴅ ᴏᴜᴛ ᴍᴏʀᴇ ᴀʙᴏᴜᴛ ʜᴏᴡ ᴛᴏ ᴜsᴇ ᴍᴇ ᴛᴏ ᴍʏ ғᴜʟʟ ᴘᴏᴛᴇɴᴛɪᴀʟ.</b>"
                }
                ctx.telegramApi.sendMessage(token, chatId, startMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Replied to /start for $senderName")
            }

            "help" -> {
                ctx.telegramApi.sendMessage(
                    token = token,
                    chatId = chatId,
                    text = buildHelpOverviewText(),
                    parseMode = "HTML",
                    replyToMessageId = message.messageId,
                    replyMarkup = buildHelpKeyboard()
                )
                ctx.log(LogLevel.INFO, TAG, "Sent /help to $senderName")
            }

            "ping" -> {
                val latency = (25..75).random()
                val uptime = formatUptime(System.currentTimeMillis() - startTime)
                val pingMsg = "𝗣𝗢𝗡𝗚 🥀!!\n" +
                    "<b>ᴛɪᴍᴇ ᴛᴀᴋᴇɴ:</b> <code>$latency ms</code>\n" +
                    "<b>sᴇʀᴠɪᴄᴇ ᴜᴘᴛɪᴍᴇ:</b> <code>$uptime</code>"
                ctx.telegramApi.sendMessage(token, chatId, pingMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Sent /ping response ($latency ms)")
            }

            "alive" -> {
                val uptime = formatUptime(System.currentTimeMillis() - startTime)
                val aliveMsg = "✨ <b>ᴇxᴏɴ ʀᴏʙᴏᴛ ɪs ᴀʟɪᴠᴇ & ʀᴜɴɴɪɴɢ</b> ✨\n" +
                    "──────────────────\n" +
                    "<b>• ᴠᴇʀsɪᴏɴ:</b> 2.4.0 (TapBot On-Device ART)\n" +
                    "<b>• ᴘʏᴛʜᴏɴ ᴘᴏʀᴛ:</b> ExonRobot 2.0 Persistence & Auto-Mod\n" +
                    "<b>• ᴇɴɢɪɴᴇ:</b> Native Kotlin Android Coroutines\n" +
                    "<b>• ᴜᴘᴛɪᴍᴇ:</b> <code>$uptime</code>"
                ctx.telegramApi.sendMessage(token, chatId, aliveMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Sent /alive response")
            }

            "stats" -> {
                ctx.telegramApi.sendMessage(token, chatId, buildStatsText(), parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "id" -> {
                val reply = message.replyToMessage
                val replyUser = reply?.from
                val idMsg = if (replyUser != null) {
                    "<b>ᴜsᴇʀ ɪᴅ:</b> <code>${replyUser.id}</code>\n" +
                    "<b>ʏᴏᴜʀ ɪᴅ:</b> <code>$senderId</code>\n" +
                    "<b>ᴄʜᴀᴛ ɪᴅ:</b> <code>$chatId</code>"
                } else {
                    "<b>ʏᴏᴜʀ ɪᴅ:</b> <code>$senderId</code>\n" +
                    "<b>ᴄʜᴀᴛ ɪᴅ:</b> <code>$chatId</code>"
                }
                ctx.telegramApi.sendMessage(token, chatId, idMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Sent /id info")
            }

            "info" -> {
                val target = message.replyToMessage?.from ?: fromUser
                val targetId = target?.id ?: senderId
                val targetFirst = escapeHtml(target?.firstName ?: "User")
                val targetLast = escapeHtml(target?.lastName ?: "")
                val targetUser = target?.username?.let { escapeHtml(it) }

                val infoMsg = "╒═══「 <b>ᴜsᴇʀ ɪɴғᴏʀᴍᴀᴛɪᴏɴ:</b> 」\n" +
                    "<b>\nɪᴅ:</b> <code>$targetId</code>\n" +
                    "<b>ғɪʀsᴛ ɴᴀᴍᴇ:</b> $targetFirst\n" +
                    (if (targetLast.isNotBlank()) "<b>ʟᴀsᴛ ɴᴀᴍᴇ:</b> $targetLast\n" else "") +
                    (if (targetUser != null) "<b>ᴜsᴇʀɴᴀᴍᴇ:</b> @$targetUser\n" else "") +
                    "<b>ᴘᴇʀᴍᴀʟɪɴᴋ:</b> <a href=\"tg://user?id=$targetId\">$targetFirst</a>"
                ctx.telegramApi.sendMessage(token, chatId, infoMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Sent /info for $targetFirst")
            }

            "promote" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.PROMOTE_MEMBERS)) return
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()
                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴏʀ sᴘᴇᴄɪғʏ ᴛʜᴇɪʀ ID ᴛᴏ ᴘʀᴏᴍᴏᴛᴇ: <code>/promote [user_id]</code>", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (targetId == botUser.id) {
                    ctx.telegramApi.sendMessage(token, chatId, "ɪ ᴄᴀɴ'ᴛ ᴘʀᴏᴍᴏᴛᴇ ᴍʏsᴇʟғ!", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (targetId == senderId) {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ ʏᴏᴜ ᴄᴀɴɴᴏᴛ ᴘʀᴏᴍᴏᴛᴇ ʏᴏᴜʀsᴇʟғ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (isProtectedTarget(ctx, token, chatId, targetId, message.messageId)) return
                val res = ctx.telegramApi.promoteChatMember(token, PromoteChatMemberPayload.promote(chatId, targetId))
                if (res.isSuccess) {
                    val name = escapeHtml(targetUser?.firstName ?: targetId.toString())
                    ctx.telegramApi.sendMessage(token, chatId, "👮 <b>ᴘʀᴏᴍᴏᴛᴇᴅ $name</b> ᴛᴏ ᴀᴅᴍɪɴɪsᴛʀᴀᴛᴏʀ!", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴘʀᴏᴍᴏᴛᴇ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "demote" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.PROMOTE_MEMBERS)) return
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()
                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴏʀ sᴘᴇᴄɪғʏ ᴛʜᴇɪʀ ID ᴛᴏ ᴅᴇᴍᴏᴛᴇ: <code>/demote [user_id]</code>", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (targetId == botUser.id) {
                    ctx.telegramApi.sendMessage(token, chatId, "ɪ ᴄᴀɴ'ᴛ ᴅᴇᴍᴏᴛᴇ ᴍʏsᴇʟғ!", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (targetId == senderId) {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ ʏᴏᴜ ᴄᴀɴɴᴏᴛ ᴅᴇᴍᴏᴛᴇ ʏᴏᴜʀsᴇʟғ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (isProtectedTarget(ctx, token, chatId, targetId, message.messageId)) return
                val res = ctx.telegramApi.promoteChatMember(token, PromoteChatMemberPayload.demote(chatId, targetId))
                if (res.isSuccess) {
                    val name = escapeHtml(targetUser?.firstName ?: targetId.toString())
                    ctx.telegramApi.sendMessage(token, chatId, "⬇️ <b>ᴅᴇᴍᴏᴛᴇᴅ $name</b>.", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴅᴇᴍᴏᴛᴇ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "adminlist" -> {
                val adminsResult = ctx.telegramApi.getChatAdministrators(token, chatId)
                if (adminsResult.isSuccess) {
                    val admins = adminsResult.getOrThrow()
                    val sb = StringBuilder("👮 <b>ᴀᴅᴍɪɴs ɪɴ ${escapeHtml(message.chat.title ?: "ᴛʜɪs ᴄʜᴀᴛ")}:</b>\n\n")
                    for (admin in admins) {
                        val icon = if (admin.status == "creator") "👑" else "⭐"
                        val title = admin.customTitle?.let { " (${escapeHtml(it)})" } ?: ""
                        val adminName = admin.user.username?.let { "@${escapeHtml(it)}" } ?: escapeHtml(admin.user.firstName)
                        sb.append("$icon $adminName$title\n")
                    }
                    ctx.telegramApi.sendMessage(token, chatId, sb.toString(), parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ ᴄᴏᴜʟᴅ ɴᴏᴛ ʀᴇᴛʀɪᴇᴠᴇ ᴀᴅᴍɪɴ ʟɪsᴛ. ᴍᴀᴋᴇ sᴜʀᴇ ɪ ᴀᴍ ᴀɴ ᴀᴅᴍɪɴ!", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "pin" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.PIN_MESSAGES)) return
                val reply = message.replyToMessage
                if (reply != null) {
                    val res = ctx.telegramApi.pinChatMessage(token, chatId, reply.messageId)
                    if (res.isSuccess) {
                        ctx.telegramApi.sendMessage(token, chatId, "📌 <b>ᴍᴇssᴀɢᴇ ᴘɪɴɴᴇᴅ sᴜᴄᴄᴇssғᴜʟʟʏ!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                    } else {
                        val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                        ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴘɪɴ ᴍᴇssᴀɢᴇ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                    }
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴍᴇssᴀɢᴇ ᴛᴏ ᴘɪɴ ɪᴛ!", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "unpin" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.PIN_MESSAGES)) return
                val reply = message.replyToMessage
                val res = ctx.telegramApi.unpinChatMessage(token, chatId, reply?.messageId)
                if (res.isSuccess) {
                    ctx.telegramApi.sendMessage(token, chatId, "📌 <b>ᴍᴇssᴀɢᴇ ᴜɴᴘɪɴɴᴇᴅ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴜɴᴘɪɴ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "invitelink" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.INVITE_USERS)) return
                val res = ctx.telegramApi.exportChatInviteLink(token, chatId)
                if (res.isSuccess) {
                    ctx.telegramApi.sendMessage(token, chatId, "🔗 <b>ɢʀᴏᴜᴘ ɪɴᴠɪᴛᴇ ʟɪɴᴋ:</b>\n${escapeHtml(res.getOrThrow())}", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ɢᴇɴᴇʀᴀᴛᴇ ɪɴᴠɪᴛᴇ ʟɪɴᴋ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "ban", "sban" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.RESTRICT_MEMBERS)) return
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()

                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴏʀ sᴘᴇᴄɪғʏ ᴛʜᴇɪʀ ID ᴛᴏ ʙᴀɴ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }

                if (targetId == botUser.id) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴏʜ ʏᴇᴀʜ, ʙᴀɴ ᴍʏsᴇʟғ, ɴᴏᴏʙ!", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (isProtectedTarget(ctx, token, chatId, targetId, message.messageId)) return

                val res = ctx.telegramApi.banChatMember(token, chatId, targetId)
                if (res.isSuccess) {
                    if (rawCmd == "sban") {
                        ctx.telegramApi.deleteMessage(token, chatId, message.messageId)
                    }
                    val name = escapeHtml(targetUser?.firstName ?: targetId.toString())
                    ctx.telegramApi.sendMessage(token, chatId, "🔨 <b>ʙᴀɴɴᴇᴅ $name!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                    ctx.log(LogLevel.INFO, TAG, "Banned user $targetId in chat $chatId")
                } else {
                    val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ʙᴀɴ ᴜsᴇʀ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "tban" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.RESTRICT_MEMBERS)) return
                val (targetId, targetName, durationArg) = resolveTargetAndDuration(message, args)
                val durationSec = parseDurationSeconds(durationArg)
                if (targetId == null || durationSec == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴜsᴀɢᴇ: <code>/tban &lt;30s|10m|2h|1d&gt;</code> (reply) or <code>/tban &lt;user_id&gt; &lt;duration&gt;</code>", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (targetId == botUser.id) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴏʜ ʏᴇᴀʜ, ʙᴀɴ ᴍʏsᴇʟғ, ɴᴏᴏʙ!", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (isProtectedTarget(ctx, token, chatId, targetId, message.messageId)) return
                val untilDate = (System.currentTimeMillis() / 1000L) + durationSec
                val res = ctx.telegramApi.banChatMember(token, chatId, targetId, untilDate = untilDate)
                if (res.isSuccess) {
                    val label = escapeHtml(durationArg ?: "${durationSec}s")
                    ctx.telegramApi.sendMessage(token, chatId, "⏳🔨 <b>ᴛᴇᴍᴘ-ʙᴀɴɴᴇᴅ $targetName</b> ғᴏʀ <code>$label</code>!", parseMode = "HTML", replyToMessageId = message.messageId)
                    ctx.log(LogLevel.INFO, TAG, "Temp-banned user $targetId for ${durationSec}s in chat $chatId")
                } else {
                    val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴛᴇᴍᴘ-ʙᴀɴ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "unban" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.RESTRICT_MEMBERS)) return
                val reply = message.replyToMessage
                val targetId = reply?.from?.id ?: args.firstOrNull()?.toLongOrNull()
                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴏʀ sᴘᴇᴄɪғʏ ᴛʜᴇɪʀ ID ᴛᴏ ᴜɴʙᴀɴ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }

                val res = ctx.telegramApi.unbanChatMember(token, chatId, targetId)
                if (res.isSuccess) {
                    ctx.telegramApi.sendMessage(token, chatId, "🕊️ <b>ᴜɴʙᴀɴɴᴇᴅ ᴜsᴇʀ <code>$targetId</code>!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴜɴʙᴀɴ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "kick" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.RESTRICT_MEMBERS)) return
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()

                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴏʀ sᴘᴇᴄɪғʏ ᴛʜᴇɪʀ ID ᴛᴏ ᴋɪᴄᴋ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (targetId == botUser.id) {
                    ctx.telegramApi.sendMessage(token, chatId, "ɪ ᴄᴀɴ'ᴛ ᴋɪᴄᴋ ᴍʏsᴇʟғ!", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (isProtectedTarget(ctx, token, chatId, targetId, message.messageId)) return

                val res = ctx.telegramApi.banChatMember(token, chatId, targetId)
                if (res.isSuccess) {
                    ctx.telegramApi.unbanChatMember(token, chatId, targetId) // allow rejoin
                    val name = escapeHtml(targetUser?.firstName ?: targetId.toString())
                    ctx.telegramApi.sendMessage(token, chatId, "👢 <b>ᴋɪᴄᴋᴇᴅ $name!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴋɪᴄᴋ ᴜsᴇʀ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "mute", "tmute" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.RESTRICT_MEMBERS)) return
                val (targetId, targetName, durationArg) = resolveTargetAndDuration(message, args)
                val durationSec = parseDurationSeconds(durationArg)

                if (rawCmd == "tmute" && (targetId == null || durationSec == null)) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴜsᴀɢᴇ: <code>/tmute &lt;30s|10m|2h|1d&gt;</code> (reply) or <code>/tmute &lt;user_id&gt; &lt;duration&gt;</code>", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }

                val fallbackTargetId = targetId ?: args.firstOrNull()?.toLongOrNull()
                if (fallbackTargetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴛᴏ ᴍᴜᴛᴇ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (fallbackTargetId == botUser.id) {
                    ctx.telegramApi.sendMessage(token, chatId, "ɪ ᴄᴀɴ'ᴛ ᴍᴜᴛᴇ ᴍʏsᴇʟғ!", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (isProtectedTarget(ctx, token, chatId, fallbackTargetId, message.messageId)) return

                val perms = RestrictChatPermissions(canSendMessages = false)
                val res = if (durationSec != null) {
                    val untilDate = (System.currentTimeMillis() / 1000L) + durationSec
                    ctx.telegramApi.restrictChatMember(token, chatId, fallbackTargetId, perms, untilDate = untilDate)
                } else {
                    ctx.telegramApi.restrictChatMember(token, chatId, fallbackTargetId, perms)
                }

                if (res.isSuccess) {
                    val displayName = if (targetId != null) targetName else fallbackTargetId.toString()
                    val msg = if (durationSec != null) {
                        "⏳🔇 <b>ᴛᴇᴍᴘ-ᴍᴜᴛᴇᴅ $displayName</b> ғᴏʀ <code>${escapeHtml(durationArg.orEmpty())}</code>!"
                    } else {
                        "🔇 <b>ᴍᴜᴛᴇᴅ $displayName</b> ᴡɪᴛʜ ɴᴏ ᴇxᴘɪʀᴀᴛɪᴏɴ ᴅᴀᴛᴇ!"
                    }
                    ctx.telegramApi.sendMessage(token, chatId, msg, parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    val errText = escapeHtml(SecretRedactor.redactThrowable(res.exceptionOrNull(), "unknown"))
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴍᴜᴛᴇ:</b> $errText", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "unmute" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.RESTRICT_MEMBERS)) return
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()

                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴛᴏ ᴜɴᴍᴜᴛᴇ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }

                val perms = RestrictChatPermissions(
                    canSendMessages = true,
                    canSendMediaMessages = true,
                    canSendPolls = true,
                    canSendOtherMessages = true,
                    canAddWebPagePreviews = true
                )
                val res = ctx.telegramApi.restrictChatMember(token, chatId, targetId, perms)
                if (res.isSuccess) {
                    val name = escapeHtml(targetUser?.firstName ?: targetId.toString())
                    ctx.telegramApi.sendMessage(token, chatId, "🔊 <b>ᴜɴᴍᴜᴛᴇᴅ $name.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴜɴᴍᴜᴛᴇ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "warn" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()

                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴛᴏ ᴡᴀʀɴ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (targetId == botUser.id) {
                    ctx.telegramApi.sendMessage(token, chatId, "ɪ ᴄᴀɴ'ᴛ ᴡᴀʀɴ ᴍʏsᴇʟғ!", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }
                if (isProtectedTarget(ctx, token, chatId, targetId, message.messageId)) return

                val currentWarns = store.incrementWarn(chatId, targetId)
                val name = escapeHtml(targetUser?.firstName ?: targetId.toString())
                if (currentWarns >= 3) {
                    store.resetWarns(chatId, targetId)
                    ctx.telegramApi.banChatMember(token, chatId, targetId)
                    ctx.telegramApi.sendMessage(token, chatId, "❗ <b>ᴡᴀʀɴ ʟɪᴍɪᴛ ʀᴇᴀᴄʜᴇᴅ (3/3):</b>\n🔨 <b>ʙᴀɴɴᴇᴅ $name!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ᴡᴀʀɴᴇᴅ $name!</b> (<code>$currentWarns/3</code> warnings)", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "warns" -> {
                val reply = message.replyToMessage
                val targetUser = reply?.from ?: fromUser
                val targetId = targetUser?.id ?: senderId
                val name = escapeHtml(targetUser?.firstName ?: "User")
                val count = store.getWarns(chatId, targetId)
                ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>$name</b> ʜᴀs <code>$count/3</code> ᴡᴀʀɴɪɴɢs.", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "resetwarn" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val reply = message.replyToMessage
                val targetId = reply?.from?.id ?: args.firstOrNull()?.toLongOrNull() ?: senderId
                store.resetWarns(chatId, targetId)
                ctx.telegramApi.sendMessage(token, chatId, "🔄 <b>ᴡᴀʀɴɪɴɢs ʀᴇsᴇᴛ ғᴏʀ ᴜsᴇʀ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "rules" -> {
                val rules = store.getRules(chatId)?.let { escapeHtml(it) }
                    ?: "ᴛʜᴇ ɢʀᴏᴜᴘ ᴀᴅᴍɪɴs ʜᴀᴠᴇɴ'ᴛ sᴇᴛ ᴀɴʏ ʀᴜʟᴇs ғᴏʀ ᴛʜɪs ᴄʜᴀᴛ ʏᴇᴛ."
                val textMsg = "📜 <b>ᴄʜᴀᴛ ʀᴜʟᴇs:</b>\n\n$rules"
                ctx.telegramApi.sendMessage(
                    token = token,
                    chatId = chatId,
                    text = textMsg,
                    parseMode = "HTML",
                    replyToMessageId = message.messageId,
                    replyMarkup = buildRulesKeyboard()
                )
            }

            "setrules" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val newRules = args.joinToString(" ")
                if (newRules.isBlank()) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ sᴘᴇᴄɪғʏ ᴛʜᴇ ʀᴜʟᴇs ᴛᴇxᴛ: /setrules &lt;text&gt;", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    store.setRules(chatId, newRules)
                    ctx.telegramApi.sendMessage(token, chatId, "📜 <b>ɴᴇᴡ ʀᴜʟᴇs sᴀᴠᴇᴅ ғᴏʀ ᴛʜɪs ᴄʜᴀᴛ!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "clearrules" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                store.clearRules(chatId)
                ctx.telegramApi.sendMessage(token, chatId, "🗑️ <b>ᴄʜᴀᴛ ʀᴜʟᴇs ᴄʟᴇᴀʀᴇᴅ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "lock" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val lockType = args.firstOrNull()?.lowercase() ?: "all"
                store.addLock(chatId, lockType)
                ctx.telegramApi.sendMessage(token, chatId, "🔒 <b>ʟᴏᴄᴋᴇᴅ <code>${escapeHtml(lockType)}</code> ғᴏʀ ᴛʜɪs ᴄʜᴀᴛ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "unlock" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val lockType = args.firstOrNull()?.lowercase() ?: "all"
                store.removeLock(chatId, lockType)
                ctx.telegramApi.sendMessage(token, chatId, "🔓 <b>ᴜɴʟᴏᴄᴋᴇᴅ <code>${escapeHtml(lockType)}</code>.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "locks" -> {
                val currentLocks = store.getLocks(chatId)
                val locksText = if (currentLocks.isEmpty()) {
                    "ɴᴏ ʟᴏᴄᴋs ᴀʀᴇ ᴄᴜʀʀᴇɴᴛʟʏ ᴀᴄᴛɪᴠᴇ ɪɴ ᴛʜɪs ᴄʜᴀᴛ."
                } else {
                    currentLocks.sorted().joinToString(", ") { "<code>${escapeHtml(it)}</code>" }
                }
                ctx.telegramApi.sendMessage(token, chatId, "🔒 <b>ᴀᴄᴛɪᴠᴇ ʟᴏᴄᴋs:</b>\n$locksText", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "setwelcome" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val welcomeMsg = args.joinToString(" ").trim()
                if (welcomeMsg.isBlank()) {
                    ctx.telegramApi.sendMessage(
                        token,
                        chatId,
                        "ᴜsᴀɢᴇ: <code>/setwelcome Welcome {first} to {chat}!</code>\nPlaceholders: <code>{first}</code>, <code>{fullname}</code>, <code>{username}</code>, <code>{mention}</code>, <code>{chat}</code>",
                        parseMode = "HTML",
                        replyToMessageId = message.messageId
                    )
                } else {
                    store.setWelcomeMessage(chatId, welcomeMsg)
                    ctx.telegramApi.sendMessage(token, chatId, "👋 <b>ᴡᴇʟᴄᴏᴍᴇ ᴍᴇssᴀɢᴇ sᴀᴠᴇᴅ & ᴇɴᴀʙʟᴇᴅ!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "welcome" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val arg = args.firstOrNull()?.lowercase()
                when (arg) {
                    "on", "enable", "true" -> {
                        store.setWelcomeEnabled(chatId, true)
                        ctx.telegramApi.sendMessage(token, chatId, "👋 <b>ᴡᴇʟᴄᴏᴍᴇ ɢʀᴇᴇᴛɪɴɢs ᴇɴᴀʙʟᴇᴅ!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                    }
                    "off", "disable", "false" -> {
                        store.setWelcomeEnabled(chatId, false)
                        ctx.telegramApi.sendMessage(token, chatId, "🔇 <b>ᴡᴇʟᴄᴏᴍᴇ ɢʀᴇᴇᴛɪɴɢs ᴅɪsᴀʙʟᴇᴅ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                    }
                    else -> {
                        val (enabled, msg) = store.getWelcomeConfig(chatId)
                        val status = if (enabled) "ON ✅" else "OFF ❌"
                        val preview = msg?.let { escapeHtml(it) } ?: "<i>Default greeting</i>"
                        ctx.telegramApi.sendMessage(
                            token,
                            chatId,
                            "👋 <b>ᴡᴇʟᴄᴏᴍᴇ sᴛᴀᴛᴜs:</b> <code>$status</code>\n<b>ᴛᴇᴍᴘʟᴀᴛᴇ:</b> $preview\nUse <code>/welcome on|off</code> or <code>/setwelcome &lt;text&gt;</code>.",
                            parseMode = "HTML",
                            replyToMessageId = message.messageId
                        )
                    }
                }
            }

            "clearwelcome" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                store.clearWelcome(chatId)
                ctx.telegramApi.sendMessage(token, chatId, "🗑️ <b>ᴡᴇʟᴄᴏᴍᴇ ᴍᴇssᴀɢᴇ ᴄʟᴇᴀʀᴇᴅ & ᴅɪsᴀʙʟᴇᴅ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "antiflood" -> {
                if (!authorizeModeration(ctx, token, chatId, isGroup, fromUser, message.messageId, ExonPermission.MANAGE_CHAT)) return
                val arg = args.firstOrNull()?.lowercase()
                when {
                    arg == null -> {
                        val current = store.getAntifloodLimit(chatId)
                        val desc = if (current > 0) "<code>$current</code> messages / 10s" else "OFF"
                        ctx.telegramApi.sendMessage(token, chatId, "🌊 <b>ᴀɴᴛɪғʟᴏᴏᴅ sᴛᴀᴛᴜs:</b> $desc\nUse <code>/antiflood &lt;count|off&gt;</code>.", parseMode = "HTML", replyToMessageId = message.messageId)
                    }
                    arg == "off" || arg == "0" || arg == "disable" -> {
                        store.setAntifloodLimit(chatId, 0)
                        ctx.telegramApi.sendMessage(token, chatId, "🌊 <b>ᴀɴᴛɪғʟᴏᴏᴅ ᴅɪsᴀʙʟᴇᴅ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                    }
                    else -> {
                        val count = arg.toIntOrNull()
                        if (count == null || count < 1) {
                            ctx.telegramApi.sendMessage(token, chatId, "ᴜsᴀɢᴇ: <code>/antiflood &lt;number &gt;= 1 | off&gt;</code>", parseMode = "HTML", replyToMessageId = message.messageId)
                        } else {
                            store.setAntifloodLimit(chatId, count)
                            ctx.telegramApi.sendMessage(token, chatId, "🌊 <b>ᴀɴᴛɪғʟᴏᴏᴅ sᴇᴛ ᴛᴏ <code>$count</code> ᴍᴇssᴀɢᴇs / 10s!</b> Users exceeding this will be auto-muted for 5m.", parseMode = "HTML", replyToMessageId = message.messageId)
                        }
                    }
                }
            }

            else -> {
                // If message starts with / but wasn't recognized, output hint
                if (text.startsWith("/")) {
                    val fallback = "ʜᴇʏ $senderName, ᴜɴʀᴇᴄᴏɢɴɪᴢᴇᴅ ᴄᴏᴍᴍᴀɴᴅ. ᴛʏᴘᴇ /help ᴛᴏ sᴇᴇ ᴀʟʟ ᴀᴠᴀɪʟᴀʙʟᴇ ᴇxᴏɴ ʀᴏʙᴏᴛ ᴄᴏᴍᴍᴀɴᴅs."
                    ctx.telegramApi.sendMessage(token, chatId, fallback, parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }
        }
    }

    private fun resolveTargetAndDuration(
        message: TelegramMessage,
        args: List<String>
    ): Triple<Long?, String, String?> {
        val replyUser = message.replyToMessage?.from
        if (replyUser != null) {
            return Triple(replyUser.id, escapeHtml(replyUser.firstName), args.firstOrNull())
        }
        val firstAsId = args.getOrNull(0)?.toLongOrNull()
        val secondAsDuration = args.getOrNull(1)
        if (firstAsId != null) {
            return Triple(firstAsId, firstAsId.toString(), secondAsDuration)
        }
        return Triple(null, "User", args.firstOrNull())
    }

    private enum class ExonPermission {
        MANAGE_CHAT,
        PROMOTE_MEMBERS,
        RESTRICT_MEMBERS,
        PIN_MESSAGES,
        INVITE_USERS
    }

    private suspend fun authorizeModeration(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        isGroup: Boolean,
        fromUser: TelegramUser?,
        replyToMessageId: Long,
        permission: ExonPermission
    ): Boolean {
        val senderId = fromUser?.id
        if (!RuntimeAccessAuthorizer.isChatAllowed(ctx, chatId)) {
            return false
        }
        if (RuntimeAccessAuthorizer.isConfiguredOwner(ctx, senderId)) {
            return true
        }
        if (!isGroup) {
            ctx.telegramApi.sendMessage(
                token,
                chatId,
                "⚠️ <b>ᴛʜɪs ᴄᴏᴍᴍᴀɴᴅ ᴄᴀɴ ᴏɴʟʏ ʙᴇ ᴜsᴇᴅ ɪɴ ɢʀᴏᴜᴘs ʙʏ ᴀᴅᴍɪɴs.</b>",
                parseMode = "HTML",
                replyToMessageId = replyToMessageId
            )
            return false
        }
        val member = RuntimeAccessAuthorizer.getVerifiedGroupAdmin(ctx, token, chatId, senderId)
        if (member == null) {
            ctx.telegramApi.sendMessage(
                token,
                chatId,
                "⛔ <b>ᴀᴄᴄᴇss ᴅᴇɴɪᴇᴅ:</b> ᴏɴʟʏ ɢʀᴏᴜᴘ ᴀᴅᴍɪɴɪsᴛʀᴀᴛᴏʀs ᴄᴀɴ ᴜsᴇ ᴛʜɪs ᴄᴏᴍᴍᴀɴᴅ.",
                parseMode = "HTML",
                replyToMessageId = replyToMessageId
            )
            return false
        }
        if (member.status == "creator") return true
        val hasPerm = when (permission) {
            ExonPermission.MANAGE_CHAT -> {
                member.canManageChat || member.canChangeInfo || member.canRestrictMembers ||
                    member.canDeleteMessages || member.canPromoteMembers ||
                    member.canPinMessages || member.canInviteUsers
            }
            ExonPermission.PROMOTE_MEMBERS -> member.canPromoteMembers
            ExonPermission.RESTRICT_MEMBERS -> member.canRestrictMembers
            ExonPermission.PIN_MESSAGES -> member.canPinMessages
            ExonPermission.INVITE_USERS -> member.canInviteUsers
        }
        if (!hasPerm) {
            ctx.telegramApi.sendMessage(
                token,
                chatId,
                "⛔ <b>ɪɴsᴜғғɪᴄɪᴇɴᴛ ᴀᴅᴍɪɴ ʀɪɢʜᴛs</b> ғᴏʀ ᴛʜɪs ᴀᴄᴛɪᴏɴ.",
                parseMode = "HTML",
                replyToMessageId = replyToMessageId
            )
            return false
        }
        return true
    }

    private suspend fun isProtectedTarget(
        ctx: RuntimeContext,
        token: String,
        chatId: Long,
        targetId: Long,
        replyToMessageId: Long
    ): Boolean {
        val targetMember = ctx.telegramApi.getChatMember(token, chatId, targetId).getOrNull()
        if (targetMember?.status == "creator") {
            ctx.telegramApi.sendMessage(
                token,
                chatId,
                "👑 <b>ᴄᴀɴɴᴏᴛ ᴍᴏᴅᴇʀᴀᴛᴇ ᴛʜᴇ ɢʀᴏᴜᴘ ᴄʀᴇᴀᴛᴏʀ.</b>",
                parseMode = "HTML",
                replyToMessageId = replyToMessageId
            )
            return true
        }
        return false
    }

    private fun formatWelcomeTemplate(template: String, user: TelegramUser, chatTitle: String): String {
        val first = escapeHtml(user.firstName)
        val full = escapeHtml(listOfNotNull(user.firstName, user.lastName?.takeIf { it.isNotBlank() }).joinToString(" "))
        val uname = user.username?.let { "@${escapeHtml(it)}" } ?: first
        val mention = "<a href=\"tg://user?id=${user.id}\">$first</a>"

        return template
            .replace("{first}", first)
            .replace("{fullname}", full)
            .replace("{username}", uname)
            .replace("{mention}", mention)
            .replace("{chat}", escapeHtml(chatTitle))
    }

    private fun buildHelpOverviewText(): String {
        return "<b>ᴄʜᴏᴏsᴇ ᴏɴᴇ ᴏғ ᴛʜᴇ ᴄᴀᴛᴇɢᴏʀɪᴇs ʙᴇʟᴏᴡ ᴛᴏ sᴇᴇ ᴛʜᴇ ᴀᴠᴀɪʟᴀʙʟᴇ ᴄᴏᴍᴍᴀɴᴅs:</b>\n\n" +
            "👮 <b>ᴀᴅᴍɪɴ</b>: /promote, /demote, /adminlist, /pin, /unpin, /invitelink\n" +
            "🔨 <b>ʙᴀɴs &amp; ᴋɪᴄᴋs</b>: /ban, /sban, /tban, /unban, /kick\n" +
            "🔇 <b>ᴍᴜᴛɪɴɢ</b>: /mute, /tmute, /unmute\n" +
            "⚠️ <b>ᴡᴀʀɴs</b>: /warn, /warns, /resetwarn\n" +
            "🔒 <b>ʟᴏᴄᴋs</b>: /lock, /unlock, /locks\n" +
            "📜 <b>ʀᴜʟᴇs</b>: /rules, /setrules, /clearrules\n" +
            "👋 <b>ᴡᴇʟᴄᴏᴍᴇ &amp; ғʟᴏᴏᴅ</b>: /setwelcome, /welcome, /antiflood\n" +
            "ℹ️ <b>ɪɴғᴏ</b>: /id, /info\n" +
            "⚡ <b>sʏsᴛᴇᴍ</b>: /ping, /alive, /stats"
    }

    private fun buildStatsText(): String {
        val uptime = if (startTime > 0L) formatUptime(System.currentTimeMillis() - startTime) else "0s"
        return "📊 <b>ᴇxᴏɴ ʀᴏʙᴏᴛ 2.0 sᴛᴀᴛɪsᴛɪᴄs</b>\n" +
            "──────────────────\n" +
            "• <b>ᴜᴘᴛɪᴍᴇ:</b> <code>$uptime</code>\n" +
            "• <b>ᴘᴏʟʟs:</b> <code>${pollCounter.get()}</code>\n" +
            "• <b>ᴍᴇssᴀɢᴇs:</b> <code>${messageCounter.get()}</code>\n" +
            "• <b>ᴍᴀɴᴀɢᴇᴅ ᴄʜᴀᴛs:</b> <code>${store.getTrackedChatCount()}</code>\n" +
            "• <b>ᴀᴄᴛɪᴠᴇ ʟᴏᴄᴋs:</b> <code>${store.getTotalActiveLocks()}</code>\n" +
            "• <b>ᴀᴄᴛɪᴠᴇ ᴡᴀʀɴs:</b> <code>${store.getTotalWarnCount()}</code>"
    }

    private fun buildHelpKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(
                    InlineKeyboardButton("👮 Admin", callbackData = "exon:help:admin"),
                    InlineKeyboardButton("🔨 Bans & Mute", callbackData = "exon:help:mod")
                ),
                listOf(
                    InlineKeyboardButton("⚠️ Warns & Locks", callbackData = "exon:help:locks"),
                    InlineKeyboardButton("📜 Welcome & Flood", callbackData = "exon:help:welcome")
                ),
                listOf(
                    InlineKeyboardButton("📊 Live Stats", callbackData = "exon:help:stats")
                )
            )
        )
    }

    private fun buildHelpBackKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(InlineKeyboardButton("⬅️ Back to Menu", callbackData = "exon:help:main"))
            )
        )
    }

    private fun buildRulesKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            inlineKeyboard = listOf(
                listOf(InlineKeyboardButton("🔄 Refresh Rules", callbackData = "exon:rules:refresh"))
            )
        )
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    override suspend fun stop() {
        val ctx = context
        ctx?.log(LogLevel.INFO, TAG, "Stopping Exon Robot runtime...")
        pollJob?.cancel()
        pollJob = null
        floodWindows.clear()
        _state.value = BotRuntimeState.Stopped
        ctx?.log(LogLevel.INFO, TAG, "Exon Robot stopped.")
    }

    private fun formatUptime(uptimeMillis: Long): String {
        val totalSeconds = (uptimeMillis / 1000).coerceAtLeast(0L)
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            append("${seconds}s")
        }
    }

    companion object {
        private const val TAG = "ExonBotRuntime"
        private const val DEFAULT_WELCOME_TEMPLATE = "👋 ᴡᴇʟᴄᴏᴍᴇ {mention} ᴛᴏ <b>{chat}</b>!"
        private val DURATION_REGEX = Regex("^(\\d+)([smhdwSMHDW])$")
        private val URL_REGEX = Regex("(https?://|t\\.me/|www\\.)", RegexOption.IGNORE_CASE)

        fun parseDurationSeconds(raw: String?): Long? {
            if (raw.isNullOrBlank()) return null
            val match = DURATION_REGEX.matchEntire(raw.trim()) ?: return null
            val amount = match.groupValues[1].toLongOrNull() ?: return null
            if (amount <= 0L) return null
            return when (match.groupValues[2].lowercase()) {
                "s" -> amount
                "m" -> amount * 60L
                "h" -> amount * 3600L
                "d" -> amount * 86400L
                "w" -> amount * 604800L
                else -> null
            }
        }
    }
}
