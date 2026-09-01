package com.arete.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AreteDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao

    companion object {
        fun create(context: Context): AreteDatabase =
            Room.databaseBuilder(context, AreteDatabase::class.java, "arete.db")
                .build()
    }
}
