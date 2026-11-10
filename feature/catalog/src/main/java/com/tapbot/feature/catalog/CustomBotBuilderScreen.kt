package com.tapbot.feature.catalog

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.core.ui.components.MonospaceBadge
import com.tapbot.core.ui.components.PrimaryButton
import com.tapbot.core.ui.motion.TapBotMotion
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder

private enum class ActiveTemplateTargetKind {
    WELCOME,
    RESPONSE,
    WEBHOOK_BODY
}

private data class ActiveTemplateTarget(
    val kind: ActiveTemplateTargetKind = ActiveTemplateTargetKind.RESPONSE,
    val ruleId: String? = null
)

@Composable
fun CustomBotBuilderScreen(
    viewModel: CustomBotBuilderViewModel,
    onBackClick: () -> Unit,
    onBotCreated: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.savedBotId) {
        val createdId = state.savedBotId
        if (createdId != null) {
            onBotCreated(createdId)
        }
    }

    CustomBotBuilderContent(
        state = state,
        onBackClick = onBackClick,
        onBotNameChanged = viewModel::updateBotName,
        onSummaryChanged = viewModel::updateSummary,
        onTokenChanged = viewModel::updateTelegramToken,
        onWelcomeChanged = viewModel::updateWelcomeMessage,
        onApplyBlueprint = viewModel::applyBlueprint,
        onAddRule = viewModel::addRule,
        onUpdateRule = viewModel::updateRule,
        onRemoveRule = viewModel::removeRule,
        onSaveClick = viewModel::saveCustomBot
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CustomBotBuilderContent(
    state: CustomBotBuilderUiState,
    onBackClick: () -> Unit,
    onBotNameChanged: (String) -> Unit,
    onSummaryChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onWelcomeChanged: (String) -> Unit,
    onApplyBlueprint: (String) -> Unit,
    onAddRule: () -> Unit,
    onUpdateRule: (String, (DraftCommandRule) -> DraftCommandRule) -> Unit,
    onRemoveRule: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    val context = LocalContext.current
    var showToken by remember { mutableStateOf(false) }
    var activeTarget by remember(state.editingBotId) {
        mutableStateOf(
            ActiveTemplateTarget(
                kind = ActiveTemplateTargetKind.RESPONSE,
                ruleId = state.rules.firstOrNull()?.id
            )
        )
    }

    val listState = rememberLazyListState()
    var previousRuleCount by remember { mutableIntStateOf(state.rules.size) }
    var ruleBaseIndex by remember { mutableIntStateOf(3) }

    LaunchedEffect(state.rules.size) {
        if (state.rules.size > previousRuleCount) {
            val newlyAddedIndex = ruleBaseIndex + state.rules.size - 1
            if (newlyAddedIndex >= 0) {
                listState.animateScrollToItem(newlyAddedIndex)
            }
            activeTarget = ActiveTemplateTarget(
                kind = ActiveTemplateTargetKind.RESPONSE,
                ruleId = state.rules.lastOrNull()?.id
            )
        }
        previousRuleCount = state.rules.size
    }

    val targetLabel = when (activeTarget.kind) {
        ActiveTemplateTargetKind.WELCOME -> "/start Welcome"
        ActiveTemplateTargetKind.WEBHOOK_BODY -> {
            val idx = state.rules.indexOfFirst { it.id == activeTarget.ruleId }
            if (idx >= 0) "Rule #${idx + 1} POST Body" else "Webhook Body"
        }
        ActiveTemplateTargetKind.RESPONSE -> {
            val idx = state.rules.indexOfFirst { it.id == activeTarget.ruleId }
            if (idx >= 0) "Rule #${idx + 1} Reply" else "Rule #1 Reply"
        }
    }

    fun insertVariable(token: String) {
        when (activeTarget.kind) {
            ActiveTemplateTargetKind.WELCOME -> {
                val updated = if (state.welcomeMessage.isEmpty() || state.welcomeMessage.endsWith(" ")) {
                    "${state.welcomeMessage}$token"
                } else {
                    "${state.welcomeMessage} $token"
                }
                onWelcomeChanged(updated)
            }
            ActiveTemplateTargetKind.WEBHOOK_BODY -> {
                val targetId = activeTarget.ruleId ?: state.rules.firstOrNull()?.id ?: return
                onUpdateRule(targetId) { rule ->
                    val updated = rule.webhookBodyTemplate + token
                    rule.copy(webhookBodyTemplate = updated)
                }
            }
            ActiveTemplateTargetKind.RESPONSE -> {
                val targetId = activeTarget.ruleId ?: state.rules.firstOrNull()?.id ?: return
                onUpdateRule(targetId) { rule ->
                    val updated = if (rule.responseTemplate.isEmpty() || rule.responseTemplate.endsWith(" ")) {
                        "${rule.responseTemplate}$token"
                    } else {
                        "${rule.responseTemplate} $token"
                    }
                    rule.copy(responseTemplate = updated)
                }
            }
        }
    }

    Scaffold(
        containerColor = TapBotColors.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.isEditing) "Edit Workflow Bot" else "Custom Bot Builder",
                            color = TapBotColors.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.isEditing) {
                                "Update rules, templates & webhooks"
                            } else {
                                "No-Code Slash Commands & Webhooks"
                            },
                            color = TapBotColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TapBotColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TapBotColors.Canvas),
                modifier = Modifier.border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(0.dp))
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
                .background(TapBotColors.Canvas),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!state.isEditing) {
                    item(key = "blueprints") {
                        StarterBlueprintsStrip(
                            selectedId = state.selectedBlueprintId,
                            onSelectBlueprint = onApplyBlueprint
                        )
                    }
                }

                item(key = "identity_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
                            .background(TapBotColors.SurfacePrimary)
                            .premiumCardBorder()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = TapBotColors.BrandCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bot Identity & Keystore Token",
                                color = TapBotColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        BuilderTextField(
                            label = "Bot Name",
                            value = state.botName,
                            onValueChange = onBotNameChanged,
                            placeholder = "e.g. Deploy Webhook Bot"
                        )

                        BuilderTextField(
                            label = "Short Summary",
                            value = state.summary,
                            onValueChange = onSummaryChanged,
                            placeholder = "e.g. Triggers server webhooks from Telegram commands"
                        )

                        OutlinedTextField(
                            value = state.telegramToken,
                            onValueChange = onTokenChanged,
                            label = {
                                Text("Telegram Bot Token (@BotFather)", color = TapBotColors.TextSecondary, fontSize = 12.sp)
                            },
                            placeholder = {
                                Text("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ", color = TapBotColors.TextMuted, fontSize = 12.sp)
                            },
                            singleLine = true,
                            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            readClipboardText(context)?.let { pasted ->
                                                if (pasted.isNotBlank()) onTokenChanged(pasted.trim())
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste token from clipboard",
                                            tint = TapBotColors.BrandCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showToken = !showToken },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showToken) "Hide token" else "Show token",
                                            tint = TapBotColors.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            textStyle = TextStyle(
                                color = TapBotColors.TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TapBotColors.BrandCyan,
                                unfocusedBorderColor = TapBotColors.Border,
                                focusedContainerColor = TapBotColors.SurfaceElevated,
                                unfocusedContainerColor = TapBotColors.SurfaceElevated
                            ),
                            shape = RoundedCornerShape(TapBotColors.RadiusMd),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TapBotColors.SuccessText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Encrypted in Keystore (AES-256-GCM)",
                                    color = TapBotColors.SuccessText,
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clickable(role = Role.Button) {
                                        openExternalUrl(context, "https://t.me/BotFather")
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                                        .background(TapBotColors.BrandCyanSurface)
                                        .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "@BotFather",
                                        color = TapBotColors.BrandCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = "Open @BotFather",
                                        tint = TapBotColors.BrandCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        BuilderTextField(
                            label = "/start Welcome Greeting",
                            value = state.welcomeMessage,
                            onValueChange = onWelcomeChanged,
                            placeholder = "Hello {user}! Send /help for commands.",
                            singleLine = false,
                            modifier = Modifier.onFocusChanged { focus ->
                                if (focus.isFocused) {
                                    activeTarget = ActiveTemplateTarget(kind = ActiveTemplateTargetKind.WELCOME)
                                }
                            }
                        )
                    }
                }

                item(key = "variables_bar") {
                    ruleBaseIndex = if (state.isEditing) 2 else 3
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
                            .background(TapBotColors.SurfacePrimary)
                            .premiumCardBorder()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Command & Webhook Rules",
                                    color = TapBotColors.TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap chip to insert into: $targetLabel",
                                    color = TapBotColors.BrandCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            MonospaceBadge(text = "${state.rules.size} rule(s)")
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CustomBotBuilderViewModel.TEMPLATE_VARIABLES.forEach { variable ->
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clickable(role = Role.Button) { insertVariable(variable) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                                            .background(TapBotColors.SurfaceElevated)
                                            .border(1.dp, TapBotColors.BorderFocus, RoundedCornerShape(TapBotColors.RadiusSm))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = variable,
                                            color = TapBotColors.Info,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                itemsIndexed(state.rules, key = { _, r -> r.id }) { idx, rule ->
                    CommandRuleEditorCard(
                        index = idx,
                        rule = rule,
                        canDelete = state.rules.size > 1,
                        onUpdate = { transform -> onUpdateRule(rule.id, transform) },
                        onDelete = { onRemoveRule(rule.id) },
                        onResponseFocused = {
                            activeTarget = ActiveTemplateTarget(
                                kind = ActiveTemplateTargetKind.RESPONSE,
                                ruleId = rule.id
                            )
                        },
                        onWebhookBodyFocused = {
                            activeTarget = ActiveTemplateTarget(
                                kind = ActiveTemplateTargetKind.WEBHOOK_BODY,
                                ruleId = rule.id
                            )
                        }
                    )
                }

                item(key = "add_rule_btn") {
                    OutlinedButton(
                        onClick = onAddRule,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(TapBotColors.RadiusMd),
                        border = BorderStroke(1.dp, TapBotColors.BrandCyanBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TapBotColors.SurfaceElevated,
                            contentColor = TapBotColors.TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TapBotColors.BrandCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Command Rule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                item(key = "sample_chat_preview") {
                    val previewRule = state.rules.firstOrNull { it.id == activeTarget.ruleId }
                        ?: state.rules.firstOrNull()
                    if (previewRule != null) {
                        SampleTelegramChatPreviewCard(
                            botName = state.botName.ifBlank { "Custom Bot" },
                            rule = previewRule
                        )
                    }
                }

                if (state.errorMessage != null) {
                    item(key = "error_banner") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                                .background(TapBotColors.ErrorSurface)
                                .border(1.dp, TapBotColors.ErrorBorder, RoundedCornerShape(TapBotColors.RadiusMd))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = state.errorMessage,
                                color = TapBotColors.ErrorText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item(key = "save_button") {
                    PrimaryButton(
                        text = if (state.isEditing) "Save Workflow Changes" else "Save & Register Custom Bot",
                        isLoading = state.isSaving,
                        onClick = onSaveClick
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StarterBlueprintsStrip(
    selectedId: String?,
    onSelectBlueprint: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = TapBotColors.BrandCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Starter Blueprints",
                color = TapBotColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Pre-fill commands in 1 tap",
                color = TapBotColors.TextMuted,
                fontSize = 11.sp
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(CustomBotBuilderViewModel.STARTER_BLUEPRINTS, key = { it.id }) { blueprint ->
                val isSelected = selectedId == blueprint.id
                Column(
                    modifier = Modifier
                        .width(172.dp)
                        .heightIn(min = 78.dp)
                        .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                        .background(if (isSelected) TapBotColors.BrandCyanSurface else TapBotColors.SurfacePrimary)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) TapBotColors.BrandCyan else TapBotColors.Border,
                            shape = RoundedCornerShape(TapBotColors.RadiusMd)
                        )
                        .clickable(role = Role.Button) { onSelectBlueprint(blueprint.id) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = blueprint.title,
                        color = if (isSelected) TapBotColors.BrandCyan else TapBotColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = blueprint.tagline,
                        color = TapBotColors.TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    onDelete: () -> Unit,
    onResponseFocused: () -> Unit,
    onWebhookBodyFocused: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .animateContentSize(animationSpec = TapBotMotion.ContentSpring)
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
                color = TapBotColors.BrandCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove rule #${index + 1}",
                        tint = TapBotColors.ErrorText,
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                .background(TapBotColors.SurfaceElevated)
                .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                .clickable(role = Role.Switch) { onUpdate { it.copy(matchPrefix = !it.matchPrefix) } }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (rule.matchPrefix) "Prefix Match (Accepts {args})" else "Exact Match Only",
                    color = TapBotColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (rule.matchPrefix) {
                        "Matches '${rule.command.ifBlank { "/cmd" }} anything'"
                    } else {
                        "Only matches '${rule.command.ifBlank { "/cmd" }}' with no extra words"
                    },
                    color = TapBotColors.TextSecondary,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = rule.matchPrefix,
                onCheckedChange = { checked -> onUpdate { it.copy(matchPrefix = checked) } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TapBotColors.BrandCyanInk,
                    checkedTrackColor = TapBotColors.BrandCyan,
                    uncheckedThumbColor = TapBotColors.TextSecondary,
                    uncheckedTrackColor = TapBotColors.SurfaceInteractive
                )
            )
        }

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
            singleLine = false,
            modifier = Modifier.onFocusChanged { if (it.isFocused) onResponseFocused() }
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
                Text("Method:", color = TapBotColors.TextSecondary, fontSize = 12.sp)
                listOf(CustomHttpMethod.GET, CustomHttpMethod.POST).forEach { method ->
                    val selected = rule.webhookMethod == method
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.RadioButton) {
                                onUpdate { it.copy(webhookMethod = method) }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                                .background(if (selected) TapBotColors.PrimaryCobalt else TapBotColors.SurfaceElevated)
                                .border(
                                    1.dp,
                                    if (selected) TapBotColors.BrandCyan.copy(alpha = 0.65f) else TapBotColors.Border,
                                    RoundedCornerShape(TapBotColors.RadiusPill)
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method.name,
                                color = if (selected) Color.White else TapBotColors.TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (rule.webhookMethod == CustomHttpMethod.POST) {
                BuilderTextField(
                    label = "Optional JSON POST Body Template",
                    value = rule.webhookBodyTemplate,
                    onValueChange = { v -> onUpdate { it.copy(webhookBodyTemplate = v) } },
                    placeholder = "{\"user\":\"{username}\",\"args\":\"{args}\"}",
                    singleLine = false,
                    isMonospace = true,
                    modifier = Modifier.onFocusChanged { if (it.isFocused) onWebhookBodyFocused() }
                )
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
private fun SampleTelegramChatPreviewCard(
    botName: String,
    rule: DraftCommandRule
) {
    val cmd = rule.command.trim().ifBlank { "/hello" }
    val sampleUserMessage = if (rule.matchPrefix) "$cmd production" else cmd
    val sampleReply = resolveSampleTemplate(rule.responseTemplate.ifBlank { "No response template entered yet." })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder(borderColorTop = TapBotColors.WarningBorder)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = TapBotColors.BrandCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Telegram Chat Preview",
                    color = TapBotColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                    .background(TapBotColors.WarningSurface)
                    .border(1.dp, TapBotColors.WarningBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SAMPLE PREVIEW",
                    color = TapBotColors.WarningText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                .background(TapBotColors.Canvas)
                .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(TapBotColors.PrimaryCobalt)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = sampleUserMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Column(horizontalAlignment = Alignment.Start) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                        .background(TapBotColors.SurfaceElevated)
                        .border(1.dp, TapBotColors.Border, RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = botName,
                            color = TapBotColors.BrandCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = sampleReply,
                            color = TapBotColors.TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                if (rule.buttonLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                            .background(TapBotColors.SurfaceInteractive)
                            .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(TapBotColors.RadiusSm))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rule.buttonLabel,
                            color = TapBotColors.BrandCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun resolveSampleTemplate(template: String): String {
    return template
        .replace("{user}", "Alex")
        .replace("{username}", "@alex_dev")
        .replace("{user_id}", "4829103")
        .replace("{chat_id}", "-10019283")
        .replace("{args}", "production")
        .replace("{date}", "2026-11-10 12:00 UTC")
        .replace("{webhook_response}", "200 OK {\"status\":\"ok\"}")
}

@Composable
private fun BuilderTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isMonospace: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TapBotColors.TextSecondary, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = TapBotColors.TextMuted, fontSize = 12.sp) },
        singleLine = singleLine,
        textStyle = TextStyle(
            color = TapBotColors.TextPrimary,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontSize = 13.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TapBotColors.BrandCyan,
            unfocusedBorderColor = TapBotColors.Border,
            focusedContainerColor = TapBotColors.SurfaceElevated,
            unfocusedContainerColor = TapBotColors.SurfaceElevated
        ),
        shape = RoundedCornerShape(TapBotColors.RadiusMd),
        modifier = modifier.fillMaxWidth()
    )
}

private fun readClipboardText(context: Context): String? {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
    } catch (_: Exception) {
        null
    }
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Exception) {}
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, heightDp = 820)
@Composable
private fun CustomBotBuilderCreatePreview() {
    val bp = CustomBotBuilderViewModel.STARTER_BLUEPRINTS[1]
    val state = CustomBotBuilderUiState(
        selectedBlueprintId = bp.id,
        botName = bp.defaultBotName,
        summary = bp.summary,
        welcomeMessage = bp.welcomeMessage,
        rules = bp.rules
    )
    TapBotTheme {
        CustomBotBuilderContent(
            state = state,
            onBackClick = {},
            onBotNameChanged = {},
            onSummaryChanged = {},
            onTokenChanged = {},
            onWelcomeChanged = {},
            onApplyBlueprint = {},
            onAddRule = {},
            onUpdateRule = { _, _ -> },
            onRemoveRule = {},
            onSaveClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 840, heightDp = 760)
@Composable
private fun CustomBotBuilderEditTabletPreview() {
    val bp = CustomBotBuilderViewModel.STARTER_BLUEPRINTS[0]
    val state = CustomBotBuilderUiState(
        editingBotId = "bot_custom_support",
        botName = bp.defaultBotName,
        summary = bp.summary,
        welcomeMessage = bp.welcomeMessage,
        rules = bp.rules
    )
    TapBotTheme {
        CustomBotBuilderContent(
            state = state,
            onBackClick = {},
            onBotNameChanged = {},
            onSummaryChanged = {},
            onTokenChanged = {},
            onWelcomeChanged = {},
            onApplyBlueprint = {},
            onAddRule = {},
            onUpdateRule = { _, _ -> },
            onRemoveRule = {},
            onSaveClick = {}
        )
    }
}
