package com.elementeracoast.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

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
    val enabled = LocalCoastAppearance.current.preset.usesSnowLetterDecorations()
    val settings = LocalSnowLetterVisuals.current
    val textureAlpha = settings.paperTextureAlpha.coerceIn(0f, 1f)
    val shadowShape = snowLetterShadowShape(role, fallbackShape)
    val surfaceModifier = if (enabled) {
        Modifier
            .optionalShadow(snowLetterElevation(role), shadowShape)
            .drawWithContent {
                drawSnowLetterTemplateSurface(role, textureAlpha)
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
    if (!LocalCoastAppearance.current.preset.usesSnowLetterDecorations()) {
        PaddingValues(0.dp)
    } else {
        when (role) {
            SnowLetterSurfaceRole.AssistantBubble -> PaddingValues(horizontal = 20.dp, vertical = 18.dp)
            SnowLetterSurfaceRole.UserBubble -> PaddingValues(horizontal = 14.dp, vertical = 11.dp)
            SnowLetterSurfaceRole.ComposerField -> PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            SnowLetterSurfaceRole.ComposerButton -> PaddingValues(0.dp)
            SnowLetterSurfaceRole.ActionButton -> PaddingValues(0.dp)
            SnowLetterSurfaceRole.StatusCard -> PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            SnowLetterSurfaceRole.DogtalkCard -> PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            SnowLetterSurfaceRole.DogtalkField -> PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        }
    }

private fun Modifier.optionalShadow(elevation: Dp, shape: Shape): Modifier =
    if (elevation.value > 0f) shadow(elevation, shape, clip = false) else this

private fun snowLetterShadowShape(role: SnowLetterSurfaceRole, fallback: Shape): Shape = when (role) {
    SnowLetterSurfaceRole.ActionButton -> RoundedCornerShape(50)
    SnowLetterSurfaceRole.ComposerButton -> RoundedCornerShape(50)
    SnowLetterSurfaceRole.ComposerField -> RoundedCornerShape(32.dp)
    SnowLetterSurfaceRole.DogtalkField -> RoundedCornerShape(22.dp)
    else -> fallback
}

private fun snowLetterElevation(role: SnowLetterSurfaceRole): Dp = when (role) {
    SnowLetterSurfaceRole.AssistantBubble -> 3.dp
    SnowLetterSurfaceRole.UserBubble -> 2.dp
    SnowLetterSurfaceRole.ComposerField -> 2.dp
    SnowLetterSurfaceRole.ComposerButton -> 2.dp
    SnowLetterSurfaceRole.ActionButton -> 1.dp
    SnowLetterSurfaceRole.StatusCard -> 2.dp
    SnowLetterSurfaceRole.DogtalkCard -> 2.dp
    SnowLetterSurfaceRole.DogtalkField -> 1.dp
}

private fun DrawScope.drawSnowLetterTemplateSurface(role: SnowLetterSurfaceRole, textureAlpha: Float) {
    if (role == SnowLetterSurfaceRole.ActionButton || role == SnowLetterSurfaceRole.ComposerButton) {
        drawRoundStamp(role, textureAlpha)
        return
    }

    if (size.width < 28.dp.toPx() || size.height < 24.dp.toPx()) {
        drawRoundStamp(role, textureAlpha)
        return
    }

    val shadow = tornPath(2.dp.toPx(), 3.dp.toPx(), size.width - 1.dp.toPx(), size.height, role)
    drawPath(shadow, Color(0xFFB7C8D8).copy(alpha = .18f))

    val paper = tornPath(0f, 0f, size.width - 3.dp.toPx(), size.height - 3.dp.toPx(), role)
    drawPath(paper, snowLetterPaperColor(role).copy(alpha = snowLetterPaperAlpha(role)))
    drawPath(paper, snowLetterEdgeColor(role), style = Stroke(width = 1.dp.toPx()))

    drawSubtlePaperLines(role, textureAlpha)
    drawCornerTape(role)
    drawRoleDetails(role)
}

private fun DrawScope.tornPath(left: Float, top: Float, right: Float, bottom: Float, role: SnowLetterSurfaceRole): Path {
    val isStrip = role == SnowLetterSurfaceRole.ComposerField || role == SnowLetterSurfaceRole.StatusCard || role == SnowLetterSurfaceRole.DogtalkCard || role == SnowLetterSurfaceRole.DogtalkField
    val step = if (isStrip) 18.dp.toPx() else 22.dp.toPx()
    val wobble = if (isStrip) 1.6.dp.toPx() else 2.4.dp.toPx()
    val safeLeft = left + 8.dp.toPx()
    val safeRight = right - 8.dp.toPx()
    val safeTop = top + 5.dp.toPx()
    val safeBottom = bottom - 5.dp.toPx()
    val path = Path()
    path.moveTo(safeLeft, top + wobble)
    var x = safeLeft
    var i = 0
    while (x < safeRight) {
        x = (x + step).coerceAtMost(safeRight)
        path.lineTo(x, top + ((i % 3) - 1) * wobble)
        i += 1
    }
    var y = safeTop
    while (y < safeBottom) {
        y = (y + step).coerceAtMost(safeBottom)
        path.lineTo(right + ((i % 3) - 1) * wobble, y)
        i += 1
    }
    x = safeRight
    while (x > safeLeft) {
        x = (x - step).coerceAtLeast(safeLeft)
        path.lineTo(x, bottom + ((i % 3) - 1) * wobble)
        i += 1
    }
    y = safeBottom
    while (y > safeTop) {
        y = (y - step).coerceAtLeast(safeTop)
        path.lineTo(left + ((i % 3) - 1) * wobble, y)
        i += 1
    }
    path.close()
    return path
}

private fun DrawScope.drawRoundStamp(role: SnowLetterSurfaceRole, textureAlpha: Float) {
    val radius = min(size.width, size.height) / 2f
    val base = snowLetterPaperColor(role).copy(alpha = .88f)
    drawRoundRect(base, size = size, cornerRadius = CornerRadius(radius, radius))
    drawRoundRect(
        color = snowLetterEdgeColor(role).copy(alpha = .72f),
        size = size,
        cornerRadius = CornerRadius(radius, radius),
        style = Stroke(width = 1.dp.toPx())
    )
    if (textureAlpha > .2f && size.width > 38.dp.toPx()) {
        drawCircle(Color(0xFFD7A84D).copy(alpha = .13f * textureAlpha), 4.dp.toPx(), Offset(size.width * .72f, size.height * .30f))
    }
}

private fun DrawScope.drawSubtlePaperLines(role: SnowLetterSurfaceRole, textureAlpha: Float) {
    val baseAlpha = when (role) {
        SnowLetterSurfaceRole.AssistantBubble -> .10f
        SnowLetterSurfaceRole.UserBubble -> .07f
        SnowLetterSurfaceRole.ComposerField, SnowLetterSurfaceRole.DogtalkField -> .045f
        SnowLetterSurfaceRole.StatusCard, SnowLetterSurfaceRole.DogtalkCard -> .035f
        else -> 0f
    }
    if (baseAlpha <= 0f) return
    val color = Color(0xFFAFC4D8).copy(alpha = baseAlpha * (.65f + textureAlpha))
    val step = if (role == SnowLetterSurfaceRole.ComposerField || role == SnowLetterSurfaceRole.DogtalkField) 20.dp.toPx() else 34.dp.toPx()
    var y = step * .86f
    var index = 0
    while (y < size.height - 8.dp.toPx()) {
        drawLine(
            color = color,
            start = Offset(18.dp.toPx(), y + (index % 2) * .6.dp.toPx()),
            end = Offset(size.width - 18.dp.toPx(), y - (index % 3) * .5.dp.toPx()),
            strokeWidth = .8.dp.toPx()
        )
        y += step
        index += 1
    }
}

private fun DrawScope.drawCornerTape(role: SnowLetterSurfaceRole) {
    if (role != SnowLetterSurfaceRole.AssistantBubble && role != SnowLetterSurfaceRole.StatusCard && role != SnowLetterSurfaceRole.DogtalkCard) return
    val tapeColor = Color(0xFFEBDDC9).copy(alpha = .34f)
    rotate(degrees = -8f, pivot = Offset(size.width - 24.dp.toPx(), 18.dp.toPx())) {
        drawRoundRect(
            color = tapeColor,
            topLeft = Offset(size.width - 52.dp.toPx(), 5.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(38.dp.toPx(), 13.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
    }
}

private fun DrawScope.drawRoleDetails(role: SnowLetterSurfaceRole) {
    when (role) {
        SnowLetterSurfaceRole.AssistantBubble -> {
            drawPawMini(Offset(size.width - 28.dp.toPx(), size.height - 18.dp.toPx()), .55f, Color(0xFF23425F).copy(alpha = .10f))
            drawCircle(Color(0xFFD7A84D).copy(alpha = .30f), 4.dp.toPx(), Offset(18.dp.toPx(), 17.dp.toPx()))
        }
        SnowLetterSurfaceRole.UserBubble -> {
            drawCircle(Color(0xFFD7A84D).copy(alpha = .22f), 4.dp.toPx(), Offset(size.width - 18.dp.toPx(), 15.dp.toPx()))
        }
        SnowLetterSurfaceRole.StatusCard, SnowLetterSurfaceRole.DogtalkCard -> {
            drawPawMini(Offset(size.width - 34.dp.toPx(), size.height * .52f), .62f, Color(0xFF23425F).copy(alpha = .09f))
        }
        else -> Unit
    }
}

private fun snowLetterPaperColor(role: SnowLetterSurfaceRole): Color = when (role) {
    SnowLetterSurfaceRole.UserBubble -> Color(0xFFF7FBFF)
    SnowLetterSurfaceRole.ComposerField -> Color(0xFFFFFEFA)
    SnowLetterSurfaceRole.ComposerButton -> Color(0xFFFFFEFA)
    SnowLetterSurfaceRole.ActionButton -> Color(0xFFFFFEFA)
    SnowLetterSurfaceRole.StatusCard -> Color(0xFFF9FCFF)
    SnowLetterSurfaceRole.DogtalkCard -> Color(0xFFF9FCFF)
    else -> Color(0xFFFFFCF5)
}

private fun snowLetterPaperAlpha(role: SnowLetterSurfaceRole): Float = when (role) {
    SnowLetterSurfaceRole.AssistantBubble -> .93f
    SnowLetterSurfaceRole.UserBubble -> .88f
    SnowLetterSurfaceRole.StatusCard -> .82f
    SnowLetterSurfaceRole.DogtalkCard -> .82f
    SnowLetterSurfaceRole.ComposerField -> .86f
    SnowLetterSurfaceRole.DogtalkField -> .82f
    else -> .88f
}

private fun snowLetterEdgeColor(role: SnowLetterSurfaceRole): Color = when (role) {
    SnowLetterSurfaceRole.UserBubble -> Color(0xFFE3C690).copy(alpha = .48f)
    SnowLetterSurfaceRole.ComposerField, SnowLetterSurfaceRole.DogtalkField -> Color(0xFFD0DFEB).copy(alpha = .58f)
    else -> Color(0xFFC9D9E6).copy(alpha = .58f)
}

private fun DrawScope.drawPawMini(center: Offset, scale: Float, color: Color) {
    drawCircle(color, 3.5.dp.toPx() * scale, center + Offset(0f, 4.5.dp.toPx() * scale))
    drawCircle(color, 1.8.dp.toPx() * scale, center + Offset(-4.5.dp.toPx() * scale, -1.dp.toPx() * scale))
    drawCircle(color, 1.9.dp.toPx() * scale, center + Offset(-1.4.dp.toPx() * scale, -3.dp.toPx() * scale))
    drawCircle(color, 1.9.dp.toPx() * scale, center + Offset(2.2.dp.toPx() * scale, -2.8.dp.toPx() * scale))
    drawCircle(color, 1.7.dp.toPx() * scale, center + Offset(5.dp.toPx() * scale, -.6.dp.toPx() * scale))
}
