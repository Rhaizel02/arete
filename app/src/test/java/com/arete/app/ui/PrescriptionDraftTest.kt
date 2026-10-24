package com.arete.app.ui

import com.arete.app.data.local.SetType
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PrescriptionDraftTest {
    @Test fun savedRowsKeepIdentityOrderAndInvalidUncommittedInputAcrossRecreation() {
        val rows = listOf(SetEditorState(type = SetType.WARMUP, min = "", max = "oops"), SetEditorState(min = "6", max = "9"))
        val reordered = rows.reversed()
        val restored = Json.decodeFromString<List<SetEditorState>>(Json.encodeToString(reordered))
        assertEquals(reordered, restored)
        assertEquals(2, restored.map { it.id }.distinct().size)
        assertEquals("", restored.last().min)
    }
}
