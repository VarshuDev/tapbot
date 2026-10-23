package com.tapbot.core.network

import com.tapbot.core.model.SendMessagePayload
import com.tapbot.core.model.TelegramApiResponse
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
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

    /**
     * Verifies the bot token and returns information about the bot.
     */
    open suspend fun getMe(token: String): Result<TelegramUser> = runCatching {
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
    ): Result<List<TelegramUpdate>> = runCatching {
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
    ): Result<Boolean> = runCatching {
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
        return runCatching {
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
     * Bans a user from a chat group.
     */
    open suspend fun banChatMember(
        token: String,
        chatId: Long,
        userId: Long
    ): Result<Boolean> = runCatching {
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
     * Unbans a previously banned user.
     */
    open suspend fun unbanChatMember(
        token: String,
        chatId: Long,
        userId: Long
    ): Result<Boolean> = runCatching {
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
    ): Result<Boolean> = runCatching {
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
     * Pins a message in a group.
     */
    open suspend fun pinChatMessage(
        token: String,
        chatId: Long,
        messageId: Long
    ): Result<Boolean> = runCatching {
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
    ): Result<Boolean> = runCatching {
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
    ): Result<Boolean> = runCatching {
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
    ): Result<String> = runCatching {
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
    ): Result<List<com.tapbot.core.model.TelegramChatMember>> = runCatching {
        withContext(Dispatchers.IO) {
            val url = "$BASE_URL/bot$token/getChatAdministrators?chat_id=$chatId"
            val request = Request.Builder().url(url).get().build()

            val body = client.newCall(request).execute().use { response ->
                response.body?.string().orEmpty()
            }
            val apiResponse = json.decodeFromString<TelegramApiResponse<List<com.tapbot.core.model.TelegramChatMember>>>(body)
            if (apiResponse.ok) {
                apiResponse.result.orEmpty()
            } else {
                throw IOException(apiResponse.description ?: "getChatAdministrators error")
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
    ): Result<Boolean> = runCatching {
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
     * Broadcasts a chat action status (e.g. "typing", "upload_document") to the chat.
     */
    open suspend fun sendChatAction(
        token: String,
        chatId: Long,
        action: String = "typing"
    ): Result<Boolean> = runCatching {
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
