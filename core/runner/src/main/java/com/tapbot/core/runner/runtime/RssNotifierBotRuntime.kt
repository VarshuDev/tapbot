package com.tapbot.core.runner.runtime

import com.tapbot.core.model.LogLevel
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.runner.rss.RssFeedParser
import com.tapbot.core.runner.rss.RssItem
import com.tapbot.core.runner.util.AdaptiveBackoff
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class RssPersistedState(
    val feeds: List<String> = emptyList(),
    val targetChatId: Long? = null,
    val pollIntervalMinutes: Int = 15,
    val seenIds: Set<String> = emptySet()
)

/**
 * On-device runtime for RSS Channel Broadcaster (`bot_rss_notifier`).
 * Polls RSS 2.0 and Atom feeds on Android, deduplicates seen article GUIDs/links on disk,
 * broadcasts new items to a target Telegram chat/channel, and supports interactive management commands.
 */
class RssNotifierBotRuntime(
    override val botId: String = "bot_rss_notifier",
    private val feedFetcher: suspend (String) -> Result<String> = defaultFeedFetcher(),
    private val storageFile: File? = null
) : BotRuntime {

    private val _state = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
    override val state: StateFlow<BotRuntimeState> = _state.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val stateMutex = Mutex()
    private var persistedFile: File? = storageFile
    private var feeds = mutableListOf<String>()
    private var seenIds = LinkedHashSet<String>()
    private var targetChatId: Long? = null
    private var pollIntervalMinutes: Int = 15

    private var context: RuntimeContext? = null
    private var pollJob: Job? = null
    private var feedPollJob: Job? = null
    private var lastUpdateId: Long? = null
    private var startedAtTimestamp: Long = 0L

    override suspend fun initialize(context: RuntimeContext) {
        this.context = context
        if (persistedFile == null) {
            val baseDir = context.androidContext?.filesDir
                ?: File(System.getProperty("java.io.tmpdir") ?: ".", "tapbot_rss")
            baseDir.mkdirs()
            val safeId = botId.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            persistedFile = File(baseDir, "rss_state_$safeId.json")
        }

        stateMutex.withLock {
            loadStateFromDiskLocked()

            val credTarget = context.credentials["target_chat_id"]?.trim()?.toLongOrNull()
            if (credTarget != null) {
                targetChatId = credTarget
            }

            val credInterval = context.credentials["poll_interval_minutes"]?.trim()?.toIntOrNull()
            if (credInterval != null) {
                pollIntervalMinutes = credInterval.coerceIn(1, 1440)
            }

            val credFeed = context.credentials["feed_url"]?.trim().orEmpty()
            if (credFeed.isNotBlank()) {
                val candidateUrls = credFeed.split(',', '\n', ';')
                    .map { it.trim() }
                    .filter { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
                for (u in candidateUrls) {
                    if (u !in feeds) {
                        feeds.add(u)
                    }
                }
            }
            saveStateToDiskLocked()
        }
    }

    override suspend fun start() {
        val ctx = context ?: throw IllegalStateException("Runtime not initialized")
        val token = ctx.token

        if (token.isBlank()) {
            val err = "Cannot start RSS Broadcaster: Empty Telegram Bot Token."
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        _state.value = BotRuntimeState.Starting
        startedAtTimestamp = System.currentTimeMillis()
        ctx.log(LogLevel.INFO, TAG, "Starting RSS Channel Broadcaster (feeds=${feeds.size}, interval=${pollIntervalMinutes}m)...")

        val meResult = ctx.telegramApi.getMe(token)
        if (meResult.isFailure) {
            val err = "Telegram authentication failed: ${meResult.exceptionOrNull()?.message}"
            ctx.log(LogLevel.ERROR, TAG, err)
            _state.value = BotRuntimeState.Error(err)
            return
        }

        val botUser = meResult.getOrThrow()
        val username = botUser.username ?: "RssBroadcasterBot"
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
                        backoff.recordFailure()
                        backoff.delayWithBackoff(ctx.networkState)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ctx.log(LogLevel.WARN, TAG, "RSS command polling error: ${e.message}")
                    backoff.recordFailure()
                    backoff.delayWithBackoff(ctx.networkState)
                }
            }
        }

        feedPollJob = ctx.scope.launch {
            while (isActive) {
                try {
                    if (ctx.isNetworkAvailable() && feeds.isNotEmpty() && targetChatId != null) {
                        checkAllFeeds(ctx, token, fallbackChatId = targetChatId)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ctx.log(LogLevel.WARN, TAG, "Scheduled feed check error: ${e.message}")
                }
                delay(pollIntervalMinutes.coerceIn(1, 1440) * 60_000L)
            }
        }
    }

    suspend fun handleMessage(
        ctx: RuntimeContext,
        token: String,
        message: TelegramMessage,
        botUsername: String = "RssBroadcasterBot"
    ): String? {
        val rawText = message.text?.trim().orEmpty()
        if (!rawText.startsWith("/")) return null

        val parts = rawText.split("\\s+".toRegex(), limit = 2)
        val cmdToken = parts[0]
        if (cmdToken.contains("@")) {
            val target = cmdToken.substringAfter("@")
            if (!target.equals(botUsername, ignoreCase = true)) return null
        }

        val cmd = cmdToken.removePrefix("/").substringBefore("@").lowercase(Locale.ROOT)
        val arg = parts.getOrNull(1)?.trim().orEmpty()
        val chatId = message.chat.id

        val reply = when (cmd) {
            "start", "help" -> formatHelp()
            "feeds", "list" -> formatFeedsList()
            "addfeed" -> handleAddFeed(arg)
            "removefeed", "delfeed" -> handleRemoveFeed(arg)
            "checknow", "poll" -> {
                val stats = checkAllFeeds(ctx, token, fallbackChatId = chatId)
                "📰 Feed scan complete: ${stats.feedsChecked} feed(s) checked, ${stats.newItemsSent} new item(s) published."
            }
            "interval" -> handleIntervalCommand(arg)
            "settarget" -> handleSetTargetCommand(arg, chatId)
            "status" -> formatStatus()
            else -> "Unrecognized command `/$cmd`. Send /help to see RSS Broadcaster commands."
        }

        ctx.telegramApi.sendMessage(token, chatId, reply, null, message.messageId)
        ctx.log(LogLevel.INFO, TAG, "Handled RSS command '/$cmd'")
        return reply
    }

    data class FeedCheckStats(
        val feedsChecked: Int,
        val newItemsSent: Int,
        val errors: List<String> = emptyList()
    )

    suspend fun checkAllFeeds(
        ctx: RuntimeContext,
        token: String,
        fallbackChatId: Long? = null
    ): FeedCheckStats {
        val snapshotFeeds: List<String>
        val destinationChat: Long?
        stateMutex.withLock {
            snapshotFeeds = feeds.toList()
            destinationChat = targetChatId ?: fallbackChatId
        }

        if (snapshotFeeds.isEmpty() || destinationChat == null) {
            return FeedCheckStats(0, 0)
        }

        var sentCount = 0
        val errorList = mutableListOf<String>()

        for (feedUrl in snapshotFeeds) {
            val xmlRes = feedFetcher(feedUrl)
            if (xmlRes.isFailure) {
                val errMsg = "Failed fetching $feedUrl: ${xmlRes.exceptionOrNull()?.message}"
                errorList.add(errMsg)
                ctx.log(LogLevel.WARN, TAG, errMsg)
                continue
            }

            val feedRes = RssFeedParser.parse(xmlRes.getOrThrow())
            if (feedRes.isFailure) {
                val errMsg = "Failed parsing $feedUrl: ${feedRes.exceptionOrNull()?.message}"
                errorList.add(errMsg)
                ctx.log(LogLevel.WARN, TAG, errMsg)
                continue
            }

            val feed = feedRes.getOrThrow()
            val unseenItems = mutableListOf<RssItem>()
            stateMutex.withLock {
                for (item in feed.items) {
                    if (!seenIds.contains(item.id)) {
                        unseenItems.add(item)
                    }
                }
                // Mark every fetched item as seen before broadcasting so network restarts never duplicate
                for (item in feed.items) {
                    seenIds.add(item.id)
                }
                while (seenIds.size > MAX_SEEN_IDS) {
                    val first = seenIds.firstOrNull() ?: break
                    seenIds.remove(first)
                }
                saveStateToDiskLocked()
            }

            // Broadcast up to MAX_ITEMS_PER_CHECK items per scan cycle in oldest->newest order
            val toPublish = unseenItems.take(MAX_ITEMS_PER_CHECK).reversed()
            for (item in toPublish) {
                val postText = formatArticlePost(feed.title, item)
                val sendRes = ctx.telegramApi.sendMessage(token, destinationChat, postText, null, null)
                if (sendRes.isSuccess) {
                    sentCount++
                }
            }
        }

        return FeedCheckStats(
            feedsChecked = snapshotFeeds.size,
            newItemsSent = sentCount,
            errors = errorList
        )
    }

    private suspend fun handleAddFeed(urlArg: String): String {
        val url = urlArg.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            return "Usage: /addfeed https://example.com/feed.xml"
        }
        return stateMutex.withLock {
            if (feeds.contains(url)) {
                "Feed is already subscribed: $url"
            } else {
                feeds.add(url)
                saveStateToDiskLocked()
                "✅ Subscribed to RSS/Atom feed:\n$url"
            }
        }
    }

    private suspend fun handleRemoveFeed(arg: String): String {
        if (arg.isBlank()) {
            return "Usage: /removefeed <url or index>\nView indices via /feeds"
        }
        return stateMutex.withLock {
            val index = arg.toIntOrNull()
            val removed = when {
                index != null && index in 1..feeds.size -> feeds.removeAt(index - 1)
                feeds.remove(arg.trim()) -> arg.trim()
                else -> null
            }
            if (removed != null) {
                saveStateToDiskLocked()
                "🗑️ Removed feed:\n$removed"
            } else {
                "Feed not found: $arg"
            }
        }
    }

    private suspend fun handleIntervalCommand(arg: String): String {
        if (arg.isBlank()) {
            return "⏱ Current poll interval: $pollIntervalMinutes minute(s).\nUpdate with: /interval <minutes>"
        }
        val mins = arg.toIntOrNull()?.coerceIn(1, 1440)
            ?: return "Invalid interval. Enter a number of minutes between 1 and 1440."
        stateMutex.withLock {
            pollIntervalMinutes = mins
            saveStateToDiskLocked()
        }
        return "⏱ Updated RSS polling interval to $mins minute(s)."
    }

    private suspend fun handleSetTargetCommand(arg: String, currentChatId: Long): String {
        val target = if (arg.isBlank()) currentChatId else arg.toLongOrNull()
            ?: return "Usage: /settarget <chat_or_channel_id> (e.g. -1001234567890)"
        stateMutex.withLock {
            targetChatId = target
            saveStateToDiskLocked()
        }
        return "📢 Target broadcast chat/channel set to: $target"
    }

    private fun formatHelp(): String {
        return """
            📰 TapBot RSS / Atom Channel Broadcaster

            Commands:
            • /feeds — List subscribed feeds
            • /addfeed <url> — Subscribe to an RSS or Atom feed
            • /removefeed <url|#> — Unsubscribe from a feed
            • /checknow — Poll all feeds immediately
            • /interval [minutes] — Set poll frequency (default 15m)
            • /settarget [chat_id] — Set destination channel/chat ID
            • /status — View feed health & deduplication stats
        """.trimIndent()
    }

    private fun formatFeedsList(): String {
        if (feeds.isEmpty()) {
            return "No RSS/Atom feeds configured yet.\nAdd one using: /addfeed https://news.ycombinator.com/rss"
        }
        val sb = StringBuilder("📡 Subscribed Feeds (${feeds.size}):\n\n")
        feeds.forEachIndexed { idx, url ->
            sb.append("${idx + 1}. $url\n")
        }
        sb.append("\nTarget Chat ID: ${targetChatId ?: "Not set (use /settarget)"}")
        return sb.toString()
    }

    private fun formatStatus(): String {
        val uptimeSec = if (startedAtTimestamp > 0L) (System.currentTimeMillis() - startedAtTimestamp) / 1000L else 0L
        return """
            📰 RSS Broadcaster Status
            • Active Feeds: ${feeds.size}
            • Target Chat ID: ${targetChatId ?: "Unset"}
            • Poll Interval: ${pollIntervalMinutes}m
            • Seen GUIDs Tracked: ${seenIds.size}
            • Uptime: ${uptimeSec}s
        """.trimIndent()
    }

    private fun formatArticlePost(feedTitle: String, item: RssItem): String {
        return buildString {
            append("📰 ").append(item.title).append("\n")
            if (!item.summary.isNullOrBlank()) {
                append("\n").append(item.summary).append("\n")
            }
            if (item.link.isNotBlank()) {
                append("\n🔗 ").append(item.link)
            }
            if (!item.publishedAt.isNullOrBlank()) {
                append("\n🕒 ").append(item.publishedAt)
            }
            append("\n📡 Source: ").append(feedTitle)
        }
    }

    private fun loadStateFromDiskLocked() {
        val f = persistedFile ?: return
        if (!f.exists() || f.length() == 0L) return
        try {
            val loaded = json.decodeFromString<RssPersistedState>(f.readText())
            feeds = loaded.feeds.toMutableList()
            targetChatId = loaded.targetChatId
            pollIntervalMinutes = loaded.pollIntervalMinutes.coerceIn(1, 1440)
            seenIds = LinkedHashSet(loaded.seenIds)
        } catch (_: Exception) {
            // Keep defaults if corrupt
        }
    }

    private fun saveStateToDiskLocked() {
        val f = persistedFile ?: return
        try {
            f.parentFile?.mkdirs()
            val payload = RssPersistedState(
                feeds = feeds.toList(),
                targetChatId = targetChatId,
                pollIntervalMinutes = pollIntervalMinutes,
                seenIds = seenIds.toSet()
            )
            f.writeText(json.encodeToString(payload))
        } catch (_: Exception) {
            // Ignore persistence write error
        }
    }

    override suspend fun stop() {
        feedPollJob?.cancel()
        pollJob?.cancel()
        feedPollJob = null
        pollJob = null
        _state.value = BotRuntimeState.Stopped
    }

    companion object {
        private const val TAG = "RssNotifierRuntime"
        private const val MAX_SEEN_IDS = 1500
        private const val MAX_ITEMS_PER_CHECK = 5

        fun defaultFeedFetcher(
            client: OkHttpClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        ): suspend (String) -> Result<String> = { feedUrl ->
            runCatching {
                withContext(Dispatchers.IO) {
                    val req = Request.Builder()
                        .url(feedUrl)
                        .header("User-Agent", "TapBot-RSS/2.3")
                        .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                        .get()
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            throw IOException("HTTP ${resp.code} fetching feed")
                        }
                        resp.body?.string().orEmpty()
                    }
                }
            }
        }
    }
}
