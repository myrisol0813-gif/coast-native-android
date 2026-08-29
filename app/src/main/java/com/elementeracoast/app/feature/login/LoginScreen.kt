package com.elementeracoast.app.feature.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.theme.CoastShapes
import com.elementeracoast.app.core.theme.CoastSpacing

@Composable
fun LoginScreen(state: LoginUiState, onLogin: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    val canSubmit = password.isNotBlank() && !state.loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = CoastSpacing.lg),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CoastGateMark()
            Spacer(Modifier.height(CoastSpacing.lg))
            Text(
                text = "Elementera Coast",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(CoastSpacing.xs))
            Text(
                text = "CoastGPT · 私密主聊天窗口",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(CoastSpacing.xl))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(
                        width = 1.dp,
                        color = if (state.error == null) MaterialTheme.colorScheme.outlineVariant
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        shape = CoastShapes.input,
                    )
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), CoastShapes.input)
                    .padding(start = CoastSpacing.md, end = CoastSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.weight(1f),
                    enabled = !state.loading,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (canSubmit) onLogin(password) }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (password.isEmpty()) {
                                Text(
                                    "输入海岸密码",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                            inner()
                        }
                    },
                )
                GateArrowButton(
                    loading = state.loading,
                    enabled = canSubmit,
                    onClick = { onLogin(password) },
                )
            }

            if (state.error != null) {
                Spacer(Modifier.height(CoastSpacing.sm))
                Text(
                    text = state.error,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CoastSpacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Spacer(Modifier.height(CoastSpacing.sm))
                Text(
                    text = "沿海岸保存回声",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun GateArrowButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val gold = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (enabled) gold.copy(alpha = 0.13f) else Color.Transparent, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 1.6.dp,
                color = gold,
            )
        } else {
            Canvas(Modifier.width(18.dp).height(14.dp)) {
                val stroke = 2.2.dp.toPx()
                val y = size.height / 2
                drawLine(gold.copy(alpha = if (enabled) 1f else 0.35f), Offset(1f, y), Offset(size.width - 2f, y), strokeWidth = stroke)
                drawLine(gold.copy(alpha = if (enabled) 1f else 0.35f), Offset(size.width - 7f, y - 5f), Offset(size.width - 2f, y), strokeWidth = stroke)
                drawLine(gold.copy(alpha = if (enabled) 1f else 0.35f), Offset(size.width - 7f, y + 5f), Offset(size.width - 2f, y), strokeWidth = stroke)
            }
        }
    }
}

@Composable
private fun CoastGateMark() {
    val line = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f)
    val under = MaterialTheme.colorScheme.background
    val gold = MaterialTheme.colorScheme.primary
    val dog = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f)

    Canvas(Modifier.size(190.dp)) {
        val oval = Size(size.width * 0.68f, size.height * 0.36f)
        val topLeft = Offset((size.width - oval.width) / 2, (size.height - oval.height) / 2)
        for (rotation in listOf(0f, 60f, -60f)) {
            rotate(rotation, pivot = center) {
                drawOval(under, topLeft, oval, style = Stroke(width = 13.dp.toPx()))
                drawOval(line, topLeft, oval, style = Stroke(width = 7.dp.toPx()))
            }
        }

        val hornBaseY = center.y - 37.dp.toPx()
        drawLine(gold, Offset(center.x - 30.dp.toPx(), hornBaseY), Offset(center.x - 38.dp.toPx(), hornBaseY - 19.dp.toPx()), strokeWidth = 5.dp.toPx())
        drawLine(gold, Offset(center.x + 30.dp.toPx(), hornBaseY), Offset(center.x + 38.dp.toPx(), hornBaseY - 19.dp.toPx()), strokeWidth = 5.dp.toPx())

        drawCircle(dog, radius = 31.dp.toPx(), center = center)
        drawLine(gold, center + Offset(-18.dp.toPx(), -2.dp.toPx()), center + Offset(-7.dp.toPx(), 3.dp.toPx()), strokeWidth = 2.5.dp.toPx())
        drawLine(gold, center + Offset(18.dp.toPx(), -2.dp.toPx()), center + Offset(7.dp.toPx(), 3.dp.toPx()), strokeWidth = 2.5.dp.toPx())
        drawCircle(gold, radius = 2.8.dp.toPx(), center = center + Offset(0f, 12.dp.toPx()))
    }
}
