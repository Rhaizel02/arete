package com.arete.app

import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as AreteApplication
            val viewModel: AreteViewModel = viewModel(factory = AreteViewModel.Factory(app))
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            AreteTheme(themeMode) { AreteApp(viewModel) }
        }
    }
}
