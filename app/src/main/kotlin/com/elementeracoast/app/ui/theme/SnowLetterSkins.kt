package com.elementeracoast.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SnowLetterSurfaceRole {
    AssistantBubble,
    UserBubble,
    ComposerField,
    ComposerButton,
    ActionButton,
    StatusCard,
    DogtalkCard,
    DogtalkField
}

@Composable
fun SnowLetterSurface(
    modifier: Modifier = Modifier,
    role: SnowLetterSurfaceRole,
    fallbackColor: Color,
    fallbackShape: Shape = RoundedCornerShape(18.dp),
    fallbackBorder: BorderStroke? = null,
    fallbackElevation: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val appearance = LocalCoastAppearance.current
    val enabled = role == SnowLetterSurfaceRole.AssistantBubble && appearance.paperMode == CoastPaperMode.Wave
    val surfaceModifier = if (enabled) {
        Modifier.drawWithContent {
            drawQuietReadingPaper()
            drawContent()
        }
    } else {
        Modifier
            .optionalShadow(fallbackElevation, fallbackShape)
            .background(fallbackColor, fallbackShape)
            .then(if (fallbackBorder != null) Modifier.border(fallbackBorder, fallbackShape) else Modifier)
    }

    Box(
        modifier = modifier.then(surfaceModifier),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun snowLetterInnerPadding(role: SnowLetterSurfaceRole): PaddingValues =
    if (role == SnowLetterSurfaceRole.AssistantBubble) {
        PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    } else {
        PaddingValues(0.dp)
    }

@Composable
fun snowLetterSheetContainerColor(): Color = MaterialTheme.colorScheme.background

@Composable
fun snowLetterComposerGlyphColor(fallback: Color): Color = fallback

private fun Modifier.optionalShadow(elevation: Dp, shape: Shape): Modifier =
    if (elevation.value > 0f) shadow(elevation, shape, clip = false) else this

private fun DrawScope.drawQuietReadingPaper() {
    val radius = 19.dp.toPx()
    val shadowOffset = 1.5.dp.toPx()
    val inset = 0.5.dp.toPx()
    drawRoundRect(
        color = Color(0xFFB7C8D8).copy(alpha = .10f),
        topLeft = Offset(shadowOffset, shadowOffset),
        size = Size((size.width - shadowOffset).coerceAtLeast(0f), (size.height - shadowOffset).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(radius, radius)
    )
    drawRoundRect(
        color = Color(0xFFFFFCF6),
        topLeft = Offset(inset, inset),
        size = Size((size.width - inset * 2).coerceAtLeast(0f), (size.height - inset * 2).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(radius, radius)
    )
    drawRoundRect(
        color = Color(0xFFD4DFE8).copy(alpha = .58f),
        topLeft = Offset(inset, inset),
        size = Size((size.width - inset * 2).coerceAtLeast(0f), (size.height - inset * 2).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = 0.8.dp.toPx())
    )
}
