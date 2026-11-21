package com.tapbot.core.network

import com.tapbot.core.model.AppUpdateManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State representing OTA app update status.
 */
sealed class AppUpdateUiState {
    object Idle : AppUpdateUiState()
    object Checking : AppUpdateUiState()

    data class UpdateAvailable(
        val manifest: AppUpdateManifest,
        val isForceUpdate: Boolean,
        val isManualCheck: Boolean
    ) : AppUpdateUiState()

    data class UpToDate(
        val currentVersionCode: Long,
        val isManualCheck: Boolean
    ) : AppUpdateUiState()

    data class Error(
        val message: String,
        val isManualCheck: Boolean
    ) : AppUpdateUiState()
}

/**
 * State coordinator managing remote version checks, UI notifications, and mandatory force update state.
 */
class AppUpdateManager(
    private val checker: AppUpdateChecker = AppUpdateChecker(),
    private val scope: CoroutineScope
) {
    private val _updateState = MutableStateFlow<AppUpdateUiState>(AppUpdateUiState.Idle)
    val updateState: StateFlow<AppUpdateUiState> = _updateState.asStateFlow()

    fun checkForUpdates(
        currentVersionCode: Long,
        isManualCheck: Boolean = false,
        customUrl: String? = null
    ) {
        _updateState.value = AppUpdateUiState.Checking
        scope.launch {
            val result = checker.check(currentVersionCode, customUrl)
            result.onSuccess { checkResult ->
                when (checkResult) {
                    is AppUpdateCheckResult.UpdateAvailable -> {
                        _updateState.value = AppUpdateUiState.UpdateAvailable(
                            manifest = checkResult.manifest,
                            isForceUpdate = checkResult.isForceUpdate,
                            isManualCheck = isManualCheck
                        )
                    }
                    is AppUpdateCheckResult.UpToDate -> {
                        _updateState.value = AppUpdateUiState.UpToDate(
                            currentVersionCode = currentVersionCode,
                            isManualCheck = isManualCheck
                        )
                    }
                }
            }.onFailure { error ->
                _updateState.value = AppUpdateUiState.Error(
                    message = error.localizedMessage ?: "Failed to check for updates",
                    isManualCheck = isManualCheck
                )
            }
        }
    }

    /**
     * Dismiss optional update notifications or status messages.
     * Force updates cannot be dismissed.
     */
    fun dismissUpdate() {
        val current = _updateState.value
        if (current is AppUpdateUiState.UpdateAvailable && !current.isForceUpdate) {
            _updateState.value = AppUpdateUiState.Idle
        } else if (current is AppUpdateUiState.UpToDate || current is AppUpdateUiState.Error) {
            _updateState.value = AppUpdateUiState.Idle
        }
    }
}
