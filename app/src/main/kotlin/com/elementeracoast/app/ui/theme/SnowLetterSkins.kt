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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
            drawSnowLetterCutPaper()
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
        PaddingValues(horizontal = 12.dp, vertical = 9.dp)
    } else {
        PaddingValues(0.dp)
    }

@Composable
fun snowLetterSheetContainerColor(): Color = MaterialTheme.colorScheme.background

@Composable
fun snowLetterComposerGlyphColor(fallback: Color): Color = fallback

private fun Modifier.optionalShadow(elevation: Dp, shape: Shape): Modifier =
    if (elevation.value > 0f) shadow(elevation, shape, clip = false) else this

private fun DrawScope.drawSnowLetterCutPaper() {
    val paper = cutPaperPath(offset = Offset.Zero)
    val shadow = cutPaperPath(offset = Offset(1.2.dp.toPx(), 1.4.dp.toPx()))
    drawPath(shadow, Color(0xFFB7C8D8).copy(alpha = .08f))
    drawPath(paper, Color(0xFFFFFCF6).copy(alpha = .96f))
    drawPath(
        path = paper,
        color = Color(0xFFD3DFE9).copy(alpha = .52f),
        style = Stroke(width = 0.75.dp.toPx())
    )
}

private fun DrawScope.cutPaperPath(offset: Offset): Path {
    val inset = 0.8.dp.toPx()
    val cut = 2.0.dp.toPx()
    val w = size.width
    val h = size.height
    val left = inset + offset.x
    val top = inset + offset.y
    val right = (w - inset) + offset.x
    val bottom = (h - inset) + offset.y
    return Path().apply {
        moveTo(left + 18.dp.toPx(), top + cut * .20f)
        lineTo(w * .14f + offset.x, top - cut * .25f)
        lineTo(w * .30f + offset.x, top + cut * .55f)
        lineTo(w * .47f + offset.x, top - cut * .15f)
        lineTo(w * .66f + offset.x, top + cut * .35f)
        lineTo(w * .83f + offset.x, top - cut * .20f)
        lineTo(right - 18.dp.toPx(), top + cut * .25f)
        lineTo(right + cut * .35f, h * .15f + offset.y)
        lineTo(right - cut * .20f, h * .32f + offset.y)
        lineTo(right + cut * .45f, h * .51f + offset.y)
        lineTo(right - cut * .10f, h * .72f + offset.y)
        lineTo(right - 6.dp.toPx(), bottom - 15.dp.toPx())
        lineTo(w * .82f + offset.x, bottom + cut * .25f)
        lineTo(w * .63f + offset.x, bottom - cut * .15f)
        lineTo(w * .44f + offset.x, bottom + cut * .38f)
        lineTo(w * .26f + offset.x, bottom - cut * .22f)
        lineTo(left + 15.dp.toPx(), bottom + cut * .12f)
        lineTo(left - cut * .25f, h * .78f + offset.y)
        lineTo(left + cut * .25f, h * .58f + offset.y)
        lineTo(left - cut * .18f, h * .36f + offset.y)
        lineTo(left + cut * .15f, h * .18f + offset.y)
        close()
    }
}
