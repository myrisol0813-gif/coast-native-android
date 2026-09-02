package com.elementeracoast.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.elementeracoast.app.core.model.CoastThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF6B28), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEFE6), onPrimaryContainer = Color(0xFF3A1A0C),
    background = Color.White, onBackground = Color(0xFF3A3B40),
    surface = Color.White, onSurface = Color(0xFF3A3B40),
    surfaceVariant = Color(0xFFF7F7F7), onSurfaceVariant = Color(0xFF85858C),
    outline = Color(0xFFDCDCE0), outlineVariant = Color(0xFFECECEF),
    error = Color(0xFFB3261E), onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF1F1F2), onPrimary = Color(0xFF16171A),
    primaryContainer = Color(0xFF2B2C31), onPrimaryContainer = Color(0xFFF5F5F6),
    background = Color(0xFF111216), onBackground = Color(0xFFE8E8EA),
    surface = Color(0xFF17181D), onSurface = Color(0xFFE8E8EA),
    surfaceVariant = Color(0xFF23242A), onSurfaceVariant = Color(0xFFB6B6BC),
    outline = Color(0xFF47484F), outlineVariant = Color(0xFF303137),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005)
)

private val GoldColors = darkColorScheme(
    primary = Color(0xFFD8B66A), onPrimary = Color(0xFF241B09),
    primaryContainer = Color(0xFF332916), onPrimaryContainer = Color(0xFFFFE9B1),
    background = Color(0xFF0B0B0C), onBackground = Color(0xFFE9E2D5),
    surface = Color(0xFF121213), onSurface = Color(0xFFE9E2D5),
    surfaceVariant = Color(0xFF1D1A14), onSurfaceVariant = Color(0xFFC7BDAA),
    outline = Color(0xFF66583A), outlineVariant = Color(0xFF302A1D),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005)
)

private val CoastSans = FontFamily.SansSerif
private val CoastTypography = Typography(
    headlineMedium = TextStyle(fontFamily = CoastSans, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = CoastSans, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    titleMedium = TextStyle(fontFamily = CoastSans, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.18).sp),
    bodyLarge = TextStyle(fontFamily = CoastSans, fontSize = 16.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(fontFamily = CoastSans, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontFamily = CoastSans, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontFamily = CoastSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = CoastSans, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontFamily = CoastSans, fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun CoastTheme(
    mode: CoastThemeMode,
    accentHex: String = "",
    userBubbleHex: String = "",
    content: @Composable () -> Unit
) {
    val base = when (mode) {
        CoastThemeMode.Light -> LightColors
        CoastThemeMode.Dark -> DarkColors
        CoastThemeMode.Gold -> GoldColors
    }
    val accent = parseCoastHex(accentHex)
    val colors = if (accent == null) base else base.copy(primary = accent)
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

    CompositionLocalProvider(
        LocalCoastAppearance provides CoastAppearance(parseCoastHex(userBubbleHex))
    ) {
        MaterialTheme(colorScheme = colors, typography = CoastTypography, content = content)
    }
}
