package com.elementeracoast.app.feature.daily

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
import com.elementeracoast.app.core.local.LocalDailyStore
import com.elementeracoast.app.core.model.DiaryEntry
import com.elementeracoast.app.core.model.RoomType
import java.time.LocalDate

@Composable
internal fun DiaryScreen(
    store: LocalDailyStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var editor by remember { mutableStateOf<DiaryEntry?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun log(key: String, label: String) {
        actionLogStore.record(
            actionKey = key,
            label = label,
            roomType = roomType,
            conversationId = conversationId,
            inputSummary = "diary body omitted",
            outputSummary = "local diary updated"
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("日记", style = MaterialTheme.typography.headlineMedium)
            Text("日期 · 天气 · 心情 · 标签 · local-only", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Button(onClick = { creating = true }) { Text("写日记") } }
        if (state.diaries.isEmpty()) {
            item { Text("还没有日记。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        state.diaries.forEach { entry ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(entry.date, fontWeight = FontWeight.SemiBold)
                    Text("${entry.weather.ifBlank { "—" }} · ${entry.mood.ifBlank { "—" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (entry.tags.isNotEmpty()) Text(entry.tags.joinToString(" · ") { "#$it" }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    Text(entry.content)
                    Row {
                        TextButton(onClick = { editor = entry }) { Text("编辑") }
                        TextButton(onClick = {
                            if (store.deleteDiary(entry.id)) {
                                log("daily.delete_diary", "删除了一篇日记")
                                onSnackbar("本地日记已删除")
                            }
                        }) { Text("删除") }
                    }
                }
            }
        }
    }

    if (creating) {
        DiaryEditor(
            initial = null,
            onDismiss = { creating = false },
            onSave = { date, weather, mood, tags, content ->
                store.createDiary(date, weather, mood, tags, content)?.let {
                    log("daily.create_diary", "写了一篇日记")
                    onSnackbar("日记已写入本机")
                }
                creating = false
            }
        )
    }
    editor?.let { entry ->
        DiaryEditor(
            initial = entry,
            onDismiss = { editor = null },
            onSave = { date, weather, mood, tags, content ->
                if (store.editDiary(entry.copy(date = date, weather = weather, mood = mood, tags = tags, content = content))) {
                    log("daily.edit_diary", "编辑了一篇日记")
                }
                editor = null
            }
        )
    }
}

@Composable
private fun DiaryEditor(
    initial: DiaryEntry?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<String>, String) -> Unit
) {
    var date by remember(initial?.id) { mutableStateOf(initial?.date ?: LocalDate.now().toString()) }
    var weather by remember(initial?.id) { mutableStateOf(initial?.weather.orEmpty()) }
    var mood by remember(initial?.id) { mutableStateOf(initial?.mood.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags?.joinToString(",").orEmpty()) }
    var content by remember(initial?.id) { mutableStateOf(initial?.content.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "写日记" else "编辑日记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it.take(20) }, label = { Text("日期") }, singleLine = true)
                OutlinedTextField(weather, { weather = it.take(40) }, label = { Text("天气") }, singleLine = true)
                OutlinedTextField(mood, { mood = it.take(40) }, label = { Text("心情") }, singleLine = true)
                OutlinedTextField(tags, { tags = it.take(240) }, label = { Text("标签 · 逗号分隔") }, singleLine = true)
                OutlinedTextField(content, { content = it.take(8000) }, label = { Text("正文") }, minLines = 4)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(date, weather, mood, tags.split(',').map(String::trim).filter(String::isNotBlank), content)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
