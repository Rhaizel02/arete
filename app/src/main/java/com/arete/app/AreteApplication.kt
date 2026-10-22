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

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            preferences.settings.collectLatest(reminderScheduler::schedule)
        }
    }

    suspend fun scheduleCurrentReminder() {
        reminderScheduler.schedule(preferences.settings.first())
    }
}
