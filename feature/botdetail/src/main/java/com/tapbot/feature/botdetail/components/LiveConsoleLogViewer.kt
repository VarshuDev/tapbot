package com.tapbot.feature.botdetail.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.model.LogLevel
import com.tapbot.core.security.SecretRedactor
import com.tapbot.core.ui.theme.TabularMonoStyle
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ConsoleLogFilter(val label: String) {
    ALL("All"),
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR")
}

@Composable
fun LiveConsoleLogViewer(
    logs: List<BotLogEntry>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    var selectedFilter by remember { mutableStateOf(ConsoleLogFilter.ALL) }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    var copiedNotice by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            ConsoleLogFilter.ALL -> logs
            ConsoleLogFilter.INFO -> logs.filter { it.level == LogLevel.INFO }
            ConsoleLogFilter.WARN -> logs.filter { it.level == LogLevel.WARN }
            ConsoleLogFilter.ERROR -> logs.filter { it.level == LogLevel.ERROR }
        }
    }

    LaunchedEffect(filteredLogs.size, autoScrollEnabled) {
        if (autoScrollEnabled && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    LaunchedEffect(copiedNotice) {
        if (copiedNotice) {
            delay(1800)
            copiedNotice = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .padding(16.dp)
    ) {
        // Terminal Window Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TapBotColors.Error))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TapBotColors.Warning))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TapBotColors.Success))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = TapBotColors.SuccessText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Live Console",
                    color = TapBotColors.TextPrimary,
                    style = TabularMonoStyle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${filteredLogs.size}",
                        color = TapBotColors.BrandCyan,
                        style = TabularMonoStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                            .background(TapBotColors.BrandCyanSurface)
                            .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (copiedNotice) {
                    Text(
                        text = "Copied",
                        color = TapBotColors.SuccessText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                            .background(TapBotColors.SuccessSurface)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = { autoScrollEnabled = !autoScrollEnabled },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (autoScrollEnabled) Icons.Rounded.VerticalAlignBottom else Icons.Rounded.Lock,
                        contentDescription = if (autoScrollEnabled) "Pause console auto-scroll" else "Resume console auto-scroll",
                        tint = if (autoScrollEnabled) TapBotColors.BrandCyan else TapBotColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (filteredLogs.isNotEmpty()) {
                            val formatted = filteredLogs.joinToString("\n") { entry ->
                                val ts = timeFormat.format(Date(entry.timestamp))
                                val safeMsg = SecretRedactor.redact(entry.message)
                                "[$ts] [${entry.level.name}] ${entry.tag}: $safeMsg"
                            }
                            if (copyToClipboard(context, "TapBot Console Logs", formatted)) {
                                copiedNotice = true
                            }
                        }
                    },
                    enabled = filteredLogs.isNotEmpty(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy sanitized logs",
                        tint = if (filteredLogs.isNotEmpty()) TapBotColors.TextSecondary else TapBotColors.TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onClearLogs,
                    enabled = logs.isNotEmpty(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear logs",
                        tint = if (logs.isNotEmpty()) TapBotColors.TextSecondary else TapBotColors.TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Level filter pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ConsoleLogFilter.entries) { filter ->
                val isSelected = selectedFilter == filter
                val count = when (filter) {
                    ConsoleLogFilter.ALL -> logs.size
                    ConsoleLogFilter.INFO -> logs.count { it.level == LogLevel.INFO }
                    ConsoleLogFilter.WARN -> logs.count { it.level == LogLevel.WARN }
                    ConsoleLogFilter.ERROR -> logs.count { it.level == LogLevel.ERROR }
                }
                val activeColor = when (filter) {
                    ConsoleLogFilter.ALL -> TapBotColors.BrandCyan
                    ConsoleLogFilter.INFO -> TapBotColors.SuccessText
                    ConsoleLogFilter.WARN -> TapBotColors.WarningText
                    ConsoleLogFilter.ERROR -> TapBotColors.ErrorText
                }
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Tab) { selectedFilter = filter }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                            .background(
                                if (isSelected) activeColor.copy(alpha = 0.16f) else TapBotColors.SurfaceElevated
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) activeColor.copy(alpha = 0.65f) else TapBotColors.Border,
                                shape = RoundedCornerShape(TapBotColors.RadiusPill)
                            )
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val labelText = if (count > 0) "${filter.label} ($count)" else filter.label
                        Text(
                            text = labelText,
                            color = if (isSelected) activeColor else TapBotColors.TextSecondary,
                            style = TabularMonoStyle.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Terminal Output Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 260.dp)
                .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                .background(Color(0xFF03060C))
                .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusMd))
                .padding(12.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                val emptyText = if (logs.isEmpty()) {
                    "❯ Standby. Start the bot to stream live runtime logs."
                } else {
                    "❯ No ${selectedFilter.label} entries captured yet."
                }
                Text(
                    text = emptyText,
                    color = TapBotColors.TextMuted,
                    style = TabularMonoStyle.copy(fontSize = 12.sp),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(filteredLogs) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = timeFormat.format(Date(entry.timestamp)),
                                color = TapBotColors.TextMuted,
                                style = TabularMonoStyle.copy(fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val levelColor = when (entry.level) {
                                LogLevel.ERROR -> TapBotColors.ErrorText
                                LogLevel.WARN -> TapBotColors.WarningText
                                LogLevel.INFO -> TapBotColors.SuccessText
                                LogLevel.DEBUG -> TapBotColors.Info
                            }
                            Text(
                                text = "[${entry.level.name.take(4)}]",
                                color = levelColor,
                                style = TabularMonoStyle.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.message,
                                color = when (entry.level) {
                                    LogLevel.ERROR -> TapBotColors.ErrorText
                                    LogLevel.WARN -> TapBotColors.WarningText
                                    else -> TapBotColors.TextPrimary
                                },
                                style = TabularMonoStyle.copy(fontSize = 12.sp, lineHeight = 16.sp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String): Boolean {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        clipboard != null
    } catch (_: Exception) {
        false
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360)
@Composable
private fun LiveConsoleLogViewerPreview() {
    val sampleLogs = listOf(
        BotLogEntry(
            botId = "bot_commander",
            timestamp = System.currentTimeMillis() - 12_000L,
            level = LogLevel.INFO,
            tag = "Runtime",
            message = "Polling started as @tapbot_cmd_bot"
        ),
        BotLogEntry(
            botId = "bot_commander",
            timestamp = System.currentTimeMillis() - 8_000L,
            level = LogLevel.WARN,
            tag = "Power",
            message = "Device Doze whitelist inactive; background delays possible"
        ),
        BotLogEntry(
            botId = "bot_commander",
            timestamp = System.currentTimeMillis() - 3_000L,
            level = LogLevel.ERROR,
            tag = "Net",
            message = "Transient read timeout; backing off 2s"
        )
    )
    TapBotTheme {
        Column(modifier = Modifier.background(TapBotColors.Canvas).padding(16.dp)) {
            LiveConsoleLogViewer(logs = sampleLogs, onClearLogs = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360)
@Composable
private fun LiveConsoleLogViewerEmptyPreview() {
    TapBotTheme {
        Column(modifier = Modifier.background(TapBotColors.Canvas).padding(16.dp)) {
            LiveConsoleLogViewer(logs = emptyList(), onClearLogs = {})
        }
    }
}
