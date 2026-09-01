@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.local.RoutineEntity
import com.arete.app.data.local.WorkoutEntity

@Composable
fun RoutineListScreen(
    viewModel: AreteViewModel,
    onOpenRoutine: (String) -> Unit,
    onOpenCatalog: () -> Unit,
    onImport: () -> Unit,
    onExport: (RoutineEntity) -> Unit
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    var createDialog by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<RoutineEntity?>(null) }
    var deleting by remember { mutableStateOf<RoutineEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Arete", fontWeight = FontWeight.Bold)
                        Text("Progreso con intención", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenCatalog) { Icon(Icons.Default.FitnessCenter, "Catálogo") }
                    IconButton(onClick = onImport) { Icon(Icons.Default.FileOpen, "Importar rutina") }
                    IconButton(onClick = viewModel::cycleTheme) { Icon(Icons.Default.DarkMode, "Cambiar tema") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { createDialog = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva rutina") }
            )
        }
    ) { padding ->
        if (routines.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    "Tu entrenamiento empieza aquí",
                    "Crea una rutina y organiza sus workouts en el orden que prefieras.",
                    "Crear rutina"
                ) { createDialog = true }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routines, key = { it.id }) { routine ->
                    RoutineCard(
                        routine = routine,
                        onOpen = { onOpenRoutine(routine.id) },
                        onActivate = { viewModel.activateRoutine(routine.id) },
                        onRename = { rename = routine },
                        onExport = { onExport(routine) },
                        onDelete = { deleting = routine }
                    )
                }
            }
        }
    }

    if (createDialog) NameDialog("Nueva rutina", onDismiss = { createDialog = false }) { name ->
        createDialog = false
        viewModel.createRoutine(name, onOpenRoutine)
    }
    rename?.let { routine ->
        NameDialog("Cambiar nombre", routine.name, onDismiss = { rename = null }) { name ->
            rename = null
            viewModel.renameRoutine(routine, name)
        }
    }
    deleting?.let { routine ->
        ConfirmDialog(
            "Eliminar ${routine.name}",
            "Se eliminarán sus workouts y prescripciones. Esta acción no se puede deshacer.",
            onDismiss = { deleting = null }
        ) {
            deleting = null
            viewModel.deleteRoutine(routine)
        }
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineEntity,
    onOpen: () -> Unit,
    onActivate: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(routine.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (routine.isActive) {
                    AssistChip(onClick = {}, label = { Text("Rutina activa") }, leadingIcon = {
                        Icon(Icons.Default.CheckCircle, null)
                    })
                } else {
                    Text("Lista para editar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más acciones") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (!routine.isActive) DropdownMenuItem(
                        text = { Text("Marcar como activa") },
                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, null) },
                        onClick = { menu = false; onActivate() }
                    )
                    DropdownMenuItem(text = { Text("Cambiar nombre") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(
                        text = { Text("Exportar JSON") },
                        leadingIcon = { Icon(Icons.Default.IosShare, null) },
                        onClick = { menu = false; onExport() }
                    )
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
fun RoutineEditorScreen(
    routineId: String,
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onOpenWorkout: (String) -> Unit
) {
    val routineFlow = remember(routineId) { viewModel.observeRoutine(routineId) }
    val routine by routineFlow.collectAsStateWithLifecycle(initialValue = null)
    val workouts = routine?.orderedWorkouts().orEmpty()
    var adding by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<WorkoutEntity?>(null) }
    var deleting by remember { mutableStateOf<WorkoutEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(routine?.routine?.name ?: "Rutina") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Añadir workout") }
            )
        }
    ) { padding ->
        if (workouts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Rutina vacía", "Añade workouts y ordénalos según tu secuencia de entrenamiento.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(workouts, key = { _, item -> item.workout.id }) { index, item ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenWorkout(item.workout.id) }) {
                        Column(Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
                            Text(item.workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${item.exercises.size} ejercicio${if (item.exercises.size == 1) "" else "s"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ReorderActions(
                                canMoveUp = index > 0,
                                canMoveDown = index < workouts.lastIndex,
                                onMoveUp = { viewModel.moveWorkout(routineId, item.workout.id, -1) },
                                onMoveDown = { viewModel.moveWorkout(routineId, item.workout.id, 1) },
                                onEdit = { rename = item.workout },
                                onDelete = { deleting = item.workout }
                            )
                        }
                    }
                }
            }
        }
    }

    if (adding) NameDialog("Nuevo workout", label = "Nombre (p. ej. Torso A)", onDismiss = { adding = false }) { name ->
        adding = false
        viewModel.addWorkout(routineId, name, onOpenWorkout)
    }
    rename?.let { workout ->
        NameDialog("Cambiar nombre", workout.name, onDismiss = { rename = null }) { name ->
            rename = null
            viewModel.renameWorkout(workout, name)
        }
    }
    deleting?.let { workout ->
        ConfirmDialog("Eliminar workout", "Se eliminarán sus ejercicios y series.", onDismiss = { deleting = null }) {
            deleting = null
            viewModel.deleteWorkout(workout)
        }
    }
}

@Composable
fun WorkoutEditorScreen(
    routineId: String,
    workoutId: String,
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    onConfigure: (String) -> Unit
) {
    val routineFlow = remember(routineId) { viewModel.observeRoutine(routineId) }
    val routine by routineFlow.collectAsStateWithLifecycle(initialValue = null)
    val workout = routine?.orderedWorkouts()?.firstOrNull { it.workout.id == workoutId }
    val exercises = workout?.orderedExercises().orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.workout?.name ?: "Workout") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExercise) { Icon(Icons.Default.Add, "Añadir ejercicio") }
        }
    ) { padding ->
        if (exercises.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Sin ejercicios", "Busca en el catálogo o crea un ejercicio personalizado.", "Añadir ejercicio", onAddExercise)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(exercises, key = { _, item -> item.item.id }) { index, details ->
                    val setSummary = details.orderedSets().joinToString(" · ") { set ->
                        val reps = if (set.repsMin == set.repsMax) "${set.repsMin}" else "${set.repsMin}-${set.repsMax}"
                        "${if (set.type.name == "WARMUP") "C" else "T"} $reps"
                    }
                    Card(Modifier.fillMaxWidth().clickable { onConfigure(details.item.id) }) {
                        Column(Modifier.padding(start = 18.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(details.exercise.nameEs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    if (details.exercise.nameEn.isNotBlank()) Text(
                                        details.exercise.nameEn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                details.item.groupId?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                            }
                            Text(setSummary.ifBlank { "Sin series" }, Modifier.padding(top = 8.dp))
                            Text(
                                "Descanso ${details.item.restSeconds}s${details.item.incrementKg?.let { " · +${it} kg" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ReorderActions(
                                canMoveUp = index > 0,
                                canMoveDown = index < exercises.lastIndex,
                                onMoveUp = { viewModel.moveExercise(workoutId, details.item.id, -1) },
                                onMoveDown = { viewModel.moveExercise(workoutId, details.item.id, 1) },
                                onEdit = { onConfigure(details.item.id) },
                                onDelete = { viewModel.deleteExercise(details.item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
