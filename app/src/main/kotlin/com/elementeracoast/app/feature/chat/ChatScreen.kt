/*
 * Message-flow proportions and the split between a right-aligned user bubble
 * and naturally flowing assistant text are derived from MiniiChat's
 * MIT-licensed ChatScreen interaction structure. Coast-specific layout,
 * strings, state, and visuals are rewritten here. See THIRD_PARTY_NOTICES.md.
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.MessageRole

@Composable
fun ChatScreen(
    state: CoastShellState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onOpenModels: () -> Unit,
    onDismissModels: () -> Unit,
    onSelectModel: (String) -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val tailLength = state.messages.lastOrNull()?.text?.length ?: 0

    LaunchedEffect(state.messages.size, tailLength) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        CoastHeader(model = state.currentModel, onOpenModels = onOpenModels)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                MessageItem(
                    message = message,
                    isStreamingTail = state.isStreaming && message.id == state.messages.lastOrNull()?.id
                )
            }
        }

        InputBar(
            value = input,
            onValueChange = { input = it },
            isStreaming = state.isStreaming,
            onSend = {
                val outgoing = input
                input = ""
                onSend(outgoing)
            },
            onStop = onStop
        )
    }

    if (state.showModelPicker) {
        ModelPickerSheet(
            models = state.models,
            currentModel = state.currentModel,
            onPick = onSelectModel,
            onDismiss = onDismissModels
        )
    }
}

@Composable
private fun CoastHeader(model: String, onOpenModels: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Text(
            "CoastGPT",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
                .clickable(onClick = onOpenModels)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                model,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Choose model",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun MessageItem(message: ChatMessage, isStreamingTail: Boolean) {
    if (message.role == MessageRole.User) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = message.text,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 5.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Myri · ${if (isStreamingTail) "streaming" else "assistant"}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = if (message.text.isEmpty() && isStreamingTail) "•••" else message.text,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
