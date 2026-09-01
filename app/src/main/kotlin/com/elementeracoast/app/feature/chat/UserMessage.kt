package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.ui.theme.CoastChatTokens

@Composable
internal fun UserMessage(
    message: ChatMessage,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onAction: (MessageAction) -> Unit,
    onVariantPlaceholder: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubble = maxWidth * CoastChatTokens.UserBubbleWidth
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier.widthIn(max = maxBubble),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(CoastChatTokens.UserBubbleRadius)
                        )
                        .padding(
                            horizontal = CoastChatTokens.UserBubbleHorizontalPadding,
                            vertical = CoastChatTokens.UserBubbleVerticalPadding
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = CoastChatTokens.ChatBodySize,
                        lineHeight = CoastChatTokens.UserBodyLineHeight
                    )
                )
                Spacer(Modifier.height(CoastChatTokens.UserActionTopGap))
                MessageActionRow {
                    MessageActionButton(
                        icon = Icons.Default.ContentCopy,
                        label = "复制",
                        onClick = onCopy
                    )
                    MessageActionButton(
                        icon = Icons.Default.Edit,
                        label = "编辑",
                        onClick = onEdit
                    )
                    MessageActionButton(
                        icon = Icons.Default.DeleteOutline,
                        label = "删除",
                        onClick = { onAction(MessageAction.Delete(message.id)) }
                    )
                    Spacer(Modifier.width(3.dp))
                    VariantControl(
                        index = message.variantIndex,
                        count = message.variantCount,
                        onPlaceholder = onVariantPlaceholder
                    )
                }
            }
        }
    }
}
