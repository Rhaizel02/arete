@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arete.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.local.*
import com.arete.app.data.ProgressMetrics
import com.arete.app.data.ProgressSet
import com.arete.app.data.PrType
import com.arete.app.ui.theme.ActionBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

@Composable
fun SessionScreen(sessionId: String, viewModel: AreteViewModel, onBack: () -> Unit, onAddExercise: () -> Unit,
    onSubstitute: (String) -> Unit, onFinished: () -> Unit, onDeleted: () -> Unit, onRoutineSaved: (String) -> Unit) {
    val flow = remember(sessionId) { viewModel.observeSession(sessionId) }
    val data by flow.collectAsStateWithLifecycle(initialValue = null)
    val vibration by viewModel.vibrationEnabled.collectAsStateWithLifecycle()
    val confirmed by viewModel.progressState.collectAsStateWithLifecycle()
    val details = data?.session
    val session = details?.session
    val exercises = details?.orderedExercises().orEmpty()
    var selectedId by rememberSaveable(sessionId) { mutableStateOf(viewModel.sessionSelections[sessionId]?.first) }
    var previousIndex by rememberSaveable(sessionId) { mutableIntStateOf(viewModel.sessionSelections[sessionId]?.second ?: 0) }
    val actualId = resolveSelectedExercise(exercises, selectedId, previousIndex)
    val index = exercises.indexOfFirst { it.exercise.id == actualId }.coerceAtLeast(0)
    LaunchedEffect(exercises.map { it.exercise.id }) {
        if (exercises.isNotEmpty()) { selectedId = actualId; previousIndex = index; viewModel.sessionSelections[sessionId] = actualId!! to index }
    }
    var menu by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var saveRoutine by remember { mutableStateOf(false) }
    var editNotes by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    fun select(target: Int) {
        focus.clearFocus()
        selectedId = exercises[target].exercise.id; previousIndex = target
        viewModel.sessionSelections[sessionId] = selectedId!! to target
        scope.launch { list.scrollToItem(0) }
    }
    if (session == null) { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { Text("Cargando sesión…") }; return }
    val active = session.status == SessionStatus.IN_PROGRESS
    val progress = sessionWorkProgress(exercises)
    Scaffold(modifier = Modifier.imePadding(), topBar = {
        TopAppBar(title = { Column {
            Text(if (active) "Sesión activa" else "Sesión finalizada", style = MaterialTheme.typography.titleLarge)
            Text(session.workoutNameSnapshot, style = MaterialTheme.typography.bodySmall)
        } }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }, actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Acciones de sesión") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Añadir ejercicio") }, onClick = { menu = false; onAddExercise() })
                DropdownMenuItem(text = { Text("Notas de la sesión") }, onClick = { menu = false; editNotes = true })
                if (active) DropdownMenuItem(text = { Text("Finalizar entrenamiento") }, onClick = { menu = false; finish = true })
                if (!active && session.sourceWorkoutId == null) DropdownMenuItem(text = { Text("Guardar como rutina") }, onClick = { menu = false; saveRoutine = true })
                DropdownMenuItem(text = { Text(if (active) "Cancelar y eliminar" else "Eliminar sesión") }, onClick = { menu = false; delete = true })
            }
        })
    }, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Row(Modifier.navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryAction(enabled = index > 0, onClick = { select(index - 1) }, modifier = Modifier.heightIn(min = 52.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Ejercicio anterior") }
                Button(onClick = {
                    if (index < exercises.lastIndex) select(index + 1) else if (active) finish = true else onBack()
                }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White)) {
                    Text(if (index < exercises.lastIndex) "Siguiente ejercicio" else if (active) "Finalizar entrenamiento" else "Volver")
                    Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = list, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${progress.completed} / ${progress.total} series de trabajo", style = MaterialTheme.typography.labelMedium)
                        Text("${(progress.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth(), color = ActionBlue, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                }
            }
            if (exercises.isEmpty()) item { EmptyState("Tu sesión empieza aquí", "Añade el primer ejercicio para registrar tus series.", "Añadir ejercicio", onAddExercise) }
            exercises.getOrNull(index)?.let { current ->
                item(key = current.exercise.id) {
                    SessionExerciseCard(current, current.exercise.exerciseId?.let { data?.lastPerformance?.get(it) }.orEmpty(), index, exercises.size, viewModel,
                        confirmed.confirmedSets, session,
                        { onSubstitute(current.exercise.id) }, onAddExercise)
                }
            }
            session.restTimerEndAt?.let { endAt -> item { RestTimerBanner(endAt, vibration, { viewModel.adjustRestTimer(session, it) }, { viewModel.skipRestTimer(session) }) } }
            if (exercises.size > 1) item { SectionHeading("Ejercicios de la sesión") }
            exercises.forEachIndexed { position, item -> if (position != index) item(key = item.exercise.id) {
                TonalCard(onClick = { select(position) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${position + 1}", Modifier.padding(end = 16.dp), color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(item.exercise.exerciseNameEsSnapshot, style = MaterialTheme.typography.titleMedium)
                            Text("${item.sets.count { it.isCompleted }} / ${item.sets.size} series" + (item.exercise.groupIdSnapshot?.let { " · Grupo $it" } ?: ""), style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            } }
            item { OutlinedButton(onClick = onAddExercise, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Añadir ejercicio") } }
        }
    }
    if (finish) AlertDialog(onDismissRequest = { finish = false }, title = { Text("Finalizar entrenamiento") }, text = { Text("Se guardarán ${progress.completed} series de trabajo completadas. Podrás corregir la sesión desde Historial.") }, confirmButton = { Button(onClick = { finish = false; viewModel.finishSession(session, onFinished) }) { Text("Finalizar") } }, dismissButton = { TextButton(onClick = { finish = false }) { Text("Seguir entrenando") } })
    if (delete) ConfirmDialog("Eliminar sesión", "Se eliminarán la sesión y sus series.", { delete = false }) { delete = false; viewModel.deleteSession(session, onDeleted) }
    if (saveRoutine) NameDialog("Guardar como rutina", session.workoutNameSnapshot, onDismiss = { saveRoutine = false }) { name -> saveRoutine = false; viewModel.saveSessionAsRoutine(session.id, name, onRoutineSaved) }
    if (editNotes) NotesDialog("Notas de la sesión", session.notes, { editNotes = false }) { viewModel.updateSessionNotes(session, it) }
}

@Composable
private fun SessionExerciseCard(details: SessionExerciseWithSets, last: List<LastPerformanceRow>, index: Int, total: Int,
    viewModel: AreteViewModel, confirmed: List<ProgressSet>, session: WorkoutSessionEntity,
    onSubstitute: () -> Unit, onAddReplacement: () -> Unit) {
    val exercise = details.exercise
    var menu by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val hasCompleted = details.sets.any { it.isCompleted }
    val improvements = remember(details, confirmed, session.status) {
        val history = confirmed.filter { it.exerciseId == exercise.exerciseId && it.sessionId != session.id }
        details.sets.associate { set ->
            val candidate = ProgressSet(session.id, session.startedAt, session.completedAt ?: System.currentTimeMillis(),
                exercise.id, exercise.exerciseId, exercise.exerciseNameEsSnapshot, null, null, set.position,
                set.targetRepsMinSnapshot, set.targetRepsMaxSnapshot, set.isWarmupSnapshot, set.isCompleted,
                set.weightKg, set.reps, set.rir, set.isFailure)
            set.id to ProgressMetrics.improvements(candidate, history)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TonalCard {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${index + 1} DE $total" + (exercise.groupIdSnapshot?.let { " · GRUPO $it" } ?: ""), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(exercise.exerciseNameEsSnapshot, style = MaterialTheme.typography.headlineSmall)
                    Text("${details.sets.count { !it.isWarmupSnapshot }} series de trabajo · ${exercise.restSecondsSnapshot}s descanso", style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Acciones del ejercicio") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Notas del ejercicio") }, onClick = { menu = false; notes = true })
                        DropdownMenuItem(text = { Text("Subir") }, enabled = index > 0, onClick = { menu = false; viewModel.moveSessionExercise(exercise.sessionId, exercise.id, -1) })
                        DropdownMenuItem(text = { Text("Bajar") }, enabled = index < total - 1, onClick = { menu = false; viewModel.moveSessionExercise(exercise.sessionId, exercise.id, 1) })
                        DropdownMenuItem(text = { Text("Eliminar ejercicio") }, onClick = { menu = false; deleting = true })
                    }
                }
            }
            if (exercise.notesSnapshot.isNotBlank()) Text(exercise.notesSnapshot, style = MaterialTheme.typography.bodySmall)
            if (exercise.sessionNotes.isNotBlank()) Text(exercise.sessionNotes, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { guide = true }, contentPadding = PaddingValues(0.dp)) { Icon(Icons.AutoMirrored.Filled.MenuBook, null); Spacer(Modifier.width(8.dp)); Text("Guía técnica") }
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.small) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("ÚLTIMA SESIÓN" + (last.firstOrNull()?.let { " · ${uiDate(it.completedAt)}" } ?: ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val work = last.filterNot { it.isWarmupSnapshot }
                    Text(if (work.isEmpty()) "Sin rendimiento anterior" else work.joinToString(" · ") { "${it.weightKg?.let(::uiNumber) ?: "—"} kg × ${it.reps ?: "—"} reps${if (it.isFailure) " · fallo" else ""}" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SERIE", Modifier.width(48.dp), style = MaterialTheme.typography.labelSmall)
            listOf("KG", "REPS", "RIR").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall) }
            Text("LISTO", Modifier.width(48.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
        }
        details.orderedSets().forEachIndexed { number, set -> key(set.id) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SessionSetRow(number + 1, details.sets.size, set, viewModel)
                improvements[set.id]?.takeIf { it.isNotEmpty() }?.let { types ->
                    Text((if (session.status == SessionStatus.IN_PROGRESS) "↑ Provisional" else "↑ Mejora confirmada") + " · " + types.joinToString(" · ") {
                        when(it) { PrType.MAX_WEIGHT -> "más peso"; PrType.MAX_E1RM -> "mayor e1RM"; PrType.REPS_AT_WEIGHT -> "más reps al mismo peso" }
                    }, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { viewModel.addSessionSet(exercise.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Add, null); Text("Añadir serie") }
            SecondaryAction(onClick = if (hasCompleted) onAddReplacement else onSubstitute, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(if (hasCompleted) "Añadir sustituto" else "Sustituir") }
        }
        if (hasCompleted) Text("Las series completadas conservan su ejercicio original.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (notes) NotesDialog("Notas del ejercicio", exercise.sessionNotes, { notes = false }) { viewModel.updateSessionExerciseNotes(exercise, it) }
    if (guide) ExerciseGuideSheet(exercise.exerciseId, viewModel) { guide = false }
    if (deleting) ConfirmDialog("Eliminar ejercicio", "Se eliminarán también sus series de esta sesión.", { deleting = false }) { deleting = false; viewModel.deleteSessionExercise(exercise) }
}

@Composable
private fun SessionSetRow(number: Int, total: Int, set: SessionSetEntity, viewModel: AreteViewModel) {
    var weight by remember(set.id) { mutableStateOf(set.weightKg?.let(::uiNumber).orEmpty()) }
    var reps by remember(set.id) { mutableStateOf(set.reps?.toString().orEmpty()) }
    var rir by remember(set.id) { mutableStateOf(set.rir) }
    var failure by remember(set.id) { mutableStateOf(set.isFailure) }
    var warmup by remember(set.id) { mutableStateOf(set.isWarmupSnapshot) }
    var menu by remember { mutableStateOf(false) }
    var rirMenu by remember { mutableStateOf(false) }
    fun current() = set.copy(weightKg = weight.replace(',', '.').toDoubleOrNull(), reps = reps.toIntOrNull(), rir = rir, isFailure = failure, isWarmupSnapshot = warmup)
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            TextButton(onClick = { menu = true }, modifier = Modifier.width(48.dp).semantics { contentDescription = "Opciones serie $number" }, contentPadding = PaddingValues(0.dp)) { Text("$number${if (warmup) " C" else ""}") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Objetivo ${set.targetRepsMinSnapshot}–${set.targetRepsMaxSnapshot} reps") }, enabled = false, onClick = {})
                DropdownMenuItem(text = { Text(if (warmup) "Marcar trabajo" else "Marcar calentamiento") }, onClick = { menu = false; warmup = !warmup; viewModel.updateSessionSet(current()) })
                DropdownMenuItem(text = { Text("Subir serie") }, enabled = number > 1, onClick = { menu = false; viewModel.moveSessionSet(set, -1) })
                DropdownMenuItem(text = { Text("Bajar serie") }, enabled = number < total, onClick = { menu = false; viewModel.moveSessionSet(set, 1) })
                DropdownMenuItem(text = { Text("Eliminar serie") }, onClick = { menu = false; viewModel.deleteSessionSet(set) })
            }
        }
        NumericCell(weight, "Peso serie $number, kg", Modifier.weight(1f), KeyboardType.Decimal) { weight = it.filter { char -> char.isDigit() || char == ',' || char == '.' }; viewModel.updateSessionSet(current()) }
        NumericCell(reps, "Repeticiones serie $number", Modifier.weight(1f), KeyboardType.Number) { reps = it.filter(Char::isDigit); viewModel.updateSessionSet(current()) }
        Box(Modifier.weight(1f)) {
            TextButton(onClick = { rirMenu = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small).semantics { contentDescription = "RIR serie $number" }, contentPadding = PaddingValues(0.dp)) { Text(if (failure) "F" else rir?.let { if (it == 5) "5+" else "$it" } ?: "—") }
            DropdownMenu(rirMenu, { rirMenu = false }) {
                DropdownMenuItem(text = { Text("Sin RIR") }, onClick = { rirMenu = false; rir = null; failure = false; viewModel.updateSessionSet(current()) })
                (5 downTo 0).forEach { value -> DropdownMenuItem(text = { Text(if (value == 5) "RIR 5+" else "RIR $value") }, onClick = { rirMenu = false; rir = value; failure = false; viewModel.updateSessionSet(current()) }) }
                DropdownMenuItem(text = { Text("Fallo") }, onClick = { rirMenu = false; rir = null; failure = true; viewModel.updateSessionSet(current()) })
            }
        }
        IconButton(onClick = { viewModel.toggleSessionSetComplete(current()) }, modifier = Modifier.size(48.dp).background(if (set.isCompleted) ActionBlue else MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))) {
            Icon(if (set.isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked, if (set.isCompleted) "Desmarcar serie $number" else "Completar serie $number", tint = if (set.isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NumericCell(value: String, label: String, modifier: Modifier, keyboard: KeyboardType, onChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    BasicTextField(value, onChange, modifier.heightIn(min = 48.dp).background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small).semantics { contentDescription = label },
        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center), singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }),
        decorationBox = { inner -> Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) { inner() } })
}

@Composable
private fun RestTimerBanner(endAt: Long, vibration: Boolean, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var remaining by remember(endAt) { mutableLongStateOf((endAt - System.currentTimeMillis()).coerceAtLeast(0)) }
    LaunchedEffect(endAt) {
        do { remaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0); if (remaining > 0) delay(250) } while (remaining > 0)
        if (vibration) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onSkip()
    }
    val seconds = ceil(remaining / 1000.0).toInt()
    TonalCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f).padding(start = 8.dp)) { Text("DESCANSO", style = MaterialTheme.typography.labelSmall); Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.headlineSmall) }
            TextButton(onClick = { onAdjust(-15) }, contentPadding = PaddingValues(4.dp)) { Text("−15s") }
            TextButton(onClick = { onAdjust(15) }, contentPadding = PaddingValues(4.dp)) { Text("+15s") }
            TextButton(onClick = onSkip, contentPadding = PaddingValues(4.dp)) { Text("Saltar") }
        }
    }
}

@Composable
private fun NotesDialog(title: String, initial: String, onDismiss: () -> Unit, onChange: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it; onChange(it) }, modifier = Modifier.fillMaxWidth(), label = { Text("Notas · guardado automático") }, minLines = 3) }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
