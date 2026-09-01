package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.ui.theme.CoastChatTokens

@Composable
internal fun ChatTimeline(
    conversationId: String,
    messages: List<ChatMessage>,
    userBubble: String,
    isStreaming: Boolean,
    avatarBitmap: ImageBitmap?,
    onAvatarClick: () -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onAction: (MessageAction) -> Unit,
    onOpenActionLog: (List<String>, String) -> Unit,
    onVariantPlaceholder: (ChatMessage) -> Unit,
    onFootprint: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val tailLength = messages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(conversationId, messages.size, tailLength) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("潮水退到纸页外。", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "写点什么，新的窗口会从这里长出来。",
                    modifier = Modifier.padding(top = 7.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .68f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Box
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().widthIn(max = CoastChatTokens.TimelineMaxWidth),
            contentPadding = PaddingValues(
                start = CoastChatTokens.TimelineHorizontalPadding,
                end = CoastChatTokens.TimelineHorizontalPadding,
                top = CoastChatTokens.TimelineTopPadding,
                bottom = CoastChatTokens.TimelineBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(CoastChatTokens.MessageGap)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageItem(
                    message = message,
                    conversationId = conversationId,
                    userBubble = userBubble,
                    isStreamingTail = isStreaming && message.id == messages.lastOrNull()?.id,
                    avatarBitmap = avatarBitmap,
                    onAvatarClick = onAvatarClick,
                    onCopy = onCopy,
                    onEdit = onEdit,
                    onAction = onAction,
                    onOpenActionLog = onOpenActionLog,
                    onVariantPlaceholder = onVariantPlaceholder,
                    onFootprint = onFootprint
                )
            }
        }
    }
}
