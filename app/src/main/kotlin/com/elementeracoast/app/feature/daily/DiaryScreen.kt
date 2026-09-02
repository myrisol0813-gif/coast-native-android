package com.elementeracoast.app.feature.daily

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.wolf.WolfTextField
import java.time.LocalDate

@Composable
internal fun DiaryScreen(
    store: DailyStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var editing by remember { mutableStateOf<LocalDiary?>(null) }
    var composing by remember { mutableStateOf(false) }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("日记", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("本机纸页 · 不接后端", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SmallButton("＋ 日记") { composing = true }
            }
        }
        if (state.diaries.isEmpty()) {
            item { QuietDailyCard { Text("还没有日记。想写的时候再留一张纸。", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(state.diaries, key = { it.id }) { entry ->
                QuietDailyCard {
                    Text(entry.date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(entry.text, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("${entry.weather} · ${entry.mood}${if (entry.tags.isEmpty()) "" else " · ${entry.tags.joinToString(" / ")}"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("编辑", modifier = Modifier.clickable { editing = entry }, color = MaterialTheme.colorScheme.primary)
                        Text("删除", modifier = Modifier.clickable {
                            store.deleteDiary(entry.id)
                            onActionLogged("daily.diary.delete", "删除了一篇日记", "删除 1 篇本地日记")
                        }, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (composing) {
        DiaryEditor(null, onDismiss = { composing = false }) { draft ->
            val saved = store.saveDiary(
                date = draft.date,
                weather = draft.weather,
                mood = draft.mood,
                tags = draft.tags,
                text = draft.text
            )
            if (saved != null) {
                onActionLogged("daily.diary.write", "写了一篇日记", "新增 1 篇本地日记")
                onSnackbar("日记已留在本机")
            }
            composing = false
        }
    }
    editing?.let { entry ->
        DiaryEditor(entry, onDismiss = { editing = null }) { draft ->
            store.saveDiary(entry.id, draft.date, draft.weather, draft.mood, draft.tags, draft.text)
            onActionLogged("daily.diary.edit", "编辑了一篇日记", "更新 1 篇本地日记")
            editing = null
        }
    }
}

private data class DiaryDraft(val date: String, val weather: String, val mood: String, val tags: List<String>, val text: String)

@Composable
private fun DiaryEditor(entry: LocalDiary?, onDismiss: () -> Unit, onSave: (DiaryDraft) -> Unit) {
    var date by remember(entry?.id) { mutableStateOf(entry?.date ?: LocalDate.now().toString()) }
    var weather by remember(entry?.id) { mutableStateOf(entry?.weather ?: "") }
    var mood by remember(entry?.id) { mutableStateOf(entry?.mood ?: "") }
    var tags by remember(entry?.id) { mutableStateOf(entry?.tags?.joinToString(", ") ?: "") }
    var text by remember(entry?.id) { mutableStateOf(entry?.text ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "新日记" else "编辑日记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WolfTextField("日期", date) { date = it }
                WolfTextField("天气", weather) { weather = it }
                WolfTextField("心情", mood) { mood = it }
                WolfTextField("标签 · 逗号分隔", tags) { tags = it }
                WolfTextField("正文", text, { text = it }, minLines = 4)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onSave(DiaryDraft(date, weather, mood, tags.split(',').map(String::trim).filter(String::isNotBlank), text)) }) { Text("保存") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
