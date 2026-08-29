package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.theme.CoastSpacing
import com.elementeracoast.app.model.CoastModel

@Composable
fun ChatScreen(
    state: ChatUiState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onModelSelect: (CoastModel) -> Unit,
    onThemeCycle: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showModels by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize()) {
            ModelHeader(
                modelName = state.currentModelName,
                footprint = state.footprint,
                onModelClick = { showModels = true },
                onThemeCycle = onThemeCycle,
            )

            if (state.error != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CoastSpacing.lg, vertical = CoastSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> QuietLoadingState()
                    state.messages.isEmpty() -> EmptyChatState()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = CoastSpacing.sm,
                            end = CoastSpacing.sm,
                            top = CoastSpacing.xs,
                            bottom = CoastSpacing.lg,
                        ),
                        verticalArrangement = Arrangement.spacedBy(CoastSpacing.xs),
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            MessageBubble(message)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                InputBar(
                    value = input,
                    onValueChange = { input = it },
                    generating = state.generating,
                    loading = state.loading,
                    onSend = {
                        val text = input.trim()
                        if (text.isNotEmpty() && !state.generating) {
                            onSend(text)
                            input = ""
                        }
                    },
                    onStop = onStop,
                )
            }
        }
    }

    if (showModels) {
        ModelSelector(
            models = state.models,
            currentModelId = state.currentModelId,
            onDismiss = { showModels = false },
            onSelect = onModelSelect,
        )
    }
}

@Composable
private fun QuietLoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(bottom = CoastSpacing.sm),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            strokeWidth = 1.5.dp,
        )
        Text(
            "正在接回海岸",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
        )
    }
}

@Composable
private fun EmptyChatState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = CoastSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "主聊天窗口已经亮灯",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        )
        Text(
            "写下一句话，回声会从这里回来。",
            modifier = Modifier.padding(top = CoastSpacing.xs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
        )
    }
}
