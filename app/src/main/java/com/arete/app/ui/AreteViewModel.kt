@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.arete.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arete.app.AreteApplication
import com.arete.app.data.AreteRepository
import com.arete.app.data.BackupService
import com.arete.app.data.ExerciseDraft
import com.arete.app.data.PreferencesRepository
import com.arete.app.data.ProgressRepository
import com.arete.app.data.ExerciseProgress
import com.arete.app.data.PlanningRepository
import com.arete.app.data.SetDraft
import com.arete.app.data.SessionRepository
import com.arete.app.data.SessionViewData
import com.arete.app.data.ThemeMode
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.data.local.BodyWeightEntryEntity
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.RoutineExerciseEntity
import com.arete.app.data.local.RoutineWithWorkouts
import com.arete.app.data.local.SessionExerciseEntity
import com.arete.app.data.local.SessionSetEntity
import com.arete.app.data.local.WorkoutSessionEntity
import com.arete.app.data.local.WorkoutEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import com.arete.app.catalog.EditorialManifest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AreteViewModel(
    application: Application,
    private val repository: AreteRepository,
    private val preferences: PreferencesRepository,
    private val sessions: SessionRepository,
    private val progress: ProgressRepository,
    private val planning: PlanningRepository,
    private val backup: BackupService
) : AndroidViewModel(application) {
    private val app = application as AreteApplication
    val automaticBackupState = app.automaticBackupPreferences.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.arete.app.backup.AutomaticBackupState())
    val immediateBackupPending = app.backupWorkScheduler.immediatePending
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val dailyBackupScheduled = app.backupWorkScheduler.dailyScheduled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    var routineExportPayload: String? = null
    val sessionSelections = androidx.compose.runtime.mutableStateMapOf<String, Pair<String, Int>>()
    var backupExportPayload: String? = null
    var pendingBackupRestore by androidx.compose.runtime.mutableStateOf<String?>(null)
    val routines = repository.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val themeMode = preferences.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    val restTimerEnabled = preferences.restTimerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val settings = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.arete.app.data.AppSettings())
    val vibrationEnabled = preferences.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val homeState = sessions.homeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val completedSessions = sessions.completedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val progressState = progress.dashboard(preferences.weeklyGoal)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.arete.app.data.ProgressDashboard())
    val progressPeriod = kotlinx.coroutines.flow.MutableStateFlow(com.arete.app.data.ProgressPeriod.ALL)
    val filteredProgressState = progress.dashboard(preferences.weeklyGoal, progressPeriod)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.arete.app.data.ProgressDashboard())
    val planningState = planning.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.arete.app.data.PlanningState())

    val allExercises = repository.searchExercises("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val exerciseEditorial = flow {
        emit(runCatching { app.assets.open("exercise_editorial.json").bufferedReader().use { EditorialManifest.decode(it.readText()) } }.getOrNull())
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.initialize() }
                .onFailure { _messages.emit("No se pudo cargar el catálogo local") }
        }
    }

    fun observeRoutine(id: String): Flow<RoutineWithWorkouts?> = repository.observeRoutine(id)

    suspend fun getExerciseGuide(id: String): ExerciseEntity? = repository.getExercise(id)

    fun observeSession(id: String): Flow<SessionViewData> = sessions.observeSession(id)

    fun observeExerciseProgress(id: String): Flow<ExerciseProgress?> = progress.observeExerciseProgress(id)

    fun startPlannedWorkout(workoutId: String, onStarted: (String) -> Unit) = launchAction {
        onStarted(sessions.startPlannedWorkout(workoutId))
    }

    fun startFreeWorkout(onStarted: (String) -> Unit) = launchAction {
        onStarted(sessions.startFreeWorkout())
    }

    fun addSessionExercise(sessionId: String, exerciseId: String, onAdded: () -> Unit = {}) = launchAction {
        sessions.addExercise(sessionId, exerciseId)
        onAdded()
    }

    fun substituteSessionExercise(sessionExerciseId: String, exerciseId: String, onDone: () -> Unit = {}) = launchAction("Ejercicio sustituido") {
        sessions.substituteExercise(sessionExerciseId, exerciseId)
        onDone()
    }

    fun deleteSessionExercise(exercise: SessionExerciseEntity) = launchAction {
        sessions.deleteExercise(exercise)
    }

    fun moveSessionExercise(sessionId: String, exerciseId: String, delta: Int) = launchAction {
        sessions.moveExercise(sessionId, exerciseId, delta)
    }

    fun updateSessionNotes(session: WorkoutSessionEntity, notes: String) = launchAction {
        sessions.updateSessionNotes(session, notes)
    }

    fun updateSessionExerciseNotes(exercise: SessionExerciseEntity, notes: String) = launchAction {
        sessions.updateExerciseNotes(exercise, notes)
    }

    fun updateSessionSet(set: SessionSetEntity) = launchAction { sessions.updateSet(set) }

    fun toggleSessionSetComplete(set: SessionSetEntity) = launchAction {
        sessions.completeSet(set, preferences.restTimerEnabled.first())
    }

    fun addSessionSet(exerciseId: String) = launchAction { sessions.addSet(exerciseId) }

    fun deleteSessionSet(set: SessionSetEntity) = launchAction { sessions.deleteSet(set) }

    fun moveSessionSet(set: SessionSetEntity, delta: Int) = launchAction {
        sessions.moveSet(set.sessionExerciseId, set.id, delta)
    }

    fun adjustRestTimer(session: WorkoutSessionEntity, seconds: Int) = launchAction {
        sessions.adjustTimer(session, seconds)
    }

    fun skipRestTimer(session: WorkoutSessionEntity) = launchAction { sessions.skipTimer(session) }

    fun finishSession(session: WorkoutSessionEntity, onFinished: () -> Unit) = launchAction {
        sessions.finishSession(session)
        val record = progress.recordsForSession(session.id).firstOrNull()
        _messages.emit(record?.let {
            "Nuevo PR · ${it.exerciseName} · ${formatKg(it.weightKg)} kg × ${it.reps}"
        } ?: "Entrenamiento finalizado")
        onFinished()
    }

    fun deleteSession(session: WorkoutSessionEntity, onDeleted: () -> Unit) = launchAction("Sesión eliminada") {
        sessions.deleteSession(session)
        onDeleted()
    }

    fun saveSessionAsRoutine(sessionId: String, name: String, onSaved: (String) -> Unit) =
        launchAction("Rutina creada desde la sesión") {
            onSaved(sessions.saveAsRoutine(sessionId, name))
        }

    fun setRestTimerEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setRestTimerEnabled(enabled)
    }

    fun setWeeklyGoal(goal: Int) = viewModelScope.launch { preferences.setWeeklyGoal(goal) }

    fun saveBodyWeight(id: String?, weightKg: Double, notes: String, recordedAt: Long, onSaved: () -> Unit = {}) = launchAction("Peso guardado") {
        progress.saveBodyWeight(id, weightKg, notes, recordedAt)
        onSaved()
    }

    fun deleteBodyWeight(entry: BodyWeightEntryEntity) = launchAction("Registro eliminado") {
        progress.deleteBodyWeight(entry)
    }

    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setVibrationEnabled(enabled) }

    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setRemindersEnabled(enabled) }

    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        preferences.setReminderTime(hour, minute)
    }

    fun setPlannedWorkout(dayOfWeek: Int, workoutId: String?) = launchAction {
        planning.setPlannedWorkout(dayOfWeek, workoutId)
    }

    fun skipToday() = launchAction("Día marcado como omitido") { planning.skipToday() }

    fun undoSkippedDay(entry: com.arete.app.data.local.SkippedPlannedWorkoutEntity) = launchAction("Día recuperado") {
        planning.undoSkippedDay(entry)
    }

    fun exportBackup(onExported: (String) -> Unit) = launchAction {
        onExported(kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { backup.export() })
    }

    fun restoreBackup(raw: String, onRestored: () -> Unit = {}) = launchAction("Backup restaurado") {
        app.automaticBackups.restore(raw)
        onRestored()
    }

    fun selectBackupFolder(uri: String, enable: Boolean) = launchAction { app.automaticBackups.selectFolder(uri, enable) }
    fun setAutomaticBackupEnabled(enabled: Boolean) = launchAction { app.automaticBackups.setEnabled(enabled) }
    fun createBackupNow() = launchAction { app.automaticBackups.requestNow() }
    fun refreshBackupAccess() = launchAction { app.automaticBackups.reconcile() }

    fun showMessage(message: String) = viewModelScope.launch { _messages.emit(message) }

    fun createRoutine(name: String, onCreated: (String) -> Unit) = launchAction {
        onCreated(repository.createRoutine(name))
    }

    fun renameRoutine(routine: RoutineEntity, name: String) = launchAction {
        repository.renameRoutine(routine, name)
    }

    fun deleteRoutine(routine: RoutineEntity) = launchAction("Rutina eliminada") {
        repository.deleteRoutine(routine)
    }

    fun activateRoutine(id: String) = launchAction("Rutina activa actualizada") {
        repository.activateRoutine(id)
    }

    fun addWorkout(routineId: String, name: String, onCreated: (String) -> Unit = {}) = launchAction {
        onCreated(repository.addWorkout(routineId, name))
    }

    fun renameWorkout(workout: WorkoutEntity, name: String) = launchAction {
        repository.renameWorkout(workout, name)
    }

    fun deleteWorkout(workout: WorkoutEntity) = launchAction { repository.deleteWorkout(workout) }

    fun moveWorkout(routineId: String, workoutId: String, delta: Int) = launchAction {
        repository.moveWorkout(routineId, workoutId, delta)
    }

    fun addExercise(workoutId: String, exerciseId: String, onCreated: (String) -> Unit = {}) = launchAction {
        onCreated(repository.addExercise(workoutId, exerciseId))
    }

    fun deleteExercise(item: RoutineExerciseEntity) = launchAction { repository.deleteExercise(item) }

    fun moveExercise(workoutId: String, itemId: String, delta: Int) = launchAction {
        repository.moveExercise(workoutId, itemId, delta)
    }

    fun savePrescription(
        item: RoutineExerciseEntity,
        restSeconds: Int,
        incrementKg: Double?,
        notes: String,
        groupId: String?,
        sets: List<SetDraft>,
        onSaved: () -> Unit
    ) = launchAction("Configuración guardada") {
        repository.savePrescription(item, restSeconds, incrementKg, notes, groupId, sets)
        onSaved()
    }

    fun saveCustomExercise(
        existing: ExerciseEntity?,
        draft: ExerciseDraft,
        onSaved: (String) -> Unit
    ) = launchAction("Ejercicio guardado") {
        onSaved(repository.saveCustomExercise(existing, draft))
    }

    fun importRoutine(raw: String, onImported: (String) -> Unit) = launchAction("Rutina importada") {
        onImported(repository.importRoutine(raw))
    }

    fun exportRoutine(id: String, onExported: (String) -> Unit) = launchAction {
        onExported(repository.exportRoutine(id))
    }

    fun cycleTheme() = viewModelScope.launch {
        val next = when (themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        preferences.setThemeMode(next)
    }

    private fun launchAction(success: String? = null, block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }
            .onSuccess { success?.let { _messages.emit(it) } }
            .onFailure { error -> _messages.emit(error.message ?: "No se pudo completar la acción") }
    }

    class Factory(private val application: AreteApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AreteViewModel(
                application,
                application.repository,
                application.preferences,
                application.sessionRepository,
                application.progressRepository,
                application.planningRepository,
                application.backupService
            ) as T
    }

    private fun formatKg(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString()
        else "%.2f".format(value).trimEnd('0').trimEnd(',').trimEnd('.').replace('.', ',')
}
