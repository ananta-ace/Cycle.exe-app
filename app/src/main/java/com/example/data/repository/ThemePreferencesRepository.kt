package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.cycleDataStore by preferencesDataStore(name = "cycle_preferences")

class ThemePreferencesRepository(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("cycle_dark_theme")
    private val dismissedWelcomeKeysPref = stringSetPreferencesKey("cycle_dismissed_welcome_keys")

    val isDarkTheme: Flow<Boolean> = context.cycleDataStore.data.map { prefs ->
        prefs[darkThemeKey] ?: false
    }

    val dismissedWelcomeKeys: Flow<Set<String>> = context.cycleDataStore.data.map { prefs ->
        prefs[dismissedWelcomeKeysPref] ?: emptySet()
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.cycleDataStore.edit { prefs ->
            prefs[darkThemeKey] = enabled
        }
    }

    suspend fun addDismissedWelcomeKey(key: String) {
        context.cycleDataStore.edit { prefs ->
            val current = prefs[dismissedWelcomeKeysPref] ?: emptySet()
            prefs[dismissedWelcomeKeysPref] = current + key
        }
    }
}
