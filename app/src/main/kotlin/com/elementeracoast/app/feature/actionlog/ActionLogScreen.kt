package com.elementeracoast.app.feature.actionlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.model.ActionLogRecord

@Composable
fun ActionLogScreen(store: LocalActionLogStore) {
    val state by store.state.collectAsState()
    val visible = store.visibleRecords()
    val actionKeys = state.records.map { it.actionKey }.distinct().sorted()
    val conversations = state.records.map { it.conversationId }.filter(String::isNotBlank).distinct().sorted()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Serpent Action Log / 小蛇行动日志", style = MaterialTheme.typography.headlineMedium)
            Text(
                "海岸家具 · 本地动作摘要 · 工具透明层",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (state.focusedActionIds.isNotEmpty()) {
            item {
                FilterChipRow(
                    label = "来自本轮家具 · ${state.focusedActionIds.size} 个 action id",
                    active = true,
                    onClick = store::clearFocus
                )
            }
        }
        item {
            Text("状态", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 6.dp)) {
                FilterChipRow("全部", state.statusFilter.isBlank()) { store.setFilters(status = "") }
                FilterChipRow("success", state.statusFilter == "success") { store.setFilters(status = "success") }
                FilterChipRow("error", state.statusFilter == "error") { store.setFilters(status = "error") }
            }
        }
        item {
            Text("类型", fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                FilterChipRow("全部类型", state.actionFilter.isBlank()) { store.setFilters(action = "") }
                actionKeys.forEach { key ->
                    FilterChipRow(key, state.actionFilter == key) { store.setFilters(action = key) }
                }
            }
        }
        if (conversations.size > 1) {
            item {
                Text("conversation", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    FilterChipRow("全部窗口", state.conversationFilter.isBlank()) { store.setFilters(conversation = "") }
                    conversations.take(12).forEach { id ->
                        FilterChipRow(id, state.conversationFilter == id) { store.setFilters(conversation = id) }
                    }
                }
            }
        }
        item { Text("行动记录 · ${visible.size}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp)) }
        if (visible.isEmpty()) {
            item {
                Text(
                    "当前筛选下还没有本地行动记录。",
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            visible.forEach { record -> item { ActionLogCard(record) } }
        }
    }
}

@Composable
private fun FilterChipRow(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.background(
            if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            RoundedCornerShape(999.dp)
        ).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun ActionLogCard(record: ActionLogRecord) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row {
            Text(
                if (record.status == "error") "! ${record.label}" else "✓ ${record.label}",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
            )
            Text(record.status, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text(record.actionKey, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        Text("${record.roomType.wireValue} · ${record.conversationId}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text("input 摘要：${record.inputSummary.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
        Text("output 摘要：${record.outputSummary.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
        record.errorMessage?.let { Text("错误：$it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Text("action_id · ${record.actionId}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
