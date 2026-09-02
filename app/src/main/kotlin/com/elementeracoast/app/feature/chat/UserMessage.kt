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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.ui.theme.CoastChatTokens
import com.elementeracoast.app.ui.theme.LocalCoastAppearance

@Composable
internal fun UserMessage(
    message: ChatMessage,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onAction: (MessageAction) -> Unit
) {
    val customBubble = LocalCoastAppearance.current.userBubbleColor
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxBubble = maxWidth * CoastChatTokens.UserBubbleWidth
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Column(modifier = Modifier.widthIn(max = maxBubble), horizontalAlignment = Alignment.End) {
                val bubbleShape = RoundedCornerShape(CoastChatTokens.UserBubbleRadius)
                Text(
                    text = message.text,
                    modifier = Modifier
                        .shadow(2.dp, bubbleShape, clip = false)
                        .background(customBubble ?: MaterialTheme.colorScheme.surfaceVariant, bubbleShape)
                        .padding(
                            horizontal = CoastChatTokens.UserBubbleHorizontalPadding,
                            vertical = CoastChatTokens.UserBubbleVerticalPadding
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = CoastChatTokens.ChatBodySize,
                        lineHeight = CoastChatTokens.UserBodyLineHeight,
                        fontWeight = FontWeight.Normal
                    )
                )
                Spacer(Modifier.height(CoastChatTokens.UserActionTopGap))
                MessageActionRow {
                    MessageActionButton(Icons.Default.Edit, "编辑", onClick = onEdit)
                    MessageActionButton(Icons.Default.ContentCopy, "复制", onClick = onCopy)
                    if (message.variantCount > 1) {
                        MessageActionButton(
                            Icons.Default.DeleteOutline,
                            "删除当前版本",
                            onClick = { onAction(MessageAction.Delete(message.id)) }
                        )
                        Spacer(Modifier.width(CoastChatTokens.VariantActionGap))
                        VariantControl(
                            index = message.variantIndex,
                            count = message.variantCount,
                            onPrevious = { onAction(MessageAction.SelectVariant(message.id, message.variantIndex - 1)) },
                            onNext = { onAction(MessageAction.SelectVariant(message.id, message.variantIndex + 1)) }
                        )
                    }
                }
            }
        }
    }
}
