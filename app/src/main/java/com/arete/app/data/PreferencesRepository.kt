package com.arete.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class PreferencesRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val restTimerKey = booleanPreferencesKey("rest_timer_enabled")
    private val weeklyGoalKey = intPreferencesKey("weekly_training_goal")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val restTimerEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[restTimerKey] ?: true
    }

    val weeklyGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        (preferences[weeklyGoalKey] ?: 3).coerceIn(1, 14)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setRestTimerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[restTimerKey] = enabled }
    }

    suspend fun setWeeklyGoal(goal: Int) {
        context.dataStore.edit { it[weeklyGoalKey] = goal.coerceIn(1, 14) }
    }
}
