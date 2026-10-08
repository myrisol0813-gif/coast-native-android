package com.elementeracoast.app.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.ThoughtSoilSnapshot
import com.elementeracoast.app.core.model.TurnDeskReceipt

@Composable
internal fun MessageItem(
    conversationId: String,
    message: ChatMessage,
    isStreamingTail: Boolean,
    avatarBitmap: ImageBitmap?,
    thoughtSoil: ThoughtSoilSnapshot?,
    turnDeskReceipt: TurnDeskReceipt?,
    attachmentPreviewSource: AttachmentPreviewRemoteDataSource,
    onAvatarClick: () -> Unit,
    onOpenThoughtSoil: () -> Unit,
    onOpenTurnDesk: () -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onAction: (MessageAction) -> Unit,
    onFootprint: (ChatMessage) -> Unit
) {
    when (message.role) {
        MessageRole.User -> UserMessage(
            conversationId = conversationId,
            message = message,
            attachmentPreviewSource = attachmentPreviewSource,
            onCopy = { onCopy(message) },
            onEdit = { onEdit(message) },
            onAction = onAction
        )
        MessageRole.Assistant -> AssistantMessage(
            message = message,
            isStreamingTail = isStreamingTail,
            avatarBitmap = avatarBitmap,
            thoughtSoil = thoughtSoil,
            turnDeskReceipt = turnDeskReceipt,
            onAvatarClick = onAvatarClick,
            onOpenThoughtSoil = onOpenThoughtSoil,
            onOpenTurnDesk = onOpenTurnDesk,
            onCopy = { onCopy(message) },
            onAction = onAction,
            onFootprint = { onFootprint(message) }
        )
    }
}
