package com.arete.app.data

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone
import java.time.ZonedDateTime

class ProgressPeriodTest {
    private val madrid = TimeZone.getTimeZone("Europe/Madrid")
    private fun epoch(date: String) = ZonedDateTime.parse(date).toInstant().toEpochMilli()

    @Test fun calendarMonthBoundariesRespectLocalMidnightDstAndInclusiveEnd() {
        val now = epoch("2026-03-31T12:00:00+02:00[Europe/Madrid]")
        val start = epoch("2026-02-28T00:00:00+01:00[Europe/Madrid]")
        assertEquals(start, ProgressPeriod.MONTH.start(now, madrid))
        assertTrue(ProgressPeriod.MONTH.contains(start, now, madrid))
        assertFalse(ProgressPeriod.MONTH.contains(start - 1, now, madrid))
        assertTrue(ProgressPeriod.MONTH.contains(now, now, madrid))
        assertFalse(ProgressPeriod.ALL.contains(now + 1, now, madrid))
        assertEquals(epoch("2025-12-31T00:00:00+01:00[Europe/Madrid]"), ProgressPeriod.QUARTER.start(now, madrid))
        assertEquals(epoch("2025-09-30T00:00:00+02:00[Europe/Madrid]"), ProgressPeriod.HALF_YEAR.start(now, madrid))
        assertNull(ProgressPeriod.ALL.start(now, madrid))
    }

    @Test fun comparisonsHandlePositiveNegativeNeutralAndMissingEstimates() {
        fun point(time: Long, estimate: Double?) = ExerciseSessionPoint("s$time", time, null, null, null, estimate)
        val points = listOf(point(1, 100.0), point(2, 110.0), point(3, 105.0))
        assertEquals(-5.0, sessionComparison(points)!!.amount, 0.0)
        assertEquals(5.0, periodComparison(points)!!.percent!!, .001)
        assertEquals(0.0, performanceDelta(100.0, 100.0)!!.percent!!, 0.0)
        assertNull(performanceDelta(10.0, 0.0)!!.percent)
        assertNull(performanceDelta(10.0, -1.0)!!.percent)
        assertNull(performanceDelta(Double.NaN, 20.0))
        assertNull(sessionComparison(points + point(4, null)))
        assertNull(periodComparison(listOf(point(1, 100.0))))
        assertNull(sessionComparison(emptyList()))
    }

    @Test fun provisionalImprovementsRequireCompletedComparableWorkAndNeverBecomeHistory() {
        fun set(session: String, date: Long, kg: Double, reps: Int) = ProgressSet(
            session, date - 1, date, "$session-e", "bench", "Press", "Pecho", 2.5, 0,
            6, 12, false, true, kg, reps, 2, false
        )
        val history = listOf(set("old", 100, 80.0, 8))
        val candidate = set("active", 200, 80.0, 10)
        assertEquals(setOf(PrType.MAX_E1RM, PrType.REPS_AT_WEIGHT), ProgressMetrics.improvements(candidate, history).toSet())
        assertTrue(ProgressMetrics.improvements(candidate.copy(isCompleted = false), history).isEmpty())
        assertTrue(ProgressMetrics.improvements(candidate.copy(isWarmup = true), history).isEmpty())
        assertTrue(ProgressMetrics.improvements(candidate.copy(exerciseId = "squat"), history).isEmpty())
        assertTrue(ProgressMetrics.improvements(candidate, emptyList()).isEmpty())
        assertTrue(ProgressMetrics.improvements(candidate.copy(weightKg = 90.0), history).contains(PrType.MAX_WEIGHT))
        assertFalse(ProgressMetrics.improvements(candidate.copy(weightKg = 90.0), history).contains(PrType.REPS_AT_WEIGHT))
        assertEquals("old", ProgressMetrics.allRecordEvents(history).first().sessionId)
        assertTrue(ProgressMetrics.allRecordEvents(history + candidate).any { it.sessionId == "active" })
    }
}
