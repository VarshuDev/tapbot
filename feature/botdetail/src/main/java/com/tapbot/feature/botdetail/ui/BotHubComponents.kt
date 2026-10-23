package com.tapbot.feature.botdetail.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotRunState

/**
 * BotHub Centralized Design Tokens for BotDetail
 */
object BotHubTheme {
    val Background = Color(0xFF070D18)
    val PrimarySurface = Color(0xFF0D1728)
    val ElevatedSurface = Color(0xFF111F33)
    val SecondarySurface = Color(0xFF16263D)

    val PrimaryBlue = Color(0xFF3988FF)
    val BluePressed = Color(0xFF2674E8)
    val BlueSubtle = Color(0xFF1A3866)
    val BlueBorder = Color(0xFF27436B)

    val PrimaryText = Color(0xFFF3F6FC)
    val SecondaryText = Color(0xFF9AAAC0)
    val MutedText = Color(0xFF6E819B)

    val Border = Color(0xFF22334B)
    val BorderSubtle = Color(0xFF1A2A40)

    val Success = Color(0xFF27D995)
    val Warning = Color(0xFFFFB84D)
    val Error = Color(0xFFFF647C)
    val Info = Color(0xFF65B7FF)

    val SuccessSurface = Color(0xFF0D2E24)
    val SuccessBorder = Color(0xFF1B5E4A)
    val ErrorSurface = Color(0xFF33141E)
    val ErrorBorder = Color(0xFF631C2D)
    val WarningSurface = Color(0xFF332612)
    val WarningBorder = Color(0xFF5E431B)

    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusXl = 20.dp
    val RadiusPill = 50.dp
}

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
            Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF047857))) to Icons.Default.Handyman
        }
        lowerCat.contains("product") || lowerCat.contains("task") || lowerName.contains("productivity") -> {
            Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309))) to Icons.Default.TaskAlt
        }
        lowerCat.contains("game") || lowerName.contains("game") -> {
            Brush.linearGradient(listOf(Color(0xFFDB2777), Color(0xFFBE185D))) to Icons.Default.SportsEsports
        }
        lowerCat.contains("weather") || lowerName.contains("weather") -> {
            Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))) to Icons.Default.Cloud
        }
        else -> {
            Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))) to Icons.Default.SmartToy
        }
    }
}

@Composable
fun BotIcon(
    name: String,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = if (size >= 64.dp) 18.dp else 12.dp
) {
    val (brush, vector) = resolveBotVisualIdentity(category, name)
    val iconSize = (size.value * 0.52f).dp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
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

@Composable
fun StatusBadge(
    status: BotInstanceStatus,
    modifier: Modifier = Modifier
) {
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
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
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

@Composable
fun RunStateBadge(
    runState: BotRunState,
    modifier: Modifier = Modifier
) {
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
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
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

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(BotHubTheme.RadiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = BotHubTheme.PrimaryBlue,
            contentColor = Color.White,
            disabledContainerColor = BotHubTheme.SecondarySurface,
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
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

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
        modifier = modifier.height(48.dp),
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
