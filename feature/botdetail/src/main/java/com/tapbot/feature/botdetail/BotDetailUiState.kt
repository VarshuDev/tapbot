package com.tapbot.feature.botdetail

import com.tapbot.core.model.BotLogEntry
import com.tapbot.core.model.BotMetadata
import com.tapbot.core.model.BotRunState
import com.tapbot.core.model.CustomBotDefinition
import java.util.UUID

enum class SimulatorSender {
    USER,
    BOT,
    SYSTEM
}

data class SimulatorChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: SimulatorSender,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed interface BotDetailUiState {
    object Loading : BotDetailUiState
    data class Success(
        val bot: BotMetadata,
        val isInstalled: Boolean = false,
        val isInstalling: Boolean = false,
        val installProgress: Float = 0.0f,
        val installError: String? = null,
        val installedVersion: String? = null,
        val credentials: Map<String, String> = emptyMap(),
        val credentialErrors: Map<String, String> = emptyMap(),
        val generalCredentialError: String? = null,
        val isConfiguring: Boolean = false,
        val runState: BotRunState = BotRunState.Stopped,
        val logs: List<BotLogEntry> = emptyList(),
        val isSavingCredentials: Boolean = false,
        val credentialSaveSuccess: Boolean = false,
        val verifiedBotUsername: String? = null,
        val isVerifyingToken: Boolean = false,
        val tokenVerifyError: String? = null,
        val customDefinition: CustomBotDefinition? = null,
        val activePairingCode: String? = null,
        val pairingCodeExpiresAtMs: Long? = null,
        val isSyncingCommands: Boolean = false,
        val commandSyncMessage: String? = null,
        val commandSyncError: String? = null,
        val isSimulatorOpen: Boolean = false,
        val simulatorMessages: List<SimulatorChatMessage> = emptyList(),
        val isSimulatorRunning: Boolean = false
    ) : BotDetailUiState
    data class Error(val message: String) : BotDetailUiState
}
