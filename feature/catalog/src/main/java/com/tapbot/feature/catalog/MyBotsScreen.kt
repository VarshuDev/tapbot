package com.tapbot.feature.catalog

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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotUpdateProgress
import com.tapbot.core.model.BotVersion
import com.tapbot.core.model.UpdateState
import com.tapbot.core.runner.BotForegroundService
import com.tapbot.core.runner.ServiceLocator
import com.tapbot.core.runner.audio.AudioSourceType
import com.tapbot.core.runner.audio.AudioTrack
import com.tapbot.core.runner.audio.PlaybackState
import com.tapbot.core.runner.audio.PlaybackStatus
import com.tapbot.core.runner.telemetry.DeviceTelemetryProvider
import com.tapbot.core.runner.telemetry.SystemHudTelemetry
import com.tapbot.core.ui.components.BotIcon
import com.tapbot.core.ui.components.MonospaceBadge
import com.tapbot.core.ui.components.StatusBadge
import com.tapbot.core.ui.motion.TapBotMotion
import com.tapbot.core.ui.motion.rememberReducedMotion
import com.tapbot.core.ui.theme.TabularMonoStyle
import com.tapbot.core.ui.theme.TabularNumberStyle
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder
import com.tapbot.core.ui.theme.resolveBotVisualIdentity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale

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
    onEditCustomBot: (String) -> Unit = {},
    onUpdateBot: (String, BotVersion) -> Unit = { _, _ -> },
    onCheckForUpdates: () -> Unit = {},
    isCheckingUpdates: Boolean = false,
    onBrowseStoreClick: () -> Unit,
    onBuildCustomBotClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hudTelemetry by remember {
        mutableStateOf(
            DeviceTelemetryProvider(if (isPreview) null else context).getSystemHudTelemetry()
        )
    }

    if (!isPreview) {
        DisposableEffect(lifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hudTelemetry = DeviceTelemetryProvider(context).getSystemHudTelemetry()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(5000L)
                hudTelemetry = DeviceTelemetryProvider(context).getSystemHudTelemetry()
            }
        }
    }

    val botUsernames by BotForegroundService.botUsernames.collectAsState()
    val activeAudioPlayer by ServiceLocator.activeAudioPlayer.collectAsState()
    val audioStateFlow = remember(activeAudioPlayer) {
        activeAudioPlayer?.playbackState ?: MutableStateFlow(PlaybackState())
    }
    val playbackState by audioStateFlow.collectAsState()

    val showNowPlaying = activeAudioPlayer != null &&
        playbackState.currentTrack != null &&
        (playbackState.status == PlaybackStatus.PLAYING ||
            playbackState.status == PlaybackStatus.PAUSED ||
            playbackState.status == PlaybackStatus.BUFFERING)

    MyBotsScreenBody(
        instances = instances,
        hudTelemetry = hudTelemetry,
        botUsernames = botUsernames,
        availableUpdates = availableUpdates,
        updateProgress = updateProgress,
        playbackState = if (showNowPlaying) playbackState else null,
        onAudioPlayPause = {
            if (playbackState.isPlaying) activeAudioPlayer?.pause() else activeAudioPlayer?.resume()
        },
        onAudioStop = { activeAudioPlayer?.stop() },
        onAudioVolumeStep = { delta ->
            activeAudioPlayer?.setVolume((playbackState.volumePct + delta).coerceIn(0, 100))
        },
        onStartBot = onStartBot,
        onStopBot = onStopBot,
        onRestartBot = onRestartBot,
        onConfigureBot = onConfigureBot,
        onUninstallBot = onUninstallBot,
        onEditCustomBot = onEditCustomBot,
        onUpdateBot = onUpdateBot,
        onCheckForUpdates = onCheckForUpdates,
        isCheckingUpdates = isCheckingUpdates,
        onRequestDozeWhitelist = { requestDozeWhitelistSettings(context) },
        onOpenTelegramBot = { username -> openTelegramBotProfile(context, username) },
        onBrowseStoreClick = onBrowseStoreClick,
        onBuildCustomBotClick = onBuildCustomBotClick,
        modifier = modifier
    )
}

@Composable
private fun MyBotsScreenBody(
    instances: List<BotInstance>,
    hudTelemetry: SystemHudTelemetry,
    botUsernames: Map<String, String>,
    availableUpdates: Map<String, BotVersion>,
    updateProgress: Map<String, BotUpdateProgress>,
    playbackState: PlaybackState?,
    onAudioPlayPause: () -> Unit,
    onAudioStop: () -> Unit,
    onAudioVolumeStep: (Int) -> Unit,
    onStartBot: (String) -> Unit,
    onStopBot: (String) -> Unit,
    onRestartBot: (String) -> Unit,
    onConfigureBot: (String) -> Unit,
    onUninstallBot: (String) -> Unit,
    onEditCustomBot: (String) -> Unit,
    onUpdateBot: (String, BotVersion) -> Unit,
    onCheckForUpdates: () -> Unit,
    isCheckingUpdates: Boolean,
    onRequestDozeWhitelist: () -> Unit,
    onOpenTelegramBot: (String) -> Unit,
    onBrowseStoreClick: () -> Unit,
    onBuildCustomBotClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val hasRunning = remember(instances) { instances.any { it.status.isRunning } }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var pendingUninstall by remember { mutableStateOf<BotInstance?>(null) }

    LaunchedEffect(hasRunning) {
        if (hasRunning) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val pulseAlpha = if (hasRunning && !reducedMotion) {
        rememberLivePulseAlpha()
    } else {
        1f
    }

    if (instances.isEmpty()) {
        EmptyMyBotsView(
            hudTelemetry = hudTelemetry,
            onRequestDozeWhitelist = onRequestDozeWhitelist,
            onBrowseStoreClick = onBrowseStoreClick,
            onBuildCustomBotClick = onBuildCustomBotClick,
            modifier = modifier
        )
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(TapBotColors.Canvas),
            contentAlignment = Alignment.TopCenter
        ) {
            val isTabletWidth = maxWidth >= 600.dp

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "fleet_header") {
                    FleetHeaderBar(
                        instances = instances,
                        isCheckingUpdates = isCheckingUpdates,
                        onBuildCustomBotClick = onBuildCustomBotClick,
                        onCheckForUpdates = onCheckForUpdates
                    )
                }

                item(key = "host_hud") {
                    HostTelemetryHudCard(
                        telemetry = hudTelemetry,
                        onRequestDozeWhitelist = onRequestDozeWhitelist
                    )
                }

                if (playbackState != null) {
                    item(key = "tapstream_now_playing") {
                        TapStreamNowPlayingStrip(
                            state = playbackState,
                            onPlayPause = onAudioPlayPause,
                            onStop = onAudioStop,
                            onVolumeStep = onAudioVolumeStep
                        )
                    }
                }

                if (isTabletWidth) {
                    val rows = instances.chunked(2)
                    items(
                        items = rows,
                        key = { row -> row.joinToString("_") { it.installationId } }
                    ) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (instance in pair) {
                                BotInstanceCard(
                                    instance = instance,
                                    verifiedUsername = botUsernames[instance.botId],
                                    nowMs = nowMs,
                                    reducedMotion = reducedMotion,
                                    pulseAlpha = pulseAlpha,
                                    updateVersion = availableUpdates[instance.installationId],
                                    updateProgress = updateProgress[instance.installationId],
                                    onStart = { onStartBot(instance.installationId) },
                                    onStop = { onStopBot(instance.installationId) },
                                    onRestart = { onRestartBot(instance.installationId) },
                                    onConfigure = { onConfigureBot(instance.botId) },
                                    onEditFlow = if (isCustomBotId(instance.botId)) {
                                        { onEditCustomBot(instance.botId) }
                                    } else null,
                                    onUninstall = { pendingUninstall = instance },
                                    onOpenTelegram = { username -> onOpenTelegramBot(username) },
                                    onUpdate = { version -> onUpdateBot(instance.installationId, version) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(instances, key = { it.installationId }) { instance ->
                        BotInstanceCard(
                            instance = instance,
                            verifiedUsername = botUsernames[instance.botId],
                            nowMs = nowMs,
                            reducedMotion = reducedMotion,
                            pulseAlpha = pulseAlpha,
                            updateVersion = availableUpdates[instance.installationId],
                            updateProgress = updateProgress[instance.installationId],
                            onStart = { onStartBot(instance.installationId) },
                            onStop = { onStopBot(instance.installationId) },
                            onRestart = { onRestartBot(instance.installationId) },
                            onConfigure = { onConfigureBot(instance.botId) },
                            onEditFlow = if (isCustomBotId(instance.botId)) {
                                { onEditCustomBot(instance.botId) }
                            } else null,
                            onUninstall = { pendingUninstall = instance },
                            onOpenTelegram = { username -> onOpenTelegramBot(username) },
                            onUpdate = { version -> onUpdateBot(instance.installationId, version) }
                        )
                    }
                }
            }
        }
    }

    pendingUninstall?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingUninstall = null },
            containerColor = TapBotColors.SurfacePrimary,
            title = {
                Text(
                    text = "Uninstall ${target.name}?",
                    color = TapBotColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This stops the bot process, removes its local package files, and deletes all encrypted Android Keystore tokens for this bot.",
                    color = TapBotColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUninstallBot(target.installationId)
                        pendingUninstall = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Uninstall Bot", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = null }) {
                    Text("Cancel", color = TapBotColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun rememberLivePulseAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "FleetLivePulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FleetLivePulseAlpha"
    )
    return alpha
}

@Composable
private fun FleetHeaderBar(
    instances: List<BotInstance>,
    isCheckingUpdates: Boolean,
    onBuildCustomBotClick: () -> Unit,
    onCheckForUpdates: () -> Unit
) {
    val runningCount = instances.count { it.status.isRunning }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Installed Fleet",
                color = TapBotColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$runningCount of ${instances.size} running locally",
                color = if (runningCount > 0) TapBotColors.SuccessText else TapBotColors.TextSecondary,
                style = TabularMonoStyle.copy(fontSize = 12.sp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onBuildCustomBotClick)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                        .background(TapBotColors.BrandCyanSurface)
                        .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = TapBotColors.BrandCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Build", color = TapBotColors.BrandCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(enabled = !isCheckingUpdates, role = Role.Button, onClick = onCheckForUpdates)
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                        .background(TapBotColors.SurfaceElevated)
                        .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusPill))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCheckingUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = TapBotColors.BrandCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Syncing", color = TapBotColors.TextSecondary, fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = TapBotColors.Info
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Updates", color = TapBotColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HostTelemetryHudCard(
    telemetry: SystemHudTelemetry,
    onRequestDozeWhitelist: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dozeOk = telemetry.battery.isDozeWhitelisted

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder(
                borderColorTop = if (dozeOk) TapBotColors.BorderFocus else TapBotColors.WarningBorder
            )
            .animateContentSize(animationSpec = TapBotMotion.ContentSpring)
            .clickable(role = Role.Button, onClick = { expanded = !expanded })
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Host Hardware HUD",
                        color = TapBotColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                            .background(if (dozeOk) TapBotColors.SuccessSurface else TapBotColors.WarningSurface)
                            .border(
                                1.dp,
                                if (dozeOk) TapBotColors.SuccessBorder else TapBotColors.WarningBorder,
                                RoundedCornerShape(TapBotColors.RadiusPill)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (dozeOk) "Doze Exempt" else "Doze Restricted",
                            color = if (dozeOk) TapBotColors.SuccessText else TapBotColors.WarningText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bat ${telemetry.battery.percentage}% • RAM ${telemetry.ram.usedPct}% • ${telemetry.network.transport}",
                    color = TapBotColors.TextSecondary,
                    style = TabularMonoStyle.copy(fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse host HUD" else "Expand host HUD",
                tint = TapBotColors.TextSecondary
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudMetricTile(
                    icon = Icons.Rounded.BatteryChargingFull,
                    label = "BATTERY",
                    value = "${telemetry.battery.percentage}% • ${telemetry.battery.temperatureCelsius}°C",
                    detail = if (telemetry.battery.isCharging) "Charging" else "Battery",
                    accent = TapBotColors.SuccessText,
                    modifier = Modifier.weight(1f)
                )
                HudMetricTile(
                    icon = Icons.Rounded.Memory,
                    label = "MEMORY",
                    value = "${telemetry.ram.usedMemMB}/${telemetry.ram.totalMemMB} MB",
                    detail = "JVM ${telemetry.ram.jvmUsedMB} MB",
                    accent = TapBotColors.Info,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HudMetricTile(
                    icon = Icons.Rounded.Storage,
                    label = "STORAGE",
                    value = String.format(Locale.US, "%.1f GB Free", telemetry.storage.freeGB),
                    detail = "${telemetry.storage.usedPct}% used",
                    accent = TapBotColors.WarningText,
                    modifier = Modifier.weight(1f)
                )
                HudMetricTile(
                    icon = Icons.Rounded.Wifi,
                    label = "HOST NODE",
                    value = telemetry.uptimeFormatted,
                    detail = telemetry.deviceModel,
                    accent = TapBotColors.BrandCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!dozeOk) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRequestDozeWhitelist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.WarningSurface,
                        contentColor = TapBotColors.WarningText
                    ),
                    border = BorderStroke(1.dp, TapBotColors.WarningBorder)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Allow Unrestricted 24/7 Battery Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HudMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(TapBotColors.SurfaceElevated)
            .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                color = TapBotColors.TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TapBotColors.TextPrimary,
            style = TabularNumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = detail,
            color = TapBotColors.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TapStreamNowPlayingStrip(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onVolumeStep: (Int) -> Unit
) {
    val track = state.currentTrack ?: return
    val duration = state.durationMs
    val progress = if (duration > 0L) (state.elapsedMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1C1438),
                        TapBotColors.SurfacePrimary
                    )
                )
            )
            .premiumCardBorder(borderColorTop = TapBotColors.AccentMusic.copy(alpha = 0.65f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TapBotColors.AccentMusic.copy(alpha = 0.22f))
                    .border(1.dp, TapBotColors.AccentMusic.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (state.isPlaying) "TAPSTREAM PLAYING" else state.status.name,
                        color = TapBotColors.BrandCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (!track.genre.isNullOrBlank()) {
                        MonospaceBadge(
                            text = track.genre!!.uppercase(),
                            color = TapBotColors.TextPrimary,
                            backgroundColor = TapBotColors.SurfaceElevated
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.title,
                    color = TapBotColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.durationFormatted} • Vol ${state.volumePct}%",
                    color = TapBotColors.TextSecondary,
                    style = TabularNumberStyle.copy(fontSize = 11.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (duration > 0L) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = TapBotColors.BrandCyan,
                trackColor = TapBotColors.SurfaceInteractive
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause audio" else "Resume audio",
                        tint = TapBotColors.TextPrimary
                    )
                }
                IconButton(onClick = onStop, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop audio",
                        tint = TapBotColors.ErrorText
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onVolumeStep(-10) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                        contentDescription = "Decrease volume",
                        tint = TapBotColors.TextSecondary
                    )
                }
                Text(
                    text = "${state.volumePct}%",
                    color = TapBotColors.TextPrimary,
                    style = TabularMonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { onVolumeStep(10) }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Increase volume",
                        tint = TapBotColors.TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BotInstanceCard(
    instance: BotInstance,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onConfigure: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
    updateVersion: BotVersion? = null,
    updateProgress: BotUpdateProgress? = null,
    onUpdate: (BotVersion) -> Unit = {},
    verifiedUsername: String? = null,
    nowMs: Long = System.currentTimeMillis(),
    reducedMotion: Boolean = false,
    pulseAlpha: Float = 1f,
    onEditFlow: (() -> Unit)? = null,
    onOpenTelegram: (String) -> Unit = {}
) {
    val isRunning = instance.status.isRunning
    val isCrashed = instance.status.isCrashed
    val identity = resolveBotVisualIdentity(
        botId = instance.botId,
        category = instance.runtimeType,
        name = instance.name
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder(
                borderColorTop = when {
                    isRunning -> TapBotColors.SuccessBorder
                    isCrashed -> TapBotColors.ErrorBorder
                    else -> identity.borderTint
                }
            )
            .clickable(role = Role.Button, onClick = onConfigure)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotIcon(
                name = instance.name,
                category = instance.runtimeType,
                botId = instance.botId,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instance.name,
                    color = TapBotColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MonospaceBadge(text = "v${instance.version}")
                    Text(
                        text = identity.categoryLabel,
                        color = identity.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    if (!verifiedUsername.isNullOrBlank()) {
                        val clean = verifiedUsername.trim().removePrefix("@")
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                                .background(TapBotColors.SuccessSurface)
                                .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                                .clickable { onOpenTelegram(clean) }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "@$clean",
                                color = TapBotColors.SuccessText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Open @$clean in Telegram",
                                tint = TapBotColors.SuccessText,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(
                status = instance.status,
                reducedMotion = reducedMotion,
                pulseAlpha = pulseAlpha
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        StatusDiagnosticDetail(
            status = instance.status,
            startedAt = instance.startedAt,
            nowMs = nowMs
        )

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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isRunning) {
                    Button(
                        onClick = onStop,
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TapBotColors.ErrorSurface,
                            contentColor = TapBotColors.ErrorText
                        ),
                        border = BorderStroke(1.dp, TapBotColors.ErrorBorder),
                        shape = RoundedCornerShape(TapBotColors.RadiusSm),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("STOP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(TapBotColors.RadiusSm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCrashed) TapBotColors.ErrorSurface else TapBotColors.PrimaryCobalt,
                            contentColor = if (isCrashed) TapBotColors.ErrorText else Color.White
                        ),
                        border = if (isCrashed) BorderStroke(1.dp, TapBotColors.ErrorBorder) else null,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(if (isCrashed) "RETRY" else "START", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    onClick = onRestart,
                    shape = RoundedCornerShape(TapBotColors.RadiusSm),
                    color = TapBotColors.SurfaceElevated,
                    border = BorderStroke(1.dp, TapBotColors.Border),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart ${instance.name}",
                            tint = TapBotColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onEditFlow != null) {
                    Surface(
                        onClick = onEditFlow,
                        shape = RoundedCornerShape(TapBotColors.RadiusSm),
                        color = TapBotColors.BrandCyanSurface,
                        border = BorderStroke(1.dp, TapBotColors.BrandCyanBorder),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = TapBotColors.BrandCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text("Flow", color = TapBotColors.BrandCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Surface(
                    onClick = onConfigure,
                    shape = RoundedCornerShape(TapBotColors.RadiusSm),
                    color = TapBotColors.SurfaceElevated,
                    border = BorderStroke(1.dp, TapBotColors.Border),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Configure ${instance.name}",
                            tint = TapBotColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    onClick = onUninstall,
                    shape = RoundedCornerShape(TapBotColors.RadiusSm),
                    color = TapBotColors.SurfaceElevated,
                    border = BorderStroke(1.dp, TapBotColors.ErrorBorder.copy(alpha = 0.55f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Uninstall ${instance.name}",
                            tint = TapBotColors.ErrorText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDiagnosticDetail(
    status: BotInstanceStatus,
    startedAt: Long?,
    nowMs: Long
) {
    when (status) {
        is BotInstanceStatus.Running -> {
            val uptimeSeconds = if (startedAt != null) ((nowMs - startedAt) / 1000L).coerceAtLeast(0L) else 0L
            val uptimeFormatted = when {
                uptimeSeconds >= 86400L -> "${uptimeSeconds / 86400L}d ${(uptimeSeconds % 86400L) / 3600L}h"
                uptimeSeconds >= 3600L -> "${uptimeSeconds / 3600L}h ${(uptimeSeconds % 3600L) / 60L}m"
                uptimeSeconds >= 60L -> "${uptimeSeconds / 60L}m ${uptimeSeconds % 60L}s"
                else -> "${uptimeSeconds}s"
            }
            Text(
                text = "Polling • ${status.pollCount} polls • ${status.messageCount} msgs • Up $uptimeFormatted",
                style = TabularMonoStyle.copy(fontSize = 11.sp),
                color = TapBotColors.SuccessText
            )
        }
        is BotInstanceStatus.Crashed -> {
            Text(
                text = "Error: ${status.reason} (${status.crashCount}x)",
                style = TabularMonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = TapBotColors.ErrorText
            )
        }
        is BotInstanceStatus.Starting -> {
            Text(
                text = "Connecting to Telegram Bot API...",
                fontSize = 12.sp,
                color = TapBotColors.WarningText
            )
        }
        is BotInstanceStatus.Stopping -> {
            Text(
                text = "Shutting down bot coroutine cleanly...",
                fontSize = 12.sp,
                color = TapBotColors.TextMuted
            )
        }
        else -> {
            Text(
                text = "Process idle. Tap card to inspect console or start.",
                fontSize = 12.sp,
                color = TapBotColors.TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyMyBotsView(
    hudTelemetry: SystemHudTelemetry,
    onRequestDozeWhitelist: () -> Unit,
    onBrowseStoreClick: () -> Unit,
    onBuildCustomBotClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TapBotColors.Canvas)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(TapBotColors.RadiusLg))
                .background(TapBotColors.SurfacePrimary)
                .premiumCardBorder(borderColorTop = TapBotColors.BrandCyanBorder)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TapBotColors.BrandCyanSurface)
                    .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = TapBotColors.BrandCyan,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Fleet Is Ready to Deploy",
                color = TapBotColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Deploy pre-built runtimes from the Bot Store or craft your own slash-command & webhook bot locally on ${hudTelemetry.deviceModel}.",
                color = TapBotColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            if (!hudTelemetry.battery.isDozeWhitelisted) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                        .background(TapBotColors.WarningSurface)
                        .border(1.dp, TapBotColors.WarningBorder, RoundedCornerShape(TapBotColors.RadiusMd))
                        .clickable(role = Role.Button, onClick = onRequestDozeWhitelist)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = TapBotColors.WarningText, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Tip: Allow Unrestricted Battery Access before starting 24/7 bots ->",
                        color = TapBotColors.WarningText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onBrowseStoreClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TapBotColors.PrimaryCobalt,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Bot Store", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBuildCustomBotClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                border = BorderStroke(1.dp, TapBotColors.BrandCyanBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = TapBotColors.SurfaceElevated,
                    contentColor = TapBotColors.TextPrimary
                )
            ) {
                Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = TapBotColors.BrandCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Build No-Code Custom Bot", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(TapBotColors.WarningSurface)
            .border(1.dp, TapBotColors.WarningBorder, RoundedCornerShape(TapBotColors.RadiusMd))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            tint = TapBotColors.WarningText,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Update available — v${updateVersion.version}",
                color = TapBotColors.WarningText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            val notes = updateVersion.releaseNotes
            if (!notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notes,
                    color = TapBotColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onUpdate,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(TapBotColors.RadiusPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = TapBotColors.Warning,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
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
        isSuccess -> TapBotColors.SuccessSurface
        isFailed -> TapBotColors.ErrorSurface
        isRollback -> TapBotColors.WarningSurface
        else -> TapBotColors.SurfaceElevated
    }
    val borderColor = when {
        isSuccess -> TapBotColors.SuccessBorder
        isFailed -> TapBotColors.ErrorBorder
        isRollback -> TapBotColors.WarningBorder
        else -> TapBotColors.Border
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(TapBotColors.RadiusMd))
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
                UpdateState.VERIFYING -> "Verifying SHA-256"
                UpdateState.INSTALLING -> "Staging Package"
                UpdateState.STARTING -> "Starting v${updateProgress.targetVersion}"
                UpdateState.SUCCESS -> "Updated to v${updateProgress.targetVersion}"
                UpdateState.ROLLING_BACK -> "Rolled back to v${updateProgress.currentVersion}"
                UpdateState.FAILED -> "Update Failed"
                UpdateState.IDLE -> ""
            }

            val statusIcon = when {
                isSuccess -> Icons.Default.CheckCircle
                isFailed -> Icons.Default.Error
                isRollback -> Icons.Default.Warning
                else -> Icons.Default.Sync
            }

            val tintColor = when {
                isSuccess -> TapBotColors.SuccessText
                isFailed -> TapBotColors.ErrorText
                isRollback -> TapBotColors.WarningText
                else -> TapBotColors.BrandCyan
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = statusIcon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tintColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tintColor)
            }

            if (updateProgress.state == UpdateState.DOWNLOADING) {
                Text(
                    text = "${(updateProgress.progress * 100).toInt()}%",
                    style = TabularMonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = TapBotColors.TextPrimary
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
                color = TapBotColors.BrandCyan,
                trackColor = TapBotColors.SurfaceInteractive
            )
        }

        updateProgress.message?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = msg, fontSize = 11.sp, color = TapBotColors.TextSecondary)
        }

        updateProgress.error?.let { err ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = err, fontSize = 11.sp, color = TapBotColors.ErrorText, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusPill),
                border = BorderStroke(1.dp, TapBotColors.Border)
            ) {
                Text("Retry Update", fontSize = 12.sp, color = TapBotColors.TextPrimary)
            }
        }
    }
}

private fun isCustomBotId(botId: String): Boolean {
    return botId.startsWith("bot_custom_") || botId.startsWith("custom_")
}

internal fun requestDozeWhitelistSettings(context: Context) {
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

private fun openTelegramBotProfile(context: Context, username: String) {
    val clean = username.trim().removePrefix("@")
    if (clean.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$clean")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun previewFleetInstances(): List<BotInstance> = listOf(
    BotInstance(
        installationId = "inst_cmd",
        botId = "bot_commander",
        name = "TapBot Commander",
        version = "1.0.0",
        status = BotInstanceStatus.Running(pollCount = 42, messageCount = 9),
        startedAt = System.currentTimeMillis() - 195_000L
    ),
    BotInstance(
        installationId = "inst_music",
        botId = "bot_music",
        name = "TapStream Audio",
        version = "2.0.0",
        status = BotInstanceStatus.Running(pollCount = 18, messageCount = 3),
        startedAt = System.currentTimeMillis() - 65_000L
    ),
    BotInstance(
        installationId = "inst_custom",
        botId = "bot_custom_alert_1",
        name = "Deploy Hook Bot",
        version = "1.0.0",
        status = BotInstanceStatus.Stopped
    )
)

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, heightDp = 760)
@Composable
private fun MyBotsRunningPhonePreview() {
    val hud = DeviceTelemetryProvider(null).getSystemHudTelemetry().let {
        it.copy(battery = it.battery.copy(isDozeWhitelisted = false))
    }
    val playback = PlaybackState(
        status = PlaybackStatus.PLAYING,
        currentTrack = AudioTrack(
            id = "radio_lofi",
            title = "Lofi Girl Study Beats",
            artist = "Lofi Girl 24/7",
            streamUrl = "",
            genre = "lofi"
        ),
        elapsedMs = 45_000L,
        durationMs = 180_000L,
        volumePct = 70
    )
    TapBotTheme {
        MyBotsScreenBody(
            instances = previewFleetInstances(),
            hudTelemetry = hud,
            botUsernames = mapOf("bot_commander" to "tapbot_cmd_bot"),
            availableUpdates = emptyMap(),
            updateProgress = emptyMap(),
            playbackState = playback,
            onAudioPlayPause = {},
            onAudioStop = {},
            onAudioVolumeStep = {},
            onStartBot = {},
            onStopBot = {},
            onRestartBot = {},
            onConfigureBot = {},
            onUninstallBot = {},
            onEditCustomBot = {},
            onUpdateBot = { _, _ -> },
            onCheckForUpdates = {},
            isCheckingUpdates = false,
            onRequestDozeWhitelist = {},
            onOpenTelegramBot = {},
            onBrowseStoreClick = {},
            onBuildCustomBotClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 840, heightDp = 640)
@Composable
private fun MyBotsTabletPreview() {
    TapBotTheme {
        MyBotsScreenBody(
            instances = previewFleetInstances(),
            hudTelemetry = DeviceTelemetryProvider(null).getSystemHudTelemetry(),
            botUsernames = mapOf("bot_commander" to "tapbot_cmd_bot"),
            availableUpdates = emptyMap(),
            updateProgress = emptyMap(),
            playbackState = null,
            onAudioPlayPause = {},
            onAudioStop = {},
            onAudioVolumeStep = {},
            onStartBot = {},
            onStopBot = {},
            onRestartBot = {},
            onConfigureBot = {},
            onUninstallBot = {},
            onEditCustomBot = {},
            onUpdateBot = { _, _ -> },
            onCheckForUpdates = {},
            isCheckingUpdates = false,
            onRequestDozeWhitelist = {},
            onOpenTelegramBot = {},
            onBrowseStoreClick = {},
            onBuildCustomBotClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, fontScale = 1.3f)
@Composable
private fun MyBotsEmptyLargeFontPreview() {
    val hud = DeviceTelemetryProvider(null).getSystemHudTelemetry().let {
        it.copy(battery = it.battery.copy(isDozeWhitelisted = false))
    }
    TapBotTheme {
        EmptyMyBotsView(
            hudTelemetry = hud,
            onRequestDozeWhitelist = {},
            onBrowseStoreClick = {},
            onBuildCustomBotClick = {}
        )
    }
}
