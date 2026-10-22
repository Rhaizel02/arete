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
        TopAppBar(
            title = { Text("Ajustes") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Preferencias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SettingSwitch("Temporizador de descanso", settings.restTimerEnabled, viewModel::setRestTimerEnabled)
                        SettingSwitch("Vibración", settings.vibrationEnabled, viewModel::setVibrationEnabled)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Tema")
                                Text(themeLabel(settings.themeMode), style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = viewModel::cycleTheme) { Text("Cambiar") }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Objetivo semanal", Modifier.weight(1f))
                            TextButton(enabled = settings.weeklyGoal > 1, onClick = { viewModel.setWeeklyGoal(settings.weeklyGoal - 1) }) { Text("−") }
                            Text("${settings.weeklyGoal} sesiones", fontWeight = FontWeight.Medium)
                            TextButton(enabled = settings.weeklyGoal < 14, onClick = { viewModel.setWeeklyGoal(settings.weeklyGoal + 1) }) { Text("+") }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Planificación y recordatorios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(planning.routineName?.let { "$it · ${planning.weeklyPlan.size} días planificados" } ?: "Activa una rutina para planificarla", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onOpenPlanning, enabled = planning.workouts.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                            Text("Configurar semana")
                        }
                        SettingSwitch("Recordatorios en días planificados", settings.remindersEnabled, ::setReminders)
                        OutlinedButton(onClick = { showTimePicker = true }, enabled = settings.remindersEnabled, modifier = Modifier.fillMaxWidth()) {
                            Text("Hora · %02d:%02d".format(settings.reminderHour, settings.reminderMinute))
                        }
                        Text("Android puede retrasar el aviso por ahorro de batería. Nunca se avisa si ya entrenaste, omitiste el día o hay una sesión en curso.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Datos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Routine JSON comparte una rutina. Backup JSON reemplaza y restaura toda la aplicación.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onExportBackup, Modifier.fillMaxWidth()) { Text("Exportar backup completo") }
                        OutlinedButton(onClick = onRestoreBackup, Modifier.fillMaxWidth()) { Text("Restaurar backup") }
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun WeeklyPlanningScreen(viewModel: AreteViewModel, onBack: () -> Unit) {
    val state by viewModel.planningState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Planificación semanal") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("La planificación orienta días y avisos. El próximo workout continúa siendo siempre el siguiente de la secuencia.", style = MaterialTheme.typography.bodyMedium)
            }
            if (state.workouts.isEmpty()) item { EmptyState("Sin rutina activa", "Activa una rutina antes de configurar la semana.") }
            items(WEEK_DAYS, key = { it.first }) { (day, label) ->
                var expanded by remember(day) { mutableStateOf(false) }
                val selectedId = state.weeklyPlan.firstOrNull { it.dayOfWeek == day }?.workoutId
                val selected = state.workouts.firstOrNull { it.id == selectedId }
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Column {
                            OutlinedButton(onClick = { expanded = true }) { Text(selected?.name ?: "Sin plan") }
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
}

@Composable
fun ActivityScreen(
    viewModel: AreteViewModel,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit
) {
    val state by viewModel.planningState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Actividad") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item { Text("Últimos 14 días y próximos 7", style = MaterialTheme.typography.bodyMedium) }
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
    Card(
        Modifier.fillMaxWidth().then(if (clickable) Modifier.clickable { onOpenSession(day.completedSessions.first().id) } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = when (day.status) {
                ActivityDayStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                ActivityDayStatus.SKIPPED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (day.status) {
                    ActivityDayStatus.COMPLETED -> Icons.Default.CheckCircle
                    ActivityDayStatus.PLANNED -> Icons.Default.Event
                    ActivityDayStatus.SKIPPED -> Icons.Default.Block
                    ActivityDayStatus.NONE -> Icons.Default.RadioButtonUnchecked
                },
                null
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
                    style = MaterialTheme.typography.bodySmall
                )
            }
            day.skipped?.let { TextButton(onClick = { onUndoSkip(it) }) { Text("Deshacer") } }
        }
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
