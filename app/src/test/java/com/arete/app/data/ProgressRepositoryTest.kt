package com.arete.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.arete.app.data.local.AreteDatabase
import com.arete.app.data.local.ExerciseEntity
import com.arete.app.data.local.BodyWeightEntryEntity
import com.arete.app.data.local.WorkoutSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressRepositoryTest {
    private lateinit var database: AreteDatabase
    private lateinit var sessions: SessionRepository
    private lateinit var progress: ProgressRepository
    private var clock = 1_800_000_000_000L
    private var ids = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AreteDatabase::class.java).allowMainThreadQueries().build()
        sessions = SessionRepository(database, now = { clock }, newId = { "session-${ids++}" })
        progress = ProgressRepository(database, now = { clock }, newId = { "weight-${ids++}" })
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun editsAndDeletesRecalculateProgressPrAndMainStatisticsFromRoom() = runTest {
        database.exerciseDao().upsert(exercise())
        val sessionId = sessions.startFreeWorkout()
        val exerciseId = sessions.addExercise(sessionId, "bench")
        val sets = database.sessionDao().getSets(exerciseId)
        sessions.updateSet(sets[0].copy(weightKg = 100.0, reps = 10))
        sessions.completeSet(database.sessionDao().getSet(sets[0].id)!!, false)
        sessions.updateSet(sets[1].copy(weightKg = 200.0, reps = 3, isWarmupSnapshot = true))
        sessions.completeSet(database.sessionDao().getSet(sets[1].id)!!, false)
        sessions.updateSet(sets[2].copy(weightKg = 250.0, reps = 5))
        clock += 3_600_000
        sessions.finishSession(database.sessionDao().getSession(sessionId)!!.session)

        var dashboard = progress.dashboard(flowOf(3)).first()
        assertEquals(1, dashboard.stats.totalWorkouts)
        assertEquals(1, dashboard.stats.effectiveWorkSets)
        assertEquals(60, dashboard.stats.averageDurationMinutes)
        assertEquals(1, dashboard.stats.muscleDistribution.single().sets)
        assertEquals(1, dashboard.stats.monthlyActivity.single().workouts)
        assertTrue(dashboard.stats.workoutsPerWeek > 0.0)
        assertEquals(100.0, dashboard.exercises.single().maxWeightKg!!, 0.0)
        assertEquals(1, dashboard.exercises.single().sessionCount)

        sessions.updateSet(database.sessionDao().getSet(sets[0].id)!!.copy(weightKg = 80.0, reps = 8))
        dashboard = progress.dashboard(flowOf(3)).first()
        assertEquals(80.0, dashboard.exercises.single().maxWeightKg!!, 0.0)
        assertEquals(80.0, dashboard.latestRecord!!.weightKg, 0.0)

        sessions.deleteSession(database.sessionDao().getSession(sessionId)!!.session)
        dashboard = progress.dashboard(flowOf(3)).first()
        assertEquals(0, dashboard.stats.totalWorkouts)
        assertTrue(dashboard.exercises.isEmpty())
        assertNull(dashboard.latestRecord)
    }

    @Test
    fun bodyWeightCrudRecalculatesTrend() = runTest {
        val firstId = progress.saveBodyWeight(null, 80.0, "inicio")
        clock += 86_400_000
        val secondId = progress.saveBodyWeight(null, 79.0)
        var dashboard = progress.dashboard(flowOf(3)).first()
        assertEquals(2, dashboard.bodyWeights.size)
        assertEquals(-1.0, dashboard.bodyWeightTrend.recentChangeKg!!, 0.0)

        val correctedDate = clock - 2 * 86_400_000L
        progress.saveBodyWeight(secondId, 78.5, "corregido", correctedDate)
        dashboard = progress.dashboard(flowOf(3)).first()
        assertEquals(secondId, dashboard.bodyWeights.first().id)
        assertEquals(correctedDate, dashboard.bodyWeights.first().recordedAt)
        assertEquals(80.0, dashboard.bodyWeightTrend.latestKg!!, 0.0)
        assertEquals("corregido", dashboard.bodyWeights.first().notes)

        progress.deleteBodyWeight(dashboard.bodyWeights.single { it.id == firstId })
        dashboard = progress.dashboard(flowOf(3)).first()
        assertEquals(1, dashboard.bodyWeights.size)
        assertNull(dashboard.bodyWeightTrend.recentChangeKg)
    }

    @Test
    fun periodStatsWeightsAndComparisonsRecalculateButAbsolutePrRemainsHistorical() = runTest {
        database.exerciseDao().upsert(exercise())
        val first = sessions.startFreeWorkout()
        val firstExercise = sessions.addExercise(first, "bench")
        val firstSet = database.sessionDao().getSets(firstExercise).first()
        sessions.updateSet(firstSet.copy(weightKg = 100.0, reps = 10))
        sessions.completeSet(database.sessionDao().getSet(firstSet.id)!!, false)
        clock += 60_000
        sessions.finishSession(database.sessionDao().getSession(first)!!.session)
        progress.saveBodyWeight(null, 80.0)
        clock += 40L * 86_400_000
        val second = sessions.startFreeWorkout()
        val secondExercise = sessions.addExercise(second, "bench")
        val secondSet = database.sessionDao().getSets(secondExercise).first()
        sessions.updateSet(secondSet.copy(weightKg = 80.0, reps = 8))
        sessions.completeSet(database.sessionDao().getSet(secondSet.id)!!, false)
        // Active sets do not feed confirmed history, even if completed.
        assertEquals(1, progress.dashboard(flowOf(3)).first().confirmedSets.count { it.isCompleted && !it.isWarmup })
        clock += 60_000
        sessions.finishSession(database.sessionDao().getSession(second)!!.session)
        progress.saveBodyWeight(null, 79.0)
        var filtered = progress.dashboard(flowOf(3), flowOf(ProgressPeriod.MONTH)).first()
        assertEquals(1, filtered.stats.totalWorkouts)
        assertEquals(1, filtered.stats.effectiveWorkSets)
        assertEquals(79.0, filtered.bodyWeights.single().weightKg, 0.0)
        assertEquals(80.0, filtered.exercises.single().maxWeightKg!!, 0.0)
        assertEquals(100.0, filtered.exercises.single().records.first { it.type == PrType.MAX_WEIGHT }.weightKg, 0.0)
        assertNull(sessionComparison(filtered.exercises.single().history))
        sessions.updateSet(database.sessionDao().getSet(secondSet.id)!!.copy(weightKg = 85.0))
        filtered = progress.dashboard(flowOf(3), flowOf(ProgressPeriod.MONTH)).first()
        assertEquals(85.0, filtered.exercises.single().maxWeightKg!!, 0.0)
        sessions.deleteSession(database.sessionDao().getSession(second)!!.session)
        filtered = progress.dashboard(flowOf(3), flowOf(ProgressPeriod.MONTH)).first()
        assertEquals(0, filtered.stats.totalWorkouts)
        assertTrue(filtered.exercises.single().history.isEmpty())
        assertEquals(100.0, filtered.exercises.single().records.first { it.type == PrType.MAX_WEIGHT }.weightKg, 0.0)
    }

    private fun exercise() = ExerciseEntity(
        id = "bench",
        nameEs = "Press banca",
        nameEn = "Bench Press",
        primaryMuscle = "Pecho",
        equipment = "Barra",
        createdAt = 1,
        updatedAt = 1
    )
}
