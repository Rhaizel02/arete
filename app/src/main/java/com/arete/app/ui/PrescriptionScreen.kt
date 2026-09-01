@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
        topBar = {
            TopAppBar(
                title = { Text(details?.exercise?.nameEs ?: "Configurar ejercicio") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = { TextButton(onClick = ::save) { Text("Guardar") } }
            )
        }
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
                    onDelete = { if (sets.size > 1) sets.removeAt(index) }
                )
            }
            item {
                Button(onClick = { sets += (sets.lastOrNull() ?: SetEditorState()) }) {
                    Icon(Icons.Default.Add, null)
                    Text("Añadir serie", Modifier.padding(start = 8.dp))
                }
            }
            item { Text("Descanso y progresión", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        rest, { rest = it.filter(Char::isDigit) }, Modifier.weight(1f),
                        label = { Text("Descanso (s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    OutlinedTextField(
                        increment, { increment = it }, Modifier.weight(1f),
                        label = { Text("Incremento kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
                    )
                }
            }
            item { OutlinedTextField(groupId, { groupId = it }, Modifier.fillMaxWidth(), label = { Text("Grupo (p. ej. A1, circuito-1)") }, singleLine = true) }
            item { OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notas") }, minLines = 3) }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

@Composable
private fun SetEditorCard(
    number: Int,
    state: SetEditorState,
    onChange: (SetEditorState) -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Serie $number", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Eliminar serie") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == SetType.WARMUP,
                    onClick = { onChange(state.copy(type = SetType.WARMUP)) },
                    label = { Text("Calentamiento") }
                )
                FilterChip(
                    selected = state.type == SetType.WORK,
                    onClick = { onChange(state.copy(type = SetType.WORK)) },
                    label = { Text("Trabajo") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.isRange,
                    onClick = { onChange(state.copy(isRange = false, max = state.min)) },
                    label = { Text("Reps exactas") }
                )
                FilterChip(
                    selected = state.isRange,
                    onClick = { onChange(state.copy(isRange = true)) },
                    label = { Text("Rango") }
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    state.min,
                    { onChange(state.copy(min = it.filter(Char::isDigit), max = if (state.isRange) state.max else it.filter(Char::isDigit))) },
                    Modifier.weight(1f),
                    label = { Text(if (state.isRange) "Mínimo" else "Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                if (state.isRange) OutlinedTextField(
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
