package com.elementeracoast.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepCoast = darkColorScheme(
    primary = Color(0xFFD4AD63),
    onPrimary = Color(0xFF101820),
    background = Color(0xFF07111A),
    onBackground = Color(0xFFECE4D5),
    surface = Color(0xFF0B1823),
    onSurface = Color(0xFFECE4D5),
    surfaceVariant = Color(0xFF102230),
    onSurfaceVariant = Color(0xFF9EAFBA),
    outline = Color(0xFF455966),
    outlineVariant = Color(0xFF253946),
    error = Color(0xFFE7A09A),
    onError = Color(0xFF2A0D0A),
)

private val TideLight = lightColorScheme(
    primary = Color(0xFF8B682B),
    onPrimary = Color(0xFFFFFBF4),
    background = Color(0xFFF4F0E8),
    onBackground = Color(0xFF272B2F),
    surface = Color(0xFFFBF8F1),
    onSurface = Color(0xFF272B2F),
    surfaceVariant = Color(0xFFE9E1D5),
    onSurfaceVariant = Color(0xFF6A645C),
    outline = Color(0xFF9A8D7E),
    outlineVariant = Color(0xFFD7CFC4),
    error = Color(0xFFA04B45),
)

private val NightGold = darkColorScheme(
    primary = Color(0xFFE6BF73),
    onPrimary = Color(0xFF241A08),
    background = Color(0xFF0B0C11),
    onBackground = Color(0xFFF0E4CB),
    surface = Color(0xFF13151C),
    onSurface = Color(0xFFF0E4CB),
    surfaceVariant = Color(0xFF1E202A),
    onSurfaceVariant = Color(0xFFC0B49E),
    outline = Color(0xFF5A554D),
    outlineVariant = Color(0xFF343128),
    error = Color(0xFFE5A19A),
)

@Composable
fun CoastTheme(mode: CoastThemeMode, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        CoastThemeMode.DEEP_COAST -> DeepCoast
        CoastThemeMode.TIDE_LIGHT -> TideLight
        CoastThemeMode.NIGHT_GOLD -> NightGold
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = CoastTypography,
        content = content,
    )
}
