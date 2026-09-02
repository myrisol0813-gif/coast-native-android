package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.elementeracoast.app.feature.wolf.ChoiceRow
import com.elementeracoast.app.feature.wolf.WolfTextField
import java.time.LocalDate

@Composable
internal fun SeedLibraryScreen(
    store: MemoryStore,
    createRequested: Boolean,
    onCreateConsumed: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var filterKind by remember { mutableStateOf(MemoryFilterKind.Tag) }
    var filterValue by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LocalSeed?>(null) }
    var creating by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(createRequested) {
        if (createRequested) {
            creating = true
            onCreateConsumed()
        }
    }

    val filterValues = when (filterKind) {
        MemoryFilterKind.Tag -> (canonicalMemoryTags + state.seeds.flatMap { it.tags }).distinct()
        MemoryFilterKind.Date -> state.seeds.map { it.sourceDate }.filter(String::isNotBlank).distinct().sortedDescending()
        MemoryFilterKind.Model -> state.seeds.map { it.sourceModel }.filter(String::isNotBlank).distinct().sorted()
        MemoryFilterKind.Window -> state.seeds.map { it.sourceWindow }.filter(String::isNotBlank).distinct().sorted()
    }
    val visible = store.searchSeeds(query).filter { seed ->
        filterValue.isBlank() || when (filterKind) {
            MemoryFilterKind.Tag -> filterValue in seed.tags
            MemoryFilterKind.Date -> seed.sourceDate == filterValue
            MemoryFilterKind.Model -> seed.sourceModel == filterValue
            MemoryFilterKind.Window -> seed.sourceWindow == filterValue
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
        item { MemoryPendingCard(0) { onSnackbar("本地待确认袋目前还是空的") } }
        item {
            Text(
                "种子库",
                modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                        Text(seed.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(seed.status.name.lowercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        if (seed.tags.isNotEmpty()) Text(seed.tags.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        if (expandedId == seed.id) {
                            Spacer(Modifier.height(8.dp))
                            Text(seed.content, style = MaterialTheme.typography.bodyMedium)
                            Text("索引：${seed.sourceModel} / ${seed.sourceWindow} / ${seed.sourceDate}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
    var tags by remember(seed?.id) { mutableStateOf(seed?.tags?.joinToString(", ") ?: "") }
    var sourceModel by remember(seed?.id) { mutableStateOf(seed?.sourceModel ?: "手动整理") }
    var sourceWindow by remember(seed?.id) { mutableStateOf(seed?.sourceWindow ?: "本地") }
    var sourceDate by remember(seed?.id) { mutableStateOf(seed?.sourceDate ?: LocalDate.now().toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (seed == null) "新增种子" else "编辑种子") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                WolfTextField("标题", title, { title = it })
                WolfTextField("内容", content, { content = it }, minLines = 3)
                WolfTextField("标签", tags, { tags = it })
                WolfTextField("模型", sourceModel, { sourceModel = it })
                WolfTextField("窗口", sourceWindow, { sourceWindow = it })
                WolfTextField("日期", sourceDate, { sourceDate = it })
                SeedStatus.entries.forEach { ChoiceRow(it.name.lowercase(), status == it) { status = it } }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    LocalSeed(
                        id = seed?.id.orEmpty(),
                        title = title,
                        content = content,
                        status = status,
                        tags = tags.split(',').map(String::trim).filter(String::isNotBlank),
                        sourceModel = sourceModel,
                        sourceWindow = sourceWindow,
                        sourceDate = sourceDate
                    )
                )
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
