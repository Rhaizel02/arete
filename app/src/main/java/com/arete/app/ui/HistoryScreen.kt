@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Date

@Composable
fun HistoryScreen(viewModel: AreteViewModel, onOpenSession: (String) -> Unit, onBack: () -> Unit) {
    val sessions by viewModel.completedSessions.collectAsStateWithLifecycle()
    Scaffold(topBar = { ScreenHeader("Historial", onBack, "ENTRENAMIENTOS") }) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Aún no hay sesiones", "Los entrenamientos finalizados aparecerán aquí.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { Text("${sessions.size} ${if (sessions.size == 1) "sesión completada" else "sesiones completadas"}", Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(sessions, key = { it.session.id }) { details ->
                    val completedSets = details.exercises.sumOf { it.sets.count { set -> set.isCompleted } }
                    EditorialRow(onClick = { onOpenSession(details.session.id) }) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.secondary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(details.session.workoutNameSnapshot, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(details.session.routineNameSnapshot ?: "Entrenamiento libre", color = MaterialTheme.colorScheme.primary)
                            Text(
                                "${formatSessionDate(details.session.completedAt ?: details.session.startedAt)} · $completedSets series",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSessionDate(timestamp: Long): String =
    DateFormat.format("dd MMM yyyy · HH:mm", Date(timestamp)).toString()
