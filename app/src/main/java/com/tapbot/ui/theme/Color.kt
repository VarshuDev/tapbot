package com.tapbot.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * BotHub Centralized Design Tokens (ui-ux-pro-max OLED Dark Theme)
 */
object BotHubColors {
    // True OLED Deep Canvas & Surface Hierarchy
    val Background = Color(0xFF030712)
    val PrimarySurface = Color(0xFF0B132B)
    val ElevatedSurface = Color(0xFF111E38)
    val SecondarySurface = Color(0xFF162544)

    // Vibrant Electric Primary & Accents
    val PrimaryBlue = Color(0xFF3B82F6)
    val PrimaryBlueDark = Color(0xFF1D4ED8)
    val BluePressed = Color(0xFF2563EB)
    val BlueSubtle = Color(0xFF1E3A5F)
    val BlueBorder = Color(0xFF2D4B75)

    // Content & Typography
    val PrimaryText = Color(0xFFF8FAFC)
    val SecondaryText = Color(0xFF94A3B8)
    val MutedText = Color(0xFF64748B)

    // Hairline & Structural Borders
    val Border = Color(0xFF1F3252)
    val BorderSubtle = Color(0xFF152238)
    val BorderHighlight = Color(0xFF2E466E)

    // Semantic Status Colors
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF06B6D4)

    // Semantic Surface Tints
    val SuccessSurface = Color(0xFF06281E)
    val SuccessBorder = Color(0xFF0F5640)
    val ErrorSurface = Color(0xFF2E0D15)
    val ErrorBorder = Color(0xFF6B1D2F)
    val WarningSurface = Color(0xFF2E1F0A)
    val WarningBorder = Color(0xFF6B4512)

    // Category Color Accents
    val CategoryMusic = Color(0xFF3B82F6)
    val CategoryAI = Color(0xFF8B5CF6)
    val CategoryUtility = Color(0xFF10B981)
    val CategoryProductivity = Color(0xFFF59E0B)
    val CategoryGames = Color(0xFFEC4899)
    val CategoryWeather = Color(0xFF06B6D4)
}

// Backward-compatibility aliases for legacy code
val DarkBackground = BotHubColors.Background
val DarkSurface = BotHubColors.PrimarySurface
val DarkSurfaceVariant = BotHubColors.ElevatedSurface
val AccentCyan = BotHubColors.Info
val AccentGreen = BotHubColors.Success
val Purple80 = BotHubColors.PrimaryBlue
val PurpleGrey80 = BotHubColors.SecondaryText
val Pink80 = BotHubColors.Info
val Purple40 = BotHubColors.BluePressed
val PurpleGrey40 = BotHubColors.MutedText
val Pink40 = BotHubColors.Error
