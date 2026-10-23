package com.tapbot.feature.catalog.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotMetadata

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
 * Resolves distinctive visual theme (color gradient and icon) for a bot based on category and name.
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
 * StatusBadge pill component with live pulsing animation for running instances.
 */
@Composable
fun StatusBadge(
    status: BotInstanceStatus,
    modifier: Modifier = Modifier
) {
    val isLive = status is BotInstanceStatus.Running || status is BotInstanceStatus.Starting

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransitionCat")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlphaCat"
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

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

/**
 * CategoryChip pill component with high-contrast active state and subtle glow.
 */
@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6)))
    } else {
        Brush.horizontalGradient(listOf(BotHubTheme.ElevatedSurface, BotHubTheme.ElevatedSurface))
    }

    val textColor = if (isSelected) Color.White else BotHubTheme.SecondaryText
    val borderColor = if (isSelected) Color.Transparent else BotHubTheme.Border

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
            .background(backgroundBrush)
            .border(1.dp, borderColor, RoundedCornerShape(BotHubTheme.RadiusPill))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * SearchField component styled specifically for BotHub's dark aesthetic.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search bots, runtimes, tags..."
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.ElevatedSurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = BotHubTheme.MutedText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholderText,
                    color = BotHubTheme.MutedText,
                    fontSize = 14.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = BotHubTheme.PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(BotHubTheme.PrimaryBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    tint = BotHubTheme.SecondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * FeaturedBotCard with hairline gradient border and category micro-pill.
 */
@Composable
fun FeaturedBotCard(
    bot: BotMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder(borderColorTop = Color(0xFF3B82F6), borderColorBottom = Color(0xFF1E3A5F))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
                            .background(BotHubTheme.BlueSubtle)
                            .border(1.dp, BotHubTheme.BlueBorder, RoundedCornerShape(BotHubTheme.RadiusPill))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FEATURED BOT",
                            color = BotHubTheme.Info,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "v${bot.version}",
                        color = BotHubTheme.SecondaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = bot.name,
                    color = BotHubTheme.PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = bot.description,
                    color = BotHubTheme.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BotHubTheme.ElevatedSurface)
                    .border(1.dp, BotHubTheme.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View featured bot",
                    tint = BotHubTheme.PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * BotCard list item with elevated styling, category badge, and tactile action button.
 */
@Composable
fun BotCard(
    bot: BotMetadata,
    isInstalled: Boolean,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotIcon(
            name = bot.name,
            category = bot.category,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bot.name,
                color = BotHubTheme.PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = bot.category,
                    color = BotHubTheme.SecondaryText,
                    fontSize = 12.sp
                )
                Text(
                    text = " • ",
                    color = BotHubTheme.MutedText,
                    fontSize = 12.sp
                )
                Text(
                    text = "v${bot.version}",
                    color = BotHubTheme.Info,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action button (INSTALL or OPEN)
        val buttonBrush = if (isInstalled) {
            Brush.horizontalGradient(listOf(BotHubTheme.SecondarySurface, BotHubTheme.SecondarySurface))
        } else {
            Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6)))
        }

        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
                .background(buttonBrush)
                .border(
                    width = 1.dp,
                    color = if (isInstalled) BotHubTheme.Border else Color.Transparent,
                    shape = RoundedCornerShape(BotHubTheme.RadiusPill)
                )
                .clickable(onClick = onActionClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isInstalled) "Open" else "Install",
                color = if (isInstalled) BotHubTheme.PrimaryText else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * SectionHeader for dividing list groups.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = BotHubTheme.PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = BotHubTheme.PrimaryBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Primary action button with 50dp height (>=48dp touch target) and subtle gradient fill.
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
 * Secondary action button with 50dp height.
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
