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

internal fun furnitureHeadline(runs: List<FurnitureSummary>): String? =
    runs.takeIf { it.isNotEmpty() }?.let { "小蛇摆弄了 ${it.size} 件家具" }

internal fun furnitureActionIds(runs: List<FurnitureSummary>): List<String> =
    runs.map { it.actionId }.filter(String::isNotBlank)

internal fun furnitureSafeSummaryLines(runs: List<FurnitureSummary>): List<String> = buildList {
    runs.forEach { run ->
        val prefix = if (run.status == "error") "!" else "✓"
        val suffix = if (run.actionKey == "memory.search") "：${run.count} 条" else ""
        add("$prefix ${run.label}$suffix")
        run.items.take(5).forEach { item -> add("· ${item.kind}｜${item.title}") }
        run.errorType?.let { add("错误类型：$it") }
    }
}

@Composable
internal fun FurnitureBubble(
    runs: List<FurnitureSummary>,
    conversationId: String,
    onOpenActionLog: (List<String>, String) -> Unit
) {
    val headline = furnitureHeadline(runs) ?: return
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
            Text(headline, fontWeight = FontWeight.SemiBold)
        }
        if (expanded) {
            furnitureSafeSummaryLines(runs).forEach { line ->
                Text(
                    line,
                    color = if (line.startsWith("错误类型")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = if (line.startsWith("·")) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (line.startsWith("✓") || line.startsWith("!")) FontWeight.Medium else FontWeight.Normal
                )
            }
            TextButton(
                onClick = { onOpenActionLog(furnitureActionIds(runs), conversationId) }
            ) { Text("查看小蛇行动日志") }
        }
    }
}
