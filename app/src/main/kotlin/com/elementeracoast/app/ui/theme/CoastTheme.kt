package com.elementeracoast.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val CoastColors = darkColorScheme(
    primary = Color(0xFFD4B46A),
    onPrimary = Color(0xFF17140D),
    primaryContainer = Color(0xFF4A3E22),
    onPrimaryContainer = Color(0xFFF7E9BC),
    background = Color(0xFF071018),
    onBackground = Color(0xFFE8EDF0),
    surface = Color(0xFF0C1822),
    onSurface = Color(0xFFE8EDF0),
    surfaceVariant = Color(0xFF152430),
    onSurfaceVariant = Color(0xFFB8C3C9),
    outline = Color(0xFF746748),
    outlineVariant = Color(0xFF263742),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val CoastTypography = Typography(
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp)
)

@Composable
fun CoastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoastColors,
        typography = CoastTypography,
        content = content
    )
}
