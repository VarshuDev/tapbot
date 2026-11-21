package com.tapbot.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tapbot.core.model.AppUpdateManifest
import com.tapbot.core.ui.theme.TapBotColors

/**
 * Dialog displaying remote version control notifications and enforcing mandatory force updates.
 *
 * When [isForceUpdate] is true:
 * - Dialog cannot be dismissed via back button or outside touch.
 * - Clearly indicates a critical update is required before the app can be used.
 * - Directs the user to download and install the new APK.
 */
@Composable
fun ForceUpdateDialog(
    manifest: AppUpdateManifest,
    currentVersionCode: Long,
    currentVersionName: String,
    isForceUpdate: Boolean,
    onDownloadClick: (downloadUrl: String) -> Unit,
    onOpenReleasePageClick: (url: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!isForceUpdate) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForceUpdate,
            dismissOnClickOutside = !isForceUpdate,
            usePlatformDefaultWidth = false
        )
    ) {
        val accentColor = if (isForceUpdate) Color(0xFFF59E0B) else Color(0xFF10B981)
        val accentBg = if (isForceUpdate) Color(0x22F59E0B) else Color(0x2210B981)
        val accentBorder = if (isForceUpdate) Color(0x66F59E0B) else Color(0x6610B981)

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(1.5.dp, accentBorder), RoundedCornerShape(20.dp)),
            color = TapBotColors.SurfacePrimary,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Icon & Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accentBg)
                        .border(1.dp, accentBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isForceUpdate) Icons.Default.Warning else Icons.Default.Security,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Badge Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(accentBg)
                        .border(1.dp, accentBorder, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isForceUpdate) "CRITICAL UPDATE REQUIRED" else "NEW VERSION AVAILABLE",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = manifest.title.ifBlank { if (isForceUpdate) "TapBot Upgrade Required" else "TapBot Update Available" },
                    color = TapBotColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Version Progression Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TapBotColors.SurfaceElevated)
                        .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT",
                            color = TapBotColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v$currentVersionName ($currentVersionCode)",
                            color = TapBotColors.TextSecondary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "LATEST",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${manifest.effectiveLatestVersionName} (${manifest.effectiveLatestVersionCode})",
                            color = TapBotColors.TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Release Notes Box
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TapBotColors.Canvas)
                        .border(1.dp, TapBotColors.Border, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = "WHAT'S NEW",
                        color = TapBotColors.BrandCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = manifest.effectiveReleaseNotes.ifBlank {
                            "Performance optimizations, security hardening, and bot runtime stability enhancements."
                        },
                        color = TapBotColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (isForceUpdate) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This update is required to ensure protocol compatibility, runtime stability, and security.",
                        color = accentColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Download & Install Action Button (Primary)
                val targetDownloadUrl = manifest.effectiveApkUrl.ifBlank {
                    manifest.effectiveDownloadUrl.ifBlank {
                        manifest.effectiveGithubReleaseUrl
                    }
                }

                Button(
                    onClick = { onDownloadClick(targetDownloadUrl) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.85f))
                                )
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download & Install Update",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Link (View on GitHub / Website)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onOpenReleasePageClick(manifest.effectiveGithubReleaseUrl) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TapBotColors.BorderSubtle)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = TapBotColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Release Notes",
                            color = TapBotColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isForceUpdate) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TapBotColors.BorderSubtle)
                        ) {
                            Text(
                                text = "Later",
                                color = TapBotColors.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
