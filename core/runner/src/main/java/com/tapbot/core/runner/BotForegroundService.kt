package com.tapbot.core.runner

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.os.PowerManager
import com.tapbot.core.logging.BotLogRepository
import com.tapbot.core.model.BotInstanceStatus
import com.tapbot.core.model.BotRunState
import com.tapbot.core.model.LogLevel
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.audio.DeviceAudioPlayer
import com.tapbot.core.runner.manager.BotInstanceManager
import com.tapbot.core.runner.runtime.BotRuntime
import com.tapbot.core.runner.runtime.BotRuntimeFactory
import com.tapbot.core.runner.runtime.BotRuntimeState
import com.tapbot.core.runner.runtime.RuntimeContext
import com.tapbot.core.security.CredentialStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Android Foreground Service hosting multiple on-device Telegram Bot runtimes simultaneously.
 * Guarantees that active bots continue executing concurrently in isolated coroutines
 * when the app UI is closed, minimized, or when the screen is locked.
 *
 * Implements strict rules against:
 * - Duplicate processes
 * - Duplicate Telegram connections
 * - Conflicting installations
 * - Orphaned services and background processes
 */
class BotForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private lateinit var notificationHelper: NotificationHelper
    private var powerManager: PowerManager? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _networkState = MutableStateFlow(true)
    val networkState: StateFlow<Boolean> = _networkState.asStateFlow()

    private var lastNotifiedNames: Set<String>? = null
    private var messageWakeLock: PowerManager.WakeLock? = null

    private val lifecycleMutex = Mutex()

    // Map of active running instances: installationId -> ActiveExecution
    private val activeExecutions = ConcurrentHashMap<String, ActiveExecution>()

    private data class ActiveExecution(
        val installationId: String,
        val botId: String,
        val botName: String,
        val token: String,
        val runtime: BotRuntime,
        val job: Job
    )

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.appContext = applicationContext
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val targetInstallationId = intent?.getStringExtra(EXTRA_INSTALLATION_ID)
            ?: intent?.getStringExtra(EXTRA_BOT_ID)

        if (action == ACTION_START_BOT || action == ACTION_RESTART_BOT || action == ACTION_RECOVER_ACTIVE) {
            val count = maxOf(activeExecutions.size, 1)
            val names = activeExecutions.values.map { it.botName }.ifEmpty { listOf(targetInstallationId ?: "Bot") }
            val notification = notificationHelper.buildForegroundNotification(count, names)
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        when (action) {
            ACTION_START_BOT -> {
                serviceScope.launch {
                    startBotInstance(targetInstallationId ?: DEFAULT_BOT_ID)
                }
            }
            ACTION_STOP_BOT -> {
                serviceScope.launch {
                    stopBotInstance(targetInstallationId ?: DEFAULT_BOT_ID)
                }
            }
            ACTION_RESTART_BOT -> {
                serviceScope.launch {
                    val id = targetInstallationId ?: DEFAULT_BOT_ID
                    stopBotInstance(id)
                    startBotInstance(id)
                }
            }
            ACTION_RECOVER_ACTIVE -> {
                val explicitIds = intent?.getStringArrayExtra(EXTRA_INSTALLATION_IDS)?.toList().orEmpty()
                serviceScope.launch {
                    recoverActiveInstances(explicitIds)
                }
            }
            ACTION_STOP_ALL -> {
                serviceScope.launch {
                    stopAllInstances()
                }
            }
            else -> {
                // START_STICKY system recovery: recover active instances from manager
                serviceScope.launch {
                    recoverActiveInstances()
                }
            }
        }

        return START_STICKY
    }

    private suspend fun startBotInstance(installationId: String) = lifecycleMutex.withLock {
        val manager = ServiceLocator.botInstanceManager
        val credStore = ServiceLocator.credentialStore ?: ServiceLocator.pocCredentialStore
        val logRepo = ServiceLocator.logRepository
        val telegramApi = ServiceLocator.telegramApiClient ?: TelegramApiClient()

        // Resolve BotInstance
        val instance = manager?.getInstance(installationId)
            ?: manager?.getInstanceByBotId(installationId)

        val effectiveInstallationId = instance?.installationId ?: installationId
        val botId = instance?.botId ?: installationId
        val botName = instance?.name ?: "Telegram Bot"

        // 1. Duplicate Process Check: is this instance or bot already active?
        if (activeExecutions.containsKey(effectiveInstallationId) ||
            activeExecutions.values.any { it.botId == botId || it.installationId == effectiveInstallationId }
        ) {
            return@withLock
        }

        // Resolve Token
        val token = credStore?.getCredential(botId, "bot_token")
            ?: credStore?.getCredential(botId, "telegram_bot_token")
            ?: credStore?.getToken()

        if (token.isNullOrBlank()) {
            val err = "Cannot start '$botName': Token not found in secure storage."
            logRepo?.appendLog(botId, LogLevel.ERROR, TAG, err)
            manager?.updateInstanceStatus(effectiveInstallationId, BotInstanceStatus.Crashed(err))
            _runtimeState.value = BotRuntimeState.Error(err)
            _botStates.update { it + (botId to BotRunState.Error(err)) }
            checkOrphanedService()
            return@withLock
        }

        // 2. Duplicate Telegram Connection Check: is this exact token already polled by another bot?
        for (active in activeExecutions.values) {
            if (active.token == token && active.installationId != effectiveInstallationId) {
                val err = "Duplicate Telegram connection prevented: Token is already active on '${active.botName}'"
                logRepo?.appendLog(botId, LogLevel.WARN, TAG, err)
                manager?.updateInstanceStatus(effectiveInstallationId, BotInstanceStatus.Crashed(err))
                _botStates.update { it + (botId to BotRunState.Error(err)) }
                checkOrphanedService()
                return@withLock
            }
        }

        // Update state
        manager?.updateInstanceStatus(effectiveInstallationId, BotInstanceStatus.Starting)
        _runtimeState.value = BotRuntimeState.Starting
        _botStates.update { it + (botId to BotRunState.Starting) }

        val runtime = BotRuntimeFactory.createRuntime(botId, botName)

        // 3. Supervised Execution with CoroutineExceptionHandler for Crash Detection
        val crashHandler = CoroutineExceptionHandler { _, throwable ->
            val crashMsg = throwable.message ?: "Uncaught runtime crash in $botName"
            logRepo?.appendLog(botId, LogLevel.ERROR, TAG, "CRASH DETECTED in '$botName': $crashMsg")
            manager?.recordCrash(effectiveInstallationId, crashMsg)
            _botStates.update { it + (botId to BotRunState.Error(crashMsg)) }
            _botUsernames.update { it - botId }
            activeExecutions.remove(effectiveInstallationId)
            updateNotification()
            checkOrphanedService()
        }

        val childJob = serviceScope.launch(crashHandler) {
            runtime.state.collect { st ->
                when (st) {
                    is BotRuntimeState.Connected -> {
                        val uname = st.botUsername.trim().removePrefix("@")
                        if (uname.isNotBlank()) {
                            _botUsernames.update { it + (botId to uname) }
                        }
                    }
                    is BotRuntimeState.Running -> {
                        val uname = st.botUsername.trim().removePrefix("@")
                        if (uname.isNotBlank()) {
                            _botUsernames.update { it + (botId to uname) }
                        }
                        manager?.updateInstanceStatus(
                            effectiveInstallationId,
                            BotInstanceStatus.Running(
                                pollCount = st.pollCount,
                                messageCount = st.messageCount,
                                lastActivityAt = st.lastActivityAt
                            )
                        )
                        _runtimeState.value = st
                        _botStates.update {
                            it + (botId to BotRunState.Running(
                                startedAt = st.startedAt,
                                pollCount = st.pollCount,
                                lastActivityAt = st.lastActivityAt
                            ))
                        }
                        // Note: updateNotification is omitted here to prevent expensive IPC binder churn on every poll tick.
                        // Notifications are only refreshed on lifecycle transitions (add, remove, crash).
                    }
                    is BotRuntimeState.Error -> {
                        logRepo?.appendLog(botId, LogLevel.ERROR, TAG, "Runtime error: ${st.message}")
                        manager?.recordCrash(effectiveInstallationId, st.message)
                        _botStates.update { it + (botId to BotRunState.Error(st.message)) }
                        _botUsernames.update { it - botId }
                    }
                    is BotRuntimeState.Stopped -> {
                        manager?.updateInstanceStatus(effectiveInstallationId, BotInstanceStatus.Stopped)
                        _botStates.update { it + (botId to BotRunState.Stopped) }
                        _botUsernames.update { it - botId }
                    }
                    else -> {}
                }
            }
        }

        val execution = ActiveExecution(
            installationId = effectiveInstallationId,
            botId = botId,
            botName = botName,
            token = token,
            runtime = runtime,
            job = childJob
        )
        activeExecutions[effectiveInstallationId] = execution

        val allCreds = credStore?.getAllCredentials(botId) ?: emptyMap()
        val context = RuntimeContext(
            botId = botId,
            token = token,
            telegramApi = telegramApi,
            log = { level, tag, msg ->
                if (level == LogLevel.INFO && msg.contains("Received message")) {
                    acquireMessageWakeLock()
                } else if (level == LogLevel.INFO && (msg.contains("reply sent") || msg.contains("Replied"))) {
                    releaseMessageWakeLock()
                }
                logRepo?.appendLog(botId, level, tag, msg)
            },
            scope = serviceScope,
            isNetworkAvailable = { _networkState.value },
            networkState = _networkState.asStateFlow(),
            androidContext = this,
            credentials = allCreds
        )

        try {
            runtime.initialize(context)
            runtime.start()
            logRepo?.appendLog(botId, LogLevel.INFO, TAG, "Bot '$botName' running on Android ART.")
        } catch (e: Exception) {
            val err = "Initialization failed: ${e.message}"
            logRepo?.appendLog(botId, LogLevel.ERROR, TAG, err)
            manager?.recordCrash(effectiveInstallationId, err)
            _botStates.update { it + (botId to BotRunState.Error(err)) }
            _botUsernames.update { it - botId }
            activeExecutions.remove(effectiveInstallationId)
            childJob.cancel()
        }

        updateNotification()
    }

    private suspend fun stopBotInstance(installationId: String) = lifecycleMutex.withLock {
        // Resolve target execution either by direct key, botId, or installationId
        val entry = activeExecutions.entries.firstOrNull {
            it.key == installationId || it.value.botId == installationId || it.value.installationId == installationId
        }
        val execution = entry?.let { activeExecutions.remove(it.key) }

        if (execution != null) {
            execution.job.cancel()
            try {
                execution.runtime.stop()
            } catch (_: Exception) {}
            ServiceLocator.logRepository?.appendLog(
                execution.botId,
                LogLevel.INFO,
                TAG,
                "Stopped bot '${execution.botName}' by user command."
            )
            ServiceLocator.botInstanceManager?.updateInstanceStatus(execution.installationId, BotInstanceStatus.Stopped)
            _botStates.update { it + (execution.botId to BotRunState.Stopped) }
            _botUsernames.update { it - execution.botId }
        } else {
            val resolvedInstance = ServiceLocator.botInstanceManager?.getInstance(installationId)
                ?: ServiceLocator.botInstanceManager?.getInstanceByBotId(installationId)
            val resolvedInstId = resolvedInstance?.installationId ?: installationId
            val targetBotId = resolvedInstance?.botId ?: installationId
            ServiceLocator.botInstanceManager?.updateInstanceStatus(resolvedInstId, BotInstanceStatus.Stopped)
            _botStates.update { it + (targetBotId to BotRunState.Stopped) }
            _botUsernames.update { it - targetBotId - installationId }
        }

        updateNotification()
        checkOrphanedService()
    }

    private suspend fun stopAllInstances() = lifecycleMutex.withLock {
        for ((id, execution) in activeExecutions) {
            execution.job.cancel()
            try {
                execution.runtime.stop()
            } catch (_: Exception) {}
            ServiceLocator.botInstanceManager?.updateInstanceStatus(id, BotInstanceStatus.Stopped)
        }
        activeExecutions.clear()
        _runtimeState.value = BotRuntimeState.Stopped
        _botStates.value = emptyMap()
        _botUsernames.value = emptyMap()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun recoverActiveInstances(explicitIds: List<String> = emptyList()) {
        val manager = ServiceLocator.botInstanceManager ?: return
        val managerRunningIds = manager.instances.value
            .filter { it.status.isRunning }
            .map { it.installationId }
        val allIds = (explicitIds + managerRunningIds).distinct()
        for (instId in allIds) {
            startBotInstance(instId)
        }
    }

    /**
     * Updates foreground notification to reflect all concurrently running bots.
     * Throttled to only trigger when the set of active bot names or count actually changes.
     */
    private fun updateNotification(force: Boolean = false) {
        val count = activeExecutions.size
        if (count == 0) {
            lastNotifiedNames = null
            return
        }

        val currentNames = activeExecutions.values.map { it.botName }.toSet()
        if (!force && currentNames == lastNotifiedNames) {
            return // No change in active running bots; avoid expensive IPC binder call
        }
        lastNotifiedNames = currentNames

        val notification = notificationHelper.buildForegroundNotification(count, currentNames.toList())
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
    }

    /**
     * Rule 4: Orphaned Service & Process Prevention
     * When 0 bots are running, stop the foreground notification and shut down the service.
     */
    private fun checkOrphanedService() {
        if (activeExecutions.isEmpty()) {
            _runtimeState.value = BotRuntimeState.Stopped
            _botStates.value = emptyMap()
            _botUsernames.value = emptyMap()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun getOrCreateWakeLock(): PowerManager.WakeLock? {
        if (messageWakeLock == null) {
            messageWakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TapBot:MessageReplyWakeLock")?.apply {
                setReferenceCounted(false)
            }
        }
        return messageWakeLock
    }

    private fun acquireMessageWakeLock(timeoutMs: Long = 2000L) {
        try {
            getOrCreateWakeLock()?.acquire(timeoutMs /* 2s max safety timeout */)
        } catch (e: Exception) {
            ServiceLocator.logRepository?.appendLog(DEFAULT_BOT_ID, LogLevel.WARN, TAG, "WakeLock acquire: ${e.message}")
        }
    }

    private fun releaseMessageWakeLock() {
        try {
            if (messageWakeLock?.isHeld == true) {
                messageWakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkState.value = true
            }

            override fun onLost(network: Network) {
                _networkState.value = false
            }
        }

        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMessageWakeLock()
        serviceJob.cancel()
        networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_BOT = "com.tapbot.action.START_BOT"
        const val ACTION_STOP_BOT = "com.tapbot.action.STOP_BOT"
        const val ACTION_STOP_ALL = "com.tapbot.action.STOP_ALL"
        const val ACTION_RESTART_BOT = "com.tapbot.action.RESTART_BOT"
        const val ACTION_RECOVER_ACTIVE = "com.tapbot.action.RECOVER_ACTIVE"
        const val EXTRA_BOT_ID = "extra_bot_id"
        const val EXTRA_INSTALLATION_ID = "extra_installation_id"
        const val EXTRA_INSTALLATION_IDS = "extra_installation_ids"
        const val DEFAULT_BOT_ID = "bot_pingpong_poc"
        private const val TAG = "BotForegroundService"
        private const val PREFS_NAME = "tapbot_service_state"

        private val _runtimeState = MutableStateFlow<BotRuntimeState>(BotRuntimeState.Stopped)
        val runtimeState: StateFlow<BotRuntimeState> = _runtimeState.asStateFlow()

        private val _botStates = MutableStateFlow<Map<String, BotRunState>>(emptyMap())
        val botStates: StateFlow<Map<String, BotRunState>> = _botStates.asStateFlow()

        private val _botUsernames = MutableStateFlow<Map<String, String>>(emptyMap())
        val botUsernames: StateFlow<Map<String, String>> = _botUsernames.asStateFlow()

        internal fun updateStateDirectly(state: BotRuntimeState) {
            _runtimeState.value = state
        }

        internal fun updateBotState(botId: String, state: BotRunState) {
            _botStates.update { it + (botId to state) }
        }
    }
}

/**
 * ServiceLocator hook for injecting modules into Android Service.
 */
object ServiceLocator {
    var appContext: Context? = null
    var pocCredentialStore: CredentialStore? = null
    var credentialStore: CredentialStore? = null
    var logRepository: BotLogRepository? = null
    var telegramApiClient: TelegramApiClient? = null
    var botInstanceManager: BotInstanceManager? = null
    var appPreferencesRepository: com.tapbot.core.network.AppPreferencesRepository? = null
    var customBotRepository: com.tapbot.core.runner.custom.CustomBotRepository? = null

    private val _activeAudioPlayer = MutableStateFlow<DeviceAudioPlayer?>(null)
    val activeAudioPlayer: StateFlow<DeviceAudioPlayer?> = _activeAudioPlayer.asStateFlow()

    fun setActiveAudioPlayer(player: DeviceAudioPlayer?) {
        _activeAudioPlayer.value = player
    }
}
