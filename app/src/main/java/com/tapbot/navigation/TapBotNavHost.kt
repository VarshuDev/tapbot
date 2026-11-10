package com.tapbot.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tapbot.TapBotApplication
import com.tapbot.core.ui.motion.TapBotMotion
import com.tapbot.core.ui.motion.rememberReducedMotion
import com.tapbot.feature.botdetail.BotDetailScreen
import com.tapbot.feature.botdetail.BotDetailViewModel
import com.tapbot.feature.catalog.CatalogScreen
import com.tapbot.feature.catalog.CatalogViewModel
import com.tapbot.feature.catalog.CustomBotBuilderScreen
import com.tapbot.feature.catalog.CustomBotBuilderViewModel

@Composable
fun TapBotNavHost(
    app: TapBotApplication,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()

    NavHost(
        navController = navController,
        startDestination = Screen.Catalog.route,
        modifier = modifier,
        enterTransition = {
            if (reducedMotion) {
                fadeIn(animationSpec = tween(0))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(TapBotMotion.EnterDurationMs, easing = TapBotMotion.StandardEasing),
                    initialOffset = { (it * 0.12f).toInt() }
                ) + fadeIn(
                    animationSpec = tween(TapBotMotion.EnterDurationMs, easing = TapBotMotion.StandardEasing)
                )
            }
        },
        exitTransition = {
            if (reducedMotion) {
                fadeOut(animationSpec = tween(0))
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(TapBotMotion.ExitDurationMs, easing = TapBotMotion.StandardEasing),
                    targetOffset = { (it * 0.08f).toInt() }
                ) + fadeOut(
                    animationSpec = tween(TapBotMotion.ExitDurationMs, easing = TapBotMotion.StandardEasing)
                )
            }
        },
        popEnterTransition = {
            if (reducedMotion) {
                fadeIn(animationSpec = tween(0))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(TapBotMotion.EnterDurationMs, easing = TapBotMotion.StandardEasing),
                    initialOffset = { (it * 0.12f).toInt() }
                ) + fadeIn(
                    animationSpec = tween(TapBotMotion.EnterDurationMs, easing = TapBotMotion.StandardEasing)
                )
            }
        },
        popExitTransition = {
            if (reducedMotion) {
                fadeOut(animationSpec = tween(0))
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(TapBotMotion.ExitDurationMs, easing = TapBotMotion.StandardEasing),
                    targetOffset = { (it * 0.08f).toInt() }
                ) + fadeOut(
                    animationSpec = tween(TapBotMotion.ExitDurationMs, easing = TapBotMotion.StandardEasing)
                )
            }
        }
    ) {
        composable(route = Screen.Catalog.route) {
            val catalogViewModel: CatalogViewModel = viewModel(
                factory = CatalogViewModel.provideFactory(
                    catalogRepository = app.catalogRepository,
                    botInstanceManager = app.botInstanceManager,
                    packageDownloader = app.botPackageDownloader,
                    installationManager = app.localBotInstallationManager,
                    appPreferencesRepository = app.appPreferencesRepository,
                    customBotRepository = app.customBotRepository
                )
            )
            CatalogScreen(
                viewModel = catalogViewModel,
                onBotClick = { botId ->
                    navController.navigate(Screen.BotDetail.createRoute(botId))
                },
                onBuildCustomBotClick = {
                    navController.navigate(Screen.CustomBotBuilder.createRoute())
                },
                onEditCustomBotClick = { botId ->
                    navController.navigate(Screen.CustomBotBuilder.createRoute(botId))
                },
                onClearCache = { app.clearAppCache() },
                onRefreshCacheSizeBytes = { app.getCacheSizeBytes() },
                onBuildDiagnosticsReport = { app.buildDiagnosticsReport() },
                appVersionName = app.getAppVersionName(),
                appVersionCode = app.getAppVersionCode().toLong()
            )
        }

        composable(
            route = Screen.CustomBotBuilder.route,
            arguments = listOf(
                navArgument(Screen.CustomBotBuilder.ARG_EDIT_BOT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val editBotId = backStackEntry.arguments?.getString(Screen.CustomBotBuilder.ARG_EDIT_BOT_ID)
            val builderViewModel: CustomBotBuilderViewModel = viewModel(
                factory = CustomBotBuilderViewModel.provideFactory(
                    customBotRepository = app.customBotRepository,
                    botInstanceManager = app.botInstanceManager,
                    credentialStore = app.credentialStore,
                    localCatalogCache = app.localCatalogCache,
                    localInstallationManager = app.localBotInstallationManager
                )
            )
            LaunchedEffect(editBotId) {
                if (!editBotId.isNullOrBlank()) {
                    builderViewModel.loadBotForEditing(editBotId)
                }
            }
            CustomBotBuilderScreen(
                viewModel = builderViewModel,
                onBackClick = { navController.popBackStack() },
                onBotCreated = { botId ->
                    if (!editBotId.isNullOrBlank()) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Screen.BotDetail.createRoute(botId)) {
                            popUpTo(Screen.CustomBotBuilder.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.BotDetail.route,
            arguments = listOf(
                navArgument("botId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId") ?: return@composable
            val botDetailViewModel: BotDetailViewModel = viewModel(
                factory = BotDetailViewModel.provideFactory(
                    botId = botId,
                    catalogRepository = app.catalogRepository,
                    credentialStore = app.credentialStore,
                    botServiceController = app.botServiceController,
                    logRepository = app.logRepository,
                    packageDownloader = app.botPackageDownloader,
                    installationManager = app.localBotInstallationManager,
                    appVersionCode = app.getAppVersionCode(),
                    botInstanceManager = app.botInstanceManager,
                    telegramApiClient = app.telegramApiClient,
                    customBotRepository = app.customBotRepository
                )
            )
            BotDetailScreen(
                viewModel = botDetailViewModel,
                onBackClick = { navController.popBackStack() },
                onEditCustomBot = { editId ->
                    navController.navigate(Screen.CustomBotBuilder.createRoute(editId))
                }
            )
        }

        composable(route = Screen.Poc.route) {
            val pocViewModel: com.tapbot.poc.ProofOfConceptViewModel = viewModel(
                factory = com.tapbot.poc.ProofOfConceptViewModel.provideFactory(
                    credentialStore = app.pocCredentialStore,
                    botInstanceManager = app.botInstanceManager,
                    logRepository = app.logRepository
                )
            )
            com.tapbot.poc.ProofOfConceptScreen(viewModel = pocViewModel)
        }
    }
}
