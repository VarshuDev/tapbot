package com.tapbot.feature.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotUpdateProgress
import com.tapbot.core.model.BotVersion
import com.tapbot.core.model.UpdateState
import com.tapbot.feature.catalog.ui.BotHubTheme
import com.tapbot.feature.catalog.ui.BotIcon
import com.tapbot.feature.catalog.ui.MonospaceBadge
import com.tapbot.feature.catalog.ui.StatusBadge
import com.tapbot.feature.catalog.ui.premiumCardBorder

/**
 * "MY FLEET" Screen displaying real-time independent state for all locally installed bots.
 * Styled for BotHub's dark OLED developer aesthetic.
 */
@Composable
fun MyBotsContent(
    instances: List<BotInstance>,
    availableUpdates: Map<String, BotVersion> = emptyMap(),
    updateProgress: Map<String, BotUpdateProgress> = emptyMap(),
    onStartBot: (String) -> Unit,
    onStopBot: (String) -> Unit,
    onRestartBot: (String) -> Unit,
    onConfigureBot: (String) -> Unit,
    onUninstallBot: (String) -> Unit,
    onUpdateBot: (String, BotVersion) -> Unit = { _, _ -> },
    onCheckForUpdates: () -> Unit = {},
    isCheckingUpdates: Boolean = false,
    onBrowseStoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (instances.isEmpty()) {
        EmptyMyBotsView(onBrowseStoreClick = onBrowseStoreClick, modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(BotHubTheme.Background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Installed Fleet",
                            color = BotHubTheme.PrimaryText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val runningCount = instances.count { it.status.isRunning }
                        Text(
                            text = "$runningCount of ${instances.size} running locally",
                            color = if (runningCount > 0) BotHubTheme.Success else BotHubTheme.SecondaryText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdates,
                        shape = RoundedCornerShape(BotHubTheme.RadiusPill),
                        color = BotHubTheme.ElevatedSurface,
                        border = BorderStroke(1.dp, BotHubTheme.Border)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = BotHubTheme.PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checking...", color = BotHubTheme.SecondaryText, fontSize = 11.sp)
                            } else {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Check for updates",
                                    modifier = Modifier.size(14.dp),
                                    tint = BotHubTheme.PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Updates", color = BotHubTheme.PrimaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            items(instances, key = { it.installationId }) { instance ->
                BotInstanceCard(
                    instance = instance,
                    updateVersion = availableUpdates[instance.installationId],
                    updateProgress = updateProgress[instance.installationId],
                    onStart = { onStartBot(instance.installationId) },
                    onStop = { onStopBot(instance.installationId) },
                    onRestart = { onRestartBot(instance.installationId) },
                    onConfigure = { onConfigureBot(instance.botId) },
                    onUninstall = { onUninstallBot(instance.installationId) },
                    onUpdate = { version -> onUpdateBot(instance.installationId, version) }
                )
            }
        }
    }
}

@Composable
fun BotInstanceCard(
    instance: BotInstance,
    updateVersion: BotVersion? = null,
    updateProgress: BotUpdateProgress? = null,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onConfigure: () -> Unit,
    onUninstall: () -> Unit,
    onUpdate: (BotVersion) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRunning = instance.status.isRunning
    val isCrashed = instance.status.isCrashed

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder()
            .padding(16.dp)
    ) {
        // Header Row: BotIcon, Name, Version & Status Badge with animated pulse
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotIcon(
                name = instance.name,
                category = instance.runtimeType,
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instance.name,
                    color = BotHubTheme.PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonospaceBadge(text = "v${instance.version}")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = instance.runtimeType,
                        color = BotHubTheme.SecondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            StatusBadge(status = instance.status)
        }

        // Real State Diagnostic Detail
        Spacer(modifier = Modifier.height(12.dp))
        StatusDiagnosticDetail(status = instance.status, startedAt = instance.startedAt)

        // Version Update Banner / Active Progress Tracker
        if (updateProgress != null && updateProgress.state != UpdateState.IDLE) {
            Spacer(modifier = Modifier.height(10.dp))
            UpdateProgressBanner(
                updateProgress = updateProgress,
                onRetry = { updateVersion?.let { onUpdate(it) } }
            )
        } else if (updateVersion != null) {
            Spacer(modifier = Modifier.height(10.dp))
            UpdateAvailableBanner(
                updateVersion = updateVersion,
                onUpdate = { onUpdate(updateVersion) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Independent Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRunning) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotHubTheme.ErrorSurface,
                            contentColor = BotHubTheme.Error
                        ),
                        border = BorderStroke(1.dp, BotHubTheme.ErrorBorder),
                        shape = RoundedCornerShape(BotHubTheme.RadiusSm),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(BotHubTheme.RadiusSm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCrashed) BotHubTheme.ErrorSurface else BotHubTheme.PrimaryBlue,
                            contentColor = if (isCrashed) BotHubTheme.Error else Color.White
                        ),
                        border = if (isCrashed) BorderStroke(1.dp, BotHubTheme.ErrorBorder) else null,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isCrashed) "RETRY" else "START", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = onRestart,
                    shape = RoundedCornerShape(BotHubTheme.RadiusSm),
                    color = BotHubTheme.ElevatedSurface,
                    border = BorderStroke(1.dp, BotHubTheme.Border)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = BotHubTheme.PrimaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = onConfigure,
                    shape = RoundedCornerShape(BotHubTheme.RadiusSm),
                    color = BotHubTheme.ElevatedSurface,
                    border = BorderStroke(1.dp, BotHubTheme.Border)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Credentials & Config",
                            tint = BotHubTheme.SecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    onClick = onUninstall,
                    shape = RoundedCornerShape(BotHubTheme.RadiusSm),
                    color = BotHubTheme.ElevatedSurface,
                    border = BorderStroke(1.dp, BotHubTheme.Border)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Uninstall",
                            tint = BotHubTheme.Error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDiagnosticDetail(status: BotInstanceStatus, startedAt: Long?) {
    when (status) {
        is BotInstanceStatus.Running -> {
            val uptimeSeconds = if (startedAt != null) (System.currentTimeMillis() - startedAt) / 1000 else 0
            val uptimeFormatted = if (uptimeSeconds >= 3600) {
                "${uptimeSeconds / 3600}h ${(uptimeSeconds % 3600) / 60}m"
            } else if (uptimeSeconds >= 60) {
                "${uptimeSeconds / 60}m ${uptimeSeconds % 60}s"
            } else {
                "${uptimeSeconds}s"
            }
            Text(
                text = "Polling active • ${status.pollCount} polls • ${status.messageCount} msgs • Uptime: $uptimeFormatted",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = BotHubTheme.Success
            )
        }
        is BotInstanceStatus.Crashed -> {
            Text(
                text = "Crash: ${status.reason} (Count: ${status.crashCount})",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = BotHubTheme.Error,
                fontWeight = FontWeight.SemiBold
            )
        }
        is BotInstanceStatus.Starting -> {
            Text(
                text = "Connecting to Telegram Bot API loop...",
                fontSize = 12.sp,
                color = BotHubTheme.Warning
            )
        }
        is BotInstanceStatus.Stopping -> {
            Text(
                text = "Shutting down bot process...",
                fontSize = 12.sp,
                color = BotHubTheme.MutedText
            )
        }
        else -> {
            Text(
                text = "Process idle. Ready to launch.",
                fontSize = 12.sp,
                color = BotHubTheme.SecondaryText
            )
        }
    }
}

@Composable
private fun EmptyMyBotsView(onBrowseStoreClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BotHubTheme.Background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BotHubTheme.ElevatedSurface)
                    .border(1.dp, BotHubTheme.Border, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = BotHubTheme.PrimaryBlue,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No Bots Installed",
                color = BotHubTheme.PrimaryText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You haven't deployed any bots yet. Browse the Bot Store to install Gemini AI Assistant, Music Bot, or Utility Bot.",
                color = BotHubTheme.SecondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBrowseStoreClick,
                shape = RoundedCornerShape(BotHubTheme.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BotHubTheme.PrimaryBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Bot Store", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UpdateAvailableBanner(
    updateVersion: BotVersion,
    onUpdate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.WarningSurface)
            .border(1.dp, BotHubTheme.WarningBorder, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            tint = BotHubTheme.Warning,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Update available — v${updateVersion.version}",
                color = BotHubTheme.Warning,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            val notes = updateVersion.releaseNotes
            if (!notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notes,
                    color = BotHubTheme.SecondaryText,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onUpdate,
            shape = RoundedCornerShape(BotHubTheme.RadiusPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = BotHubTheme.Warning,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text("Update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UpdateProgressBanner(
    updateProgress: BotUpdateProgress,
    onRetry: () -> Unit
) {
    val isSuccess = updateProgress.state == UpdateState.SUCCESS
    val isFailed = updateProgress.state == UpdateState.FAILED
    val isRollback = updateProgress.state == UpdateState.ROLLING_BACK

    val bgColor = when {
        isSuccess -> BotHubTheme.SuccessSurface
        isFailed -> BotHubTheme.ErrorSurface
        isRollback -> BotHubTheme.WarningSurface
        else -> BotHubTheme.ElevatedSurface
    }
    val borderColor = when {
        isSuccess -> BotHubTheme.SuccessBorder
        isFailed -> BotHubTheme.ErrorBorder
        isRollback -> BotHubTheme.WarningBorder
        else -> BotHubTheme.Border
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(BotHubTheme.RadiusMd))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusLabel = when (updateProgress.state) {
                UpdateState.CHECKING -> "Checking Compatibility"
                UpdateState.DOWNLOADING -> "Downloading v${updateProgress.targetVersion}"
                UpdateState.VERIFYING -> "Verifying SHA-256 Checksum"
                UpdateState.INSTALLING -> "Staging & Replacing Package"
                UpdateState.STARTING -> "Starting v${updateProgress.targetVersion}"
                UpdateState.SUCCESS -> "Successfully Updated to v${updateProgress.targetVersion}"
                UpdateState.ROLLING_BACK -> "Startup Failed — Rolling Back to v${updateProgress.currentVersion}"
                UpdateState.FAILED -> "Update Failed"
                UpdateState.IDLE -> ""
            }

            val statusIcon = when {
                isSuccess -> Icons.Default.CheckCircle
                isFailed -> Icons.Default.Error
                isRollback -> Icons.Default.Warning
                else -> Icons.Default.Sync
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when {
                        isSuccess -> BotHubTheme.Success
                        isFailed -> BotHubTheme.Error
                        isRollback -> BotHubTheme.Warning
                        else -> BotHubTheme.PrimaryBlue
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSuccess -> BotHubTheme.Success
                        isFailed -> BotHubTheme.Error
                        isRollback -> BotHubTheme.Warning
                        else -> BotHubTheme.PrimaryBlue
                    }
                )
            }

            if (updateProgress.state == UpdateState.DOWNLOADING) {
                Text(
                    text = "${(updateProgress.progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = BotHubTheme.PrimaryText
                )
            }
        }

        if (updateProgress.isInProgress) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (updateProgress.state == UpdateState.DOWNLOADING) updateProgress.progress else 0.5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = BotHubTheme.PrimaryBlue,
                trackColor = BotHubTheme.SecondarySurface
            )
        }

        updateProgress.message?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = msg,
                fontSize = 11.sp,
                color = BotHubTheme.SecondaryText
            )
        }

        updateProgress.error?.let { err ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = err,
                fontSize = 11.sp,
                color = BotHubTheme.Error,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(BotHubTheme.RadiusPill),
                border = BorderStroke(1.dp, BotHubTheme.Border),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Retry Update", fontSize = 11.sp, color = BotHubTheme.PrimaryText)
            }
        }
    }
}
