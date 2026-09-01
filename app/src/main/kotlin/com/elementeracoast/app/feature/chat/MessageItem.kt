package com.elementeracoast.app.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole

@Composable
internal fun MessageItem(
    message: ChatMessage,
    isStreamingTail: Boolean,
    avatarBitmap: ImageBitmap?,
    onAvatarClick: () -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onAction: (MessageAction) -> Unit,
    onVariantPlaceholder: (ChatMessage) -> Unit,
    onFootprint: (ChatMessage) -> Unit
) {
    when (message.role) {
        MessageRole.User -> UserMessage(
            message = message,
            onCopy = { onCopy(message) },
            onEdit = { onEdit(message) },
            onAction = onAction,
            onVariantPlaceholder = { onVariantPlaceholder(message) }
        )

        MessageRole.Assistant -> AssistantMessage(
            message = message,
            isStreamingTail = isStreamingTail,
            avatarBitmap = avatarBitmap,
            onAvatarClick = onAvatarClick,
            onCopy = { onCopy(message) },
            onAction = onAction,
            onVariantPlaceholder = { onVariantPlaceholder(message) },
            onFootprint = { onFootprint(message) }
        )
    }
}
