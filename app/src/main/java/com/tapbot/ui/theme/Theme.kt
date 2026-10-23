package com.tapbot.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BotHubDarkColorScheme = darkColorScheme(
    primary = BotHubColors.PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = BotHubColors.SecondarySurface,
    onPrimaryContainer = BotHubColors.PrimaryText,
    secondary = BotHubColors.Info,
    onSecondary = Color.Black,
    secondaryContainer = BotHubColors.ElevatedSurface,
    onSecondaryContainer = BotHubColors.PrimaryText,
    tertiary = BotHubColors.Success,
    onTertiary = Color.Black,
    background = BotHubColors.Background,
    onBackground = BotHubColors.PrimaryText,
    surface = BotHubColors.PrimarySurface,
    onSurface = BotHubColors.PrimaryText,
    surfaceVariant = BotHubColors.ElevatedSurface,
    onSurfaceVariant = BotHubColors.SecondaryText,
    outline = BotHubColors.Border,
    outlineVariant = BotHubColors.BorderSubtle,
    error = BotHubColors.Error,
    onError = Color.White
)

private val BotHubLightColorScheme = BotHubDarkColorScheme // BotHub is intentionally dark-first for the developer utility aesthetic

@Composable
fun TapBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to preserve the BotHub dark navy identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> BotHubDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
