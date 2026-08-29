package com.elementeracoast.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepCoast = darkColorScheme(
    primary = Color(0xFFE0B45D),
    onPrimary = Color(0xFF17202A),
    background = Color(0xFF07131E),
    onBackground = Color(0xFFF2E9D8),
    surface = Color(0xFF0D1E2D),
    onSurface = Color(0xFFF2E9D8),
    surfaceVariant = Color(0xFF142A3C),
    onSurfaceVariant = Color(0xFFB8C6D2),
    outline = Color(0xFF6F7E8A),
    error = Color(0xFFFFB4AB),
)

private val TideLight = lightColorScheme(
    primary = Color(0xFF87631D),
    onPrimary = Color.White,
    background = Color(0xFFF8F4EC),
    onBackground = Color(0xFF252A2F),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF252A2F),
    surfaceVariant = Color(0xFFEDE4D5),
    onSurfaceVariant = Color(0xFF5F5A52),
)

private val NightGold = darkColorScheme(
    primary = Color(0xFFFFD27A),
    onPrimary = Color(0xFF2C2108),
    background = Color(0xFF0B0B12),
    onBackground = Color(0xFFF5E6C3),
    surface = Color(0xFF171620),
    onSurface = Color(0xFFF5E6C3),
    surfaceVariant = Color(0xFF25212C),
    onSurfaceVariant = Color(0xFFD1C1A6),
)

@Composable
fun CoastTheme(mode: CoastThemeMode, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        CoastThemeMode.DEEP_COAST -> DeepCoast
        CoastThemeMode.TIDE_LIGHT -> TideLight
        CoastThemeMode.NIGHT_GOLD -> NightGold
    }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
}
