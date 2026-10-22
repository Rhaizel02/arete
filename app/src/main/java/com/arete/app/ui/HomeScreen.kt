@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arete.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.ActivityDayStatus
import com.arete.app.ui.theme.ActionBlue

@Composable
fun HomeScreen(
    viewModel: AreteViewModel, onContinue: (String) -> Unit, onStartPlanned: (String) -> Unit,
    onStartFree: () -> Unit, onOpenRoutines: () -> Unit, onOpenHistory: () -> Unit,
    onOpenProgress: () -> Unit, onOpenActivity: () -> Unit, onOpenSettings: () -> Unit
) {
    val training by viewModel.homeState.collectAsStateWithLifecycle()
    val progress by viewModel.progressState.collectAsStateWithLifecycle()
    val planning by viewModel.planningState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Column {
            Text("Arete", style = MaterialTheme.typography.headlineSmall)
        } }, actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Tune, "Ajustes") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                val active = training?.activeSession
                val next = training?.nextWorkout
                TonalCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (active != null) "  SESIÓN EN CURSO" else "  SIGUIENTE SESIÓN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(active?.session?.workoutNameSnapshot ?: next?.workoutName ?: "Empieza a entrenar", style = MaterialTheme.typography.headlineLarge)
                    Text(when {
                        active != null -> "${active.exercises.size} ejercicios · series completadas: ${active.exercises.sumOf { it.sets.count { set -> set.isCompleted } }}"
                        next != null -> "${next.routineName} · ${next.exerciseCount} ejercicios"
                        else -> "Activa una rutina o empieza un entrenamiento libre."
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { when { active != null -> onContinue(active.session.id); next != null -> onStartPlanned(next.workoutId); else -> onStartFree() } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionBlue, contentColor = Color.White),
                        shape = MaterialTheme.shapes.medium
                    ) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(if (active != null) "Continuar entrenamiento" else "Empezar entrenamiento") }
                    if (active == null) TextButton(onClick = if (next != null) onStartFree else onOpenRoutines, modifier = Modifier.fillMaxWidth()) {
                        Text(if (next != null) "Entrenamiento libre" else "Gestionar rutinas")
                    }
                }
            }
            item {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TonalCard(Modifier.weight(1f).fillMaxHeight(), onClick = onOpenProgress) {
                        Text("◎  OBJETIVO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${progress.weekly.currentCount} / ${progress.weekly.goal}", style = MaterialTheme.typography.displaySmall)
                        Text(if (progress.weekly.currentCount >= progress.weekly.goal) "Semana completada" else "sesiones esta semana", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { (progress.weekly.currentCount.toFloat() / progress.weekly.goal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = ActionBlue, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    }
                    TonalCard(Modifier.weight(1f).fillMaxHeight(), onClick = onOpenProgress) {
                        Row { Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp)); Text(" RACHA", style = MaterialTheme.typography.labelMedium) }
                        Text("${progress.weekly.streak}", style = MaterialTheme.typography.displaySmall)
                        Text("${if (progress.weekly.streak == 1) "semana" else "semanas"} cumpliendo", style = MaterialTheme.typography.bodySmall)
                        Text("Constancia semanal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            progress.latestRecord?.let { record -> item {
                TonalCard(onClick = onOpenProgress) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🏆  ÚLTIMO PR", style = MaterialTheme.typography.labelMedium); Text(uiDate(record.achievedAt), style = MaterialTheme.typography.labelSmall) }
                    Text(record.exerciseName, style = MaterialTheme.typography.titleLarge)
                    Text("${uiNumber(record.weightKg)} kg × ${record.reps} reps", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
            } }
            item {
                val today = planning.today
                SectionHeading("Hoy", "Ver actividad", onOpenActivity)
                TonalCard {
                    when {
                        today.completedSession != null -> { Text("Entrenamiento completado", style = MaterialTheme.typography.titleMedium); Text(today.completedSession.workoutNameSnapshot) }
                        today.skipped != null -> { Text("Hoy no entreno", style = MaterialTheme.typography.titleMedium); Text(today.skipped.workoutNameSnapshot); TextButton(onClick = { viewModel.undoSkippedDay(today.skipped) }) { Text("Deshacer") } }
                        today.plannedWorkout != null -> { Text("Planificado · ${today.plannedWorkout.name}", style = MaterialTheme.typography.titleMedium); Text("La secuencia de tu rutina se mantiene.", style = MaterialTheme.typography.bodySmall); SecondaryAction(onClick = viewModel::skipToday) { Text("Hoy no entreno") } }
                        else -> Text("Sin planificación para hoy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val upcoming = planning.activity.filter { it.epochDay > planning.today.epochDay && it.status == ActivityDayStatus.PLANNED }.take(3)
            if (upcoming.isNotEmpty()) {
                item { SectionHeading("Próximos entrenamientos", "Ver plan", onOpenActivity) }
                upcoming.forEach { day -> item(key = day.epochDay) { TonalCard(onClick = onOpenActivity) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.FitnessCenter, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 12.dp)) { Text(day.plannedWorkoutName.orEmpty(), style = MaterialTheme.typography.titleMedium); Text(uiDate(day.timestamp), style = MaterialTheme.typography.bodySmall) } }
                } } }
            }
            if (!training?.recentSessions.isNullOrEmpty()) item { TextButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) { Text("Consultar historial") } }
        }
    }
}
