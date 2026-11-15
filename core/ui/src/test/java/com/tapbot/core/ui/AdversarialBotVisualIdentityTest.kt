package com.tapbot.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.tapbot.core.ui.theme.ChipLabelStyle
import com.tapbot.core.ui.theme.TabularMetricLarge
import com.tapbot.core.ui.theme.TabularMetricMedium
import com.tapbot.core.ui.theme.TabularMetricSmall
import com.tapbot.core.ui.theme.TabularMonoCode
import com.tapbot.core.ui.theme.resolveBotVisualIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdversarialBotVisualIdentityTest {

    private val flagshipBots = listOf(
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
    fun testAll20FlagshipBotsVisualIdentity() {
        assertEquals("Must test exactly 20 flagship bots", 20, flagshipBots.size)

        val seenIcons = mutableMapOf<String, ImageVector>()

        for (botId in flagshipBots) {
            val identity = resolveBotVisualIdentity(botId = botId)

            // Non-unspecified accent and secondary colors
            assertNotNull("Identity for $botId must not be null", identity)
            assertNotEquals("Accent color for $botId must not be Unspecified", Color.Unspecified, identity.accentColor)
            assertNotEquals("Secondary color for $botId must not be Unspecified", Color.Unspecified, identity.secondaryColor)

            // Valid non-null surfaceTint, borderTint, brush
            assertNotEquals("Surface tint for $botId must not be Unspecified", Color.Unspecified, identity.surfaceTint)
            assertNotEquals("Border tint for $botId must not be Unspecified", Color.Unspecified, identity.borderTint)
            assertNotNull("Brush for $botId must not be null", identity.brush)

            // Zero fallback to Icons.Rounded.SmartToy for flagship bots
            assertNotEquals(
                "Bot $botId must NOT fall back to Icons.Rounded.SmartToy",
                Icons.Rounded.SmartToy,
                identity.icon
            )

            // Rating scores are valid floats strictly between 4.7 and 5.0
            val rating = identity.ratingScore.toFloatOrNull()
            assertNotNull("Rating for $botId must be a valid float", rating)
            assertTrue(
                "Rating $rating for $botId must be in [4.7, 5.0]",
                rating!! in 4.7f..5.0f
            )

            // Review counts follow expected formats (e.g. 1.8k, 890, 4.2k)
            assertTrue(
                "Review count '${identity.reviewCount}' for $botId format check",
                identity.reviewCount.matches(Regex("""^(\d+(\.\d+)?k|\d+)$""", RegexOption.IGNORE_CASE))
            )

            // Download counts follow expected formats (e.g. 10K+, 25K+, 50K+)
            assertTrue(
                "Download count '${identity.downloadCount}' for $botId format check",
                identity.downloadCount.endsWith("K+") || identity.downloadCount.endsWith("K")
            )

            // Verified developer studios (not 'TapBot Community')
            assertNotEquals(
                "Bot $botId studio must not be 'TapBot Community'",
                "TapBot Community",
                identity.developerName
            )
            assertTrue(
                "Bot $botId developerName must be non-blank",
                identity.developerName.isNotBlank()
            )
            assertTrue(
                "Bot $botId isVerifiedDeveloper must be true",
                identity.isVerifiedDeveloper
            )

            seenIcons[botId] = identity.icon
        }

        // Exactly 20 distinct vector icons (no duplicates across the 20 bots)
        val distinctIcons = seenIcons.values.distinct()
        assertEquals(
            "Expected exactly 20 distinct icons across the 20 flagship bots",
            20,
            distinctIcons.size
        )
    }

    @Test
    fun testFallbackResilienceEdgeCases() {
        val edgeCaseInputs = listOf(
            "",
            "   ",
            "unknown_bot_999",
            "super_random_nonexistent_bot",
            "null",
            "undefined",
            "!@#\$%^&*()",
            "🤖✨👾",
            "a".repeat(500),
            "   whitespace_padded_id   ",
            "UNKNOWN_UPPERCASE_ID"
        )

        for (input in edgeCaseInputs) {
            val identity = resolveBotVisualIdentity(botId = input)
            assertNotNull("Fallback identity for '$input' must not be null", identity)
            assertEquals("Fallback icon for '$input' must be SmartToy", Icons.Rounded.SmartToy, identity.icon)
            assertEquals("Fallback developer for '$input' must be TapBot Community", "TapBot Community", identity.developerName)
            assertFalse("Fallback isVerifiedDeveloper for '$input' must be false", identity.isVerifiedDeveloper)
            assertNotEquals("Fallback accentColor must not be Unspecified", Color.Unspecified, identity.accentColor)
            assertNotEquals("Fallback secondaryColor must not be Unspecified", Color.Unspecified, identity.secondaryColor)
            assertNotEquals("Fallback surfaceTint must not be Unspecified", Color.Unspecified, identity.surfaceTint)
            assertNotEquals("Fallback borderTint must not be Unspecified", Color.Unspecified, identity.borderTint)
            assertNotNull("Fallback brush must not be null", identity.brush)
        }
    }

    @Test
    fun testTypographyTokensFontFeatureSettingsAndLetterSpacing() {
        // TabularMetricLarge defines fontFeatureSettings = "tnum"
        assertEquals(
            "TabularMetricLarge fontFeatureSettings must be 'tnum'",
            "tnum",
            TabularMetricLarge.fontFeatureSettings
        )

        // TabularMetricMedium defines fontFeatureSettings = "tnum"
        assertEquals(
            "TabularMetricMedium fontFeatureSettings must be 'tnum'",
            "tnum",
            TabularMetricMedium.fontFeatureSettings
        )

        // TabularMetricSmall defines fontFeatureSettings = "tnum"
        assertEquals(
            "TabularMetricSmall fontFeatureSettings must be 'tnum'",
            "tnum",
            TabularMetricSmall.fontFeatureSettings
        )

        // TabularMonoCode defines fontFeatureSettings = "tnum"
        assertEquals(
            "TabularMonoCode fontFeatureSettings must be 'tnum'",
            "tnum",
            TabularMonoCode.fontFeatureSettings
        )

        // ChipLabelStyle defines letterSpacing = 0.6.sp
        assertEquals(
            "ChipLabelStyle letterSpacing must be 0.6.sp",
            0.6.sp,
            ChipLabelStyle.letterSpacing
        )
    }
}
