package com.arete.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.arete.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF315B4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4F1D3),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4D6359),
    secondaryContainer = Color(0xFFD0E8DB),
    tertiary = Color(0xFF3D6373),
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFF7FAF7),
    surfaceVariant = Color(0xFFDEE5E0),
    outline = Color(0xFF707974)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF99D5B8),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF174F3B),
    onPrimaryContainer = Color(0xFFB4F1D3),
    secondary = Color(0xFFB4CCBF),
    secondaryContainer = Color(0xFF364B41),
    tertiary = Color(0xFFA5CDDF),
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
    surfaceVariant = Color(0xFF414844),
    outline = Color(0xFF89928D)
)

@Composable
fun AreteTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
