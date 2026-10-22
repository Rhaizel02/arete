@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    viewModel: AreteViewModel,
    onContinue: (String) -> Unit,
    onStartPlanned: (String) -> Unit,
    onStartFree: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val timerEnabled by viewModel.restTimerEnabled.collectAsStateWithLifecycle()
    val progress by viewModel.progressState.collectAsStateWithLifecycle()
    val planning by viewModel.planningState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Arete", fontWeight = FontWeight.Bold)
                        Text("Entrena. Registra. Mejora.", style = MaterialTheme.typography.labelMedium)
                    }
                },
                actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Ajustes") } }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state?.activeSession?.let { active ->
                item {
                    Card(
                        Modifier.fillMaxWidth().clickable { onContinue(active.session.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    " Entrenamiento en curso",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(active.session.workoutNameSnapshot, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("${active.exercises.sumOf { it.sets.count { set -> set.isCompleted } }} series completadas")
                            Button(onClick = { onContinue(active.session.id) }, Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.PlayArrow, null)
                                Text("Continuar entrenamiento")
                            }
                        }
                    }
                }
            }

            item { Text("Próximo entrenamiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            item {
                val next = state?.nextWorkout
                if (next != null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(next.routineName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(next.workoutName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("${next.exerciseCount} ejercicios · secuencia automática")
                            Button(
                                enabled = state?.activeSession == null,
                                onClick = { onStartPlanned(next.workoutId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FitnessCenter, null)
                                Text("Entrenar ${next.workoutName}")
                            }
                        }
                    }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("No hay una rutina activa con workouts", style = MaterialTheme.typography.titleMedium)
                            Text("Activa una rutina para que Arete mantenga la secuencia por ti.")
                            OutlinedButton(onClick = onOpenRoutines) { Text("Gestionar rutinas") }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    enabled = state?.activeSession == null,
                    onClick = onStartFree,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bolt, null)
                    Text("Entrenamiento libre")
                }
            }

            item {
                val today = planning.today
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                            Text(" Estado de hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        when {
                            today.completedSession != null -> {
                                Text("Entrenamiento completado", fontWeight = FontWeight.Medium)
                                Text(today.completedSession.workoutNameSnapshot, style = MaterialTheme.typography.bodySmall)
                            }
                            today.skipped != null -> {
                                Text("Hoy no entreno", fontWeight = FontWeight.Medium)
                                Text(today.skipped.workoutNameSnapshot, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { viewModel.undoSkippedDay(today.skipped) }) { Text("Deshacer") }
                            }
                            today.plannedWorkout != null -> {
                                Text("Planificado · ${today.plannedWorkout.name}", fontWeight = FontWeight.Medium)
                                Text("La secuencia real no cambia si hoy no puedes entrenar.", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(onClick = viewModel::skipToday) { Text("Hoy no entreno") }
                            }
                            else -> Text("Sin entrenamiento planificado", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = onOpenActivity) { Text("Ver actividad") }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().clickable(onClick = onOpenProgress)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                if (progress.weekly.currentCount >= progress.weekly.goal) "Semana completada"
                                else "${progress.weekly.currentCount} / ${progress.weekly.goal} esta semana",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (progress.weekly.streak == 1) "🔥 1 semana cumpliendo"
                                else "🔥 ${progress.weekly.streak} semanas cumpliendo",
                                style = MaterialTheme.typography.bodySmall
                            )
                            progress.latestRecord?.let {
                                Text("PR reciente · ${it.exerciseName} · ${formatHomeWeight(it.weightKg)} kg × ${it.reps}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("Ver", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Temporizador de descanso", fontWeight = FontWeight.Medium)
                            Text("Se inicia al completar una serie de trabajo", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = timerEnabled, onCheckedChange = viewModel::setRestTimerEnabled)
                    }
                }
            }

            if (!state?.recentSessions.isNullOrEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sesiones recientes", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = onOpenHistory) {
                            Icon(Icons.Default.History, null)
                            Text("Ver todas")
                        }
                    }
                }
                state?.recentSessions.orEmpty().forEach { session ->
                    item(key = session.id) {
                        Card(Modifier.fillMaxWidth().clickable { onOpenHistory() }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(session.workoutNameSnapshot, fontWeight = FontWeight.Medium)
                                Text(session.routineNameSnapshot ?: "Entrenamiento libre", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHomeWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value).replace('.', ',')
