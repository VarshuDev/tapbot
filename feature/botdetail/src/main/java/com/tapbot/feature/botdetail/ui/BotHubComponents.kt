package com.tapbot.feature.botdetail.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotRunState

/**
 * BotHub Centralized Design Tokens (ui-ux-pro-max OLED Dark Theme)
 */
object BotHubTheme {
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

    // Corner Radii
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusXl = 20.dp
    val RadiusPill = 50.dp
}

/**
 * Extension modifier applying a hairline gradient border that gives cards a subtle light-source reflection.
 */
fun Modifier.premiumCardBorder(
    cornerRadius: Dp = BotHubTheme.RadiusLg,
    borderColorTop: Color = Color(0xFF2C3E5D),
    borderColorBottom: Color = Color(0xFF142033)
): Modifier = this.border(
    width = 1.dp,
    brush = Brush.verticalGradient(listOf(borderColorTop, borderColorBottom)),
    shape = RoundedCornerShape(cornerRadius)
)

/**
 * Resolves category gradient and vector icon for bots.
 */
fun resolveBotVisualIdentity(category: String, name: String): Pair<Brush, ImageVector> {
    val lowerCat = category.lowercase()
    val lowerName = name.lowercase()
    return when {
        lowerCat.contains("music") || lowerName.contains("music") || lowerName.contains("audio") || lowerName.contains("sound") -> {
            Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))) to Icons.Default.MusicNote
        }
        lowerCat.contains("ai") || lowerCat.contains("assistant") || lowerName.contains("ai") || lowerName.contains("gpt") -> {
            Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))) to Icons.Default.AutoAwesome
        }
        lowerCat.contains("util") || lowerCat.contains("tool") || lowerName.contains("utility") -> {
            Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857))) to Icons.Default.Handyman
        }
        lowerCat.contains("product") || lowerCat.contains("task") || lowerName.contains("productivity") -> {
            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFB45309))) to Icons.Default.TaskAlt
        }
        lowerCat.contains("game") || lowerName.contains("game") -> {
            Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFBE185D))) to Icons.Default.SportsEsports
        }
        lowerCat.contains("weather") || lowerName.contains("weather") -> {
            Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF0369A1))) to Icons.Default.Cloud
        }
        else -> {
            Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))) to Icons.Default.SmartToy
        }
    }
}

/**
 * Reusable squircle icon with ambient border ring.
 */
@Composable
fun BotIcon(
    name: String,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = if (size >= 64.dp) 20.dp else 12.dp
) {
    val (brush, vector) = resolveBotVisualIdentity(category, name)
    val iconSize = (size.value * 0.52f).dp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = vector,
            contentDescription = "$name icon",
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * StatusBadge with animated live pulse dot for running instances.
 */
@Composable
fun StatusBadge(
    status: BotInstanceStatus,
    modifier: Modifier = Modifier
) {
    val isLive = status is BotInstanceStatus.Running || status is BotInstanceStatus.Starting

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (bgColor, borderColor, textColor, dotColor, label) = when (status) {
        is BotInstanceStatus.Running -> Tuple5(
            BotHubTheme.SuccessSurface,
            BotHubTheme.SuccessBorder,
            BotHubTheme.Success,
            BotHubTheme.Success,
            "Running"
        )
        is BotInstanceStatus.Crashed -> Tuple5(
            BotHubTheme.ErrorSurface,
            BotHubTheme.ErrorBorder,
            BotHubTheme.Error,
            BotHubTheme.Error,
            "Crashed"
        )
        is BotInstanceStatus.Starting -> Tuple5(
            BotHubTheme.WarningSurface,
            BotHubTheme.WarningBorder,
            BotHubTheme.Warning,
            BotHubTheme.Warning,
            "Starting"
        )
        is BotInstanceStatus.Stopping -> Tuple5(
            BotHubTheme.SecondarySurface,
            BotHubTheme.Border,
            BotHubTheme.MutedText,
            BotHubTheme.MutedText,
            "Stopping"
        )
        is BotInstanceStatus.Stopped -> Tuple5(
            BotHubTheme.SecondarySurface,
            BotHubTheme.Border,
            BotHubTheme.SecondaryText,
            BotHubTheme.MutedText,
            "Stopped"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(BotHubTheme.RadiusPill))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
                .alpha(if (isLive) pulseAlpha else 1.0f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * RunStateBadge with live animated pulse dot.
 */
@Composable
fun RunStateBadge(
    runState: BotRunState,
    modifier: Modifier = Modifier
) {
    val isLive = runState is BotRunState.Running || runState is BotRunState.Starting

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition2")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha2"
    )

    val (bgColor, borderColor, textColor, dotColor, label) = when (runState) {
        is BotRunState.Running -> Tuple5(
            BotHubTheme.SuccessSurface,
            BotHubTheme.SuccessBorder,
            BotHubTheme.Success,
            BotHubTheme.Success,
            "Running"
        )
        is BotRunState.Starting -> Tuple5(
            BotHubTheme.WarningSurface,
            BotHubTheme.WarningBorder,
            BotHubTheme.Warning,
            BotHubTheme.Warning,
            "Starting"
        )
        is BotRunState.Error -> Tuple5(
            BotHubTheme.ErrorSurface,
            BotHubTheme.ErrorBorder,
            BotHubTheme.Error,
            BotHubTheme.Error,
            "Error"
        )
        is BotRunState.PausedNoNetwork -> Tuple5(
            BotHubTheme.WarningSurface,
            BotHubTheme.WarningBorder,
            BotHubTheme.Warning,
            BotHubTheme.Warning,
            "Waiting Network"
        )
        is BotRunState.Stopped -> Tuple5(
            BotHubTheme.SecondarySurface,
            BotHubTheme.Border,
            BotHubTheme.SecondaryText,
            BotHubTheme.MutedText,
            "Stopped"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(BotHubTheme.RadiusPill))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
                .alpha(if (isLive) pulseAlpha else 1.0f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

/**
 * Modern tactile Primary Button with subtle gradient fill and >=48dp touch target.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    val buttonBrush = if (enabled && !isLoading) {
        Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6)))
    } else {
        Brush.horizontalGradient(listOf(BotHubTheme.SecondarySurface, BotHubTheme.SecondarySurface))
    }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(buttonBrush),
        shape = RoundedCornerShape(BotHubTheme.RadiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = BotHubTheme.MutedText
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * Modern tactile Secondary Button with >=48dp touch target.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    borderColor: Color = BotHubTheme.Border
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(BotHubTheme.RadiusMd),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = BotHubTheme.ElevatedSurface,
            contentColor = BotHubTheme.PrimaryText,
            disabledContainerColor = BotHubTheme.PrimarySurface,
            disabledContentColor = BotHubTheme.MutedText
        )
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Developer Monospace Badge for versions, hashes, or sizes.
 */
@Composable
fun MonospaceBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BotHubTheme.Info,
    backgroundColor: Color = BotHubTheme.ElevatedSurface
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
