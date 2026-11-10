package com.tapbot.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotPackageInfo
import com.tapbot.core.model.BotRunState
import com.tapbot.core.ui.motion.rememberReducedMotion
import com.tapbot.core.ui.motion.tapBotPressScale
import com.tapbot.core.ui.theme.TabularNumberStyle
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder
import com.tapbot.core.ui.theme.resolveBotVisualIdentity

/**
 * Vector-faithful TapBot mark drawn directly from `ic_launcher_foreground.xml` (108x108 normalized).
 */
@Composable
fun TapBotLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val cyan = TapBotColors.BrandCyan
    val chassisFill = Color(0xFF161B22)
    val visorFill = Color(0xFF0D1117)

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "TapBot node logo" }
    ) {
        val scale = this.size.minDimension / 108f
        fun sx(v: Float) = v * scale
        fun sy(v: Float) = v * scale

        // Antenna stem & tip
        drawLine(
            color = cyan,
            start = Offset(sx(54f), sy(22f)),
            end = Offset(sx(54f), sy(34f)),
            strokeWidth = sx(3.6f),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = cyan,
            radius = sx(4.5f),
            center = Offset(sx(54f), sy(21f))
        )

        // Side ears
        drawRoundRect(
            color = cyan,
            topLeft = Offset(sx(14f), sy(48f)),
            size = Size(sx(6f), sy(18f)),
            cornerRadius = CornerRadius(sx(2f), sx(2f))
        )
        drawRoundRect(
            color = cyan,
            topLeft = Offset(sx(88f), sy(48f)),
            size = Size(sx(6f), sy(18f)),
            cornerRadius = CornerRadius(sx(2f), sx(2f))
        )

        // Head chassis
        val headTopLeft = Offset(sx(20f), sy(34f))
        val headSize = Size(sx(68f), sy(52f))
        val headCorner = CornerRadius(sx(10f), sx(10f))
        drawRoundRect(
            color = chassisFill,
            topLeft = headTopLeft,
            size = headSize,
            cornerRadius = headCorner
        )
        drawRoundRect(
            color = cyan,
            topLeft = headTopLeft,
            size = headSize,
            cornerRadius = headCorner,
            style = Stroke(width = sx(4.2f))
        )

        // Dark visor
        drawRoundRect(
            color = visorFill,
            topLeft = Offset(sx(26f), sy(46f)),
            size = Size(sx(56f), sy(30f)),
            cornerRadius = CornerRadius(sx(6f), sx(6f))
        )

        // Glowing eyes
        drawRoundRect(
            color = cyan,
            topLeft = Offset(sx(35f), sy(55f)),
            size = Size(sx(14f), sy(13f)),
            cornerRadius = CornerRadius(sx(3.5f), sx(3.5f))
        )
        drawRoundRect(
            color = cyan,
            topLeft = Offset(sx(59f), sy(55f)),
            size = Size(sx(14f), sy(13f)),
            cornerRadius = CornerRadius(sx(3.5f), sx(3.5f))
        )

        // Forehead power bolt
        val bolt = Path().apply {
            moveTo(sx(52f), sy(36.5f))
            lineTo(sx(58f), sy(36.5f))
            lineTo(sx(54.5f), sy(40f))
            lineTo(sx(57f), sy(40f))
            lineTo(sx(52f), sy(44f))
            lineTo(sx(53.2f), sy(40.5f))
            lineTo(sx(52f), sy(40.5f))
            close()
        }
        drawPath(bolt, color = cyan)
    }
}

/**
 * Reusable squircle icon calibrated to the bot's visual identity.
 */
@Composable
fun BotIcon(
    name: String,
    category: String,
    modifier: Modifier = Modifier,
    botId: String = "",
    size: Dp = 48.dp,
    cornerRadius: Dp = if (size >= 60.dp) 18.dp else 13.dp
) {
    val identity = resolveBotVisualIdentity(botId = botId, category = category, name = name)
    val iconSize = (size.value * 0.50f).dp

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(identity.brush)
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = identity.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

private data class StatusPresentation(
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val dotColor: Color,
    val icon: ImageVector,
    val label: String,
    val isLive: Boolean
)

/**
 * Semantic StatusBadge combining color + icon + text (never per-row infinite animation).
 */
@Composable
fun StatusBadge(
    status: BotInstanceStatus,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    pulseAlpha: Float = 1f
) {
    val spec = when (status) {
        is BotInstanceStatus.Running -> StatusPresentation(
            bgColor = TapBotColors.SuccessSurface,
            borderColor = TapBotColors.SuccessBorder,
            textColor = TapBotColors.SuccessText,
            dotColor = TapBotColors.Success,
            icon = Icons.Rounded.CheckCircle,
            label = "Running",
            isLive = true
        )
        is BotInstanceStatus.Crashed -> StatusPresentation(
            bgColor = TapBotColors.ErrorSurface,
            borderColor = TapBotColors.ErrorBorder,
            textColor = TapBotColors.ErrorText,
            dotColor = TapBotColors.Error,
            icon = Icons.Rounded.ErrorOutline,
            label = "Crashed",
            isLive = false
        )
        is BotInstanceStatus.Starting -> StatusPresentation(
            bgColor = TapBotColors.WarningSurface,
            borderColor = TapBotColors.WarningBorder,
            textColor = TapBotColors.WarningText,
            dotColor = TapBotColors.Warning,
            icon = Icons.Rounded.HourglassTop,
            label = "Starting",
            isLive = true
        )
        is BotInstanceStatus.Stopping -> StatusPresentation(
            bgColor = TapBotColors.SurfaceInteractive,
            borderColor = TapBotColors.Border,
            textColor = TapBotColors.TextSecondary,
            dotColor = TapBotColors.TextMuted,
            icon = Icons.Rounded.HourglassTop,
            label = "Stopping",
            isLive = false
        )
        is BotInstanceStatus.Stopped -> StatusPresentation(
            bgColor = TapBotColors.SurfaceInteractive,
            borderColor = TapBotColors.Border,
            textColor = TapBotColors.TextSecondary,
            dotColor = TapBotColors.TextMuted,
            icon = Icons.Rounded.StopCircle,
            label = "Stopped",
            isLive = false
        )
    }

    val effectiveAlpha = if (spec.isLive && !reducedMotion) pulseAlpha else 1f
    StatusPillContent(
        spec = spec,
        dotAlpha = effectiveAlpha,
        modifier = modifier
    )
}

/**
 * Semantic RunStateBadge combining color + icon + label for live runtime telemetry.
 */
@Composable
fun RunStateBadge(
    runState: BotRunState,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion(),
    pulseAlpha: Float = 1f
) {
    val spec = when (runState) {
        is BotRunState.Running -> StatusPresentation(
            bgColor = TapBotColors.SuccessSurface,
            borderColor = TapBotColors.SuccessBorder,
            textColor = TapBotColors.SuccessText,
            dotColor = TapBotColors.Success,
            icon = Icons.Rounded.CheckCircle,
            label = "Running",
            isLive = true
        )
        is BotRunState.Starting -> StatusPresentation(
            bgColor = TapBotColors.WarningSurface,
            borderColor = TapBotColors.WarningBorder,
            textColor = TapBotColors.WarningText,
            dotColor = TapBotColors.Warning,
            icon = Icons.Rounded.HourglassTop,
            label = "Starting",
            isLive = true
        )
        is BotRunState.Error -> StatusPresentation(
            bgColor = TapBotColors.ErrorSurface,
            borderColor = TapBotColors.ErrorBorder,
            textColor = TapBotColors.ErrorText,
            dotColor = TapBotColors.Error,
            icon = Icons.Rounded.ErrorOutline,
            label = "Error",
            isLive = false
        )
        is BotRunState.PausedNoNetwork -> StatusPresentation(
            bgColor = TapBotColors.WarningSurface,
            borderColor = TapBotColors.WarningBorder,
            textColor = TapBotColors.WarningText,
            dotColor = TapBotColors.Warning,
            icon = Icons.Rounded.WifiOff,
            label = "Offline Wait",
            isLive = false
        )
        is BotRunState.Stopped -> StatusPresentation(
            bgColor = TapBotColors.SurfaceInteractive,
            borderColor = TapBotColors.Border,
            textColor = TapBotColors.TextSecondary,
            dotColor = TapBotColors.TextMuted,
            icon = Icons.Rounded.StopCircle,
            label = "Stopped",
            isLive = false
        )
    }

    val effectiveAlpha = if (spec.isLive && !reducedMotion) pulseAlpha else 1f
    StatusPillContent(
        spec = spec,
        dotAlpha = effectiveAlpha,
        modifier = modifier
    )
}

@Composable
private fun StatusPillContent(
    spec: StatusPresentation,
    dotAlpha: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
            .background(spec.bgColor)
            .border(1.dp, spec.borderColor, RoundedCornerShape(TapBotColors.RadiusPill))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (spec.isLive) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(spec.dotColor)
                    .alpha(dotAlpha)
            )
        } else {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = spec.textColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = spec.label,
            color = spec.textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * Category filter chip with minimum 48dp accessible hit area.
 */
@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    val bgColor = if (isSelected) TapBotColors.PrimaryCobalt else TapBotColors.SurfaceElevated
    val borderColor = if (isSelected) TapBotColors.BrandCyan.copy(alpha = 0.75f) else TapBotColors.Border
    val textColor = if (isSelected) Color.White else TapBotColors.TextSecondary
    val displayLabel = if (count != null && count > 0) "$text ($count)" else text

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(TapBotColors.RadiusPill))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayLabel,
                color = textColor,
                style = TabularNumberStyle.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Console Search Field with focus highlight and 48dp clear button.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search bots, commands, capabilities..."
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(TapBotColors.SurfaceElevated)
            .border(
                width = 1.dp,
                color = if (isFocused) TapBotColors.BrandCyan.copy(alpha = 0.65f) else TapBotColors.Border,
                shape = RoundedCornerShape(TapBotColors.RadiusMd)
            )
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (isFocused) TapBotColors.BrandCyan else TapBotColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholderText,
                    color = TapBotColors.TextMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = TapBotColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(TapBotColors.BrandCyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    tint = TapBotColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Featured Bot showcase card built for multi-item horizontal paging.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeaturedBotCard(
    bot: BotMetadata,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isInstalled: Boolean = false,
    isRunning: Boolean = false,
    previewCommands: List<String> = emptyList()
) {
    val identity = resolveBotVisualIdentity(botId = bot.id, category = bot.category, name = bot.name)
    val runtimeLabel = if (bot.packageInfo.runtimeType == "native_art") "ART Native" else "Built-In"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(
                Brush.verticalGradient(
                    listOf(
                        identity.surfaceTint.copy(alpha = 0.60f),
                        TapBotColors.SurfacePrimary
                    )
                )
            )
            .premiumCardBorder(
                borderColorTop = identity.accentColor.copy(alpha = 0.55f),
                borderColorBottom = TapBotColors.Border
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // Accent hairline + top badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                        .background(identity.surfaceTint)
                        .border(1.dp, identity.borderTint, RoundedCornerShape(TapBotColors.RadiusPill))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = identity.categoryLabel.uppercase(),
                        color = identity.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                MonospaceBadge(
                    text = "$runtimeLabel • v${bot.version}",
                    color = TapBotColors.TextSecondary,
                    backgroundColor = TapBotColors.SurfaceElevated
                )
            }

            if (isRunning || isInstalled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                        .background(if (isRunning) TapBotColors.SuccessSurface else TapBotColors.BlueSubtle)
                        .border(
                            1.dp,
                            if (isRunning) TapBotColors.SuccessBorder else TapBotColors.BlueBorder,
                            RoundedCornerShape(TapBotColors.RadiusPill)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isRunning) "RUNNING" else "INSTALLED",
                        color = if (isRunning) TapBotColors.SuccessText else TapBotColors.Info,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotIcon(
                name = bot.name,
                category = bot.category,
                botId = bot.id,
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bot.name,
                    color = TapBotColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "by ${bot.author}",
                    color = TapBotColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(TapBotColors.SurfaceElevated)
                    .border(1.dp, identity.borderTint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Inspect ${bot.name}",
                    tint = identity.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = bot.summary.ifBlank { bot.description },
            color = TapBotColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (previewCommands.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                previewCommands.take(4).forEach { cmd ->
                    MonospaceBadge(
                        text = cmd,
                        color = identity.accentColor,
                        backgroundColor = TapBotColors.Canvas
                    )
                }
            }
        }
    }
}

/**
 * Rich BotCard list/grid item showing identity accent, summary, real commands, and honest CTA.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BotCard(
    bot: BotMetadata,
    isInstalled: Boolean,
    onClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRunning: Boolean = false,
    hasUpdate: Boolean = false,
    previewCommands: List<String> = emptyList()
) {
    val identity = resolveBotVisualIdentity(botId = bot.id, category = bot.category, name = bot.name)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder(
                borderColorTop = if (isRunning) TapBotColors.SuccessBorder else TapBotColors.BorderFocus,
                borderColorBottom = TapBotColors.BorderSubtle
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotIcon(
                name = bot.name,
                category = bot.category,
                botId = bot.id,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) TapBotColors.Success else identity.accentColor)
                    )
                    Text(
                        text = bot.name,
                        color = TapBotColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = identity.categoryLabel,
                        color = identity.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "•",
                        color = TapBotColors.TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "v${bot.version}",
                        color = TapBotColors.TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val actionLabel = when {
                isRunning -> "Active"
                hasUpdate -> "Update"
                isInstalled -> "Open"
                else -> "Setup"
            }
            val pillBg = when {
                isRunning -> TapBotColors.SuccessSurface
                hasUpdate -> TapBotColors.WarningSurface
                isInstalled -> TapBotColors.SurfaceInteractive
                else -> TapBotColors.PrimaryCobalt
            }
            val pillBorder = when {
                isRunning -> TapBotColors.SuccessBorder
                hasUpdate -> TapBotColors.WarningBorder
                isInstalled -> TapBotColors.BorderFocus
                else -> TapBotColors.PrimaryBlue.copy(alpha = 0.5f)
            }
            val pillTextColor = when {
                isRunning -> TapBotColors.SuccessText
                hasUpdate -> TapBotColors.WarningText
                isInstalled -> TapBotColors.TextPrimary
                else -> Color.White
            }

            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onActionClick)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                        .background(pillBg)
                        .border(1.dp, pillBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionLabel,
                        color = pillTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val summaryText = bot.summary.ifBlank { bot.description }
        if (summaryText.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summaryText,
                color = TapBotColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (previewCommands.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                previewCommands.take(3).forEach { cmd ->
                    MonospaceBadge(
                        text = cmd,
                        color = TapBotColors.Info,
                        backgroundColor = TapBotColors.SurfaceElevated
                    )
                }
            }
        }
    }
}

/**
 * Calm structured loading placeholder (no infinite shimmer animation).
 */
@Composable
fun BotCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusLg))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(TapBotColors.SurfaceElevated)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TapBotColors.SurfaceInteractive)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TapBotColors.SurfaceElevated)
                )
            }
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                    .background(TapBotColors.SurfaceElevated)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TapBotColors.SurfaceElevated)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TapBotColors.SurfaceElevated)
        )
    }
}

/**
 * SectionHeader with accessible 48dp optional action button.
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
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TapBotColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onActionClick != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                    .clickable(role = Role.Button, onClick = onActionClick)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionText,
                    color = TapBotColors.BrandCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Primary action button with >=48dp touch height and restrained press-scale feedback.
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
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .tapBotPressScale(interactionSource, reducedMotion),
        shape = RoundedCornerShape(TapBotColors.RadiusMd),
        colors = ButtonDefaults.buttonColors(
            containerColor = TapBotColors.PrimaryCobalt,
            contentColor = Color.White,
            disabledContainerColor = TapBotColors.SurfaceInteractive,
            disabledContentColor = TapBotColors.TextMuted
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
                letterSpacing = 0.25.sp
            )
        }
    }
}

/**
 * Secondary outlined button with 50dp height.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    borderColor: Color = TapBotColors.BorderFocus
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(TapBotColors.RadiusMd),
        border = BorderStroke(1.dp, if (enabled) borderColor else TapBotColors.BorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = TapBotColors.SurfaceElevated,
            contentColor = TapBotColors.TextPrimary,
            disabledContainerColor = TapBotColors.SurfacePrimary,
            disabledContentColor = TapBotColors.TextMuted
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
 * Developer Monospace Badge for versions, commands, or technical tags.
 */
@Composable
fun MonospaceBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TapBotColors.Info,
    backgroundColor: Color = TapBotColors.SurfaceElevated
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(TapBotColors.RadiusXs))
            .background(backgroundColor)
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(TapBotColors.RadiusXs))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360)
@Composable
private fun TapBotCoreComponentsPreview() {
    TapBotTheme {
        Column(
            modifier = Modifier
                .background(TapBotColors.Canvas)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TapBotLogoMark(size = 36.dp)
                StatusBadge(status = BotInstanceStatus.Running(), reducedMotion = true)
                RunStateBadge(runState = BotRunState.PausedNoNetwork(), reducedMotion = true)
            }
            FeaturedBotCard(
                bot = BotMetadata(
                    id = "bot_commander",
                    name = "TapBot Commander",
                    summary = "Flagship device watchdog, remote phone control & fleet orchestrator.",
                    description = "",
                    author = "TapBot Core Team",
                    version = "1.0.0",
                    iconUrl = "",
                    category = "Automation",
                    packageInfo = BotPackageInfo("", "", "native_art", packageSizeBytes = 1703L),
                    isFeatured = true
                ),
                onClick = {},
                isRunning = true,
                previewCommands = listOf("/status", "/bots", "/ring")
            )
            BotCardSkeleton()
        }
    }
}
