package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.MemoryRecord
import com.elementeracoast.app.core.model.RoomType

@Composable
internal fun MemoryRecordsScreen(
    store: LocalMemoryStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MemoryRecord?>(null) }
    val visible = remember(query, state.memories) { store.searchMemories(query) }

    fun logSearch() {
        val items = visible.take(5).map { FurnitureItem(it.tags.firstOrNull() ?: "记忆", it.title) }
        actionLogStore.record(
            actionKey = "memory.search",
            label = "搜索了记忆",
            roomType = roomType,
            conversationId = conversationId,
            inputSummary = "query length ${query.length}",
            outputSummary = "${visible.size} local hits",
            count = visible.size.coerceAtLeast(1),
            items = items
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Text("记忆库", style = MaterialTheme.typography.headlineMedium)
            Text("标题 · 核心 · content · usage / avoid · tags", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                query,
                { query = it.take(120) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("搜索记忆") },
                trailingIcon = { TextButton(onClick = ::logSearch) { Text("记录搜索") } }
            )
            Button(onClick = { creating = true }, modifier = Modifier.padding(top = 8.dp)) { Text("新增记忆") }
        }
        if (visible.isEmpty()) item { Text("当前没有匹配记忆。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        visible.forEach { record ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(record.title, fontWeight = FontWeight.SemiBold)
                    if (record.tags.isNotEmpty()) Text(record.tags.joinToString(" · "), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    if (record.lifeCore.isNotBlank()) Text(record.lifeCore, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(record.content, maxLines = 4)
                    Row {
                        TextButton(onClick = { editing = record }) { Text("编辑") }
                        TextButton(onClick = {
                            if (store.deleteMemory(record.id)) onSnackbar("本地记忆已删除")
                        }) { Text("删除") }
                    }
                }
            }
        }
    }

    if (creating) {
        MemoryEditor(
            initial = null,
            onDismiss = { creating = false },
            onSave = { title, core, content, usage, avoid, tags ->
                store.addMemory(title, core, content, usage, avoid, tags)?.let { onSnackbar("记忆已写入本机") }
                creating = false
            }
        )
    }
    editing?.let { record ->
        MemoryEditor(
            initial = record,
            onDismiss = { editing = null },
            onSave = { title, core, content, usage, avoid, tags ->
                store.editMemory(record.copy(title = title, lifeCore = core, content = content, usageHint = usage, avoidHint = avoid, tags = tags))
                editing = null
            }
        )
    }
}

@Composable
private fun MemoryEditor(
    initial: MemoryRecord?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, List<String>) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var core by remember(initial?.id) { mutableStateOf(initial?.lifeCore.orEmpty()) }
    var content by remember(initial?.id) { mutableStateOf(initial?.content.orEmpty()) }
    var usage by remember(initial?.id) { mutableStateOf(initial?.usageHint.orEmpty()) }
    var avoid by remember(initial?.id) { mutableStateOf(initial?.avoidHint.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags?.joinToString(",").orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增记忆" else "编辑记忆") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("标题") })
                OutlinedTextField(core, { core = it.take(1000) }, label = { Text("life_core / 核心") })
                OutlinedTextField(content, { content = it.take(12000) }, label = { Text("content") }, minLines = 3)
                OutlinedTextField(usage, { usage = it.take(2000) }, label = { Text("usage_hint") })
                OutlinedTextField(avoid, { avoid = it.take(2000) }, label = { Text("avoid_hint") })
                OutlinedTextField(tags, { tags = it.take(400) }, label = { Text("tags · 逗号分隔") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(title, core, content, usage, avoid, tags.split(',').map(String::trim).filter(String::isNotBlank))
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
