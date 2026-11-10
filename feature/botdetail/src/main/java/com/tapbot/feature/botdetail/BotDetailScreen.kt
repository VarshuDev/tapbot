package com.tapbot.feature.botdetail

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tapbot.core.model.BotCredentialSpec
import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotPackageInfo
import com.tapbot.core.model.BotRunState
import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.LogLevel
import com.tapbot.core.runner.telemetry.DeviceTelemetryProvider
import com.tapbot.core.security.SecurityWindowManager
import com.tapbot.core.ui.catalog.BotCommandCatalog
import com.tapbot.core.ui.catalog.BotCommandEntry
import com.tapbot.core.ui.components.BotIcon
import com.tapbot.core.ui.components.MonospaceBadge
import com.tapbot.core.ui.components.PrimaryButton
import com.tapbot.core.ui.components.RunStateBadge
import com.tapbot.core.ui.components.SecondaryButton
import com.tapbot.core.ui.motion.TapBotMotion
import com.tapbot.core.ui.motion.rememberReducedMotion
import com.tapbot.core.ui.theme.TabularMonoStyle
import com.tapbot.core.ui.theme.TabularNumberStyle
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder
import com.tapbot.core.ui.theme.resolveBotVisualIdentity
import com.tapbot.feature.botdetail.components.CredentialInputSection
import com.tapbot.feature.botdetail.components.LiveConsoleLogViewer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotDetailScreen(
    viewModel: BotDetailViewModel,
    onBackClick: () -> Unit,
    onEditCustomBot: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isDozeWhitelisted by remember {
        mutableStateOf(
            DeviceTelemetryProvider(if (isPreview) null else context).getBatteryTelemetry().isDozeWhitelisted
        )
    }

    if (!isPreview) {
        DisposableEffect(Unit) {
            val activity = context as? Activity
            SecurityWindowManager.setSecureFlag(activity, true)
            onDispose {
                SecurityWindowManager.setSecureFlag(activity, false)
            }
        }

        DisposableEffect(lifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isDozeWhitelisted = DeviceTelemetryProvider(context).getBatteryTelemetry().isDozeWhitelisted
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    val successState = uiState as? BotDetailUiState.Success
    val canEditCustom = successState != null &&
        onEditCustomBot != null &&
        isCustomBot(successState.bot.id, successState.customDefinition != null)

    Scaffold(
        containerColor = TapBotColors.Canvas,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = successState?.bot?.name ?: "Bot Node",
                                color = TapBotColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = when {
                                !successState?.verifiedBotUsername.isNullOrBlank() ->
                                    "@${successState?.verifiedBotUsername}"
                                successState != null ->
                                    "${successState.bot.category} • v${successState.bot.version}"
                                else -> null
                            }
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    color = if (!successState?.verifiedBotUsername.isNullOrBlank()) {
                                        TapBotColors.BrandCyan
                                    } else {
                                        TapBotColors.TextSecondary
                                    },
                                    style = TabularMonoStyle.copy(fontSize = 11.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TapBotColors.TextPrimary
                            )
                        }
                    },
                    actions = {
                        if (canEditCustom && successState != null) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .heightIn(min = 48.dp)
                                    .clickable(role = Role.Button) {
                                        onEditCustomBot(successState.bot.id)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                                        .background(TapBotColors.BrandCyanSurface)
                                        .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = TapBotColors.BrandCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Edit Flow",
                                        color = TapBotColors.BrandCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TapBotColors.Canvas
                    )
                )
                HorizontalDivider(color = TapBotColors.BorderSubtle, thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is BotDetailUiState.Loading -> {
                    BotDetailLoadingSkeleton()
                }
                is BotDetailUiState.Error -> {
                    BotDetailErrorState(
                        message = state.message,
                        onBackClick = onBackClick
                    )
                }
                is BotDetailUiState.Success -> {
                    BotDetailContent(
                        state = state,
                        isDozeWhitelisted = isDozeWhitelisted,
                        onInstallClick = viewModel::installBot,
                        onCredentialChanged = viewModel::updateCredential,
                        onSaveCredentials = { viewModel.saveCredentials() },
                        onDeleteCredentials = viewModel::deleteCredentials,
                        onConfigureToggle = viewModel::setConfiguring,
                        onVerifyToken = viewModel::verifyToken,
                        onStartBot = viewModel::startBot,
                        onStopBot = viewModel::stopBot,
                        onClearLogs = viewModel::clearLogs,
                        onRequestDozeWhitelist = { requestDozeWhitelist(context) },
                        onOpenTelegramBot = { username -> openTelegramBot(context, username) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BotDetailLoadingSkeleton() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TapBotColors.RadiusLg))
                    .background(TapBotColors.SurfacePrimary)
                    .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusLg))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(TapBotColors.SurfaceElevated)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TapBotColors.SurfaceInteractive)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(TapBotColors.SurfaceElevated)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                            .background(TapBotColors.SurfacePrimary)
                            .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(TapBotColors.RadiusLg))
                    .background(TapBotColors.SurfacePrimary)
                    .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusLg))
            )
        }
    }
}

@Composable
private fun BotDetailErrorState(
    message: String,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(TapBotColors.RadiusLg))
                .background(TapBotColors.SurfacePrimary)
                .premiumCardBorder()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = TapBotColors.ErrorText,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Failed to load bot details",
                color = TapBotColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = TapBotColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            SecondaryButton(
                text = "Back to Store",
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun BotDetailContent(
    state: BotDetailUiState.Success,
    isDozeWhitelisted: Boolean,
    onInstallClick: () -> Unit,
    onCredentialChanged: (String, String) -> Unit,
    onSaveCredentials: () -> Unit,
    onDeleteCredentials: () -> Unit,
    onConfigureToggle: (Boolean) -> Unit,
    onVerifyToken: () -> Unit,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onClearLogs: () -> Unit,
    onRequestDozeWhitelist: () -> Unit,
    onOpenTelegramBot: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    var installSectionY by remember { mutableIntStateOf(0) }
    var credentialsSectionY by remember { mutableIntStateOf(0) }
    var verifySectionY by remember { mutableIntStateOf(0) }
    var runnerSectionY by remember { mutableIntStateOf(0) }

    fun scrollTo(y: Int) {
        coroutineScope.launch {
            if (reducedMotion) {
                scrollState.scrollTo(y)
            } else {
                scrollState.animateScrollTo(y)
            }
        }
    }

    val tokenKey = remember(state.bot.requiredCredentials) {
        state.bot.requiredCredentials
            .firstOrNull { isTelegramTokenKey(it.key) }
            ?.key
    }
    val hasTokenSpec = tokenKey != null
    val hasCredentialsConfigured = remember(state.bot.requiredCredentials, state.credentials) {
        val required = state.bot.requiredCredentials.filter { it.isRequired }
        if (required.isEmpty()) true
        else required.all { !state.credentials[it.key].isNullOrBlank() }
    }
    val hasTokenValue = tokenKey != null && !state.credentials[tokenKey].isNullOrBlank()
    val isVerified = if (hasTokenSpec) !state.verifiedBotUsername.isNullOrBlank() else hasCredentialsConfigured

    val commands = remember(state.bot.id, state.customDefinition, state.bot.category, state.bot.name) {
        BotCommandCatalog.getCommandsForBot(
            botId = state.bot.id,
            customDefinition = state.customDefinition,
            category = state.bot.category,
            name = state.bot.name
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 900.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card
            BotHeroCard(
                bot = state.bot,
                verifiedUsername = state.verifiedBotUsername
            )

            // 2. Interactive 4-Step Setup Progress Rail
            SetupProgressRail(
                isInstalled = state.isInstalled,
                hasCredentials = hasCredentialsConfigured,
                isVerified = isVerified,
                isRunning = state.runState is BotRunState.Running,
                onStepClick = { step ->
                    val targetY = when (step) {
                        0 -> installSectionY
                        1 -> if (state.isInstalled) credentialsSectionY else installSectionY
                        2 -> if (state.isInstalled) verifySectionY else installSectionY
                        else -> if (state.isInstalled) runnerSectionY else installSectionY
                    }
                    scrollTo(targetY)
                }
            )

            // 3. Bento Specifications Card
            BotSpecificationsBentoCard(bot = state.bot)

            // 4. Command Cheatsheet (always visible so users inspect commands pre/post install)
            if (commands.isNotEmpty()) {
                CommandCheatsheetCard(commands = commands)
            }

            // 5. Install OR Runtime Control Flow
            if (!state.isInstalled) {
                Box(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        installSectionY = coords.positionInParent().y.toInt()
                    }
                ) {
                    InstallActionCard(
                        bot = state.bot,
                        isInstalling = state.isInstalling,
                        progress = state.installProgress,
                        error = state.installError,
                        onInstallClick = onInstallClick
                    )
                }
            } else {
                Box(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        installSectionY = coords.positionInParent().y.toInt()
                    }
                ) {
                    InstalledStatusBanner(
                        installedVersion = state.installedVersion ?: state.bot.version
                    )
                }

                Box(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        credentialsSectionY = coords.positionInParent().y.toInt()
                    }
                ) {
                    CredentialInputSection(
                        specs = state.bot.requiredCredentials,
                        values = state.credentials,
                        errors = state.credentialErrors,
                        generalError = state.generalCredentialError,
                        isConfiguring = state.isConfiguring,
                        onValueChanged = onCredentialChanged,
                        onSaveClick = onSaveCredentials,
                        onDeleteClick = onDeleteCredentials,
                        onConfigureToggle = onConfigureToggle,
                        isSaving = state.isSavingCredentials,
                        saveSuccess = state.credentialSaveSuccess
                    )
                }

                if (hasTokenSpec) {
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            verifySectionY = coords.positionInParent().y.toInt()
                        }
                    ) {
                        TelegramConnectionCard(
                            hasToken = hasTokenValue,
                            verifiedBotUsername = state.verifiedBotUsername,
                            isVerifying = state.isVerifyingToken,
                            verifyError = state.tokenVerifyError,
                            onVerifyClick = onVerifyToken,
                            onOpenTelegramBot = onOpenTelegramBot
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            verifySectionY = coords.positionInParent().y.toInt()
                        }
                    )
                }

                Box(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        runnerSectionY = coords.positionInParent().y.toInt()
                    }
                ) {
                    RunnerExecutionCard(
                        runState = state.runState,
                        verifiedUsername = state.verifiedBotUsername,
                        reducedMotion = reducedMotion,
                        onStartBot = onStartBot,
                        onStopBot = onStopBot,
                        onOpenTelegramBot = onOpenTelegramBot
                    )
                }

                BackgroundExecutionNoticeCard(
                    isDozeWhitelisted = isDozeWhitelisted,
                    onRequestDozeWhitelist = onRequestDozeWhitelist
                )

                LiveConsoleLogViewer(
                    logs = state.logs,
                    onClearLogs = onClearLogs
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BotHeroCard(
    bot: BotMetadata,
    verifiedUsername: String?
) {
    val identity = resolveBotVisualIdentity(botId = bot.id, category = bot.category, name = bot.name)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(
                Brush.verticalGradient(
                    listOf(
                        identity.surfaceTint.copy(alpha = 0.55f),
                        TapBotColors.SurfacePrimary
                    )
                )
            )
            .premiumCardBorder(
                borderColorTop = identity.accentColor.copy(alpha = 0.45f),
                borderColorBottom = TapBotColors.BorderSubtle
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BotIcon(
            name = bot.name,
            category = bot.category,
            botId = bot.id,
            size = 76.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = bot.name,
            color = TapBotColors.TextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                    .background(identity.surfaceTint)
                    .border(1.dp, identity.borderTint, RoundedCornerShape(TapBotColors.RadiusPill))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = identity.categoryLabel.uppercase(),
                    color = identity.accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            MonospaceBadge(
                text = "v${bot.version}",
                color = TapBotColors.TextSecondary,
                backgroundColor = TapBotColors.SurfaceElevated
            )

            if (!verifiedUsername.isNullOrBlank()) {
                MonospaceBadge(
                    text = "@$verifiedUsername",
                    color = TapBotColors.BrandCyan,
                    backgroundColor = TapBotColors.BrandCyanSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = bot.description.ifBlank { bot.summary },
            color = TapBotColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )

        if (bot.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bot.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                            .background(TapBotColors.SurfaceElevated)
                            .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusPill))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            color = TapBotColors.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private data class SetupStepSpec(
    val number: Int,
    val label: String,
    val isDone: Boolean,
    val isActive: Boolean
)

@Composable
private fun SetupProgressRail(
    isInstalled: Boolean,
    hasCredentials: Boolean,
    isVerified: Boolean,
    isRunning: Boolean,
    onStepClick: (Int) -> Unit
) {
    val steps = listOf(
        SetupStepSpec(1, "Install", isDone = isInstalled, isActive = !isInstalled),
        SetupStepSpec(2, "Keystore", isDone = isInstalled && hasCredentials, isActive = isInstalled && !hasCredentials),
        SetupStepSpec(3, "Verify", isDone = isInstalled && isVerified, isActive = isInstalled && hasCredentials && !isVerified),
        SetupStepSpec(4, "Live", isDone = isRunning, isActive = isInstalled && isVerified && !isRunning)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = "NODE ACTIVATION PIPELINE",
            color = TapBotColors.TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            steps.forEachIndexed { index, step ->
                val bg = when {
                    step.isDone -> TapBotColors.SuccessSurface
                    step.isActive -> TapBotColors.BrandCyanSurface
                    else -> TapBotColors.SurfaceElevated
                }
                val borderColor = when {
                    step.isDone -> TapBotColors.SuccessBorder
                    step.isActive -> TapBotColors.BrandCyanBorder
                    else -> TapBotColors.Border
                }
                val fg = when {
                    step.isDone -> TapBotColors.SuccessText
                    step.isActive -> TapBotColors.BrandCyan
                    else -> TapBotColors.TextMuted
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                        .clickable(role = Role.Button) { onStepClick(index) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                            .background(bg)
                            .border(1.dp, borderColor, RoundedCornerShape(TapBotColors.RadiusSm))
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (step.isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = fg,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Text(
                                text = "${step.number}.",
                                color = fg,
                                style = TabularNumberStyle.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = step.label,
                            color = fg,
                            fontSize = 11.sp,
                            fontWeight = if (step.isDone || step.isActive) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BotSpecificationsBentoCard(bot: BotMetadata) {
    val context = LocalContext.current
    var copiedHash by remember { mutableStateOf(false) }

    LaunchedEffect(copiedHash) {
        if (copiedHash) {
            delay(1800)
            copiedHash = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = TapBotColors.BrandCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Package Specifications",
                    color = TapBotColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                    .background(TapBotColors.SuccessSurface)
                    .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = TapBotColors.SuccessText,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "VERIFIED",
                    color = TapBotColors.SuccessText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoTile(
                modifier = Modifier.weight(1f),
                label = "RUNTIME ENGINE",
                value = bot.packageInfo.runtimeType.ifBlank { "native_art" },
                subtext = "On-device coroutine loop",
                icon = Icons.Default.Code,
                accentColor = TapBotColors.BrandCyan
            )

            val sizeStr = if (bot.packageInfo.packageSizeBytes > 0) {
                formatBytes(bot.packageInfo.packageSizeBytes)
            } else {
                "< 1.0 MB"
            }
            BentoTile(
                modifier = Modifier.weight(1f),
                label = "PAYLOAD SIZE",
                value = sizeStr,
                subtext = "Local package manifest",
                icon = Icons.Default.Download,
                isMonospace = true,
                accentColor = TapBotColors.Info
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoTile(
                modifier = Modifier.weight(1f),
                label = "COMPATIBILITY",
                value = "v${bot.version}",
                subtext = "Min App v${bot.minimumAppVersion}+",
                icon = Icons.Default.Smartphone,
                isMonospace = true,
                accentColor = TapBotColors.WarningText
            )

            val permText = if (bot.permissionsRequired.isEmpty()) "Isolated" else bot.permissionsRequired.first()
            val permSubtext = if (bot.permissionsRequired.size > 1) "+${bot.permissionsRequired.size - 1} more" else "Keystore & Net"
            BentoTile(
                modifier = Modifier.weight(1f),
                label = "CAPABILITIES",
                value = permText,
                subtext = permSubtext,
                icon = Icons.Default.Security,
                accentColor = TapBotColors.SuccessText
            )
        }

        val hash = bot.packageInfo.sha256Checksum
        if (hash.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                    .background(TapBotColors.SurfaceElevated)
                    .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusMd))
                    .clickable(role = Role.Button) {
                        copyText(context, "SHA-256 Checksum", hash)
                        copiedHash = true
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TapBotColors.SuccessSurface)
                        .border(1.dp, TapBotColors.SuccessBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TapBotColors.SuccessText,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SHA-256 CHECKSUM (TAP TO COPY)",
                        color = TapBotColors.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hash.length > 24) "${hash.take(12)}...${hash.takeLast(12)}" else hash,
                        color = TapBotColors.TextPrimary,
                        style = TabularMonoStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Text(
                    text = if (copiedHash) "COPIED" else "MATCH",
                    color = TapBotColors.SuccessText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val notes = bot.releaseNotes
        if (!notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                    .background(TapBotColors.SurfaceElevated)
                    .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusMd))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CHANGELOG",
                        color = TapBotColors.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    MonospaceBadge(text = "v${bot.version}")
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notes,
                    color = TapBotColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun BentoTile(
    label: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = TapBotColors.BrandCyan,
    isMonospace: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(TapBotColors.SurfaceElevated)
            .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusMd))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = TapBotColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = value,
            color = TapBotColors.TextPrimary,
            style = if (isMonospace) {
                TabularMonoStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
            } else {
                TabularNumberStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtext,
            color = TapBotColors.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CommandCheatsheetCard(
    commands: List<BotCommandEntry>
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(true) }
    var copiedCommand by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedCommand) {
        if (copiedCommand != null) {
            delay(1800)
            copiedCommand = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .animateContentSize(animationSpec = TapBotMotion.ContentSpring)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = TapBotColors.BrandCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Supported Commands",
                        color = TapBotColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any command to copy for Telegram chat",
                        color = TapBotColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                MonospaceBadge(
                    text = "${commands.size} cmds",
                    color = TapBotColors.BrandCyan,
                    backgroundColor = TapBotColors.BrandCyanSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse commands" else "Expand commands",
                    tint = TapBotColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (copiedCommand != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                    .background(TapBotColors.SuccessSurface)
                    .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusSm))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = TapBotColors.SuccessText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Copied \"$copiedCommand\" to clipboard",
                    color = TapBotColors.SuccessText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                commands.forEach { entry ->
                    val cmdToCopy = entry.command.substringBefore(" ").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                            .background(TapBotColors.SurfaceElevated)
                            .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                            .clickable(role = Role.Button) {
                                copyText(context, "Bot Command", cmdToCopy)
                                copiedCommand = cmdToCopy
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.command,
                                    color = TapBotColors.BrandCyan,
                                    style = TabularMonoStyle.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = entry.category.uppercase(),
                                    color = TapBotColors.TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = entry.summary,
                                color = TapBotColors.TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy $cmdToCopy",
                            tint = TapBotColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallActionCard(
    bot: BotMetadata,
    isInstalling: Boolean,
    progress: Float,
    error: String?,
    onInstallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Deploy On Device",
            color = TapBotColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Direct local runner. Zero middleman servers — tokens stay locked inside Android hardware Keystore.",
            color = TapBotColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                .background(TapBotColors.SurfaceElevated)
                .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrustPill(text = "SHA-256 Check")
            TrustPill(text = "Zero Cloud Logs")
            TrustPill(text = "Keystore AES")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isInstalling) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = TapBotColors.BrandCyan,
                    trackColor = TapBotColors.SurfaceInteractive
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Downloading & verifying SHA-256...",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = TapBotColors.BrandCyan,
                        style = TabularMonoStyle.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        } else {
            if (error != null) {
                Text(
                    text = error,
                    color = TapBotColors.ErrorText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            PrimaryButton(
                text = "INSTALL ${bot.name.uppercase()} LOCALLY",
                onClick = onInstallClick,
                icon = Icons.Default.Download
            )
        }
    }
}

@Composable
private fun TrustPill(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = TapBotColors.SuccessText,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = TapBotColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InstalledStatusBanner(installedVersion: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(TapBotColors.SuccessSurface)
            .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusMd))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = TapBotColors.SuccessText,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Installed & Verified",
                color = TapBotColors.SuccessText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Payload v$installedVersion verified and local runtime staged.",
                color = TapBotColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TelegramConnectionCard(
    hasToken: Boolean,
    verifiedBotUsername: String?,
    isVerifying: Boolean,
    verifyError: String?,
    onVerifyClick: () -> Unit,
    onOpenTelegramBot: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .animateContentSize(animationSpec = TapBotMotion.ContentSpring)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = if (!verifiedBotUsername.isNullOrBlank()) TapBotColors.SuccessText else TapBotColors.BrandCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Telegram Bot Handshake",
                        color = TapBotColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verify token via getMe before starting your runner.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (!verifiedBotUsername.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                    .background(TapBotColors.SuccessSurface)
                    .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusMd))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = TapBotColors.SuccessText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Verified as @$verifiedBotUsername",
                        color = TapBotColors.SuccessText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Telegram Bot API handshake succeeded.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!verifyError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TapBotColors.RadiusSm))
                    .background(TapBotColors.ErrorSurface)
                    .border(1.dp, TapBotColors.ErrorBorder, RoundedCornerShape(TapBotColors.RadiusSm))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = TapBotColors.ErrorText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = verifyError,
                    color = TapBotColors.ErrorText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onVerifyClick,
                enabled = hasToken && !isVerifying,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasToken) TapBotColors.BrandCyanBorder else TapBotColors.BorderSubtle
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = TapBotColors.BrandCyanSurface.copy(alpha = 0.5f),
                    contentColor = TapBotColors.BrandCyan,
                    disabledContainerColor = TapBotColors.SurfaceElevated,
                    disabledContentColor = TapBotColors.TextMuted
                )
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = TapBotColors.BrandCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking getMe...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        text = if (verifiedBotUsername != null) "Re-verify Token" else "Verify Token with Telegram",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!verifiedBotUsername.isNullOrBlank()) {
                Button(
                    onClick = { onOpenTelegramBot(verifiedBotUsername) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.PrimaryCobalt,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Open in Telegram",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RunnerExecutionCard(
    runState: BotRunState,
    verifiedUsername: String?,
    reducedMotion: Boolean,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onOpenTelegramBot: (String) -> Unit
) {
    val isRunning = runState is BotRunState.Running
    val isLive = isRunning || runState is BotRunState.Starting
    val pulseAlpha = if (!reducedMotion && isLive) rememberDetailLivePulseAlpha() else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder(
                borderColorTop = if (isRunning) TapBotColors.SuccessBorder else TapBotColors.BorderFocus,
                borderColorBottom = TapBotColors.BorderSubtle
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Runner Execution",
                    color = TapBotColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = when (runState) {
                        is BotRunState.Running -> "Foreground Service active • Polls: ${runState.pollCount}"
                        is BotRunState.Starting -> "Initializing Telegram long-polling loop..."
                        is BotRunState.PausedNoNetwork -> "Paused waiting for internet (${runState.reason})"
                        is BotRunState.Error -> "Failure: ${runState.message}"
                        else -> "Standby — ready to run locally"
                    },
                    color = when {
                        isRunning -> TapBotColors.SuccessText
                        runState is BotRunState.Error -> TapBotColors.ErrorText
                        runState is BotRunState.PausedNoNetwork -> TapBotColors.WarningText
                        else -> TapBotColors.TextSecondary
                    },
                    style = if (isRunning) TabularMonoStyle.copy(fontSize = 12.sp) else TabularNumberStyle.copy(fontSize = 12.sp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            RunStateBadge(
                runState = runState,
                reducedMotion = reducedMotion,
                pulseAlpha = pulseAlpha
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isRunning || runState is BotRunState.Starting || runState is BotRunState.PausedNoNetwork) {
            Button(
                onClick = onStopBot,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TapBotColors.ErrorSurface,
                    contentColor = TapBotColors.ErrorText
                ),
                border = BorderStroke(1.dp, TapBotColors.ErrorBorder)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("STOP BOT", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }

            if (isRunning && !verifiedUsername.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onOpenTelegramBot(verifiedUsername) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusMd),
                    border = BorderStroke(1.dp, TapBotColors.BrandCyanBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = TapBotColors.BrandCyanSurface.copy(alpha = 0.4f),
                        contentColor = TapBotColors.BrandCyan
                    )
                ) {
                    Text(
                        text = "Chat with @$verifiedUsername on Telegram",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        } else {
            PrimaryButton(
                text = "START BOT",
                onClick = onStartBot,
                icon = Icons.Default.PlayArrow
            )
        }
    }
}

@Composable
private fun rememberDetailLivePulseAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "botDetailPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = TapBotMotion.StandardEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "botDetailDotAlpha"
    )
    return alpha
}

@Composable
private fun BackgroundExecutionNoticeCard(
    isDozeWhitelisted: Boolean,
    onRequestDozeWhitelist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(if (isDozeWhitelisted) TapBotColors.SuccessSurface else TapBotColors.WarningSurface)
            .border(
                width = 1.dp,
                color = if (isDozeWhitelisted) TapBotColors.SuccessBorder else TapBotColors.WarningBorder,
                shape = RoundedCornerShape(TapBotColors.RadiusMd)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            tint = if (isDozeWhitelisted) TapBotColors.SuccessText else TapBotColors.WarningText,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isDozeWhitelisted) {
                    "Battery Optimization Unrestricted"
                } else {
                    "Background Execution Notice"
                },
                color = TapBotColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isDozeWhitelisted) {
                    "TapBot is exempt from Android Doze throttling for reliable 24/7 Telegram polling."
                } else {
                    "For uninterrupted 24/7 Telegram polling, exempt TapBot from Android battery optimization."
                },
                color = TapBotColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = if (isDozeWhitelisted) 0.dp else 8.dp)
            )
            if (!isDozeWhitelisted) {
                OutlinedButton(
                    onClick = onRequestDozeWhitelist,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusPill),
                    border = BorderStroke(1.dp, TapBotColors.WarningBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = TapBotColors.SurfaceElevated,
                        contentColor = TapBotColors.WarningText
                    )
                ) {
                    Text(
                        text = "Disable Battery Optimization →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun isCustomBot(botId: String, hasCustomDef: Boolean): Boolean {
    return hasCustomDef || botId.startsWith("bot_custom_") || botId.startsWith("custom_")
}

private fun isTelegramTokenKey(key: String): Boolean {
    return key.equals("bot_token", ignoreCase = true) ||
        key.equals("telegram_bot_token", ignoreCase = true)
}

private fun copyText(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    } catch (_: Exception) {}
}

private fun openTelegramBot(context: Context, username: String) {
    val clean = username.trim().removePrefix("@")
    if (clean.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$clean")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun requestDozeWhitelist(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}
    }
    try {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {}
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}

private fun previewCommanderBot(): BotMetadata = BotMetadata(
    id = "bot_commander",
    name = "TapBot Commander",
    summary = "Flagship device watchdog & remote orchestrator.",
    description = "Turn your host phone into an always-on Telegram node with live battery/RAM HUD, fleet management, and phone-finder alarm controls.",
    author = "TapBot Core Team",
    version = "2.3.0",
    iconUrl = "",
    category = "Automation",
    tags = listOf("telemetry", "fleet", "watchdog", "apk"),
    requiredCredentials = listOf(BotCredentialSpec.TELEGRAM_BOT_TOKEN),
    packageInfo = BotPackageInfo(
        packageUrl = "",
        sha256Checksum = "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3e24b7c9d1035f3c8b2e5a101",
        runtimeType = "native_art",
        packageSizeBytes = 640_000L
    ),
    releaseNotes = "Added /batteryguide and instant redacted log streaming.",
    isFeatured = true
)

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, heightDp = 760)
@Composable
private fun BotDetailPreInstallPhonePreview() {
    TapBotTheme {
        BotDetailContent(
            state = BotDetailUiState.Success(
                bot = previewCommanderBot(),
                isInstalled = false
            ),
            isDozeWhitelisted = false,
            onInstallClick = {},
            onCredentialChanged = { _, _ -> },
            onSaveCredentials = {},
            onDeleteCredentials = {},
            onConfigureToggle = {},
            onVerifyToken = {},
            onStartBot = {},
            onStopBot = {},
            onClearLogs = {},
            onRequestDozeWhitelist = {},
            onOpenTelegramBot = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, heightDp = 760)
@Composable
private fun BotDetailInstalledVerifiedPhonePreview() {
    TapBotTheme {
        BotDetailContent(
            state = BotDetailUiState.Success(
                bot = previewCommanderBot(),
                isInstalled = true,
                installedVersion = "2.3.0",
                credentials = mapOf("bot_token" to "123456:StoredDemoToken"),
                isConfiguring = false,
                verifiedBotUsername = "tapbot_cmd_bot",
                runState = BotRunState.Running(
                    startedAt = System.currentTimeMillis() - 60_000L,
                    pollCount = 37L
                ),
                logs = listOf(
                    BotLogEntry(
                        botId = "bot_commander",
                        timestamp = System.currentTimeMillis() - 9000L,
                        level = LogLevel.INFO,
                        tag = "Runner",
                        message = "Verified @tapbot_cmd_bot via getMe"
                    ),
                    BotLogEntry(
                        botId = "bot_commander",
                        timestamp = System.currentTimeMillis() - 2000L,
                        level = LogLevel.INFO,
                        tag = "Dispatch",
                        message = "Handled /status from owner"
                    )
                )
            ),
            isDozeWhitelisted = false,
            onInstallClick = {},
            onCredentialChanged = { _, _ -> },
            onSaveCredentials = {},
            onDeleteCredentials = {},
            onConfigureToggle = {},
            onVerifyToken = {},
            onStartBot = {},
            onStopBot = {},
            onClearLogs = {},
            onRequestDozeWhitelist = {},
            onOpenTelegramBot = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 840, heightDp = 640)
@Composable
private fun BotDetailTabletPreview() {
    val customDef = CustomBotDefinition(
        id = "bot_custom_ops_1",
        name = "Ops Webhook Relay",
        summary = "Triggers server webhooks on /deploy",
        rules = listOf(
            CommandRule(id = "r1", command = "/deploy", description = "Trigger prod pipeline", responseTemplate = "Triggered!")
        )
    )
    TapBotTheme {
        BotDetailContent(
            state = BotDetailUiState.Success(
                bot = customDef.toBotMetadata(),
                isInstalled = true,
                installedVersion = "1.0.0",
                credentials = mapOf("bot_token" to "stored"),
                isConfiguring = false,
                verifiedBotUsername = "ops_relay_bot",
                customDefinition = customDef
            ),
            isDozeWhitelisted = true,
            onInstallClick = {},
            onCredentialChanged = { _, _ -> },
            onSaveCredentials = {},
            onDeleteCredentials = {},
            onConfigureToggle = {},
            onVerifyToken = {},
            onStartBot = {},
            onStopBot = {},
            onClearLogs = {},
            onRequestDozeWhitelist = {},
            onOpenTelegramBot = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, fontScale = 1.3f)
@Composable
private fun BotDetailLargeFontPreview() {
    TapBotTheme {
        BotDetailContent(
            state = BotDetailUiState.Success(
                bot = previewCommanderBot(),
                isInstalled = true,
                installedVersion = "2.3.0",
                isConfiguring = true,
                credentials = emptyMap(),
                tokenVerifyError = "Unauthorized: Invalid bot token"
            ),
            isDozeWhitelisted = false,
            onInstallClick = {},
            onCredentialChanged = { _, _ -> },
            onSaveCredentials = {},
            onDeleteCredentials = {},
            onConfigureToggle = {},
            onVerifyToken = {},
            onStartBot = {},
            onStopBot = {},
            onClearLogs = {},
            onRequestDozeWhitelist = {},
            onOpenTelegramBot = {}
        )
    }
}
