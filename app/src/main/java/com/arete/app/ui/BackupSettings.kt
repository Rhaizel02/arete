package com.arete.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arete.app.backup.BackupOutcome

@Composable
fun BackupSettings(viewModel: AreteViewModel, onExport: () -> Unit, onRestore: () -> Unit) {
    val state by viewModel.automaticBackupState.collectAsStateWithLifecycle()
    val pending by viewModel.immediateBackupPending.collectAsStateWithLifecycle()
    val scheduled by viewModel.dailyBackupScheduled.collectAsStateWithLifecycle()
    var notice by rememberSaveable { mutableStateOf(false) }
    var enableAfterSelection by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.selectBackupFolder(uri.toString(), enableAfterSelection)
        enableAfterSelection = false
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshBackupAccess()
        onPauseOrDispose { }
    }
    val busy = pending || state.outcome == BackupOutcome.RUNNING
    fun chooseFolder(enable: Boolean) { enableAfterSelection = enable; notice = true }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("Tus datos")
        SettingSwitch("Backup automático diario", state.enabled) { enabled ->
            if (enabled && state.treeUri == null) chooseFolder(true)
            else viewModel.setAutomaticBackupEnabled(enabled)
        }
        Text(if (state.enabled && scheduled) "Programado diariamente · primera copia cuanto antes"
            else if (state.enabled) "Activado · comprobando programación" else "Backup automático desactivado",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Android decide cuándo ejecutar la tarea; no hay una hora exacta garantizada.", style = MaterialTheme.typography.bodySmall)
        Text("CARPETA DE DESTINO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(state.folderName.ifBlank { "Sin configurar" }, style = MaterialTheme.typography.titleMedium)
        if (state.treeUri != null) Text(Uri.decode(state.treeUri), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SecondaryAction(onClick = { chooseFolder(false) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.treeUri == null) "Elegir carpeta" else "Cambiar carpeta")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
        Text("ÚLTIMA COPIA CORRECTA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(state.lastSuccessAt?.let { android.text.format.DateFormat.format("dd MMM yyyy · HH:mm", java.util.Date(it)).toString() }
            ?: "Todavía no hay una copia verificada", style = MaterialTheme.typography.titleMedium)
        state.lastSuccessAt?.let {
            Text("${if (state.lastSuccessAutomatic) "Automática" else "Manual"} · ${state.lastSuccessFolder}", style = MaterialTheme.typography.bodySmall)
            Text(state.lastSuccessName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(when {
            state.outcome == BackupOutcome.RUNNING -> "Creando copia…"
            pending -> "Copia inmediata pendiente"
            state.outcome == BackupOutcome.SUCCESS -> "✓ Última ejecución verificada"
            state.outcome == BackupOutcome.ERROR -> "Atención · última operación fallida"
            state.outcome == BackupOutcome.INTERRUPTED -> "Ejecución interrumpida"
            else -> "Programar no significa que exista una copia completada"
        }, style = MaterialTheme.typography.labelMedium,
            color = if (state.outcome == BackupOutcome.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        if (state.detail.isNotBlank()) Text(state.detail, style = MaterialTheme.typography.bodySmall)
        PrimaryAction(onClick = { if (state.treeUri == null) chooseFolder(false) else viewModel.createBackupNow() },
            enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Crear copia ahora") }
        Text("Se conservan 7 copias automáticas verificadas de esta instalación. Las manuales no se eliminan automáticamente.", style = MaterialTheme.typography.bodySmall)
        Text("JSON legible, sin cifrado adicional. La sincronización remota depende del proveedor; una carpeta solo en este móvil no protege frente a su pérdida.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
        SecondaryAction(onClick = onExport, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Exportar backup completo") }
        SecondaryAction(onClick = onRestore, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Restaurar backup") }
        Text("Restaurar reemplaza todos los datos locales. Se pedirá confirmación. Los Backup JSON v1 anteriores siguen siendo compatibles.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (notice) AlertDialog(onDismissRequest = { notice = false }, title = { Text("Elige dónde proteger tus datos") },
        text = { Text("Las copias contienen tu histórico y preferencias en JSON legible, sin cifrado adicional. Elige una carpeta que puedas recuperar fuera de Arete. La sincronización remota depende del proveedor elegido y no está garantizada.") },
        confirmButton = { TextButton(onClick = { notice = false; picker.launch(state.treeUri?.let(Uri::parse)) }) { Text("Elegir carpeta") } },
        dismissButton = { TextButton(onClick = { notice = false; enableAfterSelection = false }) { Text("Cancelar") } })
}
