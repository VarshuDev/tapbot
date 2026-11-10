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
    val message: TelegramMessage? = null,
    @SerialName("callback_query") val callbackQuery: TelegramCallbackQuery? = null
)

@Serializable
data class TelegramFileRef(
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_unique_id") val fileUniqueId: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null
)

@Serializable
data class TelegramPhotoSize(
    @SerialName("file_id") val fileId: String = "",
    @SerialName("file_unique_id") val fileUniqueId: String? = null,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class TelegramMessageEntity(
    val type: String,
    val offset: Int = 0,
    val length: Int = 0,
    val url: String? = null
)

@Serializable
data class TelegramMessage(
    @SerialName("message_id") val messageId: Long,
    val from: TelegramUser? = null,
    val chat: TelegramChat,
    val date: Long,
    val text: String? = null,
    @SerialName("reply_to_message") val replyToMessage: TelegramMessage? = null,
    @SerialName("new_chat_members") val newChatMembers: List<TelegramUser>? = null,
    val sticker: TelegramFileRef? = null,
    val photo: List<TelegramPhotoSize>? = null,
    val document: TelegramFileRef? = null,
    val caption: String? = null,
    val entities: List<TelegramMessageEntity>? = null,
    @SerialName("caption_entities") val captionEntities: List<TelegramMessageEntity>? = null,
    @SerialName("forward_date") val forwardDate: Long? = null
)

@Serializable
data class TelegramCallbackQuery(
    val id: String,
    val from: TelegramUser,
    val message: TelegramMessage? = null,
    val data: String? = null,
    @SerialName("chat_instance") val chatInstance: String? = null
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
data class InlineKeyboardButton(
    val text: String,
    @SerialName("callback_data") val callbackData: String? = null,
    val url: String? = null
)

@Serializable
data class InlineKeyboardMarkup(
    @SerialName("inline_keyboard") val inlineKeyboard: List<List<InlineKeyboardButton>>
)

@Serializable
data class SendMessagePayload(
    @SerialName("chat_id") val chatId: Long,
    val text: String,
    @SerialName("parse_mode") val parseMode: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null,
    @SerialName("reply_markup") val replyMarkup: InlineKeyboardMarkup? = null
)

@Serializable
data class AnswerCallbackQueryPayload(
    @SerialName("callback_query_id") val callbackQueryId: String,
    val text: String? = null,
    @SerialName("show_alert") val showAlert: Boolean = false
)

@Serializable
data class EditMessageTextPayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("message_id") val messageId: Long,
    val text: String,
    @SerialName("parse_mode") val parseMode: String? = null,
    @SerialName("reply_markup") val replyMarkup: InlineKeyboardMarkup? = null
)

@Serializable
data class BanChatMemberPayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("until_date") val untilDate: Long? = null
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
    val permissions: RestrictChatPermissions,
    @SerialName("until_date") val untilDate: Long? = null
)

@Serializable
data class PromoteChatMemberPayload(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("can_manage_chat") val canManageChat: Boolean,
    @SerialName("can_delete_messages") val canDeleteMessages: Boolean,
    @SerialName("can_manage_video_chats") val canManageVideoChats: Boolean,
    @SerialName("can_restrict_members") val canRestrictMembers: Boolean,
    @SerialName("can_promote_members") val canPromoteMembers: Boolean,
    @SerialName("can_change_info") val canChangeInfo: Boolean,
    @SerialName("can_invite_users") val canInviteUsers: Boolean,
    @SerialName("can_pin_messages") val canPinMessages: Boolean
) {
    companion object {
        fun promote(chatId: Long, userId: Long) = PromoteChatMemberPayload(
            chatId = chatId,
            userId = userId,
            canManageChat = true,
            canDeleteMessages = true,
            canManageVideoChats = true,
            canRestrictMembers = true,
            canPromoteMembers = false,
            canChangeInfo = true,
            canInviteUsers = true,
            canPinMessages = true
        )

        fun demote(chatId: Long, userId: Long) = PromoteChatMemberPayload(
            chatId = chatId,
            userId = userId,
            canManageChat = false,
            canDeleteMessages = false,
            canManageVideoChats = false,
            canRestrictMembers = false,
            canPromoteMembers = false,
            canChangeInfo = false,
            canInviteUsers = false,
            canPinMessages = false
        )
    }
}

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

@Serializable
data class SendAudioPayload(
    @SerialName("chat_id") val chatId: Long,
    val audio: String,
    val caption: String? = null,
    @SerialName("parse_mode") val parseMode: String? = null,
    val duration: Int? = null,
    val performer: String? = null,
    val title: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null
)

