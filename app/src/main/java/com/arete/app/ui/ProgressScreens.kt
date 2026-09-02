@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arete.app.data.ExerciseProgress
import com.arete.app.data.PersonalRecord
import com.arete.app.data.PrType
import com.arete.app.data.ProgressDashboard
import com.arete.app.data.local.BodyWeightEntryEntity
import java.util.Date
import kotlin.math.max

@Composable
fun ProgressScreen(
    viewModel: AreteViewModel,
    onOpenExercise: (String) -> Unit,
    onOpenBodyWeight: () -> Unit
) {
    val state by viewModel.progressState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Progreso", fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { WeeklyCard(state, viewModel::setWeeklyGoal) }
            state.latestRecord?.let { record -> item { RecentRecordCard(record) } }
            if (state.suggestions.isNotEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Sugerencia conservadora", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            state.suggestions.take(3).forEach { suggestion ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Buen momento para probar ${formatMetric(suggestion.suggestedWeightKg)} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${suggestion.exerciseName}: completaste todas las series en ${suggestion.targetReps} reps al peso actual.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            item { GeneralStatsCard(state) }
            item {
                Card(Modifier.fillMaxWidth().clickable(onClick = onOpenBodyWeight)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonitorWeight, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text("Peso corporal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val latest = state.bodyWeightTrend.latestKg
                            Text(if (latest == null) "Añade tu primer registro" else "Último: ${formatMetric(latest)} kg · ${state.bodyWeights.size} registros")
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, "Abrir")
                    }
                }
            }
            item { Text("Progreso por ejercicio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            if (state.exercises.isEmpty()) item {
                EmptyState("Aún no hay rendimiento", "Completa series de trabajo para ver evolución, estimaciones y PR.")
            }
            items(state.exercises, key = { it.exerciseId }) { exercise ->
                ExerciseProgressCard(exercise) { onOpenExercise(exercise.exerciseId) }
            }
        }
    }
}

@Composable
private fun WeeklyCard(state: ProgressDashboard, onGoalChange: (Int) -> Unit) {
    val weekly = state.weekly
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${weekly.currentCount} / ${weekly.goal} esta semana", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (weekly.streak == 1) "🔥 1 semana cumpliendo" else "🔥 ${weekly.streak} semanas cumpliendo")
                }
                IconButton(enabled = weekly.goal > 1, onClick = { onGoalChange(weekly.goal - 1) }) { Icon(Icons.Default.Remove, "Reducir objetivo") }
                Text("${weekly.goal}", fontWeight = FontWeight.Bold)
                IconButton(enabled = weekly.goal < 14, onClick = { onGoalChange(weekly.goal + 1) }) { Icon(Icons.Default.Add, "Aumentar objetivo") }
            }
            WeeklyBars(weekly.recentWeeks.map { it.workouts }, weekly.goal)
            Text("Semanas de lunes a domingo. La semana actual no corta la racha mientras aún puede completarse.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WeeklyBars(values: List<Int>, goal: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = .16f)
    Canvas(Modifier.fillMaxWidth().height(52.dp)) {
        if (values.isEmpty()) return@Canvas
        val gap = 5.dp.toPx()
        val width = (size.width - gap * (values.size - 1)) / values.size
        val scale = size.height / max(goal, values.maxOrNull() ?: 1).coerceAtLeast(1)
        values.forEachIndexed { index, value ->
            val left = index * (width + gap)
            drawRoundRect(muted, topLeft = Offset(left, 0f), size = androidx.compose.ui.geometry.Size(width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            val height = value * scale
            drawRoundRect(primary, topLeft = Offset(left, size.height - height), size = androidx.compose.ui.geometry.Size(width, height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
        }
    }
}

@Composable
private fun RecentRecordCard(record: PersonalRecord) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("PR reciente", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(record.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${prLabel(record.type)} · ${formatMetric(record.weightKg)} kg × ${record.reps}")
        }
    }
}

@Composable
private fun GeneralStatsCard(state: ProgressDashboard) {
    val stats = state.stats
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Actividad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatValue(stats.totalWorkouts.toString(), "entrenamientos")
                StatValue(formatMetric(stats.workoutsPerWeek), "por semana")
                StatValue(stats.averageDurationMinutes?.let { "$it min" } ?: "—", "duración media")
                StatValue(stats.effectiveWorkSets.toString(), "series efectivas")
            }
            if (stats.muscleDistribution.isNotEmpty()) {
                Text("Series por músculo principal", style = MaterialTheme.typography.labelLarge)
                Text(stats.muscleDistribution.take(4).joinToString(" · ") { "${it.muscle} ${it.sets}" }, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ExerciseProgressCard(exercise: ExerciseProgress, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${exercise.sessionCount} sesiones · última ${formatDate(exercise.lastPerformedAt)}", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Mejor peso ${exercise.maxWeightKg?.let { "${formatMetric(it)} kg" } ?: "—"} · e1RM est. ${exercise.bestE1RmKg?.let { "${formatMetric(it)} kg" } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium
            )
            exercise.recentE1RmChangeKg?.let {
                Text("Evolución reciente ${if (it >= 0) "+" else ""}${formatMetric(it)} kg e1RM", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ExerciseProgressScreen(exerciseId: String, viewModel: AreteViewModel, onBack: () -> Unit) {
    val flow = remember(exerciseId) { viewModel.observeExerciseProgress(exerciseId) }
    val progress by flow.collectAsStateWithLifecycle(initialValue = null)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(progress?.exerciseName ?: "Progreso del ejercicio") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
        )
    }) { padding ->
        val item = progress
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Sin datos de rendimiento") }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatValue(item.sessionCount.toString(), "sesiones")
                    StatValue(item.maxWeightKg?.let { "${formatMetric(it)} kg" } ?: "—", "mejor peso")
                    StatValue(item.bestWeightKg?.let { "${formatMetric(it)}×${item.bestReps}" } ?: "—", "mejor serie")
                    StatValue(item.bestE1RmKg?.let { "${formatMetric(it)} kg" } ?: "—", "e1RM est.")
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Evolución de e1RM estimado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val values = item.history.asReversed().mapNotNull { it.bestE1RmKg }
                        if (values.size >= 2) SimpleLineChart(values) else Text("Se necesitan al menos dos sesiones representativas.", style = MaterialTheme.typography.bodySmall)
                        Text("Fórmula de Epley; solo series de trabajo completadas de 1 a 12 repeticiones.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (item.records.isNotEmpty()) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Récords derivados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        item.records.forEach { Text("${prLabel(it.type)} · ${formatMetric(it.weightKg)} kg × ${it.reps}") }
                    }
                }
            }
            item { Text("Historial de sesiones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(item.history, key = { it.sessionId }) { point ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(formatDate(point.completedAt), fontWeight = FontWeight.Medium)
                            Text(point.bestWeightKg?.let { "Mejor serie ${formatMetric(it)} kg × ${point.bestReps}" } ?: "Serie sin carga externa", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(point.bestE1RmKg?.let { "${formatMetric(it)} kg est." } ?: "—", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun BodyWeightScreen(viewModel: AreteViewModel, onBack: () -> Unit) {
    val state by viewModel.progressState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BodyWeightEntryEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<BodyWeightEntryEntity?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Peso corporal") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showEditor = true }) { Icon(Icons.Default.Add, "Añadir peso") }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.bodyWeightTrend.latestKg?.let { "${formatMetric(it)} kg" } ?: "Sin registros", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        state.bodyWeightTrend.movingAverageKg?.let {
                            val count = minOf(7, state.bodyWeights.size)
                            Text("Media móvil ($count ${if (count == 1) "registro" else "registros"}): ${formatMetric(it)} kg")
                        }
                        state.bodyWeightTrend.recentChangeKg?.let { Text("Cambio reciente: ${if (it >= 0) "+" else ""}${formatMetric(it)} kg", color = MaterialTheme.colorScheme.primary) }
                        if (state.bodyWeights.size >= 2) SimpleLineChart(state.bodyWeights.map { it.weightKg })
                    }
                }
            }
            if (state.bodyWeights.isEmpty()) item { EmptyState("Registra tu peso", "Una entrada rápida en kg basta para empezar a ver la tendencia.") }
            items(state.bodyWeights.asReversed(), key = { it.id }) { entry ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${formatMetric(entry.weightKg)} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(formatDateTime(entry.recordedAt), style = MaterialTheme.typography.bodySmall)
                            if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { editing = entry; showEditor = true }) { Icon(Icons.Default.Edit, "Editar") }
                        IconButton(onClick = { deleting = entry }) { Icon(Icons.Default.DeleteOutline, "Eliminar") }
                    }
                }
            }
        }
    }
    if (showEditor) BodyWeightEditor(editing, onDismiss = { showEditor = false }) { kg, notes ->
        viewModel.saveBodyWeight(editing?.id, kg, notes) { showEditor = false }
    }
    deleting?.let { entry ->
        ConfirmDialog("Eliminar registro", "Se eliminará el peso de ${formatDate(entry.recordedAt)}.", onDismiss = { deleting = null }) {
            deleting = null
            viewModel.deleteBodyWeight(entry)
        }
    }
}

@Composable
private fun BodyWeightEditor(entry: BodyWeightEntryEntity?, onDismiss: () -> Unit, onSave: (Double, String) -> Unit) {
    var weight by remember(entry?.id) { mutableStateOf(entry?.weightKg?.let(::formatMetric).orEmpty()) }
    var notes by remember(entry?.id) { mutableStateOf(entry?.notes.orEmpty()) }
    val parsed = weight.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Añadir peso" else "Editar peso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { char -> char.isDigit() || char == ',' || char == '.' } },
                    label = { Text("Peso (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Nota opcional") })
            }
        },
        confirmButton = { Button(enabled = parsed != null && parsed in 1.0..1_000.0, onClick = { onSave(parsed!!, notes) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SimpleLineChart(values: List<Double>) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = .15f)
    Canvas(Modifier.fillMaxWidth().height(150.dp).padding(vertical = 8.dp)) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull()!!
        val max = values.maxOrNull()!!
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        val points = values.mapIndexed { index, value ->
            Offset(index * size.width / (values.lastIndex), size.height - ((value - min) / range * size.height).toFloat())
        }
        points.zipWithNext().forEach { (a, b) -> drawLine(primary, a, b, 3.dp.toPx(), cap = StrokeCap.Round) }
        points.forEach { drawCircle(primary, 4.dp.toPx(), it); drawCircle(Color.White, 2.dp.toPx(), it, style = androidx.compose.ui.graphics.drawscope.Fill) }
    }
}

private fun prLabel(type: PrType): String = when (type) {
    PrType.MAX_WEIGHT -> "Mayor peso"
    PrType.MAX_E1RM -> "Mayor e1RM estimado"
    PrType.REPS_AT_WEIGHT -> "Más repeticiones a ese peso"
}

private fun formatDate(timestamp: Long): String = DateFormat.format("dd MMM yyyy", Date(timestamp)).toString()
private fun formatDateTime(timestamp: Long): String = DateFormat.format("dd MMM yyyy · HH:mm", Date(timestamp)).toString()
private fun formatMetric(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value).replace('.', ',')
