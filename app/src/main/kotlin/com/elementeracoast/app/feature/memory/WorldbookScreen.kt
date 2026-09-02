package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
internal fun WorldbookScreen(
    store: MemoryStore,
    createRequested: Boolean,
    onCreateConsumed: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var testText by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<List<LocalWorldbookEntry>>(emptyList()) }
    var editing by remember { mutableStateOf<LocalWorldbookEntry?>(null) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(createRequested) {
        if (createRequested) {
            creating = true
            onCreateConsumed()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 2.dp),
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
        item {
            Text(
                "世界书",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.worldbook.isEmpty()) {
            item {
                DailySurfaceCard {
                    Text("这里还没有词条。以后聊到某个海岸名词时，再整理进来。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.worldbook, key = { it.id }) { entry ->
                DailySurfaceCard {
                    Text(entry.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
        item { Spacer(Modifier.height(24.dp)) }
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
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                WolfTextField("词条", term, { term = it })
                WolfTextField("内容", content, { content = it }, minLines = 3)
                ChoiceRow(if (enabled) "已启用" else "已停用", enabled) { enabled = !enabled }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(LocalWorldbookEntry(entry?.id.orEmpty(), term, content, enabled)) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
