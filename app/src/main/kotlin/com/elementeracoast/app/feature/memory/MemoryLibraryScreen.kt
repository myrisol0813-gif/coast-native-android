package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.daily.DailySurfaceCard
import com.elementeracoast.app.feature.wolf.WolfTextField

@Composable
internal fun MemoryLibraryScreen(
    store: MemoryStore,
    createRequest: Int,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var filterKind by remember { mutableStateOf(MemoryFilterKind.Tag) }
    var filterValue by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LocalMemoryEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(createRequest) { if (createRequest > 0) creating = true }

    val filterValues = when (filterKind) {
        MemoryFilterKind.Tag -> (canonicalMemoryTags + state.memories.flatMap { it.tags } + state.memories.map { it.category })
            .filter(String::isNotBlank).distinct()
        else -> emptyList()
    }
    val visible = store.searchMemories(query).filter { entry ->
        filterValue.isBlank() || when (filterKind) {
            MemoryFilterKind.Tag -> filterValue in entry.tags || filterValue == entry.category
            else -> true
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 2.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MemoryRetrievalCard(
                query = query,
                onQueryChange = { query = it },
                filterKind = filterKind,
                onFilterKindChange = { filterKind = it },
                filterValue = filterValue,
                values = filterValues,
                onFilterValueChange = { filterValue = it }
            )
        }
        item {
            MemoryPendingCard(0) { onSnackbar("本地待确认袋目前还是空的") }
        }
        item {
            Text(
                "记忆库",
                modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (visible.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 28.dp)) {
                    DailySurfaceCard {
                        Text(
                            if (state.memories.isEmpty()) "这里还没有长期记忆。" else "没有匹配的记忆。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else {
            items(visible, key = { it.id }) { entry ->
                Column(Modifier.padding(horizontal = 28.dp)) {
                    DailySurfaceCard(onClick = { expandedId = if (expandedId == entry.id) null else entry.id }) {
                        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (entry.lifeCore.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(entry.lifeCore, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        val meta = (entry.tags + entry.category).filter(String::isNotBlank).distinct().joinToString(" · ")
                        if (meta.isNotBlank()) Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        if (expandedId == entry.id) {
                            Spacer(Modifier.height(12.dp))
                            if (entry.content.isNotBlank()) Text(entry.content, style = MaterialTheme.typography.bodyMedium)
                            if (entry.usageHint.isNotBlank()) Text("使用时机：${entry.usageHint}", style = MaterialTheme.typography.bodySmall)
                            if (entry.avoidHint.isNotBlank()) Text("勿误用：${entry.avoidHint}", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
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
                WolfTextField("核心", lifeCore, { lifeCore = it })
                WolfTextField("内容", content, { content = it }, minLines = 3)
                WolfTextField("使用时机", usageHint, { usageHint = it })
                WolfTextField("勿误用", avoidHint, { avoidHint = it })
                WolfTextField("标签", tags, { tags = it })
                WolfTextField("分类", category, { category = it })
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
