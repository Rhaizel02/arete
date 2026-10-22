package com.arete.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class WeeklyPlanEntry(val dayOfWeek: Int, val workoutId: String)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val restTimerEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val weeklyGoal: Int = 3,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val weeklyPlan: List<WeeklyPlanEntry> = emptyList()
)

@Serializable
data class BackupPreferences(
    val themeMode: String = ThemeMode.SYSTEM.name,
    val restTimerEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val weeklyGoal: Int = 3,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val weeklyPlan: List<WeeklyPlanEntry> = emptyList()
)

class PreferencesRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val themeKey = stringPreferencesKey("theme_mode")
    private val restTimerKey = booleanPreferencesKey("rest_timer_enabled")
    private val vibrationKey = booleanPreferencesKey("vibration_enabled")
    private val weeklyGoalKey = intPreferencesKey("weekly_training_goal")
    private val remindersKey = booleanPreferencesKey("reminders_enabled")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("reminder_minute")
    private val weeklyPlanKey = stringPreferencesKey("weekly_plan_v1")

    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            themeMode = preferences[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            restTimerEnabled = preferences[restTimerKey] ?: true,
            vibrationEnabled = preferences[vibrationKey] ?: true,
            weeklyGoal = (preferences[weeklyGoalKey] ?: 3).coerceIn(1, 14),
            remindersEnabled = preferences[remindersKey] ?: false,
            reminderHour = (preferences[reminderHourKey] ?: 18).coerceIn(0, 23),
            reminderMinute = (preferences[reminderMinuteKey] ?: 0).coerceIn(0, 59),
            weeklyPlan = decodePlan(preferences[weeklyPlanKey])
        )
    }

    val themeMode: Flow<ThemeMode> = settings.map { it.themeMode }
    val restTimerEnabled: Flow<Boolean> = settings.map { it.restTimerEnabled }
    val vibrationEnabled: Flow<Boolean> = settings.map { it.vibrationEnabled }
    val weeklyGoal: Flow<Int> = settings.map { it.weeklyGoal }
    val remindersEnabled: Flow<Boolean> = settings.map { it.remindersEnabled }
    val reminderHour: Flow<Int> = settings.map { it.reminderHour }
    val reminderMinute: Flow<Int> = settings.map { it.reminderMinute }
    val weeklyPlan: Flow<List<WeeklyPlanEntry>> = settings.map { it.weeklyPlan }

    suspend fun setThemeMode(mode: ThemeMode) = update { it[themeKey] = mode.name }
    suspend fun setRestTimerEnabled(enabled: Boolean) = update { it[restTimerKey] = enabled }
    suspend fun setVibrationEnabled(enabled: Boolean) = update { it[vibrationKey] = enabled }
    suspend fun setWeeklyGoal(goal: Int) = update { it[weeklyGoalKey] = goal.coerceIn(1, 14) }
    suspend fun setRemindersEnabled(enabled: Boolean) = update { it[remindersKey] = enabled }

    suspend fun setReminderTime(hour: Int, minute: Int) = update {
        it[reminderHourKey] = hour.coerceIn(0, 23)
        it[reminderMinuteKey] = minute.coerceIn(0, 59)
    }

    suspend fun setPlannedWorkout(dayOfWeek: Int, workoutId: String?) {
        require(dayOfWeek in 1..7) { "Día de semana no válido" }
        context.dataStore.edit { preferences ->
            val plan = decodePlan(preferences[weeklyPlanKey]).associateBy { it.dayOfWeek }.toMutableMap()
            if (workoutId.isNullOrBlank()) plan.remove(dayOfWeek)
            else plan[dayOfWeek] = WeeklyPlanEntry(dayOfWeek, workoutId)
            preferences[weeklyPlanKey] = json.encodeToString(plan.values.sortedBy { it.dayOfWeek })
        }
    }

    suspend fun backupSnapshot(): BackupPreferences = settings.first().toBackup()

    suspend fun restore(snapshot: BackupPreferences) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = snapshot.themeMode
            preferences[restTimerKey] = snapshot.restTimerEnabled
            preferences[vibrationKey] = snapshot.vibrationEnabled
            preferences[weeklyGoalKey] = snapshot.weeklyGoal
            preferences[remindersKey] = snapshot.remindersEnabled
            preferences[reminderHourKey] = snapshot.reminderHour
            preferences[reminderMinuteKey] = snapshot.reminderMinute
            preferences[weeklyPlanKey] = json.encodeToString(snapshot.weeklyPlan.sortedBy { it.dayOfWeek })
        }
    }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private fun decodePlan(raw: String?): List<WeeklyPlanEntry> = raw?.let {
        runCatching { json.decodeFromString<List<WeeklyPlanEntry>>(it) }.getOrDefault(emptyList())
    }.orEmpty().filter { it.dayOfWeek in 1..7 && it.workoutId.isNotBlank() }
        .distinctBy { it.dayOfWeek }.sortedBy { it.dayOfWeek }

    private fun AppSettings.toBackup() = BackupPreferences(
        themeMode = themeMode.name,
        restTimerEnabled = restTimerEnabled,
        vibrationEnabled = vibrationEnabled,
        weeklyGoal = weeklyGoal,
        remindersEnabled = remindersEnabled,
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        weeklyPlan = weeklyPlan
    )
}
