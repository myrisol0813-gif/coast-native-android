package com.elementeracoast.app.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.ThoughtSoilSnapshot

@Composable
internal fun MessageItem(
    conversationId: String,
    message: ChatMessage,
    isStreamingTail: Boolean,
    avatarBitmap: ImageBitmap?,
    thoughtSoil: ThoughtSoilSnapshot?,
    metadataMessageId: String?,
    metadataSource: ModelMetadataRemoteDataSource,
    attachmentPreviewSource: AttachmentPreviewRemoteDataSource,
    onAvatarClick: () -> Unit,
    onOpenThoughtSoil: () -> Unit,
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
            conversationId = conversationId,
            message = message,
            isStreamingTail = isStreamingTail,
            avatarBitmap = avatarBitmap,
            thoughtSoil = thoughtSoil,
            metadataMessageId = metadataMessageId,
            metadataSource = metadataSource,
            onAvatarClick = onAvatarClick,
            onOpenThoughtSoil = onOpenThoughtSoil,
            onCopy = { onCopy(message) },
            onAction = onAction,
            onFootprint = { onFootprint(message) }
        )
    }
}
