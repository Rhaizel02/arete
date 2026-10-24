package com.arete.app.data

import com.arete.app.data.local.*
import org.junit.Assert.*
import org.junit.Test

class WorkoutDurationTest {
    private fun workout(count: Int = 1, rest: Int = 90, warmup: Boolean = false, group: String? = null, min: Int = 10, max: Int = 10, exercises: Int = 1): WorkoutWithExercises =
        WorkoutWithExercises(WorkoutEntity("w", "r", "Test", 0, createdAt = 1, updatedAt = 1), (0 until exercises).map { index ->
            RoutineExerciseWithDetails(RoutineExerciseEntity("e$index", "w", "x", index, restSeconds = rest, groupId = group, createdAt = 1, updatedAt = 1),
                ExerciseEntity("x", "X", "X", "Pecho", equipment = "Barra", createdAt = 1, updatedAt = 1),
                (0 until count).map { PrescriptionSetEntity("s$index-$it", "e$index", it, if (warmup && it == 0) SetType.WARMUP else SetType.WORK, min, max) })
        })
    @Test fun emptyWorkoutAndEmptyPrescriptionHaveNoInventedEstimate() {
        assertNull(estimateWorkoutDuration(workout(exercises = 0)))
        assertNull(estimateWorkoutDuration(workout(count = 0)))
    }
    @Test fun oneSetHasNoRestAfterItEvenWithMaximumConfiguredRest() {
        assertEquals(WorkoutDuration(1, 3, false), estimateWorkoutDuration(workout(rest = 3600)))
        assertEquals(estimateWorkoutDuration(workout(rest = 0)), estimateWorkoutDuration(workout(rest = 3600)))
    }
    @Test fun warmupsCountAndGroupsDoNotInventTimeSavings() {
        val plain = estimateWorkoutDuration(workout(count = 2))!!
        assertEquals(WorkoutDuration(3, 5, false), plain)
        assertEquals(plain, estimateWorkoutDuration(workout(count = 2, warmup = true)))
        assertEquals(plain.copy(grouped = true), estimateWorkoutDuration(workout(count = 2, group = "A")))
    }
    @Test fun editsExercisesRangesAndRestUpdateDeterministically() {
        val initial = estimateWorkoutDuration(workout(count = 2))!!
        assertTrue(estimateWorkoutDuration(workout(count = 3))!!.maximumMinutes > initial.maximumMinutes)
        assertTrue(estimateWorkoutDuration(workout(count = 2, max = 20))!!.maximumMinutes > initial.maximumMinutes)
        assertTrue(estimateWorkoutDuration(workout(count = 2, exercises = 2))!!.minimumMinutes > initial.minimumMinutes)
        assertTrue(estimateWorkoutDuration(workout(count = 2, rest = 3600))!!.minimumMinutes > initial.minimumMinutes)
    }
}
