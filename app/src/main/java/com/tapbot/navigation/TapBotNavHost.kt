package com.tapbot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tapbot.TapBotApplication
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
    NavHost(
        navController = navController,
        startDestination = Screen.Catalog.route,
        modifier = modifier
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
                    navController.navigate(Screen.CustomBotBuilder.route)
                },
                onClearCache = { app.clearAppCache() },
                onRefreshCacheSizeBytes = { app.getCacheSizeBytes() },
                onBuildDiagnosticsReport = { app.buildDiagnosticsReport() },
                appVersionName = app.getAppVersionName(),
                appVersionCode = app.getAppVersionCode().toLong()
            )
        }

        composable(route = Screen.CustomBotBuilder.route) {
            val builderViewModel: CustomBotBuilderViewModel = viewModel(
                factory = CustomBotBuilderViewModel.provideFactory(
                    customBotRepository = app.customBotRepository,
                    botInstanceManager = app.botInstanceManager,
                    credentialStore = app.credentialStore,
                    localCatalogCache = app.localCatalogCache,
                    localInstallationManager = app.localBotInstallationManager
                )
            )
            CustomBotBuilderScreen(
                viewModel = builderViewModel,
                onBackClick = { navController.popBackStack() },
                onBotCreated = { botId ->
                    navController.navigate(Screen.BotDetail.createRoute(botId)) {
                        popUpTo(Screen.CustomBotBuilder.route) { inclusive = true }
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
                    botInstanceManager = app.botInstanceManager
                )
            )
            BotDetailScreen(
                viewModel = botDetailViewModel,
                onBackClick = { navController.popBackStack() }
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
