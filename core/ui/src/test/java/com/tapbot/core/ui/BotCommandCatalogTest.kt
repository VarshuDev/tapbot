package com.tapbot.core.ui

import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.core.ui.catalog.BotCommandCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BotCommandCatalogTest {

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
}
