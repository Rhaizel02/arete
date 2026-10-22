package com.arete.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import com.arete.app.data.local.PrescriptionSetEntity
import com.arete.app.data.local.SetType

class EditorPresentationTest {
    @Test fun summaryGroupsOnlyConsecutiveEqualSetsAndKeepsWarmupsDistinct() {
        val warmup = PrescriptionSetEntity("a", "item", 0, SetType.WARMUP, 5, 5)
        val work = PrescriptionSetEntity("b", "item", 1, SetType.WORK, 8, 12)
        assertEquals("1 × 5 reps calent. · 2 × 8–12 reps · 1 × 5 reps calent.", templateSetSummary(listOf(warmup, work, work.copy(id = "c", position = 2), warmup.copy(id = "d", position = 3))))
        assertEquals("Sin series", templateSetSummary(emptyList()))
    }
    @Test fun draftOrderChangesWithoutDroppingValuesAndClampsAtBounds() {
        val values = mutableListOf("warmup-5", "work-8-12", "work-6")
        moveEditorItem(values, 2, -1)
        assertEquals(listOf("warmup-5", "work-6", "work-8-12"), values)
        moveEditorItem(values, 0, -1)
        moveEditorItem(values, 8, 1)
        assertEquals(listOf("warmup-5", "work-6", "work-8-12"), values)
        moveEditorItem(mutableListOf<String>(), 0, 1)
    }
}
