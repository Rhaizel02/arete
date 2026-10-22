@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.ExerciseDraft
import com.arete.app.data.local.ExerciseEntity

@Composable
fun CatalogScreen(
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onCreateCustom: () -> Unit,
    onEditCustom: (String) -> Unit
) {
    ExerciseCatalogScaffold(
        title = "Catálogo de ejercicios",
        viewModel = viewModel,
        onBack = onBack,
        onCreateCustom = onCreateCustom,
        onExercise = {},
        onEditCustom = onEditCustom,
        selectionMode = false
    )
}

@Composable
fun ExercisePickerScreen(
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateCustom: () -> Unit,
    title: String = "Añadir ejercicio"
) {
    ExerciseCatalogScaffold(
        title = title,
        viewModel = viewModel,
        onBack = onBack,
        onCreateCustom = onCreateCustom,
        onExercise = onSelect,
        onEditCustom = {},
        selectionMode = true
    )
}

@Composable
private fun ExerciseCatalogScaffold(
    title: String,
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onCreateCustom: () -> Unit,
    onExercise: (String) -> Unit,
    onEditCustom: (String) -> Unit,
    selectionMode: Boolean
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val query by viewModel.exerciseQuery.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            ScreenHeader(title, onBack)
        },
        bottomBar = {
            AddAction("Personalizado", onCreateCustom)
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AreteField(
                value = query,
                onValueChange = { viewModel.exerciseQuery.value = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar en español o inglés") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState("Sin resultados", "Prueba otra búsqueda o crea un ejercicio personalizado.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            selectionMode = selectionMode,
                            onClick = { if (selectionMode) onExercise(exercise.id) else selectedId = exercise.id },
                            onEdit = { onEditCustom(exercise.id) }
                        )
                    }
                }
            }
        }
    }
    selectedId?.let { id -> ExerciseGuideSheet(id, viewModel, title = "Ficha del ejercicio", onEdit = { selectedId = null; onEditCustom(it) }) { selectedId = null } }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseEntity,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    EditorialRow(onClick = onClick) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(exercise.nameEs, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                if (exercise.nameEn.isNotBlank()) Text(
                    exercise.nameEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${exercise.primaryMuscle} · ${exercise.equipment}${if (exercise.isCustom) " · Personalizado" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (selectionMode) {
                Icon(Icons.Default.Add, "Añadir")
            } else if (exercise.isCustom) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar ejercicio") }
            } else Icon(Icons.Default.ChevronRight, "Ver ficha")
    }
}

@Composable
fun CustomExerciseScreen(
    exerciseId: String?,
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val existing = allExercises.firstOrNull { it.id == exerciseId }
    var nameEs by remember(exerciseId) { mutableStateOf("") }
    var nameEn by remember(exerciseId) { mutableStateOf("") }
    var muscle by remember(exerciseId) { mutableStateOf("") }
    var secondary by remember(exerciseId) { mutableStateOf("") }
    var equipment by remember(exerciseId) { mutableStateOf("") }
    var instructions by remember(exerciseId) { mutableStateOf("") }
    var initialized by remember(exerciseId) { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            nameEs = existing.nameEs
            nameEn = existing.nameEn
            muscle = existing.primaryMuscle
            secondary = existing.secondaryMuscles.joinToString(", ")
            equipment = existing.equipment
            instructions = existing.instructions
            initialized = true
        }
    }

    val valid = nameEs.isNotBlank() && muscle.isNotBlank() && equipment.isNotBlank()
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            ScreenHeader(if (exerciseId == null) "Nuevo ejercicio" else "Editar ejercicio", onBack, "PERSONALIZADO")
        },
        bottomBar = { Surface(color = MaterialTheme.colorScheme.background) {
            PrimaryAction(modifier = Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth(),
                        enabled = valid,
                        onClick = {
                            viewModel.saveCustomExercise(
                                existing,
                                ExerciseDraft(nameEs, nameEn, muscle, secondary, equipment, instructions),
                                onSaved
                            )
                        }
                    ) { Text("Guardar ejercicio") }
        } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeading("Identidad"); Text("* Obligatorio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { AreteField(nameEs, { nameEs = it }, Modifier.fillMaxWidth(), label = { Text("Nombre en español *") }, singleLine = true) }
            item { AreteField(nameEn, { nameEn = it }, Modifier.fillMaxWidth(), label = { Text("Nombre en inglés") }, singleLine = true) }
            item { SectionHeading("Clasificación") }
            item { AreteField(muscle, { muscle = it }, Modifier.fillMaxWidth(), label = { Text("Músculo principal *") }, singleLine = true) }
            item { AreteField(secondary, { secondary = it }, Modifier.fillMaxWidth(), label = { Text("Músculos secundarios, separados por coma") }) }
            item { AreteField(equipment, { equipment = it }, Modifier.fillMaxWidth(), label = { Text("Equipamiento *") }, singleLine = true) }
            item { SectionHeading("Técnica") }
            item { AreteField(instructions, { instructions = it }, Modifier.fillMaxWidth(), label = { Text("Instrucciones") }, minLines = 4) }
        }
    }
}
