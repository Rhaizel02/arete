package com.arete.app.data

import com.arete.app.data.local.LastPerformanceRow

/** One historical occurrence only: template identity first, otherwise the relative occurrence. */
fun historicalOccurrence(
    rows: List<LastPerformanceRow>, exerciseId: String, sourceId: String?, occurrence: Int
): List<LastPerformanceRow> {
    val instances = rows.filter { it.exerciseId == exerciseId }
        .groupBy { it.sessionExerciseId }.values.sortedWith(
            compareBy<List<LastPerformanceRow>> { it.first().exercisePosition }.thenBy { it.first().sessionExerciseId })
    val sameSource = sourceId?.let { id -> instances.firstOrNull { it.first().sourceRoutineExerciseId == id } }
    if (sameSource != null) return sameSource.sortedBy { it.position }
    // A missing completed occurrence must not borrow another template occurrence after reordering.
    if (sourceId != null && instances.any { it.first().occurrenceCount > 1 } &&
        instances.any { it.first().sourceRoutineExerciseId != null }) return emptyList()
    return instances.firstOrNull { it.first().occurrenceIndex == occurrence }.orEmpty().sortedBy { it.position }
}

/** No progression or cross-type fallback. Extra sets repeat the last comparable completed set. */
fun prefillSets(warmups: List<Boolean>, history: List<LastPerformanceRow>): List<LastPerformanceRow?> {
    val byType = history.sortedBy { it.position }.groupBy { it.isWarmupSnapshot }
    val offsets = mutableMapOf<Boolean, Int>()
    return warmups.map { type ->
        val ordinal = offsets[type] ?: 0
        offsets[type] = ordinal + 1
        byType[type]?.let { rows -> rows.getOrNull(ordinal) ?: rows.lastOrNull() }
    }
}
