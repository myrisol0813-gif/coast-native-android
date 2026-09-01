package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.FurnitureSummary

@Composable
internal fun FurnitureBubble(
    runs: List<FurnitureSummary>,
    conversationId: String,
    onOpenActionLog: (List<String>, String) -> Unit
) {
    if (runs.isEmpty()) return
    var expanded by remember(runs.map { it.actionId }) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).background(
            MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(16.dp)
        ).clickable { expanded = !expanded }.padding(horizontal = 13.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row {
            Text("本轮家具", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("小蛇摆弄了 ${runs.size} 件家具", fontWeight = FontWeight.SemiBold)
        }
        if (expanded) {
            runs.forEach { run ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val prefix = if (run.status == "error") "!" else "✓"
                    val suffix = if (run.actionKey == "memory.search") "：${run.count} 条" else ""
                    Text("$prefix ${run.label}$suffix", fontWeight = FontWeight.Medium)
                    run.items.take(5).forEach { item ->
                        Text("· ${item.kind}｜${item.title}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    run.errorType?.let { Text("错误类型：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
            TextButton(
                onClick = { onOpenActionLog(runs.map { it.actionId }, conversationId) }
            ) { Text("查看小蛇行动日志") }
        }
    }
}
