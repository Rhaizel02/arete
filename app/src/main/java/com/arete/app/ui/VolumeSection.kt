package com.arete.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arete.app.data.*

@Composable
fun VolumeSection(volume: ExternalVolume, exerciseId: String? = null, period: ProgressPeriod = ProgressPeriod.ALL) {
    var expanded by rememberSaveable(exerciseId) { mutableStateOf(false) }
    TonalCard {
        Text("Volumen externo registrado", style = MaterialTheme.typography.titleLarge)
        Text(volume.totals.externalKgReps?.let { "${uiNumber(it)} kg·reps" } ?: "Sin carga externa registrada",
            style = MaterialTheme.typography.headlineSmall)
        Text("${volume.totals.effectiveSets} series efectivas · ${volume.totals.completedReps} reps · ${volume.totals.knownLoadSets} series con carga conocida",
            style = MaterialTheme.typography.bodySmall)
        Text("Σ kg × reps · trabajo completado. Sin kg también hay trabajo.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Ocultar evolución y desglose" else "Ver evolución y desglose") }
        if (expanded) {
            Text("Evolución semanal · desde el lunes", style = MaterialTheme.typography.titleMedium)
            StaticTrendChart(volume.weeks.mapNotNull { week -> week.totals.externalKgReps?.let { ChartValue(week.at, it) } }, "kg·reps")
            ExternalVolumeMetrics.completeWeekChange(volume, period, System.currentTimeMillis())?.let { change ->
                Text("Últimas dos semanas completas: ${if (change.amount > 0) "+" else ""}${uiNumber(change.amount)} kg·reps" +
                    (change.percent?.let { " (${if (it > 0) "+" else ""}${uiNumber(it)} %)" } ?: ""), style = MaterialTheme.typography.bodySmall)
            }
            Text("Carga conocida de sesiones finalizadas; no equivale a esfuerzo ni hipertrofia. Un cambio de volumen no valora el entrenamiento.", style = MaterialTheme.typography.bodySmall)
            if (volume.weeks.any { it.totals.externalKgReps == null }) Text("Semanas sin carga conocida omitidas del gráfico; no son ceros. Los puntos unen solo semanas con carga registrada.", style = MaterialTheme.typography.labelSmall)
            if (exerciseId == null) {
                Text("Carga externa por músculo principal", style = MaterialTheme.typography.titleMedium)
                volume.muscles.forEach { point -> VolumeLine(point.label, point.totals) }
            }
            Text("Por sesión del período", style = MaterialTheme.typography.titleMedium)
            if (volume.sessions.isEmpty()) Text("Sin series registradas en el período.")
            volume.sessions.sortedByDescending { it.at }.forEach { point -> VolumeLine(uiDate(point.at), point.totals) }
        }
    }
}

@Composable
private fun VolumeLine(label: String, value: VolumeTotals) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text((value.externalKgReps?.let { "${uiNumber(it)} kg·reps" } ?: "Sin carga conocida") +
            " · ${value.effectiveSets} series · ${value.completedReps} reps", style = MaterialTheme.typography.bodySmall)
    }
}
