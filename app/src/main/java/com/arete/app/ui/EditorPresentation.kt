package com.arete.app.ui

import com.arete.app.data.local.PrescriptionSetEntity
import com.arete.app.data.local.SetType

/** Reorders the unsaved presentation draft; persistence still uses savePrescription. */
fun <T> moveEditorItem(items: MutableList<T>, from: Int, delta: Int) {
    if (from !in items.indices) return
    val to = (from + delta).coerceIn(0, items.lastIndex)
    if (from != to) items.add(to, items.removeAt(from))
}

fun templateSetSummary(sets: List<PrescriptionSetEntity>): String {
    val groups = mutableListOf<Pair<String, Int>>()
    sets.forEach { set ->
        val reps = if (set.repsMin == set.repsMax) "${set.repsMin}" else "${set.repsMin}–${set.repsMax}"
        val label = "$reps reps" + if (set.type == SetType.WARMUP) " calent." else ""
        val previous = groups.lastOrNull()
        if (previous?.first == label) groups[groups.lastIndex] = label to previous.second + 1
        else groups += label to 1
    }
    return groups.joinToString(" · ") { (label, count) -> "$count × $label" }.ifBlank { "Sin series" }
}
