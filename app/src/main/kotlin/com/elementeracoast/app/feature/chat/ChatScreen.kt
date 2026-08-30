/*
 * Message-flow proportions retain the selected MIT-licensed MiniiChat-derived
 * right-aligned user / natural assistant body pattern from the PoC. The shared
 * Main/Radio/Lighthouse ChatWindow and Coast visual treatment are CoastGPT v1.
 */
package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.ChatScope
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.MessageRole

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
        DogtalkRow(scope = state.activeScope)
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
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
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
    if (message.role == MessageRole.User) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = message.text,
                modifier = Modifier
                    .fillMaxWidth(.84f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 15.dp, vertical = 11.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "✦",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (message.text.isEmpty() && isStreamingTail) "•••" else message.text,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            if (isStreamingTail) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "正在回潮…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun DogtalkRow(scope: ChatScope) {
    var open by rememberSaveable(scope) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .clickable { open = !open }
            .padding(horizontal = 15.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "小寒 · 神秘狗话",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when (scope) {
                        ChatScope.Main -> "小寒这轮很放松，因此偷懒中。"
                        ChatScope.Radio -> "电波 scope 也使用同一行狗话。"
                        ChatScope.Lighthouse -> "灯塔 scope 也使用同一行狗话。"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (open) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Native v1 先保留 Dogtalk row 的稳定位置与折叠身体；保存、读取、归档仍是 P1。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
