package com.tapbot.feature.botdetail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotRunState
import com.tapbot.core.security.SecurityWindowManager
import com.tapbot.feature.botdetail.components.CredentialInputSection
import com.tapbot.feature.botdetail.components.LiveConsoleLogViewer
import com.tapbot.feature.botdetail.ui.BotHubTheme
import com.tapbot.feature.botdetail.ui.BotIcon
import com.tapbot.feature.botdetail.ui.PrimaryButton
import com.tapbot.feature.botdetail.ui.RunStateBadge
import com.tapbot.feature.botdetail.ui.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotDetailScreen(
    viewModel: BotDetailViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Enforce hardware-backed window security (FLAG_SECURE) to prevent screenshots,
    // screen recording, and OS task switcher previews of secret credentials
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        SecurityWindowManager.setSecureFlag(activity, true)
        onDispose {
            SecurityWindowManager.setSecureFlag(activity, false)
        }
    }

    Scaffold(
        containerColor = BotHubTheme.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? BotDetailUiState.Success)?.bot?.name ?: "Bot Details",
                        color = BotHubTheme.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BotHubTheme.PrimaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BotHubTheme.Background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is BotDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = BotHubTheme.PrimaryBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Loading bot metadata...",
                                color = BotHubTheme.SecondaryText,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                is BotDetailUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
                                .background(BotHubTheme.PrimarySurface)
                                .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusLg))
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Failed to load bot details",
                                color = BotHubTheme.Error,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = BotHubTheme.SecondaryText,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is BotDetailUiState.Success -> {
                    BotDetailContent(
                        state = state,
                        onInstallClick = viewModel::installBot,
                        onCredentialChanged = viewModel::updateCredential,
                        onSaveCredentials = { viewModel.saveCredentials() },
                        onDeleteCredentials = viewModel::deleteCredentials,
                        onConfigureToggle = viewModel::setConfiguring,
                        onStartBot = viewModel::startBot,
                        onStopBot = viewModel::stopBot,
                        onClearLogs = viewModel::clearLogs
                    )
                }
            }
        }
    }
}

@Composable
private fun BotDetailContent(
    state: BotDetailUiState.Success,
    onInstallClick: () -> Unit,
    onCredentialChanged: (String, String) -> Unit,
    onSaveCredentials: () -> Unit,
    onDeleteCredentials: () -> Unit,
    onConfigureToggle: (Boolean) -> Unit,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onClearLogs: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Card: Squircle BotIcon, Title, Version/Category, Description, Tags
        BotHeroCard(bot = state.bot)

        // 2. Specifications Card: Runtime, Package Size, Min App Version, Permissions
        BotSpecificationsCard(bot = state.bot)

        // 3. Install Flow Section (If not installed)
        if (!state.isInstalled) {
            InstallActionCard(
                isInstalling = state.isInstalling,
                progress = state.installProgress,
                error = state.installError,
                onInstallClick = onInstallClick
            )
        } else {
            // 4. Installed Status Banner
            InstalledStatusBanner(installedVersion = state.installedVersion ?: state.bot.version)

            // 5. Credentials Configuration Section
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

            // 6. Runner Execution Control Card
            RunnerExecutionCard(
                runState = state.runState,
                onStartBot = onStartBot,
                onStopBot = onStopBot
            )

            // 6b. Background Execution & OEM Battery Saver Guidance
            BackgroundExecutionNoticeCard()

            // 7. Live Console Log Viewer
            LiveConsoleLogViewer(
                logs = state.logs,
                onClearLogs = onClearLogs
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun BotHeroCard(bot: BotMetadata) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusLg))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BotIcon(
            name = bot.name,
            category = bot.category,
            size = 72.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = bot.name,
            color = BotHubTheme.PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "v${bot.version} • ${bot.category} • ${bot.packageInfo.runtimeType}",
            color = BotHubTheme.SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = bot.description,
            color = BotHubTheme.PrimaryText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )

        if (bot.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bot.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
                            .background(BotHubTheme.ElevatedSurface)
                            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusPill))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag,
                            color = BotHubTheme.SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BotSpecificationsCard(bot: BotMetadata) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusLg))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = BotHubTheme.PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Package Specifications",
                color = BotHubTheme.PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SpecificationRow(label = "Runtime Engine", value = bot.packageInfo.runtimeType)
        SpecificationRow(
            label = "Package Size",
            value = if (bot.packageInfo.packageSizeBytes > 0) formatBytes(bot.packageInfo.packageSizeBytes) else "Lightweight (< 1 MB)"
        )
        SpecificationRow(label = "Min App Version", value = "v${bot.minimumAppVersion}+")
        SpecificationRow(label = "Permissions Required", value = bot.permissionsRequired.joinToString(", "))
        val lastUpdated = bot.updatedAt
        if (!lastUpdated.isNullOrBlank()) {
            SpecificationRow(label = "Last Updated", value = lastUpdated.take(10))
        }

        val notes = bot.releaseNotes
        if (!notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Release Notes (v${bot.version}):",
                color = BotHubTheme.PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = notes,
                color = BotHubTheme.SecondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SpecificationRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = BotHubTheme.SecondaryText, fontSize = 13.sp)
        Text(text = value, color = BotHubTheme.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InstallActionCard(
    isInstalling: Boolean,
    progress: Float,
    error: String?,
    onInstallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusLg))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Install Locally",
            color = BotHubTheme.PrimaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Downloads the bot package from secure Cloudflare storage, verifies SHA-256 integrity, and prepares on-device execution.",
            color = BotHubTheme.SecondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (isInstalling) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = BotHubTheme.PrimaryBlue,
                    trackColor = BotHubTheme.SecondarySurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Downloading & Verifying: ${(progress * 100).toInt()}%",
                    color = BotHubTheme.PrimaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            if (error != null) {
                Text(
                    text = error,
                    color = BotHubTheme.Error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            PrimaryButton(
                text = "Add Bot",
                onClick = onInstallClick,
                icon = Icons.Default.Download
            )
        }
    }
}

@Composable
private fun InstalledStatusBanner(installedVersion: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.SuccessSurface)
            .border(1.dp, BotHubTheme.SuccessBorder, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = BotHubTheme.Success,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Installed & Ready",
                color = BotHubTheme.Success,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Package version v$installedVersion verified and installed locally.",
                color = BotHubTheme.SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RunnerExecutionCard(
    runState: BotRunState,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit
) {
    val isRunning = runState is BotRunState.Running

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusLg))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Runner Execution",
                    color = BotHubTheme.PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (runState) {
                        is BotRunState.Running -> "Running in Foreground Service (Polls: ${runState.pollCount})"
                        is BotRunState.Starting -> "Starting connection..."
                        is BotRunState.Error -> "Error: ${runState.message}"
                        else -> "Stopped"
                    },
                    color = if (isRunning) BotHubTheme.Success else BotHubTheme.SecondaryText,
                    fontSize = 12.sp
                )
            }

            RunStateBadge(
                runState = runState
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isRunning) {
            Button(
                onClick = onStopBot,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(BotHubTheme.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotHubTheme.ErrorSurface,
                    contentColor = BotHubTheme.Error
                ),
                border = BorderStroke(1.dp, BotHubTheme.ErrorBorder)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("STOP BOT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
private fun BackgroundExecutionNoticeCard() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.ElevatedSurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            tint = BotHubTheme.Info,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Background Execution Notice",
                color = BotHubTheme.PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "For uninterrupted 24/7 operation, disable battery optimization for BotHub in Android system settings.",
                color = BotHubTheme.SecondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            Text(
                text = "Open Battery Settings →",
                color = BotHubTheme.PrimaryBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}
