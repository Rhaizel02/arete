package com.arete.app.data

import androidx.room.withTransaction
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.data.local.PrescriptionSetEntity
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.RoutineExerciseEntity
import com.arete.app.data.local.RoutineWithWorkouts
import com.arete.app.data.local.SetType
import com.arete.app.data.local.WorkoutEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class SetDraft(val type: SetType, val repsMin: Int, val repsMax: Int)

class AreteRepository(
    private val database: AreteDatabase,
    private val seeder: CatalogSeeder,
    private val routineJson: RoutineJsonService = RoutineJsonService(database),
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() }
) {
    val routines: Flow<List<RoutineEntity>> = database.routineDao().observeRoutines()

    suspend fun initialize() = seeder.seedIfEmpty()

    fun searchExercises(query: String): Flow<List<ExerciseEntity>> = database.exerciseDao().search(query.trim())

    fun observeRoutine(id: String): Flow<RoutineWithWorkouts?> = database.routineDao().observeRoutine(id)

    suspend fun createRoutine(name: String): String {
        val timestamp = now()
        val id = newId()
        database.routineDao().insertRoutine(RoutineEntity(id, name.trim(), false, timestamp, timestamp))
        return id
    }

    suspend fun renameRoutine(routine: RoutineEntity, name: String) {
        database.routineDao().updateRoutine(routine.copy(name = name.trim(), updatedAt = now()))
    }

    suspend fun deleteRoutine(routine: RoutineEntity) = database.routineDao().deleteRoutine(routine)

    suspend fun activateRoutine(id: String) = database.routineDao().activate(id, now())

    suspend fun addWorkout(routineId: String, name: String): String = database.withTransaction {
        val timestamp = now()
        val id = newId()
        val position = database.routineDao().getWorkouts(routineId).size
        database.routineDao().insertWorkout(
            WorkoutEntity(id, routineId, name.trim(), position, createdAt = timestamp, updatedAt = timestamp)
        )
        touchRoutine(routineId, timestamp)
        id
    }

    suspend fun renameWorkout(workout: WorkoutEntity, name: String) {
        database.withTransaction {
            val timestamp = now()
            database.routineDao().updateWorkout(workout.copy(name = name.trim(), updatedAt = timestamp))
            touchRoutine(workout.routineId, timestamp)
        }
    }

    suspend fun deleteWorkout(workout: WorkoutEntity) = database.withTransaction {
        database.routineDao().deleteWorkout(workout)
        normalizeWorkoutPositions(workout.routineId)
        touchRoutine(workout.routineId, now())
    }

    suspend fun moveWorkout(routineId: String, workoutId: String, delta: Int) = database.withTransaction {
        val ordered = database.routineDao().getWorkouts(routineId)
        val from = ordered.indexOfFirst { it.id == workoutId }
        if (from < 0 || ordered.size < 2) return@withTransaction
        val to = (from + delta).coerceIn(0, ordered.lastIndex)
        if (from == to) return@withTransaction
        rewriteWorkoutPositions(ordered.toMutableList().apply { add(to, removeAt(from)) })
        touchRoutine(routineId, now())
    }

    suspend fun addExercise(workoutId: String, exerciseId: String): String = database.withTransaction {
        val timestamp = now()
        val id = newId()
        val position = database.routineDao().getRoutineExercises(workoutId).size
        database.routineDao().insertRoutineExercise(
            RoutineExerciseEntity(id, workoutId, exerciseId, position, createdAt = timestamp, updatedAt = timestamp)
        )
        database.routineDao().insertSets((0 until 3).map { index ->
            PrescriptionSetEntity(newId(), id, index, SetType.WORK, 8, 12)
        })
        id
    }

    suspend fun deleteExercise(item: RoutineExerciseEntity) = database.withTransaction {
        database.routineDao().deleteRoutineExercise(item)
        normalizeExercisePositions(item.workoutId)
    }

    suspend fun moveExercise(workoutId: String, itemId: String, delta: Int) = database.withTransaction {
        val ordered = database.routineDao().getRoutineExercises(workoutId)
        val from = ordered.indexOfFirst { it.id == itemId }
        if (from < 0 || ordered.size < 2) return@withTransaction
        val to = (from + delta).coerceIn(0, ordered.lastIndex)
        if (from == to) return@withTransaction
        rewriteExercisePositions(ordered.toMutableList().apply { add(to, removeAt(from)) })
    }

    suspend fun savePrescription(
        item: RoutineExerciseEntity,
        restSeconds: Int,
        incrementKg: Double?,
        notes: String,
        groupId: String?,
        sets: List<SetDraft>
    ) = database.withTransaction {
        database.routineDao().updateRoutineExercise(
            item.copy(
                restSeconds = restSeconds,
                incrementKg = incrementKg,
                notes = notes.trim(),
                groupId = groupId?.trim()?.takeIf(String::isNotEmpty),
                updatedAt = now()
            )
        )
        database.routineDao().deleteSets(item.id)
        database.routineDao().insertSets(sets.mapIndexed { index, set ->
            PrescriptionSetEntity(newId(), item.id, index, set.type, set.repsMin, set.repsMax)
        })
    }

    suspend fun saveCustomExercise(existing: ExerciseEntity?, draft: ExerciseDraft): String {
        val timestamp = now()
        val id = existing?.id ?: newId()
        database.exerciseDao().upsert(
            ExerciseEntity(
                id = id,
                nameEs = draft.nameEs.trim(),
                nameEn = draft.nameEn.trim(),
                primaryMuscle = draft.primaryMuscle.trim(),
                secondaryMuscles = draft.secondaryMuscles.split(',').map(String::trim).filter(String::isNotEmpty),
                equipment = draft.equipment.trim(),
                instructions = draft.instructions.trim(),
                isCustom = true,
                createdAt = existing?.createdAt ?: timestamp,
                updatedAt = timestamp
            )
        )
        return id
    }

    suspend fun exportRoutine(id: String): String = routineJson.exportRoutine(id)

    suspend fun importRoutine(json: String): String = routineJson.importRoutine(json)

    private suspend fun touchRoutine(routineId: String, timestamp: Long) {
        val routine = database.routineDao().getRoutine(routineId)?.routine ?: return
        database.routineDao().updateRoutine(routine.copy(updatedAt = timestamp))
    }

    private suspend fun normalizeWorkoutPositions(routineId: String) =
        rewriteWorkoutPositions(database.routineDao().getWorkouts(routineId))

    private suspend fun rewriteWorkoutPositions(ordered: List<WorkoutEntity>) {
        val timestamp = now()
        ordered.forEachIndexed { index, item -> database.routineDao().updateWorkoutPosition(item.id, -index - 1, timestamp) }
        ordered.forEachIndexed { index, item -> database.routineDao().updateWorkoutPosition(item.id, index, timestamp) }
    }

    private suspend fun normalizeExercisePositions(workoutId: String) =
        rewriteExercisePositions(database.routineDao().getRoutineExercises(workoutId))

    private suspend fun rewriteExercisePositions(ordered: List<RoutineExerciseEntity>) {
        val timestamp = now()
        ordered.forEachIndexed { index, item -> database.routineDao().updateRoutineExercisePosition(item.id, -index - 1, timestamp) }
        ordered.forEachIndexed { index, item -> database.routineDao().updateRoutineExercisePosition(item.id, index, timestamp) }
    }
}

data class ExerciseDraft(
    val nameEs: String,
    val nameEn: String,
    val primaryMuscle: String,
    val secondaryMuscles: String,
    val equipment: String,
    val instructions: String
)
