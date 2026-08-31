package com.elementeracoast.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.elementeracoast.app.core.model.CoastThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF6B28),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEFE6),
    onPrimaryContainer = Color(0xFF3A1A0C),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF24252B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF24252B),
    surfaceVariant = Color(0xFFF6F6F6),
    onSurfaceVariant = Color(0xFF8C8C91),
    outline = Color(0xFFDCDCE0),
    outlineVariant = Color(0xFFE9E9EC),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF1F1F2),
    onPrimary = Color(0xFF16171A),
    primaryContainer = Color(0xFF2B2C31),
    onPrimaryContainer = Color(0xFFF5F5F6),
    background = Color(0xFF111216),
    onBackground = Color(0xFFF2F2F3),
    surface = Color(0xFF17181D),
    onSurface = Color(0xFFF2F2F3),
    surfaceVariant = Color(0xFF23242A),
    onSurfaceVariant = Color(0xFFB5B5BA),
    outline = Color(0xFF47484F),
    outlineVariant = Color(0xFF303137),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val GoldColors = darkColorScheme(
    primary = Color(0xFFD8B66A),
    onPrimary = Color(0xFF241B09),
    primaryContainer = Color(0xFF332916),
    onPrimaryContainer = Color(0xFFFFE9B1),
    background = Color(0xFF0B0B0C),
    onBackground = Color(0xFFF4EFE3),
    surface = Color(0xFF121213),
    onSurface = Color(0xFFF4EFE3),
    surfaceVariant = Color(0xFF1D1A14),
    onSurfaceVariant = Color(0xFFC7BDAA),
    outline = Color(0xFF66583A),
    outlineVariant = Color(0xFF302A1D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val CoastTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp)
)

@Composable
fun CoastTheme(
    mode: CoastThemeMode,
    content: @Composable () -> Unit
) {
    val colors = when (mode) {
        CoastThemeMode.Light -> LightColors
        CoastThemeMode.Dark -> DarkColors
        CoastThemeMode.Gold -> GoldColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = mode == CoastThemeMode.Light
                isAppearanceLightNavigationBars = mode == CoastThemeMode.Light
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = CoastTypography,
        content = content
    )
}
