package com.tapbot.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a bot command on Telegram servers.
 * @property command Text of the command; 1-32 characters. Can contain only lowercase English letters, digits and underscores.
 * @property description Description of the command; 1-256 characters.
 */
@Serializable
data class TelegramBotCommand(
    val command: String,
    val description: String
)

/**
 * Payload for Telegram Bot API `setMyCommands`.
 */
@Serializable
data class SetMyCommandsPayload(
    val commands: List<TelegramBotCommand>,
    val scope: TelegramBotCommandScope? = null,
    @SerialName("language_code") val languageCode: String? = null
)

/**
 * Scope for bot commands (default, all_private_chats, all_group_chats, etc.).
 */
@Serializable
data class TelegramBotCommandScope(
    val type: String = "default"
)
