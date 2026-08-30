/*
 * The multiline composer and send/stop-in-one-place interaction keep the
 * selected MIT-licensed MiniiChat-derived interaction pattern from the PoC.
 * Coast-specific three-part composer geometry is rewritten from the PWA visual
 * reference supplied for Native v1. See THIRD_PARTY_NOTICES.md.
 */
package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
fun CoastComposer(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    enabled: Boolean = true,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onPlaceholder: (String) -> Unit
) {
    val focus = LocalFocusManager.current
    val canSend = enabled && value.trim().isNotEmpty() && !isStreaming

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        RoundComposerButton(
            background = MaterialTheme.colorScheme.surfaceVariant,
            foreground = MaterialTheme.colorScheme.onSurface,
            onClick = { onPlaceholder("图片与附件将在 P1 接入。") }
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加", modifier = Modifier.size(27.dp))
        }

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 50.dp, max = 150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(27.dp))
                .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        if (enabled) "询问任何问题" else "正在准备聊天…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled && !isStreaming,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clickable(enabled = enabled && !isStreaming) {
                        onPlaceholder("语音输入将在 P1 接入。")
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "语音输入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        val actionBackground = when {
            isStreaming -> MaterialTheme.colorScheme.onSurface
            canSend -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary
        }
        val actionForeground = when {
            isStreaming -> MaterialTheme.colorScheme.background
            else -> MaterialTheme.colorScheme.onPrimary
        }
        RoundComposerButton(
            background = actionBackground,
            foreground = actionForeground,
            enabled = enabled || isStreaming,
            onClick = {
                when {
                    isStreaming -> onStop()
                    canSend -> {
                        focus.clearFocus()
                        onSend()
                    }
                    else -> onPlaceholder("空输入通话仍是 Native v1 占位。")
                }
            }
        ) {
            Icon(
                imageVector = when {
                    isStreaming -> Icons.Default.Stop
                    canSend -> Icons.Default.ArrowUpward
                    else -> Icons.Default.Call
                },
                contentDescription = when {
                    isStreaming -> "停止"
                    canSend -> "发送"
                    else -> "通话"
                },
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RoundComposerButton(
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(background, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.LocalContentColor.current
        Box(modifier = Modifier, contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides foreground,
                content = content
            )
        }
    }
}
