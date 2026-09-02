package com.arete.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Home : AppScreen
    data object Routines : AppScreen
    data object Catalog : AppScreen
    data object History : AppScreen
    data class Session(val sessionId: String) : AppScreen
    data class SessionExercisePicker(
        val sessionId: String,
        val replacingSessionExerciseId: String? = null
    ) : AppScreen
    data class RoutineEditor(val routineId: String) : AppScreen
    data class WorkoutEditor(val routineId: String, val workoutId: String) : AppScreen
    data class ExercisePicker(val routineId: String, val workoutId: String) : AppScreen
    data class Prescription(val routineId: String, val workoutId: String, val itemId: String) : AppScreen
    data class CustomExercise(
        val routineId: String? = null,
        val workoutId: String? = null,
        val exerciseId: String? = null,
        val sessionId: String? = null,
        val replacingSessionExerciseId: String? = null
    ) : AppScreen
}

@Composable
fun AreteApp(viewModel: AreteViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val stack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }
    var exportPayload by remember { mutableStateOf<String?>(null) }

    fun push(screen: AppScreen) { stack += screen }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replaceWith(screen: AppScreen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack += screen
    }
    fun selectRoot(screen: AppScreen) {
        stack.clear()
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

    val current = stack.last()
    val isRoot = current == AppScreen.Home || current == AppScreen.Routines ||
        current == AppScreen.Catalog || current == AppScreen.History
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(bottom = if (isRoot) 80.dp else 0.dp)) {
        when (val screen = stack.last()) {
            AppScreen.Home -> HomeScreen(
                viewModel = viewModel,
                onContinue = { push(AppScreen.Session(it)) },
                onStartPlanned = { workoutId ->
                    viewModel.startPlannedWorkout(workoutId) { push(AppScreen.Session(it)) }
                },
                onStartFree = { viewModel.startFreeWorkout { push(AppScreen.Session(it)) } },
                onOpenRoutines = { selectRoot(AppScreen.Routines) },
                onOpenHistory = { selectRoot(AppScreen.History) }
            )
            AppScreen.Routines -> RoutineListScreen(
                viewModel = viewModel,
                onOpenRoutine = { push(AppScreen.RoutineEditor(it)) },
                onOpenCatalog = { selectRoot(AppScreen.Catalog) },
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
            AppScreen.History -> HistoryScreen(
                viewModel = viewModel,
                onOpenSession = { push(AppScreen.Session(it)) }
            )
            is AppScreen.Session -> SessionScreen(
                sessionId = screen.sessionId,
                viewModel = viewModel,
                onBack = ::back,
                onAddExercise = { push(AppScreen.SessionExercisePicker(screen.sessionId)) },
                onSubstitute = { push(AppScreen.SessionExercisePicker(screen.sessionId, it)) },
                onFinished = { selectRoot(AppScreen.Home) },
                onDeleted = { selectRoot(AppScreen.Home) },
                onRoutineSaved = { selectRoot(AppScreen.Routines) }
            )
            is AppScreen.SessionExercisePicker -> ExercisePickerScreen(
                viewModel = viewModel,
                onBack = ::back,
                title = if (screen.replacingSessionExerciseId == null) "Añadir al entrenamiento" else "Sustituir ejercicio",
                onSelect = { exerciseId ->
                    val replacing = screen.replacingSessionExerciseId
                    if (replacing == null) {
                        viewModel.addSessionExercise(screen.sessionId, exerciseId, ::back)
                    } else {
                        viewModel.substituteSessionExercise(replacing, exerciseId, ::back)
                    }
                },
                onCreateCustom = {
                    push(
                        AppScreen.CustomExercise(
                            sessionId = screen.sessionId,
                            replacingSessionExerciseId = screen.replacingSessionExerciseId
                        )
                    )
                }
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
                    val sessionId = screen.sessionId
                    if (sessionId != null) {
                        val replacing = screen.replacingSessionExerciseId
                        val onDone = {
                            while (stack.size > 1 && stack.last() !is AppScreen.Session) back()
                        }
                        if (replacing == null) viewModel.addSessionExercise(sessionId, exerciseId, onDone)
                        else viewModel.substituteSessionExercise(replacing, exerciseId, onDone)
                    } else if (workoutId != null && routineId != null) {
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
        }
        if (isRoot) {
            NavigationBar(Modifier.align(Alignment.BottomCenter)) {
                listOf(
                    Triple(AppScreen.Home, Icons.Default.Home, "Inicio"),
                    Triple(AppScreen.Routines, Icons.AutoMirrored.Filled.List, "Rutinas"),
                    Triple(AppScreen.Catalog, Icons.Default.FitnessCenter, "Ejercicios"),
                    Triple(AppScreen.History, Icons.Default.History, "Historial")
                ).forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        selected = current == screen,
                        onClick = { selectRoot(screen) },
                        icon = { Icon(icon, label) },
                        label = { androidx.compose.material3.Text(label) }
                    )
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(bottom = if (isRoot) 80.dp else 0.dp)
        )
    }
}
