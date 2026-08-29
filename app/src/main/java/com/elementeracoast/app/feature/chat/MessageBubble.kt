package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                .fillMaxWidth(if (user) 0.84f else 0.94f)
                .background(
                    if (user) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(message.content, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (!user && message.modelId != null) {
                Text(
                    text = buildString {
                        append(message.modelId.substringAfterLast('/').removeSuffix(":free"))
                        message.totalTokens?.let { append(" · $it tok") }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
