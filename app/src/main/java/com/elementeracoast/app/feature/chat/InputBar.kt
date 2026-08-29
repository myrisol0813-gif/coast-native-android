package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.theme.CoastShapes
import com.elementeracoast.app.core.theme.CoastSpacing

@Composable
fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    generating: Boolean,
    loading: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CoastSpacing.sm, vertical = CoastSpacing.xs)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CoastShapes.input)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CoastShapes.input)
            .padding(start = CoastSpacing.md, end = CoastSpacing.xs, top = CoastSpacing.xs, bottom = CoastSpacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).heightIn(min = 40.dp, max = 132.dp).padding(vertical = 9.dp),
            enabled = !loading,
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (!generating && value.isNotBlank()) onSend()
                },
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.TopStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = if (generating) "回潮还在继续…" else "写给海岸",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.54f),
                        )
                    }
                    inner()
                }
            },
        )
        SendStopButton(
            generating = generating,
            enabled = generating || (value.isNotBlank() && !loading),
            onClick = if (generating) onStop else onSend,
        )
    }
}

@Composable
private fun SendStopButton(generating: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val gold = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (enabled) gold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val alpha = if (enabled) 1f else 0.35f
            if (generating) {
                drawRoundRect(
                    color = gold.copy(alpha = alpha),
                    topLeft = Offset(size.width * 0.30f, size.height * 0.30f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.40f, size.height * 0.40f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            } else {
                val stroke = 2.2.dp.toPx()
                val x = size.width / 2
                drawLine(gold.copy(alpha = alpha), Offset(x, size.height - 2f), Offset(x, 2f), strokeWidth = stroke)
                drawLine(gold.copy(alpha = alpha), Offset(x, 2f), Offset(x - 5f, 7f), strokeWidth = stroke)
                drawLine(gold.copy(alpha = alpha), Offset(x, 2f), Offset(x + 5f, 7f), strokeWidth = stroke)
            }
        }
    }
}
