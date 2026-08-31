package com.elementeracoast.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CoastChatIcons {
    val Menu: ImageVector by lazy {
        ImageVector.Builder(
            name = "CoastMenu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 8f)
                lineTo(19f, 8f)
                moveTo(5f, 12f)
                lineTo(16.5f, 12f)
                moveTo(5f, 16f)
                lineTo(18.5f, 16f)
            }
        }.build()
    }

    val NewChat: ImageVector by lazy {
        ImageVector.Builder(
            name = "CoastNewChat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.3f, 6f)
                lineTo(8f, 6f)
                curveTo(6.35f, 6f, 5f, 7.35f, 5f, 9f)
                lineTo(5f, 16f)
                curveTo(5f, 17.65f, 6.35f, 19f, 8f, 19f)
                lineTo(15f, 19f)
                curveTo(16.65f, 19f, 18f, 17.65f, 18f, 16f)
                lineTo(18f, 12.4f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.65f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.2f, 15.8f)
                lineTo(10.8f, 13.5f)
                lineTo(17.55f, 6.75f)
                curveTo(18.25f, 6.05f, 19.35f, 6.05f, 20.05f, 6.75f)
                curveTo(20.75f, 7.45f, 20.75f, 8.55f, 20.05f, 9.25f)
                lineTo(13.3f, 16f)
                lineTo(10.2f, 16.8f)
                close()
            }
        }.build()
    }
}
