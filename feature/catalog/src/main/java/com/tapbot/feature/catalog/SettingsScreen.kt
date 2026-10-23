package com.tapbot.feature.catalog

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.feature.catalog.ui.BotHubTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Calculate free disk storage
    val freeStorageMb = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBytes / (1024 * 1024)).toInt()
        } catch (_: Exception) {
            1024
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BotHubTheme.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Settings",
                color = BotHubTheme.PrimaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Person,
                iconColor = BotHubTheme.PrimaryBlue,
                title = "Account",
                subtitle = "Local device runner • No cloud account required",
                onClick = {}
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Tune,
                iconColor = BotHubTheme.Info,
                title = "App Preferences",
                subtitle = "Dark Navy theme, foreground service notifications",
                onClick = {}
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Security,
                iconColor = BotHubTheme.Success,
                title = "Security & Encryption",
                subtitle = "Hardware Keystore AES-256-GCM, FLAG_SECURE window masking",
                badgeText = "Secured",
                badgeColor = BotHubTheme.Success,
                onClick = {}
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.Storage,
                iconColor = BotHubTheme.Warning,
                title = "Storage & Cache",
                subtitle = "${freeStorageMb} MB free space on device • Package validation enabled",
                onClick = {}
            )
        }

        item {
            SettingsItemCard(
                icon = Icons.Default.BatteryChargingFull,
                iconColor = BotHubTheme.Info,
                title = "Background Execution",
                subtitle = "Tap to configure Android battery optimization for 24/7 uptime",
                onClick = {
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

        item {
            SettingsItemCard(
                icon = Icons.Default.Info,
                iconColor = BotHubTheme.SecondaryText,
                title = "About BotHub",
                subtitle = "Version 1.0.0 (Production Release) • Local Telegram Bot Engine",
                onClick = {}
            )
        }
    }
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
            .clip(RoundedCornerShape(BotHubTheme.RadiusMd))
            .background(BotHubTheme.PrimarySurface)
            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(BotHubTheme.RadiusMd))
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
