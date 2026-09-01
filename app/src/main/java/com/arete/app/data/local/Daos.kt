package com.arete.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY nameEs COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT * FROM exercises
        WHERE isArchived = 0
          AND (:query = '' OR nameEs LIKE '%' || :query || '%' COLLATE NOCASE
               OR nameEn LIKE '%' || :query || '%' COLLATE NOCASE)
        ORDER BY isCustom, nameEs COLLATE NOCASE
    """)
    fun search(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isCustom = 1 AND isArchived = 0 ORDER BY nameEs COLLATE NOCASE")
    fun observeCustom(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY nameEs COLLATE NOCASE")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE nameEs = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameEs(name: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE nameEn = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameEn(name: String): ExerciseEntity?

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(exercises: List<ExerciseEntity>)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY isActive DESC, updatedAt DESC")
    fun observeRoutines(): Flow<List<RoutineEntity>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeRoutine(id: String): Flow<RoutineWithWorkouts?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutine(id: String): RoutineWithWorkouts?

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("UPDATE routines SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActive()

    @Query("UPDATE routines SET isActive = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: String, updatedAt: Long)

    @Transaction
    suspend fun activate(id: String, updatedAt: Long) {
        clearActive()
        setActive(id, updatedAt)
    }

    @Query("SELECT * FROM workouts WHERE routineId = :routineId ORDER BY position")
    suspend fun getWorkouts(routineId: String): List<WorkoutEntity>

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET position = :position, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateWorkoutPosition(id: String, position: Int, updatedAt: Long)

    @Query("SELECT * FROM routine_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getRoutineExercises(workoutId: String): List<RoutineExerciseEntity>

    @Insert
    suspend fun insertRoutineExercise(item: RoutineExerciseEntity)

    @Update
    suspend fun updateRoutineExercise(item: RoutineExerciseEntity)

    @Delete
    suspend fun deleteRoutineExercise(item: RoutineExerciseEntity)

    @Query("UPDATE routine_exercises SET position = :position, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRoutineExercisePosition(id: String, position: Int, updatedAt: Long)

    @Query("DELETE FROM prescription_sets WHERE routineExerciseId = :routineExerciseId")
    suspend fun deleteSets(routineExerciseId: String)

    @Insert
    suspend fun insertSets(sets: List<PrescriptionSetEntity>)
}

@Dao
interface SessionDao {
    @Insert suspend fun insertSession(session: WorkoutSessionEntity)
    @Insert suspend fun insertExercises(exercises: List<SessionExerciseEntity>)
    @Insert suspend fun insertSets(sets: List<SessionSetEntity>)
    @Insert suspend fun insertBodyWeight(entry: BodyWeightEntryEntity)
    @Insert suspend fun insertSkippedWorkout(entry: SkippedPlannedWorkoutEntity)
}

