/*
 * The multiline composer and send/stop-in-one-place interaction are derived
 * from MiniiChat's MIT-licensed InputBar concept. Attachments and provider
 * concerns were removed. Text colors are explicitly tied to the Coast theme
 * to avoid MiniiChat's reported dark-mode LocalContentColor issue.
 * See THIRD_PARTY_NOTICES.md.
 */
package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val focus = LocalFocusManager.current
    val canSend = value.trim().isNotEmpty() && !isStreaming

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp, max = 160.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    "Message CoastGPT",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 6
            )
        }

        Spacer(Modifier.width(8.dp))

        val buttonBackground = when {
            isStreaming -> MaterialTheme.colorScheme.onSurface
            canSend -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val buttonForeground = when {
            isStreaming -> MaterialTheme.colorScheme.background
            canSend -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .background(buttonBackground, CircleShape)
                .clickable(enabled = isStreaming || canSend) {
                    if (isStreaming) {
                        onStop()
                    } else if (canSend) {
                        focus.clearFocus()
                        onSend()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.ArrowUpward,
                contentDescription = if (isStreaming) "Stop" else "Send",
                tint = buttonForeground,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
