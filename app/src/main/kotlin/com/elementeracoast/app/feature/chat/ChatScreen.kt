/*
 * Message-flow proportions retain the selected MIT-licensed MiniiChat-derived
 * right-aligned user / natural assistant body pattern from the PoC. The shared
 * Main/Radio/Lighthouse ChatWindow and Coast visual treatment are CoastGPT v1.
 */
package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.feature.dogtalk.DogtalkCard
import com.elementeracoast.app.feature.dogtalk.DogtalkScope
import com.elementeracoast.app.ui.theme.CoastChatTokens

@Composable
fun ChatWindow(
    state: CoastShellState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onPlaceholder: (String) -> Unit
) {
    var input by rememberSaveable(state.activeConversationId) { mutableStateOf("") }
    val listState = rememberLazyListState()
    val tailLength = state.messages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(state.activeConversationId, state.messages.size, tailLength) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        CoastTimeline(
            messages = state.messages,
            isStreaming = state.isStreaming,
            listState = listState,
            modifier = Modifier.weight(1f)
        )
        DogtalkCard(
            scope = DogtalkScope.from(state.activeRoomType),
            onSaved = { onPlaceholder("已暂存在本地；后端稍后接入") }
        )
        CoastComposer(
            value = input,
            onValueChange = { input = it },
            isStreaming = state.isStreaming,
            enabled = true,
            onSend = {
                val outgoing = input
                input = ""
                onSend(outgoing)
            },
            onStop = onStop,
            onPlaceholder = onPlaceholder
        )
    }
}

@Composable
fun CoastTimeline(
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
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
                isStreamingTail = isStreaming && message.id == messages.lastOrNull()?.id
            )
        }
    }
}

@Composable
private fun MessageItem(message: ChatMessage, isStreamingTail: Boolean) {
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = CoastChatTokens.ChatBodySize,
        lineHeight = CoastChatTokens.ChatBodyLineHeight
    )
    if (message.role == MessageRole.User) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = message.text,
                modifier = Modifier
                    .fillMaxWidth(CoastChatTokens.UserBubbleWidth)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(CoastChatTokens.UserBubbleRadius)
                    )
                    .padding(
                        horizontal = CoastChatTokens.UserBubbleHorizontalPadding,
                        vertical = CoastChatTokens.UserBubbleVerticalPadding
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                style = bodyStyle
            )
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(CoastChatTokens.AssistantAvatarSize)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✦",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = CoastChatTokens.AssistantStarSize)
            )
        }
        Spacer(Modifier.size(CoastChatTokens.AssistantAvatarGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (message.text.isEmpty() && isStreamingTail) "•••" else message.text,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                style = bodyStyle
            )
            if (isStreamingTail) {
                Spacer(Modifier.height(CoastChatTokens.StreamingGap))
                Text(
                    "正在回潮…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
