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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.R
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
    val shape = if (enabled) snowLetterShape(role, fallbackShape) else fallbackShape
    val paperPainter = if (enabled) snowLetterSurfaceAsset(role)?.let { painterResource(id = it) } else null
    val stickerPainter = if (enabled) snowLetterStickerAsset(role)?.let { painterResource(id = it) } else null
    val surfaceModifier = if (enabled) {
        Modifier
            .optionalShadow(snowLetterElevation(role), shape)
            .clip(shape)
            .drawWithContent {
                drawRect(snowLetterBaseColor(role).copy(alpha = snowLetterBaseAlpha(role)))
                if (paperPainter != null) {
                    with(paperPainter) {
                        draw(size = size, alpha = snowLetterPaperAssetAlpha(role, textureAlpha))
                    }
                }
                drawSnowLetterComponentSkin(role, textureAlpha, drawBase = false)
                if (stickerPainter != null) {
                    drawSnowLetterSticker(stickerPainter, role, settings.decorationAlpha.coerceIn(0f, 1f))
                }
                drawContent()
            }
            .border(BorderStroke(1.dp, snowLetterBorderColor(role)), shape)
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
            SnowLetterSurfaceRole.AssistantBubble -> PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            SnowLetterSurfaceRole.UserBubble -> PaddingValues(horizontal = 1.dp, vertical = 1.dp)
            SnowLetterSurfaceRole.ComposerField -> PaddingValues(0.dp)
            SnowLetterSurfaceRole.ComposerButton -> PaddingValues(0.dp)
            SnowLetterSurfaceRole.ActionButton -> PaddingValues(0.dp)
            SnowLetterSurfaceRole.StatusCard -> PaddingValues(0.dp)
            SnowLetterSurfaceRole.DogtalkCard -> PaddingValues(0.dp)
            SnowLetterSurfaceRole.DogtalkField -> PaddingValues(0.dp)
        }
    }

private fun Modifier.optionalShadow(elevation: Dp, shape: Shape): Modifier =
    if (elevation.value > 0f) shadow(elevation, shape, clip = false) else this

private fun snowLetterShape(role: SnowLetterSurfaceRole, fallback: Shape): Shape = when (role) {
    SnowLetterSurfaceRole.ActionButton -> RoundedCornerShape(11.dp)
    SnowLetterSurfaceRole.ComposerButton -> RoundedCornerShape(18.dp)
    else -> fallback
}

private fun snowLetterElevation(role: SnowLetterSurfaceRole): Dp = when (role) {
    SnowLetterSurfaceRole.AssistantBubble -> 1.dp
    SnowLetterSurfaceRole.UserBubble -> 2.dp
    SnowLetterSurfaceRole.ComposerField -> 1.dp
    SnowLetterSurfaceRole.ComposerButton -> 1.dp
    SnowLetterSurfaceRole.ActionButton -> 0.dp
    SnowLetterSurfaceRole.StatusCard -> 1.dp
    SnowLetterSurfaceRole.DogtalkCard -> 1.dp
    SnowLetterSurfaceRole.DogtalkField -> 0.dp
}

private fun snowLetterSurfaceAsset(role: SnowLetterSurfaceRole): Int? = when (role) {
    SnowLetterSurfaceRole.StatusCard -> R.drawable.snow_letter_paper_card
    SnowLetterSurfaceRole.DogtalkCard -> R.drawable.snow_letter_paper_card
    else -> null
}

private fun snowLetterStickerAsset(role: SnowLetterSurfaceRole): Int? = when (role) {
    SnowLetterSurfaceRole.StatusCard -> R.drawable.snow_letter_snake_paper
    SnowLetterSurfaceRole.DogtalkCard -> R.drawable.snow_letter_wolf_write
    else -> null
}

private fun snowLetterBaseColor(role: SnowLetterSurfaceRole): Color = when (role) {
    SnowLetterSurfaceRole.AssistantBubble -> Color(0xFFFFFEFA)
    SnowLetterSurfaceRole.UserBubble -> Color(0xFFFFFBF3)
    SnowLetterSurfaceRole.ComposerField -> Color(0xFFFFFFFF)
    SnowLetterSurfaceRole.ComposerButton -> Color(0xFFF7FBFF)
    SnowLetterSurfaceRole.ActionButton -> Color(0xFFFFFFFF)
    SnowLetterSurfaceRole.StatusCard -> Color(0xFFF8FCFF)
    SnowLetterSurfaceRole.DogtalkCard -> Color(0xFFF7FBFF)
    SnowLetterSurfaceRole.DogtalkField -> Color(0xFFFFFEFA)
}

private fun snowLetterBaseAlpha(role: SnowLetterSurfaceRole): Float = when (role) {
    SnowLetterSurfaceRole.AssistantBubble -> .92f
    SnowLetterSurfaceRole.UserBubble -> .88f
    SnowLetterSurfaceRole.ComposerField -> .82f
    SnowLetterSurfaceRole.ComposerButton -> .72f
    SnowLetterSurfaceRole.ActionButton -> .66f
    SnowLetterSurfaceRole.StatusCard -> .78f
    SnowLetterSurfaceRole.DogtalkCard -> .78f
    SnowLetterSurfaceRole.DogtalkField -> .84f
}

private fun snowLetterPaperAssetAlpha(role: SnowLetterSurfaceRole, textureAlpha: Float): Float = when (role) {
    SnowLetterSurfaceRole.StatusCard -> .12f
    SnowLetterSurfaceRole.DogtalkCard -> .10f
    else -> 0f
} * (.70f + textureAlpha.coerceIn(0f, 1f) * .35f)

private fun snowLetterBorderColor(role: SnowLetterSurfaceRole): Color = when (role) {
    SnowLetterSurfaceRole.UserBubble -> Color(0xFFE6C996).copy(alpha = .42f)
    SnowLetterSurfaceRole.ComposerButton -> Color(0xFFD8E6F1).copy(alpha = .66f)
    SnowLetterSurfaceRole.ActionButton -> Color(0xFFD7E2EC).copy(alpha = .38f)
    else -> Color(0xFFD6E2EC).copy(alpha = .56f)
}

private fun DrawScope.drawSnowLetterComponentSkin(
    role: SnowLetterSurfaceRole,
    textureAlpha: Float,
    drawBase: Boolean = true
) {
    if (drawBase) {
        drawRect(snowLetterBaseColor(role).copy(alpha = snowLetterBaseAlpha(role)))
    }

    val lineAlpha = when (role) {
        SnowLetterSurfaceRole.ActionButton, SnowLetterSurfaceRole.ComposerButton -> .045f
        SnowLetterSurfaceRole.ComposerField, SnowLetterSurfaceRole.DogtalkField -> .075f
        SnowLetterSurfaceRole.AssistantBubble, SnowLetterSurfaceRole.UserBubble -> .070f
        else -> .060f
    } * (.45f + textureAlpha)
    val lineColor = Color(0xFFAFC4D8).copy(alpha = lineAlpha)
    val step = when (role) {
        SnowLetterSurfaceRole.ComposerField, SnowLetterSurfaceRole.DogtalkField -> 18.dp.toPx()
        SnowLetterSurfaceRole.ActionButton, SnowLetterSurfaceRole.ComposerButton -> 24.dp.toPx()
        else -> 30.dp.toPx()
    }
    var y = step * .78f
    var index = 0
    while (y < size.height) {
        val drift = if (index % 2 == 0) 0.dp.toPx() else 6.dp.toPx()
        drawLine(
            color = lineColor,
            start = Offset(9.dp.toPx(), y + drift * .06f),
            end = Offset(size.width - 9.dp.toPx(), y - drift * .04f),
            strokeWidth = .7.dp.toPx()
        )
        y += step
        index += 1
    }

    if (role != SnowLetterSurfaceRole.ActionButton && size.width > 96.dp.toPx() && size.height > 42.dp.toPx()) {
        drawTinyPaperDetails(role)
    }
}

private fun DrawScope.drawSnowLetterSticker(
    painter: Painter,
    role: SnowLetterSurfaceRole,
    decorationAlpha: Float
) {
    val side = min(size.width, size.height) * when (role) {
        SnowLetterSurfaceRole.StatusCard -> .50f
        SnowLetterSurfaceRole.DogtalkCard -> .46f
        else -> .32f
    }
    if (side < 24.dp.toPx()) return

    val alpha = when (role) {
        SnowLetterSurfaceRole.StatusCard -> .055f
        SnowLetterSurfaceRole.DogtalkCard -> .050f
        else -> .035f
    } * decorationAlpha.coerceIn(0f, 1f)

    val inset = 8.dp.toPx()
    translate(left = size.width - side - inset, top = size.height - side - inset) {
        with(painter) {
            draw(size = Size(side, side), alpha = alpha)
        }
    }
}

private fun DrawScope.drawTinyPaperDetails(role: SnowLetterSurfaceRole) {
    val gold = Color(0xFFD7A84D)
    val ink = Color(0xFF23425F)
    val snow = Color(0xFFAFC4D8)

    if (role == SnowLetterSurfaceRole.UserBubble || role == SnowLetterSurfaceRole.ComposerField || role == SnowLetterSurfaceRole.DogtalkField) {
        val mark = Offset(size.width - min(size.width * .18f, 32.dp.toPx()), min(size.height * .30f, 18.dp.toPx()))
        drawCircle(gold.copy(alpha = .08f), 4.dp.toPx(), mark)
        drawLine(gold.copy(alpha = .12f), mark + Offset(-4.dp.toPx(), -4.dp.toPx()), mark + Offset(4.dp.toPx(), 4.dp.toPx()), .8.dp.toPx())
    }

    if (role == SnowLetterSurfaceRole.AssistantBubble || role == SnowLetterSurfaceRole.StatusCard || role == SnowLetterSurfaceRole.DogtalkCard) {
        drawPawMini(
            center = Offset(size.width - 22.dp.toPx(), size.height - 16.dp.toPx()),
            scale = .48f,
            color = ink.copy(alpha = .055f)
        )
    }

    if (role == SnowLetterSurfaceRole.StatusCard || role == SnowLetterSurfaceRole.DogtalkCard) {
        val left = 12.dp.toPx()
        val top = 9.dp.toPx()
        drawLine(snow.copy(alpha = .12f), Offset(left, top), Offset(left + 26.dp.toPx(), top - 2.dp.toPx()), .8.dp.toPx())
        drawLine(snow.copy(alpha = .10f), Offset(left + 2.dp.toPx(), top + 5.dp.toPx()), Offset(left + 34.dp.toPx(), top + 3.dp.toPx()), .8.dp.toPx())
    }
}

private fun DrawScope.drawPawMini(center: Offset, scale: Float, color: Color) {
    drawCircle(color, 3.5.dp.toPx() * scale, center + Offset(0f, 4.5.dp.toPx() * scale))
    drawCircle(color, 1.8.dp.toPx() * scale, center + Offset(-4.5.dp.toPx() * scale, -1.dp.toPx() * scale))
    drawCircle(color, 1.9.dp.toPx() * scale, center + Offset(-1.4.dp.toPx() * scale, -3.dp.toPx() * scale))
    drawCircle(color, 1.9.dp.toPx() * scale, center + Offset(2.2.dp.toPx() * scale, -2.8.dp.toPx() * scale))
    drawCircle(color, 1.7.dp.toPx() * scale, center + Offset(5.dp.toPx() * scale, -.6.dp.toPx() * scale))
}
