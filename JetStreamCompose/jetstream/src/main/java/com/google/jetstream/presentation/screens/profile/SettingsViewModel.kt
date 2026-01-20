package com.google.jetstream.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetstream.data.repositories.SettingsRepository
import com.google.jetstream.presentation.theme.ThemeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeOption = settingsRepository.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeOption.Ocean
    )

    val parentalEnabled = settingsRepository.parentalEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val isPinSet = settingsRepository.isParentalPinSet.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    fun setTheme(option: ThemeOption) {
        viewModelScope.launch {
            settingsRepository.setTheme(option)
        }
    }

    fun setParentalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setParentalEnabled(enabled)
        }
    }

    fun setParentalPin(pin: String) {
        viewModelScope.launch {
            settingsRepository.setParentalPin(pin)
        }
    }

    fun clearParentalPin() {
        viewModelScope.launch {
            settingsRepository.clearParentalPin()
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return settingsRepository.verifyPin(pin)
    }
}
