package com.arete.app.ui

import com.arete.app.data.local.SessionExerciseWithSets

data class SessionWorkProgress(val completed: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

fun sessionWorkProgress(exercises: List<SessionExerciseWithSets>): SessionWorkProgress {
    val work = exercises.flatMap { it.sets }.filterNot { it.isWarmupSnapshot }
    return SessionWorkProgress(work.count { it.isCompleted }, work.size)
}

fun resolveSelectedExercise(exercises: List<SessionExerciseWithSets>, selectedId: String?, previousIndex: Int): String? {
    if (exercises.isEmpty()) return null
    if (selectedId != null) return exercises.firstOrNull { it.exercise.id == selectedId }?.exercise?.id
        ?: exercises[previousIndex.coerceIn(0, exercises.lastIndex)].exercise.id
    return (exercises.firstOrNull { item -> item.sets.any { !it.isWarmupSnapshot && !it.isCompleted } } ?: exercises.first()).exercise.id
}
