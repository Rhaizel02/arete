@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arete.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.RoutineExerciseEntity
import com.arete.app.data.local.WorkoutEntity

@Composable
fun RoutineListScreen(viewModel: AreteViewModel, onOpenRoutine: (String) -> Unit, onOpenCatalog: () -> Unit,
    onImport: () -> Unit, onExport: (RoutineEntity) -> Unit) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<RoutineEntity?>(null) }
    var deleting by remember { mutableStateOf<RoutineEntity?>(null) }
    Scaffold(topBar = { ScreenHeader("Rutinas", subtitle = "ARETE · PLANIFICA", actions = {
        IconButton(onClick = onOpenCatalog) { Icon(Icons.Default.FitnessCenter, "Catálogo") }
        IconButton(onClick = onImport) { Icon(Icons.Default.FileOpen, "Importar rutina") }
    }) }, bottomBar = { AddAction("Nueva rutina") { creating = true } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp)) {
            item {
                Text("Entrena con estructura", style = MaterialTheme.typography.titleLarge)
                Text("Activa una rutina y configura tu secuencia.", Modifier.padding(top = 4.dp, bottom = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (routines.isEmpty()) item { EmptyState("Tu entrenamiento empieza aquí", "Crea una rutina y organiza sus workouts.", "Crear rutina") { creating = true } }
            items(routines, key = { it.id }) { routine ->
                var menu by remember { mutableStateOf(false) }
                EditorialRow(onClick = { onOpenRoutine(routine.id) }) {
                    Icon(Icons.Default.FitnessCenter, null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(routine.name, style = MaterialTheme.typography.titleMedium)
                        if (routine.isActive) StatusLabel("Rutina activa", positive = true)
                        else Text("Toca para configurar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Acciones de rutina") }
                        DropdownMenu(menu, { menu = false }) {
                            if (!routine.isActive) DropdownMenuItem(text = { Text("Marcar como activa") }, onClick = { menu = false; viewModel.activateRoutine(routine.id) })
                            DropdownMenuItem(text = { Text("Cambiar nombre") }, onClick = { menu = false; rename = routine })
                            DropdownMenuItem(text = { Text("Exportar JSON") }, leadingIcon = { Icon(Icons.Default.IosShare, null) }, onClick = { menu = false; onExport(routine) })
                            DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; deleting = routine })
                        }
                    }
                }
            }
        }
    }
    if (creating) NameDialog("Nueva rutina", onDismiss = { creating = false }) { creating = false; viewModel.createRoutine(it, onOpenRoutine) }
    rename?.let { item -> NameDialog("Cambiar nombre", item.name, onDismiss = { rename = null }) { rename = null; viewModel.renameRoutine(item, it) } }
    deleting?.let { item -> ConfirmDialog("Eliminar ${item.name}", "Se eliminarán sus workouts y prescripciones. Esta acción no se puede deshacer.", { deleting = null }) { deleting = null; viewModel.deleteRoutine(item) } }
}

@Composable
fun RoutineEditorScreen(routineId: String, viewModel: AreteViewModel, onBack: () -> Unit, onOpenWorkout: (String) -> Unit) {
    val flow = remember(routineId) { viewModel.observeRoutine(routineId) }
    val routine by flow.collectAsStateWithLifecycle(initialValue = null)
    val workouts = routine?.orderedWorkouts().orEmpty()
    var adding by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<WorkoutEntity?>(null) }
    var deleting by remember { mutableStateOf<WorkoutEntity?>(null) }
    Scaffold(topBar = { ScreenHeader(routine?.routine?.name ?: "Rutina", onBack, "SECUENCIA DE ENTRENAMIENTO") },
        bottomBar = { AddAction("Añadir workout") { adding = true } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp)) {
            item { SectionHeading("Workouts") }
            if (workouts.isEmpty()) item { EmptyState("Rutina vacía", "Añade workouts y ordénalos según tu secuencia.", "Añadir workout") { adding = true } }
            itemsIndexed(workouts, key = { _, item -> item.workout.id }) { index, item ->
                val setCount = item.exercises.sumOf { it.sets.size }
                EditorialRow(onClick = { onOpenWorkout(item.workout.id) }) {
                    Text("%02d".format(index + 1), Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(item.workout.name, style = MaterialTheme.typography.titleMedium)
                        Text("${item.exercises.size} ${if (item.exercises.size == 1) "ejercicio" else "ejercicios"} · $setCount ${if (setCount == 1) "serie" else "series"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    ReorderActions(index > 0, index < workouts.lastIndex,
                        { viewModel.moveWorkout(routineId, item.workout.id, -1) }, { viewModel.moveWorkout(routineId, item.workout.id, 1) },
                        { rename = item.workout }, { deleting = item.workout })
                }
            }
        }
    }
    if (adding) NameDialog("Nuevo workout", label = "Nombre (p. ej. Torso A)", onDismiss = { adding = false }) { adding = false; viewModel.addWorkout(routineId, it, onOpenWorkout) }
    rename?.let { item -> NameDialog("Cambiar nombre", item.name, onDismiss = { rename = null }) { rename = null; viewModel.renameWorkout(item, it) } }
    deleting?.let { item -> ConfirmDialog("Eliminar workout", "Se eliminarán sus ejercicios y series.", { deleting = null }) { deleting = null; viewModel.deleteWorkout(item) } }
}

@Composable
fun WorkoutEditorScreen(routineId: String, workoutId: String, viewModel: AreteViewModel, onBack: () -> Unit,
    onAddExercise: () -> Unit, onConfigure: (String) -> Unit) {
    val flow = remember(routineId) { viewModel.observeRoutine(routineId) }
    val routine by flow.collectAsStateWithLifecycle(initialValue = null)
    val workout = routine?.orderedWorkouts()?.firstOrNull { it.workout.id == workoutId }
    val exercises = workout?.orderedExercises().orEmpty()
    val setCount = exercises.sumOf { it.sets.size }
    var deleting by remember { mutableStateOf<RoutineExerciseEntity?>(null) }
    Scaffold(topBar = { ScreenHeader(workout?.workout?.name ?: "Workout", onBack, "EJERCICIOS Y SERIES") },
        bottomBar = { AddAction("Añadir ejercicio", onAddExercise) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp)) {
            item { Text("${exercises.size} ${if (exercises.size == 1) "ejercicio" else "ejercicios"} · $setCount ${if (setCount == 1) "serie" else "series"} de plantilla", Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (exercises.isEmpty()) item { EmptyState("Sin ejercicios", "Busca en el catálogo o crea uno personalizado.", "Añadir ejercicio", onAddExercise) }
            itemsIndexed(exercises, key = { _, item -> item.item.id }) { index, details ->
                EditorialRow(onClick = { onConfigure(details.item.id) }) {
                    Text("${index + 1}", Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(details.exercise.nameEs, style = MaterialTheme.typography.titleMedium)
                        Text(templateSetSummary(details.orderedSets()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${details.item.restSeconds} s descanso" + (details.item.incrementKg?.let { " · +${uiNumber(it)} kg" } ?: ""), style = MaterialTheme.typography.labelSmall)
                        details.item.groupId?.let { StatusLabel("Grupo $it") }
                    }
                    ReorderActions(index > 0, index < exercises.lastIndex,
                        { viewModel.moveExercise(workoutId, details.item.id, -1) }, { viewModel.moveExercise(workoutId, details.item.id, 1) },
                        { onConfigure(details.item.id) }, { deleting = details.item })
                }
            }
        }
    }
    deleting?.let { item -> ConfirmDialog("Eliminar ejercicio", "Se eliminarán sus series de esta plantilla. El historial no cambia.", { deleting = null }) { deleting = null; viewModel.deleteExercise(item) } }
}
