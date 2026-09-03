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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class SnowLetterVisualSettings(
    val chatBackgroundAlpha: Float = .28f,
    val decorationAlpha: Float = .72f,
    val paperTextureAlpha: Float = .22f
)

val LocalSnowLetterVisuals = staticCompositionLocalOf { SnowLetterVisualSettings() }

fun CoastThemePreset.usesSnowLetterDecorations(): Boolean = this == CoastThemePreset.SnowLetter

@Composable
fun SnowLetterChatScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val appearance = LocalCoastAppearance.current
    val enabled = appearance.preset.usesSnowLetterDecorations()
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (enabled) SnowLetterChatCanvas(Modifier.fillMaxSize())
        content()
    }
}

@Composable
private fun SnowLetterChatCanvas(modifier: Modifier = Modifier) {
    val settings = LocalSnowLetterVisuals.current
    Canvas(modifier) {
        val backgroundAlpha = settings.chatBackgroundAlpha.coerceIn(0f, 1f)
        val decorationAlpha = settings.decorationAlpha.coerceIn(0f, 1f)
        val textureAlpha = settings.paperTextureAlpha.coerceIn(0f, 1f)
        drawPaperWash(textureAlpha * backgroundAlpha)
        drawSnowLetterEdges(backgroundAlpha)
        drawBottomSnowRoad(decorationAlpha * backgroundAlpha)
        drawPawTrail(decorationAlpha * backgroundAlpha)
        drawSoftSnowflakes(decorationAlpha * backgroundAlpha)
        drawCornerStickers(decorationAlpha * backgroundAlpha)
    }
}

private val InkBlue = Color(0xFF23425F)
private val SnowBlue = Color(0xFFAFC4D8)
private val PaleBlue = Color(0xFFDDEAF3)
private val PaperCream = Color(0xFFFFFEFA)
private val OldGold = Color(0xFFD7A84D)
private val SnakeDark = Color(0xFF273243)
private val SnakeBelly = Color(0xFFF4E4C9)
private val WolfWhite = Color(0xFFFFFAF2)
private val WolfEar = Color(0xFFF4CFA1)
private val StickerWhite = Color(0xFFFFFFFF)

private fun DrawScope.drawPaperWash(alpha: Float) {
    drawRect(PaperCream.copy(alpha = .52f * alpha))
    val step = 74.dp.toPx()
    var y = -step
    var index = 0
    while (y < size.height + step) {
        drawLine(
            color = SnowBlue.copy(alpha = .08f * alpha),
            start = Offset(-24.dp.toPx(), y + index.mod(3) * 9.dp.toPx()),
            end = Offset(size.width + 24.dp.toPx(), y + 26.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        y += step
        index += 1
    }
}

private fun DrawScope.drawSnowLetterEdges(alpha: Float) {
    val edge = 9.dp.toPx()
    drawRect(SnowBlue.copy(alpha = .14f * alpha), topLeft = Offset(0f, 0f), size = Size(edge, size.height))
    drawRect(SnowBlue.copy(alpha = .12f * alpha), topLeft = Offset(size.width - edge, 0f), size = Size(edge, size.height))
    drawLine(
        color = InkBlue.copy(alpha = .08f * alpha),
        start = Offset(0f, 57.dp.toPx()),
        end = Offset(size.width, 57.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )
}

private fun DrawScope.drawBottomSnowRoad(alpha: Float) {
    val base = size.height - 160.dp.toPx()
    val nearBase = size.height - 96.dp.toPx()
    val far = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, base)
        cubicTo(size.width * .18f, base - 42.dp.toPx(), size.width * .36f, base + 10.dp.toPx(), size.width * .52f, base - 20.dp.toPx())
        cubicTo(size.width * .70f, base - 54.dp.toPx(), size.width * .82f, base + 18.dp.toPx(), size.width, base - 18.dp.toPx())
        lineTo(size.width, size.height)
        close()
    }
    val near = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, nearBase)
        cubicTo(size.width * .24f, nearBase + 12.dp.toPx(), size.width * .38f, nearBase - 36.dp.toPx(), size.width * .58f, nearBase - 6.dp.toPx())
        cubicTo(size.width * .76f, nearBase + 24.dp.toPx(), size.width * .86f, nearBase - 26.dp.toPx(), size.width, nearBase)
        lineTo(size.width, size.height)
        close()
    }
    drawPath(far, PaleBlue.copy(alpha = .55f * alpha))
    drawPath(near, StickerWhite.copy(alpha = .72f * alpha))
    drawLine(SnowBlue.copy(alpha = .34f * alpha), Offset(0f, base), Offset(size.width, base - 12.dp.toPx()), 1.dp.toPx())
}

private fun DrawScope.drawPawTrail(alpha: Float) {
    val points = listOf(
        Offset(size.width * .50f, size.height - 66.dp.toPx()),
        Offset(size.width * .46f, size.height - 102.dp.toPx()),
        Offset(size.width * .43f, size.height - 140.dp.toPx()),
        Offset(size.width * .49f, size.height - 180.dp.toPx()),
        Offset(size.width * .55f, size.height - 226.dp.toPx()),
        Offset(size.width * .52f, size.height - 282.dp.toPx()),
        Offset(size.width * .58f, size.height - 350.dp.toPx())
    )
    points.forEachIndexed { index, center ->
        drawPaw(center = center, scale = .62f + index * .018f, color = InkBlue.copy(alpha = (.18f + index * .012f) * alpha))
    }
}

private fun DrawScope.drawSoftSnowflakes(alpha: Float) {
    val flakes = listOf(
        Offset(size.width * .11f, size.height * .18f),
        Offset(size.width * .86f, size.height * .22f),
        Offset(size.width * .17f, size.height * .53f),
        Offset(size.width * .79f, size.height * .61f),
        Offset(size.width * .29f, size.height * .73f)
    )
    flakes.forEachIndexed { index, center -> drawSnowflake(center, (5 + index.mod(3) * 2).dp.toPx(), alpha * .34f) }
}

private fun DrawScope.drawCornerStickers(alpha: Float) {
    drawWolfPeek(Offset(32.dp.toPx(), size.height - 224.dp.toPx()), .70f, alpha)
    drawSnake(Offset(size.width - 66.dp.toPx(), size.height - 114.dp.toPx()), .76f, alpha)
    drawPostage(Offset(size.width - 82.dp.toPx(), 22.dp.toPx()), alpha)
}

private fun DrawScope.drawPaw(center: Offset, scale: Float, color: Color) {
    val pad = 1.dp.toPx() * scale
    drawCircle(color, 3.8.dp.toPx() * scale, center + Offset(0f, 5.dp.toPx() * scale))
    drawCircle(color, 2.dp.toPx() * scale, center + Offset(-5.dp.toPx() * scale, -1.dp.toPx() * scale + pad))
    drawCircle(color, 2.1.dp.toPx() * scale, center + Offset(-1.7.dp.toPx() * scale, -3.dp.toPx() * scale))
    drawCircle(color, 2.1.dp.toPx() * scale, center + Offset(2.4.dp.toPx() * scale, -2.8.dp.toPx() * scale))
    drawCircle(color, 1.9.dp.toPx() * scale, center + Offset(5.5.dp.toPx() * scale, -.4.dp.toPx() * scale))
}

private fun DrawScope.drawSnowflake(center: Offset, radius: Float, alpha: Float) {
    repeat(6) { index ->
        val angle = Math.PI.toFloat() * index / 3f
        drawLine(
            color = SnowBlue.copy(alpha = alpha),
            start = center - Offset(cos(angle) * radius, sin(angle) * radius),
            end = center + Offset(cos(angle) * radius, sin(angle) * radius),
            strokeWidth = 1.dp.toPx()
        )
    }
}

private fun DrawScope.drawWolfPeek(topLeft: Offset, scale: Float, alpha: Float) {
    val head = topLeft + Offset(30.dp.toPx() * scale, 30.dp.toPx() * scale)
    drawCircle(StickerWhite.copy(alpha = .95f * alpha), 34.dp.toPx() * scale, head)
    drawCircle(WolfWhite.copy(alpha = alpha), 26.dp.toPx() * scale, head)
    drawTriangle(head + Offset(-17.dp.toPx() * scale, -24.dp.toPx() * scale), 10.dp.toPx() * scale, WolfWhite.copy(alpha = alpha))
    drawTriangle(head + Offset(17.dp.toPx() * scale, -24.dp.toPx() * scale), 10.dp.toPx() * scale, WolfWhite.copy(alpha = alpha))
    drawTriangle(head + Offset(-17.dp.toPx() * scale, -22.dp.toPx() * scale), 5.dp.toPx() * scale, WolfEar.copy(alpha = .75f * alpha))
    drawTriangle(head + Offset(17.dp.toPx() * scale, -22.dp.toPx() * scale), 5.dp.toPx() * scale, WolfEar.copy(alpha = .75f * alpha))
    drawCircle(InkBlue.copy(alpha = .72f * alpha), 2.3.dp.toPx() * scale, head + Offset(-7.dp.toPx() * scale, -2.dp.toPx() * scale))
    drawCircle(InkBlue.copy(alpha = .72f * alpha), 2.3.dp.toPx() * scale, head + Offset(7.dp.toPx() * scale, -2.dp.toPx() * scale))
    drawCircle(OldGold.copy(alpha = .40f * alpha), 3.4.dp.toPx() * scale, head + Offset(-12.dp.toPx() * scale, 7.dp.toPx() * scale))
    drawCircle(OldGold.copy(alpha = .40f * alpha), 3.4.dp.toPx() * scale, head + Offset(12.dp.toPx() * scale, 7.dp.toPx() * scale))
}

private fun DrawScope.drawSnake(center: Offset, scale: Float, alpha: Float) {
    drawCircle(StickerWhite.copy(alpha = .95f * alpha), 42.dp.toPx() * scale, center)
    drawCircle(SnakeDark.copy(alpha = alpha), 27.dp.toPx() * scale, center + Offset(4.dp.toPx() * scale, 3.dp.toPx() * scale))
    drawCircle(SnakeDark.copy(alpha = alpha), 20.dp.toPx() * scale, center + Offset(-10.dp.toPx() * scale, -17.dp.toPx() * scale))
    drawOval(
        color = SnakeBelly.copy(alpha = .95f * alpha),
        topLeft = center + Offset(-8.dp.toPx() * scale, -5.dp.toPx() * scale),
        size = Size(11.dp.toPx() * scale, 34.dp.toPx() * scale)
    )
    drawTriangle(center + Offset(-19.dp.toPx() * scale, -38.dp.toPx() * scale), 6.dp.toPx() * scale, OldGold.copy(alpha = alpha))
    drawTriangle(center + Offset(-3.dp.toPx() * scale, -39.dp.toPx() * scale), 6.dp.toPx() * scale, OldGold.copy(alpha = alpha))
    drawCircle(OldGold.copy(alpha = alpha), 2.4.dp.toPx() * scale, center + Offset(-15.dp.toPx() * scale, -19.dp.toPx() * scale))
    drawCircle(OldGold.copy(alpha = alpha), 2.4.dp.toPx() * scale, center + Offset(-4.dp.toPx() * scale, -19.dp.toPx() * scale))
    drawLine(Color(0xFFFF7E7A).copy(alpha = alpha), center + Offset(-24.dp.toPx() * scale, -12.dp.toPx() * scale), center + Offset(-34.dp.toPx() * scale, -10.dp.toPx() * scale), 2.dp.toPx() * scale)
    drawLine(Color(0xFFFF7E7A).copy(alpha = alpha), center + Offset(-34.dp.toPx() * scale, -10.dp.toPx() * scale), center + Offset(-38.dp.toPx() * scale, -14.dp.toPx() * scale), 1.4.dp.toPx() * scale)
    drawLine(Color(0xFFFF7E7A).copy(alpha = alpha), center + Offset(-34.dp.toPx() * scale, -10.dp.toPx() * scale), center + Offset(-38.dp.toPx() * scale, -6.dp.toPx() * scale), 1.4.dp.toPx() * scale)
    drawCircle(OldGold.copy(alpha = .70f * alpha), 4.dp.toPx() * scale, center + Offset(20.dp.toPx() * scale, 21.dp.toPx() * scale))
}

private fun DrawScope.drawPostage(topLeft: Offset, alpha: Float) {
    drawRoundRect(
        color = PaleBlue.copy(alpha = .48f * alpha),
        topLeft = topLeft,
        size = Size(56.dp.toPx(), 34.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
    )
    drawPaw(topLeft + Offset(29.dp.toPx(), 18.dp.toPx()), .5f, InkBlue.copy(alpha = .20f * alpha))
    repeat(4) { i ->
        drawLine(
            color = InkBlue.copy(alpha = .18f * alpha),
            start = topLeft + Offset(-42.dp.toPx(), (6 + i * 6).dp.toPx()),
            end = topLeft + Offset(-5.dp.toPx(), (8 + i * 6).dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
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
