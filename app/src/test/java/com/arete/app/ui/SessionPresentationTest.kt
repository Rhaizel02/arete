package com.arete.app.ui

import com.arete.app.data.local.*
import org.junit.Assert.*
import org.junit.Test

class SessionPresentationTest {
    @Test fun progressExcludesWarmupsAndHandlesNoWork() {
        val item = exercise("a", listOf(set("1", completed = true), set("2"), set("3", completed = true, warmup = true)))
        assertEquals(SessionWorkProgress(1, 2), sessionWorkProgress(listOf(item)))
        assertEquals(0.5f, sessionWorkProgress(listOf(item)).fraction)
        assertEquals(0f, sessionWorkProgress(emptyList()).fraction)
    }
    @Test fun selectedExerciseSurvivesCompletionReorderingAndDeletion() {
        val a = exercise("a", listOf(set("1", completed = true)))
        val b = exercise("b", listOf(set("2")))
        val c = exercise("c", emptyList())
        assertEquals("b", resolveSelectedExercise(listOf(a, b, c), null, 0))
        assertEquals("b", resolveSelectedExercise(listOf(a, b.copy(sets = b.sets.map { it.copy(isCompleted = true) }), c), "b", 1))
        assertEquals("b", resolveSelectedExercise(listOf(b, a, c), "b", 1))
        assertEquals("c", resolveSelectedExercise(listOf(a, c), "b", 1))
        assertNull(resolveSelectedExercise(emptyList(), "b", 1))
    }
    private fun set(id: String, completed: Boolean = false, warmup: Boolean = false) = SessionSetEntity(id, "a", 0, 8, 12, warmup, isCompleted = completed)
    private fun exercise(id: String, sets: List<SessionSetEntity>) = SessionExerciseWithSets(
        SessionExerciseEntity(id, "session", "catalog", 0, id, "", 120, null, "", null), sets
    )
}
