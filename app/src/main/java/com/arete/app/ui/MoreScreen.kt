@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.arete.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MoreScreen(onOpen: (AppScreen) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Más", style = MaterialTheme.typography.headlineSmall) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                Triple(AppScreen.Catalog, Icons.Default.FitnessCenter, "Ejercicios"),
                Triple(AppScreen.History, Icons.Default.History, "Historial"),
                Triple(AppScreen.Activity, Icons.Default.CalendarMonth, "Actividad"),
                Triple(AppScreen.Settings, Icons.Default.Tune, "Ajustes")
            ).forEach { (screen, icon, label) -> item { TonalCard(onClick = { onOpen(screen) }) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                    Text(label, Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            } } }
        }
    }
}
