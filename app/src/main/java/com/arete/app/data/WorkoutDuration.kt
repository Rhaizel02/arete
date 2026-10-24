package com.arete.app.data

import com.arete.app.data.local.WorkoutWithExercises
import kotlin.math.ceil
import kotlin.math.floor

data class WorkoutDuration(val minimumMinutes: Int, val maximumMinutes: Int, val grouped: Boolean) {
    val label: String get() = "Duración estimada: $minimumMinutes–$maximumMinutes min"
}

/** 2–4 s/rep, 60–120 s preparation/exercise. Includes warmups; no rest after each exercise's last set. */
fun estimateWorkoutDuration(workout: WorkoutWithExercises): WorkoutDuration? {
    val exercises = workout.orderedExercises().filter { it.sets.isNotEmpty() }
    if (exercises.isEmpty()) return null
    var low = 0L
    var high = 0L
    exercises.forEach { exercise ->
        val rest = exercise.item.restSeconds.coerceIn(0, 3600).toLong() * (exercise.sets.size - 1)
        low += 60 + rest + exercise.sets.sumOf { it.repsMin.coerceAtLeast(1).toLong() * 2 }
        high += 120 + rest + exercise.sets.sumOf { it.repsMax.coerceAtLeast(it.repsMin.coerceAtLeast(1)).toLong() * 4 }
    }
    return WorkoutDuration(floor(low / 60.0).toInt().coerceAtLeast(1), ceil(high / 60.0).toInt().coerceAtLeast(1),
        exercises.any { !it.item.groupId.isNullOrBlank() })
}
