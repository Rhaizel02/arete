package com.arete.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Routines : AppScreen
    data object Catalog : AppScreen
    data class RoutineEditor(val routineId: String) : AppScreen
    data class WorkoutEditor(val routineId: String, val workoutId: String) : AppScreen
    data class ExercisePicker(val routineId: String, val workoutId: String) : AppScreen
    data class Prescription(val routineId: String, val workoutId: String, val itemId: String) : AppScreen
    data class CustomExercise(
        val routineId: String? = null,
        val workoutId: String? = null,
        val exerciseId: String? = null
    ) : AppScreen
}

@Composable
fun AreteApp(viewModel: AreteViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val stack = remember { mutableStateListOf<AppScreen>(AppScreen.Routines) }
    var exportPayload by remember { mutableStateOf<String?>(null) }

    fun push(screen: AppScreen) { stack += screen }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replaceWith(screen: AppScreen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack += screen
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                .onSuccess { raw ->
                    if (raw != null) viewModel.importRoutine(raw) { id -> push(AppScreen.RoutineEditor(id)) }
                }
                .onFailure { scope.launch { snackbar.showSnackbar("No se pudo leer el archivo") } }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = exportPayload
        if (uri != null && content != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) } }
                .onSuccess { scope.launch { snackbar.showSnackbar("Rutina exportada") } }
                .onFailure { scope.launch { snackbar.showSnackbar("No se pudo guardar el archivo") } }
        }
        exportPayload = null
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    BackHandler(enabled = stack.size > 1, onBack = ::back)

    Box(Modifier.fillMaxSize()) {
        when (val screen = stack.last()) {
            AppScreen.Routines -> RoutineListScreen(
                viewModel = viewModel,
                onOpenRoutine = { push(AppScreen.RoutineEditor(it)) },
                onOpenCatalog = { push(AppScreen.Catalog) },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                onExport = { routine ->
                    viewModel.exportRoutine(routine.id) { content ->
                        exportPayload = content
                        val safeName = routine.name.lowercase().replace(Regex("[^a-z0-9áéíóúüñ]+"), "-").trim('-')
                        exportLauncher.launch("${safeName.ifBlank { "rutina" }}.arete.json")
                    }
                }
            )
            AppScreen.Catalog -> CatalogScreen(
                viewModel = viewModel,
                onBack = ::back,
                onCreateCustom = { push(AppScreen.CustomExercise()) },
                onEditCustom = { push(AppScreen.CustomExercise(exerciseId = it)) }
            )
            is AppScreen.RoutineEditor -> RoutineEditorScreen(
                routineId = screen.routineId,
                viewModel = viewModel,
                onBack = ::back,
                onOpenWorkout = { push(AppScreen.WorkoutEditor(screen.routineId, it)) }
            )
            is AppScreen.WorkoutEditor -> WorkoutEditorScreen(
                routineId = screen.routineId,
                workoutId = screen.workoutId,
                viewModel = viewModel,
                onBack = ::back,
                onAddExercise = { push(AppScreen.ExercisePicker(screen.routineId, screen.workoutId)) },
                onConfigure = { push(AppScreen.Prescription(screen.routineId, screen.workoutId, it)) }
            )
            is AppScreen.ExercisePicker -> ExercisePickerScreen(
                viewModel = viewModel,
                onBack = ::back,
                onSelect = { exerciseId ->
                    viewModel.addExercise(screen.workoutId, exerciseId) { itemId ->
                        replaceWith(AppScreen.Prescription(screen.routineId, screen.workoutId, itemId))
                    }
                },
                onCreateCustom = {
                    push(AppScreen.CustomExercise(screen.routineId, screen.workoutId))
                }
            )
            is AppScreen.Prescription -> PrescriptionScreen(
                routineId = screen.routineId,
                itemId = screen.itemId,
                viewModel = viewModel,
                onBack = ::back
            )
            is AppScreen.CustomExercise -> CustomExerciseScreen(
                exerciseId = screen.exerciseId,
                viewModel = viewModel,
                onBack = ::back,
                onSaved = { exerciseId ->
                    val workoutId = screen.workoutId
                    val routineId = screen.routineId
                    if (workoutId != null && routineId != null) {
                        viewModel.addExercise(workoutId, exerciseId) { itemId ->
                            while (stack.size > 1 && stack.last() !is AppScreen.WorkoutEditor) back()
                            push(AppScreen.Prescription(routineId, workoutId, itemId))
                        }
                    } else {
                        back()
                    }
                }
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

