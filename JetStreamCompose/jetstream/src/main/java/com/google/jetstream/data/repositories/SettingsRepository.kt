package com.google.jetstream.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.jetstream.presentation.theme.ThemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_PARENTAL_ENABLED = booleanPreferencesKey("parental_enabled")
        private val KEY_PARENTAL_PIN = stringPreferencesKey("parental_pin")
    }

    val themeFlow: Flow<ThemeOption> = context.dataStore.data.map { preferences ->
        ThemeOption.fromId(preferences[KEY_THEME])
    }

    val parentalEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_PARENTAL_ENABLED] ?: false
    }

    val parentalPinHashFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PARENTAL_PIN]
    }

    val isParentalPinSet: Flow<Boolean> = parentalPinHashFlow.map { !it.isNullOrBlank() }

    suspend fun setTheme(option: ThemeOption) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = option.id
        }
    }

    suspend fun setParentalEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PARENTAL_ENABLED] = enabled
        }
    }

    suspend fun setParentalPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PARENTAL_PIN] = hashPin(pin)
        }
    }

    suspend fun clearParentalPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PARENTAL_PIN)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val storedHash = context.dataStore.data.first()[KEY_PARENTAL_PIN] ?: return false
        return storedHash == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(pin.toByteArray())
        return hashed.joinToString("") { "%02x".format(it) }
    }
}
