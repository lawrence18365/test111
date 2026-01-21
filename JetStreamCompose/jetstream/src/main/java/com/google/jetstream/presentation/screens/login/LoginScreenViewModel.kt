/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.jetstream.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.repositories.xtream.XtreamRepository
import com.google.jetstream.data.repositories.xtream.XtreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    fun loginWithM3u(m3uUrl: String) {
        if (m3uUrl.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter an M3U URL")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = xtreamRepository.setM3uUrl(m3uUrl)) {
                is XtreamResult.Success -> {
                    _uiState.value = LoginUiState.Success
                }
                is XtreamResult.Error -> {
                    _uiState.value = LoginUiState.Error(result.message)
                }
                else -> {}
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
