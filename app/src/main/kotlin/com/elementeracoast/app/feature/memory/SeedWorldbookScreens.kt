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
import com.elementeracoast.app.feature.daily.DailyField
import com.elementeracoast.app.feature.daily.DailyPrimaryButton
import com.elementeracoast.app.feature.daily.DailySurfaceCard
import com.elementeracoast.app.feature.wolf.ChoiceRow
import com.elementeracoast.app.feature.wolf.WolfTextField

@Composable
internal fun SeedLibraryScreen(store: MemoryStore, createRequest: Int, onSnackbar: (String) -> Unit) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var filterKind by remember { mutableStateOf(MemoryFilterKind.Tag) }
    var filterValue by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LocalSeed?>(null) }
    var creating by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(createRequest) { if (createRequest > 0) creating = true }

    val visible = store.searchSeeds(query)
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
                values = emptyList(),
                onFilterValueChange = { filterValue = it }
            )
        }
        item { MemoryPendingCard(0) { onSnackbar("本地待确认袋目前还是空的") } }
        item {
            Text(
                "种子库",
                modifier = Modifier.padding(horizontal = 36.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (visible.isEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 28.dp)) {
                    DailySurfaceCard { Text(if (state.seeds.isEmpty()) "这里还没有种子。" else "没有匹配的种子。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        } else {
            items(visible, key = { it.id }) { seed ->
                Column(Modifier.padding(horizontal = 28.dp)) {
                    DailySurfaceCard(onClick = { expandedId = if (expandedId == seed.id) null else seed.id }) {
                        Text(seed.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(seed.status.name.lowercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        if (expandedId == seed.id) {
                            Spacer(Modifier.height(8.dp))
                            Text(seed.content, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                Text("编辑", modifier = Modifier.clickable { editing = seed }, color = MaterialTheme.colorScheme.primary)
                                Text("删除", modifier = Modifier.clickable { store.deleteSeed(seed.id) }, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
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
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                WolfTextField("标题", title, { title = it })
                WolfTextField("内容", content, { content = it }, minLines = 3)
                SeedStatus.entries.forEach { ChoiceRow(it.name.lowercase(), status == it) { status = it } }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(LocalSeed(seed?.id.orEmpty(), title, content, status)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun WorldbookScreen(store: MemoryStore, createRequest: Int, onSnackbar: (String) -> Unit) {
    val state by store.state.collectAsState()
    var testText by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<List<LocalWorldbookEntry>>(emptyList()) }
    var editing by remember { mutableStateOf<LocalWorldbookEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    LaunchedEffect(createRequest) { if (createRequest > 0) creating = true }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DailySurfaceCard {
                DailyField("试一句", testText, { testText = it }, "聊到哪个海岸名词，就试哪个")
                Spacer(Modifier.height(14.dp))
                DailyPrimaryButton("测试命中") {
                    testResult = state.worldbook.filter { it.enabled && testText.contains(it.term, ignoreCase = true) }
                    onSnackbar(if (testResult.isEmpty()) "没有命中启用词条" else "命中 ${testResult.size} 条")
                }
                if (testResult.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    testResult.forEach { Text("✓ ${it.term}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if (state.worldbook.isEmpty()) {
            item {
                DailySurfaceCard {
                    Text("这里还没有词条。以后聊到某个海岸名词时，再整理进来。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            items(state.worldbook, key = { it.id }) { entry ->
                DailySurfaceCard {
                    Text(entry.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (entry.enabled) "已启用" else "已停用", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(entry.content)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(if (entry.enabled) "停用" else "启用", modifier = Modifier.clickable { store.toggleWorldbook(entry.id) }, color = MaterialTheme.colorScheme.primary)
                        Text("编辑", modifier = Modifier.clickable { editing = entry }, color = MaterialTheme.colorScheme.primary)
                        Text("删除", modifier = Modifier.clickable { store.deleteWorldbook(entry.id) }, color = MaterialTheme.colorScheme.primary)
                    }
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
        title = { Text(if (entry == null) "新增词条" else "编辑词条") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                WolfTextField("词条", term, { term = it })
                WolfTextField("内容", content, { content = it }, minLines = 3)
                ChoiceRow(if (enabled) "已启用" else "已停用", enabled) { enabled = !enabled }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(LocalWorldbookEntry(entry?.id.orEmpty(), term, content, enabled)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
