@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.Surface
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.SetDraft
import com.arete.app.data.local.SetType

private data class SetEditorState(
    val type: SetType = SetType.WORK,
    val isRange: Boolean = true,
    val min: String = "8",
    val max: String = "12"
)

@Composable
fun PrescriptionScreen(
    routineId: String,
    itemId: String,
    viewModel: AreteViewModel,
    onBack: () -> Unit
) {
    val routineFlow = remember(routineId) { viewModel.observeRoutine(routineId) }
    val routine by routineFlow.collectAsStateWithLifecycle(initialValue = null)
    val details = routine?.workouts?.flatMap { it.exercises }?.firstOrNull { it.item.id == itemId }
    var rest by remember(itemId) { mutableStateOf("120") }
    var increment by remember(itemId) { mutableStateOf("") }
    var notes by remember(itemId) { mutableStateOf("") }
    var groupId by remember(itemId) { mutableStateOf("") }
    var error by remember(itemId) { mutableStateOf<String?>(null) }
    var initialized by remember(itemId) { mutableStateOf(false) }
    val sets = remember(itemId) { mutableStateListOf<SetEditorState>() }
    val focus = LocalFocusManager.current

    LaunchedEffect(details?.item?.id) {
        if (!initialized && details != null) {
            rest = details.item.restSeconds.toString()
            increment = details.item.incrementKg?.toString().orEmpty()
            notes = details.item.notes
            groupId = details.item.groupId.orEmpty()
            sets.clear()
            sets.addAll(details.orderedSets().map { set ->
                SetEditorState(
                    type = set.type,
                    isRange = set.repsMin != set.repsMax,
                    min = set.repsMin.toString(),
                    max = set.repsMax.toString()
                )
            })
            initialized = true
        }
    }

    fun save() {
        val item = details?.item ?: return
        val restValue = rest.toIntOrNull()
        val incrementValue = increment.replace(',', '.').toDoubleOrNull()
        val drafts = sets.mapNotNull { set ->
            val min = set.min.toIntOrNull() ?: return@mapNotNull null
            val max = if (set.isRange) set.max.toIntOrNull() ?: return@mapNotNull null else min
            if (min <= 0 || max < min) null else SetDraft(set.type, min, max)
        }
        error = when {
            restValue == null || restValue !in 0..3600 -> "Introduce un descanso entre 0 y 3600 segundos."
            increment.isNotBlank() && (incrementValue == null || incrementValue < 0) -> "El incremento de peso no es válido."
            sets.isEmpty() -> "Añade al menos una serie."
            drafts.size != sets.size -> "Revisa las repeticiones de cada serie."
            else -> null
        }
        if (error == null) viewModel.savePrescription(
            item, restValue!!, incrementValue, notes, groupId, drafts, onBack
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            ScreenHeader(details?.exercise?.nameEs ?: "Configurar ejercicio", onBack, "PRESCRIPCIÓN")
        },
        bottomBar = { Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                PrimaryAction(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text("Guardar configuración") }
            }
        } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Prescripción", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            itemsIndexed(sets, key = { index, _ -> index }) { index, set ->
                SetEditorCard(
                    number = index + 1,
                    state = set,
                    onChange = { sets[index] = it },
                    onDelete = { if (sets.size > 1) sets.removeAt(index) },
                    total = sets.size,
                    onMove = { delta -> focus.clearFocus(); moveEditorItem(sets, index, delta) }
                )
            }
            item {
                SecondaryAction(onClick = { sets += (sets.lastOrNull() ?: SetEditorState()) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Text("Añadir serie", Modifier.padding(start = 8.dp))
                }
            }
            item { Text("Descanso y progresión", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AreteField(
                        rest, { rest = it.filter(Char::isDigit) }, Modifier.weight(1f),
                        label = { Text("Descanso (s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    AreteField(
                        increment, { increment = it }, Modifier.weight(1f),
                        label = { Text("Incremento kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                    )
                }
            }
            item { AreteField(groupId, { groupId = it }, Modifier.fillMaxWidth(), label = { Text("Grupo (p. ej. A1, circuito-1)") }, singleLine = true) }
            item { AreteField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notas") }, minLines = 3) }
        }
    }
}

@Composable
private fun SetEditorCard(
    number: Int,
    state: SetEditorState,
    onChange: (SetEditorState) -> Unit,
    onDelete: () -> Unit,
    total: Int,
    onMove: (Int) -> Unit
) {
    var typeMenu by remember { mutableStateOf(false) }
    var rangeMenu by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("$number", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Box {
                    TextButton(onClick = { typeMenu = true }) { Text(if (state.type == SetType.WARMUP) "Calent. ▾" else "Trabajo ▾") }
                    DropdownMenu(typeMenu, { typeMenu = false }) {
                        SetType.entries.forEach { type -> DropdownMenuItem(text = { Text(if (type == SetType.WARMUP) "Calentamiento" else "Trabajo") }, onClick = { typeMenu = false; onChange(state.copy(type = type)) }) }
                    }
                }
                Box {
                    TextButton(onClick = { rangeMenu = true }) { Text(if (state.isRange) "Rango ▾" else "Exactas ▾") }
                    DropdownMenu(rangeMenu, { rangeMenu = false }) {
                        DropdownMenuItem(text = { Text("Reps exactas") }, onClick = { rangeMenu = false; onChange(state.copy(isRange = false, max = state.min)) })
                        DropdownMenuItem(text = { Text("Rango") }, onClick = { rangeMenu = false; onChange(state.copy(isRange = true)) })
                    }
                }
                ReorderActions(number > 1, number < total, { onMove(-1) }, { onMove(1) }, onDelete = onDelete)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AreteField(
                    state.min,
                    { onChange(state.copy(min = it.filter(Char::isDigit), max = if (state.isRange) state.max else it.filter(Char::isDigit))) },
                    Modifier.weight(1f),
                    label = { Text(if (state.isRange) "Mínimo" else "Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (state.isRange) AreteField(
                    state.max,
                    { onChange(state.copy(max = it.filter(Char::isDigit))) },
                    Modifier.weight(1f),
                    label = { Text("Máximo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }
    }
}
