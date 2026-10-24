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
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import com.arete.app.ui.theme.ActionBlue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

sealed interface AppScreen : java.io.Serializable {
    data object Home : AppScreen
    data object More : AppScreen
    data object Routines : AppScreen
    data object Catalog : AppScreen
    data object History : AppScreen
    data object Progress : AppScreen
    data object BodyWeight : AppScreen
    data object Settings : AppScreen
    data object Planning : AppScreen
    data object Activity : AppScreen
    data class ExerciseProgress(val exerciseId: String) : AppScreen
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
fun AreteApp(viewModel: AreteViewModel, openSessionRequest: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val stack = rememberSaveable(saver = Saver<androidx.compose.runtime.snapshots.SnapshotStateList<AppScreen>, java.util.ArrayList<AppScreen>>(
        save = { java.util.ArrayList(it) },
        restore = { it.toMutableStateList() }
    )) { mutableStateListOf<AppScreen>(AppScreen.Home) }

    fun push(screen: AppScreen) { if (stack.lastOrNull() != screen) stack += screen }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun replaceWith(screen: AppScreen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        stack += screen
    }
    fun selectRoot(screen: AppScreen) {
        stack.clear()
        stack += screen
    }
    LaunchedEffect(openSessionRequest) {
        if (openSessionRequest > 0) viewModel.activeSessionId()?.let { id ->
            if (stack.lastOrNull() != AppScreen.Session(id)) { selectRoot(AppScreen.Home); push(AppScreen.Session(id)) }
        }
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
        val content = viewModel.routineExportPayload
        if (uri != null && content != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) } }
                .onSuccess { scope.launch { snackbar.showSnackbar("Rutina exportada") } }
                .onFailure { scope.launch { snackbar.showSnackbar("No se pudo guardar el archivo") } }
        }
        viewModel.routineExportPayload = null
    }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = viewModel.backupExportPayload
        if (uri != null && content != null) {
            scope.launch {
                runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri) ?: error("No se pudo abrir el archivo")
                    output.bufferedWriter().use { it.write(content) }
                } }.onSuccess { snackbar.showSnackbar("Backup exportado") }
                    .onFailure { snackbar.showSnackbar("No se pudo guardar el backup") }
            }
        }
        viewModel.backupExportPayload = null
    }
    val backupRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri) ?: error("No se pudo abrir el backup")
                    input.bufferedReader().use { it.readText() }
                } }.onSuccess { viewModel.pendingBackupRestore = it }
                    .onFailure { snackbar.showSnackbar("No se pudo leer el backup") }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    BackHandler(enabled = stack.size > 1, onBack = ::back)

    val current = stack.last()
    val isRoot = current == AppScreen.Home || current == AppScreen.Routines ||
        current == AppScreen.More || current == AppScreen.Progress
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(bottom = if (isRoot) 80.dp else 0.dp)) {
        when (val screen = stack.last()) {
            AppScreen.More -> MoreScreen { push(it) }
            AppScreen.Home -> HomeScreen(
                viewModel = viewModel,
                onContinue = { push(AppScreen.Session(it)) },
                onStartPlanned = { workoutId ->
                    viewModel.startPlannedWorkout(workoutId) { push(AppScreen.Session(it)) }
                },
                onStartFree = { viewModel.startFreeWorkout { push(AppScreen.Session(it)) } },
                onOpenRoutines = { selectRoot(AppScreen.Routines) },
                onOpenHistory = { push(AppScreen.History) },
                onOpenProgress = { selectRoot(AppScreen.Progress) },
                onOpenActivity = { push(AppScreen.Activity) },
                onOpenSettings = { push(AppScreen.Settings) }
            )
            AppScreen.Routines -> RoutineListScreen(
                viewModel = viewModel,
                onOpenRoutine = { push(AppScreen.RoutineEditor(it)) },
                onOpenCatalog = { push(AppScreen.Catalog) },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                onExport = { routine ->
                    viewModel.exportRoutine(routine.id) { content ->
                        viewModel.routineExportPayload = content
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
                onOpenSession = { push(AppScreen.Session(it)) },
                onBack = ::back
            )
            AppScreen.Progress -> ProgressScreen(
                viewModel = viewModel,
                onOpenExercise = { push(AppScreen.ExerciseProgress(it)) },
                onOpenBodyWeight = { push(AppScreen.BodyWeight) }
            )
            AppScreen.BodyWeight -> BodyWeightScreen(viewModel, ::back)
            is AppScreen.ExerciseProgress -> ExerciseProgressScreen(screen.exerciseId, viewModel, ::back)
            AppScreen.Settings -> SettingsScreen(
                viewModel = viewModel,
                onBack = ::back,
                onOpenPlanning = { push(AppScreen.Planning) },
                onExportBackup = {
                    viewModel.exportBackup { content ->
                        viewModel.backupExportPayload = content
                        backupExportLauncher.launch("arete-backup.json")
                    }
                },
                onRestoreBackup = { backupRestoreLauncher.launch(arrayOf("application/json", "text/plain")) }
            )
            AppScreen.Planning -> WeeklyPlanningScreen(viewModel, ::back)
            AppScreen.Activity -> ActivityScreen(viewModel, ::back) { push(AppScreen.Session(it)) }
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
            NavigationBar(Modifier.align(Alignment.BottomCenter), containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                listOf(
                    Triple(AppScreen.Home, Icons.Default.Home, "Inicio"),
                    Triple(AppScreen.Routines, Icons.AutoMirrored.Filled.List, "Rutinas"),
                    Triple(AppScreen.Progress, Icons.Default.Insights, "Progreso"),
                    Triple(AppScreen.More, Icons.Default.Tune, "Más")
                ).forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        selected = current == screen,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = ActionBlue, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        onClick = { selectRoot(screen) },
                        icon = { Icon(icon, label) },
                        label = { Text(label) }
                    )
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(bottom = if (isRoot) 80.dp else 0.dp)
        )
    }
    viewModel.pendingBackupRestore?.let { raw ->
        AlertDialog(
            onDismissRequest = { viewModel.pendingBackupRestore = null },
            title = { Text("Restaurar backup completo") },
            text = { Text("Se reemplazarán rutinas, sesiones, peso, planificación y preferencias locales. El archivo se validará antes de modificar nada.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.pendingBackupRestore = null
                    viewModel.restoreBackup(raw) { selectRoot(AppScreen.Home) }
                }) { Text("Reemplazar y restaurar") }
            },
            dismissButton = { TextButton(onClick = { viewModel.pendingBackupRestore = null }) { Text("Cancelar") } },
            shape = MaterialTheme.shapes.medium, containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    }
}
