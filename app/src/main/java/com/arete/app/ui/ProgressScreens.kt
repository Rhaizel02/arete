@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import com.arete.app.data.PlanningDates
import com.arete.app.data.local.BodyWeightEntryEntity
import java.util.Date
import kotlin.math.max


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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricTile("Sesiones", item.sessionCount.toString(), Modifier.weight(1f))
                        MetricTile("Mejor peso", item.maxWeightKg?.let { "${formatMetric(it)} kg" } ?: "—", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricTile("Mejor serie", item.bestWeightKg?.let { "${formatMetric(it)}×${item.bestReps}" } ?: "—", Modifier.weight(1f))
                        MetricTile("e1RM est.", item.bestE1RmKg?.let { "${formatMetric(it)} kg" } ?: "—", Modifier.weight(1f))
                    }
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
    if (showEditor) BodyWeightEditor(editing, onDismiss = { showEditor = false }) { kg, notes, recordedAt ->
        viewModel.saveBodyWeight(editing?.id, kg, notes, recordedAt) { showEditor = false }
    }
    deleting?.let { entry ->
        ConfirmDialog("Eliminar registro", "Se eliminará el peso de ${formatDate(entry.recordedAt)}.", onDismiss = { deleting = null }) {
            deleting = null
            viewModel.deleteBodyWeight(entry)
        }
    }
}

@Composable
private fun BodyWeightEditor(
    entry: BodyWeightEntryEntity?,
    onDismiss: () -> Unit,
    onSave: (Double, String, Long) -> Unit
) {
    var weight by remember(entry?.id) { mutableStateOf(entry?.weightKg?.let(::formatMetric).orEmpty()) }
    var notes by remember(entry?.id) { mutableStateOf(entry?.notes.orEmpty()) }
    var selectedDate by remember(entry?.id) {
        mutableStateOf(PlanningDates.pickerDate(entry?.recordedAt ?: System.currentTimeMillis()))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val parsed = weight.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Añadir peso" else "Editar peso") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { char -> char.isDigit() || char == ',' || char == '.' } },
                    label = { Text("Peso (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedButton(onClick = { showDatePicker = true }, Modifier.fillMaxWidth()) {
                    Text("Fecha · ${formatDate(PlanningDates.timestampFromPickerDate(selectedDate))}")
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Nota opcional") })
            }
        },
        confirmButton = {
            Button(
                enabled = parsed != null && parsed in 1.0..1_000.0,
                onClick = { onSave(parsed!!, notes, PlanningDates.timestampFromPickerDate(selectedDate)) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }
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
