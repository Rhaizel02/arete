@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arete.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.PrType

@Composable
fun ProgressScreen(viewModel: AreteViewModel, onOpenExercise: (String) -> Unit, onOpenBodyWeight: () -> Unit) {
    val state by viewModel.filteredProgressState.collectAsStateWithLifecycle()
    val period by viewModel.progressPeriod.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val eligible = state.exercises.filter { exercise -> exercise.recordEvents.any { it.type == PrType.MAX_E1RM } }
    val selected = eligible.firstOrNull { it.exerciseId == selectedId } ?: eligible.firstOrNull()
    var menu by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Column {
        Text("ARETE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Progreso", style = MaterialTheme.typography.headlineSmall)
    } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { ProgressPeriodSelector(period) { viewModel.progressPeriod.value = it } }
            item { Text("RENDIMIENTO · ${period.label.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Entrenamientos", "${state.stats.totalWorkouts}", Modifier.weight(1f), "sesiones completadas")
                MetricTile("Series efectivas", "${state.stats.effectiveWorkSets}", Modifier.weight(1f), "sin calentamientos")
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Sesiones por semana", uiNumber(state.stats.workoutsPerWeek), Modifier.weight(1f), "media del período")
                MetricTile("Duración media", state.stats.averageDurationMinutes?.let { "$it min" } ?: "—", Modifier.weight(1f), "sesiones con duración fiable")
            } }
            item { VolumeSection(state.externalVolume, period = period) }
            item { TonalCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Evolución de fuerza", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    if (eligible.isNotEmpty()) Box {
                        TextButton(onClick = { menu = true }) { Text("Ejercicio ▾") }
                        DropdownMenu(menu, { menu = false }) { eligible.forEach { exercise -> DropdownMenuItem(text = { Text(exercise.exerciseName) }, onClick = { selectedId = exercise.exerciseId; menu = false }) } }
                    }
                }
                if (selected != null) {
                    Text(selected.exerciseName, style = MaterialTheme.typography.titleMedium)
                    Text(selected.bestE1RmKg?.let { "${uiNumber(it)} kg" } ?: "—", style = MaterialTheme.typography.displaySmall)
                    Text("Mejor e1RM del período · Epley", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StaticTrendChart(selected.history.mapNotNull { point -> point.bestE1RmKg?.let { ChartValue(point.completedAt, it) } }, "kg est.")
                    TextButton(onClick = { onOpenExercise(selected.exerciseId) }) { Text("Ver detalle del ejercicio"); Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
                } else Text("Completa series de trabajo representativas para ver tu evolución.", style = MaterialTheme.typography.bodyMedium)
            } }
            if (state.stats.muscleDistribution.isNotEmpty()) item { TonalCard {
                Text("Series por músculo", style = MaterialTheme.typography.titleLarge)
                Text("Músculo principal · series efectivas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val max = state.stats.muscleDistribution.maxOf { it.sets }.coerceAtLeast(1)
                state.stats.muscleDistribution.forEach { muscle ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(muscle.muscle); Text("${muscle.sets}", style = MaterialTheme.typography.titleMedium) }
                    LinearProgressIndicator(progress = { muscle.sets.toFloat() / max }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                }
            } }
            val records = state.exercises.flatMap { it.records }.sortedByDescending { it.achievedAt }.distinctBy { it.exerciseId }.take(3)
            if (records.isNotEmpty()) {
                item { SectionHeading("Récords absolutos (PR)") }
                records.forEach { record -> item(key = "pr-${record.exerciseId}") { TonalCard(onClick = { onOpenExercise(record.exerciseId) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(record.exerciseName, style = MaterialTheme.typography.titleMedium)
                            Text("${uiNumber(record.weightKg)} kg × ${record.reps} reps · ${uiDate(record.achievedAt)}", style = MaterialTheme.typography.bodySmall)
                            Text(when(record.type) { PrType.MAX_WEIGHT -> "Mayor peso"; PrType.MAX_E1RM -> "Mayor e1RM estimado"; PrType.REPS_AT_WEIGHT -> "Más reps a ese peso" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                } } }
            }
            item { TonalCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MonitorWeight, null, tint = MaterialTheme.colorScheme.primary); Text(" Peso corporal", style = MaterialTheme.typography.titleLarge) }
                Text(state.bodyWeightTrend.latestKg?.let { "${uiNumber(it)} kg" } ?: "Añade tu primer registro", style = MaterialTheme.typography.headlineMedium)
                if (state.bodyWeights.isNotEmpty()) StaticTrendChart(state.bodyWeights.map { ChartValue(it.recordedAt, it.weightKg) }, "kg")
                TextButton(onClick = onOpenBodyWeight) { Text("Consultar y registrar peso →") }
            } }
            state.suggestions.forEach { suggestion -> item(key = "suggestion-${suggestion.exerciseId}") { TonalCard {
                Text("Sugerencia · última sesión histórica", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(suggestion.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text("Buen momento para probar ${uiNumber(suggestion.suggestedWeightKg)} kg")
                Text("Todas las series alcanzaron el máximo del rango. La rutina no se modifica automáticamente.", style = MaterialTheme.typography.bodySmall)
            } } }
            item { SectionHeading("Todos los ejercicios") }
            if (state.exercises.isEmpty()) item { Text("Aquí aparecerá tu historial de rendimiento al finalizar tus primeras sesiones.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            state.exercises.forEach { exercise -> item(key = exercise.exerciseId) { TonalCard(onClick = { onOpenExercise(exercise.exerciseId) }) {
                Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                Text("${exercise.sessionCount} sesiones en el período", style = MaterialTheme.typography.bodySmall)
                Text("Mejor peso ${exercise.maxWeightKg?.let { "${uiNumber(it)} kg" } ?: "—"} · ${exercise.bestE1RmKg?.let { "${uiNumber(it)} kg e1RM est." } ?: "Sin e1RM representativo"}", style = MaterialTheme.typography.bodySmall)
            } } }
        }
    }
}
