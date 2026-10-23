package com.tapbot.feature.catalog.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotMetadata

/**
 * BotHub Centralized Design Tokens
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

/**
 * Reusable BotIcon squircle component with distinctive color, clipping, and accessible description.
 */
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

/**
 * StatusBadge pill component for bot operational states.
 */
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

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

/**
 * CategoryChip pill component for catalog filtering.
 */
@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) BotHubTheme.PrimaryBlue else BotHubTheme.PrimarySurface
    val textColor = if (isSelected) BotHubTheme.PrimaryText else BotHubTheme.SecondaryText
    val borderStroke = if (isSelected) null else BorderStroke(1.dp, BotHubTheme.Border)

    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(BotHubTheme.RadiusPill),
        color = bgColor,
        border = borderStroke
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
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
    placeholderText: String = "Search bots..."
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
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
 * FeaturedBotCard matching the reference hero card with subtle electric blue border.
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
            .background(BotHubTheme.ElevatedSurface)
            .border(1.dp, BotHubTheme.BlueBorder, RoundedCornerShape(BotHubTheme.RadiusLg))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Featured",
                    color = BotHubTheme.PrimaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
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
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View featured bot",
                tint = BotHubTheme.MutedText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * BotCard list item matching the reference "Popular Bots" row.
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
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.PrimarySurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotIcon(
            name = bot.name,
            category = bot.category,
            size = 46.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bot.name,
                color = BotHubTheme.PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${bot.category} • v${bot.version}",
                color = BotHubTheme.SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))

        // Action button (Install or Open)
        Surface(
            onClick = onActionClick,
            modifier = Modifier.height(34.dp),
            shape = RoundedCornerShape(BotHubTheme.RadiusPill),
            color = if (isInstalled) BotHubTheme.SecondarySurface else BotHubTheme.PrimaryBlue,
            border = if (isInstalled) BorderStroke(1.dp, BotHubTheme.Border) else null
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isInstalled) "Open" else "Install",
                    color = if (isInstalled) BotHubTheme.PrimaryText else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
            .padding(vertical = 8.dp),
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
 * Primary action button with 48dp minimum touch target.
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

/**
 * Secondary action button with 48dp minimum touch target.
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
