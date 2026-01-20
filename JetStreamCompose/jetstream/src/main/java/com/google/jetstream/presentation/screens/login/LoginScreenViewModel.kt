/*
 * Login Screen ViewModel
 */
package com.google.jetstream.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    private val xtreamRepository: XtreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(serverUrl: String, username: String, password: String) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please fill in all fields")
            return
        }

        // Normalize server URL
        val normalizedUrl = normalizeServerUrl(serverUrl)

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            when (val result = xtreamRepository.authenticate(normalizedUrl, username, password)) {
                is XtreamResult.Success -> {
                    _uiState.value = LoginUiState.Success
                }
                is XtreamResult.Error -> {
                    _uiState.value = LoginUiState.Error(result.message)
                }
                is XtreamResult.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun normalizeServerUrl(url: String): String {
        var normalized = url.trim()

        // Add http:// if no protocol specified
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }

        // Remove trailing slash
        normalized = normalized.trimEnd('/')

        return normalized
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
