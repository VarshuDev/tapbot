package com.tapbot.feature.botdetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.ui.catalog.BotCommandCatalog
import com.tapbot.core.ui.components.BotIcon
import com.tapbot.core.ui.theme.TabularMonoStyle
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.resolveBotVisualIdentity
import com.tapbot.feature.botdetail.SimulatorChatMessage
import com.tapbot.feature.botdetail.SimulatorSender
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppBotSimulatorModal(
    bot: BotMetadata,
    messages: List<SimulatorChatMessage>,
    isProcessing: Boolean,
    onSendMessage: (String) -> Unit,
    onClearMessages: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val visualIdentity = resolveBotVisualIdentity(bot.id, bot.category, bot.name)

    val quickCommands = remember(bot) {
        val preview = BotCommandCatalog.getPreviewCommands(bot.id, null, bot.category, bot.name, limit = 4)
        val defaults = listOf("/start", "/help", "/status")
        (defaults + preview).distinct()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TapBotColors.SurfacePrimary,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(topStart = TapBotColors.RadiusLg, topEnd = TapBotColors.RadiusLg),
        dragHandle = null,
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TapBotColors.SurfacePrimary)
        ) {
            // Header Bar
            SimulatorHeader(
                bot = bot,
                onClear = onClearMessages,
                onClose = onDismiss
            )

            // Chat Messages Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(TapBotColors.Canvas)
            ) {
                if (messages.isEmpty()) {
                    SimulatorEmptyState(
                        botName = bot.name,
                        onQuickCommand = { onSendMessage(it) }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            SimulatorMessageBubble(message = msg, botName = bot.name)
                        }

                        if (isProcessing) {
                            item(key = "simulator_typing_indicator") {
                                TypingIndicatorRow(botName = bot.name)
                            }
                        }
                    }
                }
            }

            // Quick Command Chips Row
            QuickCommandChipsRow(
                commands = quickCommands,
                onCommandSelected = { cmd ->
                    onSendMessage(cmd)
                }
            )

            // Input Bar
            SimulatorInputBar(
                inputText = inputText,
                onInputChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        onSendMessage(text)
                    }
                },
                isProcessing = isProcessing
            )
        }
    }
}

@Composable
private fun SimulatorHeader(
    bot: BotMetadata,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TapBotColors.SurfaceElevated)
            .border(width = 1.dp, color = TapBotColors.BorderSubtle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            BotIcon(
                botId = bot.id,
                category = bot.category,
                name = bot.name,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${bot.name} Sandbox",
                    color = TapBotColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TapBotColors.SuccessText)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Android ART Local Sandbox",
                        color = TapBotColors.SuccessText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Chat",
                    tint = TapBotColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Simulator",
                    tint = TapBotColors.TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SimulatorEmptyState(
    botName: String,
    onQuickCommand: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(TapBotColors.BrandCyanSurface)
                .border(1.dp, TapBotColors.BrandCyanBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = TapBotColors.BrandCyan,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Interactive Bot Sandbox",
            color = TapBotColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Test $botName commands locally on Android ART without needing Telegram. Tap /start below to begin!",
            color = TapBotColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("/start", "/help", "/status").forEach { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                        .background(TapBotColors.PrimaryCobalt.copy(alpha = 0.2f))
                        .border(1.dp, TapBotColors.PrimaryCobalt, RoundedCornerShape(TapBotColors.RadiusPill))
                        .clickable { onQuickCommand(cmd) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cmd,
                        color = TapBotColors.BrandCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulatorMessageBubble(
    message: SimulatorChatMessage,
    botName: String
) {
    val isUser = message.sender == SimulatorSender.USER
    val isSystem = message.sender == SimulatorSender.SYSTEM
    val timeFormatted = remember(message.timestampMs) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestampMs))
    }

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.text,
                color = TapBotColors.TextMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                    .background(TapBotColors.SurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
            ) {
                Text(
                    text = botName,
                    color = TapBotColors.BrandCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeFormatted,
                    color = TapBotColors.TextMuted,
                    style = TabularMonoStyle.copy(fontSize = 10.sp)
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.horizontalGradient(
                            listOf(TapBotColors.PrimaryCobalt, TapBotColors.PrimaryBlue)
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(TapBotColors.SurfaceElevated, TapBotColors.SurfaceInteractive)
                        )
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) TapBotColors.BrandCyan.copy(alpha = 0.3f) else TapBotColors.BorderSubtle,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else TapBotColors.TextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        if (isUser) {
            Text(
                text = timeFormatted,
                color = TapBotColors.TextMuted,
                style = TabularMonoStyle.copy(fontSize = 10.sp),
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun TypingIndicatorRow(botName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 2.dp,
            color = TapBotColors.BrandCyan
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$botName is typing...",
            color = TapBotColors.TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun QuickCommandChipsRow(
    commands: List<String>,
    onCommandSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TapBotColors.SurfacePrimary)
            .border(width = 1.dp, color = TapBotColors.BorderSubtle)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        commands.forEach { cmd ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                    .background(TapBotColors.SurfaceElevated)
                    .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusPill))
                    .clickable { onCommandSelected(cmd) }
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    text = cmd,
                    color = TapBotColors.BrandCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SimulatorInputBar(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    isProcessing: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TapBotColors.SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Type a command (e.g. /help)...", color = TapBotColors.TextMuted, fontSize = 13.sp)
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            singleLine = true,
            shape = RoundedCornerShape(TapBotColors.RadiusPill),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TapBotColors.SurfacePrimary,
                unfocusedContainerColor = TapBotColors.SurfacePrimary,
                focusedBorderColor = TapBotColors.BrandCyan,
                unfocusedBorderColor = TapBotColors.Border,
                focusedTextColor = TapBotColors.TextPrimary,
                unfocusedTextColor = TapBotColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (inputText.isNotBlank() && !isProcessing) TapBotColors.PrimaryCobalt else TapBotColors.SurfaceInteractive
                )
                .clickable(enabled = inputText.isNotBlank() && !isProcessing) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send",
                tint = if (inputText.isNotBlank() && !isProcessing) Color.White else TapBotColors.TextMuted,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
