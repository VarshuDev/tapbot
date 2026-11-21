package com.tapbot.feature.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.tapbot.core.model.BotInstance
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotPackageInfo
import com.tapbot.core.runner.telemetry.DeviceTelemetryProvider
import com.tapbot.core.ui.catalog.BotCommandCatalog
import com.tapbot.core.ui.components.BotCard
import com.tapbot.core.ui.components.BotCardSkeleton
import com.tapbot.core.ui.components.CategoryChip
import com.tapbot.core.ui.components.FeaturedBotCard
import com.tapbot.core.ui.components.MonospaceBadge
import com.tapbot.core.ui.components.PlayStoreRankedBotRow
import com.tapbot.core.ui.components.PlayStoreSearchBar
import com.tapbot.core.ui.components.PlayStoreShelfCard
import com.tapbot.core.ui.components.PlayStoreShelfHeader
import com.tapbot.core.ui.components.PlayStoreSpotlightBanner
import com.tapbot.core.ui.components.PlayStoreSubTabs
import com.tapbot.core.ui.components.PrimaryButton
import com.tapbot.core.ui.components.SearchField
import com.tapbot.core.ui.components.SectionHeader
import com.tapbot.core.ui.components.TapBotLogoMark
import com.tapbot.core.ui.motion.TapBotMotion
import com.tapbot.core.ui.motion.rememberReducedMotion
import com.tapbot.core.ui.theme.TabularNumberStyle
import com.tapbot.core.ui.theme.TapBotColors
import com.tapbot.core.ui.theme.TapBotTheme
import com.tapbot.core.ui.theme.premiumCardBorder
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onBotClick: (String) -> Unit,
    onBuildCustomBotClick: () -> Unit = {},
    onEditCustomBotClick: (String) -> Unit = {},
    onClearCache: () -> Long = { 0L },
    onRefreshCacheSizeBytes: () -> Long = { 0L },
    onBuildDiagnosticsReport: (() -> String)? = null,
    appVersionName: String = "2.6.0",
    appVersionCode: Long = 260L,
    onCheckForAppUpdate: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val appPreferences by viewModel.appPreferences.collectAsState()
    val reducedMotion = rememberReducedMotion()
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var batteryPct by remember {
        mutableIntStateOf(
            DeviceTelemetryProvider(if (isPreview) null else context).getBatteryTelemetry().percentage
        )
    }

    if (!isPreview) {
        DisposableEffect(lifecycleOwner, context) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    batteryPct = DeviceTelemetryProvider(context).getBatteryTelemetry().percentage
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(15_000L)
                batteryPct = DeviceTelemetryProvider(context).getBatteryTelemetry().percentage
            }
        }
    }

    when (val state = uiState) {
        is CatalogUiState.Loading -> {
            CatalogLoadingScaffold(
                batteryPct = batteryPct,
                onBuildCustomBotClick = onBuildCustomBotClick
            )
        }

        is CatalogUiState.Error -> {
            CatalogErrorScreen(
                message = state.message,
                onRetry = { viewModel.loadCatalog(forceRefresh = true) }
            )
        }

        is CatalogUiState.Success -> {
            val runningCount = state.installedInstances.count { it.status.isRunning }

            Scaffold(
                containerColor = TapBotColors.PlayBackground,
                topBar = {
                    if (state.selectedTab == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TapBotColors.PlayBackground)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            PlayStoreSearchBar(
                                query = state.searchQuery,
                                onQueryChange = viewModel::updateSearchQuery,
                                runningCount = runningCount,
                                batteryPct = batteryPct,
                                onTelemetryClick = { viewModel.selectTab(1) },
                                placeholderText = "Search bots, automations & tools..."
                            )
                        }
                    } else {
                        TapBotCatalogTopBar(
                            selectedTab = state.selectedTab,
                            isOffline = state.isOffline,
                            isRefreshing = state.isRefreshing,
                            runningCount = runningCount,
                            batteryPct = batteryPct,
                            onTelemetryClick = { viewModel.selectTab(1) },
                            onBuildCustomBotClick = onBuildCustomBotClick,
                            onRefreshClick = { viewModel.refresh() }
                        )
                    }
                },
                bottomBar = {
                    TapBotBottomNavigationBar(
                        selectedTab = state.selectedTab,
                        onTabSelected = viewModel::selectTab,
                        onBuildCustomBotClick = onBuildCustomBotClick,
                        installedCount = state.installedInstances.size,
                        runningCount = runningCount
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(
                        targetState = state.selectedTab,
                        animationSpec = tween(
                            durationMillis = if (reducedMotion) 0 else TapBotMotion.TabSwapDurationMs,
                            easing = TapBotMotion.StandardEasing
                        ),
                        label = "CatalogTabCrossfade"
                    ) { tab ->
                        when (tab) {
                            0 -> {
                                CatalogStoreContent(
                                    state = state,
                                    reducedMotion = reducedMotion,
                                    onSearchQueryChanged = viewModel::updateSearchQuery,
                                    onCategorySelected = viewModel::selectCategory,
                                    onResetFilters = viewModel::resetFilters,
                                    onBotClick = onBotClick,
                                    onBuildCustomBotClick = onBuildCustomBotClick
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
                                    onStartAll = viewModel::startAllIdleBots,
                                    onStopAll = viewModel::stopAllActiveBots,
                                    onEditCustomBot = onEditCustomBotClick,
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
                                    onBuildDiagnosticsReport = onBuildDiagnosticsReport,
                                    onCheckForAppUpdate = onCheckForAppUpdate
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TapBotCatalogTopBar(
    selectedTab: Int,
    isOffline: Boolean,
    isRefreshing: Boolean,
    runningCount: Int,
    batteryPct: Int,
    onTelemetryClick: () -> Unit,
    onBuildCustomBotClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TapBotLogoMark(size = 32.dp)
                Column {
                    Text(
                        text = when (selectedTab) {
                            1 -> "My Fleet"
                            2 -> "Settings"
                            else -> "Bot Store"
                        },
                        color = TapBotColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (selectedTab) {
                            1 -> "On-Device ART Runtime"
                            2 -> "Node Console Preferences"
                            else -> if (isOffline) "Offline Local Cache" else "Cloudflare Edge Verified"
                        },
                        color = if (isOffline && selectedTab == 0) TapBotColors.WarningText else TapBotColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            NodeTelemetryPill(
                runningCount = runningCount,
                batteryPct = batteryPct,
                onClick = onTelemetryClick
            )
            if (selectedTab == 0 || selectedTab == 1) {
                IconButton(
                    onClick = onBuildCustomBotClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Build Custom Bot",
                        tint = TapBotColors.BrandCyan
                    )
                }
            }
            if (selectedTab == 0) {
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = TapBotColors.BrandCyan
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh catalog",
                            tint = TapBotColors.TextSecondary
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TapBotColors.Canvas
        ),
        modifier = Modifier.border(
            width = 1.dp,
            color = TapBotColors.BorderSubtle,
            shape = RoundedCornerShape(0.dp)
        )
    )
}

@Composable
private fun NodeTelemetryPill(
    runningCount: Int,
    batteryPct: Int,
    onClick: () -> Unit
) {
    val active = runningCount > 0
    val bgColor = if (active) TapBotColors.SuccessSurface else TapBotColors.SurfaceElevated
    val borderColor = if (active) TapBotColors.SuccessBorder else TapBotColors.Border
    val dotColor = if (active) TapBotColors.Success else TapBotColors.TextMuted
    val textColor = if (active) TapBotColors.SuccessText else TapBotColors.TextSecondary

    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(TapBotColors.RadiusPill))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = "$runningCount • $batteryPct%",
                color = textColor,
                style = TabularNumberStyle.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogLoadingScaffold(
    batteryPct: Int,
    onBuildCustomBotClick: () -> Unit
) {
    Scaffold(
        containerColor = TapBotColors.Canvas,
        topBar = {
            TapBotCatalogTopBar(
                selectedTab = 0,
                isOffline = false,
                isRefreshing = true,
                runningCount = 0,
                batteryPct = batteryPct,
                onTelemetryClick = {},
                onBuildCustomBotClick = onBuildCustomBotClick,
                onRefreshClick = {}
            )
        },
        bottomBar = {
            TapBotBottomNavigationBar(
                selectedTab = 0,
                onTabSelected = {},
                installedCount = 0,
                runningCount = 0
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(TapBotColors.Canvas),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                        .background(TapBotColors.SurfaceElevated)
                        .border(1.dp, TapBotColors.BorderSubtle, RoundedCornerShape(TapBotColors.RadiusMd))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                                .background(TapBotColors.SurfaceElevated)
                        )
                    }
                }
                BotCardSkeleton()
                BotCardSkeleton()
                BotCardSkeleton()
            }
        }
    }
}

@Composable
private fun CatalogErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBotColors.Canvas)
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
                .premiumCardBorder(borderColorTop = TapBotColors.ErrorBorder)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TapBotColors.ErrorSurface)
                    .border(1.dp, TapBotColors.ErrorBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = TapBotColors.ErrorText,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Unable to connect to store",
                color = TapBotColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = TapBotColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = "Retry Connection",
                onClick = onRetry,
                icon = Icons.Default.Refresh
            )
        }
    }
}

@Composable
internal fun CatalogStoreContent(
    state: CatalogUiState.Success,
    reducedMotion: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onResetFilters: () -> Unit,
    onBotClick: (String) -> Unit,
    onBuildCustomBotClick: () -> Unit
) {
    val installedBotIds = remember(state.installedInstances) {
        state.installedInstances.map { it.botId }.toSet()
    }
    val runningBotIds = remember(state.installedInstances) {
        state.installedInstances.filter { it.status.isRunning }.map { it.botId }.toSet()
    }
    val updateBotIds = remember(state.installedInstances, state.availableUpdates) {
        state.installedInstances
            .filter { state.availableUpdates.containsKey(it.installationId) }
            .map { it.botId }
            .toSet()
    }

    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }
    val showOnboarding = state.installedInstances.isEmpty() && !onboardingDismissed && state.searchQuery.isBlank()
    val commanderTargetId = remember(state.bots, state.featuredBots) {
        state.bots.firstOrNull {
            it.id == "bot_commander" || it.id == "bot_commander_flagship" || it.id == "tapbot-commander" || it.name.equals("TapBot Commander", ignoreCase = true)
        }?.id
            ?: state.featuredBots.firstOrNull()?.id
            ?: state.bots.firstOrNull()?.id
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TapBotColors.PlayBackground),
        contentAlignment = Alignment.TopCenter
    ) {
        val isTabletWidth = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Play Store Sub-Tabs
            item(key = "store_sub_tabs") {
                PlayStoreSubTabs(
                    selectedTab = selectedSubTab,
                    onTabSelected = { selectedSubTab = it }
                )
            }

            if (state.isOffline) {
                item(key = "offline_banner") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
                            .background(TapBotColors.WarningSurface)
                            .border(1.dp, TapBotColors.WarningBorder, RoundedCornerShape(TapBotColors.RadiusMd))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = TapBotColors.WarningText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Offline mode — browsing verified local catalog cache.",
                            color = TapBotColors.WarningText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (state.searchQuery.isNotBlank()) {
                // Search Results View
                item(key = "search_results_header") {
                    SectionHeader(
                        title = "Results for \"${state.searchQuery}\" (${state.bots.size})",
                        actionText = "Clear Search",
                        onActionClick = { onSearchQueryChanged("") }
                    )
                }

                if (state.bots.isEmpty()) {
                    item(key = "search_empty") {
                        EmptyFilterRecoveryCard(
                            query = state.searchQuery,
                            category = state.selectedCategory,
                            onResetFilters = onResetFilters,
                            onBuildCustomBotClick = onBuildCustomBotClick
                        )
                    }
                } else {
                    itemsIndexed(state.bots, key = { _, bot -> bot.id }) { index, bot ->
                        PlayStoreRankedBotRow(
                            rank = index + 1,
                            bot = bot,
                            isInstalled = installedBotIds.contains(bot.id),
                            isRunning = runningBotIds.contains(bot.id),
                            hasUpdate = updateBotIds.contains(bot.id),
                            onClick = { onBotClick(bot.id) },
                            onActionClick = { onBotClick(bot.id) }
                        )
                    }
                }
            } else {
                when (selectedSubTab) {
                    0 -> {
                        // "For you"
                        if (showOnboarding) {
                            item(key = "first_run_onboarding") {
                                FirstRunOnboardingCard(
                                    hasStarterBot = commanderTargetId != null,
                                    onSetupStarterClick = { commanderTargetId?.let(onBotClick) },
                                    onBuildCustomBotClick = onBuildCustomBotClick,
                                    onDismiss = { onboardingDismissed = true }
                                )
                            }
                        }

                        // Hero Spotlight Carousel
                        if (state.featuredBots.isNotEmpty()) {
                            item(key = "hero_spotlight") {
                                FeaturedBotsCarousel(
                                    featuredBots = state.featuredBots,
                                    installedBotIds = installedBotIds,
                                    runningBotIds = runningBotIds,
                                    onBotClick = onBotClick
                                )
                            }
                        }

                        // Shelf 1: Top Free Telegram Bots (#1, #2, #3)
                        item(key = "shelf_top_free") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlayStoreShelfHeader(
                                    title = "Top Free Telegram Bots",
                                    subtitle = "Most popular on-device runtimes",
                                    actionText = "Top charts",
                                    onActionClick = { selectedSubTab = 1 }
                                )
                                state.bots.take(3).forEachIndexed { index, bot ->
                                    PlayStoreRankedBotRow(
                                        rank = index + 1,
                                        bot = bot,
                                        isInstalled = installedBotIds.contains(bot.id),
                                        isRunning = runningBotIds.contains(bot.id),
                                        hasUpdate = updateBotIds.contains(bot.id),
                                        onClick = { onBotClick(bot.id) },
                                        onActionClick = { onBotClick(bot.id) }
                                    )
                                }
                            }
                        }

                        // Shelf 2: Essential Phone Utilities
                        item(key = "shelf_utilities") {
                            val utilityBots = state.bots.filter {
                                it.category.equals("Automation", ignoreCase = true) || it.category.equals("Utilities", ignoreCase = true)
                            }.ifEmpty { state.bots.take(4) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlayStoreShelfHeader(
                                    title = "Essential Phone Utilities",
                                    subtitle = "Diagnostics, battery watchdog & system automation",
                                    onActionClick = {
                                        onCategorySelected("Automation")
                                        selectedSubTab = 1
                                    }
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(utilityBots, key = { it.id }) { bot ->
                                        PlayStoreShelfCard(
                                            bot = bot,
                                            isInstalled = installedBotIds.contains(bot.id),
                                            isRunning = runningBotIds.contains(bot.id),
                                            onClick = { onBotClick(bot.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Shelf 3: Smart Assistants & AI
                        item(key = "shelf_ai") {
                            val aiBots = state.bots.filter {
                                it.category.equals("AI", ignoreCase = true)
                            }.ifEmpty { state.bots.takeLast(4) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlayStoreShelfHeader(
                                    title = "Smart Assistants & AI",
                                    subtitle = "On-device intelligence & Gemini integration",
                                    onActionClick = {
                                        onCategorySelected("AI")
                                        selectedSubTab = 1
                                    }
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(aiBots, key = { it.id }) { bot ->
                                        PlayStoreShelfCard(
                                            bot = bot,
                                            isInstalled = installedBotIds.contains(bot.id),
                                            isRunning = runningBotIds.contains(bot.id),
                                            onClick = { onBotClick(bot.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Shelf 4: Audio & Media Streamers
                        item(key = "shelf_media") {
                            val mediaBots = state.bots.filter {
                                it.category.equals("Media", ignoreCase = true)
                            }.ifEmpty { state.bots.take(4) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlayStoreShelfHeader(
                                    title = "Audio & Media Streamers",
                                    subtitle = "Direct speaker playback & 24/7 radio bots",
                                    onActionClick = {
                                        onCategorySelected("Media")
                                        selectedSubTab = 1
                                    }
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(mediaBots, key = { it.id }) { bot ->
                                        PlayStoreShelfCard(
                                            bot = bot,
                                            isInstalled = installedBotIds.contains(bot.id),
                                            isRunning = runningBotIds.contains(bot.id),
                                            onClick = { onBotClick(bot.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Shelf 5: Group Moderation & Security
                        item(key = "shelf_moderation") {
                            val modBots = state.bots.filter {
                                it.category.equals("Moderation", ignoreCase = true)
                            }.ifEmpty { state.bots.take(4) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlayStoreShelfHeader(
                                    title = "Group Moderation & Security",
                                    subtitle = "Anti-flood, spam guard & group management",
                                    onActionClick = {
                                        onCategorySelected("Moderation")
                                        selectedSubTab = 1
                                    }
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(modBots, key = { it.id }) { bot ->
                                        PlayStoreShelfCard(
                                            bot = bot,
                                            isInstalled = installedBotIds.contains(bot.id),
                                            isRunning = runningBotIds.contains(bot.id),
                                            onClick = { onBotClick(bot.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Shelf 6: Daily Life, Habits & Wellness
                        item(key = "shelf_lifestyle") {
                            val lifestyleBots = state.bots.filter {
                                it.category.equals("Lifestyle", ignoreCase = true)
                            }
                            if (lifestyleBots.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PlayStoreShelfHeader(
                                        title = "Daily Life, Habits & Wellness",
                                        subtitle = "Streak trackers, scheduled alarms & hydration logs",
                                        onActionClick = {
                                            onCategorySelected("Lifestyle")
                                            selectedSubTab = 1
                                        }
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(lifestyleBots, key = { it.id }) { bot ->
                                            PlayStoreShelfCard(
                                                bot = bot,
                                                isInstalled = installedBotIds.contains(bot.id),
                                                isRunning = runningBotIds.contains(bot.id),
                                                onClick = { onBotClick(bot.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Shelf 7: Personal Finance & Market Watchers
                        item(key = "shelf_finance") {
                            val financeBots = state.bots.filter {
                                it.category.equals("Finance", ignoreCase = true)
                            }
                            if (financeBots.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PlayStoreShelfHeader(
                                        title = "Personal Finance & Market Watchers",
                                        subtitle = "Expense logs, monthly budgets & live market radar",
                                        onActionClick = {
                                            onCategorySelected("Finance")
                                            selectedSubTab = 1
                                        }
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(financeBots, key = { it.id }) { bot ->
                                            PlayStoreShelfCard(
                                                bot = bot,
                                                isInstalled = installedBotIds.contains(bot.id),
                                                isRunning = runningBotIds.contains(bot.id),
                                                onClick = { onBotClick(bot.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Shelf 8: Deep Work & Thought Scratchpad
                        item(key = "shelf_productivity") {
                            val prodBots = state.bots.filter {
                                it.category.equals("Productivity", ignoreCase = true)
                            }
                            if (prodBots.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PlayStoreShelfHeader(
                                        title = "Deep Work & Thought Scratchpad",
                                        subtitle = "25/5 Pomodoro intervals, checklists & markdown notes",
                                        onActionClick = {
                                            onCategorySelected("Productivity")
                                            selectedSubTab = 1
                                        }
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(prodBots, key = { it.id }) { bot ->
                                            PlayStoreShelfCard(
                                                bot = bot,
                                                isInstalled = installedBotIds.contains(bot.id),
                                                isRunning = runningBotIds.contains(bot.id),
                                                onClick = { onBotClick(bot.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "studio_promo") {
                            CustomBuilderPromoCard(onClick = onBuildCustomBotClick)
                        }
                    }

                    1 -> {
                        // "Top charts"
                        item(key = "chart_category_chips") {
                            val categories = if (state.categories.contains("All")) {
                                state.categories
                            } else {
                                listOf("All") + state.categories
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories, key = { it }) { category ->
                                    CategoryChip(
                                        text = category,
                                        count = if (state.selectedCategory == category) state.bots.size else null,
                                        isSelected = state.selectedCategory == category,
                                        onClick = { onCategorySelected(category) }
                                    )
                                }
                            }
                        }

                        item(key = "charts_header") {
                            PlayStoreShelfHeader(
                                title = if (state.selectedCategory == "All") "Top Free Charts" else "${state.selectedCategory} Charts",
                                subtitle = "Ranked by active device deployments & user rating",
                                actionText = if (state.selectedCategory != "All") "Show All" else null,
                                onActionClick = { onResetFilters() }
                            )
                        }

                        itemsIndexed(state.bots, key = { _, bot -> bot.id }) { index, bot ->
                            PlayStoreRankedBotRow(
                                rank = index + 1,
                                bot = bot,
                                isInstalled = installedBotIds.contains(bot.id),
                                isRunning = runningBotIds.contains(bot.id),
                                hasUpdate = updateBotIds.contains(bot.id),
                                onClick = { onBotClick(bot.id) },
                                onActionClick = { onBotClick(bot.id) }
                            )
                        }
                    }

                    2 -> {
                        // "Categories"
                        item(key = "categories_header") {
                            PlayStoreShelfHeader(
                                title = "Browse by Category",
                                subtitle = "Discover specialized runtimes crafted for your phone",
                                actionText = null,
                                onActionClick = null
                            )
                        }

                        item(key = "categories_list") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PlayStoreCategoryRow(
                                    title = "Automation & Utilities",
                                    description = "Phone telemetry, hardware controls, alarms, APK sender",
                                    icon = Icons.Rounded.Bolt,
                                    accentColor = TapBotColors.BrandCyan,
                                    count = state.bots.count { it.category.equals("Automation", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("Automation")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "AI & Smart Assistants",
                                    description = "Gemini integration, context-aware queries, code assistance",
                                    icon = Icons.Rounded.Terminal,
                                    accentColor = TapBotColors.PrimaryCobalt,
                                    count = state.bots.count { it.category.equals("AI", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("AI")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "Audio & Media",
                                    description = "Bluetooth speaker streaming, 24/7 internet radio, inline players",
                                    icon = Icons.Default.SmartToy,
                                    accentColor = Color(0xFFA855F7),
                                    count = state.bots.count { it.category.equals("Media", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("Media")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "Group Moderation",
                                    description = "Anti-flood, spam shields, lock channels, auto-warns and bans",
                                    icon = Icons.Rounded.Bolt,
                                    accentColor = TapBotColors.Success,
                                    count = state.bots.count { it.category.equals("Moderation", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("Moderation")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "Lifestyle & Wellness",
                                    description = "Streak habit trackers, health reminders, water & workout logs",
                                    icon = Icons.Rounded.CheckCircle,
                                    accentColor = TapBotColors.Success,
                                    count = state.bots.count { it.category.equals("Lifestyle", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("Lifestyle")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "Personal Finance",
                                    description = "Expense ledgers, monthly budget burn rates, crypto market quotes",
                                    icon = Icons.Rounded.AccountBalanceWallet,
                                    accentColor = TapBotColors.Warning,
                                    count = state.bots.count { it.category.equals("Finance", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("Finance")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "Productivity & Focus",
                                    description = "25/5 Pomodoro intervals, task checklists & markdown scratchpad",
                                    icon = Icons.Rounded.Timer,
                                    accentColor = Color(0xFFF43F5E),
                                    count = state.bots.count { it.category.equals("Productivity", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("Productivity")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "Developer Tools",
                                    description = "GitHub repository event monitoring, CI/CD status, PR reviews",
                                    icon = Icons.Rounded.Code,
                                    accentColor = Color(0xFF64748B),
                                    count = state.bots.count { it.category.equals("Developer Tools", ignoreCase = true) },
                                    onClick = {
                                        onCategorySelected("Developer Tools")
                                        selectedSubTab = 1
                                    }
                                )
                                PlayStoreCategoryRow(
                                    title = "Custom & Studio Bots",
                                    description = "User-authored no-code workflows, webhooks & responders",
                                    icon = Icons.Rounded.AccountTree,
                                    accentColor = TapBotColors.AccentCustom,
                                    count = state.customBotCount,
                                    onClick = onBuildCustomBotClick
                                )
                            }
                        }
                    }

                    3 -> {
                        // "Studio Blueprints"
                        item(key = "blueprints_header") {
                            PlayStoreShelfHeader(
                                title = "TapBot Studio Blueprints",
                                subtitle = "Pre-architected templates you can fork and deploy in seconds",
                                actionText = "+ Build Custom",
                                onActionClick = onBuildCustomBotClick
                            )
                        }

                        item(key = "blueprints_content") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                BlueprintCard(
                                    title = "Webhook Alert Relay",
                                    subtitle = "GitHub • Stripe • Vercel",
                                    description = "Listen on local/tunnel HTTP port for incoming webhooks and dispatch formatted alerts to Telegram.",
                                    tag = "WEBHOOK",
                                    tagColor = TapBotColors.BrandCyan,
                                    onUseBlueprint = onBuildCustomBotClick
                                )
                                BlueprintCard(
                                    title = "Customer Support Auto-Responder",
                                    subtitle = "Keyword Triggers • Interactive Buttons",
                                    description = "Automatically respond to common customer inquiries with pre-configured rich message buttons.",
                                    tag = "INTERACTIVE",
                                    tagColor = Color(0xFFA855F7),
                                    onUseBlueprint = onBuildCustomBotClick
                                )
                                BlueprintCard(
                                    title = "Scheduled System Heartbeat",
                                    subtitle = "Cron Worker • Telemetry Ping",
                                    description = "Sends scheduled battery, thermal, and network health reports to your Telegram chat every hour.",
                                    tag = "SCHEDULED",
                                    tagColor = TapBotColors.Success,
                                    onUseBlueprint = onBuildCustomBotClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstRunOnboardingCard(
    hasStarterBot: Boolean,
    onSetupStarterClick: () -> Unit,
    onBuildCustomBotClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(
                Brush.linearGradient(
                    listOf(
                        TapBotColors.BrandCyanSurface,
                        TapBotColors.SurfacePrimary
                    )
                )
            )
            .premiumCardBorder(
                borderColorTop = TapBotColors.BrandCyan.copy(alpha = 0.55f),
                borderColorBottom = TapBotColors.Border
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TapBotColors.SurfaceElevated)
                        .border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = TapBotColors.BrandCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Run Telegram bots 24/7 on your phone",
                        color = TapBotColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zero VPS hosting • Encrypted in Keystore",
                        color = TapBotColors.BrandCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss onboarding banner",
                    tint = TapBotColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Deploy TapBot Commander to control phone diagnostics from Telegram, or craft custom slash commands and webhooks in minutes.",
            color = TapBotColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasStarterBot) {
                Button(
                    onClick = onSetupStarterClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(TapBotColors.RadiusMd),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TapBotColors.BrandCyan,
                        contentColor = TapBotColors.BrandCyanInk
                    )
                ) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Setup Commander",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            OutlinedButton(
                onClick = onBuildCustomBotClick,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                border = BorderStroke(1.dp, TapBotColors.BrandCyanBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = TapBotColors.SurfaceElevated,
                    contentColor = TapBotColors.TextPrimary
                )
            ) {
                Icon(Icons.Rounded.AccountTree, contentDescription = null, tint = TapBotColors.BrandCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Create No-Code Bot",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FeaturedBotsCarousel(
    featuredBots: List<BotMetadata>,
    installedBotIds: Set<String>,
    runningBotIds: Set<String>,
    onBotClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { featuredBots.size })

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val bot = featuredBots[page]
            PlayStoreSpotlightBanner(
                bot = bot,
                isInstalled = installedBotIds.contains(bot.id),
                isRunning = runningBotIds.contains(bot.id),
                onBotClick = { onBotClick(bot.id) },
                onActionClick = { onBotClick(bot.id) }
            )
        }

        if (featuredBots.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(featuredBots.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(if (selected) 20.dp else 6.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                            .background(if (selected) TapBotColors.BrandCyan else TapBotColors.PlayBorderSubtle)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayStoreCategoryRow(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TapBotColors.PlaySurface)
            .border(1.dp, TapBotColors.PlayBorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TapBotColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TapBotColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TapBotColors.RadiusPill))
                    .background(TapBotColors.PlaySurfaceElevated)
                    .border(1.dp, TapBotColors.PlayBorderSubtle, RoundedCornerShape(TapBotColors.RadiusPill))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$count bots",
                    color = TapBotColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BlueprintCard(
    title: String,
    subtitle: String,
    description: String,
    tag: String,
    tagColor: Color,
    onUseBlueprint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TapBotColors.PlaySurface)
            .border(1.dp, TapBotColors.PlayBorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tagColor.copy(alpha = 0.18f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = tag,
                    color = tagColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = subtitle,
                color = TapBotColors.TextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            color = TapBotColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            color = TapBotColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onUseBlueprint,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            shape = RoundedCornerShape(TapBotColors.RadiusMd),
            colors = ButtonDefaults.buttonColors(
                containerColor = TapBotColors.PlayInstallGreen,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Use Blueprint in Studio",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyFilterRecoveryCard(
    query: String,
    category: String,
    onResetFilters: () -> Unit,
    onBuildCustomBotClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(TapBotColors.PlaySurface)
            .border(1.dp, TapBotColors.PlayBorderSubtle, RoundedCornerShape(TapBotColors.RadiusLg))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.SearchOff,
            contentDescription = null,
            tint = TapBotColors.TextMuted,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (query.isNotBlank()) "No bots match \"$query\"" else "No bots in $category",
            color = TapBotColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Reset filters to explore all verified runtimes, or build a custom workflow bot.",
            color = TapBotColors.TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onResetFilters,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                border = BorderStroke(1.dp, TapBotColors.PlayBorder)
            ) {
                Text("Reset Filters", color = TapBotColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onBuildCustomBotClick,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(TapBotColors.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TapBotColors.PrimaryCobalt,
                    contentColor = Color.White
                )
            ) {
                Text("Build Custom", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CustomBuilderPromoCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TapBotColors.RadiusLg))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF072622),
                        TapBotColors.PlaySurface
                    )
                )
            )
            .border(
                1.dp,
                TapBotColors.AccentCustom.copy(alpha = 0.50f),
                RoundedCornerShape(TapBotColors.RadiusLg)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TapBotColors.AccentCustom.copy(alpha = 0.16f))
                .border(1.dp, TapBotColors.AccentCustom.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountTree,
                contentDescription = null,
                tint = TapBotColors.AccentCustom,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Build Your Own Workflow Bot",
                color = TapBotColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "No code needed • Slash commands, HTTP webhooks & live chat preview",
                color = TapBotColors.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        MonospaceBadge(
            text = "+ Build",
            color = TapBotColors.AccentCustom,
            backgroundColor = TapBotColors.PlaySurfaceElevated
        )
    }
}

@Composable
private fun TapBotBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    installedCount: Int,
    runningCount: Int,
    modifier: Modifier = Modifier,
    onBuildCustomBotClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TapBotColors.PlaySurface,
        border = BorderStroke(1.dp, TapBotColors.PlayBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TapBotNavItem(
                icon = Icons.Default.Home,
                label = "Store",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )

            TapBotNavItem(
                icon = Icons.Default.SmartToy,
                label = "Fleet",
                isSelected = selectedTab == 1,
                badgeCount = if (installedCount > 0) installedCount else null,
                isActiveRunning = runningCount > 0,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )

            TapBotNavItem(
                icon = Icons.Rounded.AccountTree,
                label = "Studio",
                isSelected = false,
                onClick = onBuildCustomBotClick,
                modifier = Modifier.weight(1f)
            )

            TapBotNavItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TapBotNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
    isActiveRunning: Boolean = false
) {
    val tint = if (isSelected) TapBotColors.BrandCyan else TapBotColors.TextMuted
    val textColor = if (isSelected) TapBotColors.TextPrimary else TapBotColors.TextSecondary
    val pillColor = if (isSelected) TapBotColors.BrandCyanSurface else Color.Transparent

    Column(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(TapBotColors.RadiusMd))
            .background(pillColor)
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, TapBotColors.BrandCyanBorder, RoundedCornerShape(TapBotColors.RadiusMd))
                } else Modifier
            )
            .semantics { selected = isSelected }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            if (badgeCount != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(start = 15.dp)
                        .clip(CircleShape)
                        .background(if (isActiveRunning) TapBotColors.Success else TapBotColors.PrimaryCobalt)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = if (isActiveRunning) TapBotColors.BrandCyanInk else Color.White,
                        style = TabularNumberStyle.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

private fun previewBotsFixture(): List<BotMetadata> = listOf(
    BotMetadata(
        id = "bot_commander",
        name = "TapBot Commander",
        summary = "Host telemetry watchdog, remote phone finder alarm, torch & APK sender.",
        description = "",
        author = "TapBot Core",
        version = "1.0.0",
        iconUrl = "",
        category = "Automation",
        packageInfo = BotPackageInfo("", "", "native_art", packageSizeBytes = 1703L),
        isFeatured = true
    ),
    BotMetadata(
        id = "bot_music",
        name = "TapStream Audio",
        summary = "Direct speaker/Bluetooth streaming, curated 24/7 radio & inline Telegram player.",
        description = "",
        author = "TapBot Media",
        version = "2.0.0",
        iconUrl = "",
        category = "Media",
        packageInfo = BotPackageInfo("", "", "native_art", packageSizeBytes = 2048L),
        isFeatured = true
    ),
    BotMetadata(
        id = "bot_exon",
        name = "Exon Group Guard",
        summary = "Full anti-flood, locks, warn/ban moderation & welcome rules in pure Kotlin.",
        description = "",
        author = "TapBot Security",
        version = "2.0.0",
        iconUrl = "",
        category = "Moderation",
        packageInfo = BotPackageInfo("", "", "built-in", packageSizeBytes = 1500L),
        isFeatured = false
    ),
    BotMetadata(
        id = "bot_ai",
        name = "Gemini AI Studio",
        summary = "Conversational Gemini chat with per-group system prompts & code assist.",
        description = "",
        author = "TapBot AI",
        version = "1.2.0",
        iconUrl = "",
        category = "AI",
        requiredCredentials = listOf(BotCredentialSpec.TELEGRAM_BOT_TOKEN),
        packageInfo = BotPackageInfo("", "", "native_art", packageSizeBytes = 1200L)
    )
)

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, heightDp = 760)
@Composable
private fun CatalogStorePhonePreview() {
    val bots = previewBotsFixture()
    val state = CatalogUiState.Success(
        bots = bots,
        featuredBots = bots.filter { it.isFeatured },
        categories = listOf("All", "Automation", "Media", "Moderation", "AI"),
        installedInstances = listOf(
            BotInstance(
                installationId = "inst_1",
                botId = "bot_commander",
                name = "TapBot Commander",
                version = "1.0.0",
                status = BotInstanceStatus.Running(pollCount = 12, messageCount = 4)
            )
        )
    )
    TapBotTheme {
        Scaffold(
            containerColor = TapBotColors.Canvas,
            topBar = {
                TapBotCatalogTopBar(
                    selectedTab = 0,
                    isOffline = false,
                    isRefreshing = false,
                    runningCount = 1,
                    batteryPct = 85,
                    onTelemetryClick = {},
                    onBuildCustomBotClick = {},
                    onRefreshClick = {}
                )
            },
            bottomBar = {
                TapBotBottomNavigationBar(
                    selectedTab = 0,
                    onTabSelected = {},
                    installedCount = 1,
                    runningCount = 1
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                CatalogStoreContent(
                    state = state,
                    reducedMotion = true,
                    onSearchQueryChanged = {},
                    onCategorySelected = {},
                    onResetFilters = {},
                    onBotClick = {},
                    onBuildCustomBotClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 840, heightDp = 720)
@Composable
private fun CatalogStoreTabletPreview() {
    val bots = previewBotsFixture()
    val state = CatalogUiState.Success(
        bots = bots,
        featuredBots = bots.filter { it.isFeatured },
        categories = listOf("All", "Automation", "Media", "Moderation", "AI")
    )
    TapBotTheme {
        CatalogStoreContent(
            state = state,
            reducedMotion = true,
            onSearchQueryChanged = {},
            onCategorySelected = {},
            onResetFilters = {},
            onBotClick = {},
            onBuildCustomBotClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360, fontScale = 1.3f)
@Composable
private fun CatalogStoreEmptyLargeFontPreview() {
    val state = CatalogUiState.Success(
        bots = emptyList(),
        featuredBots = emptyList(),
        categories = listOf("All", "Automation", "Media"),
        selectedCategory = "Media",
        searchQuery = "postgres",
        isOffline = true
    )
    TapBotTheme {
        CatalogStoreContent(
            state = state,
            reducedMotion = true,
            onSearchQueryChanged = {},
            onCategorySelected = {},
            onResetFilters = {},
            onBotClick = {},
            onBuildCustomBotClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060A11, widthDp = 360)
@Composable
private fun CatalogLoadingPreview() {
    TapBotTheme {
        CatalogLoadingScaffold(batteryPct = 85, onBuildCustomBotClick = {})
    }
}
