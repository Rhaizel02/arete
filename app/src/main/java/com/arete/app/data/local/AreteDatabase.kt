package com.arete.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        WorkoutEntity::class,
        RoutineExerciseEntity::class,
        PrescriptionSetEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetEntity::class,
        BodyWeightEntryEntity::class,
        SkippedPlannedWorkoutEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AreteDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restTimerEndAt INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restTimerDurationSeconds INTEGER")
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN sourceRoutineExerciseId TEXT")
                db.execSQL("ALTER TABLE session_exercises ADD COLUMN sessionNotes TEXT NOT NULL DEFAULT ''")
            }
        }

        fun create(context: Context): AreteDatabase =
            Room.databaseBuilder(context, AreteDatabase::class.java, "arete.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
