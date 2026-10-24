package com.arete.app.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExternalVolumeTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun date(year: Int, month: Int, day: Int, hour: Int = 12, zone: TimeZone = utc) = Calendar.getInstance(zone).apply {
        clear(); set(year, month - 1, day, hour, 0)
    }.timeInMillis
    private fun set(id: String = "s", kg: Double? = 80.0, reps: Int? = 10, warm: Boolean = false,
        done: Boolean = true, exercise: String? = "bench", muscle: String? = "Pecho", at: Long = date(2026, 10, 20)) =
        ProgressSet(id, at - 1000, at, "instance-$id", exercise, exercise ?: "Borrado", muscle, null, 0, 8, 12, warm, done, kg, reps, null, false)
    @Test fun exactFormulaAndExclusionsKeepWorkRepsSeparateFromLoad() {
        val rows = listOf(set(), set("warm", warm = true), set("pending", done = false), set("body", kg = null),
            set("zero", kg = 0.0), set("nan", kg = Double.NaN), set("inf", kg = Double.POSITIVE_INFINITY),
            set("negative", kg = -1.0), set("no-reps", reps = null), set("zero-reps", reps = 0))
        assertEquals(VolumeTotals(800.0, 6, 60, 1), ExternalVolumeMetrics.totals(rows))
        assertNull(ExternalVolumeMetrics.load(set(kg = Double.MAX_VALUE, reps = 100)))
    }
    @Test fun bodyweightIsRealWorkButNotFabricatedExternalLoad() {
        val value = ExternalVolumeMetrics.totals(listOf(set(kg = null), set("b", kg = 0.0, reps = 12)))
        assertEquals(VolumeTotals(null, 2, 22, 0), value)
        assertEquals(VolumeTotals(), ExternalVolumeMetrics.totals(emptyList()))
    }
    @Test fun sessionsExercisesAndMainMuscleAggregateOnceWithoutSecondaryCounts() {
        val now = date(2026, 10, 24)
        val result = ExternalVolumeMetrics.calculate(listOf(set(), set("b", kg = 20.0, exercise = "curl", muscle = "Bíceps"),
            set("c", kg = 10.0, exercise = null, muscle = null)), ProgressPeriod.ALL, now, utc)
        assertEquals(1100.0, result.totals.externalKgReps!!, 0.0)
        assertEquals(3, result.sessions.size)
        assertEquals(3, result.exercises.size)
        assertEquals(3, result.muscles.size)
        assertEquals(result.totals.externalKgReps!!, result.muscles.sumOf { it.totals.externalKgReps!! }, 0.0)
        assertTrue(result.muscles.any { it.label == "Sin clasificar" })
    }
    @Test fun filtersExcludeOutsideAndFutureAndMissingWeeksAreNotZero() {
        val now = date(2026, 10, 24)
        val result = ExternalVolumeMetrics.calculate(listOf(set(at = date(2026, 8, 1)), set(), set("future", at = now + 1000)), ProgressPeriod.MONTH, now, utc)
        assertEquals(800.0, result.totals.externalKgReps!!, 0.0)
        assertEquals(1, result.sessions.size)
        assertTrue(result.weeks.any { it.totals.externalKgReps == null && it.totals.effectiveSets == 0 })
    }
    @Test fun mondayWeeksUseCivilTimeAcrossDstAndYearBoundaries() {
        val madrid = TimeZone.getTimeZone("Europe/Madrid")
        val now = date(2026, 11, 2, zone = madrid)
        val result = ExternalVolumeMetrics.calculate(listOf(set(at = date(2026, 10, 24, zone = madrid)),
            set("dst", at = date(2026, 10, 25, zone = madrid)), set("monday", at = date(2026, 10, 26, zone = madrid))), ProgressPeriod.ALL, now, madrid)
        assertEquals(listOf(1600.0, 800.0, null), result.weeks.map { it.totals.externalKgReps })
        assertEquals(169L * 3_600_000, result.weeks[1].at - result.weeks[0].at)
        val crossing = ExternalVolumeMetrics.calculate(listOf(set(at = date(2025, 12, 31)), set("jan", at = date(2026, 1, 1))), ProgressPeriod.ALL, date(2026, 1, 3), utc)
        assertEquals(1, crossing.weeks.size)
        assertEquals(1600.0, crossing.weeks.single().totals.externalKgReps!!, 0.0)
    }
    @Test fun percentComparisonDoesNotInventImprovementFromZero() {
        assertNull(performanceDelta(100.0, 0.0)!!.percent)
    }
    @Test fun comparisonUsesOnlyTwoConsecutiveFullCalendarWeeksInsideThePeriod() {
        val now = date(2026, 10, 24)
        val result = ExternalVolumeMetrics.calculate(listOf(set(at = date(2026, 10, 7)), set("recent", kg = 100.0, at = date(2026, 10, 14)),
            set("partial", kg = 900.0, at = date(2026, 10, 21))), ProgressPeriod.MONTH, now, utc)
        assertEquals(PerformanceDelta(200.0, 25.0), ExternalVolumeMetrics.completeWeekChange(result, ProgressPeriod.MONTH, now, utc))
        assertNull(ExternalVolumeMetrics.completeWeekChange(result.copy(weeks = result.weeks.takeLast(1)), ProgressPeriod.ALL, now, utc))
    }
}
