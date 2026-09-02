@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.local.LastPerformanceRow
import com.arete.app.data.local.SessionExerciseWithSets
import com.arete.app.data.local.SessionSetEntity
import com.arete.app.data.local.SessionStatus
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun SessionScreen(
    sessionId: String,
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    onSubstitute: (String) -> Unit,
    onFinished: () -> Unit,
    onDeleted: () -> Unit,
    onRoutineSaved: (String) -> Unit
) {
    val sessionFlow = remember(sessionId) { viewModel.observeSession(sessionId) }
    val data by sessionFlow.collectAsStateWithLifecycle(initialValue = null)
    val details = data?.session
    val session = details?.session
    val exercises = details?.orderedExercises().orEmpty()
    var finishDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var saveRoutineDialog by remember { mutableStateOf(false) }
    var notes by remember(sessionId) { mutableStateOf<String?>(null) }

    LaunchedEffect(session?.notes) {
        if (notes == null && session != null) notes = session.notes
    }

    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cargando sesión…") }
        return
    }
    val active = session.status == SessionStatus.IN_PROGRESS

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session.workoutNameSnapshot)
                        Text(
                            if (active) "En curso · guardado automático" else "Sesión finalizada · editable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Acciones") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (active) DropdownMenuItem(
                                text = { Text("Finalizar entrenamiento") },
                                onClick = { menu = false; finishDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text(if (active) "Cancelar y eliminar" else "Eliminar sesión") },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                                onClick = { menu = false; deleteDialog = true }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExercise,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Ejercicio") }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            session.restTimerEndAt?.let { endAt ->
                item(key = "timer") {
                    RestTimerBanner(
                        endAt = endAt,
                        onAdjust = { viewModel.adjustRestTimer(session, it) },
                        onSkip = { viewModel.skipRestTimer(session) }
                    )
                }
            }
            item(key = "session-notes") {
                OutlinedTextField(
                    value = notes.orEmpty(),
                    onValueChange = {
                        notes = it
                        viewModel.updateSessionNotes(session, it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas de la sesión") },
                    minLines = 2
                )
            }
            if (exercises.isEmpty()) {
                item {
                    EmptyState(
                        "Añade el primer ejercicio",
                        "El entrenamiento libre y las sesiones dinámicas usan exactamente el mismo registro.",
                        "Añadir ejercicio",
                        onAddExercise
                    )
                }
            }
            itemsIndexed(exercises, key = { _, item -> item.exercise.id }) { index, exercise ->
                val group = exercise.exercise.groupIdSnapshot
                if (group != null && (index == 0 || exercises[index - 1].exercise.groupIdSnapshot != group)) {
                    Text(
                        "Grupo $group · ${exercises.count { it.exercise.groupIdSnapshot == group }} ejercicios",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                SessionExerciseCard(
                    details = exercise,
                    lastPerformance = exercise.exercise.exerciseId?.let { data?.lastPerformance?.get(it) }.orEmpty(),
                    index = index,
                    total = exercises.size,
                    viewModel = viewModel,
                    onSubstitute = { onSubstitute(exercise.exercise.id) },
                    onAddReplacement = onAddExercise
                )
            }
            if (!active && session.sourceWorkoutId == null) {
                item {
                    OutlinedButton(onClick = { saveRoutineDialog = true }, Modifier.fillMaxWidth()) {
                        Text("Guardar como rutina")
                    }
                }
            }
        }
    }

    if (finishDialog) AlertDialog(
        onDismissRequest = { finishDialog = false },
        title = { Text("Finalizar entrenamiento") },
        text = {
            val completed = exercises.sumOf { it.sets.count(SessionSetEntity::isCompleted) }
            Text("Se guardará la sesión con $completed series completadas. Podrás corregirla después desde Historial.")
        },
        confirmButton = {
            Button(onClick = {
                finishDialog = false
                viewModel.finishSession(session, onFinished)
            }) { Text("Finalizar") }
        },
        dismissButton = { TextButton(onClick = { finishDialog = false }) { Text("Seguir entrenando") } }
    )

    if (deleteDialog) ConfirmDialog(
        title = if (active) "Cancelar entrenamiento" else "Eliminar sesión",
        message = "Se eliminarán definitivamente la sesión y todas sus series.",
        onDismiss = { deleteDialog = false }
    ) {
        deleteDialog = false
        viewModel.deleteSession(session, onDeleted)
    }

    if (saveRoutineDialog) NameDialog(
        title = "Guardar como rutina",
        initialValue = session.workoutNameSnapshot,
        onDismiss = { saveRoutineDialog = false }
    ) { name ->
        saveRoutineDialog = false
        viewModel.saveSessionAsRoutine(session.id, name, onRoutineSaved)
    }
}

@Composable
private fun RestTimerBanner(
    endAt: Long,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var remaining by remember(endAt) { mutableLongStateOf((endAt - System.currentTimeMillis()).coerceAtLeast(0)) }
    LaunchedEffect(endAt) {
        do {
            remaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0)
            if (remaining > 0) delay(250)
        } while (remaining > 0)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onSkip()
    }
    val totalSeconds = ceil(remaining / 1_000.0).toInt()
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Timer, null)
            Text(
                "%d:%02d".format(totalSeconds / 60, totalSeconds % 60),
                Modifier.padding(horizontal = 12.dp).weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { onAdjust(-15) }) { Text("−15") }
            TextButton(onClick = { onAdjust(15) }) { Text("+15") }
            TextButton(onClick = onSkip) { Text("Saltar") }
        }
    }
}

@Composable
private fun SessionExerciseCard(
    details: SessionExerciseWithSets,
    lastPerformance: List<LastPerformanceRow>,
    index: Int,
    total: Int,
    viewModel: AreteViewModel,
    onSubstitute: () -> Unit,
    onAddReplacement: () -> Unit
) {
    val exercise = details.exercise
    var menu by remember { mutableStateOf(false) }
    var sessionNotes by remember(exercise.id) { mutableStateOf(exercise.sessionNotes) }
    val grouped = exercise.groupIdSnapshot != null
    val hasCompletedSets = details.sets.any { it.isCompleted }
    Card(
        Modifier.fillMaxWidth(),
        colors = if (grouped) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.exerciseNameEsSnapshot, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (exercise.exerciseNameEnSnapshot.isNotBlank()) Text(
                        exercise.exerciseNameEnSnapshot,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Descanso ${exercise.restSecondsSnapshot}s" + (exercise.groupIdSnapshot?.let { " · Grupo $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Acciones del ejercicio") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(if (hasCompletedSets) "No se puede sustituir: hay series hechas" else "Sustituir solo esta sesión") },
                            enabled = !hasCompletedSets,
                            onClick = { menu = false; onSubstitute() }
                        )
                        if (hasCompletedSets) DropdownMenuItem(
                            text = { Text("Añadir sustituto como ejercicio nuevo") },
                            onClick = { menu = false; onAddReplacement() }
                        )
                        DropdownMenuItem(
                            text = { Text("Subir") },
                            enabled = index > 0,
                            onClick = { menu = false; viewModel.moveSessionExercise(exercise.sessionId, exercise.id, -1) }
                        )
                        DropdownMenuItem(
                            text = { Text("Bajar") },
                            enabled = index < total - 1,
                            onClick = { menu = false; viewModel.moveSessionExercise(exercise.sessionId, exercise.id, 1) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar del entrenamiento") },
                            onClick = { menu = false; viewModel.deleteSessionExercise(exercise) }
                        )
                    }
                }
            }
            if (exercise.notesSnapshot.isNotBlank()) Text(
                exercise.notesSnapshot,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatLastPerformance(lastPerformance),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            OutlinedTextField(
                value = sessionNotes,
                onValueChange = {
                    sessionNotes = it
                    viewModel.updateSessionExerciseNotes(exercise, it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nota de hoy") },
                singleLine = true
            )
            details.orderedSets().forEachIndexed { setIndex, set ->
                SessionSetRow(setIndex + 1, set, viewModel)
            }
            TextButton(onClick = { viewModel.addSessionSet(exercise.id) }) {
                Icon(Icons.Default.Add, null)
                Text("Añadir serie")
            }
        }
    }
}

@Composable
private fun SessionSetRow(number: Int, set: SessionSetEntity, viewModel: AreteViewModel) {
    val focus = LocalFocusManager.current
    var weight by remember(set.id) { mutableStateOf(formatEditableWeight(set.weightKg)) }
    var reps by remember(set.id) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var rir by remember(set.id) { mutableStateOf(set.rir) }
    var isFailure by remember(set.id) { mutableStateOf(set.isFailure) }
    var isWarmup by remember(set.id) { mutableStateOf(set.isWarmupSnapshot) }
    var rirMenu by remember { mutableStateOf(false) }

    fun currentSet(): SessionSetEntity = set.copy(
        weightKg = weight.replace(',', '.').toDoubleOrNull(),
        reps = reps.toIntOrNull(),
        rir = rir,
        isFailure = isFailure,
        isWarmupSnapshot = isWarmup
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$number", Modifier.widthIn(min = 24.dp), fontWeight = FontWeight.Bold)
            FilterChip(
                selected = isWarmup,
                onClick = {
                    isWarmup = !isWarmup
                    viewModel.updateSessionSet(currentSet())
                },
                label = { Text(if (isWarmup) "Calent." else "Trabajo") }
            )
            Text(
                targetLabel(set),
                Modifier.padding(start = 8.dp).weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = { viewModel.deleteSessionSet(set) }) {
                Icon(Icons.Default.DeleteOutline, "Eliminar serie")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = weight,
                onValueChange = { value ->
                    weight = value.filter { it.isDigit() || it == ',' || it == '.' }
                    viewModel.updateSessionSet(currentSet())
                },
                modifier = Modifier.weight(1.15f),
                label = { Text("kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
            )
            OutlinedTextField(
                value = reps,
                onValueChange = { value ->
                    reps = value.filter(Char::isDigit)
                    viewModel.updateSessionSet(currentSet())
                },
                modifier = Modifier.weight(0.9f),
                label = { Text("reps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) })
            )
            Box {
                OutlinedButton(onClick = { rirMenu = true }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text(rirLabel(rir, isFailure))
                }
                DropdownMenu(expanded = rirMenu, onDismissRequest = { rirMenu = false }) {
                    DropdownMenuItem(text = { Text("Sin RIR") }, onClick = {
                        rirMenu = false
                        rir = null
                        isFailure = false
                        viewModel.updateSessionSet(currentSet())
                    })
                    listOf(5, 4, 3, 2, 1, 0).forEach { value ->
                        DropdownMenuItem(text = { Text(if (value == 5) "RIR 5+" else "RIR $value") }, onClick = {
                            rirMenu = false
                            rir = value
                            isFailure = false
                            viewModel.updateSessionSet(currentSet())
                        })
                    }
                    DropdownMenuItem(text = { Text("Fallo") }, onClick = {
                        rirMenu = false
                        rir = null
                        isFailure = true
                        viewModel.updateSessionSet(currentSet())
                    })
                }
            }
            IconButton(onClick = { viewModel.toggleSessionSetComplete(currentSet()) }) {
                Icon(
                    if (set.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    if (set.isCompleted) "Marcar no completada" else "Completar serie",
                    tint = if (set.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun targetLabel(set: SessionSetEntity): String =
    if (set.targetRepsMinSnapshot == set.targetRepsMaxSnapshot) "Objetivo ${set.targetRepsMinSnapshot}"
    else "Objetivo ${set.targetRepsMinSnapshot}-${set.targetRepsMaxSnapshot}"

private fun rirLabel(rir: Int?, isFailure: Boolean): String = when {
    isFailure -> "Fallo"
    rir == 5 -> "5+"
    rir != null -> "RIR $rir"
    else -> "RIR"
}

private fun formatEditableWeight(value: Double?): String = value?.let {
    if (it % 1.0 == 0.0) it.toInt().toString() else it.toString().replace('.', ',')
}.orEmpty()

private fun formatLastPerformance(rows: List<LastPerformanceRow>): String {
    if (rows.isEmpty()) return "Última vez: —"
    val workRows = rows.filterNot { it.isWarmupSnapshot }.ifEmpty { rows }
    return "Última vez: " + workRows.joinToString(" · ") { row ->
        val weight = row.weightKg?.let(::formatEditableWeight) ?: "—"
        val reps = row.reps?.toString() ?: "—"
        "$weight×$reps${if (row.isFailure) " F" else ""}"
    }
}
