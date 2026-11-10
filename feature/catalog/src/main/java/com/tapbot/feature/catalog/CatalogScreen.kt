package com.tapbot.feature.catalog

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapbot.core.model.BotMetadata
import com.tapbot.feature.catalog.ui.BotCard
import com.tapbot.feature.catalog.ui.BotHubTheme
import com.tapbot.feature.catalog.ui.CategoryChip
import com.tapbot.feature.catalog.ui.FeaturedBotCard
import com.tapbot.feature.catalog.ui.SearchField
import com.tapbot.feature.catalog.ui.SectionHeader
import com.tapbot.feature.catalog.ui.premiumCardBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onBotClick: (String) -> Unit,
    onBuildCustomBotClick: () -> Unit = {},
    onClearCache: () -> Long = { 0L },
    onRefreshCacheSizeBytes: () -> Long = { 0L },
    onBuildDiagnosticsReport: (() -> String)? = null,
    appVersionName: String = "2.3.0",
    appVersionCode: Long = 230L
) {
    val uiState by viewModel.uiState.collectAsState()
    val appPreferences by viewModel.appPreferences.collectAsState()

    when (val state = uiState) {
        is CatalogUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BotHubTheme.Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(BotHubTheme.ElevatedSurface)
                            .border(1.dp, BotHubTheme.Border, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = BotHubTheme.PrimaryBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading Store...",
                        color = BotHubTheme.SecondaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        is CatalogUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BotHubTheme.Background)
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
                        text = "Unable to connect to store",
                        color = BotHubTheme.Error,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = BotHubTheme.SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.loadCatalog(forceRefresh = true) },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(BotHubTheme.RadiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BotHubTheme.PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Connection", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        is CatalogUiState.Success -> {
            Scaffold(
                containerColor = BotHubTheme.Background,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = when (state.selectedTab) {
                                        1 -> "My Fleet"
                                        2 -> "Settings"
                                        else -> "Bot Store"
                                    },
                                    color = BotHubTheme.PrimaryText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (state.selectedTab) {
                                        1 -> "Local On-Device Execution"
                                        2 -> "Preferences, Security & Storage"
                                        else -> "Cloudflare Edge Verified"
                                    },
                                    color = BotHubTheme.SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        actions = {
                            if (state.selectedTab == 0 || state.selectedTab == 1) {
                                IconButton(onClick = onBuildCustomBotClick) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Build Custom Bot",
                                        tint = BotHubTheme.PrimaryBlue
                                    )
                                }
                            }
                            if (state.selectedTab == 0) {
                                IconButton(
                                    onClick = { viewModel.refresh() },
                                    enabled = !state.isRefreshing
                                ) {
                                    if (state.isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = BotHubTheme.PrimaryBlue
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Refresh catalog",
                                            tint = BotHubTheme.SecondaryText
                                        )
                                    }
                                }
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
                },
                bottomBar = {
                    BotHubBottomNavigationBar(
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::selectTab,
                        installedCount = state.installedInstances.size
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (state.selectedTab) {
                        0 -> {
                            CatalogStoreContent(
                                state = state,
                                onSearchQueryChanged = viewModel::updateSearchQuery,
                                onCategorySelected = viewModel::selectCategory,
                                onBotClick = onBotClick
                            )
                        }
                        1 -> {
                            MyBotsContent(
                                instances = state.installedInstances,
                                availableUpdates = state.availableUpdates,
                                updateProgress = state.updateProgress,
                                onStartBot = viewModel::startBot,
                                onStopBot = viewModel::stopBot,
                                onRestartBot = viewModel::restartBot,
                                onConfigureBot = onBotClick,
                                onUninstallBot = viewModel::uninstallBot,
                                onUpdateBot = viewModel::updateBot,
                                onCheckForUpdates = viewModel::checkForUpdates,
                                isCheckingUpdates = state.isCheckingUpdates,
                                onBrowseStoreClick = { viewModel.selectTab(0) },
                                onBuildCustomBotClick = onBuildCustomBotClick
                            )
                        }
                        2 -> {
                            SettingsScreen(
                                preferences = appPreferences,
                                installedInstances = state.installedInstances,
                                customBotCount = state.customBotCount,
                                appVersionName = appVersionName,
                                appVersionCode = appVersionCode,
                                initialCacheSizeBytes = onRefreshCacheSizeBytes(),
                                onAutoStartOnBootChanged = viewModel::setAutoStartOnBoot,
                                onWifiOnlyDownloadsChanged = viewModel::setWifiOnlyDownloads,
                                onDiagnosticsExportEnabledChanged = viewModel::setDiagnosticsExportEnabled,
                                onSaveCustomCatalogUrl = viewModel::saveCustomCatalogUrl,
                                onStopAllBots = viewModel::stopAllRunningBots,
                                onClearCache = onClearCache,
                                onRefreshCacheSizeBytes = onRefreshCacheSizeBytes,
                                onBuildDiagnosticsReport = onBuildDiagnosticsReport
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogStoreContent(
    state: CatalogUiState.Success,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onBotClick: (String) -> Unit
) {
    val installedBotIds = state.installedInstances.map { it.botId }.toSet()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BotHubTheme.Background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Field
        item {
            SearchField(
                query = state.searchQuery,
                onQueryChange = onSearchQueryChanged,
                placeholderText = "Search bots, runtimes, tags..."
            )
        }

        // Category Chips Carousel
        item {
            val categories = if (state.categories.contains("All")) {
                state.categories
            } else {
                listOf("All") + state.categories
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(categories) { category ->
                    CategoryChip(
                        text = category,
                        isSelected = state.selectedCategory == category,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
        }

        // Featured Bot Card (shown when not searching)
        if (state.searchQuery.isBlank() && state.featuredBots.isNotEmpty()) {
            item {
                val featuredBot = state.featuredBots.firstOrNull()
                if (featuredBot != null) {
                    FeaturedBotCard(
                        bot = featuredBot,
                        onClick = { onBotClick(featuredBot.id) }
                    )
                }
            }
        }

        // Popular Bots Section Header
        item {
            SectionHeader(
                title = if (state.searchQuery.isNotBlank()) "Search Results" else "Explore Bots",
                actionText = if (state.searchQuery.isBlank() && state.bots.size > 3) "See All" else null,
                onActionClick = {}
            )
        }

        // Bots List
        if (state.bots.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bots match your criteria",
                        color = BotHubTheme.MutedText,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(state.bots, key = { it.id }) { bot ->
                val isInstalled = installedBotIds.contains(bot.id)
                BotCard(
                    bot = bot,
                    isInstalled = isInstalled,
                    onClick = { onBotClick(bot.id) },
                    onActionClick = { onBotClick(bot.id) }
                )
            }
        }
    }
}

/**
 * Bottom Navigation Bar with illuminated active tab indicator.
 */
@Composable
private fun BotHubBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    installedCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BotHubTheme.PrimarySurface,
        border = BorderStroke(1.dp, BotHubTheme.BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotHubNavItem(
                icon = Icons.Default.Home,
                label = "Store",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )

            BotHubNavItem(
                icon = Icons.Default.SmartToy,
                label = "Fleet",
                isSelected = selectedTab == 1,
                badgeCount = if (installedCount > 0) installedCount else null,
                onClick = { onTabSelected(1) }
            )

            BotHubNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) }
            )
        }
    }
}

@Composable
private fun BotHubNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int? = null
) {
    val tint = if (isSelected) BotHubTheme.PrimaryBlue else BotHubTheme.MutedText
    val textColor = if (isSelected) BotHubTheme.PrimaryBlue else BotHubTheme.SecondaryText

    val backgroundModifier = if (isSelected) {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BotHubTheme.BlueSubtle)
    } else {
        Modifier.clip(RoundedCornerShape(12.dp))
    }

    Column(
        modifier = Modifier
            .then(backgroundModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            if (badgeCount != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(start = 14.dp, top = (-4).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(BotHubTheme.PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
