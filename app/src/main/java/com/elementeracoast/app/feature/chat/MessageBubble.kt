package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.theme.CoastShapes
import com.elementeracoast.app.core.theme.CoastSpacing
import com.elementeracoast.app.model.ChatMessage

@Composable
fun MessageBubble(message: ChatMessage) {
    val user = message.role == ChatMessage.Role.USER
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (user) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (user) 0.78f else 0.94f)
                .widthIn(max = if (user) 430.dp else 620.dp)
                .background(
                    color = if (user) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.46f),
                    shape = if (user) CoastShapes.userMessage else CoastShapes.assistantMessage,
                )
                .padding(
                    horizontal = if (user) CoastSpacing.md else CoastSpacing.lg,
                    vertical = if (user) CoastSpacing.sm else CoastSpacing.md,
                ),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!user && message.modelId != null) {
                Text(
                    text = buildString {
                        append(message.modelId.substringAfterLast('/').removeSuffix(":free"))
                        message.totalTokens?.let { append(" · $it tok") }
                    },
                    modifier = Modifier.padding(top = CoastSpacing.xs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
                )
            }
        }
    }
}
