package com.arete.app.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arete.app.data.local.AreteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AreteDatabase::class.java
    )

    @Test
    fun migration1To2PreservesSessionAndAddsRecoveryFields() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                """INSERT INTO workout_sessions
                    (id, routineId, sourceWorkoutId, routineNameSnapshot, workoutNameSnapshot, startedAt, completedAt, status, notes)
                    VALUES ('session-1', NULL, NULL, NULL, 'Entrenamiento libre', 100, NULL, 'IN_PROGRESS', 'nota')
                """.trimIndent()
            )
            execSQL(
                """INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, exerciseNameEsSnapshot, exerciseNameEnSnapshot,
                     restSecondsSnapshot, incrementKgSnapshot, notesSnapshot, groupIdSnapshot)
                    VALUES ('exercise-1', 'session-1', NULL, 0, 'Ejercicio', '', 120, NULL, 'plantilla', 'A1')
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true, AreteDatabase.MIGRATION_1_2)
        db.query("SELECT notes, restTimerEndAt, restTimerDurationSeconds FROM workout_sessions WHERE id='session-1'").use {
            it.moveToFirst()
            assertEquals("nota", it.getString(0))
            assertNull(if (it.isNull(1)) null else it.getLong(1))
            assertNull(if (it.isNull(2)) null else it.getInt(2))
        }
        db.query("SELECT notesSnapshot, groupIdSnapshot, sourceRoutineExerciseId, sessionNotes FROM session_exercises WHERE id='exercise-1'").use {
            it.moveToFirst()
            assertEquals("plantilla", it.getString(0))
            assertEquals("A1", it.getString(1))
            assertNull(if (it.isNull(2)) null else it.getString(2))
            assertEquals("", it.getString(3))
        }
        db.close()
    }

    private companion object {
        const val DB_NAME = "arete-migration-1-2"
    }
}
