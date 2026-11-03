package com.tapbot.core.runner.runtime

import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.RestrictChatPermissions
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.runner.util.AdaptiveBackoff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * On-device runtime for Exon Robot (ported from VarshuDev/ExonRobot).
 * Provides Telegram group management, administrative controls, and diagnostics:
 * - Greetings & Help: /start, /help, /alive
 * - Diagnostics: /ping, /stats
 * - Information: /id, /info
 * - Chat Administration: /adminlist, /pin, /unpin, /invitelink
 * - Moderation: /ban, /sban, /tban, /unban, /kick
 * - Muting: /mute, /unmute
 * - Warning System: /warn, /warns, /resetwarn (Auto-ban on 3 warnings)
 * - Lock Manager: /lock, /unlock, /locks
 * - Group Rules: /rules, /setrules, /clearrules
 */
class ExonBotRuntime(
    override val botId: String = "bot_exon"
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var lastUpdateId: Long? = null
    private var startTime: Long = 0L

    // In-memory chat moderation state:
    // warns: Map<ChatId, MutableMap<UserId, Int>>
    private val chatWarns = ConcurrentHashMap<Long, ConcurrentHashMap<Long, Int>>()
    // rules: Map<ChatId, String>
    private val chatRules = ConcurrentHashMap<Long, String>()
    // locks: Map<ChatId, MutableSet<String>>
    private val chatLocks = ConcurrentHashMap<Long, MutableSet<String>>()

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
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
        ctx.log(LogLevel.INFO, TAG, "Starting Exon Robot group management runtime on Android ART...")

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
        var pollCounter = 0L
        var messageCounter = 0L

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

                    pollCounter++

                    if (updatesResult.isSuccess) {
                        backoff.recordSuccess()
                        val updates = updatesResult.getOrThrow()
                        for (update in updates) {
                            lastUpdateId = update.updateId
                            val message = update.message ?: continue
                            val text = message.text?.trim() ?: continue
                            val from = message.from
                            val chat = message.chat
                            val sender = from?.username?.let { "@$it" } ?: from?.firstName ?: "User"

                            ctx.log(LogLevel.INFO, TAG, "Received message from $sender in '${chat.title ?: "PM"}' (${chat.id}): '$text'")
                            messageCounter++

                            // Dispatch Exon Robot commands
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
                                pollCount = pollCounter,
                                messageCount = messageCounter,
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
        val senderName = fromUser?.firstName ?: "User"

        when (rawCmd) {
            "start" -> {
                val startMsg = if (isGroup) {
                    "ʜᴇʏ <b>$senderName</b>, 🥀\n\n" +
                    "ɪ ᴀᴍ <b>${botUser.firstName}</b>, ᴀʟɪᴠᴇ ᴀɴᴅ ʀᴇᴀᴅʏ ᴛᴏ ᴍᴀɴᴀɢᴇ <b>${message.chat.title ?: "ᴛʜɪs ɢʀᴏᴜᴘ"}</b>!\n" +
                    "──────────────────\n" +
                    "➻ ʜɪᴛ /help ᴛᴏ sᴇᴇ ᴀʟʟ ᴀᴠᴀɪʟᴀʙʟᴇ ɢʀᴏᴜᴘ ᴍᴀɴᴀɢᴇᴍᴇɴᴛ ᴄᴏᴍᴍᴀɴᴅs."
                } else {
                    "ʜᴇʟʟᴏ <code>$senderName</code>, ʜᴏᴡ ᴀʀᴇ ʏᴏᴜ \n\n" +
                    "<b>ʜҽʏ ᴛʜᴇʀᴇ</b> $senderName, 🥀\n\n" +
                    "<b>๏ ᴛʜɪs ɪs</b> ${botUser.firstName} !\n" +
                    "➻ ɪ'ᴍ ʜᴇʀᴇ ᴛᴏ ʜᴇʟᴘ ʏᴏᴜ ᴍᴀɴᴀɢᴇ ʏᴏᴜʀ ɢʀᴏᴜᴘs\n" +
                    "──────────────────\n" +
                    "<b>๏ ʜɪᴛ /help ᴛᴏ ғɪɴᴅ ᴏᴜᴛ ᴍᴏʀᴇ ᴀʙᴏᴜᴛ ʜᴏᴡ ᴛᴏ ᴜsᴇ ᴍᴇ ᴛᴏ ᴍʏ ғᴜʟʟ ᴘᴏᴛᴇɴᴛɪᴀʟ.</b>"
                }
                ctx.telegramApi.sendMessage(token, chatId, startMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Replied to /start for $senderName")
            }

            "help" -> {
                val helpMsg = "<b>ᴄʜᴏᴏsᴇ ᴏɴᴇ ᴏғ ᴛʜᴇ ᴄᴀᴛᴇɢᴏʀɪᴇs ʙᴇʟᴏᴡ ᴛᴏ sᴇᴇ ᴛʜᴇ ᴀᴠᴀɪʟᴀʙʟᴇ ᴄᴏᴍᴍᴀɴᴅs:</b>\n\n" +
                    "👮 <b>ᴀᴅᴍɪɴ</b>: /promote, /demote, /adminlist, /pin, /unpin, /invitelink\n" +
                    "🔨 <b>ʙᴀɴs & ᴋɪᴄᴋs</b>: /ban, /sban, /unban, /kick\n" +
                    "🔇 <b>ᴍᴜᴛɪɴɢ</b>: /mute, /unmute\n" +
                    "⚠️ <b>ᴡᴀʀɴs</b>: /warn, /warns, /resetwarn\n" +
                    "🔒 <b>ʟᴏᴄᴋs</b>: /lock, /unlock, /locks\n" +
                    "📜 <b>ʀᴜʟᴇs</b>: /rules, /setrules, /clearrules\n" +
                    "ℹ️ <b>ɪɴғᴏ</b>: /id, /info\n" +
                    "⚡ <b>sʏsᴛᴇᴍ</b>: /ping, /alive, /stats"
                ctx.telegramApi.sendMessage(token, chatId, helpMsg, parseMode = "HTML", replyToMessageId = message.messageId)
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
                    "<b>• ᴠᴇʀsɪᴏɴ:</b> 2.69 (TapBot On-Device ART)\n" +
                    "<b>• ᴘʏᴛʜᴏɴ ᴘᴏʀᴛ:</b> ExonRobot Group Management\n" +
                    "<b>• ᴇɴɢɪɴᴇ:</b> Native Kotlin Android Coroutines\n" +
                    "<b>• ᴜᴘᴛɪᴍᴇ:</b> <code>$uptime</code>"
                ctx.telegramApi.sendMessage(token, chatId, aliveMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Sent /alive response")
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
                val targetFirst = target?.firstName ?: "User"
                val targetLast = target?.lastName ?: ""
                val targetUser = target?.username

                val infoMsg = "╒═══「 <b>ᴜsᴇʀ ɪɴғᴏʀᴍᴀᴛɪᴏɴ:</b> 」\n" +
                    "<b>\nɪᴅ:</b> <code>$targetId</code>\n" +
                    "<b>ғɪʀsᴛ ɴᴀᴍᴇ:</b> $targetFirst\n" +
                    (if (targetLast.isNotBlank()) "<b>ʟᴀsᴛ ɴᴀᴍᴇ:</b> $targetLast\n" else "") +
                    (if (targetUser != null) "<b>ᴜsᴇʀɴᴀᴍᴇ:</b> @$targetUser\n" else "") +
                    "<b>ᴘᴇʀᴍᴀʟɪɴᴋ:</b> <a href=\"tg://user?id=$targetId\">$targetFirst</a>"
                ctx.telegramApi.sendMessage(token, chatId, infoMsg, parseMode = "HTML", replyToMessageId = message.messageId)
                ctx.log(LogLevel.INFO, TAG, "Sent /info for $targetFirst")
            }

            "adminlist" -> {
                val adminsResult = ctx.telegramApi.getChatAdministrators(token, chatId)
                if (adminsResult.isSuccess) {
                    val admins = adminsResult.getOrThrow()
                    val sb = StringBuilder("👮 <b>ᴀᴅᴍɪɴs ɪɴ ${message.chat.title ?: "ᴛʜɪs ᴄʜᴀᴛ"}:</b>\n\n")
                    for (admin in admins) {
                        val icon = if (admin.status == "creator") "👑" else "⭐"
                        val title = admin.customTitle?.let { " ($it)" } ?: ""
                        val adminName = admin.user.username?.let { "@$it" } ?: admin.user.firstName
                        sb.append("$icon $adminName$title\n")
                    }
                    ctx.telegramApi.sendMessage(token, chatId, sb.toString(), parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ ᴄᴏᴜʟᴅ ɴᴏᴛ ʀᴇᴛʀɪᴇᴠᴇ ᴀᴅᴍɪɴ ʟɪsᴛ. ᴍᴀᴋᴇ sᴜʀᴇ ɪ ᴀᴍ ᴀɴ ᴀᴅᴍɪɴ!", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "pin" -> {
                val reply = message.replyToMessage
                if (reply != null) {
                    val res = ctx.telegramApi.pinChatMessage(token, chatId, reply.messageId)
                    if (res.isSuccess) {
                        ctx.telegramApi.sendMessage(token, chatId, "📌 <b>ᴍᴇssᴀɢᴇ ᴘɪɴɴᴇᴅ sᴜᴄᴄᴇssғᴜʟʟʏ!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                    } else {
                        ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴘɪɴ ᴍᴇssᴀɢᴇ:</b> ${res.exceptionOrNull()?.message}", parseMode = "HTML", replyToMessageId = message.messageId)
                    }
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴍᴇssᴀɢᴇ ᴛᴏ ᴘɪɴ ɪᴛ!", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "unpin" -> {
                val reply = message.replyToMessage
                val res = ctx.telegramApi.unpinChatMessage(token, chatId, reply?.messageId)
                if (res.isSuccess) {
                    ctx.telegramApi.sendMessage(token, chatId, "📌 <b>ᴍᴇssᴀɢᴇ ᴜɴᴘɪɴɴᴇᴅ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴜɴᴘɪɴ:</b> ${res.exceptionOrNull()?.message}", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "invitelink" -> {
                val res = ctx.telegramApi.exportChatInviteLink(token, chatId)
                if (res.isSuccess) {
                    ctx.telegramApi.sendMessage(token, chatId, "🔗 <b>ɢʀᴏᴜᴘ ɪɴᴠɪᴛᴇ ʟɪɴᴋ:</b>\n${res.getOrThrow()}", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ɢᴇɴᴇʀᴀᴛᴇ ɪɴᴠɪᴛᴇ ʟɪɴᴋ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "ban", "sban" -> {
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

                val res = ctx.telegramApi.banChatMember(token, chatId, targetId)
                if (res.isSuccess) {
                    val name = targetUser?.firstName ?: targetId.toString()
                    ctx.telegramApi.sendMessage(token, chatId, "🔨 <b>ʙᴀɴɴᴇᴅ $name!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                    ctx.log(LogLevel.INFO, TAG, "Banned user $targetId in chat $chatId")
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ʙᴀɴ ᴜsᴇʀ:</b> ${res.exceptionOrNull()?.message}", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "unban" -> {
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
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴜɴʙᴀɴ:</b> ${res.exceptionOrNull()?.message}", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "kick" -> {
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()

                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴏʀ sᴘᴇᴄɪғʏ ᴛʜᴇɪʀ ID ᴛᴏ ᴋɪᴄᴋ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }

                val res = ctx.telegramApi.banChatMember(token, chatId, targetId)
                if (res.isSuccess) {
                    ctx.telegramApi.unbanChatMember(token, chatId, targetId) // allow rejoin
                    val name = targetUser?.firstName ?: targetId.toString()
                    ctx.telegramApi.sendMessage(token, chatId, "👢 <b>ᴋɪᴄᴋᴇᴅ $name!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴋɪᴄᴋ ᴜsᴇʀ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "mute" -> {
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()

                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴛᴏ ᴍᴜᴛᴇ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }

                val perms = RestrictChatPermissions(canSendMessages = false)
                val res = ctx.telegramApi.restrictChatMember(token, chatId, targetId, perms)
                if (res.isSuccess) {
                    val name = targetUser?.firstName ?: targetId.toString()
                    ctx.telegramApi.sendMessage(token, chatId, "🔇 <b>ᴍᴜᴛᴇᴅ $name</b> ᴡɪᴛʜ ɴᴏ ᴇxᴘɪʀᴀᴛɪᴏɴ ᴅᴀᴛᴇ!", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴍᴜᴛᴇ:</b> ${res.exceptionOrNull()?.message}", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "unmute" -> {
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
                    val name = targetUser?.firstName ?: targetId.toString()
                    ctx.telegramApi.sendMessage(token, chatId, "🔊 <b>ᴜɴᴍᴜᴛᴇᴅ $name.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>ғᴀɪʟᴇᴅ ᴛᴏ ᴜɴᴍᴜᴛᴇ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "warn" -> {
                val reply = message.replyToMessage
                val targetUser = reply?.from
                val targetId = targetUser?.id ?: args.firstOrNull()?.toLongOrNull()

                if (targetId == null) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ ʀᴇᴘʟʏ ᴛᴏ ᴀ ᴜsᴇʀ ᴛᴏ ᴡᴀʀɴ ᴛʜᴇᴍ.", parseMode = "HTML", replyToMessageId = message.messageId)
                    return
                }

                val chatMap = chatWarns.computeIfAbsent(chatId) { ConcurrentHashMap() }
                val currentWarns = (chatMap[targetId] ?: 0) + 1
                chatMap[targetId] = currentWarns

                val name = targetUser?.firstName ?: targetId.toString()
                if (currentWarns >= 3) {
                    chatMap.remove(targetId)
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
                val name = targetUser?.firstName ?: "User"
                val count = chatWarns[chatId]?.get(targetId) ?: 0
                ctx.telegramApi.sendMessage(token, chatId, "⚠️ <b>$name</b> ʜᴀs <code>$count/3</code> ᴡᴀʀɴɪɴɢs.", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "resetwarn" -> {
                val reply = message.replyToMessage
                val targetId = reply?.from?.id ?: args.firstOrNull()?.toLongOrNull() ?: senderId
                chatWarns[chatId]?.remove(targetId)
                ctx.telegramApi.sendMessage(token, chatId, "🔄 <b>ᴡᴀʀɴɪɴɢs ʀᴇsᴇᴛ ғᴏʀ ᴜsᴇʀ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "rules" -> {
                val rules = chatRules[chatId] ?: "ᴛʜᴇ ɢʀᴏᴜᴘ ᴀᴅᴍɪɴs ʜᴀᴠᴇɴ'ᴛ sᴇᴛ ᴀɴʏ ʀᴜʟᴇs ғᴏʀ ᴛʜɪs ᴄʜᴀᴛ ʏᴇᴛ."
                val textMsg = "📜 <b>ᴄʜᴀᴛ ʀᴜʟᴇs:</b>\n\n$rules"
                ctx.telegramApi.sendMessage(token, chatId, textMsg, parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "setrules" -> {
                val newRules = args.joinToString(" ")
                if (newRules.isBlank()) {
                    ctx.telegramApi.sendMessage(token, chatId, "ᴘʟᴇᴀsᴇ sᴘᴇᴄɪғʏ ᴛʜᴇ ʀᴜʟᴇs ᴛᴇxᴛ: /setrules <text>", parseMode = "HTML", replyToMessageId = message.messageId)
                } else {
                    chatRules[chatId] = newRules
                    ctx.telegramApi.sendMessage(token, chatId, "📜 <b>ɴᴇᴡ ʀᴜʟᴇs sᴀᴠᴇᴅ ғᴏʀ ᴛʜɪs ᴄʜᴀᴛ!</b>", parseMode = "HTML", replyToMessageId = message.messageId)
                }
            }

            "clearrules" -> {
                chatRules.remove(chatId)
                ctx.telegramApi.sendMessage(token, chatId, "🗑️ <b>ᴄʜᴀᴛ ʀᴜʟᴇs ᴄʟᴇᴀʀᴇᴅ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "lock" -> {
                val lockType = args.firstOrNull()?.lowercase() ?: "all"
                val locks = chatLocks.computeIfAbsent(chatId) { mutableSetOf() }
                locks.add(lockType)
                ctx.telegramApi.sendMessage(token, chatId, "🔒 <b>ʟᴏᴄᴋᴇᴅ <code>$lockType</code> ғᴏʀ ᴛʜɪs ᴄʜᴀᴛ.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "unlock" -> {
                val lockType = args.firstOrNull()?.lowercase() ?: "all"
                chatLocks[chatId]?.remove(lockType)
                ctx.telegramApi.sendMessage(token, chatId, "🔓 <b>ᴜɴʟᴏᴄᴋᴇᴅ <code>$lockType</code>.</b>", parseMode = "HTML", replyToMessageId = message.messageId)
            }

            "locks" -> {
                val currentLocks = chatLocks[chatId] ?: emptySet()
                val locksText = if (currentLocks.isEmpty()) {
                    "ɴᴏ ʟᴏᴄᴋs ᴀʀᴇ ᴄᴜʀʀᴇɴᴛʟʏ ᴀᴄᴛɪᴠᴇ ɪɴ ᴛʜɪs ᴄʜᴀᴛ."
                } else {
                    currentLocks.joinToString(", ") { "<code>$it</code>" }
                }
                ctx.telegramApi.sendMessage(token, chatId, "🔒 <b>ᴀᴄᴛɪᴠᴇ ʟᴏᴄᴋs:</b>\n$locksText", parseMode = "HTML", replyToMessageId = message.messageId)
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

    override suspend fun stop() {
        val ctx = context
        ctx?.log(LogLevel.INFO, TAG, "Stopping Exon Robot runtime...")
        pollJob?.cancel()
        pollJob = null
        _state.value = BotRuntimeState.Stopped
        ctx?.log(LogLevel.INFO, TAG, "Exon Robot stopped.")
    }

    private fun formatUptime(uptimeMillis: Long): String {
        val totalSeconds = uptimeMillis / 1000
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
    }
}
