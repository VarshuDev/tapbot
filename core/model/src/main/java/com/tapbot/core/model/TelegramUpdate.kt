package com.tapbot.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelegramApiResponse<T>(
    val ok: Boolean,
    val result: T? = null,
    val description: String? = null,
    @SerialName("error_code") val errorCode: Int? = null
)

@Serializable
data class TelegramUpdate(
    @SerialName("update_id") val updateId: Long,
    val message: TelegramMessage? = null
)

@Serializable
data class TelegramMessage(
    @SerialName("message_id") val messageId: Long,
    val from: TelegramUser? = null,
    val chat: TelegramChat,
    val date: Long,
    val text: String? = null,
    @SerialName("reply_to_message") val replyToMessage: TelegramMessage? = null
)

@Serializable
data class TelegramUser(
    val id: Long,
    @SerialName("is_bot") val isBot: Boolean = false,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String? = null,
    val username: String? = null
)

@Serializable
data class TelegramChat(
    val id: Long,
    val type: String,
    val title: String? = null,
    val username: String? = null,
    @SerialName("first_name") val firstName: String? = null
)

@Serializable
data class TelegramChatMember(
    val user: TelegramUser,
    val status: String,
    @SerialName("custom_title") val customTitle: String? = null
)

@Serializable
data class SendMessagePayload(
    @SerialName("chat_id") val chatId: Long,
    val text: String,
    @SerialName("parse_mode") val parseMode: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null
)

@Serializable
data class BanChatMemberPayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long
)

@Serializable
data class UnbanChatMemberPayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("only_if_banned") val onlyIfBanned: Boolean = true
)

@Serializable
data class RestrictChatPermissions(
    @SerialName("can_send_messages") val canSendMessages: Boolean = false,
    @SerialName("can_send_media_messages") val canSendMediaMessages: Boolean = false,
    @SerialName("can_send_polls") val canSendPolls: Boolean = false,
    @SerialName("can_send_other_messages") val canSendOtherMessages: Boolean = false,
    @SerialName("can_add_web_page_previews") val canAddWebPagePreviews: Boolean = false
)

@Serializable
data class RestrictChatMemberPayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long,
    val permissions: RestrictChatPermissions
)

@Serializable
data class PinChatMessagePayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("message_id") val messageId: Long,
    @SerialName("disable_notification") val disableNotification: Boolean = false
)

@Serializable
data class UnpinChatMessagePayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("message_id") val messageId: Long? = null
)

@Serializable
data class DeleteMessagePayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("message_id") val messageId: Long
)
