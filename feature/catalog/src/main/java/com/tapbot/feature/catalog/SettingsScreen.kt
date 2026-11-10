package com.tapbot.feature.catalog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.network.AppPreferences
import com.tapbot.core.network.CloudflareCatalogApi
import com.tapbot.core.runner.telemetry.DeviceTelemetryProvider
import com.tapbot.core.security.SecretRedactor
import com.tapbot.core.ui.components.MonospaceBadge
import com.tapbot.core.ui.components.PrimaryButton
import com.tapbot.core.ui.motion.TapBotMotion
import com.tapbot.core.ui.theme.TabularMonoStyle
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder
import java.util.Locale

private enum class SettingsSection {
    PREFERENCES,
    BACKGROUND,
    IDENTITY,
    SECURITY,
    STORAGE,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val isPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var expandedSections by remember {
        mutableStateOf(setOf(SettingsSection.PREFERENCES, SettingsSection.BACKGROUND))
    }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    var reportPreview by remember { mutableStateOf<String?>(null) }
    var liveCacheBytes by remember(initialCacheSizeBytes) { mutableLongStateOf(initialCacheSizeBytes) }

    var showCustomUrlSheet by remember { mutableStateOf(false) }
    var showConfirmClearCache by remember { mutableStateOf(false) }
    var showConfirmStopAll by remember { mutableStateOf(false) }

    var isDozeWhitelisted by remember {
        mutableStateOf(
            DeviceTelemetryProvider(if (isPreview) null else context).getBatteryTelemetry().isDozeWhitelisted
        )
    }

    if (!isPreview) {
        DisposableEffect(lifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isDozeWhitelisted = DeviceTelemetryProvider(context).getBatteryTelemetry().isDozeWhitelisted
                    liveCacheBytes = onRefreshCacheSizeBytes()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    fun toggleSection(section: SettingsSection) {
        expandedSections = if (section in expandedSections) {
            expandedSections - section
        } else {
            if (section == SettingsSection.STORAGE) {
                liveCacheBytes = onRefreshCacheSizeBytes()
            }
            expandedSections + section
        }
    }

    val freeStorageMb = remember(isPreview) {
        if (isPreview) 46080 else try {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBytes / (1024 * 1024)).toInt()
        } catch (_: Exception) {
            1024
        }
    }

    val runningCount = installedInstances.count { it.status.isRunning }
    val usingCustomCatalog = preferences.customCatalogUrl.isNotBlank()
    val cacheFormatted = if (liveCacheBytes >= 1024 * 1024) {
        String.format(Locale.US, "%.2f MB", liveCacheBytes / (1024.0 * 1024.0))
    } else {
        "${liveCacheBytes / 1024} KB"
    }

    val deviceLabel = remember {
        "${Build.MANUFACTURER.orEmpty()} ${Build.MODEL.orEmpty()}".trim().ifBlank { "Android Device" }
    }
    val osLabel = remember {
        "Android ${Build.VERSION.RELEASE ?: "14"} (API ${Build.VERSION.SDK_INT})"
    }
    val abiLabel = remember {
        Build.SUPPORTED_ABIS?.firstOrNull() ?: "arm64-v8a"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TapBotColors.Canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "settings_header") {
                Column(modifier = Modifier.padding(bottom = 2.dp)) {
                    Text(
                        text = "Node Settings",
                        color = TapBotColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TapBot v$appVersionName (Build $appVersionCode) • On-Device ART Engine",
                        color = TapBotColors.TextSecondary,
                        style = TabularMonoStyle.copy(fontSize = 12.sp)
                    )
                }
            }

            if (statusNotice != null) {
                item(key = "status_notice") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                            .background(TapBotColors.SuccessSurface)
                            .border(1.dp, TapBotColors.SuccessBorder, RoundedCornerShape(TapBotColors.RadiusMd))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusNotice!!,
                            color = TapBotColors.SuccessText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { statusNotice = null },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Dismiss", color = TapBotColors.TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 1. App Preferences & Catalog Source
            item(key = "sec_preferences") {
                ExpandableSettingsSectionCard(
                    icon = Icons.Default.Tune,
                    iconColor = TapBotColors.BrandCyan,
                    title = "App Preferences & Catalog Edge",
                    subtitle = if (usingCustomCatalog) "Custom API endpoint active" else "Cloudflare Edge Default",
                    expanded = SettingsSection.PREFERENCES in expandedSections,
                    onToggleExpand = { toggleSection(SettingsSection.PREFERENCES) }
                ) {
                    PreferenceToggleRow(
                        title = "Auto-start active bots on boot",
                        subtitle = "Automatically recover running bots after device reboot or update",
                        checked = preferences.autoStartOnBoot,
                        onCheckedChange = onAutoStartOnBootChanged
                    )
                    PreferenceToggleRow(
                        title = "Wi-Fi only package downloads",
                        subtitle = "Prefer unmetered Wi-Fi when downloading remote updates",
                        checked = preferences.wifiOnlyDownloads,
                        onCheckedChange = onWifiOnlyDownloadsChanged
                    )
                    PreferenceToggleRow(
                        title = "Allow diagnostic export",
                        subtitle = "Enable redacted diagnostic summary generation below",
                        checked = preferences.diagnosticsExportEnabled,
                        onCheckedChange = onDiagnosticsExportEnabledChanged
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                            .background(TapBotColors.SurfaceElevated)
                            .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Catalog Source Endpoint",
                            color = TapBotColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CatalogSourceChip(
                                label = "Cloudflare Edge",
                                isSelected = !usingCustomCatalog,
                                onClick = {
                                    if (usingCustomCatalog) {
                                        onSaveCustomCatalogUrl("")
                                        statusNotice = "Reset Catalog API endpoint to Cloudflare Edge."
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CatalogSourceChip(
                                label = "Custom URL",
                                isSelected = usingCustomCatalog,
                                onClick = { showCustomUrlSheet = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preferences.customCatalogUrl.ifBlank { defaultCatalogUrl },
                                color = TapBotColors.TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { showCustomUrlSheet = true },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = TapBotColors.BrandCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", color = TapBotColors.BrandCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 2. 24/7 Background Execution & Doze
            item(key = "sec_background") {
                ExpandableSettingsSectionCard(
                    icon = Icons.Default.BatteryChargingFull,
                    iconColor = if (isDozeWhitelisted) TapBotColors.SuccessText else TapBotColors.WarningText,
                    title = "24/7 Background & Doze Protection",
                    subtitle = if (isDozeWhitelisted) "Battery optimization unrestricted" else "Action needed for continuous polling",
                    badgeText = if (isDozeWhitelisted) "Unrestricted ✓" else "Battery Restricted",
                    badgeColor = if (isDozeWhitelisted) TapBotColors.SuccessText else TapBotColors.WarningText,
                    badgeBg = if (isDozeWhitelisted) TapBotColors.SuccessSurface else TapBotColors.WarningSurface,
                    badgeBorder = if (isDozeWhitelisted) TapBotColors.SuccessBorder else TapBotColors.WarningBorder,
                    expanded = SettingsSection.BACKGROUND in expandedSections,
                    onToggleExpand = { toggleSection(SettingsSection.BACKGROUND) }
                ) {
                    Text(
                        text = "TapBot uses Foreground Service isolation with adaptive WakeLocks and boot recovery. Whitelisting battery optimization prevents Android from pausing polling when your screen locks.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { requestDozeWhitelistSettings(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(TapBotColors.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDozeWhitelisted) TapBotColors.SurfaceInteractive else TapBotColors.BrandCyan,
                            contentColor = if (isDozeWhitelisted) TapBotColors.TextPrimary else TapBotColors.BrandCyanInk
                        )
                    ) {
                        Text(
                            text = if (isDozeWhitelisted) "Review Android Battery Settings" else "Disable Battery Optimization (1-Tap)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                            .background(TapBotColors.SurfaceElevated)
                            .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("OEM Checklists:", color = TapBotColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("• Samsung: App Battery -> Unrestricted & add to Never Sleeping Apps.", color = TapBotColors.TextSecondary, fontSize = 11.sp)
                        Text("• Xiaomi / HyperOS: Enable Autostart + Battery Saver -> No restrictions.", color = TapBotColors.TextSecondary, fontSize = 11.sp)
                        Text("• Pixel / OnePlus: App Battery Usage -> Allow background + Unrestricted.", color = TapBotColors.TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // 3. Host Runner & Hardware Identity
            item(key = "sec_identity") {
                ExpandableSettingsSectionCard(
                    icon = Icons.Default.Person,
                    iconColor = TapBotColors.PrimaryBlue,
                    title = "Host Runner & Hardware",
                    subtitle = "$deviceLabel • ${installedInstances.size} installed ($runningCount active)",
                    badgeText = "Local ART",
                    badgeColor = TapBotColors.Info,
                    badgeBg = TapBotColors.InfoSurface,
                    badgeBorder = TapBotColors.InfoBorder,
                    expanded = SettingsSection.IDENTITY in expandedSections,
                    onToggleExpand = { toggleSection(SettingsSection.IDENTITY) }
                ) {
                    Text(
                        text = "TapBot executes locally on your device with zero TapBot cloud login, subscriptions, or external proxy servers.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    SettingsInfoRow("Runner Mode", "Local Android ART")
                    SettingsInfoRow("Device", deviceLabel)
                    SettingsInfoRow("Operating System", osLabel)
                    SettingsInfoRow("Primary ABI", abiLabel)
                    SettingsInfoRow("Installed Fleet", "${installedInstances.size} bots ($runningCount running)")
                    SettingsInfoRow("Custom Bots", "$customBotCount workflow(s)")
                }
            }

            // 4. Security & Encryption
            item(key = "sec_security") {
                ExpandableSettingsSectionCard(
                    icon = Icons.Default.Security,
                    iconColor = TapBotColors.SuccessText,
                    title = "Security & Keystore Vault",
                    subtitle = "Hardware AES-256-GCM, FLAG_SECURE masking, backup disabled",
                    badgeText = "Secured",
                    badgeColor = TapBotColors.SuccessText,
                    badgeBg = TapBotColors.SuccessSurface,
                    badgeBorder = TapBotColors.SuccessBorder,
                    expanded = SettingsSection.SECURITY in expandedSections,
                    onToggleExpand = { toggleSection(SettingsSection.SECURITY) }
                ) {
                    Text(
                        text = "Telegram bot tokens and third-party API keys are encrypted at rest inside hardware-backed Android Keystore and redacted from all logs and reports.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    SettingsInfoRow("Master Key Spec", "AES-256-GCM (MasterKey)")
                    SettingsInfoRow("Key Encryption", "AES-256-SIV PrefKeys")
                    SettingsInfoRow("Value Encryption", "AES-256-GCM PrefValues")
                    SettingsInfoRow("Screen Capture", "FLAG_SECURE Active in Detail")
                    SettingsInfoRow("Cloud Backup", "Disabled (allowBackup=false)")
                    SettingsInfoRow("Protected Bots", "${installedInstances.size} namespace(s)")
                }
            }

            // 5. Storage, Cache & Diagnostics
            item(key = "sec_storage") {
                ExpandableSettingsSectionCard(
                    icon = Icons.Default.Storage,
                    iconColor = TapBotColors.WarningText,
                    title = "Storage, Cache & Diagnostics",
                    subtitle = "$freeStorageMb MB free • Cache: $cacheFormatted",
                    expanded = SettingsSection.STORAGE in expandedSections,
                    onToggleExpand = { toggleSection(SettingsSection.STORAGE) }
                ) {
                    SettingsInfoRow("Free Device Storage", "$freeStorageMb MB")
                    SettingsInfoRow("Local Cache Size", cacheFormatted)
                    SettingsInfoRow("Package Integrity", "SHA-256 Enforced")

                    OutlinedButton(
                        onClick = { showConfirmClearCache = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(TapBotColors.RadiusMd),
                        border = BorderStroke(1.dp, TapBotColors.WarningBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = TapBotColors.WarningSurface,
                            contentColor = TapBotColors.WarningText
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Package & Catalog Cache", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (preferences.diagnosticsExportEnabled && onBuildDiagnosticsReport != null) {
                        OutlinedButton(
                            onClick = {
                                val raw = onBuildDiagnosticsReport()
                                val clean = SecretRedactor.redact(raw)
                                reportPreview = clean
                                copyToClipboard(context, "TapBot Diagnostics", clean)
                                statusNotice = "Copied redacted diagnostics report to clipboard."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(TapBotColors.RadiusMd),
                            border = BorderStroke(1.dp, TapBotColors.BorderFocus),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = TapBotColors.SurfaceElevated,
                                contentColor = TapBotColors.TextPrimary
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TapBotColors.BrandCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export & Copy Redacted Diagnostics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (reportPreview != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                                .background(TapBotColors.Canvas)
                                .border(1.dp, TapBotColors.Border, RoundedCornerShape(TapBotColors.RadiusMd))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = reportPreview!!,
                                color = TapBotColors.TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // 6. Emergency Fleet Control
            item(key = "sec_emergency_stop") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(TapBotColors.RadiusLg))
                        .background(TapBotColors.SurfacePrimary)
                        .premiumCardBorder(borderColorTop = if (runningCount > 0) TapBotColors.ErrorBorder else TapBotColors.Border)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Emergency Fleet Control",
                        color = TapBotColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Immediately halts all running bot loops and removes the active foreground notification.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = { showConfirmStopAll = true },
                        enabled = runningCount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(TapBotColors.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TapBotColors.ErrorSurface,
                            contentColor = TapBotColors.ErrorText,
                            disabledContainerColor = TapBotColors.SurfaceElevated,
                            disabledContentColor = TapBotColors.TextMuted
                        ),
                        border = BorderStroke(1.dp, if (runningCount > 0) TapBotColors.ErrorBorder else TapBotColors.BorderSubtle)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (runningCount > 0) "Emergency Stop All ($runningCount Active)" else "No Bots Currently Running",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 7. About TapBot
            item(key = "sec_about") {
                ExpandableSettingsSectionCard(
                    icon = Icons.Default.Info,
                    iconColor = TapBotColors.TextSecondary,
                    title = "About TapBot",
                    subtitle = "v$appVersionName ($appVersionCode) • Apache-2.0 Open Source",
                    expanded = SettingsSection.ABOUT in expandedSections,
                    onToggleExpand = { toggleSection(SettingsSection.ABOUT) }
                ) {
                    Text(
                        text = "On-Device Telegram Bot Store & Multi-Runtime Engine powered by Kotlin Coroutines on Android ART.",
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    SettingsInfoRow("Version", "$appVersionName (Build $appVersionCode)")
                    SettingsInfoRow("Target Android", "API 26 – 35 (Android 8–15)")
                    SettingsInfoRow("Runtimes", "Commander v2, TapStream, Exon, Gemini, RSS, Workflow")
                    SettingsInfoRow("Catalog Edge", preferences.customCatalogUrl.ifBlank { defaultCatalogUrl })
                    SettingsInfoRow("License", "Apache-2.0")
                }
            }
        }
    }

    if (showCustomUrlSheet) {
        CustomCatalogUrlSheet(
            currentUrl = preferences.customCatalogUrl,
            defaultUrl = defaultCatalogUrl,
            onDismiss = { showCustomUrlSheet = false },
            onSaveUrl = { url ->
                onSaveCustomCatalogUrl(url)
                statusNotice = if (url.isBlank()) {
                    "Reset Catalog API endpoint to default Cloudflare Edge."
                } else {
                    "Updated Catalog API URL and refreshed store."
                }
                showCustomUrlSheet = false
            }
        )
    }

    if (showConfirmClearCache) {
        AlertDialog(
            onDismissRequest = { showConfirmClearCache = false },
            containerColor = TapBotColors.SurfacePrimary,
            title = { Text("Clear Local Cache?", color = TapBotColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Clears cached catalog metadata and temporary .botpkg archives, then re-seeds bundled runtimes. Installed bots and credentials remain untouched.",
                    color = TapBotColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val freed = onClearCache()
                        liveCacheBytes = onRefreshCacheSizeBytes()
                        statusNotice = "Cleared cache (${freed / 1024L} KB freed) & re-verified bundled packages."
                        showConfirmClearCache = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.Warning,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Clear Cache", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearCache = false }) {
                    Text("Cancel", color = TapBotColors.TextSecondary)
                }
            }
        )
    }

    if (showConfirmStopAll) {
        AlertDialog(
            onDismissRequest = { showConfirmStopAll = false },
            containerColor = TapBotColors.SurfacePrimary,
            title = { Text("Stop All Active Bots?", color = TapBotColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This immediately stops all $runningCount active Telegram bot process(es) running in TapBot Foreground Service.",
                    color = TapBotColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStopAllBots()
                        statusNotice = "Sent emergency stop signal to all active bots."
                        showConfirmStopAll = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Stop All Bots", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmStopAll = false }) {
                    Text("Cancel", color = TapBotColors.TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomCatalogUrlSheet(
    currentUrl: String,
    defaultUrl: String,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var urlError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TapBotColors.SurfacePrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Custom Catalog API Endpoint",
                color = TapBotColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Point TapBot to your own Cloudflare Worker or self-hosted catalog JSON registry.",
                color = TapBotColors.TextSecondary,
                fontSize = 12.sp
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    urlError = null
                },
                singleLine = true,
                isError = urlError != null,
                placeholder = { Text(defaultUrl, color = TapBotColors.TextMuted, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TapBotColors.BrandCyan,
                    unfocusedBorderColor = TapBotColors.Border,
                    focusedTextColor = TapBotColors.TextPrimary,
                    unfocusedTextColor = TapBotColors.TextPrimary,
                    focusedContainerColor = TapBotColors.SurfaceElevated,
                    unfocusedContainerColor = TapBotColors.SurfaceElevated
                ),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                modifier = Modifier.fillMaxWidth()
            )

            if (urlError != null) {
                Text(text = urlError!!, color = TapBotColors.ErrorText, fontSize = 11.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onSaveUrl("") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusMd),
                    border = BorderStroke(1.dp, TapBotColors.BorderFocus)
                ) {
                    Text("Use Default Edge", color = TapBotColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                PrimaryButton(
                    text = "Save & Refresh",
                    onClick = {
                        val trimmed = urlInput.trim()
                        if (trimmed.isNotBlank() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                            urlError = "URL must start with http:// or https://"
                        } else {
                            onSaveUrl(trimmed)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ExpandableSettingsSectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    badgeText: String? = null,
    badgeColor: Color = TapBotColors.SuccessText,
    badgeBg: Color = TapBotColors.SuccessSurface,
    badgeBorder: Color = TapBotColors.SuccessBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.SurfacePrimary)
            .premiumCardBorder()
            .animateContentSize(animationSpec = TapBotMotion.ContentSpring)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onToggleExpand)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(21.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = TapBotColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                                .background(badgeBg)
                                .border(1.dp, badgeBorder, RoundedCornerShape(TapBotColors.RadiusPill))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(text = badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TapBotColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = TapBotColors.TextSecondary
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun CatalogSourceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(TapBotColors.RadiusSm))
            .background(if (isSelected) TapBotColors.PrimaryCobalt else TapBotColors.SurfacePrimary)
            .border(
                1.dp,
                if (isSelected) TapBotColors.BrandCyan.copy(alpha = 0.7f) else TapBotColors.Border,
                RoundedCornerShape(TapBotColors.RadiusSm)
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TapBotColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
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
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(TapBotColors.SurfaceElevated)
            .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TapBotColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TapBotColors.TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TapBotColors.BrandCyanInk,
                checkedTrackColor = TapBotColors.BrandCyan,
                uncheckedThumbColor = TapBotColors.TextSecondary,
                uncheckedTrackColor = TapBotColors.SurfaceInteractive
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TapBotColors.SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TapBotColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            color = TapBotColors.TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    } catch (_: Exception) {}
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, heightDp = 820)
@Composable
private fun SettingsPhonePreview() {
    TapBotTheme {
        SettingsScreen(
            installedInstances = listOf(
                BotInstance(
                    installationId = "inst_1",
                    botId = "bot_commander",
                    name = "TapBot Commander",
                    version = "1.0.0",
                    status = BotInstanceStatus.Running()
                )
            ),
            customBotCount = 1,
            initialCacheSizeBytes = 2_450_000L
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 840, heightDp = 760)
@Composable
private fun SettingsTabletPreview() {
    TapBotTheme {
        SettingsScreen(
            preferences = AppPreferences(
                autoStartOnBoot = true,
                wifiOnlyDownloads = true,
                customCatalogUrl = "https://workers.example.dev/catalog"
            )
        )
    }
}
