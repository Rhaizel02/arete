@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.ActivityDay
import com.arete.app.data.ActivityDayStatus
import com.arete.app.data.ThemeMode
import java.util.Calendar
import java.util.Date

@Composable
fun SettingsScreen(
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onOpenPlanning: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val planning by viewModel.planningState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setRemindersEnabled(true)
        else viewModel.showMessage("Sin permiso de notificaciones no se pueden activar recordatorios")
    }
    fun setReminders(enabled: Boolean) {
        if (!enabled) viewModel.setRemindersEnabled(false)
        else if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) viewModel.setRemindersEnabled(true)
        else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(topBar = {
        ScreenHeader("Ajustes", onBack, "ARETE · A TU MEDIDA")
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TonalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Entrenamiento")
                        SettingSwitch("Temporizador de descanso", settings.restTimerEnabled, viewModel::setRestTimerEnabled)
                        SettingSwitch("Vibración", settings.vibrationEnabled, viewModel::setVibrationEnabled)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Tema")
                                Text(themeLabel(settings.themeMode), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = viewModel::cycleTheme) { Text("Cambiar tema") }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("Objetivo semanal"); Text("Sesiones por semana", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(enabled = settings.weeklyGoal > 1, onClick = { viewModel.setWeeklyGoal(settings.weeklyGoal - 1) }) { Icon(Icons.Default.Remove, "Reducir objetivo") }
                            Text("${settings.weeklyGoal}", style = MaterialTheme.typography.titleLarge)
                            IconButton(enabled = settings.weeklyGoal < 14, onClick = { viewModel.setWeeklyGoal(settings.weeklyGoal + 1) }) { Icon(Icons.Default.Add, "Aumentar objetivo") }
                        }
                    }
                }
            }
            item {
                TonalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Semana y recordatorios")
                        Text(planning.routineName?.let { "$it · ${planning.weeklyPlan.size} días planificados" } ?: "Activa una rutina para planificarla", style = MaterialTheme.typography.bodySmall)
                        SecondaryAction(onClick = onOpenPlanning, enabled = planning.workouts.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                            Text("Configurar semana")
                        }
                        SettingSwitch("Recordatorios en días planificados", settings.remindersEnabled, ::setReminders)
                        SecondaryAction(onClick = { showTimePicker = true }, enabled = settings.remindersEnabled, modifier = Modifier.fillMaxWidth()) {
                            Text("Hora · %02d:%02d".format(settings.reminderHour, settings.reminderMinute))
                        }
                        Text("Android puede retrasar el aviso por ahorro de batería. Nunca se avisa si ya entrenaste, omitiste el día o hay una sesión en curso.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                TonalCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeading("Tus datos")
                        Text("Routine JSON comparte una rutina. Backup JSON reemplaza y restaura toda la aplicación.", style = MaterialTheme.typography.bodySmall)
                        PrimaryAction(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) { Text("Exportar backup completo") }
                        SecondaryAction(onClick = onRestoreBackup, Modifier.fillMaxWidth()) { Text("Restaurar backup") }
                        Text("Restaurar reemplaza todos los datos locales. Se pedirá confirmación.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (showTimePicker) ReminderTimeDialog(
        initialHour = settings.reminderHour,
        initialMinute = settings.reminderMinute,
        onDismiss = { showTimePicker = false }
    ) { hour, minute ->
        showTimePicker = false
        viewModel.setReminderTime(hour, minute)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ReminderTimeDialog(initialHour: Int, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour, initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hora del recordatorio") },
        text = { TimeInput(state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        shape = MaterialTheme.shapes.medium, containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
}

@Composable
fun WeeklyPlanningScreen(viewModel: AreteViewModel, onBack: () -> Unit) {
    val state by viewModel.planningState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        ScreenHeader("Tu semana", onBack, "PLANIFICACIÓN")
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(state.routineName ?: "Planifica tus entrenamientos", style = MaterialTheme.typography.titleLarge)
                Text("Asigna tus días. La secuencia real de la rutina no cambia.", Modifier.padding(top = 4.dp, bottom = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.workouts.isEmpty()) item { EmptyState("Sin rutina activa", "Activa una rutina antes de configurar la semana.") }
            items(WEEK_DAYS, key = { it.first }) { (day, label) ->
                var expanded by remember(day) { mutableStateOf(false) }
                val selectedId = state.weeklyPlan.firstOrNull { it.dayOfWeek == day }?.workoutId
                val selected = state.workouts.firstOrNull { it.id == selectedId }
                EditorialRow {
                        Text(label, Modifier.weight(.4f), style = MaterialTheme.typography.titleMedium)
                        Column(Modifier.weight(.6f), horizontalAlignment = Alignment.End) {
                            TextButton(onClick = { expanded = true }, enabled = state.workouts.isNotEmpty(), modifier = Modifier.semantics { contentDescription = "Plan de $label" }) { Text((selected?.name ?: "Descanso") + " ▾") }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("Sin planificación") }, onClick = {
                                    expanded = false
                                    viewModel.setPlannedWorkout(day, null)
                                })
                                state.workouts.forEach { workout ->
                                    DropdownMenuItem(text = { Text(workout.name) }, onClick = {
                                        expanded = false
                                        viewModel.setPlannedWorkout(day, workout.id)
                                    })
                                }
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun ActivityScreen(
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit
) {
    val state by viewModel.planningState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        ScreenHeader("Actividad", onBack, "CONSTANCIA SEMANAL")
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { Text("Últimos 14 días y próximos 7", Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.activity.sortedByDescending { it.timestamp }, key = { it.epochDay }) { day ->
                ActivityDayCard(day, onOpenSession) { skipped -> viewModel.undoSkippedDay(skipped) }
            }
        }
    }
}

@Composable
private fun ActivityDayCard(
    day: ActivityDay,
    onOpenSession: (String) -> Unit,
    onUndoSkip: (com.arete.app.data.local.SkippedPlannedWorkoutEntity) -> Unit
) {
    val clickable = day.completedSessions.isNotEmpty()
    EditorialRow(onClick = if (clickable) ({ onOpenSession(day.completedSessions.first().id) }) else null) {
            Icon(
                when (day.status) {
                    ActivityDayStatus.COMPLETED -> Icons.Default.CheckCircle
                    ActivityDayStatus.PLANNED -> Icons.Default.Event
                    ActivityDayStatus.SKIPPED -> Icons.Default.Block
                    ActivityDayStatus.NONE -> Icons.Default.RadioButtonUnchecked
                },
                null,
                tint = when (day.status) {
                    ActivityDayStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
                    ActivityDayStatus.SKIPPED -> MaterialTheme.colorScheme.tertiary
                    ActivityDayStatus.PLANNED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }, modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(DateFormat.format("EEE, dd MMM", Date(day.timestamp)).toString(), fontWeight = FontWeight.Medium)
                Text(
                    when (day.status) {
                        ActivityDayStatus.COMPLETED -> day.completedSessions.joinToString { it.workoutNameSnapshot }
                        ActivityDayStatus.PLANNED -> "Pendiente · ${day.plannedWorkoutName}"
                        ActivityDayStatus.SKIPPED -> "Día omitido · ${day.skipped?.workoutNameSnapshot.orEmpty()}"
                        ActivityDayStatus.NONE -> "Sin planificación"
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            day.skipped?.let { TextButton(onClick = { onUndoSkip(it) }) { Text("Deshacer") } }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Oscuro"
}

private val WEEK_DAYS = listOf(
    Calendar.MONDAY to "Lunes",
    Calendar.TUESDAY to "Martes",
    Calendar.WEDNESDAY to "Miércoles",
    Calendar.THURSDAY to "Jueves",
    Calendar.FRIDAY to "Viernes",
    Calendar.SATURDAY to "Sábado",
    Calendar.SUNDAY to "Domingo"
)
