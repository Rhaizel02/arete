@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.arete.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arete.app.AreteApplication
import com.arete.app.data.AreteRepository
import com.arete.app.data.ExerciseDraft
import com.arete.app.data.PreferencesRepository
import com.arete.app.data.SetDraft
import com.arete.app.data.SessionRepository
import com.arete.app.data.SessionViewData
import com.arete.app.data.ThemeMode
import com.arete.app.data.local.ExerciseEntity
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AreteViewModel(
    application: Application,
    private val repository: AreteRepository,
    private val preferences: PreferencesRepository,
    private val sessions: SessionRepository
) : AndroidViewModel(application) {
    val routines = repository.routines.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val themeMode = preferences.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    val restTimerEnabled = preferences.restTimerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val homeState = sessions.homeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val completedSessions = sessions.completedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseQuery = MutableStateFlow("")
    val exercises = exerciseQuery.flatMapLatest(repository::searchExercises)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allExercises = repository.searchExercises("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.initialize() }
                .onFailure { _messages.emit("No se pudo cargar el catálogo local") }
        }
    }

    fun observeRoutine(id: String): Flow<RoutineWithWorkouts?> = repository.observeRoutine(id)

    fun observeSession(id: String): Flow<SessionViewData> = sessions.observeSession(id)

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
        sessions.completeSet(set, restTimerEnabled.value)
    }

    fun addSessionSet(exerciseId: String) = launchAction { sessions.addSet(exerciseId) }

    fun deleteSessionSet(set: SessionSetEntity) = launchAction { sessions.deleteSet(set) }

    fun adjustRestTimer(session: WorkoutSessionEntity, seconds: Int) = launchAction {
        sessions.adjustTimer(session, seconds)
    }

    fun skipRestTimer(session: WorkoutSessionEntity) = launchAction { sessions.skipTimer(session) }

    fun finishSession(session: WorkoutSessionEntity, onFinished: () -> Unit) = launchAction("Entrenamiento finalizado") {
        sessions.finishSession(session)
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
                application.sessionRepository
            ) as T
    }
}
