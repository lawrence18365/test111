package com.google.jetstream.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.jetstream.presentation.theme.ThemeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
    }

    val themeFlow: Flow<ThemeOption> = context.dataStore.data.map { preferences ->
        ThemeOption.fromId(preferences[KEY_THEME])
    }

    suspend fun setTheme(option: ThemeOption) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = option.id
        }
    }
}
