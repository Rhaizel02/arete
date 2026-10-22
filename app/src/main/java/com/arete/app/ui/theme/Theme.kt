package com.arete.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.arete.app.R
import com.arete.app.data.ThemeMode

private fun localFont(resource: Int, weight: FontWeight) = Font(resource, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
private val DisplayFont = FontFamily(localFont(R.font.space_grotesk, FontWeight.Normal), localFont(R.font.space_grotesk, FontWeight.Bold), localFont(R.font.space_grotesk, FontWeight.SemiBold))
private val BodyFont = FontFamily(localFont(R.font.hanken_grotesk, FontWeight.Normal), localFont(R.font.hanken_grotesk, FontWeight.Medium), localFont(R.font.hanken_grotesk, FontWeight.SemiBold), localFont(R.font.hanken_grotesk, FontWeight.Bold))
private fun display(size: Int, line: Int) = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = line.sp, fontFeatureSettings = "tnum")
private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = BodyFont, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, fontFeatureSettings = "tnum")
private val AreteTypography = Typography(
    displayLarge = display(40, 44), displayMedium = display(36, 40), displaySmall = display(30, 36),
    headlineLarge = display(28, 34), headlineMedium = display(24, 30), headlineSmall = display(22, 28),
    titleLarge = display(20, 26), titleMedium = body(16, 22, FontWeight.SemiBold), titleSmall = body(14, 20, FontWeight.SemiBold),
    bodyLarge = body(16, 24), bodyMedium = body(14, 20), bodySmall = body(12, 17),
    labelLarge = body(14, 20, FontWeight.SemiBold), labelMedium = body(12, 16, FontWeight.Medium), labelSmall = body(11, 15, FontWeight.Medium)
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB), onPrimary = Color.White, primaryContainer = Color(0xFFDBEAFE), onPrimaryContainer = Color(0xFF123A93),
    secondary = Color(0xFF047857), onSecondary = Color.White, secondaryContainer = Color(0xFFD1FAE5), onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFF995700), background = Color(0xFFF8FAFC), onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC), onSurface = Color(0xFF0F172A), onSurfaceVariant = Color(0xFF526079),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White, surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE8EEF6), surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFF64748B), outlineVariant = Color(0xFFE2E8F0), surfaceVariant = Color(0xFFE2E8F0), surfaceTint = Color(0xFF2563EB)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF), onPrimary = Color(0xFF002A78), primaryContainer = Color(0xFF2563EB), onPrimaryContainer = Color.White,
    secondary = Color(0xFF4EDEA3), onSecondary = Color(0xFF003824), secondaryContainer = Color(0xFF103D33), onSecondaryContainer = Color(0xFF6FFBBE),
    tertiary = Color(0xFFFFB95F), background = Color(0xFF0F131C), onBackground = Color(0xFFDFE2EE),
    surface = Color(0xFF0F131C), onSurface = Color(0xFFDFE2EE), onSurfaceVariant = Color(0xFFC3C6D7),
    surfaceContainerLowest = Color(0xFF0A0E16), surfaceContainerLow = Color(0xFF181C24), surfaceContainer = Color(0xFF1C2028),
    surfaceContainerHigh = Color(0xFF262A33), surfaceContainerHighest = Color(0xFF31353E),
    outline = Color(0xFF8D90A0), outlineVariant = Color(0xFF434655), surfaceVariant = Color(0xFF31353E), surfaceTint = Color(0xFFB4C5FF)
)
val ActionBlue = Color(0xFF2563EB)
object AreteSpace { val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp }

@Composable
fun AreteTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        WindowCompat.getInsetsController((view.context as Activity).window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AreteTypography,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(24.dp)),
        content = content
    )
}
