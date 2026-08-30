package com.elementeracoast.app.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Exact geometry migrated from the original Gate SVG embedded in
 * elementera-coast/functions/auth.js (PWA baseline b6b8267e...).
 *
 * This is not traced from a screenshot and not a replacement logo. The three
 * ellipses, horns, wolf paths and source colors use the original SVG numbers.
 * Text is intentionally rendered by Compose outside the mark so Android can
 * use native typography while the identity mark stays source-faithful.
 */
@Composable
fun CoastBrandMark(
    modifier: Modifier = Modifier,
    separatorColor: Color = CoastPaper
) {
    Canvas(modifier = modifier.aspectRatio(390f / 230f)) {
        val scale = size.width / 390f
        fun x(value: Float) = value * scale
        fun y(value: Float) = (value - 100f) * scale
        fun wolfY(value: Float) = ((value * .8f + 54f) - 100f) * scale

        fun hornPath(left: Boolean): Path = Path().apply {
            if (left) {
                moveTo(x(126f), y(166f))
                cubicTo(x(127f), y(146f), x(135f), y(130f), x(149f), y(118f))
                cubicTo(x(158f), y(135f), x(160f), y(150f), x(157f), y(168f))
            } else {
                moveTo(x(264f), y(166f))
                cubicTo(x(263f), y(146f), x(255f), y(130f), x(241f), y(118f))
                cubicTo(x(232f), y(135f), x(230f), y(150f), x(233f), y(168f))
            }
            close()
        }
        drawPath(hornPath(true), CoastGold)
        drawPath(hornPath(false), CoastGold)

        fun loop(angle: Float, color: Color, width: Float) {
            withTransform({
                rotate(angle, pivot = Offset(x(195f), y(260f)))
            }) {
                drawOval(
                    color = color,
                    topLeft = Offset(x(82f), y(195f)),
                    size = Size(x(226f), x(130f)),
                    style = Stroke(
                        width = x(width),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        listOf(0f, 60f, -60f).forEach { angle -> loop(angle, separatorColor, 40f) }
        listOf(0f, 60f, -60f).forEach { angle -> loop(angle, CoastInk, 29f) }

        val wolf = Path().apply {
            moveTo(x(145f), wolfY(238f))
            lineTo(x(157f), wolfY(207f))
            cubicTo(x(160f), wolfY(199f), x(167f), wolfY(198f), x(173f), wolfY(204f))
            lineTo(x(188f), wolfY(219f))
            cubicTo(x(193f), wolfY(217f), x(197f), wolfY(217f), x(202f), wolfY(219f))
            lineTo(x(217f), wolfY(204f))
            cubicTo(x(223f), wolfY(198f), x(230f), wolfY(200f), x(233f), wolfY(208f))
            lineTo(x(245f), wolfY(238f))
            lineTo(x(245f), wolfY(267f))
            cubicTo(x(245f), wolfY(278f), x(240f), wolfY(287f), x(231f), wolfY(293f))
            lineTo(x(219f), wolfY(301f))
            cubicTo(x(212f), wolfY(306f), x(204f), wolfY(309f), x(195f), wolfY(309f))
            cubicTo(x(186f), wolfY(309f), x(178f), wolfY(306f), x(171f), wolfY(301f))
            lineTo(x(159f), wolfY(293f))
            cubicTo(x(150f), wolfY(287f), x(145f), wolfY(278f), x(145f), wolfY(267f))
            close()
        }
        drawPath(wolf, CoastCream)

        fun polygon(points: List<Pair<Float, Float>>, color: Color) {
            val path = Path().apply {
                moveTo(x(points.first().first), wolfY(points.first().second))
                points.drop(1).forEach { (px, py) -> lineTo(x(px), wolfY(py)) }
                close()
            }
            drawPath(path, color)
        }

        val leftEar = Path().apply {
            moveTo(x(161f), wolfY(214f))
            lineTo(x(166f), wolfY(239f))
            lineTo(x(185f), wolfY(225f))
            lineTo(x(171f), wolfY(209f))
            cubicTo(x(167f), wolfY(204f), x(163f), wolfY(207f), x(161f), wolfY(214f))
            close()
        }
        val rightEar = Path().apply {
            moveTo(x(229f), wolfY(214f))
            lineTo(x(224f), wolfY(239f))
            lineTo(x(205f), wolfY(225f))
            lineTo(x(219f), wolfY(209f))
            cubicTo(x(223f), wolfY(204f), x(227f), wolfY(207f), x(229f), wolfY(214f))
            close()
        }
        drawPath(leftEar, CoastGold)
        drawPath(rightEar, CoastGold)

        polygon(listOf(150f to 250f, 130f to 261f, 145f to 269f, 132f to 280f, 155f to 284f), CoastCream)
        polygon(listOf(240f to 250f, 260f to 261f, 245f to 269f, 258f to 280f, 235f to 284f), CoastCream)

        val faceStroke = Stroke(width = x(5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val leftEye = Path().apply {
            moveTo(x(163f), wolfY(260f))
            quadraticBezierTo(x(174f), wolfY(271f), x(185f), wolfY(260f))
        }
        val rightEye = Path().apply {
            moveTo(x(205f), wolfY(260f))
            quadraticBezierTo(x(216f), wolfY(271f), x(227f), wolfY(260f))
        }
        drawPath(leftEye, CoastGold, style = faceStroke)
        drawPath(rightEye, CoastGold, style = faceStroke)

        val nose = Path().apply {
            moveTo(x(189f), wolfY(276f))
            quadraticBezierTo(x(195f), wolfY(272f), x(201f), wolfY(276f))
            quadraticBezierTo(x(199f), wolfY(281f), x(195f), wolfY(282f))
            quadraticBezierTo(x(191f), wolfY(281f), x(189f), wolfY(276f))
            close()
        }
        drawPath(nose, CoastGold)

        val mouthStroke = Stroke(width = x(4f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val mouthLeft = Path().apply {
            moveTo(x(195f), wolfY(282f))
            quadraticBezierTo(x(195f), wolfY(288f), x(187f), wolfY(288f))
        }
        val mouthRight = Path().apply {
            moveTo(x(195f), wolfY(282f))
            quadraticBezierTo(x(195f), wolfY(288f), x(203f), wolfY(288f))
        }
        drawPath(mouthLeft, CoastGold, style = mouthStroke)
        drawPath(mouthRight, CoastGold, style = mouthStroke)
    }
}

val CoastInk = Color(0xFF24252B)
val CoastGold = Color(0xFFF2B84B)
val CoastCream = Color(0xFFFFF0D8)
val CoastPaper = Color(0xFFFFFFFF)
val CoastMuted = Color(0xFF9C8872)
