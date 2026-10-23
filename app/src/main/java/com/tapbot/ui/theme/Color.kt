package com.tapbot.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * BotHub Centralized Design Tokens
 * Premium, polished Telegram Bot Store palette matching the reference specification.
 */
object BotHubColors {
    // Canvas & Surfaces
    val Background = Color(0xFF070D18)
    val PrimarySurface = Color(0xFF0D1728)
    val ElevatedSurface = Color(0xFF111F33)
    val SecondarySurface = Color(0xFF16263D)

    // Primary Accents
    val PrimaryBlue = Color(0xFF3988FF)
    val BluePressed = Color(0xFF2674E8)
    val BlueSubtle = Color(0xFF1A3866)

    // Typography & Content
    val PrimaryText = Color(0xFFF3F6FC)
    val SecondaryText = Color(0xFF9AAAC0)
    val MutedText = Color(0xFF6E819B)

    // Dividers & Outlines
    val Border = Color(0xFF22334B)
    val BorderSubtle = Color(0xFF1A2A40)

    // Semantic States
    val Success = Color(0xFF27D995)
    val Warning = Color(0xFFFFB84D)
    val Error = Color(0xFFFF647C)
    val Info = Color(0xFF65B7FF)

    // Surface Status Tints
    val SuccessSurface = Color(0xFF0D2E24)
    val SuccessBorder = Color(0xFF1B5E4A)
    val ErrorSurface = Color(0xFF33141E)
    val ErrorBorder = Color(0xFF631C2D)
    val WarningSurface = Color(0xFF332612)
    val WarningBorder = Color(0xFF5E431B)

    // Bot Category Gradients / Backgrounds
    val CategoryMusic = Color(0xFF2563EB)
    val CategoryAI = Color(0xFF7C3AED)
    val CategoryUtility = Color(0xFF059669)
    val CategoryProductivity = Color(0xFFD97706)
    val CategoryGames = Color(0xFFDB2777)
    val CategoryWeather = Color(0xFF0284C7)
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
