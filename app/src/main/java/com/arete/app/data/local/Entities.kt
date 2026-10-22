package com.arete.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

@Entity(
    tableName = "exercises",
    indices = [Index("nameEs"), Index("nameEn"), Index(value = ["source", "sourceId"], unique = true)]
)
@Serializable
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val nameEs: String,
    val nameEn: String,
    val primaryMuscle: String,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: String,
    val instructions: String = "",
    val imageUrl: String? = null,
    val externalUrl: String? = null,
    val source: String? = null,
    val sourceId: String? = null,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "routines", indices = [Index("isActive")])
@Serializable
data class RoutineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "workouts",
    foreignKeys = [ForeignKey(
        entity = RoutineEntity::class,
        parentColumns = ["id"],
        childColumns = ["routineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routineId"), Index(value = ["routineId", "position"], unique = true)]
)
@Serializable
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val routineId: String,
    val name: String,
    val position: Int,
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("workoutId"), Index("exerciseId"), Index(value = ["workoutId", "position"], unique = true)]
)
@Serializable
data class RoutineExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val position: Int,
    val restSeconds: Int = 120,
    val incrementKg: Double? = null,
    val notes: String = "",
    val groupId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "prescription_sets",
    foreignKeys = [ForeignKey(
        entity = RoutineExerciseEntity::class,
        parentColumns = ["id"],
        childColumns = ["routineExerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routineExerciseId"), Index(value = ["routineExerciseId", "position"], unique = true)]
)
@Serializable
data class PrescriptionSetEntity(
    @PrimaryKey val id: String,
    val routineExerciseId: String,
    val position: Int,
    val type: SetType,
    val repsMin: Int,
    val repsMax: Int
)

/**
 * Names and prescription targets are snapshots so later routine edits cannot rewrite history.
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(RoutineEntity::class, ["id"], ["routineId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(WorkoutEntity::class, ["id"], ["sourceWorkoutId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("routineId"), Index("sourceWorkoutId"), Index("startedAt")]
)
@Serializable
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val routineId: String?,
    val sourceWorkoutId: String?,
    val routineNameSnapshot: String?,
    val workoutNameSnapshot: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
    val notes: String = "",
    val restTimerEndAt: Long? = null,
    val restTimerDurationSeconds: Int? = null
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(WorkoutSessionEntity::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("sessionId"), Index("exerciseId"), Index(value = ["sessionId", "position"], unique = true)]
)
@Serializable
data class SessionExerciseEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String?,
    val position: Int,
    val exerciseNameEsSnapshot: String,
    val exerciseNameEnSnapshot: String,
    val restSecondsSnapshot: Int,
    val incrementKgSnapshot: Double?,
    val notesSnapshot: String,
    val groupIdSnapshot: String?,
    val sourceRoutineExerciseId: String? = null,
    @ColumnInfo(defaultValue = "''") val sessionNotes: String = ""
)

@Entity(
    tableName = "session_sets",
    foreignKeys = [ForeignKey(
        SessionExerciseEntity::class,
        ["id"],
        ["sessionExerciseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionExerciseId"), Index(value = ["sessionExerciseId", "position"], unique = true)]
)
@Serializable
data class SessionSetEntity(
    @PrimaryKey val id: String,
    val sessionExerciseId: String,
    val position: Int,
    val targetRepsMinSnapshot: Int,
    val targetRepsMaxSnapshot: Int,
    val isWarmupSnapshot: Boolean,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rir: Int? = null,
    val isFailure: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Entity(tableName = "body_weight_entries", indices = [Index(value = ["recordedAt"], unique = true)])
@Serializable
data class BodyWeightEntryEntity(
    @PrimaryKey val id: String,
    val weightKg: Double,
    val recordedAt: Long,
    val notes: String = ""
)

@Entity(
    tableName = "skipped_planned_workouts",
    foreignKeys = [
        ForeignKey(RoutineEntity::class, ["id"], ["routineId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(WorkoutEntity::class, ["id"], ["workoutId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("routineId"), Index("workoutId"), Index(value = ["plannedEpochDay", "workoutId"], unique = true)]
)
@Serializable
data class SkippedPlannedWorkoutEntity(
    @PrimaryKey val id: String,
    val routineId: String?,
    val workoutId: String?,
    val routineNameSnapshot: String?,
    val workoutNameSnapshot: String,
    val plannedEpochDay: Long,
    val skippedAt: Long,
    val reason: String = ""
)
