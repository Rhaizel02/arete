package com.arete.app

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arete.app.ui.AreteApp
import com.arete.app.ui.AreteViewModel
import com.arete.app.ui.theme.AreteTheme

class MainActivity : ComponentActivity() {
    private var openSessionRequest by mutableIntStateOf(0)
    private fun accept(intent: Intent?) {
        if (intent?.action == com.arete.app.reminders.RestAlarmScheduler.ACTION_OPEN_SESSION) {
            openSessionRequest++
            intent.action = null
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); accept(intent) }
    override fun onStart() { super.onStart(); (application as AreteApplication).onForeground(true) }
    override fun onStop() { (application as AreteApplication).onForeground(false); super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accept(intent)
        enableEdgeToEdge()
        setContent {
            val app = application as AreteApplication
            val viewModel: AreteViewModel = viewModel(factory = AreteViewModel.Factory(app))
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            AreteTheme(themeMode) { AreteApp(viewModel, openSessionRequest) }
        }
    }
}
