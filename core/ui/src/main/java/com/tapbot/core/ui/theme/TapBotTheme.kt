package com.tapbot.core.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountTree
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
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TapBot Hardware Node Console Design Tokens.
 *
 * Visual Direction: Deep Obsidian Console grounded in the app's cyan-cobalt robot visor identity
 * (ic_launcher_foreground.xml), structured with calibrated WCAG AA+ surface separation,
 * semantic state pairs (color + icon + label), and tabular numeral support for live node telemetry.
 */
object TapBotColors {
    // Layered Canvas & Console Surfaces
    val Canvas = Color(0xFF060A11)
    val SurfacePrimary = Color(0xFF0C121D)
    val SurfaceElevated = Color(0xFF121B2B)
    val SurfaceInteractive = Color(0xFF192438)

    // Backwards-compatible aliases
    val Background = Canvas
    val PrimarySurface = SurfacePrimary
    val ElevatedSurface = SurfaceElevated
    val SecondarySurface = SurfaceInteractive

    // Brand Identity & Interactive Actions
    val BrandCyan = Color(0xFF00E5FF)
    val BrandCyanInk = Color(0xFF04131A)
    val BrandCyanSurface = Color(0xFF072733)
    val BrandCyanBorder = Color(0xFF0E4D64)

    val PrimaryCobalt = Color(0xFF2563EB)
    val PrimaryBlue = Color(0xFF3B82F6)
    val PrimaryBlueDark = Color(0xFF1D4ED8)
    val PrimaryPressed = Color(0xFF1D4ED8)
    val BluePressed = PrimaryPressed
    val BlueSubtle = Color(0xFF152847)
    val BlueBorder = Color(0xFF284676)

    // Content & Typography Roles (all verified >= 4.5:1 against SurfaceElevated / SurfacePrimary)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF8A9BB3)

    val PrimaryText = TextPrimary
    val SecondaryText = TextSecondary
    val MutedText = TextMuted

    // Hairline Borders & Focus Rings
    val Border = Color(0xFF1E2B42)
    val BorderSubtle = Color(0xFF142033)
    val BorderFocus = Color(0xFF2E476E)
    val BorderHighlight = BorderFocus

    // Semantic Status Pairs
    val Success = Color(0xFF10B981)
    val SuccessText = Color(0xFF34D399)
    val SuccessSurface = Color(0xFF06281E)
    val SuccessBorder = Color(0xFF0F5640)

    val Warning = Color(0xFFF59E0B)
    val WarningText = Color(0xFFFBBF24)
    val WarningSurface = Color(0xFF2E1F0A)
    val WarningBorder = Color(0xFF6B4512)

    val Error = Color(0xFFEF4444)
    val ErrorText = Color(0xFFF87171)
    val ErrorSurface = Color(0xFF2E0D15)
    val ErrorBorder = Color(0xFF7F1D1D)

    val Info = Color(0xFF38BDF8)
    val InfoSurface = Color(0xFF092338)
    val InfoBorder = Color(0xFF164E78)

    // Category Accents
    val AccentCommander = Color(0xFF00B8D4)
    val AccentMusic = Color(0xFF8B5CF6)
    val AccentAI = Color(0xFF6366F1)
    val AccentModeration = Color(0xFFF59E0B)
    val AccentRss = Color(0xFFF97316)
    val AccentCustom = Color(0xFF14B8A6)
    val AccentUtility = Color(0xFF38BDF8)

    // Dedicated Flagship Domain Accents
    val AccentHabit = Color(0xFF10B981)
    val AccentFinance = Color(0xFFEAB308)
    val AccentFocus = Color(0xFFF43F5E)
    val AccentNote = Color(0xFFF59E0B)
    val AccentWeather = Color(0xFF0284C7)
    val AccentHealth = Color(0xFF14B8A6)
    val AccentVault = Color(0xFF2563EB)
    val AccentTranslate = Color(0xFFA855F7)
    val AccentCrypto = Color(0xFF84CC16)
    val AccentFitness = Color(0xFFFF6B00)
    val AccentShipping = Color(0xFFD97706)
    val AccentDeveloper = Color(0xFF64748B)
    val AccentSentry = Color(0xFFDC2626)
    val AccentSocial = Color(0xFFE11D48)
    val AccentTrivia = Color(0xFFFBBF24)

    // Google Play Store Specific Surfaces & Accents
    val PlayBackground = Color(0xFF0E1217)
    val PlaySurface = Color(0xFF161B22)
    val PlaySurfaceElevated = Color(0xFF212833)
    val PlaySurfaceHighlight = Color(0xFF2B3442)
    val PlayBorder = Color(0xFF2B3545)
    val PlayBorderSubtle = Color(0xFF1D2430)
    
    val PlayInstallGreen = Color(0xFF00A86B)
    val PlayInstallGreenPressed = Color(0xFF00875A)
    val PlayInstallGreenBg = Color(0xFF062B1E)
    val PlayInstallGreenBorder = Color(0xFF0F5A3E)
    
    val PlayStarYellow = Color(0xFFFFB800)
    val PlayVerifiedBlue = Color(0xFF38BDF8)
    val PlayChipActive = Color(0xFF2563EB)

    // Spacing & Radii Scale
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 24.dp

    val RadiusXs = 6.dp
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusXl = 20.dp
    val RadiusPill = 50.dp
}

typealias BotHubTheme = TapBotColors

data class BotVisualIdentity(
    val accentColor: Color,
    val secondaryColor: Color,
    val surfaceTint: Color,
    val borderTint: Color,
    val brush: Brush,
    val icon: ImageVector,
    val categoryLabel: String,
    val ratingScore: String = "4.9",
    val reviewCount: String = "1.2k",
    val downloadCount: String = "10K+",
    val developerName: String = "TapBot Curated",
    val isVerifiedDeveloper: Boolean = true
)

/**
 * Resolves coherent color signature and iconography per bot runtime identity.
 * Maps all 20 daily-usage flagship bots using exact bot IDs and aliases first,
 * with dedicated primary/secondary colors, surface/border tints, gradient brushes,
 * unique Material Rounded vector icons, Play Store ratings (4.7–5.0), reviews,
 * download counts, and verified developer studios.
 */
fun resolveBotVisualIdentity(
    botId: String = "",
    category: String = "",
    name: String = ""
): BotVisualIdentity {
    val cleanId = botId.trim().lowercase().replace("-", "_")
    val lowerCat = category.lowercase().trim()
    val lowerName = name.lowercase().trim()
    val combined = "$cleanId $lowerCat $lowerName"

    return when {
        // 1. HabitForge Daily Tracker
        cleanId == "bot_habit_forge" || cleanId == "habit_forge" || cleanId == "habitforge" || combined.contains("habitforge") || combined.contains("habit_forge") || lowerName.contains("habit forge") -> {
            val primary = TapBotColors.AccentHabit
            val secondary = Color(0xFF059669)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF06281E),
                borderTint = Color(0xFF0F5640),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.CheckCircle,
                categoryLabel = "Habit Tracker",
                ratingScore = "4.9",
                reviewCount = "1.8k",
                downloadCount = "12K+",
                developerName = "Forge Habit Labs",
                isVerifiedDeveloper = true
            )
        }
        // 2. PennyWise Expense Tracker
        cleanId == "bot_penny_wise" || cleanId == "penny_wise" || cleanId == "pennywise" || combined.contains("pennywise") || combined.contains("penny_wise") || lowerName.contains("penny wise") -> {
            val primary = TapBotColors.AccentFinance
            val secondary = Color(0xFF10B981)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF2A2007),
                borderTint = Color(0xFF664F0F),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.AccountBalanceWallet,
                categoryLabel = "Expense Ledger",
                ratingScore = "4.9",
                reviewCount = "2.3k",
                downloadCount = "18K+",
                developerName = "Wise Wallet Studio",
                isVerifiedDeveloper = true
            )
        }
        // 3. PomoFocus Deep Work Timer
        cleanId == "bot_pomo_focus" || cleanId == "pomo_focus" || cleanId == "pomofocus" || combined.contains("pomofocus") || combined.contains("pomo_focus") || lowerName.contains("pomodoro") -> {
            val primary = TapBotColors.AccentFocus
            val secondary = Color(0xFFBE123C)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF2B0A13),
                borderTint = Color(0xFF6E1B32),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Timer,
                categoryLabel = "Focus Timer",
                ratingScore = "4.8",
                reviewCount = "1.4k",
                downloadCount = "10K+",
                developerName = "Pomo Sprint Labs",
                isVerifiedDeveloper = true
            )
        }
        // 4. TapNote Scratchpad
        cleanId == "bot_tap_note" || cleanId == "tap_note" || cleanId == "tapnote" || combined.contains("tapnote") || combined.contains("tap_note") || lowerName.contains("scratchpad") -> {
            val primary = TapBotColors.AccentNote
            val secondary = Color(0xFFD97706)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF2B1D08),
                borderTint = Color(0xFF6B4712),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.EditNote,
                categoryLabel = "Scratchpad",
                ratingScore = "4.9",
                reviewCount = "3.1k",
                downloadCount = "22K+",
                developerName = "TapPaper Studio",
                isVerifiedDeveloper = true
            )
        }
        // 5. SkyPulse Weather Radar
        cleanId == "bot_sky_pulse" || cleanId == "sky_pulse" || cleanId == "skypulse" || combined.contains("skypulse") || combined.contains("sky_pulse") || lowerName.contains("weather radar") -> {
            val primary = TapBotColors.AccentWeather
            val secondary = Color(0xFF38BDF8)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF082030),
                borderTint = Color(0xFF134E70),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.WbSunny,
                categoryLabel = "Weather Radar",
                ratingScore = "4.8",
                reviewCount = "2.7k",
                downloadCount = "20K+",
                developerName = "AeroCast Weather",
                isVerifiedDeveloper = true
            )
        }
        // 6. RemindMe Health & Pill Alarms
        cleanId == "bot_remind_me" || cleanId == "remind_me" || cleanId == "remindme" || combined.contains("remindme") || combined.contains("remind_me") || lowerName.contains("pill alarm") -> {
            val primary = TapBotColors.AccentHealth
            val secondary = Color(0xFF0D9488)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF082622),
                borderTint = Color(0xFF125C54),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.NotificationsActive,
                categoryLabel = "Health & Alarms",
                ratingScore = "4.9",
                reviewCount = "1.9k",
                downloadCount = "14K+",
                developerName = "Pulse Health Labs",
                isVerifiedDeveloper = true
            )
        }
        // 7. LinkDrop Link & Clipboard Vault
        cleanId == "bot_link_drop" || cleanId == "link_drop" || cleanId == "linkdrop" || combined.contains("linkdrop") || combined.contains("link_drop") || lowerName.contains("clipboard vault") -> {
            val primary = TapBotColors.AccentVault
            val secondary = Color(0xFF1D4ED8)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF0E1A33),
                borderTint = Color(0xFF1F3D78),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Bookmark,
                categoryLabel = "Clipboard Vault",
                ratingScore = "4.8",
                reviewCount = "1.1k",
                downloadCount = "8K+",
                developerName = "CloudVault Tools",
                isVerifiedDeveloper = true
            )
        }
        // 8. PolyGlot Instant Translator
        cleanId == "bot_poly_glot" || cleanId == "poly_glot" || cleanId == "polyglot" || combined.contains("polyglot") || combined.contains("poly_glot") || lowerName.contains("translator") -> {
            val primary = TapBotColors.AccentTranslate
            val secondary = Color(0xFFD946EF)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF241038),
                borderTint = Color(0xFF5A288C),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Translate,
                categoryLabel = "Translator",
                ratingScore = "4.9",
                reviewCount = "4.2k",
                downloadCount = "30K+",
                developerName = "LingoCore AI",
                isVerifiedDeveloper = true
            )
        }
        // 9. CoinRadar Crypto & Stock Watcher
        cleanId == "bot_coin_radar" || cleanId == "coin_radar" || cleanId == "coinradar" || combined.contains("coinradar") || combined.contains("coin_radar") || lowerName.contains("crypto watcher") -> {
            val primary = TapBotColors.AccentCrypto
            val secondary = Color(0xFF16A34A)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF162506),
                borderTint = Color(0xFF3B5E0E),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.TrendingUp,
                categoryLabel = "Crypto & Stocks",
                ratingScore = "4.8",
                reviewCount = "3.8k",
                downloadCount = "28K+",
                developerName = "RadarFi Labs",
                isVerifiedDeveloper = true
            )
        }
        // 10. FitTrack Hydration & Workouts
        cleanId == "bot_fit_track" || cleanId == "fit_track" || cleanId == "fittrack" || combined.contains("fittrack") || combined.contains("fit_track") || lowerName.contains("workout") -> {
            val primary = TapBotColors.AccentFitness
            val secondary = Color(0xFFEF4444)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF2B1207),
                borderTint = Color(0xFF6B2B11),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.FitnessCenter,
                categoryLabel = "Fitness Track",
                ratingScore = "4.8",
                reviewCount = "1.6k",
                downloadCount = "11K+",
                developerName = "FitCore Studio",
                isVerifiedDeveloper = true
            )
        }
        // 11. ParcelTrack Delivery Watcher
        cleanId == "bot_parcel_track" || cleanId == "parcel_track" || cleanId == "parceltrack" || combined.contains("parceltrack") || combined.contains("parcel_track") || lowerName.contains("delivery watcher") -> {
            val primary = TapBotColors.AccentShipping
            val secondary = Color(0xFF92400E)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF271607),
                borderTint = Color(0xFF613711),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.LocalShipping,
                categoryLabel = "Shipment Watch",
                ratingScore = "4.8",
                reviewCount = "2.0k",
                downloadCount = "16K+",
                developerName = "RoutePulse Logistics",
                isVerifiedDeveloper = true
            )
        }
        // 12. GitNotifier Repo Watcher
        cleanId == "bot_git_notifier" || cleanId == "git_notifier" || cleanId == "gitnotifier" || combined.contains("gitnotifier") || combined.contains("git_notifier") || lowerName.contains("repo watcher") -> {
            val primary = TapBotColors.AccentDeveloper
            val secondary = Color(0xFF334155)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF141923),
                borderTint = Color(0xFF2F3B52),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Code,
                categoryLabel = "Repo Watcher",
                ratingScore = "4.9",
                reviewCount = "2.5k",
                downloadCount = "19K+",
                developerName = "DevPulse Systems",
                isVerifiedDeveloper = true
            )
        }
        // 13. CamSentry Hardware Security
        cleanId == "bot_cam_sentry" || cleanId == "cam_sentry" || cleanId == "camsentry" || combined.contains("camsentry") || combined.contains("cam_sentry") || lowerName.contains("hardware security") -> {
            val primary = TapBotColors.AccentSentry
            val secondary = Color(0xFF991B1B)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF2A0C0C),
                borderTint = Color(0xFF661E1E),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Videocam,
                categoryLabel = "Cam Sentry",
                ratingScore = "4.9",
                reviewCount = "1.7k",
                downloadCount = "13K+",
                developerName = "SentryCam Defense",
                isVerifiedDeveloper = true
            )
        }
        // 14. WhisperBox Anonymous Mailbox
        cleanId == "bot_whisper_box" || cleanId == "whisper_box" || cleanId == "whisperbox" || combined.contains("whisperbox") || combined.contains("whisper_box") || lowerName.contains("anonymous mailbox") -> {
            val primary = TapBotColors.AccentSocial
            val secondary = Color(0xFF9333EA)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF270A1E),
                borderTint = Color(0xFF61194B),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Forum,
                categoryLabel = "Anon Mailbox",
                ratingScore = "4.8",
                reviewCount = "1.3k",
                downloadCount = "9K+",
                developerName = "WhisperNet Social",
                isVerifiedDeveloper = true
            )
        }
        // 15. TriviaMaster Daily Brain Quiz
        cleanId == "bot_trivia_master" || cleanId == "trivia_master" || cleanId == "triviamaster" || combined.contains("triviamaster") || combined.contains("trivia_master") || lowerName.contains("brain quiz") -> {
            val primary = TapBotColors.AccentTrivia
            val secondary = Color(0xFF7C3AED)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF281C06),
                borderTint = Color(0xFF63460F),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Psychology,
                categoryLabel = "Brain Quiz",
                ratingScore = "4.9",
                reviewCount = "3.6k",
                downloadCount = "25K+",
                developerName = "BrainForge Studio",
                isVerifiedDeveloper = true
            )
        }
        // 16. TapStream Music
        cleanId == "bot_music_streamer" || cleanId == "bot_music" || cleanId == "tapstream_music" || cleanId == "tapstream-music" || combined.contains("music") || combined.contains("tapstream") || combined.contains("audio") || combined.contains("stream") || lowerCat.contains("media") -> {
            val primary = TapBotColors.AccentMusic
            val secondary = Color(0xFF06B6D4)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF1C1438),
                borderTint = Color(0xFF473380),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.GraphicEq,
                categoryLabel = "Audio Stream",
                ratingScore = "4.8",
                reviewCount = "2.1k",
                downloadCount = "15K+",
                developerName = "TapStream Media",
                isVerifiedDeveloper = true
            )
        }
        // 17. Exon Robot Sentinel
        cleanId == "bot_exon" || cleanId == "exon" || cleanId == "exon_robot" || cleanId.startsWith("bot_1790084493920") || combined.contains("exon") || combined.contains("moderation") || combined.contains("management") -> {
            val primary = TapBotColors.AccentModeration
            val secondary = Color(0xFFB45309)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF291A06),
                borderTint = Color(0xFF66410F),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Shield,
                categoryLabel = "Group Guard",
                ratingScore = "4.9",
                reviewCount = "3.4k",
                downloadCount = "25K+",
                developerName = "Exon Security Labs",
                isVerifiedDeveloper = true
            )
        }
        // 18. TapBot Commander
        cleanId == "bot_commander" || cleanId == "tapbot_commander" || cleanId == "bot_commander_flagship" || combined.contains("commander") || combined.contains("watchdog") || combined.contains("fleet") || lowerCat.contains("automation") -> {
            val primary = TapBotColors.BrandCyan
            val secondary = TapBotColors.PrimaryCobalt
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF072230),
                borderTint = Color(0xFF134E68),
                brush = Brush.linearGradient(listOf(Color(0xFF06B6D4), TapBotColors.PrimaryCobalt)),
                icon = Icons.Rounded.Terminal,
                categoryLabel = "Automation",
                ratingScore = "5.0",
                reviewCount = "4.8k",
                downloadCount = "50K+",
                developerName = "TapBot Core Team",
                isVerifiedDeveloper = true
            )
        }
        // 19. Gemini Smart Assistant
        cleanId == "bot_ai_assistant" || cleanId == "bot_ai" || cleanId == "bot_gemini" || cleanId == "gemini_assistant" || combined.contains("ai") || combined.contains("gemini") || combined.contains("gpt") -> {
            val primary = TapBotColors.AccentAI
            val secondary = Color(0xFF4338CA)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF17183B),
                borderTint = Color(0xFF393E8A),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.AutoAwesome,
                categoryLabel = "Gemini AI",
                ratingScore = "4.9",
                reviewCount = "5.2k",
                downloadCount = "40K+",
                developerName = "Gemini AI Studio",
                isVerifiedDeveloper = true
            )
        }
        // 20. NewsPulse RSS Broadcaster
        cleanId == "bot_rss_notifier" || cleanId == "bot_rss" || cleanId == "rss_notifier" || cleanId == "rss_broadcaster" || cleanId == "newspulse" || combined.contains("rss") || combined.contains("feed") || combined.contains("broadcaster") || lowerCat.contains("news") -> {
            val primary = TapBotColors.AccentRss
            val secondary = Color(0xFFEA580C)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF2B1408),
                borderTint = Color(0xFF6E3111),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.RssFeed,
                categoryLabel = "News & RSS",
                ratingScore = "4.7",
                reviewCount = "890",
                downloadCount = "5K+",
                developerName = "TapBot News Lab",
                isVerifiedDeveloper = true
            )
        }
        // Custom Studio Workflow Fallback
        cleanId.startsWith("bot_custom_") || cleanId.startsWith("custom_wf_") || lowerCat.contains("custom") || combined.contains("workflow") -> {
            val primary = TapBotColors.AccentCustom
            val secondary = Color(0xFF0F766E)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF072622),
                borderTint = Color(0xFF135E56),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.AccountTree,
                categoryLabel = "Custom Studio",
                ratingScore = "4.8",
                reviewCount = "620",
                downloadCount = "2K+",
                developerName = "On-Device Studio",
                isVerifiedDeveloper = true
            )
        }
        // Generic Utility Fallback
        combined.contains("echo") || combined.contains("ping") || combined.contains("util") || combined.contains("diagnostic") -> {
            val primary = TapBotColors.AccentUtility
            val secondary = TapBotColors.PrimaryBlueDark
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF0A2236),
                borderTint = Color(0xFF1A4B75),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Speed,
                categoryLabel = "Utilities",
                ratingScore = "4.8",
                reviewCount = "1.5k",
                downloadCount = "12K+",
                developerName = "TapBot Core",
                isVerifiedDeveloper = true
            )
        }
        // Default Community Fallback
        else -> {
            val primary = TapBotColors.PrimaryBlue
            val secondary = TapBotColors.PrimaryBlueDark
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = TapBotColors.BlueSubtle,
                borderTint = TapBotColors.BlueBorder,
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.SmartToy,
                categoryLabel = category.ifBlank { "Telegram Bot" },
                ratingScore = "4.7",
                reviewCount = "950",
                downloadCount = "8K+",
                developerName = "TapBot Community",
                isVerifiedDeveloper = false
            )
        }
    }
}

fun Modifier.premiumCardBorder(
    cornerRadius: Dp = TapBotColors.RadiusLg,
    borderColorTop: Color = Color(0xFF283C5C),
    borderColorBottom: Color = Color(0xFF131D2E)
): Modifier = this.border(
    width = 1.dp,
    brush = Brush.verticalGradient(listOf(borderColorTop, borderColorBottom)),
    shape = RoundedCornerShape(cornerRadius)
)

val TabularNumberStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontFeatureSettings = "tnum"
)

val TabularMonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontFeatureSettings = "tnum"
)

// Specialized Tabular Numeral Tokens
val TabularMetricLarge = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    fontFeatureSettings = "tnum"
)

val TabularMetricMedium = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    fontFeatureSettings = "tnum"
)

val TabularMetricSmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    fontFeatureSettings = "tnum"
)

val TabularMonoCode = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum"
)

val ChipLabelStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.6.sp
)

val TapBotTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

private val TapBotConsoleDarkColorScheme = darkColorScheme(
    primary = TapBotColors.PrimaryCobalt,
    onPrimary = Color.White,
    primaryContainer = TapBotColors.SurfaceInteractive,
    onPrimaryContainer = TapBotColors.TextPrimary,
    secondary = TapBotColors.BrandCyan,
    onSecondary = TapBotColors.BrandCyanInk,
    secondaryContainer = TapBotColors.SurfaceElevated,
    onSecondaryContainer = TapBotColors.TextPrimary,
    tertiary = TapBotColors.Success,
    onTertiary = TapBotColors.BrandCyanInk,
    background = TapBotColors.Canvas,
    onBackground = TapBotColors.TextPrimary,
    surface = TapBotColors.SurfacePrimary,
    onSurface = TapBotColors.TextPrimary,
    surfaceVariant = TapBotColors.SurfaceElevated,
    onSurfaceVariant = TapBotColors.TextSecondary,
    outline = TapBotColors.Border,
    outlineVariant = TapBotColors.BorderSubtle,
    error = TapBotColors.Error,
    onError = Color.White
)

@Composable
fun TapBotTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Hardware Node Console intentionally maintains a unified dark console palette
    MaterialTheme(
        colorScheme = TapBotConsoleDarkColorScheme,
        typography = TapBotTypography,
        content = content
    )
}
