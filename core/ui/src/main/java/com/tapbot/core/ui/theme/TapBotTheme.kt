package com.tapbot.core.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
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
    val categoryLabel: String
)

/**
 * Resolves coherent color signature and iconography per bot runtime identity.
 * Checks RSS/Broadcaster before generic "media" and identifies Exon, Commander, and Custom nodes explicitly.
 */
fun resolveBotVisualIdentity(
    botId: String = "",
    category: String = "",
    name: String = ""
): BotVisualIdentity {
    val lowerId = botId.lowercase()
    val lowerCat = category.lowercase()
    val lowerName = name.lowercase()
    val combined = "$lowerId $lowerCat $lowerName"

    return when {
        combined.contains("rss") || combined.contains("feed") || combined.contains("broadcaster") || lowerCat.contains("news") -> {
            val primary = TapBotColors.AccentRss
            val secondary = Color(0xFFC2410C)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF2B1408),
                borderTint = Color(0xFF6E3111),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.RssFeed,
                categoryLabel = "RSS Feed"
            )
        }
        combined.contains("exon") || lowerId.startsWith("bot_1790084493920") || combined.contains("moderation") || combined.contains("management") -> {
            val primary = TapBotColors.AccentModeration
            val secondary = Color(0xFFB45309)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF291A06),
                borderTint = Color(0xFF66410F),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.Shield,
                categoryLabel = "Group Guard"
            )
        }
        combined.contains("commander") || combined.contains("watchdog") || combined.contains("fleet") || lowerCat.contains("automation") -> {
            val primary = TapBotColors.BrandCyan
            val secondary = TapBotColors.PrimaryCobalt
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF072230),
                borderTint = Color(0xFF134E68),
                brush = Brush.linearGradient(listOf(Color(0xFF06B6D4), TapBotColors.PrimaryCobalt)),
                icon = Icons.Rounded.Terminal,
                categoryLabel = "Commander"
            )
        }
        lowerId.startsWith("bot_custom_") || lowerId.startsWith("custom_wf_") || lowerCat.contains("custom") || combined.contains("workflow") -> {
            val primary = TapBotColors.AccentCustom
            val secondary = Color(0xFF0F766E)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF072622),
                borderTint = Color(0xFF135E56),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.AccountTree,
                categoryLabel = "Custom Flow"
            )
        }
        combined.contains("music") || combined.contains("tapstream") || combined.contains("audio") || combined.contains("stream") || lowerCat.contains("media") -> {
            val primary = TapBotColors.AccentMusic
            val secondary = Color(0xFF0891B2)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF1C1438),
                borderTint = Color(0xFF473380),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.GraphicEq,
                categoryLabel = "Audio Stream"
            )
        }
        combined.contains("ai") || combined.contains("gemini") || combined.contains("gpt") -> {
            val primary = TapBotColors.AccentAI
            val secondary = Color(0xFF4338CA)
            BotVisualIdentity(
                accentColor = primary,
                secondaryColor = secondary,
                surfaceTint = Color(0xFF17183B),
                borderTint = Color(0xFF393E8A),
                brush = Brush.linearGradient(listOf(primary, secondary)),
                icon = Icons.Rounded.AutoAwesome,
                categoryLabel = "Gemini AI"
            )
        }
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
                categoryLabel = "Utility Node"
            )
        }
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
                categoryLabel = category.ifBlank { "Bot Node" }
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
