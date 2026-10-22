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

    @Transaction
    @Query("SELECT * FROM routines WHERE isActive = 1 LIMIT 1")
    fun observeActiveRoutine(): Flow<RoutineWithWorkouts?>

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

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getWorkout(id: String): WorkoutWithExercises?

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
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSessionWithDetails?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    fun observeSession(id: String): Flow<WorkoutSessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): WorkoutSessionWithDetails?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompletedSessions(): Flow<List<WorkoutSessionWithDetails>>

    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompletedSessionHeaders(): Flow<List<WorkoutSessionEntity>>

    @Insert suspend fun insertSession(session: WorkoutSessionEntity)
    @Insert suspend fun insertExercises(exercises: List<SessionExerciseEntity>)
    @Insert suspend fun insertSets(sets: List<SessionSetEntity>)
    @Update suspend fun updateSession(session: WorkoutSessionEntity)
    @Update suspend fun updateExercise(exercise: SessionExerciseEntity)
    @Update suspend fun updateSet(set: SessionSetEntity)
    @Delete suspend fun deleteSession(session: WorkoutSessionEntity)
    @Delete suspend fun deleteExercise(exercise: SessionExerciseEntity)
    @Delete suspend fun deleteSet(set: SessionSetEntity)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getExercises(sessionId: String): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_exercises WHERE id = :id LIMIT 1")
    suspend fun getExercise(id: String): SessionExerciseEntity?

    @Query("SELECT * FROM session_sets WHERE sessionExerciseId = :exerciseId ORDER BY position")
    suspend fun getSets(exerciseId: String): List<SessionSetEntity>

    @Query("SELECT * FROM session_sets WHERE id = :id LIMIT 1")
    suspend fun getSet(id: String): SessionSetEntity?

    @Query("UPDATE workout_sessions SET notes = :notes WHERE id = :id")
    suspend fun updateSessionNotes(id: String, notes: String)

    @Query("UPDATE session_exercises SET sessionNotes = :notes WHERE id = :id")
    suspend fun updateExerciseNotes(id: String, notes: String)

    @Query("""
        UPDATE session_sets
        SET weightKg = :weightKg, reps = :reps, rir = :rir,
            isFailure = :isFailure, isWarmupSnapshot = :isWarmup
        WHERE id = :id
    """)
    suspend fun updateSetValues(
        id: String,
        weightKg: Double?,
        reps: Int?,
        rir: Int?,
        isFailure: Boolean,
        isWarmup: Boolean
    )

    @Query("""
        UPDATE session_sets
        SET weightKg = :weightKg, reps = :reps, rir = :rir,
            isFailure = :isFailure, isWarmupSnapshot = :isWarmup,
            isCompleted = :isCompleted, completedAt = :completedAt
        WHERE id = :id
    """)
    suspend fun updateSetCompletion(
        id: String,
        weightKg: Double?,
        reps: Int?,
        rir: Int?,
        isFailure: Boolean,
        isWarmup: Boolean,
        isCompleted: Boolean,
        completedAt: Long?
    )

    @Query("UPDATE workout_sessions SET restTimerEndAt = :endAt, restTimerDurationSeconds = :durationSeconds WHERE id = :id")
    suspend fun startTimer(id: String, endAt: Long, durationSeconds: Int)

    @Query("UPDATE workout_sessions SET restTimerEndAt = MAX(restTimerEndAt + :deltaMillis, :now) WHERE id = :id AND restTimerEndAt IS NOT NULL")
    suspend fun adjustTimer(id: String, deltaMillis: Long, now: Long)

    @Query("UPDATE workout_sessions SET restTimerEndAt = NULL, restTimerDurationSeconds = NULL WHERE id = :id")
    suspend fun clearTimer(id: String)

    @Query("UPDATE workout_sessions SET status = 'COMPLETED', completedAt = :completedAt, restTimerEndAt = NULL, restTimerDurationSeconds = NULL WHERE id = :id")
    suspend fun finishSession(id: String, completedAt: Long)

    @Query("UPDATE session_exercises SET position = :position WHERE id = :id")
    suspend fun updateExercisePosition(id: String, position: Int)

    @Query("UPDATE session_sets SET position = :position WHERE id = :id")
    suspend fun updateSetPosition(id: String, position: Int)

    @Query("""
        SELECT se.exerciseId AS exerciseId,
               ws.id AS sessionId,
               ws.completedAt AS completedAt,
               ss.position AS position,
               ss.weightKg AS weightKg,
               ss.reps AS reps,
               ss.isWarmupSnapshot AS isWarmupSnapshot,
               ss.rir AS rir,
               ss.isFailure AS isFailure
        FROM session_sets ss
        JOIN session_exercises se ON se.id = ss.sessionExerciseId
        JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE se.exerciseId IN (:exerciseIds)
          AND ws.status = 'COMPLETED'
          AND ws.completedAt IS NOT NULL
          AND ws.completedAt < :before
          AND ss.isCompleted = 1
          AND ws.completedAt = (
              SELECT MAX(ws2.completedAt)
              FROM session_exercises se2
              JOIN workout_sessions ws2 ON ws2.id = se2.sessionId
              WHERE se2.exerciseId = se.exerciseId
                AND ws2.status = 'COMPLETED'
                AND ws2.completedAt IS NOT NULL
                AND ws2.completedAt < :before
          )
        ORDER BY se.exerciseId, ss.position
    """)
    suspend fun getLastPerformance(exerciseIds: List<String>, before: Long): List<LastPerformanceRow>

    @Query("""
        SELECT se.exerciseId AS exerciseId,
               ws.id AS sessionId,
               ws.completedAt AS completedAt,
               ss.position AS position,
               ss.weightKg AS weightKg,
               ss.reps AS reps,
               ss.isWarmupSnapshot AS isWarmupSnapshot,
               ss.rir AS rir,
               ss.isFailure AS isFailure
        FROM session_sets ss
        JOIN session_exercises se ON se.id = ss.sessionExerciseId
        JOIN workout_sessions ws ON ws.id = se.sessionId
        WHERE se.exerciseId IN (
              SELECT cur.exerciseId FROM session_exercises cur
              WHERE cur.sessionId = :sessionId AND cur.exerciseId IS NOT NULL
          )
          AND ws.status = 'COMPLETED'
          AND ws.completedAt IS NOT NULL
          AND ws.completedAt < (SELECT startedAt FROM workout_sessions WHERE id = :sessionId)
          AND ss.isCompleted = 1
          AND ws.completedAt = (
              SELECT MAX(ws2.completedAt)
              FROM session_exercises se2
              JOIN workout_sessions ws2 ON ws2.id = se2.sessionId
              WHERE se2.exerciseId = se.exerciseId
                AND ws2.status = 'COMPLETED'
                AND ws2.completedAt IS NOT NULL
                AND ws2.completedAt < (SELECT startedAt FROM workout_sessions WHERE id = :sessionId)
          )
        ORDER BY se.exerciseId, ss.position
    """)
    fun observeLastPerformance(sessionId: String): Flow<List<LastPerformanceRow>>

    @Insert suspend fun insertBodyWeight(entry: BodyWeightEntryEntity)
    @Insert suspend fun insertSkippedWorkout(entry: SkippedPlannedWorkoutEntity)
}

@Dao
interface ProgressDao {
    @Query("""
        SELECT ws.id AS sessionId,
               ws.startedAt AS sessionStartedAt,
               ws.completedAt AS sessionCompletedAt,
               se.id AS sessionExerciseId,
               se.exerciseId AS exerciseId,
               se.exerciseNameEsSnapshot AS exerciseName,
               e.primaryMuscle AS primaryMuscle,
               se.incrementKgSnapshot AS incrementKg,
               ss.id AS setId,
               ss.position AS setPosition,
               ss.targetRepsMinSnapshot AS targetRepsMin,
               ss.targetRepsMaxSnapshot AS targetRepsMax,
               ss.isWarmupSnapshot AS isWarmup,
               ss.isCompleted AS isCompleted,
               ss.weightKg AS weightKg,
               ss.reps AS reps,
               ss.rir AS rir,
               ss.isFailure AS isFailure
        FROM workout_sessions ws
        JOIN session_exercises se ON se.sessionId = ws.id
        JOIN session_sets ss ON ss.sessionExerciseId = se.id
        LEFT JOIN exercises e ON e.id = se.exerciseId
        WHERE ws.status = 'COMPLETED' AND ws.completedAt IS NOT NULL
        ORDER BY ws.completedAt, se.position, ss.position
    """)
    fun observePerformanceSets(): Flow<List<PerformanceSetRow>>

    @Query("""
        SELECT ws.id AS sessionId,
               ws.startedAt AS sessionStartedAt,
               ws.completedAt AS sessionCompletedAt,
               se.id AS sessionExerciseId,
               se.exerciseId AS exerciseId,
               se.exerciseNameEsSnapshot AS exerciseName,
               e.primaryMuscle AS primaryMuscle,
               se.incrementKgSnapshot AS incrementKg,
               ss.id AS setId,
               ss.position AS setPosition,
               ss.targetRepsMinSnapshot AS targetRepsMin,
               ss.targetRepsMaxSnapshot AS targetRepsMax,
               ss.isWarmupSnapshot AS isWarmup,
               ss.isCompleted AS isCompleted,
               ss.weightKg AS weightKg,
               ss.reps AS reps,
               ss.rir AS rir,
               ss.isFailure AS isFailure
        FROM workout_sessions ws
        JOIN session_exercises se ON se.sessionId = ws.id
        JOIN session_sets ss ON ss.sessionExerciseId = se.id
        LEFT JOIN exercises e ON e.id = se.exerciseId
        WHERE ws.status = 'COMPLETED' AND ws.completedAt IS NOT NULL AND ws.id = :sessionId
        ORDER BY se.position, ss.position
    """)
    suspend fun getPerformanceSets(sessionId: String): List<PerformanceSetRow>

    @Query("SELECT * FROM body_weight_entries ORDER BY recordedAt")
    fun observeBodyWeights(): Flow<List<BodyWeightEntryEntity>>

    @Query("SELECT * FROM body_weight_entries WHERE id = :id LIMIT 1")
    suspend fun getBodyWeight(id: String): BodyWeightEntryEntity?

    @Upsert
    suspend fun upsertBodyWeight(entry: BodyWeightEntryEntity)

    @Delete
    suspend fun deleteBodyWeight(entry: BodyWeightEntryEntity)

    @Query("SELECT * FROM body_weight_entries WHERE recordedAt = :recordedAt LIMIT 1")
    suspend fun getBodyWeightAt(recordedAt: Long): BodyWeightEntryEntity?
}

@Dao
interface PlanningDao {
    @Query("SELECT * FROM skipped_planned_workouts ORDER BY plannedEpochDay DESC")
    fun observeSkippedDays(): Flow<List<SkippedPlannedWorkoutEntity>>

    @Query("SELECT * FROM skipped_planned_workouts WHERE plannedEpochDay = :epochDay ORDER BY skippedAt DESC")
    suspend fun getSkippedDays(epochDay: Long): List<SkippedPlannedWorkoutEntity>

    @Insert
    suspend fun insertSkippedDay(entry: SkippedPlannedWorkoutEntity)

    @Delete
    suspend fun deleteSkippedDay(entry: SkippedPlannedWorkoutEntity)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM exercises ORDER BY id") suspend fun exercises(): List<ExerciseEntity>
    @Query("SELECT * FROM routines ORDER BY id") suspend fun routines(): List<RoutineEntity>
    @Query("SELECT * FROM workouts ORDER BY id") suspend fun workouts(): List<WorkoutEntity>
    @Query("SELECT * FROM routine_exercises ORDER BY id") suspend fun routineExercises(): List<RoutineExerciseEntity>
    @Query("SELECT * FROM prescription_sets ORDER BY id") suspend fun prescriptionSets(): List<PrescriptionSetEntity>
    @Query("SELECT * FROM workout_sessions ORDER BY id") suspend fun workoutSessions(): List<WorkoutSessionEntity>
    @Query("SELECT * FROM session_exercises ORDER BY id") suspend fun sessionExercises(): List<SessionExerciseEntity>
    @Query("SELECT * FROM session_sets ORDER BY id") suspend fun sessionSets(): List<SessionSetEntity>
    @Query("SELECT * FROM body_weight_entries ORDER BY id") suspend fun bodyWeights(): List<BodyWeightEntryEntity>
    @Query("SELECT * FROM skipped_planned_workouts ORDER BY id") suspend fun skippedDays(): List<SkippedPlannedWorkoutEntity>

    @Insert suspend fun insertExercises(items: List<ExerciseEntity>)
    @Insert suspend fun insertRoutines(items: List<RoutineEntity>)
    @Insert suspend fun insertWorkouts(items: List<WorkoutEntity>)
    @Insert suspend fun insertRoutineExercises(items: List<RoutineExerciseEntity>)
    @Insert suspend fun insertPrescriptionSets(items: List<PrescriptionSetEntity>)
    @Insert suspend fun insertWorkoutSessions(items: List<WorkoutSessionEntity>)
    @Insert suspend fun insertSessionExercises(items: List<SessionExerciseEntity>)
    @Insert suspend fun insertSessionSets(items: List<SessionSetEntity>)
    @Insert suspend fun insertBodyWeights(items: List<BodyWeightEntryEntity>)
    @Insert suspend fun insertSkippedDays(items: List<SkippedPlannedWorkoutEntity>)

    @Query("DELETE FROM session_sets") suspend fun clearSessionSets()
    @Query("DELETE FROM session_exercises") suspend fun clearSessionExercises()
    @Query("DELETE FROM workout_sessions") suspend fun clearWorkoutSessions()
    @Query("DELETE FROM prescription_sets") suspend fun clearPrescriptionSets()
    @Query("DELETE FROM routine_exercises") suspend fun clearRoutineExercises()
    @Query("DELETE FROM skipped_planned_workouts") suspend fun clearSkippedDays()
    @Query("DELETE FROM workouts") suspend fun clearWorkouts()
    @Query("DELETE FROM routines") suspend fun clearRoutines()
    @Query("DELETE FROM body_weight_entries") suspend fun clearBodyWeights()
    @Query("DELETE FROM exercises") suspend fun clearExercises()

    @Transaction
    suspend fun replaceAll(data: BackupDatabaseData) {
        clearSessionSets()
        clearSessionExercises()
        clearWorkoutSessions()
        clearPrescriptionSets()
        clearRoutineExercises()
        clearSkippedDays()
        clearWorkouts()
        clearRoutines()
        clearBodyWeights()
        clearExercises()
        if (data.exercises.isNotEmpty()) insertExercises(data.exercises)
        if (data.routines.isNotEmpty()) insertRoutines(data.routines)
        if (data.workouts.isNotEmpty()) insertWorkouts(data.workouts)
        if (data.routineExercises.isNotEmpty()) insertRoutineExercises(data.routineExercises)
        if (data.prescriptionSets.isNotEmpty()) insertPrescriptionSets(data.prescriptionSets)
        if (data.workoutSessions.isNotEmpty()) insertWorkoutSessions(data.workoutSessions)
        if (data.sessionExercises.isNotEmpty()) insertSessionExercises(data.sessionExercises)
        if (data.sessionSets.isNotEmpty()) insertSessionSets(data.sessionSets)
        if (data.bodyWeights.isNotEmpty()) insertBodyWeights(data.bodyWeights)
        if (data.skippedDays.isNotEmpty()) insertSkippedDays(data.skippedDays)
    }
}
