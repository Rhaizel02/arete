package com.arete.app

import android.app.Application
import com.arete.app.data.AreteRepository
import com.arete.app.data.BackupService
import com.arete.app.data.CatalogSeeder
import com.arete.app.data.PreferencesRepository
import com.arete.app.data.ProgressRepository
import com.arete.app.data.PlanningRepository
import com.arete.app.data.SessionRepository
import com.arete.app.data.local.AreteDatabase
import com.arete.app.reminders.WorkoutReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.arete.app.backup.AutomaticBackupPreferences
import com.arete.app.backup.AutomaticBackupCoordinator
import com.arete.app.backup.SafBackupDestination
import com.arete.app.backup.BackupWorkScheduler

class AreteApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database: AreteDatabase by lazy { AreteDatabase.create(this) }
    val repository: AreteRepository by lazy {
        AreteRepository(database, CatalogSeeder(this, database))
    }
    val preferences: PreferencesRepository by lazy { PreferencesRepository(this) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(database) }
    val progressRepository: ProgressRepository by lazy { ProgressRepository(database) }
    val planningRepository: PlanningRepository by lazy { PlanningRepository(database, preferences) }
    val backupService: BackupService by lazy { BackupService(database, preferences) }
    val reminderScheduler: WorkoutReminderScheduler by lazy { WorkoutReminderScheduler(this) }
    val restAlarmScheduler by lazy { com.arete.app.reminders.RestAlarmScheduler(this) }
    val restTimers by lazy { com.arete.app.reminders.RestTimerCoordinator(database, preferences, restAlarmScheduler) }
    val automaticBackupPreferences by lazy { AutomaticBackupPreferences(this) }
    val backupWorkScheduler by lazy { BackupWorkScheduler(this) }
    val automaticBackups by lazy { AutomaticBackupCoordinator(backupService, automaticBackupPreferences, SafBackupDestination(this), backupWorkScheduler) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            preferences.settings.collectLatest(reminderScheduler::schedule)
        }
        applicationScope.launch {
            runCatching { automaticBackups.reconcile(recoverInterrupted = true) }
                .onFailure { error -> automaticBackupPreferences.update { it.copy(
                    outcome = com.arete.app.backup.BackupOutcome.ERROR,
                    detail = "No se pudo comprobar la programación de copias: ${error.message.orEmpty()}") } }
        }
    }

    suspend fun scheduleCurrentReminder() {
        reminderScheduler.schedule(preferences.settings.first())
    }

    fun onForeground(visible: Boolean) {
        restTimers.appVisible = visible
        if (visible) applicationScope.launch { restTimers.reconcile(dropExpired = true) }
    }
}
