package com.tapbot.ui.theme

import androidx.compose.runtime.Composable
import com.tapbot.core.ui.theme.TapBotTheme as CoreTapBotTheme

@Composable
fun TapBotTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CoreTapBotTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
