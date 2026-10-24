package com.arete.app.ui

import android.Manifest
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arete.app.AreteApplication

@Composable
fun RestAlertSettings(enabled: Boolean, viewModel: AreteViewModel) {
    val context = LocalContext.current
    val scheduler = (context.applicationContext as AreteApplication).restAlarmScheduler
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val exact = remember(revision) { scheduler.exactAllowed() }
    val permission = remember(revision) { scheduler.notificationsAllowed() }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        revision++
        viewModel.setRestAlertsEnabled(true)
        if (!granted) viewModel.showMessage("Avisos sin permiso: el contador sigue funcionando. Puedes permitir notificaciones en Android.")
    }
    SettingSwitch("Avisos de descanso fuera de Arete", enabled) { value ->
        if (value && Build.VERSION.SDK_INT >= 33 && !scheduler.notificationsAllowed()) request.launch(Manifest.permission.POST_NOTIFICATIONS)
        else viewModel.setRestAlertsEnabled(value)
    }
    if (enabled) {
        Text(when {
            !permission -> "Notificaciones bloqueadas en Android. El contador visual sigue disponible."
            !exact -> "Aviso aproximado: Android puede retrasarlo. El contador visual mantiene el tiempo correcto."
            else -> "Aviso puntual autorizado; sujeto a límites de Android y batería."
        }, style = MaterialTheme.typography.bodySmall)
        if (!permission) TextButton(onClick = {
            val intent = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
            context.startActivity(intent)
        }) { Text("Permitir notificaciones en Android") }
        if (!exact && Build.VERSION.SDK_INT >= 31) TextButton(onClick = {
            runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())) }
                .onFailure { viewModel.showMessage("No se pudo abrir el permiso. Se mantiene el aviso aproximado.") }
        }) { Text("Permitir avisos puntuales") }
        Text("Solo para tus descansos. Sin servicio permanente. No funciona tras Forzar detención y no recupera avisos caducados.", style = MaterialTheme.typography.bodySmall)
    }
}
