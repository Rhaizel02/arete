package com.arete.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.SetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoutineJsonServiceTest {
    private lateinit var database: AreteDatabase
    private lateinit var repository: AreteRepository
    private val json = Json { ignoreUnknownKeys = true }

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
    fun exportUsesSchemaVersionOneAndRoundTripPreservesPrescription() = runTest {
        repository.initialize()
        val originalId = repository.createRoutine("Torso A/B")
        val workoutId = repository.addWorkout(originalId, "Torso A")
        val exerciseId = database.exerciseDao().findByNameEn("Barbell Bench Press")!!.id
        val itemId = repository.addExercise(workoutId, exerciseId)
        val details = database.routineDao().getRoutine(originalId)!!.orderedWorkouts().single().orderedExercises().single()
        repository.savePrescription(
            details.item,
            180,
            2.5,
            "Técnica estricta",
            "A1",
            listOf(SetDraft(SetType.WARMUP, 5, 5), SetDraft(SetType.WORK, 8, 12))
        )

        val exported = repository.exportRoutine(originalId)
        assertTrue(exported.contains("\"schemaVersion\": 1"))
        val contract = json.decodeFromString<RoutineJson>(exported)
        assertEquals(ROUTINE_JSON_SCHEMA_VERSION, contract.schemaVersion)
        assertEquals(RepsJson(exact = 5), contract.workouts.single().exercises.single().sets.first().reps)
        assertEquals(RepsJson(min = 8, max = 12), contract.workouts.single().exercises.single().sets.last().reps)

        val importedId = repository.importRoutine(exported)
        assertNotEquals(originalId, importedId)
        val imported = database.routineDao().getRoutine(importedId)!!
        val importedExercise = imported.orderedWorkouts().single().orderedExercises().single()
        assertEquals("Torso A/B", imported.routine.name)
        assertEquals(exerciseId, importedExercise.exercise.id)
        assertEquals(180, importedExercise.item.restSeconds)
        assertEquals("A1", importedExercise.item.groupId)
        assertEquals(listOf(5 to 5, 8 to 12), importedExercise.orderedSets().map { it.repsMin to it.repsMax })
    }

    @Test
    fun importResolvesByIdThenSpanishThenEnglishAndCreatesUnknownCustomExercise() = runTest {
        repository.initialize()
        val bench = database.exerciseDao().findByNameEn("Barbell Bench Press")!!
        val raw = """
            {
              "schemaVersion": 1,
              "name": "Resolución",
              "workouts": [{
                "name": "Día 1",
                "exercises": [
                  {"exercise":{"id":"${bench.id}","name":"Nombre que no coincide"},"sets":[{"reps":{"exact":5}}]},
                  {"exercise":{"name":"Remo con barra"},"sets":[{"reps":{"min":8,"max":10}}]},
                  {"exercise":{"nameEn":"Lat Pulldown"},"sets":[{"reps":{"exact":10}}]},
                  {"exercise":{"id":"exercise-from-llm","name":"Empuje diagonal propio","nameEn":"Custom diagonal press","primaryMuscle":"Pecho","equipment":"Polea"},"sets":[{"reps":{"exact":12}}]}
                ]
              }]
            }
        """.trimIndent()

        val importedId = repository.importRoutine(raw)
        val exercises = database.routineDao().getRoutine(importedId)!!.orderedWorkouts().single().orderedExercises()
        assertEquals(bench.id, exercises[0].exercise.id)
        assertEquals("Barbell Row", exercises[1].exercise.nameEn)
        assertEquals("Jalón al pecho", exercises[2].exercise.nameEs)
        assertEquals("exercise-from-llm", exercises[3].exercise.id)
        assertTrue(exercises[3].exercise.isCustom)
    }

    @Test
    fun obviousUnknownDuplicatesAreReusedWithinImport() = runTest {
        val raw = """
            {"schemaVersion":1,"name":"Sin duplicados","workouts":[
              {"name":"A","exercises":[{"exercise":{"name":"Ejercicio inventado"},"sets":[{"reps":{"exact":10}}]}]},
              {"name":"B","exercises":[{"exercise":{"name":"ejercicio inventado"},"sets":[{"reps":{"exact":12}}]}]}
            ]}
        """.trimIndent()

        val id = repository.importRoutine(raw)
        val exerciseIds = database.routineDao().getRoutine(id)!!.orderedWorkouts()
            .flatMap { it.orderedExercises() }.map { it.exercise.id }
        assertEquals(1, exerciseIds.distinct().size)
    }

    @Test
    fun invalidIncompleteAndUnsupportedJsonFailWithoutPartialRoutine() = runTest {
        val invalidInputs = listOf(
            "not-json",
            """{"name":"Sin versión","workouts":[]}""",
            """{"schemaVersion":2,"name":"Futura","workouts":[]}""",
            """{"schemaVersion":1,"workouts":[]}""",
            """{"schemaVersion":1,"name":"Rota","workouts":[{"name":"A","exercises":[{"exercise":{"name":"Press"},"sets":[]}]}]}""",
            """{"schemaVersion":1,"name":"Rota","workouts":[{"name":"A","exercises":[{"exercise":{"name":"Press"},"sets":[{"reps":{"min":12,"max":8}}]}]}]}"""
        )

        invalidInputs.forEach { raw ->
            try {
                repository.importRoutine(raw)
                throw AssertionError("Se esperaba RoutineImportException para: $raw")
            } catch (_: RoutineImportException) {
                // Expected: validation happens before any insert.
            }
        }
        assertTrue(repository.routines.first().isEmpty())
    }
}
