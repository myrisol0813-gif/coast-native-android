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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import com.elementeracoast.app.ui.theme.CoastChatTokens
import com.elementeracoast.app.ui.theme.SnowLetterSurface
import com.elementeracoast.app.ui.theme.SnowLetterSurfaceRole

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
            .padding(
                horizontal = CoastChatTokens.ComposerHorizontalPadding,
                vertical = CoastChatTokens.ComposerVerticalPadding
            ),
        verticalAlignment = Alignment.Bottom
    ) {
        RoundComposerButton(
            background = MaterialTheme.colorScheme.surfaceVariant,
            foreground = MaterialTheme.colorScheme.onSurface,
            onClick = { onPlaceholder("图片与附件将在 P1 接入。") }
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "添加",
                modifier = Modifier.size(CoastChatTokens.ComposerPlusGlyph)
            )
        }

        Spacer(Modifier.size(CoastChatTokens.ComposerGap))

        SnowLetterSurface(
            modifier = Modifier
                .weight(1f)
                .heightIn(
                    min = CoastChatTokens.ComposerPillMinHeight,
                    max = CoastChatTokens.ComposerPillMaxHeight
                ),
            role = SnowLetterSurfaceRole.ComposerField,
            fallbackColor = MaterialTheme.colorScheme.surfaceVariant,
            fallbackShape = RoundedCornerShape(CoastChatTokens.ComposerPillRadius)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CoastChatTokens.ComposerPillStartPadding,
                        end = CoastChatTokens.ComposerPillEndPadding,
                        top = CoastChatTokens.ComposerPillVerticalPadding,
                        bottom = CoastChatTokens.ComposerPillVerticalPadding
                    ),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = CoastChatTokens.ComposerMicTouch),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            if (enabled) "询问任何问题" else "正在准备聊天…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .8f),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = CoastChatTokens.ComposerTextSize,
                                lineHeight = CoastChatTokens.ComposerTextLineHeight
                            )
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled && !isStreaming,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = CoastChatTokens.ComposerTextSize,
                            lineHeight = CoastChatTokens.ComposerTextLineHeight
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 6
                    )
                }
                Box(
                    modifier = Modifier
                        .size(CoastChatTokens.ComposerMicTouch)
                        .clickable(enabled = enabled && !isStreaming) {
                            onPlaceholder("语音输入将在 P1 接入。")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "语音输入",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .78f),
                        modifier = Modifier.size(CoastChatTokens.ComposerMicGlyph)
                    )
                }
            }
        }

        Spacer(Modifier.size(CoastChatTokens.ComposerGap))

        val actionBackground = if (isStreaming) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
        val actionForeground = if (isStreaming) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimary
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
                modifier = Modifier.size(CoastChatTokens.ComposerActionGlyph)
            )
        }
    }
}

@Composable
private fun RoundComposerButton(
    background: Color,
    foreground: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(CoastChatTokens.ComposerTouchTarget)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        SnowLetterSurface(
            modifier = Modifier.size(CoastChatTokens.ComposerVisualButton),
            role = SnowLetterSurfaceRole.ComposerButton,
            fallbackColor = background,
            fallbackShape = CircleShape,
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides foreground,
                content = content
            )
        }
    }
}