package com.tapbot.feature.botdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapbot.core.logging.BotLogRepository
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.network.BotPackageDownloader
import com.tapbot.core.network.CatalogApi
import com.tapbot.core.network.CatalogRepository
import com.tapbot.core.network.DowngradeAttackChecker
import com.tapbot.core.network.LocalBotInstallationManager
import com.tapbot.core.network.ManifestValidator
import com.tapbot.core.network.OfflineFirstCatalogRepository
import com.tapbot.core.network.TelegramApiClient
import com.tapbot.core.runner.BotForegroundService
import com.tapbot.core.runner.BotServiceController
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.runner.manager.BotInstanceManager
import com.tapbot.core.runner.security.CommanderPairingManager
import com.tapbot.core.security.SecretRedactor
import com.tapbot.core.security.SecureCredentialStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

class BotDetailViewModel(
    private val botId: String,
    private val catalogRepository: CatalogRepository,
    private val credentialStore: SecureCredentialStore,
    private val botServiceController: BotServiceController,
    private val logRepository: BotLogRepository,
    private val packageDownloader: BotPackageDownloader? = null,
    private val installationManager: LocalBotInstallationManager? = null,
    private val appVersionCode: Int = 1,
    private val botInstanceManager: BotInstanceManager? = null,
    private val telegramApiClient: TelegramApiClient? = null,
    private val customBotRepository: CustomBotRepository? = null
) : ViewModel() {

    // Secondary constructor accepting CatalogApi for backward compatibility
    constructor(
        botId: String,
        catalogApi: CatalogApi,
        credentialStore: SecureCredentialStore,
        botServiceController: BotServiceController,
        logRepository: BotLogRepository
    ) : this(
        botId = botId,
        catalogRepository = OfflineFirstCatalogRepository(
            remoteSource = object : com.tapbot.core.network.RemoteBotDataSource {
                override suspend fun getPublishedBots(category: String?, search: String?) = catalogApi.getBots()
                override suspend fun getBotDetails(botId: String) = catalogApi.getBotDetails(botId)
                override suspend fun getCategories() = Result.success(listOf("All", "Utilities", "Media", "Productivity"))
                override suspend fun getBotVersions(botId: String) = catalogApi.getBotVersions(botId)
                override suspend fun getLatestVersion(botId: String) = catalogApi.getLatestVersion(botId)
            }
        ),
        credentialStore = credentialStore,
        botServiceController = botServiceController,
        logRepository = logRepository,
        packageDownloader = null,
        installationManager = null
    )

    private val manifestValidator = ManifestValidator(
        appVersionCode = appVersionCode,
        supportedRuntimes = SUPPORTED_RUNTIMES
    )

    private val _uiState = MutableStateFlow<BotDetailUiState>(BotDetailUiState.Loading)
    val uiState: StateFlow<BotDetailUiState> = _uiState.asStateFlow()

    init {
        loadBotData()
        observeRunState()
        observeLogs()
        observeBotUsername()
        observeCustomDefinition()
    }

    private fun loadBotData() {
        viewModelScope.launch {
            _uiState.value = BotDetailUiState.Loading
            val customDef = customBotRepository?.getDefinition(botId)
            val detailsResult = catalogRepository.getBotDetails(botId, forceRefresh = true)
            val resolvedResult = if (detailsResult.isSuccess) {
                detailsResult
            } else {
                customDef?.let { Result.success(it.toBotMetadata()) } ?: detailsResult
            }

            resolvedResult
                .onSuccess { bot ->
                    val savedCredentials = credentialStore.getAllCredentials(botId)
                    var instance = botInstanceManager?.getInstanceByBotId(bot.id)
                    val installedInManager = installationManager?.isInstalled(bot.id) == true

                    // Sync pre-existing installations into BotInstanceManager so My Fleet always reflects installed bots
                    if (installedInManager && botInstanceManager != null && instance == null) {
                        val existingRecord = installationManager?.getInstalledBots()?.find { it.botId == bot.id }
                        val pkgFile = existingRecord?.packagePath?.let(::File)?.takeIf { it.exists() }
                            ?: File.createTempFile("tapbot_sync_${bot.id}_", ".botpkg")
                        botInstanceManager.install(bot, pkgFile)
                        instance = botInstanceManager.getInstanceByBotId(bot.id)
                    }

                    val installed = when {
                        installationManager != null -> installedInManager || (instance != null)
                        botInstanceManager != null -> instance != null
                        else -> packageDownloader == null
                    }
                    val installedVer = installationManager?.getInstalledVersion(bot.id) ?: instance?.version
                    val liveUsername = BotForegroundService.botUsernames.value[botId]
                        ?.trim()
                        ?.removePrefix("@")
                        ?.takeIf { it.isNotBlank() }
                    val activePairing = if (botId == "bot_commander") {
                        CommanderPairingManager.getActiveCode(botId)
                    } else null

                    _uiState.value = BotDetailUiState.Success(
                        bot = customDef?.toBotMetadata() ?: bot,
                        isInstalled = installed,
                        installedVersion = installedVer,
                        credentials = savedCredentials,
                        isConfiguring = savedCredentials.isEmpty(),
                        runState = botServiceController.getBotRunState(botId).value,
                        logs = logRepository.getLogStream(botId).value,
                        verifiedBotUsername = liveUsername,
                        customDefinition = customDef,
                        activePairingCode = activePairing?.code,
                        pairingCodeExpiresAtMs = activePairing?.expiresAtMs
                    )
                }
                .onFailure { err ->
                    _uiState.value = BotDetailUiState.Error(
                        SecretRedactor.redact(err.message ?: "Failed to load bot details")
                    )
                }
        }
    }

    /**
     * Executes the 8-Step Installation Flow:
     * 1. Retrieve manifest
     * 2. Check compatibility (minimumAppVersion, runtime)
     * 3. Check installed version & Downgrade attack protection
     * 4. Download package with progress
     * 5. Verify SHA-256 checksum
     * 6. Install locally
     * 7. Store installed metadata
     * 8. Show configuration screen (does NOT start automatically)
     */
    fun installBot() {
        val initial = _uiState.value as? BotDetailUiState.Success ?: return

        viewModelScope.launch {
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) {
                    st.copy(isInstalling = true, installProgress = 0.05f, installError = null)
                } else st
            }

            val bot = initial.bot

            // Step 2: Check compatibility
            if (bot.minimumAppVersion > appVersionCode) {
                _uiState.update { st ->
                    if (st is BotDetailUiState.Success) {
                        st.copy(
                            isInstalling = false,
                            installError = "App update required (Bot requires App v${bot.minimumAppVersion}, current is v$appVersionCode)"
                        )
                    } else st
                }
                return@launch
            }

            val runtimeNormalized = bot.packageInfo.runtimeType.lowercase()
            if (runtimeNormalized !in SUPPORTED_RUNTIMES) {
                _uiState.update { st ->
                    if (st is BotDetailUiState.Success) {
                        st.copy(
                            isInstalling = false,
                            installError = "Unsupported runtime: ${bot.packageInfo.runtimeType}"
                        )
                    } else st
                }
                return@launch
            }

            // Step 3: Check installed version & Downgrade attack protection
            val currentInstalledVersion = installationManager?.getInstalledVersion(bot.id)
                ?: botInstanceManager?.getInstanceByBotId(bot.id)?.version
            if (currentInstalledVersion != null && DowngradeAttackChecker.isDowngrade(currentInstalledVersion, bot.version)) {
                _uiState.update { st ->
                    if (st is BotDetailUiState.Success) {
                        st.copy(
                            isInstalling = false,
                            installError = "Package downgrade rejected: Candidate v${bot.version} is older than installed v$currentInstalledVersion"
                        )
                    } else st
                }
                return@launch
            }

            if (currentInstalledVersion == bot.version &&
                (installationManager?.isInstalled(bot.id) == true || botInstanceManager?.getInstanceByBotId(bot.id) != null)
            ) {
                if (botInstanceManager != null && botInstanceManager.getInstanceByBotId(bot.id) == null) {
                    val pkgFile = installationManager?.getInstalledBots()?.find { it.botId == bot.id }
                        ?.packagePath?.let(::File)?.takeIf { it.exists() }
                        ?: File.createTempFile("tapbot_${bot.id}_", ".botpkg")
                    botInstanceManager.install(bot, pkgFile)
                }
                _uiState.update { st ->
                    if (st is BotDetailUiState.Success) {
                        st.copy(
                            isInstalling = false,
                            isInstalled = true,
                            installedVersion = currentInstalledVersion,
                            installProgress = 1.0f
                        )
                    } else st
                }
                return@launch
            }

            // Step 4 & 5: Download package & verify SHA-256 + manifest structure
            val tempTarget = File.createTempFile("tapbot_${bot.id}_", ".botpkg")
            val isInternalPkg = isBuiltInInternalPackageUrl(bot.packageInfo.packageUrl)
            val downloadResult = if (packageDownloader != null && bot.packageInfo.packageUrl.isNotBlank()) {
                val res = packageDownloader.downloadPackage(
                    packageUrl = bot.packageInfo.packageUrl,
                    expectedSha256 = bot.packageInfo.sha256Checksum,
                    targetFile = tempTarget,
                    onProgress = { p ->
                        _uiState.update { st ->
                            if (st is BotDetailUiState.Success) st.copy(installProgress = p) else st
                        }
                    }
                )
                if (res.isFailure && isInternalPkg) {
                    Result.success(tempTarget)
                } else if (res.isSuccess && !isInternalPkg) {
                    val archive = res.getOrThrow()
                    manifestValidator.validatePackage(
                        packageFile = archive,
                        expectedBotId = bot.id,
                        expectedVersion = bot.version
                    ).map { archive }
                } else {
                    res
                }
            } else {
                Result.success(tempTarget)
            }

            downloadResult
                .onSuccess { packageArchive ->
                    // Step 6 & 7: Install locally & store metadata (synced with My Fleet)
                    installationManager?.installBot(bot, packageArchive)
                    if (botInstanceManager != null && botInstanceManager.getInstanceByBotId(bot.id) == null) {
                        botInstanceManager.install(bot, packageArchive)
                    }

                    // Step 8: Show configuration screen
                    _uiState.update { st ->
                        if (st is BotDetailUiState.Success) {
                            st.copy(
                                isInstalled = true,
                                isInstalling = false,
                                installedVersion = bot.version,
                                installProgress = 1.0f,
                                isConfiguring = true
                            )
                        } else st
                    }
                }
                .onFailure { err ->
                    runCatching { if (tempTarget.exists()) tempTarget.delete() }
                    _uiState.update { st ->
                        if (st is BotDetailUiState.Success) {
                            st.copy(
                                isInstalling = false,
                                installError = SecretRedactor.redact(
                                    err.message ?: "Package verification or download failed"
                                )
                            )
                        } else st
                    }
                }
        }
    }

    private fun observeRunState() {
        viewModelScope.launch {
            botServiceController.getBotRunState(botId).collect { state ->
                _uiState.update { current ->
                    if (current is BotDetailUiState.Success) {
                        current.copy(runState = state)
                    } else current
                }
            }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            logRepository.getLogStream(botId).collect { logs ->
                _uiState.update { current ->
                    if (current is BotDetailUiState.Success) {
                        current.copy(logs = logs)
                    } else current
                }
            }
        }
    }

    private fun observeBotUsername() {
        viewModelScope.launch {
            BotForegroundService.botUsernames.collect { usernames ->
                val liveUsername = usernames[botId]?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }
                if (liveUsername != null) {
                    _uiState.update { current ->
                        if (current is BotDetailUiState.Success) {
                            current.copy(
                                verifiedBotUsername = liveUsername,
                                tokenVerifyError = null
                            )
                        } else current
                    }
                }
            }
        }
    }

    private fun observeCustomDefinition() {
        val repo = customBotRepository ?: return
        viewModelScope.launch {
            repo.definitions.collect { defs ->
                val def = defs.firstOrNull { it.id == botId }
                if (def != null) {
                    _uiState.update { current ->
                        if (current is BotDetailUiState.Success) {
                            current.copy(
                                customDefinition = def,
                                bot = def.toBotMetadata()
                            )
                        } else current
                    }
                }
            }
        }
    }

    /**
     * Validates all required credentials against format rules (e.g. Telegram token regex).
     * @return true if all credentials are valid; false and sets error state if invalid/missing.
     */
    fun validateCredentials(): Boolean {
        val current = _uiState.value as? BotDetailUiState.Success ?: return false
        val errors = mutableMapOf<String, String>()
        for (spec in current.bot.requiredCredentials) {
            val value = current.credentials[spec.key]?.trim().orEmpty()
            if (spec.isRequired && value.isBlank()) {
                errors[spec.key] = "${spec.label} is required"
            } else if (value.isNotBlank() && (spec.key.equals("bot_token", ignoreCase = true) || spec.key.equals("telegram_bot_token", ignoreCase = true))) {
                val tgRegex = Regex("""^\d{5,16}:[a-zA-Z0-9_-]{6,64}$""")
                if (!tgRegex.matches(value)) {
                    errors[spec.key] = "Invalid Telegram Bot Token format (e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ)"
                }
            } else if (value.isNotBlank() && (spec.key.equals("api_id", ignoreCase = true) || spec.key.equals("app_api_id", ignoreCase = true))) {
                val apiIdRegex = Regex("""^\d{4,12}$""")
                if (!apiIdRegex.matches(value)) {
                    errors[spec.key] = "Invalid App api_id format (must be numeric, e.g. 12345678)"
                }
            } else if (value.isNotBlank() && (spec.key.equals("api_hash", ignoreCase = true) || spec.key.equals("app_api_hash", ignoreCase = true))) {
                val apiHashRegex = Regex("""^[a-fA-F0-9]{32}$""")
                if (!apiHashRegex.matches(value)) {
                    errors[spec.key] = "Invalid App api_hash format (must be 32-character hexadecimal hash)"
                }
            }
        }
        if (errors.isNotEmpty()) {
            _uiState.update {
                if (it is BotDetailUiState.Success) {
                    it.copy(
                        credentialErrors = errors,
                        generalCredentialError = "Please correct the credential errors above before starting the bot."
                    )
                } else it
            }
            return false
        }
        _uiState.update {
            if (it is BotDetailUiState.Success) {
                it.copy(credentialErrors = emptyMap(), generalCredentialError = null)
            } else it
        }
        return true
    }

    fun updateCredential(key: String, value: String) {
        val isTokenKey = key.equals("bot_token", ignoreCase = true) ||
            key.equals("telegram_bot_token", ignoreCase = true)
        _uiState.update { current ->
            if (current !is BotDetailUiState.Success) return@update current
            val updated = current.credentials.toMutableMap().apply { put(key, value) }
            val updatedErrors = current.credentialErrors.toMutableMap().apply { remove(key) }
            current.copy(
                credentials = updated,
                credentialErrors = updatedErrors,
                generalCredentialError = null,
                credentialSaveSuccess = false,
                verifiedBotUsername = if (isTokenKey) null else current.verifiedBotUsername,
                tokenVerifyError = if (isTokenKey) null else current.tokenVerifyError
            )
        }
    }

    fun verifyToken() {
        val current = _uiState.value as? BotDetailUiState.Success ?: return
        val token = current.credentials["bot_token"]?.trim()?.takeIf { it.isNotBlank() }
            ?: current.credentials["telegram_bot_token"]?.trim().orEmpty()

        val tgRegex = Regex("""^\d{5,16}:[a-zA-Z0-9_-]{6,64}$""")
        if (token.isBlank() || !tgRegex.matches(token)) {
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) {
                    st.copy(
                        isVerifyingToken = false,
                        verifiedBotUsername = null,
                        tokenVerifyError = "Invalid Telegram Bot Token format (e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ)"
                    )
                } else st
            }
            return
        }

        val apiClient = telegramApiClient
        if (apiClient == null) {
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) {
                    st.copy(
                        isVerifyingToken = false,
                        tokenVerifyError = "Telegram API client is unavailable."
                    )
                } else st
            }
            return
        }

        viewModelScope.launch {
            SecretRedactor.registerSecret(token)
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) {
                    st.copy(isVerifyingToken = true, tokenVerifyError = null)
                } else st
            }
            apiClient.getMe(token)
                .onSuccess { user ->
                    val username = user.username?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }
                        ?: user.firstName.trim()
                    _uiState.update { st ->
                        if (st is BotDetailUiState.Success) {
                            st.copy(
                                isVerifyingToken = false,
                                verifiedBotUsername = username,
                                tokenVerifyError = null
                            )
                        } else st
                    }
                }
                .onFailure { err ->
                    val redactedErr = SecretRedactor.redact(err.message ?: "Telegram verification failed")
                    _uiState.update { st ->
                        if (st is BotDetailUiState.Success) {
                            st.copy(
                                isVerifyingToken = false,
                                verifiedBotUsername = null,
                                tokenVerifyError = redactedErr
                            )
                        } else st
                    }
                }
        }
    }

    fun saveCredentials(): Boolean {
        val current = _uiState.value as? BotDetailUiState.Success ?: return false
        if (!validateCredentials()) {
            return false
        }
        val credsToSave = current.credentials.toMap()
        viewModelScope.launch {
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) st.copy(isSavingCredentials = true) else st
            }
            for ((key, value) in credsToSave) {
                if (value.isNotBlank()) {
                    credentialStore.saveCredential(botId, key, value)
                    SecretRedactor.registerSecret(value)
                }
            }
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) {
                    st.copy(
                        isSavingCredentials = false,
                        credentialSaveSuccess = true,
                        isConfiguring = false,
                        credentialErrors = emptyMap(),
                        generalCredentialError = null
                    )
                } else st
            }
        }
        return true
    }

    fun deleteCredentials() {
        val current = _uiState.value as? BotDetailUiState.Success ?: return
        val oldSecrets = current.credentials.values.toList()
        viewModelScope.launch {
            credentialStore.deleteCredentials(botId)
            oldSecrets.forEach { SecretRedactor.unregisterSecret(it) }
            if (botId == "bot_commander") {
                CommanderPairingManager.revoke(botId)
            }
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) {
                    st.copy(
                        credentials = emptyMap(),
                        credentialErrors = emptyMap(),
                        generalCredentialError = null,
                        credentialSaveSuccess = false,
                        isConfiguring = true,
                        verifiedBotUsername = null,
                        isVerifyingToken = false,
                        tokenVerifyError = null,
                        activePairingCode = null,
                        pairingCodeExpiresAtMs = null
                    )
                } else st
            }
        }
    }

    fun generateCommanderPairingCode() {
        if (botId != "bot_commander") return
        val challenge = CommanderPairingManager.issuePairingCode(botId)
        _uiState.update { st ->
            if (st is BotDetailUiState.Success) {
                st.copy(
                    activePairingCode = challenge.code,
                    pairingCodeExpiresAtMs = challenge.expiresAtMs
                )
            } else st
        }
    }

    fun setConfiguring(configuring: Boolean) {
        _uiState.update {
            if (it is BotDetailUiState.Success) it.copy(isConfiguring = configuring) else it
        }
    }

    fun startBot() {
        val current = _uiState.value as? BotDetailUiState.Success ?: return
        if (!current.isInstalled) {
            installBot()
            return
        }
        if (!validateCredentials()) {
            return
        }
        val credsToSave = current.credentials.toMap()
        val botMeta = current.bot
        viewModelScope.launch {
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) st.copy(isSavingCredentials = true) else st
            }
            for ((key, value) in credsToSave) {
                if (value.isNotBlank()) {
                    credentialStore.saveCredential(botId, key, value)
                    SecretRedactor.registerSecret(value)
                }
            }
            _uiState.update { st ->
                if (st is BotDetailUiState.Success) {
                    st.copy(
                        isSavingCredentials = false,
                        credentialSaveSuccess = true,
                        isConfiguring = false,
                        credentialErrors = emptyMap(),
                        generalCredentialError = null
                    )
                } else st
            }

            if (botInstanceManager != null) {
                var instance = botInstanceManager.getInstanceByBotId(botId)
                if (instance == null) {
                    val pkgFile = installationManager?.getInstalledBots()?.find { it.botId == botId }
                        ?.packagePath?.let(::File)?.takeIf { it.exists() }
                        ?: File.createTempFile("tapbot_${botId}_", ".botpkg")
                    botInstanceManager.install(botMeta, pkgFile)
                    instance = botInstanceManager.getInstanceByBotId(botId)
                }
                if (instance != null) {
                    val startRes = botInstanceManager.start(instance.installationId)
                    if (startRes.isFailure) {
                        _uiState.update { st ->
                            if (st is BotDetailUiState.Success) {
                                st.copy(
                                    generalCredentialError = SecretRedactor.redact(
                                        startRes.exceptionOrNull()?.message
                                            ?: "Failed to start bot instance"
                                    )
                                )
                            } else st
                        }
                        return@launch
                    }
                }
            }
            botServiceController.startBot(botId)
        }
    }

    fun stopBot() {
        viewModelScope.launch {
            botInstanceManager?.getInstanceByBotId(botId)?.let { inst ->
                botInstanceManager.stop(inst.installationId)
            }
        }
        botServiceController.stopBot(botId)
    }

    fun clearLogs() {
        logRepository.clearLogs(botId)
    }

    private fun isBuiltInInternalPackageUrl(packageUrl: String): Boolean {
        val trimmed = packageUrl.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.startsWith("local://", ignoreCase = true) ||
            trimmed.startsWith("asset://", ignoreCase = true)
        ) {
            return true
        }
        if (!trimmed.contains("://")) {
            return true
        }
        val host = runCatching { URI(trimmed).host }.getOrNull() ?: return false
        return host.equals("assets.tapbot.internal", ignoreCase = true)
    }

    companion object {
        private val SUPPORTED_RUNTIMES = setOf("native_art", "built-in", "builtin", "custom")

        fun provideFactory(
            botId: String,
            catalogRepository: CatalogRepository,
            credentialStore: SecureCredentialStore,
            botServiceController: BotServiceController,
            logRepository: BotLogRepository,
            packageDownloader: BotPackageDownloader? = null,
            installationManager: LocalBotInstallationManager? = null,
            appVersionCode: Int = 1,
            botInstanceManager: BotInstanceManager? = null,
            telegramApiClient: TelegramApiClient? = null,
            customBotRepository: CustomBotRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BotDetailViewModel(
                    botId = botId,
                    catalogRepository = catalogRepository,
                    credentialStore = credentialStore,
                    botServiceController = botServiceController,
                    logRepository = logRepository,
                    packageDownloader = packageDownloader,
                    installationManager = installationManager,
                    appVersionCode = appVersionCode,
                    botInstanceManager = botInstanceManager,
                    telegramApiClient = telegramApiClient,
                    customBotRepository = customBotRepository
                ) as T
            }
        }

        fun provideFactory(
            botId: String,
            catalogApi: CatalogApi,
            credentialStore: SecureCredentialStore,
            botServiceController: BotServiceController,
            logRepository: BotLogRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BotDetailViewModel(
                    botId,
                    catalogApi,
                    credentialStore,
                    botServiceController,
                    logRepository
                ) as T
            }
        }
    }
}
