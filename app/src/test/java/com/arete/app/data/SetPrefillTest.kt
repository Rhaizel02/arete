package com.arete.app.data

import com.arete.app.data.local.LastPerformanceRow
import org.junit.Assert.*
import org.junit.Test

class SetPrefillTest {
    private fun row(position: Int, warm: Boolean = false, instance: String = "a", source: String? = "template", kg: Double = position + 10.0) =
        LastPerformanceRow("bench", "old", 100, position, kg, position + 5, warm, null, false, instance,
            if (instance == "a") 0 else 1, source, if (instance == "a") 0 else 1)
    @Test fun identicalStructuresKeepValuesAndType() {
        val rows = listOf(row(0, true), row(1), row(2))
        assertEquals(rows, prefillSets(listOf(true, false, false), rows))
    }
    @Test fun threeToFourRepeatsLastWorkWithoutProgression() {
        val rows = (0..2).map { row(it) }
        assertEquals(rows + rows.last(), prefillSets(List(4) { false }, rows))
    }
    @Test fun fourToThreeTakesFirstThreeInRelativeOrder() {
        val rows = (0..3).map { row(it) }
        assertEquals(rows.take(3), prefillSets(List(3) { false }, rows))
    }
    @Test fun insertedWarmupCannotBorrowWorkAndRemovedWarmupCannotShiftWork() {
        val work = listOf(row(0), row(1))
        assertEquals(listOf(null) + work, prefillSets(listOf(true, false, false), work))
        val warmAndWork = listOf(row(0, true), row(1), row(2))
        assertEquals(warmAndWork.drop(1), prefillSets(listOf(false, false), warmAndWork))
    }
    @Test fun interleavedAndReorderedTypesRemainSeparated() {
        val rows = listOf(row(3), row(2, true), row(0), row(1, true))
        assertEquals(listOf(row(1, true), row(0), row(2, true), row(3)), prefillSets(listOf(true, false, true, false), rows))
    }
    @Test fun noHistoryOrOtherExerciseOrMissingOccurrenceDoesNotBorrow() {
        assertEquals(listOf(null, null), prefillSets(listOf(true, false), emptyList()))
        assertTrue(historicalOccurrence(listOf(row(0)), "row", null, 0).isEmpty())
        assertTrue(historicalOccurrence(listOf(row(0)), "bench", null, 1).isEmpty())
    }
    @Test fun repeatedExercisesMatchSourceIdentityOrOccurrenceWithoutMixing() {
        val a = row(0); val b = row(0, instance = "b", source = "second", kg = 99.0)
        assertEquals(listOf(b), historicalOccurrence(listOf(b, a), "bench", "second", 0))
        assertEquals(listOf(b), historicalOccurrence(listOf(b, a), "bench", null, 1))
        assertEquals(listOf(a), historicalOccurrence(listOf(b, a), "bench", null, 0))
    }
    @Test fun missingCompletedFirstOccurrenceCannotShiftSecondIntoFirst() {
        val b = row(0, instance = "b", source = null)
        assertTrue(historicalOccurrence(listOf(b), "bench", null, 0).isEmpty())
        assertEquals(listOf(b), historicalOccurrence(listOf(b), "bench", null, 1))
    }
    @Test fun reorderedTemplateOccurrenceWithNoCompletedRowsCannotBorrowAnotherSource() {
        val first = row(0, source = "first").copy(occurrenceCount = 2)
        assertTrue(historicalOccurrence(listOf(first), "bench", "second", 0).isEmpty())
        assertEquals(listOf(first), historicalOccurrence(listOf(first), "bench", "first", 1))
        val single = first.copy(occurrenceCount = 1)
        assertEquals(listOf(single), historicalOccurrence(listOf(single), "bench", "new-template", 0))
    }
}
