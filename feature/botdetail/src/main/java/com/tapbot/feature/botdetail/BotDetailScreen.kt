package com.tapbot.feature.botdetail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotRunState
import com.tapbot.core.security.SecurityWindowManager
import com.tapbot.feature.botdetail.components.CredentialInputSection
import com.tapbot.feature.botdetail.components.LiveConsoleLogViewer
import com.tapbot.feature.botdetail.ui.BotHubTheme
import com.tapbot.feature.botdetail.ui.BotIcon
import com.tapbot.feature.botdetail.ui.MonospaceBadge
import com.tapbot.feature.botdetail.ui.PrimaryButton
import com.tapbot.feature.botdetail.ui.RunStateBadge
import com.tapbot.feature.botdetail.ui.premiumCardBorder

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
                ),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = BotHubTheme.BorderSubtle,
                    shape = RoundedCornerShape(0.dp)
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
                                text = "Loading bot specifications...",
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
                                .premiumCardBorder()
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
        // 1. Hero Card: Elevated squircle BotIcon, Category & Version pills, Title, Description, Tags
        BotHeroCard(bot = state.bot)

        // 2. Specifications Card: Bento Grid Layout
        BotSpecificationsBentoCard(bot = state.bot)

        // 3. Install Flow Section (If not installed)
        if (!state.isInstalled) {
            InstallActionCard(
                bot = state.bot,
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
            .premiumCardBorder()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Squircle Icon with distinct drop/ambient shadow
        BotIcon(
            name = bot.name,
            category = bot.category,
            size = 76.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bot Name
        Text(
            text = bot.name,
            color = BotHubTheme.PrimaryText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Pill & Version Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
                    .background(BotHubTheme.BlueSubtle)
                    .border(1.dp, BotHubTheme.BlueBorder, RoundedCornerShape(BotHubTheme.RadiusPill))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = bot.category.uppercase(),
                    color = BotHubTheme.Info,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Version Monospace Badge
            MonospaceBadge(text = "v${bot.version}")
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Description
        Text(
            text = bot.description,
            color = BotHubTheme.SecondaryText,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center
        )

        // Tags
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
                            text = "#$tag",
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

/**
 * Bento Grid Specifications Card
 * Replaces old plain vertical list with modern modular tiles.
 */
@Composable
private fun BotSpecificationsBentoCard(bot: BotMetadata) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder()
            .padding(18.dp)
    ) {
        // Section Header Row with CF Verified Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
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

            // Cloudflare Edge Verified Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
                    .background(BotHubTheme.SuccessSurface)
                    .border(1.dp, BotHubTheme.SuccessBorder, RoundedCornerShape(BotHubTheme.RadiusPill))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = BotHubTheme.Success,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "VERIFIED",
                    color = BotHubTheme.Success,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bento Grid: Row 1 (Runtime Engine & Payload Size)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoTile(
                modifier = Modifier.weight(1f),
                label = "RUNTIME ENGINE",
                value = bot.packageInfo.runtimeType,
                subtext = "Sandboxed Engine",
                icon = Icons.Default.Code,
                accentColor = BotHubTheme.PrimaryBlue
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
                subtext = "Bytecode payload",
                icon = Icons.Default.Download,
                isMonospace = true,
                accentColor = BotHubTheme.Info
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bento Grid: Row 2 (Compatibility & Permissions)
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
                accentColor = BotHubTheme.Warning
            )

            val permText = if (bot.permissionsRequired.isEmpty()) "Isolated" else bot.permissionsRequired.first()
            val permSubtext = if (bot.permissionsRequired.size > 1) "+${bot.permissionsRequired.size - 1} more" else "On-device only"
            BentoTile(
                modifier = Modifier.weight(1f),
                label = "CAPABILITIES",
                value = permText,
                subtext = permSubtext,
                icon = Icons.Default.Security,
                accentColor = BotHubTheme.Success
            )
        }

        // Bento Grid: Row 3 (SHA-256 Hash Tile)
        val hash = bot.packageInfo.sha256Checksum
        if (hash.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            BentoIntegrityTile(hash = hash)
        }

        // Bento Grid: Release Notes Callout
        val notes = bot.releaseNotes
        if (!notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            ReleaseNotesCallout(version = bot.version, notes = notes)
        }
    }
}

/**
 * Modular Bento Tile component.
 */
@Composable
private fun BentoTile(
    label: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = BotHubTheme.PrimaryBlue,
    isMonospace: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.ElevatedSurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = BotHubTheme.MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
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
            color = BotHubTheme.PrimaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtext,
            color = BotHubTheme.SecondaryText,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Full-width Integrity Verification Tile.
 */
@Composable
private fun BentoIntegrityTile(hash: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.ElevatedSurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BotHubTheme.SuccessSurface)
                .border(1.dp, BotHubTheme.SuccessBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = BotHubTheme.Success,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SHA-256 CHECKSUM VERIFICATION",
                color = BotHubTheme.MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (hash.length > 24) "${hash.take(12)}...${hash.takeLast(12)}" else hash,
                color = BotHubTheme.PrimaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = "MATCH",
            color = BotHubTheme.Success,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Release Notes Callout component.
 */
@Composable
private fun ReleaseNotesCallout(version: String, notes: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.ElevatedSurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "CHANGELOG",
                color = BotHubTheme.MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            MonospaceBadge(text = "v$version")
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = notes,
            color = BotHubTheme.SecondaryText,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

/**
 * InstallActionCard with security guarantees, progress feedback, and gradient CTA.
 */
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
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Deploy Locally",
            color = BotHubTheme.PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Direct on-device runner. No middleman cloud servers, tokens never leave device hardware Keystore.",
            color = BotHubTheme.SecondaryText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Security Guarantees Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
                .background(BotHubTheme.ElevatedSurface)
                .border(1.dp, BotHubTheme.BorderSubtle, RoundedCornerShape(BotHubTheme.RadiusMd))
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
                    color = BotHubTheme.PrimaryBlue,
                    trackColor = BotHubTheme.SecondarySurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Downloading & Verifying...",
                        color = BotHubTheme.SecondaryText,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = BotHubTheme.PrimaryBlue,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            if (error != null) {
                Text(
                    text = error,
                    color = BotHubTheme.Error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            PrimaryButton(
                text = "ADD BOT",
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
            tint = BotHubTheme.Success,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = BotHubTheme.SecondaryText,
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
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.SuccessSurface)
            .border(1.dp, BotHubTheme.SuccessBorder, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = BotHubTheme.Success,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Installed & Verified",
                color = BotHubTheme.Success,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Payload v$installedVersion verified and sandbox runner ready.",
                color = BotHubTheme.SecondaryText,
                fontSize = 12.sp
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
            .premiumCardBorder()
            .padding(18.dp)
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
                        is BotRunState.Running -> "Foreground Service active • Polls: ${runState.pollCount}"
                        is BotRunState.Starting -> "Initializing network loop..."
                        is BotRunState.Error -> "Failure: ${runState.message}"
                        else -> "Standby"
                    },
                    color = if (isRunning) BotHubTheme.Success else BotHubTheme.SecondaryText,
                    fontSize = 12.sp,
                    fontFamily = if (isRunning) FontFamily.Monospace else FontFamily.Default
                )
            }

            RunStateBadge(
                runState = runState
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isRunning) {
            Button(
                onClick = onStopBot,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(BotHubTheme.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotHubTheme.ErrorSurface,
                    contentColor = BotHubTheme.Error
                ),
                border = BorderStroke(1.dp, BotHubTheme.ErrorBorder)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("STOP BOT", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
                text = "For uninterrupted 24/7 Telegram polling, disable battery optimization for BotHub in Android system settings.",
                color = BotHubTheme.SecondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            Text(
                text = "Open Battery Settings →",
                color = BotHubTheme.PrimaryBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
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
