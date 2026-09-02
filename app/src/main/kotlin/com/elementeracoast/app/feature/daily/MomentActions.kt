package com.elementeracoast.app.feature.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal const val MyriCommentActionLabel = "叫 Myri 来评论"
internal const val MyriCommentOfflineMessage = "真实 Myri 评论将在后端接线后启用。"

@Composable
internal fun MomentActionRows(
    moment: LocalMoment,
    footer: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onDelete: () -> Unit,
    onMyriComment: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            footer,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal)
        )
        IconButton(onClick = onLike, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = if (moment.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (moment.liked) "取消点赞" else "点赞",
                tint = if (moment.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onComment, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = "评论",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    Spacer(Modifier.height(5.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        val chipShape = RoundedCornerShape(13.dp)
        Text(
            MyriCommentActionLabel,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, chipShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, chipShape)
                .clickable(onClick = onMyriComment)
                .padding(horizontal = 11.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
        )
    }
}
