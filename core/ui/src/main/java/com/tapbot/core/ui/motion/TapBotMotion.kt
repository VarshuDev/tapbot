package com.tapbot.core.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Motion constants and reduced-motion utilities aligned with TapBot Hardware Node Console.
 */
object TapBotMotion {
    const val EnterDurationMs = 260
    const val ExitDurationMs = 200
    const val TabSwapDurationMs = 190
    const val QuickFeedbackMs = 130

    val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    val ContentSpring = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> feedbackTween(reducedMotion: Boolean) = tween<T>(
        durationMillis = if (reducedMotion) 0 else QuickFeedbackMs,
        easing = StandardEasing
    )
}

/**
 * Safely reads Android's global ANIMATOR_DURATION_SCALE setting.
 * Falls back cleanly to false in Compose Preview or unit-test environments.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Draw-phase subtle tactile press scale (defaults to 0.98f) that skips spatial shrink under reduced motion.
 */
fun Modifier.tapBotPressScale(
    interactionSource: InteractionSource,
    reducedMotion: Boolean,
    pressedScale: Float = 0.98f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else TapBotMotion.QuickFeedbackMs,
            easing = TapBotMotion.StandardEasing
        ),
        label = "tapBotPressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
