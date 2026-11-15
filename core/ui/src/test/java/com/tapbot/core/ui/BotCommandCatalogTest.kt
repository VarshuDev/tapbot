package com.tapbot.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.core.ui.catalog.BotCommandCatalog
import com.tapbot.core.ui.theme.resolveBotVisualIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BotCommandCatalogTest {

    private val all20FlagshipBotIds = listOf(
        "bot_commander",
        "bot_music_streamer",
        "bot_exon",
        "bot_ai_assistant",
        "bot_habit_forge",
        "bot_penny_wise",
        "bot_pomo_focus",
        "bot_tap_note",
        "bot_sky_pulse",
        "bot_remind_me",
        "bot_link_drop",
        "bot_poly_glot",
        "bot_coin_radar",
        "bot_fit_track",
        "bot_parcel_track",
        "bot_rss_notifier",
        "bot_git_notifier",
        "bot_cam_sentry",
        "bot_whisper_box",
        "bot_trivia_master"
    )

    @Test
    fun `returns built-in commands for commander, music, exon, ai, and rss bots`() {
        val commanderCmds = BotCommandCatalog.getCommandsForBot("bot_commander", category = "Automation", name = "TapBot Commander")
        assertTrue(commanderCmds.any { it.command == "/status" })
        assertTrue(commanderCmds.any { it.command.startsWith("/ring") })
        assertTrue(commanderCmds.any { it.command == "/apk" })

        val musicCmds = BotCommandCatalog.getCommandsForBot("bot_music", category = "Media", name = "TapStream Audio")
        assertTrue(musicCmds.any { it.command.startsWith("/play") })
        assertTrue(musicCmds.any { it.command.startsWith("/radio") })

        val exonCmds = BotCommandCatalog.getCommandsForBot("bot_1790084493920", category = "Moderation", name = "Exon Group Guard")
        assertTrue(exonCmds.any { it.command == "/ban" })
        assertTrue(exonCmds.any { it.command.startsWith("/antiflood") })

        val aiCmds = BotCommandCatalog.getCommandsForBot("bot_ai", category = "AI", name = "Gemini Assistant")
        assertTrue(aiCmds.any { it.command.startsWith("/ask") })
        assertTrue(aiCmds.any { it.command.startsWith("/model") })

        val rssCmds = BotCommandCatalog.getCommandsForBot("bot_rss_notifier", category = "News", name = "RSS Notifier")
        assertTrue(rssCmds.any { it.command.startsWith("/addfeed") })
        assertTrue(rssCmds.any { it.command == "/checknow" })
    }

    @Test
    fun `returns built-in commands for all 15 new flagship bots with signature commands`() {
        // Lifestyle bots
        val habitCmds = BotCommandCatalog.getCommandsForBot("bot_habit_forge")
        assertTrue("habit_forge must have /habits", habitCmds.any { it.command == "/habits" })
        assertTrue("habit_forge must have /done", habitCmds.any { it.command.startsWith("/done") })
        assertTrue("habit_forge must have /streak", habitCmds.any { it.command.startsWith("/streak") })

        val remindCmds = BotCommandCatalog.getCommandsForBot("bot_remind_me")
        assertTrue("remind_me must have /remind", remindCmds.any { it.command.startsWith("/remind") })
        assertTrue("remind_me must have /reminders", remindCmds.any { it.command == "/reminders" })
        assertTrue("remind_me must have /water", remindCmds.any { it.command.startsWith("/water") })

        val fitCmds = BotCommandCatalog.getCommandsForBot("bot_fit_track")
        assertTrue("fit_track must have /workout", fitCmds.any { it.command.startsWith("/workout") })
        assertTrue("fit_track must have /water", fitCmds.any { it.command.startsWith("/water") })
        assertTrue("fit_track must have /calories", fitCmds.any { it.command == "/calories" })

        // Finance bots
        val pennyCmds = BotCommandCatalog.getCommandsForBot("bot_penny_wise")
        assertTrue("penny_wise must have /spend", pennyCmds.any { it.command.startsWith("/spend") })
        assertTrue("penny_wise must have /balance", pennyCmds.any { it.command == "/balance" })
        assertTrue("penny_wise must have /budget", pennyCmds.any { it.command.startsWith("/budget") })

        val coinCmds = BotCommandCatalog.getCommandsForBot("bot_coin_radar")
        assertTrue("coin_radar must have /p", coinCmds.any { it.command.startsWith("/p") })
        assertTrue("coin_radar must have /chart", coinCmds.any { it.command.startsWith("/chart") })
        assertTrue("coin_radar must have /portfolio", coinCmds.any { it.command == "/portfolio" })

        // Productivity bots
        val pomoCmds = BotCommandCatalog.getCommandsForBot("bot_pomo_focus")
        assertTrue("pomo_focus must have /pomodoro", pomoCmds.any { it.command.startsWith("/pomodoro") })
        assertTrue("pomo_focus must have /shortbreak", pomoCmds.any { it.command == "/shortbreak" })
        assertTrue("pomo_focus must have /longbreak", pomoCmds.any { it.command == "/longbreak" })

        val noteCmds = BotCommandCatalog.getCommandsForBot("bot_tap_note")
        assertTrue("tap_note must have /note", noteCmds.any { it.command.startsWith("/note") })
        assertTrue("tap_note must have /todo", noteCmds.any { it.command.startsWith("/todo") })
        assertTrue("tap_note must have /todos", noteCmds.any { it.command == "/todos" })

        // Utilities bots
        val skyCmds = BotCommandCatalog.getCommandsForBot("bot_sky_pulse")
        assertTrue("sky_pulse must have /weather", skyCmds.any { it.command.startsWith("/weather") })
        assertTrue("sky_pulse must have /forecast", skyCmds.any { it.command.startsWith("/forecast") })
        assertTrue("sky_pulse must have /radar", skyCmds.any { it.command == "/radar" })

        val linkCmds = BotCommandCatalog.getCommandsForBot("bot_link_drop")
        assertTrue("link_drop must have /save", linkCmds.any { it.command.startsWith("/save") })
        assertTrue("link_drop must have /links", linkCmds.any { it.command.startsWith("/links") })
        assertTrue("link_drop must have /search", linkCmds.any { it.command.startsWith("/search") })

        val polyCmds = BotCommandCatalog.getCommandsForBot("bot_poly_glot")
        assertTrue("poly_glot must have /tr", polyCmds.any { it.command.startsWith("/tr") })
        assertTrue("poly_glot must have /detect", polyCmds.any { it.command.startsWith("/detect") })
        assertTrue("poly_glot must have /languages", polyCmds.any { it.command == "/languages" })

        val parcelCmds = BotCommandCatalog.getCommandsForBot("bot_parcel_track")
        assertTrue("parcel_track must have /track", parcelCmds.any { it.command.startsWith("/track") })
        assertTrue("parcel_track must have /packages", parcelCmds.any { it.command == "/packages" })
        assertTrue("parcel_track must have /status", parcelCmds.any { it.command.startsWith("/status") })

        // Dev, Hardware, Social & Entertainment bots
        val gitCmds = BotCommandCatalog.getCommandsForBot("bot_git_notifier")
        assertTrue("git_notifier must have /watch", gitCmds.any { it.command.startsWith("/watch") })
        assertTrue("git_notifier must have /unwatch", gitCmds.any { it.command.startsWith("/unwatch") })
        assertTrue("git_notifier must have /repos", gitCmds.any { it.command == "/repos" })

        val camCmds = BotCommandCatalog.getCommandsForBot("bot_cam_sentry")
        assertTrue("cam_sentry must have /snap", camCmds.any { it.command.startsWith("/snap") })
        assertTrue("cam_sentry must have /flashsnap", camCmds.any { it.command == "/flashsnap" })
        assertTrue("cam_sentry must have /sentry", camCmds.any { it.command.startsWith("/sentry") })

        val whisperCmds = BotCommandCatalog.getCommandsForBot("bot_whisper_box")
        assertTrue("whisper_box must have /link", whisperCmds.any { it.command == "/link" })
        assertTrue("whisper_box must have /inbox", whisperCmds.any { it.command == "/inbox" })
        assertTrue("whisper_box must have /read", whisperCmds.any { it.command.startsWith("/read") })

        val triviaCmds = BotCommandCatalog.getCommandsForBot("bot_trivia_master")
        assertTrue("trivia_master must have /quiz", triviaCmds.any { it.command == "/quiz" })
        assertTrue("trivia_master must have /category", triviaCmds.any { it.command.startsWith("/category") })
        assertTrue("trivia_master must have /daily", triviaCmds.any { it.command == "/daily" })
    }

    @Test
    fun `every flagship bot returns non-empty actionable command list with valid structure`() {
        assertEquals("Must verify all 20 flagship bots", 20, all20FlagshipBotIds.size)

        for (botId in all20FlagshipBotIds) {
            val cmds = BotCommandCatalog.getCommandsForBot(botId)
            assertTrue("Bot '$botId' must return at least 5 commands, got ${cmds.size}", cmds.size >= 5)

            // Must include /start
            assertTrue("Bot '$botId' must include /start command", cmds.any { it.command.equals("/start", ignoreCase = true) })

            for (entry in cmds) {
                assertTrue("Command '${entry.command}' in '$botId' must start with '/'", entry.command.startsWith("/"))
                assertTrue("Summary for '${entry.command}' in '$botId' must not be blank", entry.summary.isNotBlank())
                assertTrue("Category for '${entry.command}' in '$botId' must not be blank", entry.category.isNotBlank())
            }

            // Must have distinct categories
            val categories = cmds.map { it.category }.distinct()
            assertTrue("Bot '$botId' should categorize commands into at least 2 categories, got $categories", categories.size >= 2)
        }
    }

    @Test
    fun `every flagship bot returns actionable preview commands with stripped arguments`() {
        for (botId in all20FlagshipBotIds) {
            val previews = BotCommandCatalog.getPreviewCommands(botId, limit = 3)
            assertEquals("Bot '$botId' must return 3 preview commands", 3, previews.size)

            assertFalse("Preview commands for '$botId' must not include /start", previews.contains("/start"))
            assertFalse("Preview commands for '$botId' must not include /help", previews.contains("/help"))

            for (p in previews) {
                assertTrue("Preview '$p' for '$botId' must start with '/'", p.startsWith("/"))
                assertFalse("Preview '$p' for '$botId' must not contain spaces", p.contains(" "))
                assertFalse("Preview '$p' for '$botId' must not contain '<' argument syntax", p.contains("<"))
                assertFalse("Preview '$p' for '$botId' must not contain '>' argument syntax", p.contains(">"))
                assertFalse("Preview '$p' for '$botId' must not contain '[' argument syntax", p.contains("["))
                assertFalse("Preview '$p' for '$botId' must not contain ']' argument syntax", p.contains("]"))
                assertTrue("Preview '$p' for '$botId' must have sensible length", p.length in 2..24)
            }
        }
    }

    @Test
    fun `every flagship bot resolves distinct valid visual identity with Play Store metadata`() {
        val seenIcons = mutableMapOf<String, ImageVector>()

        for (botId in all20FlagshipBotIds) {
            val identity = resolveBotVisualIdentity(botId = botId)

            assertNotNull("Identity for '$botId' must not be null", identity)
            assertNotEquals("Accent color for '$botId' must be specified", Color.Unspecified, identity.accentColor)
            assertNotEquals("Secondary color for '$botId' must be specified", Color.Unspecified, identity.secondaryColor)
            assertNotEquals("Surface tint for '$botId' must be specified", Color.Unspecified, identity.surfaceTint)
            assertNotEquals("Border tint for '$botId' must be specified", Color.Unspecified, identity.borderTint)
            assertNotNull("Brush for '$botId' must not be null", identity.brush)

            // Must not use generic SmartToy fallback
            assertNotEquals(
                "Bot '$botId' should have a dedicated icon, not generic fallback SmartToy",
                Icons.Rounded.SmartToy,
                identity.icon
            )

            // Category label
            assertTrue("Category label for '$botId' must not be blank", identity.categoryLabel.isNotBlank())

            // Rating score between 4.7 and 5.0
            val rating = identity.ratingScore.toDoubleOrNull()
            assertNotNull("Rating for '$botId' must be a valid number, got '${identity.ratingScore}'", rating)
            assertTrue("Rating for '$botId' must be in 4.7..5.0, got $rating", rating!! in 4.7..5.0)

            // Review count format (e.g. 1.8k, 890, 4.2k)
            assertTrue(
                "Review count '${identity.reviewCount}' for '$botId' must match expected format",
                identity.reviewCount.matches(Regex("""^(\d+(\.\d+)?k|\d+)$""", RegexOption.IGNORE_CASE))
            )

            // Download count format (e.g. 50K+, 18K+, 5K+)
            assertTrue(
                "Download count '${identity.downloadCount}' for '$botId' must end with K+",
                identity.downloadCount.endsWith("K+") || identity.downloadCount.endsWith("K")
            )

            // Verified developer
            assertTrue("Developer name for '$botId' must not be blank", identity.developerName.isNotBlank())
            assertNotEquals(
                "Bot '$botId' must have dedicated studio name, not generic fallback",
                "TapBot Community",
                identity.developerName
            )
            assertTrue("Flagship bot '$botId' must have isVerifiedDeveloper=true", identity.isVerifiedDeveloper)

            seenIcons[botId] = identity.icon
        }

        // Verify icon variety across the 20 bots (at least 18 unique icons among 20 bots)
        val uniqueIcons = seenIcons.values.distinct().size
        assertTrue("Expected high icon diversity across 20 bots, got $uniqueIcons unique icons", uniqueIcons >= 18)
    }

    @Test
    fun `verifies exact icon assignments for all 20 flagship bots`() {
        val expectedIcons = mapOf(
            "bot_commander" to Icons.Rounded.Terminal,
            "bot_music_streamer" to Icons.Rounded.GraphicEq,
            "bot_exon" to Icons.Rounded.Shield,
            "bot_ai_assistant" to Icons.Rounded.AutoAwesome,
            "bot_habit_forge" to Icons.Rounded.CheckCircle,
            "bot_penny_wise" to Icons.Rounded.AccountBalanceWallet,
            "bot_pomo_focus" to Icons.Rounded.Timer,
            "bot_tap_note" to Icons.Rounded.EditNote,
            "bot_sky_pulse" to Icons.Rounded.WbSunny,
            "bot_remind_me" to Icons.Rounded.NotificationsActive,
            "bot_link_drop" to Icons.Rounded.Bookmark,
            "bot_poly_glot" to Icons.Rounded.Translate,
            "bot_coin_radar" to Icons.Rounded.TrendingUp,
            "bot_fit_track" to Icons.Rounded.FitnessCenter,
            "bot_parcel_track" to Icons.Rounded.LocalShipping,
            "bot_rss_notifier" to Icons.Rounded.RssFeed,
            "bot_git_notifier" to Icons.Rounded.Code,
            "bot_cam_sentry" to Icons.Rounded.Videocam,
            "bot_whisper_box" to Icons.Rounded.Forum,
            "bot_trivia_master" to Icons.Rounded.Psychology
        )

        for ((botId, expectedIcon) in expectedIcons) {
            val identity = resolveBotVisualIdentity(botId = botId)
            assertEquals("Icon mismatch for '$botId'", expectedIcon, identity.icon)
        }
    }

    @Test
    fun `flagship bot aliases resolve to matching commands and visual identity`() {
        // Commander aliases
        val cmd1 = BotCommandCatalog.getCommandsForBot("bot_commander")
        val cmd2 = BotCommandCatalog.getCommandsForBot("tapbot-commander")
        assertEquals(cmd1, cmd2)
        assertEquals(resolveBotVisualIdentity("bot_commander").icon, resolveBotVisualIdentity("tapbot-commander").icon)

        // Music aliases
        val music1 = BotCommandCatalog.getCommandsForBot("bot_music_streamer")
        val music2 = BotCommandCatalog.getCommandsForBot("tapstream-music")
        assertEquals(music1, music2)

        // Exon aliases
        val exon1 = BotCommandCatalog.getCommandsForBot("bot_exon")
        val exon2 = BotCommandCatalog.getCommandsForBot("bot_1790084493920")
        assertEquals(exon1, exon2)

        // AI aliases
        val ai1 = BotCommandCatalog.getCommandsForBot("bot_ai_assistant")
        val ai2 = BotCommandCatalog.getCommandsForBot("bot_ai")
        assertEquals(ai1, ai2)

        // RSS aliases
        val rss1 = BotCommandCatalog.getCommandsForBot("bot_rss_notifier")
        val rss2 = BotCommandCatalog.getCommandsForBot("bot_rss")
        assertEquals(rss1, rss2)
    }

    @Test
    fun `unrecognized bot IDs fallback gracefully to utility commands and community theme`() {
        val unknownCmds = BotCommandCatalog.getCommandsForBot("unknown_bot_999")
        assertTrue(unknownCmds.any { it.command == "/ping" })
        assertTrue(unknownCmds.any { it.command == "/echo <text>" })

        val unknownIdentity = resolveBotVisualIdentity("unknown_bot_999")
        assertEquals(Icons.Rounded.SmartToy, unknownIdentity.icon)
        assertEquals("TapBot Community", unknownIdentity.developerName)
    }

    @Test
    fun `merges dynamic custom rules and deduplicates built-ins`() {
        val def = CustomBotDefinition(
            id = "bot_custom_ops_1",
            name = "Ops Webhook",
            summary = "Webhook runner",
            rules = listOf(
                CommandRule(
                    id = "r1",
                    command = "/deploy",
                    description = "Trigger production webhook",
                    responseTemplate = "Done: {webhook_response}",
                    webhookUrl = "https://example.com/hook",
                    webhookMethod = CustomHttpMethod.POST
                ),
                CommandRule(
                    id = "r2",
                    command = "/start",
                    description = "Custom start override",
                    responseTemplate = "Custom welcome"
                )
            )
        )

        val entries = BotCommandCatalog.getCommandsForBot(def.id, customDefinition = def)
        assertEquals(1, entries.count { it.command.equals("/start", ignoreCase = true) })
        val deployEntry = entries.first { it.command == "/deploy" }
        assertEquals("Webhook", deployEntry.category)
        assertTrue(deployEntry.summary.contains("POST webhook"))
        assertTrue(entries.any { it.command == "/help" })
    }

    @Test
    fun `getPreviewCommands prioritizes actionable commands and strips arg hints`() {
        val previews = BotCommandCatalog.getPreviewCommands("bot_commander", limit = 3)
        assertEquals(3, previews.size)
        assertFalse(previews.contains("/start"))
        assertFalse(previews.contains("/help"))
        assertTrue(previews.all { !it.contains(" ") })
    }

    @Test
    fun `empirical challenge 1 - routing collision resilience across all 20 bots and specific pairs`() {
        // Specific pair 1: bot_git_notifier vs bot_rss_notifier
        val gitCmds = BotCommandCatalog.getCommandsForBot("bot_git_notifier")
        val rssCmds = BotCommandCatalog.getCommandsForBot("bot_rss_notifier")
        assertNotEquals("Git Notifier and RSS Notifier command sets must not collide", gitCmds, rssCmds)
        assertTrue("git_notifier must contain /watch", gitCmds.any { it.command.startsWith("/watch") })
        assertFalse("git_notifier must not contain /feeds", gitCmds.any { it.command.startsWith("/feeds") })
        assertTrue("rss_notifier must contain /feeds", rssCmds.any { it.command.startsWith("/feeds") })
        assertFalse("rss_notifier must not contain /watch", rssCmds.any { it.command.startsWith("/watch") })

        val gitIdentity = resolveBotVisualIdentity("bot_git_notifier")
        val rssIdentity = resolveBotVisualIdentity("bot_rss_notifier")
        assertNotEquals(gitIdentity.icon, rssIdentity.icon)
        assertEquals(Icons.Rounded.Code, gitIdentity.icon)
        assertEquals(Icons.Rounded.RssFeed, rssIdentity.icon)

        // Specific pair 2: bot_cam_sentry vs bot_commander
        val camCmds = BotCommandCatalog.getCommandsForBot("bot_cam_sentry")
        val commanderCmds = BotCommandCatalog.getCommandsForBot("bot_commander")
        assertNotEquals("Cam Sentry and Commander command sets must not collide", camCmds, commanderCmds)
        assertTrue("cam_sentry must contain /snap", camCmds.any { it.command.startsWith("/snap") })
        assertFalse("cam_sentry must not contain /ring", camCmds.any { it.command.startsWith("/ring") })
        assertTrue("commander must contain /ring", commanderCmds.any { it.command.startsWith("/ring") })
        assertFalse("commander must not contain /snap", commanderCmds.any { it.command.startsWith("/snap") })

        val camIdentity = resolveBotVisualIdentity("bot_cam_sentry")
        val commanderIdentity = resolveBotVisualIdentity("bot_commander")
        assertNotEquals(camIdentity.icon, commanderIdentity.icon)
        assertEquals(Icons.Rounded.Videocam, camIdentity.icon)
        assertEquals(Icons.Rounded.Terminal, commanderIdentity.icon)

        // Specific pair 3: bot_exon vs bot_commander
        val exonCmds = BotCommandCatalog.getCommandsForBot("bot_exon")
        assertNotEquals("Exon and Commander command sets must not collide", exonCmds, commanderCmds)
        assertTrue("exon must contain /ban", exonCmds.any { it.command.startsWith("/ban") })
        assertFalse("exon must not contain /battery", exonCmds.any { it.command.startsWith("/battery") })
        assertTrue("commander must contain /battery", commanderCmds.any { it.command.startsWith("/battery") })
        assertFalse("commander must not contain /ban", commanderCmds.any { it.command.startsWith("/ban") })

        val exonIdentity = resolveBotVisualIdentity("bot_exon")
        assertNotEquals(exonIdentity.icon, commanderIdentity.icon)
        assertEquals(Icons.Rounded.Shield, exonIdentity.icon)

        // Specific pair 4: bot_habit_forge vs bot_ai_assistant
        val habitCmds = BotCommandCatalog.getCommandsForBot("bot_habit_forge")
        val aiCmds = BotCommandCatalog.getCommandsForBot("bot_ai_assistant")
        assertNotEquals("HabitForge and AI Assistant command sets must not collide", habitCmds, aiCmds)
        assertTrue("habit_forge must contain /habits", habitCmds.any { it.command.startsWith("/habits") })
        assertFalse("habit_forge must not contain /ask", habitCmds.any { it.command.startsWith("/ask") })
        assertTrue("ai_assistant must contain /ask", aiCmds.any { it.command.startsWith("/ask") })
        assertFalse("ai_assistant must not contain /habits", aiCmds.any { it.command.startsWith("/habits") })

        val habitIdentity = resolveBotVisualIdentity("bot_habit_forge")
        val aiIdentity = resolveBotVisualIdentity("bot_ai_assistant")
        assertNotEquals(habitIdentity.icon, aiIdentity.icon)
        assertEquals(Icons.Rounded.CheckCircle, habitIdentity.icon)
        assertEquals(Icons.Rounded.AutoAwesome, aiIdentity.icon)

        // Exhaustive pairwise inequality across all 20 bots (190 combinations)
        for (i in 0 until all20FlagshipBotIds.size) {
            for (j in i + 1 until all20FlagshipBotIds.size) {
                val idA = all20FlagshipBotIds[i]
                val idB = all20FlagshipBotIds[j]
                val setA = BotCommandCatalog.getCommandsForBot(idA)
                val setB = BotCommandCatalog.getCommandsForBot(idB)
                assertNotEquals("Commands for '$idA' and '$idB' must be distinct", setA, setB)
            }
        }
    }

    @Test
    fun `empirical challenge 2 - preview command stripping across all 20 bots with strict syntax checks`() {
        for (botId in all20FlagshipBotIds) {
            val allCatalogCommands = BotCommandCatalog.getCommandsForBot(botId)
            val fullCatalogVerbs = allCatalogCommands.map { it.command.trim().substringBefore(" ").lowercase() }.toSet()

            for (limit in listOf(3, 5, 10)) {
                val previews = BotCommandCatalog.getPreviewCommands(botId, limit = limit)
                assertTrue("Bot '$botId' must return at least 1 preview command", previews.isNotEmpty())

                for (preview in previews) {
                    assertTrue("Preview '$preview' for '$botId' must start with '/'", preview.startsWith("/"))
                    assertFalse("Preview '$preview' for '$botId' must not contain spaces", preview.contains(" "))
                    assertFalse("Preview '$preview' for '$botId' must not contain angle brackets '<'", preview.contains("<"))
                    assertFalse("Preview '$preview' for '$botId' must not contain angle brackets '>'", preview.contains(">"))
                    assertFalse("Preview '$preview' for '$botId' must not contain square brackets '['", preview.contains("["))
                    assertFalse("Preview '$preview' for '$botId' must not contain square brackets ']'", preview.contains("]"))
                    assertFalse("Preview '$preview' for '$botId' must not be /start", preview.equals("/start", ignoreCase = true))
                    assertFalse("Preview '$preview' for '$botId' must not be /help", preview.equals("/help", ignoreCase = true))

                    assertTrue(
                        "Preview '$preview' for '$botId' must match a catalog command verb in $fullCatalogVerbs",
                        fullCatalogVerbs.contains(preview.lowercase())
                    )
                }
            }
        }
    }

    @Test
    fun `empirical challenge 3 - edge case bot IDs casing hyphens whitespace and fallback to utilityCommands`() {
        // Case variations and hyphen normalization
        val caseVariants = listOf(
            "BOT_COMMANDER" to "bot_commander",
            "bot-commander" to "bot_commander",
            "TapBot-Commander" to "bot_commander",
            "  bot_commander  " to "bot_commander",
            "BOT_GIT_NOTIFIER" to "bot_git_notifier",
            "bot-git-notifier" to "bot_git_notifier",
            "GIT_NOTIFIER" to "bot_git_notifier",
            "BOT_RSS_NOTIFIER" to "bot_rss_notifier",
            "bot-rss-notifier" to "bot_rss_notifier",
            "RSS_NOTIFIER" to "bot_rss_notifier",
            "BOT_CAM_SENTRY" to "bot_cam_sentry",
            "bot-cam-sentry" to "bot_cam_sentry",
            "CAM_SENTRY" to "bot_cam_sentry",
            "BOT_EXON" to "bot_exon",
            "bot-exon" to "bot_exon",
            "BOT_HABIT_FORGE" to "bot_habit_forge",
            "bot-habit-forge" to "bot_habit_forge",
            "HABIT_FORGE" to "bot_habit_forge",
            "BOT_AI_ASSISTANT" to "bot_ai_assistant",
            "bot-ai-assistant" to "bot_ai_assistant",
            "BOT_PENNY_WISE" to "bot_penny_wise",
            "bot-penny-wise" to "bot_penny_wise"
        )

        for ((inputVariant, canonicalId) in caseVariants) {
            val expected = BotCommandCatalog.getCommandsForBot(canonicalId)
            val actual = BotCommandCatalog.getCommandsForBot(inputVariant)
            assertEquals("Variant '$inputVariant' should match '$canonicalId' commands", expected, actual)
        }

        // Unrecognized random bot IDs -> MUST fall back to utilityCommands
        val unrecognizedIds = listOf(
            "unknown_random_bot_12345",
            "completely_fabricated_service_id",
            "tapbot_nonexistent_xyz",
            "random_bot_999",
            "",
            "   ",
            "null",
            "12345678"
        )

        for (unknownId in unrecognizedIds) {
            val cmds = BotCommandCatalog.getCommandsForBot(unknownId)
            assertTrue("Unrecognized '$unknownId' must fall back to /ping", cmds.any { it.command == "/ping" })
            assertTrue("Unrecognized '$unknownId' must fall back to /uptime", cmds.any { it.command == "/uptime" })
            assertTrue("Unrecognized '$unknownId' must fall back to /sysinfo", cmds.any { it.command == "/sysinfo" })
            assertTrue("Unrecognized '$unknownId' must fall back to /crash", cmds.any { it.command == "/crash" })
            assertTrue("Unrecognized '$unknownId' must fall back to /echo <text>", cmds.any { it.command == "/echo <text>" })
            assertEquals("Utility command list size should be 6", 6, cmds.size)

            val identity = resolveBotVisualIdentity(unknownId)
            assertEquals("Unrecognized '$unknownId' visual identity must fall back to SmartToy", Icons.Rounded.SmartToy, identity.icon)
            assertEquals("Unrecognized '$unknownId' must fall back to TapBot Community", "TapBot Community", identity.developerName)
            assertFalse("Unrecognized '$unknownId' should not be verified developer", identity.isVerifiedDeveloper)
        }
    }
}
