package com.tapbot.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tapbot.core.model.CommandRule
import com.tapbot.core.model.CustomBotDefinition
import com.tapbot.core.model.CustomHttpMethod
import com.tapbot.core.network.LocalBotInstallationManager
import com.tapbot.core.network.LocalCatalogCache
import com.tapbot.core.runner.custom.CustomBotRepository
import com.tapbot.core.runner.manager.BotInstanceManager
import com.tapbot.core.security.CredentialStore
import com.tapbot.core.security.SecretRedactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class DraftCommandRule(
    val id: String,
    val command: String = "",
    val description: String = "",
    val responseTemplate: String = "",
    val webhookUrl: String = "",
    val webhookMethod: CustomHttpMethod = CustomHttpMethod.GET,
    val webhookBodyTemplate: String = "",
    val buttonLabel: String = "",
    val matchPrefix: Boolean = true
)

data class CustomBotBuilderUiState(
    val botName: String = "",
    val summary: String = "",
    val telegramToken: String = "",
    val welcomeMessage: String = "Hello {user}! Send /help to see my custom commands.",
    val rules: List<DraftCommandRule> = listOf(
        DraftCommandRule(
            id = "rule_1",
            command = "/hello",
            description = "Greets the caller with current time",
            responseTemplate = "Hi {user} (ID: {user_id})! You reached this bot at {date}."
        )
    ),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedBotId: String? = null
)

class CustomBotBuilderViewModel(
    private val customBotRepository: CustomBotRepository,
    private val botInstanceManager: BotInstanceManager? = null,
    private val credentialStore: CredentialStore? = null,
    private val localCatalogCache: LocalCatalogCache? = null,
    private val localInstallationManager: LocalBotInstallationManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomBotBuilderUiState())
    val uiState: StateFlow<CustomBotBuilderUiState> = _uiState.asStateFlow()

    private var ruleCounter = 2

    fun updateBotName(name: String) {
        _uiState.update { it.copy(botName = name, errorMessage = null) }
    }

    fun updateSummary(summary: String) {
        _uiState.update { it.copy(summary = summary, errorMessage = null) }
    }

    fun updateTelegramToken(token: String) {
        _uiState.update { it.copy(telegramToken = token, errorMessage = null) }
    }

    fun updateWelcomeMessage(welcome: String) {
        _uiState.update { it.copy(welcomeMessage = welcome, errorMessage = null) }
    }

    fun addRule() {
        val nextId = "rule_${ruleCounter++}"
        _uiState.update { st ->
            st.copy(
                rules = st.rules + DraftCommandRule(
                    id = nextId,
                    command = "/cmd${st.rules.size + 1}",
                    description = "",
                    responseTemplate = "Received {args} from {user}"
                ),
                errorMessage = null
            )
        }
    }

    fun removeRule(ruleId: String) {
        _uiState.update { st ->
            if (st.rules.size <= 1) {
                st.copy(errorMessage = "At least one command rule is required.")
            } else {
                st.copy(rules = st.rules.filterNot { it.id == ruleId }, errorMessage = null)
            }
        }
    }

    fun updateRule(ruleId: String, transform: (DraftCommandRule) -> DraftCommandRule) {
        _uiState.update { st ->
            st.copy(
                rules = st.rules.map { if (it.id == ruleId) transform(it) else it },
                errorMessage = null
            )
        }
    }

    fun saveCustomBot() {
        val current = _uiState.value
        val name = current.botName.trim()
        val summary = current.summary.trim().ifBlank { "Custom no-code command & webhook bot" }
        val token = current.telegramToken.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Bot Name is required.") }
            return
        }
        if (token.isBlank() || !TELEGRAM_TOKEN_REGEX.matches(token)) {
            _uiState.update {
                it.copy(errorMessage = "Enter a valid Telegram Bot Token from @BotFather (e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ).")
            }
            return
        }

        val cleanedRules = mutableListOf<CommandRule>()
        for ((index, draft) in current.rules.withIndex()) {
            val rawCmd = draft.command.trim()
            val responseTpl = draft.responseTemplate.trim()
            val hookUrl = draft.webhookUrl.trim()

            if (rawCmd.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Command #${index + 1} cannot be blank.") }
                return
            }
            if (responseTpl.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Response template for '$rawCmd' cannot be blank.") }
                return
            }
            if (hookUrl.isNotBlank() && !hookUrl.startsWith("http://") && !hookUrl.startsWith("https://")) {
                _uiState.update { it.copy(errorMessage = "Webhook URL for '$rawCmd' must start with http:// or https://.") }
                return
            }

            cleanedRules.add(
                CommandRule(
                    id = draft.id,
                    command = rawCmd,
                    description = draft.description.trim(),
                    responseTemplate = responseTpl,
                    webhookUrl = hookUrl.takeIf { it.isNotBlank() },
                    webhookMethod = draft.webhookMethod,
                    webhookBodyTemplate = draft.webhookBodyTemplate.trim().takeIf { it.isNotBlank() },
                    buttonLabel = draft.buttonLabel.trim().takeIf { it.isNotBlank() },
                    matchPrefix = draft.matchPrefix
                )
            )
        }

        if (cleanedRules.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one command rule.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val slug = name.lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "workflow" }
                .take(24)
            val botId = "bot_custom_${slug}_${System.currentTimeMillis()}"

            val definition = CustomBotDefinition(
                id = botId,
                name = name,
                summary = summary,
                description = "$summary • Built with TapBot No-Code Builder (${cleanedRules.size} rules).",
                welcomeMessage = current.welcomeMessage.trim().ifBlank { "Hello {user}! Send /help for commands." },
                rules = cleanedRules
            )

            // 1. Persist definition
            customBotRepository.saveDefinition(definition)

            // 2. Store encrypted Telegram token in hardware Keystore
            credentialStore?.saveCredential(botId, "bot_token", token)
            credentialStore?.saveCredential(botId, "telegram_bot_token", token)
            SecretRedactor.registerSecret(token)

            // 3. Register metadata in local catalog cache & Fleet manager
            val metadata = definition.toBotMetadata()
            localCatalogCache?.updateBot(metadata)

            val stubPkg = File.createTempFile("custom_${botId}_", ".botpkg").apply {
                writeText("{\"id\":\"${botId}\",\"version\":\"1.0.0\",\"runtime\":\"built-in\"}")
            }
            try {
                localInstallationManager?.installBot(metadata, stubPkg)
                if (botInstanceManager != null && botInstanceManager.getInstanceByBotId(botId) == null) {
                    botInstanceManager.install(metadata, stubPkg)
                }
            } finally {
                stubPkg.delete()
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = null,
                    savedBotId = botId
                )
            }
        }
    }

    companion object {
        private val TELEGRAM_TOKEN_REGEX = Regex("""^\d{5,16}:[a-zA-Z0-9_-]{6,64}$""")

        fun provideFactory(
            customBotRepository: CustomBotRepository,
            botInstanceManager: BotInstanceManager? = null,
            credentialStore: CredentialStore? = null,
            localCatalogCache: LocalCatalogCache? = null,
            localInstallationManager: LocalBotInstallationManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CustomBotBuilderViewModel(
                    customBotRepository = customBotRepository,
                    botInstanceManager = botInstanceManager,
                    credentialStore = credentialStore,
                    localCatalogCache = localCatalogCache,
                    localInstallationManager = localInstallationManager
                ) as T
            }
        }
    }
}
