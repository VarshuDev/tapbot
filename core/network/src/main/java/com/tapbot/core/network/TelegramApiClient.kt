package com.tapbot.core.network

import com.tapbot.core.model.AnswerCallbackQueryPayload
import com.tapbot.core.model.EditMessageTextPayload
import com.tapbot.core.model.InlineKeyboardMarkup
import com.tapbot.core.model.PromoteChatMemberPayload
import com.tapbot.core.model.SendAudioPayload
import com.tapbot.core.model.SendMessagePayload
import com.tapbot.core.model.TelegramApiResponse
import com.tapbot.core.model.TelegramChatMember
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.security.SecretRedactor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Client for communicating with the official Telegram Bot API via HTTPS.
 * Executes on the user's Android device.
 */
open class TelegramApiClient(
    private val client: OkHttpClient = sharedClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend inline fun <T> safeTelegramCall(crossinline block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            val safeMsg = SecretRedactor.redact(t.message ?: "Telegram API error")
            val wrapped = when (t) {
                is SecurityException -> SecurityException(safeMsg)
                is IllegalArgumentException -> IllegalArgumentException(safeMsg)
                else -> IOException(safeMsg)
            }
            Result.failure(wrapped)
        }
    }

    /**
     * Verifies the bot token and returns information about the bot.
     */
    open suspend fun getMe(token: String): Result<TelegramUser> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/getMe"
            val request = Request.Builder().url(url).get().build()
            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<TelegramUser>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: throw IOException("Empty user object returned by Telegram API")
            } else {
                throw IOException(apiResponse.description ?: "Unknown Telegram API error")
            }
        }
    }

    /**
     * Long-polling request to fetch new updates.
     * Uses Telegram's long-polling HTTP mechanism.
     */
    open suspend fun getUpdates(
        token: String,
        offset: Long? = null,
        timeoutSeconds: Int = 30
    ): Result<List<TelegramUpdate>> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val offsetParam = if (offset != null) "&offset=$offset" else ""
            val url = "$BASE_URL/bot$token/getUpdates?timeout=$timeoutSeconds$offsetParam"
            val request = Request.Builder().url(url).get().build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<List<TelegramUpdate>>>(body)
            if (apiResponse.ok) {
                apiResponse.result.orEmpty()
            } else {
                throw IOException(apiResponse.description ?: "getUpdates error")
            }
        }
    }

    /**
     * Sends a text message to a chat or channel.
     */
    open suspend fun sendMessage(
        token: String,
        chatId: Long,
        text: String,
        parseMode: String?
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/sendMessage"
            val payload = SendMessagePayload(
                chatId = chatId,
                text = text,
                parseMode = parseMode,
                replyToMessageId = null
            )
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<kotlinx.serialization.json.JsonElement>>(body)
            if (apiResponse.ok) {
                true
            } else {
                throw IOException(apiResponse.description ?: "sendMessage error")
            }
        }
    }

    /**
     * Sends a text message to a chat or channel, optionally as a reply.
     */
    open suspend fun sendMessage(
        token: String,
        chatId: Long,
        text: String,
        parseMode: String? = null,
        replyToMessageId: Long? = null
    ): Result<Boolean> {
        if (replyToMessageId == null) {
            return sendMessage(token, chatId, text, parseMode)
        }
        return safeTelegramCall {
            withContext(Dispatchers.IO) {
                val url = "$BASE_URL/bot$token/sendMessage"
                val payload = SendMessagePayload(
                    chatId = chatId,
                    text = text,
                    parseMode = parseMode,
                    replyToMessageId = replyToMessageId
                )
                val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).post(requestBody).build()

                val body = client.newCall(request).execute().use { response ->
                    response.body?.string().orEmpty()
                }
                val apiResponse = json.decodeFromString<TelegramApiResponse<kotlinx.serialization.json.JsonElement>>(body)
                if (apiResponse.ok) {
                    true
                } else {
                    throw IOException(apiResponse.description ?: "sendMessage error")
                }
            }
        }
    }

    /**
     * Sends a text message to a chat or channel, optionally with reply-to and inline keyboard markup.
     */
    open suspend fun sendMessage(
        token: String,
        chatId: Long,
        text: String,
        parseMode: String?,
        replyToMessageId: Long?,
        replyMarkup: InlineKeyboardMarkup?
    ): Result<Boolean> {
        if (replyMarkup == null) {
            return sendMessage(token, chatId, text, parseMode, replyToMessageId)
        }
        return safeTelegramCall {
            withContext(Dispatchers.IO) {
                val url = "$BASE_URL/bot$token/sendMessage"
                val payload = SendMessagePayload(
                    chatId = chatId,
                    text = text,
                    parseMode = parseMode,
                    replyToMessageId = replyToMessageId,
                    replyMarkup = replyMarkup
                )
                val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).post(requestBody).build()

                val body = client.newCall(request).execute().use { response ->
                    response.body?.string().orEmpty()
                }
                val apiResponse = json.decodeFromString<TelegramApiResponse<kotlinx.serialization.json.JsonElement>>(body)
                if (apiResponse.ok) {
                    true
                } else {
                    throw IOException(apiResponse.description ?: "sendMessage error")
                }
            }
        }
    }

    /**
     * Answers an inline button callback query to dismiss the loading spinner or show a toast/alert.
     */
    open suspend fun answerCallbackQuery(
        token: String,
        callbackQueryId: String,
        text: String? = null,
        showAlert: Boolean = false
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/answerCallbackQuery"
            val payload = AnswerCallbackQueryPayload(
                callbackQueryId = callbackQueryId,
                text = text,
                showAlert = showAlert
            )
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "answerCallbackQuery error")
            }
        }
    }

    /**
     * Edits the text and optional inline keyboard of an existing message.
     */
    open suspend fun editMessageText(
        token: String,
        chatId: Long,
        messageId: Long,
        text: String,
        parseMode: String? = null,
        replyMarkup: InlineKeyboardMarkup? = null
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/editMessageText"
            val payload = EditMessageTextPayload(
                chatId = chatId,
                messageId = messageId,
                text = text,
                parseMode = parseMode,
                replyMarkup = replyMarkup
            )
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<kotlinx.serialization.json.JsonElement>>(body)
            if (apiResponse.ok) {
                true
            } else {
                throw IOException(apiResponse.description ?: "editMessageText error")
            }
        }
    }

    /**
     * Bans a user from a chat group.
     */
    open suspend fun banChatMember(
        token: String,
        chatId: Long,
        userId: Long
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/banChatMember"
            val payload = com.tapbot.core.model.BanChatMemberPayload(chatId, userId)
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "banChatMember error")
            }
        }
    }

    /**
     * Bans a user from a chat group until a specific Unix timestamp (timed ban).
     */
    open suspend fun banChatMember(
        token: String,
        chatId: Long,
        userId: Long,
        untilDate: Long?
    ): Result<Boolean> {
        if (untilDate == null) {
            return banChatMember(token, chatId, userId)
        }
        return safeTelegramCall {
            withContext(Dispatchers.IO) {
                val url = "$BASE_URL/bot$token/banChatMember"
                val payload = com.tapbot.core.model.BanChatMemberPayload(chatId, userId, untilDate)
                val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).post(requestBody).build()

                val body = client.newCall(request).execute().use { response ->
                    response.body?.string().orEmpty()
                }
                val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
                if (apiResponse.ok) {
                    apiResponse.result ?: true
                } else {
                    throw IOException(apiResponse.description ?: "banChatMember error")
                }
            }
        }
    }

    /**
     * Unbans a previously banned user.
     */
    open suspend fun unbanChatMember(
        token: String,
        chatId: Long,
        userId: Long
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/unbanChatMember"
            val payload = com.tapbot.core.model.UnbanChatMemberPayload(chatId, userId)
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "unbanChatMember error")
            }
        }
    }

    /**
     * Restricts a chat member (mute / permissions limit).
     */
    open suspend fun restrictChatMember(
        token: String,
        chatId: Long,
        userId: Long,
        permissions: com.tapbot.core.model.RestrictChatPermissions
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/restrictChatMember"
            val payload = com.tapbot.core.model.RestrictChatMemberPayload(chatId, userId, permissions)
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "restrictChatMember error")
            }
        }
    }

    /**
     * Restricts a chat member until a specific Unix timestamp (timed mute).
     */
    open suspend fun restrictChatMember(
        token: String,
        chatId: Long,
        userId: Long,
        permissions: com.tapbot.core.model.RestrictChatPermissions,
        untilDate: Long?
    ): Result<Boolean> {
        if (untilDate == null) {
            return restrictChatMember(token, chatId, userId, permissions)
        }
        return safeTelegramCall {
            withContext(Dispatchers.IO) {
                val url = "$BASE_URL/bot$token/restrictChatMember"
                val payload = com.tapbot.core.model.RestrictChatMemberPayload(chatId, userId, permissions, untilDate)
                val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
                val request = Request.Builder().url(url).post(requestBody).build()

                val body = client.newCall(request).execute().use { response ->
                    response.body?.string().orEmpty()
                }
                val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
                if (apiResponse.ok) {
                    apiResponse.result ?: true
                } else {
                    throw IOException(apiResponse.description ?: "restrictChatMember error")
                }
            }
        }
    }

    /**
     * Promotes or demotes a chat member in a group/supergroup.
     */
    open suspend fun promoteChatMember(
        token: String,
        payload: PromoteChatMemberPayload
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/promoteChatMember"
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "promoteChatMember error")
            }
        }
    }

    /**
     * Pins a message in a group.
     */
    open suspend fun pinChatMessage(
        token: String,
        chatId: Long,
        messageId: Long
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/pinChatMessage"
            val payload = com.tapbot.core.model.PinChatMessagePayload(chatId, messageId)
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "pinChatMessage error")
            }
        }
    }

    /**
     * Unpins a message in a group.
     */
    open suspend fun unpinChatMessage(
        token: String,
        chatId: Long,
        messageId: Long? = null
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/unpinChatMessage"
            val payload = com.tapbot.core.model.UnpinChatMessagePayload(chatId, messageId)
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "unpinChatMessage error")
            }
        }
    }

    /**
     * Deletes a message from a group.
     */
    open suspend fun deleteMessage(
        token: String,
        chatId: Long,
        messageId: Long
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/deleteMessage"
            val payload = com.tapbot.core.model.DeleteMessagePayload(chatId, messageId)
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "deleteMessage error")
            }
        }
    }

    /**
     * Exports a new invite link for a group.
     */
    open suspend fun exportChatInviteLink(
        token: String,
        chatId: Long
    ): Result<String> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/exportChatInviteLink"
            val payload = mapOf("chat_id" to chatId.toString())
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<String>>(body)
            if (apiResponse.ok && apiResponse.result != null) {
                apiResponse.result!!
            } else {
                throw IOException(apiResponse.description ?: "exportChatInviteLink error")
            }
        }
    }

    /**
     * Retrieves the list of administrators in a chat.
     */
    open suspend fun getChatAdministrators(
        token: String,
        chatId: Long
    ): Result<List<TelegramChatMember>> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/getChatAdministrators?chat_id=$chatId"
            val request = Request.Builder().url(url).get().build()

            val body = client.newCall(request).execute().use { response ->
                UrlSecurityValidator.readResponseBodyCapped(response.body, MAX_TELEGRAM_JSON_BYTES)
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<List<TelegramChatMember>>>(body)
            if (apiResponse.ok) {
                apiResponse.result.orEmpty()
            } else {
                throw IOException(apiResponse.description ?: "getChatAdministrators error")
            }
        }
    }

    /**
     * Retrieves membership status and admin permissions of a single user in a chat.
     */
    open suspend fun getChatMember(
        token: String,
        chatId: Long,
        userId: Long
    ): Result<TelegramChatMember> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/getChatMember?chat_id=$chatId&user_id=$userId"
            val request = Request.Builder().url(url).get().build()

            val body = client.newCall(request).execute().use { response ->
                UrlSecurityValidator.readResponseBodyCapped(response.body, MAX_TELEGRAM_JSON_BYTES)
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<TelegramChatMember>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: throw IOException("Empty chat member response")
            } else {
                throw IOException(apiResponse.description ?: "getChatMember error")
            }
        }
    }

    /**
     * Uploads and sends a document (e.g. APK, log file, botpkg) to a chat or channel.
     */
    open suspend fun sendDocument(
        token: String,
        chatId: Long,
        file: File,
        caption: String? = null,
        parseMode: String? = null
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/sendDocument"
            val mediaType = "application/octet-stream".toMediaType()
            val fileBody = file.asRequestBody(mediaType)

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId.toString())
                .addFormDataPart("document", file.name, fileBody)

            if (!caption.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("caption", caption)
            }
            if (!parseMode.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("parse_mode", parseMode)
            }

            val request = Request.Builder()
                .url(url)
                .post(multipartBuilder.build())
                .build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<kotlinx.serialization.json.JsonElement>>(body)
            if (apiResponse.ok) {
                true
            } else {
                throw IOException(apiResponse.description ?: "sendDocument error")
            }
        }
    }

    /**
     * Sends an audio track to a chat via direct audio stream/file URL.
     */
    open suspend fun sendAudio(
        token: String,
        chatId: Long,
        audioUrl: String,
        title: String? = null,
        performer: String? = null,
        duration: Int? = null,
        caption: String? = null,
        parseMode: String? = null,
        replyToMessageId: Long? = null
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/sendAudio"
            val payload = SendAudioPayload(
                chatId = chatId,
                audio = audioUrl,
                caption = caption,
                parseMode = parseMode,
                duration = duration,
                performer = performer,
                title = title,
                replyToMessageId = replyToMessageId
            )
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<kotlinx.serialization.json.JsonElement>>(body)
            if (apiResponse.ok) {
                true
            } else {
                throw IOException(apiResponse.description ?: "sendAudio error")
            }
        }
    }

    /**
     * Uploads and sends a local audio file (.mp3, .m4a, .ogg) to a chat or channel.
     */
    open suspend fun sendAudio(
        token: String,
        chatId: Long,
        file: File,
        title: String? = null,
        performer: String? = null,
        duration: Int? = null,
        caption: String? = null,
        parseMode: String? = null
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/sendAudio"
            val mediaType = "audio/mpeg".toMediaType()
            val fileBody = file.asRequestBody(mediaType)

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId.toString())
                .addFormDataPart("audio", file.name, fileBody)

            if (!title.isNullOrBlank()) multipartBuilder.addFormDataPart("title", title)
            if (!performer.isNullOrBlank()) multipartBuilder.addFormDataPart("performer", performer)
            if (duration != null && duration > 0) multipartBuilder.addFormDataPart("duration", duration.toString())
            if (!caption.isNullOrBlank()) multipartBuilder.addFormDataPart("caption", caption)
            if (!parseMode.isNullOrBlank()) multipartBuilder.addFormDataPart("parse_mode", parseMode)

            val request = Request.Builder()
                .url(url)
                .post(multipartBuilder.build())
                .build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<kotlinx.serialization.json.JsonElement>>(body)
            if (apiResponse.ok) {
                true
            } else {
                throw IOException(apiResponse.description ?: "sendAudio error")
            }
        }
    }

    /**
     * Broadcasts a chat action status (e.g. "typing", "upload_document") to the chat.
     */
    open suspend fun sendChatAction(
        token: String,
        chatId: Long,
        action: String = "typing"
    ): Result<Boolean> = safeTelegramCall {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/sendChatAction"
            val payload = mapOf("chat_id" to chatId.toString(), "action" to action)
            val requestBody = json.encodeToString(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder().url(url).post(requestBody).build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<Boolean>>(body)
            if (apiResponse.ok) {
                apiResponse.result ?: true
            } else {
                throw IOException(apiResponse.description ?: "sendChatAction error")
            }
        }
    }

    companion object {
        private const val BASE_URL = "https://api.telegram.org"
        private const val MAX_TELEGRAM_JSON_BYTES = 5L * 1024 * 1024

        val sharedClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
