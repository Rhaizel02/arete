package com.arete.app.data

import androidx.room.withTransaction
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.LastPerformanceRow
import com.arete.app.data.local.SessionExerciseEntity
import com.arete.app.data.local.SessionSetEntity
import com.arete.app.data.local.SessionStatus
import com.arete.app.data.local.PrescriptionSetEntity
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.RoutineExerciseEntity
import com.arete.app.data.local.SetType
import com.arete.app.data.local.WorkoutEntity
import com.arete.app.data.local.WorkoutSessionEntity
import com.arete.app.data.local.WorkoutSessionWithDetails
import com.arete.app.data.local.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class NextWorkout(
    val routineId: String,
    val routineName: String,
    val workoutId: String,
    val workoutName: String,
    val exerciseCount: Int
)

data class HomeTrainingState(
    val activeSession: WorkoutSessionWithDetails?,
    val nextWorkout: NextWorkout?,
    val hasActiveRoutine: Boolean,
    val recentSessions: List<WorkoutSessionEntity>
)

data class SessionViewData(
    val session: WorkoutSessionWithDetails?,
    val lastPerformance: Map<String, List<LastPerformanceRow>>
)

class SessionRepository(
    private val database: AreteDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    private val dao = database.sessionDao()

    val homeState: Flow<HomeTrainingState> = combine(
        dao.observeActiveSession(),
        database.routineDao().observeActiveRoutine(),
        dao.observeCompletedSessionHeaders()
    ) { active, routine, history ->
        val ordered = routine?.orderedWorkouts().orEmpty()
        val lastPlanned = routine?.let { current ->
            history.firstOrNull { it.routineId == current.routine.id && it.sourceWorkoutId != null }
        }
        val lastIndex = ordered.indexOfFirst { it.workout.id == lastPlanned?.sourceWorkoutId }
        val next = when {
            routine == null || ordered.isEmpty() -> null
            lastIndex < 0 -> ordered.first()
            else -> ordered[(lastIndex + 1) % ordered.size]
        }
        HomeTrainingState(
            activeSession = active,
            nextWorkout = next?.let {
                NextWorkout(
                    routineId = routine!!.routine.id,
                    routineName = routine.routine.name,
                    workoutId = it.workout.id,
                    workoutName = it.workout.name,
                    exerciseCount = it.exercises.size
                )
            },
            hasActiveRoutine = routine != null,
            recentSessions = history.take(3)
        )
    }

    val completedSessions: Flow<List<WorkoutSessionWithDetails>> = dao.observeCompletedSessions()

    fun observeSession(id: String): Flow<SessionViewData> = combine(
        dao.observeSession(id),
        dao.observeLastPerformance(id)
    ) { session, last ->
        SessionViewData(session, last.groupBy { it.exerciseId })
    }

    suspend fun startPlannedWorkout(workoutId: String): String = database.withTransaction {
        dao.getActiveSession()?.session?.id?.let { return@withTransaction it }
        val workout = database.routineDao().getWorkout(workoutId)
            ?: throw IllegalArgumentException("El workout ya no existe")
        val routine = database.routineDao().getRoutine(workout.workout.routineId)?.routine
            ?: throw IllegalArgumentException("La rutina ya no existe")
        createSessionFromTemplate(workout, routine.name)
    }

    suspend fun startFreeWorkout(): String = database.withTransaction {
        dao.getActiveSession()?.session?.id?.let { return@withTransaction it }
        val id = newId()
        val timestamp = now()
        dao.insertSession(
            WorkoutSessionEntity(
                id = id,
                routineId = null,
                sourceWorkoutId = null,
                routineNameSnapshot = null,
                workoutNameSnapshot = "Entrenamiento libre",
                startedAt = timestamp
            )
        )
        id
    }

    suspend fun addExercise(sessionId: String, exerciseId: String): String = database.withTransaction {
        val session = dao.getSession(sessionId) ?: throw IllegalArgumentException("La sesión no existe")
        val catalogExercise = database.exerciseDao().findById(exerciseId)
            ?: throw IllegalArgumentException("El ejercicio no existe")
        val id = newId()
        val position = session.exercises.size
        dao.insertExercises(listOf(
            SessionExerciseEntity(
                id = id,
                sessionId = sessionId,
                exerciseId = catalogExercise.id,
                position = position,
                exerciseNameEsSnapshot = catalogExercise.nameEs,
                exerciseNameEnSnapshot = catalogExercise.nameEn,
                restSecondsSnapshot = 120,
                incrementKgSnapshot = null,
                notesSnapshot = "",
                groupIdSnapshot = null
            )
        ))
        val previous = dao.getLastPerformance(listOf(catalogExercise.id), session.session.startedAt)
        dao.insertSets((0 until 3).map { index ->
            val last = previous.firstOrNull { it.position == index }
            SessionSetEntity(
                id = newId(),
                sessionExerciseId = id,
                position = index,
                targetRepsMinSnapshot = 8,
                targetRepsMaxSnapshot = 12,
                isWarmupSnapshot = false,
                weightKg = last?.weightKg,
                reps = last?.reps
            )
        })
        id
    }

    suspend fun substituteExercise(sessionExerciseId: String, newExerciseId: String) = database.withTransaction {
        val sessionExercise = findExercise(sessionExerciseId)
        val replacement = database.exerciseDao().findById(newExerciseId)
            ?: throw IllegalArgumentException("El ejercicio sustituto no existe")
        val session = dao.getSession(sessionExercise.sessionId) ?: return@withTransaction
        dao.updateExercise(
            sessionExercise.copy(
                exerciseId = replacement.id,
                exerciseNameEsSnapshot = replacement.nameEs,
                exerciseNameEnSnapshot = replacement.nameEn
            )
        )
        val previous = dao.getLastPerformance(listOf(replacement.id), session.session.startedAt)
        dao.getSets(sessionExercise.id).forEach { set ->
            if (!set.isCompleted) {
                val last = previous.firstOrNull { it.position == set.position }
                dao.updateSet(set.copy(weightKg = last?.weightKg, reps = last?.reps))
            }
        }
    }

    suspend fun deleteExercise(exercise: SessionExerciseEntity) = database.withTransaction {
        dao.deleteExercise(exercise)
        normalizeExercisePositions(exercise.sessionId)
    }

    suspend fun moveExercise(sessionId: String, exerciseId: String, delta: Int) = database.withTransaction {
        val ordered = dao.getExercises(sessionId)
        val from = ordered.indexOfFirst { it.id == exerciseId }
        if (from < 0 || ordered.size < 2) return@withTransaction
        val to = (from + delta).coerceIn(0, ordered.lastIndex)
        if (from == to) return@withTransaction
        rewriteExercisePositions(ordered.toMutableList().apply { add(to, removeAt(from)) })
    }

    suspend fun updateSessionNotes(session: WorkoutSessionEntity, notes: String) {
        dao.updateSessionNotes(session.id, notes)
    }

    suspend fun updateExerciseNotes(exercise: SessionExerciseEntity, notes: String) {
        dao.updateExerciseNotes(exercise.id, notes)
    }

    suspend fun updateSet(set: SessionSetEntity) = dao.updateSetValues(
        id = set.id,
        weightKg = set.weightKg,
        reps = set.reps,
        rir = set.rir,
        isFailure = set.isFailure,
        isWarmup = set.isWarmupSnapshot
    )

    suspend fun completeSet(set: SessionSetEntity, timerEnabled: Boolean) = database.withTransaction {
        val stored = dao.getSet(set.id) ?: return@withTransaction
        val completed = !stored.isCompleted
        val timestamp = now()
        dao.updateSetCompletion(
            id = set.id,
            weightKg = set.weightKg,
            reps = set.reps,
            rir = set.rir,
            isFailure = set.isFailure,
            isWarmup = set.isWarmupSnapshot,
            isCompleted = completed,
            completedAt = if (completed) timestamp else null
        )
        if (completed && !set.isWarmupSnapshot && timerEnabled) {
            val exercise = findExercise(set.sessionExerciseId)
            val session = dao.getSession(exercise.sessionId)?.session ?: return@withTransaction
            if (session.status != SessionStatus.IN_PROGRESS) return@withTransaction
            val seconds = exercise.restSecondsSnapshot
            dao.startTimer(session.id, timestamp + seconds * 1_000L, seconds)
        }
    }

    suspend fun addSet(sessionExerciseId: String): String = database.withTransaction {
        val sets = dao.getSets(sessionExerciseId)
        val previous = sets.lastOrNull()
        val id = newId()
        dao.insertSets(listOf(
            SessionSetEntity(
                id = id,
                sessionExerciseId = sessionExerciseId,
                position = sets.size,
                targetRepsMinSnapshot = previous?.targetRepsMinSnapshot ?: 8,
                targetRepsMaxSnapshot = previous?.targetRepsMaxSnapshot ?: 12,
                isWarmupSnapshot = previous?.isWarmupSnapshot ?: false,
                weightKg = previous?.weightKg,
                reps = previous?.reps
            )
        ))
        id
    }

    suspend fun deleteSet(set: SessionSetEntity) = database.withTransaction {
        dao.deleteSet(set)
        normalizeSetPositions(set.sessionExerciseId)
    }

    suspend fun adjustTimer(session: WorkoutSessionEntity, seconds: Int) {
        dao.adjustTimer(session.id, seconds * 1_000L, now())
    }

    suspend fun skipTimer(session: WorkoutSessionEntity) {
        dao.clearTimer(session.id)
    }

    suspend fun finishSession(session: WorkoutSessionEntity) {
        dao.finishSession(session.id, now())
    }

    suspend fun deleteSession(session: WorkoutSessionEntity) = dao.deleteSession(session)

    suspend fun saveAsRoutine(sessionId: String, routineName: String): String = database.withTransaction {
        val source = dao.getSession(sessionId) ?: throw IllegalArgumentException("La sesión no existe")
        val timestamp = now()
        val routineId = newId()
        val workoutId = newId()
        database.routineDao().insertRoutine(
            RoutineEntity(routineId, routineName.trim(), false, timestamp, timestamp)
        )
        database.routineDao().insertWorkout(
            WorkoutEntity(
                id = workoutId,
                routineId = routineId,
                name = source.session.workoutNameSnapshot,
                position = 0,
                notes = source.session.notes,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )
        source.orderedExercises().forEachIndexed { index, details ->
            val exerciseId = details.exercise.exerciseId ?: return@forEachIndexed
            val routineExerciseId = newId()
            database.routineDao().insertRoutineExercise(
                RoutineExerciseEntity(
                    id = routineExerciseId,
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = index,
                    restSeconds = details.exercise.restSecondsSnapshot,
                    incrementKg = details.exercise.incrementKgSnapshot,
                    notes = details.exercise.sessionNotes.ifBlank { details.exercise.notesSnapshot },
                    groupId = details.exercise.groupIdSnapshot,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )
            database.routineDao().insertSets(details.orderedSets().mapIndexed { setIndex, set ->
                PrescriptionSetEntity(
                    id = newId(),
                    routineExerciseId = routineExerciseId,
                    position = setIndex,
                    type = if (set.isWarmupSnapshot) SetType.WARMUP else SetType.WORK,
                    repsMin = set.targetRepsMinSnapshot,
                    repsMax = set.targetRepsMaxSnapshot
                )
            })
        }
        routineId
    }

    private suspend fun createSessionFromTemplate(workout: WorkoutWithExercises, routineName: String): String {
        val timestamp = now()
        val sessionId = newId()
        val ordered = workout.orderedExercises()
        val exerciseIds = ordered.map { it.exercise.id }
        val previous = if (exerciseIds.isEmpty()) emptyList() else dao.getLastPerformance(exerciseIds, timestamp)
        dao.insertSession(
            WorkoutSessionEntity(
                id = sessionId,
                routineId = workout.workout.routineId,
                sourceWorkoutId = workout.workout.id,
                routineNameSnapshot = routineName,
                workoutNameSnapshot = workout.workout.name,
                startedAt = timestamp,
                notes = workout.workout.notes
            )
        )
        val sessionExercises = ordered.mapIndexed { index, item ->
            SessionExerciseEntity(
                id = newId(),
                sessionId = sessionId,
                exerciseId = item.exercise.id,
                position = index,
                exerciseNameEsSnapshot = item.exercise.nameEs,
                exerciseNameEnSnapshot = item.exercise.nameEn,
                restSecondsSnapshot = item.item.restSeconds,
                incrementKgSnapshot = item.item.incrementKg,
                notesSnapshot = item.item.notes,
                groupIdSnapshot = item.item.groupId,
                sourceRoutineExerciseId = item.item.id
            ) to item
        }
        dao.insertExercises(sessionExercises.map { it.first })
        dao.insertSets(sessionExercises.flatMap { (sessionExercise, template) ->
            template.orderedSets().mapIndexed { index, set ->
                val last = previous.filter { it.exerciseId == template.exercise.id }
                    .firstOrNull { it.position == index }
                SessionSetEntity(
                    id = newId(),
                    sessionExerciseId = sessionExercise.id,
                    position = index,
                    targetRepsMinSnapshot = set.repsMin,
                    targetRepsMaxSnapshot = set.repsMax,
                    isWarmupSnapshot = set.type == SetType.WARMUP,
                    weightKg = last?.weightKg,
                    reps = last?.reps
                )
            }
        })
        return sessionId
    }

    private suspend fun findExercise(id: String): SessionExerciseEntity =
        dao.getExercise(id) ?: throw IllegalArgumentException("El ejercicio de sesión no existe")

    private suspend fun normalizeExercisePositions(sessionId: String) =
        rewriteExercisePositions(dao.getExercises(sessionId))

    private suspend fun rewriteExercisePositions(ordered: List<SessionExerciseEntity>) {
        ordered.forEachIndexed { index, item -> dao.updateExercisePosition(item.id, -index - 1) }
        ordered.forEachIndexed { index, item -> dao.updateExercisePosition(item.id, index) }
    }

    private suspend fun normalizeSetPositions(exerciseId: String) {
        val ordered = dao.getSets(exerciseId)
        ordered.forEachIndexed { index, item -> dao.updateSetPosition(item.id, -index - 1) }
        ordered.forEachIndexed { index, item -> dao.updateSetPosition(item.id, index) }
    }
}
