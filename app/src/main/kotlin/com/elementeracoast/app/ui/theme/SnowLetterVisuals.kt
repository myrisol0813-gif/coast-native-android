package com.elementeracoast.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.R
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class SnowLetterVisualSettings(
    val chatBackgroundAlpha: Float = .14f,
    val decorationAlpha: Float = .42f,
    val paperTextureAlpha: Float = .14f
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
        if (enabled) {
            SnowLetterChatAssets(Modifier.fillMaxSize())
            SnowLetterAtmosphereCanvas(Modifier.fillMaxSize())
        }
        content()
    }
}

@Composable
fun SnowLetterFeatureScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val appearance = LocalCoastAppearance.current
    val enabled = appearance.preset.usesSnowLetterDecorations()
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (enabled) SnowLetterFeatureAssets(Modifier.fillMaxSize())
        content()
        if (enabled) SnowLetterFeatureCornerStickers(Modifier.fillMaxSize())
    }
}

@Composable
private fun SnowLetterChatAssets(modifier: Modifier = Modifier) {
    val settings = LocalSnowLetterVisuals.current
    val backgroundAlpha = settings.chatBackgroundAlpha.coerceIn(0f, 1f)
    val decorationAlpha = settings.decorationAlpha.coerceIn(0f, 1f)

    Box(modifier) {
        Image(
            painter = painterResource(id = R.drawable.snow_letter_chat_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha((backgroundAlpha * .26f).coerceIn(0f, .20f)),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_bottom_strip),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(104.dp)
                .alpha((decorationAlpha * .34f).coerceIn(0f, .28f)),
            contentScale = ContentScale.FillWidth
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_wolf_write),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-6).dp, y = (-76).dp)
                .size(72.dp)
                .alpha((decorationAlpha * .24f).coerceIn(0f, .20f)),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_snake_paper),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = (-46).dp)
                .size(70.dp)
                .alpha((decorationAlpha * .30f).coerceIn(0f, .24f)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SnowLetterFeatureAssets(modifier: Modifier = Modifier) {
    val settings = LocalSnowLetterVisuals.current
    val backgroundAlpha = settings.chatBackgroundAlpha.coerceIn(0f, 1f)
    val decorationAlpha = settings.decorationAlpha.coerceIn(0f, 1f)

    Box(modifier) {
        Image(
            painter = painterResource(id = R.drawable.snow_letter_chat_bg),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha((backgroundAlpha * .18f).coerceIn(0f, .14f)),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_bottom_strip),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(88.dp)
                .alpha((decorationAlpha * .18f).coerceIn(0f, .16f)),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun SnowLetterFeatureCornerStickers(modifier: Modifier = Modifier) {
    val settings = LocalSnowLetterVisuals.current
    val decorationAlpha = settings.decorationAlpha.coerceIn(0f, 1f)

    Box(modifier) {
        Image(
            painter = painterResource(id = R.drawable.snow_letter_snake_paper),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = 82.dp)
                .size(56.dp)
                .alpha((decorationAlpha * .12f).coerceIn(0f, .10f)),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_wolf_write),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-10).dp, y = (-12).dp)
                .size(66.dp)
                .alpha((decorationAlpha * .10f).coerceIn(0f, .09f)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SnowLetterAtmosphereCanvas(modifier: Modifier = Modifier) {
    val settings = LocalSnowLetterVisuals.current
    Canvas(modifier) {
        val backgroundAlpha = settings.chatBackgroundAlpha.coerceIn(0f, 1f)
        val decorationAlpha = settings.decorationAlpha.coerceIn(0f, 1f)
        val textureAlpha = settings.paperTextureAlpha.coerceIn(0f, 1f)
        drawPaperWash(textureAlpha * backgroundAlpha)
        drawSnowLetterEdges(backgroundAlpha)
        drawPawTrail(decorationAlpha * backgroundAlpha)
        drawSoftSnowflakes(decorationAlpha * backgroundAlpha)
    }
}

private val InkBlue = Color(0xFF23425F)
private val SnowBlue = Color(0xFFAFC4D8)
private val PaperCream = Color(0xFFFFFEFA)

private fun DrawScope.drawPaperWash(alpha: Float) {
    drawRect(PaperCream.copy(alpha = .18f * alpha))
    val step = 76.dp.toPx()
    var y = -step
    var index = 0
    while (y < size.height + step) {
        drawLine(
            color = SnowBlue.copy(alpha = .035f * alpha),
            start = Offset(-24.dp.toPx(), y + index.mod(3) * 9.dp.toPx()),
            end = Offset(size.width + 24.dp.toPx(), y + 26.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
        y += step
        index += 1
    }
}

private fun DrawScope.drawSnowLetterEdges(alpha: Float) {
    val edge = 7.dp.toPx()
    drawRect(SnowBlue.copy(alpha = .045f * alpha), topLeft = Offset(0f, 0f), size = Size(edge, size.height))
    drawRect(SnowBlue.copy(alpha = .04f * alpha), topLeft = Offset(size.width - edge, 0f), size = Size(edge, size.height))
}

private fun DrawScope.drawPawTrail(alpha: Float) {
    val points = listOf(
        Offset(size.width * .50f, size.height - 76.dp.toPx()),
        Offset(size.width * .46f, size.height - 118.dp.toPx()),
        Offset(size.width * .43f, size.height - 164.dp.toPx()),
        Offset(size.width * .49f, size.height - 218.dp.toPx()),
        Offset(size.width * .55f, size.height - 282.dp.toPx())
    )
    points.forEachIndexed { index, center ->
        drawPaw(center = center, scale = .54f + index * .014f, color = InkBlue.copy(alpha = (.10f + index * .006f) * alpha))
    }
}

private fun DrawScope.drawSoftSnowflakes(alpha: Float) {
    val flakes = listOf(
        Offset(size.width * .12f, size.height * .20f),
        Offset(size.width * .86f, size.height * .24f),
        Offset(size.width * .17f, size.height * .54f),
        Offset(size.width * .79f, size.height * .64f)
    )
    flakes.forEachIndexed { index, center -> drawSnowflake(center, (5 + index.mod(2) * 2).dp.toPx(), alpha * .20f) }
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
