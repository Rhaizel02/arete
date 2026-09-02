package com.arete.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.data.local.PrescriptionSetEntity
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.RoutineExerciseEntity
import com.arete.app.data.local.SessionStatus
import com.arete.app.data.local.SetType
import com.arete.app.data.local.WorkoutEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionRepositoryTest {
    private lateinit var database: AreteDatabase
    private lateinit var sessions: SessionRepository
    private var clock = 10_000L
    private var idCounter = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessions = SessionRepository(database, now = { clock }, newId = { "generated-${idCounter++}" })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun plannedSessionSnapshotsTemplateRecoversAndPreventsDuplicateActiveSession() = runTest {
        seedTemplate()

        val sessionId = sessions.startPlannedWorkout(WORKOUT_A)
        assertEquals(sessionId, sessions.startPlannedWorkout(WORKOUT_B))
        val started = database.sessionDao().getSession(sessionId)!!
        assertEquals("Torso A", started.session.workoutNameSnapshot)
        assertEquals(2, started.exercises.size)
        assertEquals(listOf("A1", "A1"), started.orderedExercises().map { it.exercise.groupIdSnapshot })
        assertEquals(listOf(5 to 5, 8 to 12), started.orderedExercises().first().orderedSets().map {
            it.targetRepsMinSnapshot to it.targetRepsMaxSnapshot
        })

        val routineItem = database.routineDao().getRoutine(ROUTINE)!!.orderedWorkouts().first()
            .orderedExercises().first().item
        database.routineDao().updateRoutineExercise(routineItem.copy(restSeconds = 30, notes = "Cambiada", groupId = null))
        database.exerciseDao().upsert(database.exerciseDao().findById(BENCH)!!.copy(nameEs = "Nombre nuevo"))
        val stillSnapshot = database.sessionDao().getSession(sessionId)!!.orderedExercises().first()
        assertEquals("Press banca", stillSnapshot.exercise.exerciseNameEsSnapshot)
        assertEquals(180, stillSnapshot.exercise.restSecondsSnapshot)
        assertEquals("A1", stillSnapshot.exercise.groupIdSnapshot)

        val set = stillSnapshot.orderedSets().last()
        sessions.updateSet(set.copy(weightKg = 80.0, reps = 10, rir = 2))
        val recoveredRepository = SessionRepository(database, now = { clock }, newId = { "recovered-${idCounter++}" })
        val recovered = recoveredRepository.homeState.first().activeSession!!
        val recoveredSet = recovered.orderedExercises().first().orderedSets().last()
        assertEquals(80.0, recoveredSet.weightKg!!, 0.0)
        assertEquals(10, recoveredSet.reps)
        assertEquals(2, recoveredSet.rir)
        assertFalse(recoveredSet.isCompleted)
    }

    @Test
    fun completedSessionFeedsLastPerformancePrefillAndNextSequentialWorkout() = runTest {
        seedTemplate()
        val firstId = sessions.startPlannedWorkout(WORKOUT_A)
        var first = database.sessionDao().getSession(firstId)!!
        val workSet = first.orderedExercises().first().orderedSets().last()
        sessions.updateSet(workSet.copy(weightKg = 82.5, reps = 9, rir = 1))
        val updatedSet = database.sessionDao().getSession(firstId)!!.orderedExercises().first().orderedSets().last()
        sessions.completeSet(updatedSet, timerEnabled = false)
        sessions.finishSession(database.sessionDao().getSession(firstId)!!.session)

        clock += 60_000
        val home = sessions.homeState.first()
        assertNull(home.activeSession)
        assertEquals(WORKOUT_B, home.nextWorkout!!.workoutId)

        val secondId = sessions.startPlannedWorkout(WORKOUT_A)
        val second = database.sessionDao().getSession(secondId)!!
        val prefilled = second.orderedExercises().first().orderedSets().last()
        assertEquals(82.5, prefilled.weightKg!!, 0.0)
        assertEquals(9, prefilled.reps)
        assertFalse(prefilled.isCompleted)
        val view = sessions.observeSession(secondId).first()
        assertEquals(82.5, view.lastPerformance[BENCH]!!.last().weightKg!!, 0.0)
    }

    @Test
    fun freeWorkoutSupportsDynamicExercisesSetsSubstitutionTimerFinishAndDelete() = runTest {
        seedExercises()
        val sessionId = sessions.startFreeWorkout()
        val addedId = sessions.addExercise(sessionId, BENCH)
        var exercise = database.sessionDao().getExercise(addedId)!!
        assertEquals(BENCH, exercise.exerciseId)
        assertEquals(3, database.sessionDao().getSets(addedId).size)

        sessions.substituteExercise(addedId, DUMBBELL_PRESS)
        exercise = database.sessionDao().getExercise(addedId)!!
        assertEquals(DUMBBELL_PRESS, exercise.exerciseId)
        assertEquals("Press mancuernas", exercise.exerciseNameEsSnapshot)

        val extraSetId = sessions.addSet(addedId)
        assertEquals(4, database.sessionDao().getSets(addedId).size)
        val extraSet = database.sessionDao().getSets(addedId).single { it.id == extraSetId }
        sessions.deleteSet(extraSet)
        assertEquals(listOf(0, 1, 2), database.sessionDao().getSets(addedId).map { it.position })

        val secondExerciseId = sessions.addExercise(sessionId, ROW)
        sessions.moveExercise(sessionId, secondExerciseId, -1)
        assertEquals(secondExerciseId, database.sessionDao().getExercises(sessionId).first().id)
        sessions.deleteExercise(database.sessionDao().getExercise(secondExerciseId)!!)
        assertEquals(listOf(0), database.sessionDao().getExercises(sessionId).map { it.position })

        var set = database.sessionDao().getSets(addedId).first()
        sessions.updateSet(set.copy(weightKg = 24.0, reps = 12, isFailure = true, rir = null))
        set = database.sessionDao().getSets(addedId).first()
        sessions.completeSet(set, timerEnabled = true)
        var active = database.sessionDao().getSession(sessionId)!!
        assertTrue(active.orderedExercises().first().orderedSets().first().isCompleted)
        assertNotNull(active.session.restTimerEndAt)
        sessions.adjustTimer(active.session, 15)
        active = database.sessionDao().getSession(sessionId)!!
        sessions.skipTimer(active.session)
        assertNull(database.sessionDao().getSession(sessionId)!!.session.restTimerEndAt)

        sessions.finishSession(database.sessionDao().getSession(sessionId)!!.session)
        val completed = database.sessionDao().getSession(sessionId)!!
        assertEquals(SessionStatus.COMPLETED, completed.session.status)
        assertNull(database.sessionDao().getActiveSession())
        assertEquals(sessionId, sessions.completedSessions.first().single().session.id)

        val routineId = sessions.saveAsRoutine(sessionId, "Rutina desde libre")
        val routine = database.routineDao().getRoutine(routineId)!!
        assertEquals("Rutina desde libre", routine.routine.name)
        assertEquals(DUMBBELL_PRESS, routine.orderedWorkouts().single().orderedExercises().single().exercise.id)
        assertEquals(3, routine.orderedWorkouts().single().orderedExercises().single().sets.size)

        sessions.deleteSession(completed.session)
        assertNull(database.sessionDao().getSession(sessionId))
    }

    @Test
    fun plannedSubstitutionNeverChangesRoutineTemplate() = runTest {
        seedTemplate()
        val sessionId = sessions.startPlannedWorkout(WORKOUT_A)
        val sessionExercise = database.sessionDao().getSession(sessionId)!!.orderedExercises().first().exercise
        sessions.substituteExercise(sessionExercise.id, DUMBBELL_PRESS)

        val changedSession = database.sessionDao().getSession(sessionId)!!.orderedExercises().first().exercise
        val routineExercise = database.routineDao().getRoutine(ROUTINE)!!.orderedWorkouts().first().orderedExercises().first()
        assertEquals(DUMBBELL_PRESS, changedSession.exerciseId)
        assertEquals(BENCH, routineExercise.exercise.id)
        assertEquals(sessionExercise.sourceRoutineExerciseId, changedSession.sourceRoutineExerciseId)
    }

    private suspend fun seedExercises() {
        database.exerciseDao().upsertAll(listOf(
            exercise(BENCH, "Press banca", "Bench Press"),
            exercise(DUMBBELL_PRESS, "Press mancuernas", "Dumbbell Press"),
            exercise(ROW, "Remo", "Row")
        ))
    }

    private suspend fun seedTemplate() {
        seedExercises()
        database.routineDao().insertRoutine(RoutineEntity(ROUTINE, "Torso/Pierna", true, 1, 1))
        database.routineDao().insertWorkout(WorkoutEntity(WORKOUT_A, ROUTINE, "Torso A", 0, createdAt = 1, updatedAt = 1))
        database.routineDao().insertWorkout(WorkoutEntity(WORKOUT_B, ROUTINE, "Pierna", 1, createdAt = 1, updatedAt = 1))
        database.routineDao().insertRoutineExercise(
            RoutineExerciseEntity(ROUTINE_BENCH, WORKOUT_A, BENCH, 0, restSeconds = 180, notes = "Pausa", groupId = "A1", createdAt = 1, updatedAt = 1)
        )
        database.routineDao().insertRoutineExercise(
            RoutineExerciseEntity(ROUTINE_ROW, WORKOUT_A, ROW, 1, restSeconds = 120, groupId = "A1", createdAt = 1, updatedAt = 1)
        )
        database.routineDao().insertSets(listOf(
            PrescriptionSetEntity("bench-warmup", ROUTINE_BENCH, 0, SetType.WARMUP, 5, 5),
            PrescriptionSetEntity("bench-work", ROUTINE_BENCH, 1, SetType.WORK, 8, 12),
            PrescriptionSetEntity("row-work", ROUTINE_ROW, 0, SetType.WORK, 10, 10)
        ))
    }

    private fun exercise(id: String, name: String, nameEn: String) = ExerciseEntity(
        id = id,
        nameEs = name,
        nameEn = nameEn,
        primaryMuscle = "Pecho",
        equipment = "Barra",
        isCustom = false,
        createdAt = 1,
        updatedAt = 1
    )

    private companion object {
        const val ROUTINE = "routine"
        const val WORKOUT_A = "workout-a"
        const val WORKOUT_B = "workout-b"
        const val BENCH = "bench"
        const val DUMBBELL_PRESS = "dumbbell-press"
        const val ROW = "row"
        const val ROUTINE_BENCH = "routine-bench"
        const val ROUTINE_ROW = "routine-row"
    }
}
