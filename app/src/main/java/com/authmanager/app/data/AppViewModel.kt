package com.authmanager.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One-shot UI banners (success/error) — cleared after being shown once. */
data class UiBanner(val message: String, val isError: Boolean)

data class AppUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val snapshot: RepoSnapshot? = null,
    val isFromCache: Boolean = false,
    val banner: UiBanner? = null,
    val isSyncing: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)
    private val sessionStore = SessionStore(application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState

    fun login() {
        _uiState.value = _uiState.value.copy(isLoggedIn = true)
        refresh()
    }

    fun logout() {
        viewModelScope.launch { sessionStore.setRememberMe(false) }
        _uiState.value = AppUiState()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.loadSnapshot()) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snapshot = result.data,
                        isFromCache = result.fromCache,
                        banner = if (result.fromCache) {
                            UiBanner("Offline — showing cached data", isError = true)
                        } else null,
                    )
                }
                is RepoResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        banner = UiBanner(result.message, isError = true),
                    )
                }
            }
        }
    }

    fun clearBanner() {
        _uiState.value = _uiState.value.copy(banner = null)
    }

    fun generateKey(duration: String, deviceLimit: Int, customText: String? = null, onDone: (Boolean, String) -> Unit) {
        runAction(
            action = { repository.generateKey(duration, deviceLimit, customText) },
            onSuccess = { record -> onDone(true, "Key generated: ${record.key}") },
            onFailure = { msg -> onDone(false, msg) },
        )
    }

    fun deleteKey(key: String, onDone: (Boolean, String) -> Unit) {
        runAction(
            action = { repository.deleteKey(key) },
            onSuccess = { count -> onDone(true, "Key deleted. $count device(s) unregistered.") },
            onFailure = { msg -> onDone(false, msg) },
        )
    }

    fun changeKeyDuration(key: String, duration: String, onDone: (Boolean, String) -> Unit) {
        runAction(
            action = { repository.changeKeyDuration(key, duration) },
            onSuccess = { expiry -> onDone(true, "Duration updated. Expires: $expiry") },
            onFailure = { msg -> onDone(false, msg) },
        )
    }

    fun registerDevice(key: String, hash: String, onDone: (Boolean, String) -> Unit) {
        runAction(
            action = { repository.registerDevice(key, hash) },
            onSuccess = { onDone(true, "Device registered to $key") },
            onFailure = { msg -> onDone(false, msg) },
        )
    }

    fun unregisterDevice(hash: String, onDone: (Boolean, String) -> Unit) {
        runAction(
            action = { repository.unregisterDevice(hash) },
            onSuccess = { onDone(true, "Device unregistered") },
            onFailure = { msg -> onDone(false, msg) },
        )
    }

    fun blockDevice(hash: String, onDone: (Boolean, String) -> Unit) {
        runAction(
            action = { repository.blockDevice(hash) },
            onSuccess = { onDone(true, "Device blocked") },
            onFailure = { msg -> onDone(false, msg) },
        )
    }

    fun unblockDevice(hash: String, onDone: (Boolean, String) -> Unit) {
        runAction(
            action = { repository.unblockDevice(hash) },
            onSuccess = { onDone(true, "Device unblocked") },
            onFailure = { msg -> onDone(false, msg) },
        )
    }

    private fun <T> runAction(
        action: suspend () -> RepoResult<T>,
        onSuccess: (T) -> Unit,
        onFailure: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            when (val result = action()) {
                is RepoResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSyncing = false)
                    onSuccess(result.data)
                    refresh()
                }
                is RepoResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSyncing = false)
                    onFailure(result.message)
                }
            }
        }
    }
}
