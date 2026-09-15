package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatAttachment
import com.elementeracoast.app.ui.theme.SnowLetterSurface
import com.elementeracoast.app.ui.theme.SnowLetterSurfaceRole

private fun attachmentSizeLabel(value: Long): String = when {
    value < 1024L -> "${value} B"
    value < 1024L * 1024L -> "${(value / 1024.0).let { if (it < 10) "%.1f".format(it) else "%.0f".format(it) }} KB"
    else -> "%.1f MB".format(value / 1024.0 / 1024.0)
}

@Composable
internal fun PendingAttachmentTray(
    attachments: List<ChatAttachment>,
    uploading: Boolean,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty() && !uploading) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { attachment ->
            AttachmentCard(
                attachment = attachment,
                removable = true,
                onRemove = { onRemove(attachment.id) }
            )
        }
        if (uploading) {
            SnowLetterSurface(
                role = SnowLetterSurfaceRole.StatusCard,
                fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .66f),
                fallbackShape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "正在收好附件…",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun MessageAttachmentList(
    attachments: List<ChatAttachment>,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        attachments.forEach { attachment ->
            AttachmentCard(attachment = attachment)
        }
    }
}

@Composable
private fun AttachmentCard(
    attachment: ChatAttachment,
    removable: Boolean = false,
    onRemove: () -> Unit = {}
) {
    SnowLetterSurface(
        role = SnowLetterSurfaceRole.StatusCard,
        fallbackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
        fallbackShape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = if (removable) 2.dp else 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (attachment.type == "image") Icons.Default.Image else Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(8.dp))
            Column(modifier = Modifier.padding(end = 6.dp)) {
                Text(
                    attachment.name,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${attachment.mime.ifBlank { "文件" }} · ${attachmentSizeLabel(attachment.size)}",
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (removable) {
                IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "移除附件", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
