package com.elementeracoast.app.feature.serpentdesk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.actionlog.ActionLogScreen
import com.elementeracoast.app.feature.actionlog.ActionLogStore

private enum class SerpentDeskTool { ActionLog }

@Composable
fun SerpentDeskScreen(
    actionLogStore: ActionLogStore,
    conversationId: String,
    focusIds: Set<String>
) {
    var activeTool by remember(focusIds) {
        mutableStateOf(if (focusIds.isNotEmpty()) SerpentDeskTool.ActionLog else null)
    }

    if (activeTool == SerpentDeskTool.ActionLog) {
        ActionLogToolPage(
            store = actionLogStore,
            conversationId = conversationId,
            focusIds = focusIds,
            onBack = { activeTool = null }
        )
    } else {
        SerpentDeskHome(onOpenActionLog = { activeTool = SerpentDeskTool.ActionLog })
    }
}

@Composable
private fun SerpentDeskHome(onOpenActionLog: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
        Text(
            "小蛇书桌",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Myri 的工作台",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(18.dp))
        val shape = RoundedCornerShape(22.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, shape, clip = false)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape)
                .clickable(onClick = onOpenActionLog)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "小蛇行动日志",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "工具调用成功 / 失败 · 房间 · 脱敏摘要",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ActionLogToolPage(
    store: ActionLogStore,
    conversationId: String,
    focusIds: Set<String>,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回小蛇书桌",
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.size(8.dp))
            Column {
                Text("小蛇行动日志", fontWeight = FontWeight.Bold)
                Text(
                    "工具调用成功 / 失败 · 房间 · 脱敏摘要",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        ActionLogScreen(
            store = store,
            conversationId = conversationId,
            focusIds = focusIds,
            modifier = Modifier.weight(1f)
        )
    }
}
