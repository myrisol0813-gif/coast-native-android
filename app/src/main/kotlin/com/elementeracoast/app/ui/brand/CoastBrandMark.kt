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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Source-faithful Coast identity geometry migrated from the inline Gate SVG in
 * elementera-coast/functions/auth.js. This file owns geometry only; entrance
 * timing lives in CoastBrandMarkAnimation.kt.
 */
@Composable
fun CoastBrandMark(
    modifier: Modifier = Modifier,
    separatorColor: Color = CoastPaper,
    loopAProgress: Float = 1f,
    loopBProgress: Float = 1f,
    loopCProgress: Float = 1f,
    hornProgress: Float = 1f,
    wolfProgress: Float = 1f,
    markScale: Float = 1f
) {
    Canvas(modifier = modifier.aspectRatio(390f / 300f)) {
        // The old Native canvas ended at source y=330 while a rotated ellipse
        // plus the 40px paper under-stroke reaches about y=383. Keeping the
        // source viewport at 90..390 preserves the complete rounded loops.
        val sourceTop = 90f
        val scale = size.width / 390f
        fun x(value: Float) = value * scale
        fun y(value: Float) = (value - sourceTop) * scale
        fun wolfY(value: Float) = (value * .8f + 54f - sourceTop) * scale
        val center = Offset(x(195f), y(260f))

        fun sourceHorn(left: Boolean): Path = Path().apply {
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

        fun loopSegment(progress: Float): Path {
            val full = Path().apply {
                addOval(Rect(Offset(x(82f), y(195f)), Size(x(226f), x(130f))))
            }
            val measure = PathMeasure().apply { setPath(full, false) }
            return Path().also { segment ->
                measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), segment, true)
            }
        }

        fun drawLoop(angle: Float, progress: Float, color: Color, width: Float) {
            if (progress <= 0f) return
            withTransform({ rotate(angle, center) }) {
                drawPath(
                    path = loopSegment(progress),
                    color = color,
                    style = Stroke(
                        width = x(width),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        fun polygon(points: List<Pair<Float, Float>>, color: Color) {
            val path = Path().apply {
                moveTo(x(points.first().first), wolfY(points.first().second))
                points.drop(1).forEach { (px, py) -> lineTo(x(px), wolfY(py)) }
                close()
            }
            drawPath(path, color)
        }

        withTransform({ scale(markScale, markScale, center) }) {
            val hornAlpha = hornProgress.coerceIn(0f, 1f)
            val hornScale = .72f + .28f * hornProgress
            val hornShift = x(7f * (1f - hornAlpha))
            listOf(true, false).forEach { left ->
                val pivot = Offset(x(if (left) 143f else 247f), y(168f))
                withTransform({
                    translate(top = hornShift)
                    scale(hornScale, hornScale, pivot)
                }) {
                    drawPath(sourceHorn(left), CoastGold, alpha = hornAlpha)
                }
            }

            val progresses = listOf(loopAProgress, loopBProgress, loopCProgress)
            val angles = listOf(0f, 60f, -60f)
            angles.zip(progresses).forEach { (angle, progress) ->
                drawLoop(angle, progress, separatorColor, 40f)
            }
            angles.zip(progresses).forEach { (angle, progress) ->
                drawLoop(angle, progress, CoastInk, 29f)
            }

            val wolfAlpha = wolfProgress.coerceIn(0f, 1f)
            if (wolfAlpha > 0f) {
                val wolfScale = .9f + .1f * wolfProgress
                val wolfShift = x(6f * (1f - wolfAlpha))
                val wolfPivot = Offset(x(195f), wolfY(260f))
                withTransform({
                    translate(top = wolfShift)
                    scale(wolfScale, wolfScale, wolfPivot)
                }) {
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
                    drawPath(wolf, CoastCream, alpha = wolfAlpha)

                    val leftEar = Path().apply {
                        moveTo(x(161f), wolfY(214f)); lineTo(x(166f), wolfY(239f)); lineTo(x(185f), wolfY(225f)); lineTo(x(171f), wolfY(209f))
                        cubicTo(x(167f), wolfY(204f), x(163f), wolfY(207f), x(161f), wolfY(214f)); close()
                    }
                    val rightEar = Path().apply {
                        moveTo(x(229f), wolfY(214f)); lineTo(x(224f), wolfY(239f)); lineTo(x(205f), wolfY(225f)); lineTo(x(219f), wolfY(209f))
                        cubicTo(x(223f), wolfY(204f), x(227f), wolfY(207f), x(229f), wolfY(214f)); close()
                    }
                    drawPath(leftEar, CoastGold, alpha = wolfAlpha)
                    drawPath(rightEar, CoastGold, alpha = wolfAlpha)
                    polygon(listOf(150f to 250f, 130f to 261f, 145f to 269f, 132f to 280f, 155f to 284f), CoastCream.copy(alpha = wolfAlpha))
                    polygon(listOf(240f to 250f, 260f to 261f, 245f to 269f, 258f to 280f, 235f to 284f), CoastCream.copy(alpha = wolfAlpha))

                    val faceStroke = Stroke(width = x(5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val leftEye = Path().apply {
                        moveTo(x(163f), wolfY(260f)); quadraticTo(x(174f), wolfY(271f), x(185f), wolfY(260f))
                    }
                    val rightEye = Path().apply {
                        moveTo(x(205f), wolfY(260f)); quadraticTo(x(216f), wolfY(271f), x(227f), wolfY(260f))
                    }
                    drawPath(leftEye, CoastGold, alpha = wolfAlpha, style = faceStroke)
                    drawPath(rightEye, CoastGold, alpha = wolfAlpha, style = faceStroke)

                    val nose = Path().apply {
                        moveTo(x(189f), wolfY(276f))
                        quadraticTo(x(195f), wolfY(272f), x(201f), wolfY(276f))
                        quadraticTo(x(199f), wolfY(281f), x(195f), wolfY(282f))
                        quadraticTo(x(191f), wolfY(281f), x(189f), wolfY(276f)); close()
                    }
                    drawPath(nose, CoastGold, alpha = wolfAlpha)

                    val mouthStroke = Stroke(width = x(4f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val mouthLeft = Path().apply {
                        moveTo(x(195f), wolfY(282f)); quadraticTo(x(195f), wolfY(288f), x(187f), wolfY(288f))
                    }
                    val mouthRight = Path().apply {
                        moveTo(x(195f), wolfY(282f)); quadraticTo(x(195f), wolfY(288f), x(203f), wolfY(288f))
                    }
                    drawPath(mouthLeft, CoastGold, alpha = wolfAlpha, style = mouthStroke)
                    drawPath(mouthRight, CoastGold, alpha = wolfAlpha, style = mouthStroke)
                }
            }
        }
    }
}

val CoastInk = Color(0xFF24252B)
val CoastGold = Color(0xFFF2B84B)
val CoastCream = Color(0xFFFFF0D8)
val CoastPaper = Color(0xFFFFFFFF)
val CoastMuted = Color(0xFF9C8872)
val CoastQuiet = Color(0xFFB8AFA6)
