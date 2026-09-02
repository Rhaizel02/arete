package com.arete.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ProgressMetricsTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun epleyEstimateIsEncapsulatedAndRejectsInvalidOrUnrepresentativeSets() {
        assertEquals(133.333, E1RmEstimator.estimate(100.0, 10)!!, 0.001)
        assertEquals(103.333, E1RmEstimator.estimate(100.0, 1)!!, 0.001)
        assertNull(E1RmEstimator.estimate(0.0, 8))
        assertNull(E1RmEstimator.estimate(100.0, 0))
        assertNull(E1RmEstimator.estimate(100.0, 13))
        assertNull(E1RmEstimator.estimate(Double.NaN, 8))
    }

    @Test
    fun detectsMaxWeightE1RmAndRepPrsFromRealHistory() {
        val first = set("s1", 1_000, 80.0, 8)
        val second = set("s2", 2_000, 85.0, 8)
        val secondBackoff = set("s2", 2_000, 80.0, 10, position = 1)
        val records = ProgressMetrics.recordsForSession(listOf(first, second, secondBackoff), "s2")
        assertEquals(setOf(PrType.MAX_WEIGHT, PrType.MAX_E1RM, PrType.REPS_AT_WEIGHT), records.map { it.type }.toSet())
        assertEquals(85.0, records.single { it.type == PrType.MAX_WEIGHT }.weightKg, 0.0)

        val third = set("s3", 3_000, 85.0, 9)
        val thirdRecords = ProgressMetrics.recordsForSession(listOf(first, second, secondBackoff, third), "s3")
        assertFalse(thirdRecords.any { it.type == PrType.MAX_WEIGHT })
        assertTrue(thirdRecords.any { it.type == PrType.MAX_E1RM })
        assertTrue(thirdRecords.any { it.type == PrType.REPS_AT_WEIGHT && it.reps == 9 })
    }

    @Test
    fun warmupsAndIncompleteSetsNeverAffectPerformanceOrRecords() {
        val work = set("s1", 1_000, 100.0, 8)
        val warmup = set("s1", 1_000, 200.0, 3, position = 1, warmup = true)
        val incomplete = set("s1", 1_000, 250.0, 5, position = 2, completed = false)
        val records = ProgressMetrics.recordsForSession(listOf(work, warmup, incomplete), "s1")
        assertEquals(100.0, records.single { it.type == PrType.MAX_WEIGHT }.weightKg, 0.0)
        assertNull(ProgressMetrics.e1Rm(warmup))
        assertNull(ProgressMetrics.e1Rm(incomplete))
        assertTrue(ProgressMetrics.isEffectiveWorkSet(work))
        assertFalse(ProgressMetrics.isEffectiveWorkSet(warmup))
    }

    @Test
    fun weekStartsMondayAndIncompleteCurrentWeekDoesNotPrematurelyBreakStreak() {
        val mondayPrevious = epoch(2026, Calendar.AUGUST, 24, 9)
        val sundayPrevious = epoch(2026, Calendar.AUGUST, 30, 20)
        val mondayCurrent = epoch(2026, Calendar.AUGUST, 31, 10)
        val nowWednesday = epoch(2026, Calendar.SEPTEMBER, 2, 12)
        assertEquals(epoch(2026, Calendar.AUGUST, 24, 0), ProgressMetrics.weekStart(sundayPrevious, utc))
        assertEquals(epoch(2026, Calendar.AUGUST, 31, 0), ProgressMetrics.weekStart(mondayCurrent, utc))

        val summary = ProgressMetrics.weeklyConsistency(
            listOf(mondayPrevious, sundayPrevious, mondayCurrent), goal = 2, now = nowWednesday, timeZone = utc
        )
        assertEquals(1, summary.currentCount)
        assertEquals(1, summary.streak)

        val completedCurrent = ProgressMetrics.weeklyConsistency(
            listOf(mondayPrevious, sundayPrevious, mondayCurrent, epoch(2026, Calendar.SEPTEMBER, 1, 10)),
            goal = 2, now = nowWednesday, timeZone = utc
        )
        assertEquals(2, completedCurrent.streak)
    }

    @Test
    fun progressionRequiresEveryWorkSetAtUpperRangeSameLoadAndNoFailure() {
        val achieved = (0..2).map { position ->
            set("s1", 1_000, 80.0, 12, position = position, targetMax = 12, increment = 2.5, rir = 1)
        }
        val suggestion = ProgressMetrics.progressionSuggestions(achieved).single()
        assertEquals(82.5, suggestion.suggestedWeightKg, 0.0)

        assertTrue(ProgressMetrics.progressionSuggestions(achieved.mapIndexed { index, item ->
            if (index == 2) item.copy(reps = 11) else item
        }).isEmpty())
        assertTrue(ProgressMetrics.progressionSuggestions(achieved.mapIndexed { index, item ->
            if (index == 2) item.copy(isFailure = true) else item
        }).isEmpty())
    }

    @Test
    fun bodyWeightTrendUsesLatestSevenValidEntries() {
        val trend = ProgressMetrics.bodyWeightTrend(listOf(100.0, 99.0, 98.0, 97.0, 96.0, 95.0, 94.0, 93.0))
        assertEquals(93.0, trend.latestKg!!, 0.0)
        assertEquals(96.0, trend.movingAverageKg!!, 0.0)
        assertEquals(-6.0, trend.recentChangeKg!!, 0.0)
        assertNull(ProgressMetrics.bodyWeightTrend(emptyList()).latestKg)
    }

    private fun set(
        session: String,
        completedAt: Long,
        weight: Double,
        reps: Int,
        position: Int = 0,
        warmup: Boolean = false,
        completed: Boolean = true,
        targetMax: Int = 12,
        increment: Double? = 2.5,
        rir: Int? = null
    ) = ProgressSet(
        sessionId = session,
        sessionStartedAt = completedAt - 100,
        sessionCompletedAt = completedAt,
        sessionExerciseId = "$session-exercise",
        exerciseId = "bench",
        exerciseName = "Press banca",
        primaryMuscle = "Pecho",
        incrementKg = increment,
        position = position,
        targetRepsMin = 8,
        targetRepsMax = targetMax,
        isWarmup = warmup,
        isCompleted = completed,
        weightKg = weight,
        reps = reps,
        rir = rir,
        isFailure = false
    )

    private fun epoch(year: Int, month: Int, day: Int, hour: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month, day, hour, 0, 0)
    }.timeInMillis
}
