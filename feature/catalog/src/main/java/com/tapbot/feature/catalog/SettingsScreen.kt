package com.tapbot.feature.catalog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotInstance
import com.tapbot.core.network.AppPreferences
import com.tapbot.core.network.CloudflareCatalogApi
import com.tapbot.feature.catalog.ui.BotHubTheme
import com.tapbot.feature.catalog.ui.MonospaceBadge
import com.tapbot.feature.catalog.ui.premiumCardBorder
import java.util.Locale

private enum class SettingsDialogType {
    NONE,
    ACCOUNT,
    PREFERENCES,
    SECURITY,
    STORAGE,
    BACKGROUND,
    ABOUT
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: AppPreferences = AppPreferences(),
    installedInstances: List<BotInstance> = emptyList(),
    customBotCount: Int = 0,
    appVersionName: String = "2.3.0",
    appVersionCode: Long = 230L,
    defaultCatalogUrl: String = CloudflareCatalogApi.DEFAULT_BASE_URL,
    initialCacheSizeBytes: Long = 0L,
    onAutoStartOnBootChanged: (Boolean) -> Unit = {},
    onWifiOnlyDownloadsChanged: (Boolean) -> Unit = {},
    onDiagnosticsExportEnabledChanged: (Boolean) -> Unit = {},
    onSaveCustomCatalogUrl: (String) -> Unit = {},
    onStopAllBots: () -> Unit = {},
    onClearCache: () -> Long = { 0L },
    onRefreshCacheSizeBytes: () -> Long = { initialCacheSizeBytes },
    onBuildDiagnosticsReport: (() -> String)? = null
) {
    val context = LocalContext.current
    var activeDialog by remember { mutableStateOf(SettingsDialogType.NONE) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var liveCacheBytes by remember(initialCacheSizeBytes) { mutableLongStateOf(initialCacheSizeBytes) }

    val freeStorageMb = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBytes / (1024 * 1024)).toInt()
        } catch (_: Exception) {
            1024
        }
    }

    val runningCount = installedInstances.count { it.status.isRunning }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BotHubTheme.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Settings",
                    color = BotHubTheme.PrimaryText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "TapBot v$appVersionName (Build $appVersionCode) • On-Device ART Engine",
                    color = BotHubTheme.SecondaryText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (statusNotice != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
                        .background(BotHubTheme.SuccessSurface)
                        .border(1.dp, BotHubTheme.SuccessBorder, RoundedCornerShape(BotHubTheme.RadiusMd))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = statusNotice!!,
                        color = BotHubTheme.Success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { statusNotice = null }) {
                        Text("Dismiss", color = BotHubTheme.PrimaryText, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Person,
                iconColor = BotHubTheme.PrimaryBlue,
                title = "Account & Runner Identity",
                subtitle = "${installedInstances.size} installed • $runningCount active • $customBotCount custom workflow(s)",
                badgeText = "Local",
                badgeColor = BotHubTheme.Info,
                onClick = { activeDialog = SettingsDialogType.ACCOUNT }
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Tune,
                iconColor = BotHubTheme.Info,
                title = "App Preferences",
                subtitle = "Boot auto-start: ${if (preferences.autoStartOnBoot) "ON" else "OFF"} • Custom catalog URL & fleet stop",
                onClick = { activeDialog = SettingsDialogType.PREFERENCES }
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Security,
                iconColor = BotHubTheme.Success,
                title = "Security & Encryption",
                subtitle = "Hardware Keystore AES-256-GCM, FLAG_SECURE masking, backup disabled",
                badgeText = "Secured",
                badgeColor = BotHubTheme.Success,
                onClick = { activeDialog = SettingsDialogType.SECURITY }
            )
        }

        item {
            val cacheMb = String.format(Locale.US, "%.2f", liveCacheBytes / (1024.0 * 1024.0))
            SettingsItemCard(
                icon = Icons.Default.Storage,
                iconColor = BotHubTheme.Warning,
                title = "Storage & Cache",
                subtitle = "${freeStorageMb} MB free • Cache: ${cacheMb} MB • Clear cache & export diagnostics",
                onClick = {
                    liveCacheBytes = onRefreshCacheSizeBytes()
                    activeDialog = SettingsDialogType.STORAGE
                }
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.BatteryChargingFull,
                iconColor = BotHubTheme.Info,
                title = "Background Execution",
                subtitle = "Battery optimization exemptions & OEM Doze settings for 24/7 uptime",
                onClick = { activeDialog = SettingsDialogType.BACKGROUND }
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Info,
                iconColor = BotHubTheme.SecondaryText,
                title = "About TapBot",
                subtitle = "Version $appVersionName ($appVersionCode) • Apache-2.0 Open Source Bot Engine",
                onClick = { activeDialog = SettingsDialogType.ABOUT }
            )
        }
    }

    when (activeDialog) {
        SettingsDialogType.ACCOUNT -> {
            AccountSettingsDialog(
                installedCount = installedInstances.size,
                runningCount = runningCount,
                customBotCount = customBotCount,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
        }
        SettingsDialogType.PREFERENCES -> {
            PreferencesSettingsDialog(
                preferences = preferences,
                defaultCatalogUrl = defaultCatalogUrl,
                runningCount = runningCount,
                onAutoStartOnBootChanged = onAutoStartOnBootChanged,
                onWifiOnlyDownloadsChanged = onWifiOnlyDownloadsChanged,
                onDiagnosticsExportEnabledChanged = onDiagnosticsExportEnabledChanged,
                onSaveCustomCatalogUrl = { newUrl ->
                    onSaveCustomCatalogUrl(newUrl)
                    statusNotice = if (newUrl.isBlank()) {
                        "Reset Catalog API endpoint to default Cloudflare Worker."
                    } else {
                        "Updated Catalog API URL and refreshed store."
                    }
                },
                onStopAllBots = {
                    onStopAllBots()
                    statusNotice = "Sent emergency stop signal to all active bots."
                },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
        }
        SettingsDialogType.SECURITY -> {
            SecuritySettingsDialog(
                installedCount = installedInstances.size,
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
        }
        SettingsDialogType.STORAGE -> {
            StorageSettingsDialog(
                freeStorageMb = freeStorageMb,
                cacheSizeBytes = liveCacheBytes,
                diagnosticsEnabled = preferences.diagnosticsExportEnabled,
                onClearCache = {
                    val freed = onClearCache()
                    liveCacheBytes = onRefreshCacheSizeBytes()
                    val freedKb = freed / 1024L
                    statusNotice = "Cleared cache (${freedKb} KB freed) & re-verified bundled packages."
                },
                onBuildDiagnosticsReport = onBuildDiagnosticsReport,
                onCopyReport = { report ->
                    copyToClipboard(context, "TapBot Diagnostics", report)
                    statusNotice = "Copied redacted TapBot diagnostics report to clipboard."
                },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
        }
        SettingsDialogType.BACKGROUND -> {
            BackgroundExecutionDialog(
                onOpenBatterySettings = { openBatterySettings(context) },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
        }
        SettingsDialogType.ABOUT -> {
            AboutTapBotDialog(
                appVersionName = appVersionName,
                appVersionCode = appVersionCode,
                activeCatalogUrl = preferences.customCatalogUrl.ifBlank { defaultCatalogUrl },
                onDismiss = { activeDialog = SettingsDialogType.NONE }
            )
        }
        SettingsDialogType.NONE -> Unit
    }
}

@Composable
private fun AccountSettingsDialog(
    installedCount: Int,
    runningCount: Int,
    customBotCount: Int,
    onDismiss: () -> Unit
) {
    val deviceLabel = remember {
        "${Build.MANUFACTURER.orEmpty()} ${Build.MODEL.orEmpty()}".trim().ifBlank { "Android Device" }
    }
    val osLabel = remember {
        "Android ${Build.VERSION.RELEASE ?: "?"} (API ${Build.VERSION.SDK_INT})"
    }
    val abiLabel = remember {
        Build.SUPPORTED_ABIS?.firstOrNull() ?: "arm64-v8a"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BotHubTheme.PrimarySurface,
        title = {
            Text("Account & Runner Identity", color = BotHubTheme.PrimaryText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TapBot operates 100% locally on your hardware without requiring any TapBot cloud login or subscription.",
                    color = BotHubTheme.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                DialogInfoRow("Runner Mode", "Local Android ART")
                DialogInfoRow("Device", deviceLabel)
                DialogInfoRow("Operating System", osLabel)
                DialogInfoRow("Primary ABI", abiLabel)
                DialogInfoRow("Installed Fleet", "$installedCount bots ($runningCount running)")
                DialogInfoRow("Custom Bots", "$customBotCount workflow bot(s)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BotHubTheme.PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun PreferencesSettingsDialog(
    preferences: AppPreferences,
    defaultCatalogUrl: String,
    runningCount: Int,
    onAutoStartOnBootChanged: (Boolean) -> Unit,
    onWifiOnlyDownloadsChanged: (Boolean) -> Unit,
    onDiagnosticsExportEnabledChanged: (Boolean) -> Unit,
    onSaveCustomCatalogUrl: (String) -> Unit,
    onStopAllBots: () -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember(preferences.customCatalogUrl) {
        mutableStateOf(preferences.customCatalogUrl)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BotHubTheme.PrimarySurface,
        title = {
            Text("App Preferences", color = BotHubTheme.PrimaryText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PreferenceToggleRow(
                    title = "Auto-start active bots on boot",
                    subtitle = "Automatically recover running bots after device reboot or app update",
                    checked = preferences.autoStartOnBoot,
                    onCheckedChange = onAutoStartOnBootChanged
                )
                PreferenceToggleRow(
                    title = "Wi-Fi only package downloads",
                    subtitle = "Prefer Wi-Fi / unmetered networks when fetching remote updates",
                    checked = preferences.wifiOnlyDownloads,
                    onCheckedChange = onWifiOnlyDownloadsChanged
                )
                PreferenceToggleRow(
                    title = "Allow diagnostic export",
                    subtitle = "Enable redacted diagnostic summary generation in Storage & Cache",
                    checked = preferences.diagnosticsExportEnabled,
                    onCheckedChange = onDiagnosticsExportEnabledChanged
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Custom Catalog API URL (Optional)",
                        color = BotHubTheme.PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        placeholder = {
                            Text(defaultCatalogUrl, color = BotHubTheme.MutedText, fontSize = 11.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BotHubTheme.PrimaryBlue,
                            unfocusedBorderColor = BotHubTheme.Border,
                            focusedTextColor = BotHubTheme.PrimaryText,
                            unfocusedTextColor = BotHubTheme.PrimaryText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                urlInput = ""
                                onSaveCustomCatalogUrl("")
                            },
                            border = BorderStroke(1.dp, BotHubTheme.Border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset", color = BotHubTheme.SecondaryText, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { onSaveCustomCatalogUrl(urlInput.trim()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BotHubTheme.PrimaryBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save URL", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onStopAllBots()
                        onDismiss()
                    },
                    enabled = runningCount > 0,
                    border = BorderStroke(1.dp, if (runningCount > 0) BotHubTheme.ErrorBorder else BotHubTheme.Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (runningCount > 0) BotHubTheme.Error else BotHubTheme.MutedText
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Emergency Stop All ($runningCount Active)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = BotHubTheme.PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun SecuritySettingsDialog(
    installedCount: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BotHubTheme.PrimarySurface,
        title = {
            Text("Security & Encryption", color = BotHubTheme.PrimaryText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "All Telegram Bot tokens and AI API keys stay strictly on this device and are encrypted at rest inside Android hardware Keystore.",
                    color = BotHubTheme.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                DialogInfoRow("Master Key Spec", "AES-256-GCM (MasterKey)")
                DialogInfoRow("Key Encryption", "AES-256-SIV PrefKeys")
                DialogInfoRow("Value Encryption", "AES-256-GCM PrefValues")
                DialogInfoRow("Screen Capture", "FLAG_SECURE Active")
                DialogInfoRow("Cloud Backup", "Disabled (allowBackup=false)")
                DialogInfoRow("Protected Bots", "$installedCount bot namespace(s)")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BotHubTheme.PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun StorageSettingsDialog(
    freeStorageMb: Int,
    cacheSizeBytes: Long,
    diagnosticsEnabled: Boolean,
    onClearCache: () -> Unit,
    onBuildDiagnosticsReport: (() -> String)?,
    onCopyReport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reportPreview by remember { mutableStateOf<String?>(null) }
    val cacheFormatted = if (cacheSizeBytes >= 1024 * 1024) {
        String.format(Locale.US, "%.2f MB", cacheSizeBytes / (1024.0 * 1024.0))
    } else {
        "${cacheSizeBytes / 1024} KB"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BotHubTheme.PrimarySurface,
        title = {
            Text("Storage & Diagnostics", color = BotHubTheme.PrimaryText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DialogInfoRow("Free Device Storage", "$freeStorageMb MB")
                DialogInfoRow("Local Cache Size", cacheFormatted)
                DialogInfoRow("Package Integrity", "SHA-256 Enforced")

                Button(
                    onClick = onClearCache,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotHubTheme.WarningSurface,
                        contentColor = BotHubTheme.Warning
                    ),
                    border = BorderStroke(1.dp, BotHubTheme.WarningBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Package & Catalog Cache", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (diagnosticsEnabled && onBuildDiagnosticsReport != null) {
                    OutlinedButton(
                        onClick = {
                            val report = onBuildDiagnosticsReport()
                            reportPreview = report
                            onCopyReport(report)
                        },
                        border = BorderStroke(1.dp, BotHubTheme.Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = BotHubTheme.PrimaryBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export & Copy Diagnostics", color = BotHubTheme.PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (reportPreview != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
                            .background(BotHubTheme.ElevatedSurface)
                            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = reportPreview!!,
                            color = BotHubTheme.SecondaryText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BotHubTheme.PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun BackgroundExecutionDialog(
    onOpenBatterySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BotHubTheme.PrimarySurface,
        title = {
            Text("24/7 Background Execution", color = BotHubTheme.PrimaryText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TapBot uses an Android Foreground Service with mediaPlayback & dataSync compliance, adaptive WakeLocks, and boot auto-recovery.",
                    color = BotHubTheme.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Text("Recommended OEM Settings:", color = BotHubTheme.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("• Samsung: Battery -> Unrestricted & add TapBot to Never Sleeping Apps.", color = BotHubTheme.SecondaryText, fontSize = 12.sp)
                Text("• Xiaomi / HyperOS: Enable Autostart + Battery Saver -> No restrictions.", color = BotHubTheme.SecondaryText, fontSize = 12.sp)
                Text("• Pixel / OnePlus: App Battery Usage -> Unrestricted.", color = BotHubTheme.SecondaryText, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onOpenBatterySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BotHubTheme.PrimaryBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Android Battery Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BotHubTheme.PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun AboutTapBotDialog(
    appVersionName: String,
    appVersionCode: Long,
    activeCatalogUrl: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BotHubTheme.PrimarySurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About TapBot", color = BotHubTheme.PrimaryText, fontWeight = FontWeight.Bold)
                MonospaceBadge(text = "v$appVersionName")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "On-Device Telegram Bot Store & Multi-Runtime Engine powered by Kotlin Coroutines on Android ART.",
                    color = BotHubTheme.SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                DialogInfoRow("Version", "$appVersionName (Build $appVersionCode)")
                DialogInfoRow("Target Android", "API 26 – 35 (Android 8–15)")
                DialogInfoRow("Built-in Engines", "Commander v2, TapStream v2, Exon 2.0, Gemini AI, RSS/Atom, Custom Workflow")
                DialogInfoRow("Catalog Edge", activeCatalogUrl)
                DialogInfoRow("License", "Apache-2.0")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BotHubTheme.PrimaryBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.ElevatedSurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = BotHubTheme.PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = BotHubTheme.SecondaryText, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BotHubTheme.PrimaryBlue,
                uncheckedThumbColor = BotHubTheme.SecondaryText,
                uncheckedTrackColor = BotHubTheme.SecondarySurface
            )
        )
    }
}

@Composable
private fun DialogInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BotHubTheme.ElevatedSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = BotHubTheme.MutedText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            color = BotHubTheme.PrimaryText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun openBatterySettings(context: Context) {
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

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    } catch (_: Exception) {}
}

@Composable
private fun SettingsItemCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = BotHubTheme.Success,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BotHubTheme.RadiusLg))
            .background(BotHubTheme.PrimarySurface)
            .premiumCardBorder()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = BotHubTheme.PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(BotHubTheme.RadiusPill))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(BotHubTheme.RadiusPill))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = BotHubTheme.SecondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = BotHubTheme.MutedText,
            modifier = Modifier.size(18.dp)
        )
    }
}
