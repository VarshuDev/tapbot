package com.tapbot.feature.botdetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.model.LogLevel
import com.tapbot.feature.botdetail.ui.BotHubTheme
import com.tapbot.feature.botdetail.ui.premiumCardBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveConsoleLogViewer(
    logs: List<BotLogEntry>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder()
            .padding(16.dp)
    ) {
        // Terminal Window Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // macOS / Linux Terminal Window Controls (Red, Amber, Green dots)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF10B981)))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = BotHubTheme.Success,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Live Execution Console",
                    color = BotHubTheme.PrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logs.isNotEmpty()) {
                    Text(
                        text = "${logs.size} lines",
                        color = BotHubTheme.MutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear logs",
                        tint = BotHubTheme.SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terminal Output Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 130.dp, max = 240.dp)
                .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
                .background(Color(0xFF02040A))
                .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = "❯ Standby. Start the bot to stream telemetry and runtime logs.",
                    color = BotHubTheme.MutedText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(logs) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = timeFormat.format(Date(entry.timestamp)),
                                color = BotHubTheme.MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Level indicator badge
                            val levelColor = when (entry.level) {
                                LogLevel.ERROR -> BotHubTheme.Error
                                LogLevel.WARN -> BotHubTheme.Warning
                                LogLevel.INFO -> BotHubTheme.Success
                                LogLevel.DEBUG -> BotHubTheme.Info
                            }
                            Text(
                                text = "[${entry.level.name.take(4)}]",
                                color = levelColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.message,
                                color = when (entry.level) {
                                    LogLevel.ERROR -> BotHubTheme.Error
                                    LogLevel.WARN -> BotHubTheme.Warning
                                    else -> BotHubTheme.PrimaryText
                                },
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
