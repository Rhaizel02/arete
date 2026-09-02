@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.arete.app.ui

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Date

@Composable
fun HistoryScreen(viewModel: AreteViewModel, onOpenSession: (String) -> Unit) {
    val sessions by viewModel.completedSessions.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Historial") }) }) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Aún no hay sesiones", "Los entrenamientos finalizados aparecerán aquí.")
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions, key = { it.session.id }) { details ->
                    val completedSets = details.exercises.sumOf { it.sets.count { set -> set.isCompleted } }
                    Card(Modifier.fillMaxWidth().clickable { onOpenSession(details.session.id) }) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
