package com.tapbot

import android.app.Application
import android.os.Build
import com.tapbot.core.logging.BotLogRepository
import com.tapbot.core.logging.InMemoryRingBufferLogRepository
import com.tapbot.core.network.AppPreferencesRepository
import com.tapbot.core.network.BotPackageDownloader
import com.tapbot.core.network.CatalogApi
import com.tapbot.core.network.CatalogRepository
import com.tapbot.core.network.CloudflareCatalogApi
import com.tapbot.core.network.CloudflareRemoteBotDataSource
import com.tapbot.core.network.DefaultLocalBotInstallationManager
import com.tapbot.core.network.LocalBotInstallationManager
import com.tapbot.core.network.LocalCatalogCache
import com.tapbot.core.network.MockCatalogApi
import com.tapbot.core.network.OfflineFirstCatalogRepository
import com.tapbot.core.network.OkHttpBotPackageDownloader
import com.tapbot.core.network.RemoteBotDataSource
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.AndroidBotServiceController
import com.tapbot.core.runner.BotForegroundService
import com.tapbot.core.runner.BotServiceController
import com.tapbot.core.runner.ServiceLocator
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.runner.manager.BotInstanceManager
import com.tapbot.core.runner.manager.DefaultBotInstanceManager
import com.tapbot.core.security.CredentialStore
import com.tapbot.core.security.KeystoreCredentialStore
import com.tapbot.core.security.KeystoreSecureCredentialStore
import com.tapbot.core.security.SecretRedactor
import com.tapbot.core.security.SecureCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

/**
 * Application entry point for TapBot.
 * Coordinates dependency graph and connects foreground service locator hooks.
 */
class TapBotApplication : Application() {

    lateinit var credentialStore: SecureCredentialStore
        private set

    lateinit var logRepository: BotLogRepository
        private set

    lateinit var catalogApi: CatalogApi
        private set

    lateinit var remoteBotDataSource: RemoteBotDataSource
        private set

    lateinit var localCatalogCache: LocalCatalogCache
        private set

    lateinit var catalogRepository: CatalogRepository
        private set

    lateinit var botPackageDownloader: BotPackageDownloader
        private set

    lateinit var localBotInstallationManager: LocalBotInstallationManager
        private set

    lateinit var telegramApiClient: TelegramApiClient
        private set

    lateinit var botServiceController: BotServiceController
        private set

    lateinit var pocCredentialStore: CredentialStore
        private set

    lateinit var botInstanceManager: BotInstanceManager
        private set

    lateinit var appPreferencesRepository: AppPreferencesRepository
        private set

    lateinit var customBotRepository: CustomBotRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 0. Initialize persistent user preferences and custom workflow bot repository
        appPreferencesRepository = AppPreferencesRepository(File(filesDir, "app_preferences.json"))
        customBotRepository = CustomBotRepository(File(filesDir, "custom_bots.json"))

        // 1. Initialize secure storage backed by Android Keystore
        credentialStore = KeystoreSecureCredentialStore(this)
        pocCredentialStore = KeystoreCredentialStore(this)

        // 2. Initialize in-memory ring buffer logging
        logRepository = InMemoryRingBufferLogRepository(maxCapacityPerBot = 500)

        // 3. Initialize Catalog data source, cache, and repository connecting to Cloudflare Workers / D1 API
        val okHttpClient = OkHttpClient()
        val backendUrlProvider: () -> String = {
            appPreferencesRepository.effectiveCatalogUrl(CloudflareCatalogApi.DEFAULT_BASE_URL)
        }
        remoteBotDataSource = CloudflareRemoteBotDataSource(baseUrlProvider = backendUrlProvider, client = okHttpClient)
        localCatalogCache = LocalCatalogCache(cacheFile = File(cacheDir, "catalog_cache.json"))
        // Ensure catalog cache includes flagship built-in bots (e.g. TapBot Commander)
        val cachedBots = localCatalogCache.getCachedBots()
        if (cachedBots != null && cachedBots.none { it.id == "bot_commander" || it.id.contains("commander") }) {
            val cmdBot = MockCatalogApi().getBuiltInBots().find { it.id == "bot_commander" }
            if (cmdBot != null) {
                localCatalogCache.updateBot(cmdBot)
            }
        }
        // Ensure persisted custom workflow bots remain visible in catalog cache across restarts
        for (customDef in customBotRepository.getAll()) {
            localCatalogCache.updateBot(customDef.toBotMetadata())
        }

        catalogRepository = OfflineFirstCatalogRepository(
            remoteSource = remoteBotDataSource,
            cache = localCatalogCache
        )
        catalogApi = CloudflareCatalogApi(baseUrlProvider = backendUrlProvider, fallback = MockCatalogApi())

        // 4. Initialize package downloader and local installation manager
        val packageCacheDir = File(cacheDir, "package_cache")
        seedBundledPackages(packageCacheDir)
        botPackageDownloader = OkHttpBotPackageDownloader(
            client = okHttpClient,
            cacheDir = packageCacheDir
        )
        localBotInstallationManager = DefaultLocalBotInstallationManager(
            storageFile = File(filesDir, "installed_bots.json"),
            installDir = File(filesDir, "installed_packages")
        )

        // 5. Initialize Telegram API client
        telegramApiClient = TelegramApiClient()

        // 6. Initialize runner service controller
        botServiceController = AndroidBotServiceController(this)

        // 7. Initialize BotInstanceManager with persistent storage and hardware Keystore
        botInstanceManager = DefaultBotInstanceManager(
            context = this,
            storageFile = File(filesDir, "instances.json"),
            installDir = File(filesDir, "installed_packages"),
            credentialStore = credentialStore,
            telegramApi = telegramApiClient
        )

        // 8. Connect ServiceLocator hooks for BotForegroundService & BootCompletedReceiver
        ServiceLocator.appContext = this
        ServiceLocator.botInstanceManager = botInstanceManager
        ServiceLocator.credentialStore = credentialStore
        ServiceLocator.pocCredentialStore = pocCredentialStore
        ServiceLocator.logRepository = logRepository
        ServiceLocator.telegramApiClient = telegramApiClient
        ServiceLocator.appPreferencesRepository = appPreferencesRepository
        ServiceLocator.customBotRepository = customBotRepository
    }

    fun getCacheSizeBytes(): Long {
        return computeDirectorySize(cacheDir)
    }

    fun clearAppCache(): Long {
        val before = getCacheSizeBytes()
        localCatalogCache.clearCache()
        cacheDir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                child.deleteRecursively()
            } else {
                child.delete()
            }
        }
        seedBundledPackages(File(cacheDir, "package_cache"))
        for (customDef in customBotRepository.getAll()) {
            localCatalogCache.updateBot(customDef.toBotMetadata())
        }
        val after = getCacheSizeBytes()
        return (before - after).coerceAtLeast(0L)
    }

    fun stopAllRunningBots() {
        appScope.launch {
            val running = botInstanceManager.instances.value.filter { it.status.isRunning }
            for (inst in running) {
                botInstanceManager.stop(inst.installationId)
            }
            val stopAllIntent = android.content.Intent(this@TapBotApplication, BotForegroundService::class.java).apply {
                action = BotForegroundService.ACTION_STOP_ALL
            }
            try {
                startService(stopAllIntent)
            } catch (_: Exception) {}
        }
    }

    fun getAppVersionName(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "2.3.0"
        } catch (_: Exception) {
            "2.3.0"
        }
    }

    fun getAppVersionCode(): Int {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            230
        }
    }

    fun buildDiagnosticsReport(): String {
        val pInfo = try { packageManager.getPackageInfo(packageName, 0) } catch (_: Exception) { null }
        val vName = pInfo?.versionName ?: "2.3.0"
        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo?.longVersionCode ?: 230L
        } else {
            @Suppress("DEPRECATION")
            pInfo?.versionCode?.toLong() ?: 230L
        }
        val instances = botInstanceManager.instances.value
        val prefs = appPreferencesRepository.preferences.value
        val sb = StringBuilder()
        sb.appendLine("=== TapBot Diagnostics Report ===")
        sb.appendLine("App Version: v$vName ($vCode)")
        sb.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
        sb.appendLine("ABI: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}")
        sb.appendLine("Catalog URL: ${appPreferencesRepository.effectiveCatalogUrl()}")
        sb.appendLine("Auto-Start on Boot: ${prefs.autoStartOnBoot}")
        sb.appendLine("Wi-Fi Only Downloads: ${prefs.wifiOnlyDownloads}")
        sb.appendLine("Installed Bots: ${instances.size} (Running: ${instances.count { it.status.isRunning }})")
        sb.appendLine("Custom Workflow Bots: ${customBotRepository.getAll().size}")
        for (inst in instances) {
            sb.appendLine(" - ${inst.name} [${inst.botId}] v${inst.version}: ${inst.status::class.simpleName}")
        }
        sb.appendLine("Recent Redacted Logs:")
        for (inst in instances.take(3)) {
            val logs = logRepository.getLogStream(inst.botId).value.takeLast(5)
            for (entry in logs) {
                sb.appendLine("   [${entry.level.name}] ${entry.botId}: ${SecretRedactor.redact(entry.message)}")
            }
        }
        return sb.toString()
    }

    private fun computeDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { f ->
            total += if (f.isDirectory) computeDirectorySize(f) else f.length()
        }
        return total
    }

    private fun seedBundledPackages(targetDir: File) {
        try {
            targetDir.mkdirs()
            val packageFiles = assets.list("packages") ?: return
            for (filename in packageFiles) {
                if (filename.endsWith(".botpkg")) {
                    val dest = File(targetDir, filename)
                    if (!dest.exists() || dest.length() == 0L) {
                        assets.open("packages/$filename").use { input ->
                            dest.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore asset seeding errors
        }
    }
}
