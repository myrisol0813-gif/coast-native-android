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
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.model.WorldbookRecord

@Composable
internal fun WorldbookScreen(store: LocalMemoryStore, onSnackbar: (String) -> Unit) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WorldbookRecord?>(null) }
    val visible = remember(query, state.worldbook) { store.searchWorldbook(query) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Text("世界书", style = MaterialTheme.typography.headlineMedium)
            Text("海岸词典 · 本地启停", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(query, { query = it.take(120) }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索词条") })
            Button(onClick = { creating = true }, modifier = Modifier.padding(top = 8.dp)) { Text("新增词条") }
        }
        if (visible.isEmpty()) item { Text("当前没有匹配词条。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        visible.forEach { entry ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row {
                        Text(entry.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(if (entry.enabled) "已启用" else "已停用", color = MaterialTheme.colorScheme.primary)
                    }
                    Text(entry.content, maxLines = 4)
                    Row {
                        TextButton(onClick = { editing = entry }) { Text("编辑") }
                        TextButton(onClick = { store.toggleWorldbook(entry.id) }) { Text(if (entry.enabled) "停用" else "启用") }
                        TextButton(onClick = {
                            if (store.deleteWorldbook(entry.id)) onSnackbar("世界书词条已删除")
                        }) { Text("删除") }
                    }
                }
            }
        }
    }

    if (creating) {
        WorldbookEditor(null, { creating = false }) { title, content ->
            store.addWorldbook(title, content)?.let { onSnackbar("世界书词条已保存到本机") }
            creating = false
        }
    }
    editing?.let { entry ->
        WorldbookEditor(entry, { editing = null }) { title, content ->
            store.editWorldbook(entry.copy(title = title, content = content))
            editing = null
        }
    }
}

@Composable
private fun WorldbookEditor(
    initial: WorldbookRecord?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial?.id) { mutableStateOf(initial?.content.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "新增词条" else "编辑词条") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, label = { Text("标题") })
                OutlinedTextField(content, { content = it.take(12000) }, label = { Text("内容") }, minLines = 4)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, content) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
