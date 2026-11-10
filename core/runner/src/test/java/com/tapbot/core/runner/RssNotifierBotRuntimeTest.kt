package com.tapbot.core.runner

import com.tapbot.core.model.TelegramChat
import com.tapbot.core.model.TelegramMessage
import com.tapbot.core.model.TelegramUpdate
import com.tapbot.core.model.TelegramUser
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.rss.RssFeedParser
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.RssNotifierBotRuntime
import com.tapbot.core.runner.runtime.RuntimeContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RssNotifierBotRuntimeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeTelegramApi : TelegramApiClient() {
        val sentMessages = mutableListOf<Pair<Long, String>>()

        override suspend fun getMe(token: String) = Result.success(
            TelegramUser(id = 800L, isBot = true, firstName = "RSS Bot", username = "TapBotRss")
        )

        override suspend fun getUpdates(token: String, offset: Long?, timeoutSeconds: Int) =
            Result.success(emptyList<TelegramUpdate>())

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?
        ): Result<Boolean> {
            sentMessages.add(chatId to text)
            return Result.success(true)
        }

        override suspend fun sendMessage(
            token: String,
            chatId: Long,
            text: String,
            parseMode: String?,
            replyToMessageId: Long?
        ): Result<Boolean> {
            sentMessages.add(chatId to text)
            return Result.success(true)
        }
    }

    private val sampleRssXmlV1 = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Kotlin Release Feed</title>
            <item>
              <title>Kotlin 2.1 Released</title>
              <link>https://blog.jetbrains.com/kotlin/2.1</link>
              <guid>guid-kt-21</guid>
              <pubDate>Mon, 01 Nov 2026 10:00:00 GMT</pubDate>
              <description>&lt;p&gt;Faster K2 compiler &amp;amp; tooling.&lt;/p&gt;</description>
            </item>
            <item>
              <title>Compose 1.8 Beta</title>
              <link>https://android-developers.googleblog.com/compose-18</link>
              <guid>guid-compose-18</guid>
              <pubDate>Sun, 31 Oct 2026 09:00:00 GMT</pubDate>
              <description>New Material 3 tokens.</description>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private val sampleRssXmlV2 = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Kotlin Release Feed</title>
            <item>
              <title>TapBot 2.3 Ships</title>
              <link>https://tapbot.dev/releases/2.3</link>
              <guid>guid-tapbot-23</guid>
              <pubDate>Tue, 02 Nov 2026 12:00:00 GMT</pubDate>
              <description>Gemini + RSS + Exon 2.0 on-device.</description>
            </item>
            <item>
              <title>Kotlin 2.1 Released</title>
              <link>https://blog.jetbrains.com/kotlin/2.1</link>
              <guid>guid-kt-21</guid>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private val sampleAtomXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>GitHub Releases Atom</title>
          <entry>
            <id>tag:github.com,2008:Repository/tapbot/v2.3.0</id>
            <title>Release v2.3.0</title>
            <link rel="alternate" type="text/html" href="https://github.com/tapbot/tapbot/releases/v2.3.0"/>
            <updated>2026-11-10T10:00:00Z</updated>
            <summary>Multi-phase TapBot update.</summary>
          </entry>
        </feed>
    """.trimIndent()

    @Test
    fun `factory creates RssNotifierBotRuntime for bot_rss_notifier`() {
        assertTrue(BotRuntimeFactory.createRuntime("bot_rss_notifier") is RssNotifierBotRuntime)
        assertTrue(BotRuntimeFactory.createRuntime("custom_feed", "RSS Channel Broadcaster") is RssNotifierBotRuntime)
    }

    @Test
    fun `parser parses both RSS 2_0 and Atom feeds cleanly`() {
        val rss = RssFeedParser.parse(sampleRssXmlV1).getOrThrow()
        assertEquals("Kotlin Release Feed", rss.title)
        assertEquals(2, rss.items.size)
        assertEquals("guid-kt-21", rss.items[0].id)
        assertEquals("Kotlin 2.1 Released", rss.items[0].title)
        assertEquals("Faster K2 compiler & tooling.", rss.items[0].summary)

        val atom = RssFeedParser.parse(sampleAtomXml).getOrThrow()
        assertEquals("GitHub Releases Atom", atom.title)
        assertEquals(1, atom.items.size)
        assertEquals("Release v2.3.0", atom.items[0].title)
        assertEquals("https://github.com/tapbot/tapbot/releases/v2.3.0", atom.items[0].link)
    }

    @Test
    fun `deduplicates seen GUIDs across restarts and manages feeds via commands`() = runBlocking {
        val stateFile = tempFolder.newFile("rss_state.json")
        stateFile.delete()
        var currentXml = sampleRssXmlV1
        val fakeTg = FakeTelegramApi()
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val ctx = RuntimeContext(
            botId = "bot_rss_notifier",
            token = "rss-token",
            telegramApi = fakeTg,
            log = { _, _, _ -> },
            scope = scope,
            credentials = mapOf(
                "target_chat_id" to "-100999888",
                "feed_url" to "https://mock.feed/rss.xml",
                "poll_interval_minutes" to "10"
            )
        )

        val runtime1 = RssNotifierBotRuntime(
            botId = "bot_rss_notifier",
            feedFetcher = { Result.success(currentXml) },
            storageFile = stateFile
        )
        runtime1.initialize(ctx)

        // First check broadcasts both initial items to target channel
        val stats1 = runtime1.checkAllFeeds(ctx, "rss-token")
        assertEquals(1, stats1.feedsChecked)
        assertEquals(2, stats1.newItemsSent)
        assertEquals(2, fakeTg.sentMessages.size)
        assertTrue(fakeTg.sentMessages.all { it.first == -100999888L })

        // Re-create runtime simulating device restart reading same disk file
        fakeTg.sentMessages.clear()
        val runtime2 = RssNotifierBotRuntime(
            botId = "bot_rss_notifier",
            feedFetcher = { Result.success(currentXml) },
            storageFile = stateFile
        )
        runtime2.initialize(ctx)

        // Poll again with unchanged XML -> zero duplicates
        val stats2 = runtime2.checkAllFeeds(ctx, "rss-token")
        assertEquals(0, stats2.newItemsSent)
        assertEquals(0, fakeTg.sentMessages.size)

        // Now feed gains one new item while keeping an older item
        currentXml = sampleRssXmlV2
        val checkCmd = TelegramMessage(
            messageId = 50,
            from = TelegramUser(1, false, "Admin"),
            chat = TelegramChat(777L, "private"),
            date = 2000L,
            text = "/checknow"
        )
        runtime2.handleMessage(ctx, "rss-token", checkCmd)

        val channelBroadcasts = fakeTg.sentMessages.filter { it.first == -100999888L }
        assertEquals(1, channelBroadcasts.size)
        assertTrue(channelBroadcasts.single().second.contains("TapBot 2.3 Ships"))

        val replyToAdmin = fakeTg.sentMessages.last { it.first == 777L }.second
        assertTrue(replyToAdmin.contains("1 new item"))

        // Interactive /addfeed and /feeds
        runtime2.handleMessage(
            ctx,
            "rss-token",
            TelegramMessage(51, TelegramUser(1, false, "Admin"), TelegramChat(777L, "private"), 2001L, "/addfeed https://example.com/atom.xml")
        )
        val feedsOut = runtime2.handleMessage(
            ctx,
            "rss-token",
            TelegramMessage(52, TelegramUser(1, false, "Admin"), TelegramChat(777L, "private"), 2002L, "/feeds")
        )
        assertTrue(feedsOut!!.contains("https://example.com/atom.xml"))
    }
}
