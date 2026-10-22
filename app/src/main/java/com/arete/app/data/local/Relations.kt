package com.arete.app.data.local

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

data class RoutineWithWorkouts(
    @Embedded val routine: RoutineEntity,
    @Relation(
        entity = WorkoutEntity::class,
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val workouts: List<WorkoutWithExercises>
) {
    fun orderedWorkouts(): List<WorkoutWithExercises> = workouts.sortedBy { it.workout.position }
}

data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = RoutineExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val exercises: List<RoutineExerciseWithDetails>
) {
    fun orderedExercises(): List<RoutineExerciseWithDetails> = exercises.sortedBy { it.item.position }
}

data class RoutineExerciseWithDetails(
    @Embedded val item: RoutineExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(
        entity = PrescriptionSetEntity::class,
        parentColumn = "id",
        entityColumn = "routineExerciseId"
    )
    val sets: List<PrescriptionSetEntity>
) {
    fun orderedSets(): List<PrescriptionSetEntity> = sets.sortedBy { it.position }
}

data class WorkoutSessionWithDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = SessionExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercises: List<SessionExerciseWithSets>
) {
    fun orderedExercises(): List<SessionExerciseWithSets> = exercises.sortedBy { it.exercise.position }
}

data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(
        entity = SessionSetEntity::class,
        parentColumn = "id",
        entityColumn = "sessionExerciseId"
    )
    val sets: List<SessionSetEntity>
) {
    fun orderedSets(): List<SessionSetEntity> = sets.sortedBy { it.position }
}

data class LastPerformanceRow(
    val exerciseId: String,
    val sessionId: String,
    val completedAt: Long,
    val position: Int,
    val weightKg: Double?,
    val reps: Int?,
    val isWarmupSnapshot: Boolean,
    val rir: Int?,
    val isFailure: Boolean
)

/** A single joined read model used to derive every progress metric without N+1 queries. */
data class PerformanceSetRow(
    val sessionId: String,
    val sessionStartedAt: Long,
    val sessionCompletedAt: Long,
    val sessionExerciseId: String,
    val exerciseId: String?,
    val exerciseName: String,
    val primaryMuscle: String?,
    val incrementKg: Double?,
    val setId: String,
    val setPosition: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val weightKg: Double?,
    val reps: Int?,
    val rir: Int?,
    val isFailure: Boolean
)

@Serializable
data class BackupDatabaseData(
    val exercises: List<ExerciseEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val routineExercises: List<RoutineExerciseEntity> = emptyList(),
    val prescriptionSets: List<PrescriptionSetEntity> = emptyList(),
    val workoutSessions: List<WorkoutSessionEntity> = emptyList(),
    val sessionExercises: List<SessionExerciseEntity> = emptyList(),
    val sessionSets: List<SessionSetEntity> = emptyList(),
    val bodyWeights: List<BodyWeightEntryEntity> = emptyList(),
    val skippedDays: List<SkippedPlannedWorkoutEntity> = emptyList()
)
