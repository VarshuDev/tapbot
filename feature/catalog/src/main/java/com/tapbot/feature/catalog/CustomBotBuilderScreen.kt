package com.tapbot.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.feature.catalog.ui.BotHubTheme
import com.tapbot.feature.catalog.ui.MonospaceBadge
import com.tapbot.feature.catalog.ui.PrimaryButton
import com.tapbot.feature.catalog.ui.premiumCardBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBotBuilderScreen(
    viewModel: CustomBotBuilderViewModel,
    onBackClick: () -> Unit,
    onBotCreated: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showToken by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedBotId) {
        val createdId = state.savedBotId
        if (createdId != null) {
            onBotCreated(createdId)
        }
    }

    Scaffold(
        containerColor = BotHubTheme.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Custom Bot Builder",
                            color = BotHubTheme.PrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No-Code Slash Commands & Webhooks",
                            color = BotHubTheme.SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BotHubTheme.PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BotHubTheme.Background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BotHubTheme.Background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
                        .background(BotHubTheme.PrimarySurface)
                        .premiumCardBorder()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = BotHubTheme.PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bot Identity & Keystore Token",
                            color = BotHubTheme.PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    BuilderTextField(
                        label = "Bot Name",
                        value = state.botName,
                        onValueChange = viewModel::updateBotName,
                        placeholder = "e.g. Deploy Webhook Bot"
                    )

                    BuilderTextField(
                        label = "Short Summary",
                        value = state.summary,
                        onValueChange = viewModel::updateSummary,
                        placeholder = "e.g. Triggers server webhooks from Telegram commands"
                    )

                    OutlinedTextField(
                        value = state.telegramToken,
                        onValueChange = viewModel::updateTelegramToken,
                        label = { Text("Telegram Bot Token (@BotFather)", color = BotHubTheme.SecondaryText, fontSize = 12.sp) },
                        placeholder = { Text("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ", color = BotHubTheme.MutedText, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle token visibility",
                                    tint = BotHubTheme.SecondaryText
                                )
                            }
                        },
                        textStyle = TextStyle(color = BotHubTheme.PrimaryText, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BotHubTheme.PrimaryBlue,
                            unfocusedBorderColor = BotHubTheme.Border
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BotHubTheme.Success,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Encrypted in Android Hardware Keystore (AES-256-GCM)",
                            color = BotHubTheme.Success,
                            fontSize = 11.sp
                        )
                    }

                    BuilderTextField(
                        label = "/start Welcome Greeting",
                        value = state.welcomeMessage,
                        onValueChange = viewModel::updateWelcomeMessage,
                        placeholder = "Hello {user}! Send /help for commands.",
                        singleLine = false
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Command & Webhook Rules",
                            color = BotHubTheme.PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Variables: {user} {user_id} {chat_id} {args} {date} {webhook_response}",
                            color = BotHubTheme.SecondaryText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    MonospaceBadge(text = "${state.rules.size} rule(s)")
                }
            }

            itemsIndexed(state.rules, key = { _, r -> r.id }) { idx, rule ->
                CommandRuleEditorCard(
                    index = idx,
                    rule = rule,
                    canDelete = state.rules.size > 1,
                    onUpdate = { transform -> viewModel.updateRule(rule.id, transform) },
                    onDelete = { viewModel.removeRule(rule.id) }
                )
            }

            item {
                OutlinedButton(
                    onClick = viewModel::addRule,
                    shape = RoundedCornerShape(BotHubTheme.RadiusMd),
                    border = BorderStroke(1.dp, BotHubTheme.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = BotHubTheme.PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Command Rule", color = BotHubTheme.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (state.errorMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
                            .background(BotHubTheme.ErrorSurface)
                            .border(1.dp, BotHubTheme.ErrorBorder, RoundedCornerShape(BotHubTheme.RadiusMd))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.errorMessage!!,
                            color = BotHubTheme.Error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                PrimaryButton(
                    text = "Save & Register Custom Bot",
                    isLoading = state.isSaving,
                    onClick = viewModel::saveCustomBot
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CommandRuleEditorCard(
    index: Int,
    rule: DraftCommandRule,
    canDelete: Boolean,
    onUpdate: ((DraftCommandRule) -> DraftCommandRule) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rule #${index + 1}",
                color = BotHubTheme.Info,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove rule",
                        tint = BotHubTheme.Error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        BuilderTextField(
            label = "Trigger Command (e.g. /deploy or /ping)",
            value = rule.command,
            onValueChange = { v -> onUpdate { it.copy(command = v) } },
            placeholder = "/hello",
            isMonospace = true
        )

        BuilderTextField(
            label = "Description (shown in /help)",
            value = rule.description,
            onValueChange = { v -> onUpdate { it.copy(description = v) } },
            placeholder = "Optional command description"
        )

        BuilderTextField(
            label = "Response Template",
            value = rule.responseTemplate,
            onValueChange = { v -> onUpdate { it.copy(responseTemplate = v) } },
            placeholder = "Hello {user}! Webhook output: {webhook_response}",
            singleLine = false
        )

        BuilderTextField(
            label = "Optional HTTP Webhook URL",
            value = rule.webhookUrl,
            onValueChange = { v -> onUpdate { it.copy(webhookUrl = v) } },
            placeholder = "https://example.com/hook?q={args}",
            isMonospace = true
        )

        if (rule.webhookUrl.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("HTTP Method:", color = BotHubTheme.SecondaryText, fontSize = 12.sp)
                listOf(CustomHttpMethod.GET, CustomHttpMethod.POST).forEach { method ->
                    val selected = rule.webhookMethod == method
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
                            .background(if (selected) BotHubTheme.PrimaryBlue else BotHubTheme.ElevatedSurface)
                            .border(1.dp, if (selected) Color.Transparent else BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusPill))
                            .clickable { onUpdate { it.copy(webhookMethod = method) } }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = method.name,
                            color = if (selected) Color.White else BotHubTheme.SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        BuilderTextField(
            label = "Optional Inline Button Label",
            value = rule.buttonLabel,
            onValueChange = { v -> onUpdate { it.copy(buttonLabel = v) } },
            placeholder = "e.g. Run Again"
        )
    }
}

@Composable
private fun BuilderTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    isMonospace: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = BotHubTheme.SecondaryText, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = BotHubTheme.MutedText, fontSize = 12.sp) },
        singleLine = singleLine,
        textStyle = TextStyle(
            color = BotHubTheme.PrimaryText,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontSize = 13.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BotHubTheme.PrimaryBlue,
            unfocusedBorderColor = BotHubTheme.Border
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
