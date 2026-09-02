package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.daily.QuietDailyCard
import com.elementeracoast.app.feature.daily.SmallButton
import com.elementeracoast.app.feature.wolf.WolfTextField

@Composable
internal fun MemoryLibraryScreen(store: MemoryStore, onActionLogged: (String, String, String) -> Unit) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LocalMemoryEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    val visible = store.searchMemories(query)

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("记忆库", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("本地确认纸条", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SmallButton("新增") { creating = true }
            }
        }
        item { WolfTextField("搜索标题、核心、内容、分类或标签", query, { query = it }) }
        if (visible.isEmpty()) {
            item { QuietDailyCard { Text(if (state.memories.isEmpty()) "这里还没有长期记忆。" else "没有匹配的记忆。", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(visible, key = { it.id }) { entry ->
                QuietDailyCard {
                    Text(entry.title, fontWeight = FontWeight.SemiBold)
                    Text("${entry.category}${if (entry.tags.isEmpty()) "" else " · ${entry.tags.joinToString(" / ")}"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (entry.lifeCore.isNotBlank()) Text(entry.lifeCore, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("编辑", modifier = Modifier.clickable { editing = entry }, color = MaterialTheme.colorScheme.primary)
                        Text("删除", modifier = Modifier.clickable {
                            store.deleteMemory(entry.id)
                            onActionLogged("memory.delete", "删除本地记忆", "删除 1 条本地记忆")
                        }, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (creating) MemoryEditor(null, onDismiss = { creating = false }) { draft ->
        store.saveMemory(draft)
        onActionLogged("memory.write", "写入本地记忆", "新增 1 条本地记忆")
        creating = false
    }
    editing?.let { entry ->
        MemoryEditor(entry, onDismiss = { editing = null }) { draft ->
            store.saveMemory(draft.copy(id = entry.id))
            onActionLogged("memory.edit", "编辑本地记忆", "更新 1 条本地记忆")
            editing = null
        }
    }
}

@Composable
private fun MemoryEditor(entry: LocalMemoryEntry?, onDismiss: () -> Unit, onSave: (LocalMemoryEntry) -> Unit) {
    var title by remember(entry?.id) { mutableStateOf(entry?.title ?: "") }
    var lifeCore by remember(entry?.id) { mutableStateOf(entry?.lifeCore ?: "") }
    var content by remember(entry?.id) { mutableStateOf(entry?.content ?: "") }
    var usageHint by remember(entry?.id) { mutableStateOf(entry?.usageHint ?: "") }
    var avoidHint by remember(entry?.id) { mutableStateOf(entry?.avoidHint ?: "") }
    var tags by remember(entry?.id) { mutableStateOf(entry?.tags?.joinToString(", ") ?: "") }
    var category by remember(entry?.id) { mutableStateOf(entry?.category ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "新增记忆" else "编辑记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                WolfTextField("标题", title, { title = it })
                WolfTextField("life_core / 核心", lifeCore, { lifeCore = it })
                WolfTextField("content", content, { content = it }, minLines = 3)
                WolfTextField("usage_hint", usageHint, { usageHint = it })
                WolfTextField("avoid_hint", avoidHint, { avoidHint = it })
                WolfTextField("分类", category, { category = it })
                WolfTextField("tags · 逗号分隔", tags, { tags = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    LocalMemoryEntry(
                        entry?.id.orEmpty(), title, lifeCore, content, usageHint, avoidHint,
                        tags.split(',').map(String::trim).filter(String::isNotBlank), category
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
