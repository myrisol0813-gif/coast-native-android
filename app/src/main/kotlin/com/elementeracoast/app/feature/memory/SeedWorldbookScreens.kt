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
import com.elementeracoast.app.feature.wolf.ChoiceRow
import com.elementeracoast.app.feature.wolf.WolfTextField

@Composable
internal fun SeedLibraryScreen(store: MemoryStore) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LocalSeed?>(null) }
    var creating by remember { mutableStateOf(false) }
    val visible = store.searchSeeds(query)
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) { Text("种子库", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("active / dormant", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                SmallButton("新增") { creating = true }
            }
        }
        item { WolfTextField("搜索种子", query) { query = it } }
        if (visible.isEmpty()) item { QuietDailyCard { Text(if (state.seeds.isEmpty()) "还没有种子。" else "没有匹配的种子。") } }
        items(visible, key = { it.id }) { seed ->
            QuietDailyCard {
                Text(seed.title, fontWeight = FontWeight.SemiBold)
                Text(seed.status.name.lowercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(seed.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("编辑", modifier = Modifier.clickable { editing = seed }, color = MaterialTheme.colorScheme.primary)
                    Text("删除", modifier = Modifier.clickable { store.deleteSeed(seed.id) }, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    if (creating) SeedEditor(null, { creating = false }) { store.saveSeed(it); creating = false }
    editing?.let { seed -> SeedEditor(seed, { editing = null }) { store.saveSeed(it.copy(id = seed.id)); editing = null } }
}

@Composable
private fun SeedEditor(seed: LocalSeed?, onDismiss: () -> Unit, onSave: (LocalSeed) -> Unit) {
    var title by remember(seed?.id) { mutableStateOf(seed?.title ?: "") }
    var content by remember(seed?.id) { mutableStateOf(seed?.content ?: "") }
    var status by remember(seed?.id) { mutableStateOf(seed?.status ?: SeedStatus.Active) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (seed == null) "新增种子" else "编辑种子") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            WolfTextField("标题", title) { title = it }
            WolfTextField("内容", content, { content = it }, minLines = 3)
            SeedStatus.entries.forEach { ChoiceRow(it.name.lowercase(), status == it) { status = it } }
        } },
        confirmButton = { TextButton(onClick = { onSave(LocalSeed(seed?.id.orEmpty(), title, content, status)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun WorldbookScreen(store: MemoryStore) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LocalWorldbookEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    val visible = store.searchWorldbook(query)
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) { Text("世界书", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("海岸词典 · 本地启停", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                SmallButton("新增") { creating = true }
            }
        }
        item { WolfTextField("搜索词条", query) { query = it } }
        if (visible.isEmpty()) item { QuietDailyCard { Text(if (state.worldbook.isEmpty()) "还没有世界书词条。" else "没有匹配词条。") } }
        items(visible, key = { it.id }) { entry ->
            QuietDailyCard {
                Text(entry.term, fontWeight = FontWeight.SemiBold)
                Text(if (entry.enabled) "已启用" else "已停用", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(entry.content)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(if (entry.enabled) "停用" else "启用", modifier = Modifier.clickable { store.toggleWorldbook(entry.id) }, color = MaterialTheme.colorScheme.primary)
                    Text("编辑", modifier = Modifier.clickable { editing = entry }, color = MaterialTheme.colorScheme.primary)
                    Text("删除", modifier = Modifier.clickable { store.deleteWorldbook(entry.id) }, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    if (creating) WorldbookEditor(null, { creating = false }) { store.saveWorldbook(it); creating = false }
    editing?.let { entry -> WorldbookEditor(entry, { editing = null }) { store.saveWorldbook(it.copy(id = entry.id)); editing = null } }
}

@Composable
private fun WorldbookEditor(entry: LocalWorldbookEntry?, onDismiss: () -> Unit, onSave: (LocalWorldbookEntry) -> Unit) {
    var term by remember(entry?.id) { mutableStateOf(entry?.term ?: "") }
    var content by remember(entry?.id) { mutableStateOf(entry?.content ?: "") }
    var enabled by remember(entry?.id) { mutableStateOf(entry?.enabled ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "新增世界书" else "编辑世界书") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            WolfTextField("词条", term) { term = it }
            WolfTextField("内容", content, { content = it }, minLines = 3)
            ChoiceRow(if (enabled) "已启用" else "已停用", enabled) { enabled = !enabled }
        } },
        confirmButton = { TextButton(onClick = { onSave(LocalWorldbookEntry(entry?.id.orEmpty(), term, content, enabled)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
