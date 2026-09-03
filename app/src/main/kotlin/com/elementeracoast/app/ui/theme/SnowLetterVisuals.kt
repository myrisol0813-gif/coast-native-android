package com.elementeracoast.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

@Immutable
data class SnowLetterVisualSettings(
    val chatBackgroundAlpha: Float = .88f,
    val decorationAlpha: Float = .78f,
    val paperTextureAlpha: Float = .36f
)

val LocalSnowLetterVisuals = staticCompositionLocalOf { SnowLetterVisualSettings() }

fun CoastThemePreset.usesSnowLetterDecorations(): Boolean = this == CoastThemePreset.SnowLetter

@Composable
fun SnowLetterChatScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val enabled = LocalCoastAppearance.current.preset.usesSnowLetterDecorations()
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (enabled) SnowLetterPageTemplate(Modifier.fillMaxSize(), forFeature = false)
        content()
    }
}

@Composable
fun SnowLetterFeatureScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val enabled = LocalCoastAppearance.current.preset.usesSnowLetterDecorations()
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (enabled) SnowLetterPageTemplate(Modifier.fillMaxSize(), forFeature = true)
        content()
    }
}

@Composable
private fun SnowLetterPageTemplate(
    modifier: Modifier = Modifier,
    forFeature: Boolean
) {
    val settings = LocalSnowLetterVisuals.current
    Canvas(modifier) {
        val paperAlpha = settings.chatBackgroundAlpha.coerceIn(0f, 1f) * if (forFeature) .96f else .92f
        val decorAlpha = settings.decorationAlpha.coerceIn(0f, 1f)
        val textureAlpha = settings.paperTextureAlpha.coerceIn(0f, 1f)
        drawSnowLetterBackdrop(paperAlpha, decorAlpha, textureAlpha, forFeature)
    }
}

private val InkBlue = Color(0xFF23425F)
private val SnowBlue = Color(0xFFAFC4D8)
private val PaleBlue = Color(0xFFDDEAF3)
private val PaperCream = Color(0xFFFFFEFA)
private val WarmPaper = Color(0xFFFFFBF3)
private val OldGold = Color(0xFFD7A84D)
private val ShadowBlue = Color(0xFFB7C8D8)
private val SnakeDark = Color(0xFF273243)
private val WolfWhite = Color(0xFFFFFAF2)

private fun DrawScope.drawSnowLetterBackdrop(
    paperAlpha: Float,
    decorationAlpha: Float,
    textureAlpha: Float,
    forFeature: Boolean
) {
    drawRect(Color(0xFFF5F9FC))

    val marginX = if (forFeature) 12.dp.toPx() else 10.dp.toPx()
    val marginTop = if (forFeature) 8.dp.toPx() else 6.dp.toPx()
    val marginBottom = if (forFeature) 8.dp.toPx() else 2.dp.toPx()
    val sheet = RectSpec(marginX, marginTop, size.width - marginX, size.height - marginBottom)

    drawTornPaper(sheet.shift(3.dp.toPx(), 5.dp.toPx()), ShadowBlue.copy(alpha = .20f * paperAlpha), Color.Transparent, .5f)
    drawTornPaper(sheet, PaperCream.copy(alpha = .94f * paperAlpha), SnowBlue.copy(alpha = .34f * paperAlpha), 1f)
    drawPaperGrain(sheet, textureAlpha * paperAlpha)
    drawStationeryMarks(sheet, decorationAlpha * paperAlpha, forFeature)
    drawSnowRoad(sheet, decorationAlpha * paperAlpha, forFeature)
    drawPawTrail(sheet, decorationAlpha * paperAlpha)
}

private fun DrawScope.drawTornPaper(rect: RectSpec, fill: Color, stroke: Color, wobbleScale: Float) {
    val path = tornRectPath(rect, wobbleScale)
    drawPath(path, fill)
    if (stroke.alpha > 0f) drawPath(path, stroke, style = Stroke(width = 1.dp.toPx()))
}

private fun DrawScope.tornRectPath(rect: RectSpec, wobbleScale: Float): Path {
    val step = 22.dp.toPx()
    val wobble = 2.6.dp.toPx() * wobbleScale
    val path = Path()
    path.moveTo(rect.left + 10.dp.toPx(), rect.top)
    var x = rect.left + 10.dp.toPx()
    var i = 0
    while (x < rect.right - 10.dp.toPx()) {
        x = (x + step).coerceAtMost(rect.right - 10.dp.toPx())
        path.lineTo(x, rect.top + ((i % 3) - 1) * wobble)
        i += 1
    }
    var y = rect.top + 10.dp.toPx()
    while (y < rect.bottom - 10.dp.toPx()) {
        y = (y + step).coerceAtMost(rect.bottom - 10.dp.toPx())
        path.lineTo(rect.right + ((i % 3) - 1) * wobble, y)
        i += 1
    }
    x = rect.right - 10.dp.toPx()
    while (x > rect.left + 10.dp.toPx()) {
        x = (x - step).coerceAtLeast(rect.left + 10.dp.toPx())
        path.lineTo(x, rect.bottom + ((i % 3) - 1) * wobble)
        i += 1
    }
    y = rect.bottom - 10.dp.toPx()
    while (y > rect.top + 10.dp.toPx()) {
        y = (y - step).coerceAtLeast(rect.top + 10.dp.toPx())
        path.lineTo(rect.left + ((i % 3) - 1) * wobble, y)
        i += 1
    }
    path.close()
    return path
}

private fun DrawScope.drawPaperGrain(rect: RectSpec, alpha: Float) {
    val line = SnowBlue.copy(alpha = .055f * alpha)
    var y = rect.top + 58.dp.toPx()
    var i = 0
    while (y < rect.bottom - 22.dp.toPx()) {
        drawLine(
            color = line,
            start = Offset(rect.left + 14.dp.toPx(), y + (i % 2) * 2.dp.toPx()),
            end = Offset(rect.right - 14.dp.toPx(), y - (i % 3) * 1.5.dp.toPx()),
            strokeWidth = .8.dp.toPx()
        )
        y += 68.dp.toPx()
        i += 1
    }
}

private fun DrawScope.drawStationeryMarks(rect: RectSpec, alpha: Float, forFeature: Boolean) {
    drawPaperclip(Offset(rect.left + 30.dp.toPx(), rect.top + 28.dp.toPx()), alpha)
    drawStamp(Offset(rect.right - 108.dp.toPx(), rect.top + 18.dp.toPx()), alpha)
    if (!forFeature) {
        drawHandwrittenMark(Offset(rect.left + 56.dp.toPx(), rect.top + 132.dp.toPx()), alpha)
    }
    listOf(
        Offset(rect.left + 54.dp.toPx(), rect.top + 182.dp.toPx()),
        Offset(rect.right - 62.dp.toPx(), rect.top + 260.dp.toPx()),
        Offset(rect.left + 92.dp.toPx(), rect.bottom - 300.dp.toPx()),
        Offset(rect.right - 96.dp.toPx(), rect.bottom - 188.dp.toPx())
    ).forEachIndexed { index, center ->
        drawSnowflake(center, (5 + index % 3 * 2).dp.toPx(), alpha * .34f)
    }
}

private fun DrawScope.drawPaperclip(center: Offset, alpha: Float) {
    rotate(degrees = -20f, pivot = center) {
        drawRoundRect(
            color = OldGold.copy(alpha = .55f * alpha),
            topLeft = center + Offset(-7.dp.toPx(), -22.dp.toPx()),
            size = Size(14.dp.toPx(), 44.dp.toPx()),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRoundRect(
            color = OldGold.copy(alpha = .40f * alpha),
            topLeft = center + Offset(-3.dp.toPx(), -15.dp.toPx()),
            size = Size(7.dp.toPx(), 30.dp.toPx()),
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
            style = Stroke(width = 1.3.dp.toPx())
        )
    }
}

private fun DrawScope.drawStamp(topLeft: Offset, alpha: Float) {
    drawRoundRect(
        color = PaleBlue.copy(alpha = .38f * alpha),
        topLeft = topLeft,
        size = Size(70.dp.toPx(), 42.dp.toPx()),
        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
    )
    drawRoundRect(
        color = InkBlue.copy(alpha = .13f * alpha),
        topLeft = topLeft,
        size = Size(70.dp.toPx(), 42.dp.toPx()),
        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawPaw(topLeft + Offset(36.dp.toPx(), 23.dp.toPx()), .62f, InkBlue.copy(alpha = .18f * alpha))
    repeat(4) { i ->
        drawLine(
            color = InkBlue.copy(alpha = .18f * alpha),
            start = topLeft + Offset(-50.dp.toPx(), (8 + i * 7).dp.toPx()),
            end = topLeft + Offset(-8.dp.toPx(), (11 + i * 7).dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawHandwrittenMark(topLeft: Offset, alpha: Float) {
    drawLine(InkBlue.copy(alpha = .15f * alpha), topLeft, topLeft + Offset(96.dp.toPx(), -8.dp.toPx()), 1.dp.toPx())
    drawLine(InkBlue.copy(alpha = .10f * alpha), topLeft + Offset(12.dp.toPx(), 22.dp.toPx()), topLeft + Offset(132.dp.toPx(), 16.dp.toPx()), 1.dp.toPx())
}

private fun DrawScope.drawSnowRoad(rect: RectSpec, alpha: Float, forFeature: Boolean) {
    val base = rect.bottom - if (forFeature) 112.dp.toPx() else 92.dp.toPx()
    val far = Path().apply {
        moveTo(rect.left, rect.bottom)
        lineTo(rect.left, base)
        cubicTo(rect.width * .20f, base - 32.dp.toPx(), rect.width * .44f, base + 6.dp.toPx(), rect.width * .56f, base - 18.dp.toPx())
        cubicTo(rect.width * .76f, base - 48.dp.toPx(), rect.width * .88f, base + 16.dp.toPx(), rect.right, base - 12.dp.toPx())
        lineTo(rect.right, rect.bottom)
        close()
    }
    drawPath(far, PaleBlue.copy(alpha = .42f * alpha))
    val near = Path().apply {
        moveTo(rect.left, rect.bottom)
        lineTo(rect.left, rect.bottom - 54.dp.toPx())
        cubicTo(rect.width * .25f, rect.bottom - 22.dp.toPx(), rect.width * .44f, rect.bottom - 72.dp.toPx(), rect.width * .62f, rect.bottom - 38.dp.toPx())
        cubicTo(rect.width * .82f, rect.bottom - 8.dp.toPx(), rect.width * .92f, rect.bottom - 56.dp.toPx(), rect.right, rect.bottom - 36.dp.toPx())
        lineTo(rect.right, rect.bottom)
        close()
    }
    drawPath(near, Color.White.copy(alpha = .50f * alpha))
    drawWolfPeek(Offset(rect.left + 42.dp.toPx(), rect.bottom - 98.dp.toPx()), .58f, alpha)
    drawSnake(Offset(rect.right - 66.dp.toPx(), rect.bottom - 74.dp.toPx()), .58f, alpha)
}

private fun DrawScope.drawPawTrail(rect: RectSpec, alpha: Float) {
    val points = listOf(
        Offset(rect.left + rect.width * .45f, rect.bottom - 42.dp.toPx()),
        Offset(rect.left + rect.width * .50f, rect.bottom - 78.dp.toPx()),
        Offset(rect.left + rect.width * .45f, rect.bottom - 118.dp.toPx()),
        Offset(rect.left + rect.width * .55f, rect.bottom - 170.dp.toPx()),
        Offset(rect.left + rect.width * .49f, rect.bottom - 230.dp.toPx()),
        Offset(rect.left + rect.width * .57f, rect.bottom - 298.dp.toPx())
    )
    points.forEachIndexed { index, center ->
        drawPaw(center, .62f + index * .015f, InkBlue.copy(alpha = (.12f + index * .008f) * alpha))
    }
}

private fun DrawScope.drawPaw(center: Offset, scale: Float, color: Color) {
    drawCircle(color, 3.8.dp.toPx() * scale, center + Offset(0f, 5.dp.toPx() * scale))
    drawCircle(color, 2.dp.toPx() * scale, center + Offset(-5.dp.toPx() * scale, -1.dp.toPx() * scale))
    drawCircle(color, 2.1.dp.toPx() * scale, center + Offset(-1.7.dp.toPx() * scale, -3.dp.toPx() * scale))
    drawCircle(color, 2.1.dp.toPx() * scale, center + Offset(2.4.dp.toPx() * scale, -2.8.dp.toPx() * scale))
    drawCircle(color, 1.9.dp.toPx() * scale, center + Offset(5.5.dp.toPx() * scale, -.4.dp.toPx() * scale))
}

private fun DrawScope.drawSnowflake(center: Offset, radius: Float, alpha: Float) {
    repeat(6) { index ->
        val angle = Math.PI.toFloat() * index / 3f
        val x = kotlin.math.cos(angle) * radius
        val y = kotlin.math.sin(angle) * radius
        drawLine(SnowBlue.copy(alpha = alpha), center - Offset(x, y), center + Offset(x, y), 1.dp.toPx())
    }
}

private fun DrawScope.drawWolfPeek(center: Offset, scale: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha = .75f * alpha), 34.dp.toPx() * scale, center)
    drawCircle(WolfWhite.copy(alpha = .90f * alpha), 26.dp.toPx() * scale, center)
    drawTriangle(center + Offset(-17.dp.toPx() * scale, -24.dp.toPx() * scale), 10.dp.toPx() * scale, WolfWhite.copy(alpha = .90f * alpha))
    drawTriangle(center + Offset(17.dp.toPx() * scale, -24.dp.toPx() * scale), 10.dp.toPx() * scale, WolfWhite.copy(alpha = .90f * alpha))
    drawCircle(InkBlue.copy(alpha = .55f * alpha), 2.3.dp.toPx() * scale, center + Offset(-7.dp.toPx() * scale, -2.dp.toPx() * scale))
    drawCircle(InkBlue.copy(alpha = .55f * alpha), 2.3.dp.toPx() * scale, center + Offset(7.dp.toPx() * scale, -2.dp.toPx() * scale))
    drawCircle(OldGold.copy(alpha = .28f * alpha), 3.4.dp.toPx() * scale, center + Offset(-12.dp.toPx() * scale, 7.dp.toPx() * scale))
    drawCircle(OldGold.copy(alpha = .28f * alpha), 3.4.dp.toPx() * scale, center + Offset(12.dp.toPx() * scale, 7.dp.toPx() * scale))
}

private fun DrawScope.drawSnake(center: Offset, scale: Float, alpha: Float) {
    drawCircle(Color.White.copy(alpha = .78f * alpha), 36.dp.toPx() * scale, center)
    drawCircle(SnakeDark.copy(alpha = .86f * alpha), 24.dp.toPx() * scale, center + Offset(4.dp.toPx() * scale, 4.dp.toPx() * scale))
    drawCircle(SnakeDark.copy(alpha = .90f * alpha), 18.dp.toPx() * scale, center + Offset(-10.dp.toPx() * scale, -15.dp.toPx() * scale))
    drawTriangle(center + Offset(-18.dp.toPx() * scale, -33.dp.toPx() * scale), 5.dp.toPx() * scale, OldGold.copy(alpha = .70f * alpha))
    drawTriangle(center + Offset(-4.dp.toPx() * scale, -34.dp.toPx() * scale), 5.dp.toPx() * scale, OldGold.copy(alpha = .70f * alpha))
    drawCircle(OldGold.copy(alpha = .70f * alpha), 2.2.dp.toPx() * scale, center + Offset(-14.dp.toPx() * scale, -17.dp.toPx() * scale))
    drawCircle(OldGold.copy(alpha = .70f * alpha), 2.2.dp.toPx() * scale, center + Offset(-4.dp.toPx() * scale, -17.dp.toPx() * scale))
}

private fun DrawScope.drawTriangle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x - radius * .86f, center.y + radius * .76f)
        lineTo(center.x + radius * .86f, center.y + radius * .76f)
        close()
    }
    drawPath(path, color)
}

private data class RectSpec(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    fun shift(dx: Float, dy: Float) = RectSpec(left + dx, top + dy, right + dx, bottom + dy)
}
