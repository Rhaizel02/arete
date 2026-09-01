package com.arete.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.data.local.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AretePersistenceTest {
    private lateinit var database: AreteDatabase
    private lateinit var repository: AreteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AreteRepository(database, CatalogSeeder(context, database))
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun catalogSeedsLocallyAndSearchesBothLanguages() = runTest {
        repository.initialize()

        assertTrue(database.exerciseDao().count() >= 40)
        assertEquals("Press de banca con barra", repository.searchExercises("Barbell Bench").first().single().nameEs)
        assertEquals("Barbell Bench Press", repository.searchExercises("press de banca").first().single().nameEn)
    }

    @Test
    fun routineCrudActivationOrderingAndCascadeAreConsistent() = runTest {
        repository.initialize()
        val routineA = repository.createRoutine("Torso/Pierna")
        val routineB = repository.createRoutine("Full body")
        repository.activateRoutine(routineA)
        repository.activateRoutine(routineB)

        val routines = repository.routines.first()
        assertEquals(1, routines.count { it.isActive })
        assertEquals(routineB, routines.single { it.isActive }.id)

        val torso = repository.addWorkout(routineA, "Torso")
        val pierna = repository.addWorkout(routineA, "Pierna")
        repository.moveWorkout(routineA, pierna, -1)
        assertEquals(listOf("Pierna", "Torso"), database.routineDao().getRoutine(routineA)!!.orderedWorkouts().map { it.workout.name })

        val benchId = database.exerciseDao().findByNameEn("Barbell Bench Press")!!.id
        val rowId = database.exerciseDao().findByNameEn("Barbell Row")!!.id
        val benchItem = repository.addExercise(torso, benchId)
        val rowItem = repository.addExercise(torso, rowId)
        repository.moveExercise(torso, rowItem, -1)
        val ordered = database.routineDao().getRoutine(routineA)!!.orderedWorkouts()
            .single { it.workout.id == torso }.orderedExercises()
        assertEquals(listOf(rowItem, benchItem), ordered.map { it.item.id })
        assertEquals(listOf(0, 1), ordered.map { it.item.position })

        val bench = ordered.single { it.item.id == benchItem }
        repository.savePrescription(
            item = bench.item,
            restSeconds = 180,
            incrementKg = 2.5,
            notes = "Pausa en el pecho",
            groupId = "A1",
            sets = listOf(SetDraft(SetType.WARMUP, 5, 5), SetDraft(SetType.WORK, 8, 12))
        )
        val saved = database.routineDao().getRoutine(routineA)!!.orderedWorkouts()
            .single { it.workout.id == torso }.orderedExercises().single { it.item.id == benchItem }
        assertEquals(180, saved.item.restSeconds)
        assertEquals(2.5, saved.item.incrementKg!!, 0.0)
        assertEquals("A1", saved.item.groupId)
        assertEquals(listOf(SetType.WARMUP, SetType.WORK), saved.orderedSets().map { it.type })
        assertEquals(listOf(5 to 5, 8 to 12), saved.orderedSets().map { it.repsMin to it.repsMax })

        val routineEntity = database.routineDao().getRoutine(routineA)!!.routine
        repository.deleteRoutine(routineEntity)
        assertNull(database.routineDao().getRoutine(routineA))
        assertEquals(0, countRows("workouts", "routineId", routineA))
        assertEquals(0, countRows("routine_exercises", "workoutId", torso))
    }

    @Test
    fun customExerciseUsesTheSharedExerciseModelAndCanBeEdited() = runTest {
        val id = repository.saveCustomExercise(
            null,
            ExerciseDraft("Remo casero", "Home row", "Espalda", "Bíceps, Core", "Bandas", "Controlar el regreso")
        )
        val original = database.exerciseDao().findById(id)!!
        assertTrue(original.isCustom)
        assertEquals(listOf("Bíceps", "Core"), original.secondaryMuscles)

        repository.saveCustomExercise(original, ExerciseDraft("Remo con bandas", "Band row", "Espalda", "Bíceps", "Bandas", ""))
        val edited = database.exerciseDao().findById(id)!!
        assertEquals("Remo con bandas", edited.nameEs)
        assertEquals(original.createdAt, edited.createdAt)
        assertFalse(edited.isArchived)
    }

    private fun countRows(table: String, column: String, value: String): Int =
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table WHERE $column = ?", arrayOf(value)).use {
            it.moveToFirst()
            it.getInt(0)
        }
}

