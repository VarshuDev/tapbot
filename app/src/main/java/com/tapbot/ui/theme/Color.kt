package com.tapbot.ui.theme

import com.tapbot.core.ui.theme.TapBotColors

/**
 * Legacy `:app` color accessor delegated to `:core:ui` [TapBotColors].
 */
typealias BotHubColors = TapBotColors

// Backward-compatibility aliases for existing app-level screens
val DarkBackground = TapBotColors.Canvas
val DarkSurface = TapBotColors.SurfacePrimary
val DarkSurfaceVariant = TapBotColors.SurfaceElevated
val AccentCyan = TapBotColors.BrandCyan
val AccentGreen = TapBotColors.Success
val Purple80 = TapBotColors.PrimaryBlue
val PurpleGrey80 = TapBotColors.TextSecondary
val Pink80 = TapBotColors.Info
val Purple40 = TapBotColors.PrimaryCobalt
val PurpleGrey40 = TapBotColors.TextMuted
val Pink40 = TapBotColors.Error
