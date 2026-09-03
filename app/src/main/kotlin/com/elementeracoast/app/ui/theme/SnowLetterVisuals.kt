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
                .alpha((backgroundAlpha * .92f).coerceIn(0f, .78f)),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_bottom_strip),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(178.dp)
                .alpha((decorationAlpha * .82f).coerceIn(0f, .88f)),
            contentScale = ContentScale.FillWidth
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_wolf_write),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp, y = (-92).dp)
                .size(106.dp)
                .alpha((decorationAlpha * .46f).coerceIn(0f, .54f)),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_snake_paper),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = (-48).dp)
                .size(104.dp)
                .alpha((decorationAlpha * .70f).coerceIn(0f, .76f)),
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
                .alpha((backgroundAlpha * .45f).coerceIn(0f, .42f)),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_bottom_strip),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(132.dp)
                .alpha((decorationAlpha * .46f).coerceIn(0f, .50f)),
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
                .offset(x = 18.dp, y = 76.dp)
                .size(88.dp)
                .alpha((decorationAlpha * .22f).coerceIn(0f, .26f)),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(id = R.drawable.snow_letter_wolf_write),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-12).dp, y = (-16).dp)
                .size(104.dp)
                .alpha((decorationAlpha * .18f).coerceIn(0f, .22f)),
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
    drawRect(PaperCream.copy(alpha = .30f * alpha))
    val step = 74.dp.toPx()
    var y = -step
    var index = 0
    while (y < size.height + step) {
        drawLine(
            color = SnowBlue.copy(alpha = .06f * alpha),
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
    drawRect(SnowBlue.copy(alpha = .08f * alpha), topLeft = Offset(0f, 0f), size = Size(edge, size.height))
    drawRect(SnowBlue.copy(alpha = .07f * alpha), topLeft = Offset(size.width - edge, 0f), size = Size(edge, size.height))
    drawLine(
        color = InkBlue.copy(alpha = .05f * alpha),
        start = Offset(0f, 57.dp.toPx()),
        end = Offset(size.width, 57.dp.toPx()),
        strokeWidth = 1.dp.toPx()
    )
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
        drawPaw(center = center, scale = .62f + index * .018f, color = InkBlue.copy(alpha = (.16f + index * .010f) * alpha))
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
    flakes.forEachIndexed { index, center -> drawSnowflake(center, (5 + index.mod(3) * 2).dp.toPx(), alpha * .30f) }
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
