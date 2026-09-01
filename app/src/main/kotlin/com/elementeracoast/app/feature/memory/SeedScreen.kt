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
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.SeedRecord
import com.elementeracoast.app.core.model.SeedStatus

@Composable
internal fun SeedScreen(
    store: LocalMemoryStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SeedRecord?>(null) }
    val visible = remember(query, state.seeds) { store.searchSeeds(query) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Text("种子库", style = MaterialTheme.typography.headlineMedium)
            Text("active / dormant · local-only", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(query, { query = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索种子") })
            Button(onClick = { creating = true }, modifier = Modifier.padding(top = 8.dp)) { Text("新增种子") }
        }
        if (visible.isEmpty()) item { Text("当前没有匹配种子。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        visible.forEach { seed ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row {
                        Text(seed.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(seed.status.name.lowercase(), color = MaterialTheme.colorScheme.primary)
                    }
                    Text(seed.content, maxLines = 4)
                    Row {
                        TextButton(onClick = { editing = seed }) { Text("编辑") }
                        TextButton(onClick = {
                            val next = if (seed.status == SeedStatus.Active) SeedStatus.Dormant else SeedStatus.Active
                            store.editSeed(seed.copy(status = next))
                        }) { Text(if (seed.status == SeedStatus.Active) "休眠" else "激活") }
                        TextButton(onClick = {
                            if (store.deleteSeed(seed.id)) onSnackbar("本地种子已删除")
                        }) { Text("删除") }
                    }
                }
            }
        }
    }

    if (creating) {
        SeedEditor(null, { creating = false }) { title, content, status ->
            store.addSeed(title, content, status)?.let {
                actionLogStore.record(
                    actionKey = "memory.write_candidate",
                    label = "写入了一颗本地种子",
                    roomType = roomType,
                    conversationId = conversationId,
                    inputSummary = "seed body omitted",
                    outputSummary = "local seed stored"
                )
                onSnackbar("种子已写入本机")
            }
            creating = false
        }
    }
    editing?.let { seed ->
        SeedEditor(seed, { editing = null }) { title, content, status ->
            store.editSeed(seed.copy(title = title, content = content, status = status))
            editing = null
        }
    }
}

@Composable
private fun SeedEditor(
    initial: SeedRecord?,
    onDismiss: () -> Unit,
    onSave: (String, String, SeedStatus) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial?.id) { mutableStateOf(initial?.content.orEmpty()) }
    var status by remember(initial?.id) { mutableStateOf(initial?.status ?: SeedStatus.Active) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增种子" else "编辑种子") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("标题") })
                OutlinedTextField(content, { content = it.take(8000) }, label = { Text("内容") }, minLines = 3)
                TextButton(onClick = { status = if (status == SeedStatus.Active) SeedStatus.Dormant else SeedStatus.Active }) {
                    Text("状态：${status.name.lowercase()}")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, content, status) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
