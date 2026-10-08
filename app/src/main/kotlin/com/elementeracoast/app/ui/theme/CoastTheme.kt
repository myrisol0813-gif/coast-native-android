package com.elementeracoast.app.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private const val ZHUQUE_FONT_RESOURCE = "zhuque_fangsong_regular"
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

private fun zhuqueFangsongFamily(context: Context): FontFamily? {
    val fontId = context.resources.getIdentifier(ZHUQUE_FONT_RESOURCE, "font", context.packageName)
    return if (fontId == 0) null else FontFamily(Font(fontId))
}

private fun readingFontFamily(mode: CoastFontMode, context: Context): FontFamily = when (mode) {
    CoastFontMode.Myraes -> zhuqueFangsongFamily(context) ?: FontFamily.Serif
    CoastFontMode.Soft -> FontFamily.SansSerif
    CoastFontMode.Note -> FontFamily.Cursive
}

private fun readingTypography(mode: CoastFontMode, context: Context): Typography {
    val family = readingFontFamily(mode, context)
    val tracking = when (mode) {
        CoastFontMode.Myraes -> 0.18.sp
        CoastFontMode.Soft -> 0.06.sp
        CoastFontMode.Note -> 0.18.sp
    }
    val base = CoastTypography
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family, letterSpacing = tracking),
        titleMedium = base.titleMedium.copy(fontFamily = family, letterSpacing = tracking),
        titleSmall = base.titleSmall.copy(fontFamily = family, letterSpacing = tracking),
        bodyLarge = base.bodyLarge.copy(fontFamily = family, lineHeight = 28.sp, letterSpacing = tracking),
        bodyMedium = base.bodyMedium.copy(fontFamily = family, lineHeight = 23.sp, letterSpacing = tracking),
        bodySmall = base.bodySmall.copy(fontFamily = family, lineHeight = 19.sp, letterSpacing = tracking),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
}

@Composable
fun CoastTheme(
    preset: CoastThemePreset,
    accentHex: String = "",
    userBubbleHex: String = "",
    fontMode: CoastFontMode = CoastFontMode.Myraes,
    paperMode: CoastPaperMode = CoastPaperMode.Wave,
    content: @Composable () -> Unit
) {
    val basePalette = preset.palette()
    val customAccent = parseCoastHex(accentHex)
    val palette = if (customAccent == null) basePalette else basePalette.copy(primary = customAccent, accent = customAccent)
    val colors = palette.toColorScheme()
    val view = LocalView.current
    val context = LocalContext.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = palette.isLight
                isAppearanceLightNavigationBars = palette.isLight
            }
        }
    }

    CompositionLocalProvider(
        LocalCoastAppearance provides CoastAppearance(
            preset = preset,
            palette = palette,
            userBubbleColor = parseCoastHex(userBubbleHex),
            fontMode = fontMode,
            paperMode = paperMode
        )
    ) {
        MaterialTheme(colorScheme = colors, typography = readingTypography(fontMode, context), content = content)
    }
}
