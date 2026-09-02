package com.elementeracoast.app.feature.daily

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
import com.elementeracoast.app.feature.wolf.WolfTextField
import java.time.LocalDate

@Composable
internal fun DiaryScreen(
    store: DailyStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit,
    onCompose: () -> Unit
) {
    val state by store.state.collectAsState()
    var editing by remember { mutableStateOf<LocalDiary?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.diaries.isEmpty()) {
            item {
                DailySurfaceCard(onClick = onCompose) {
                    Text("还没有日记。", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text("想写的时候再留一张纸。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            items(state.diaries, key = { it.id }) { entry ->
                DailySurfaceCard {
                    Text(entry.date.replace('-', '/'), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(7.dp))
                    Text(entry.text, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${entry.weather} · ${entry.mood}${if (entry.tags.isEmpty()) "" else " · ${entry.tags.joinToString(" / ")}"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text("编辑", modifier = Modifier.clickable { editing = entry }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("删除", modifier = Modifier.clickable {
                            store.deleteDiary(entry.id)
                            onActionLogged("daily.diary.delete", "删除了一篇日记", "删除 1 篇本地日记")
                        }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    editing?.let { entry ->
        DiaryEditor(entry, onDismiss = { editing = null }) { draft ->
            store.saveDiary(entry.id, draft.date, draft.weather, draft.mood, draft.tags, draft.text)
            onActionLogged("daily.diary.edit", "编辑了一篇日记", "更新 1 篇本地日记")
            onSnackbar("日记已更新")
            editing = null
        }
    }
}

@Composable
internal fun DiaryComposeScreen(
    store: DailyStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit,
    onDone: () -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().toString().replace('-', '/')) }
    var weather by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp)) {
        item {
            DailySurfaceCard {
                DailyField("日期", date, { date = it }, "YYYY/MM/DD")
                Spacer(Modifier.height(18.dp))
                DailyField("天气", weather, { weather = it }, "未标注")
                Spacer(Modifier.height(18.dp))
                DailyField("心情", mood, { mood = it }, "未标注")
                Spacer(Modifier.height(18.dp))
                DailyField("正文", body, { body = it }, "写今天。", minLines = 12, maxLines = 20)
                Spacer(Modifier.height(18.dp))
                DailyField("标签（逗号或换行分隔）", tags, { tags = it }, minLines = 3, maxLines = 6)
                Spacer(Modifier.height(18.dp))
                DailyPrimaryButton("写入日记") {
                    val saved = store.saveDiary(
                        date = date,
                        weather = weather,
                        mood = mood,
                        tags = splitTags(tags),
                        text = body
                    )
                    if (saved == null) {
                        onSnackbar("正文还是空的")
                    } else {
                        onActionLogged("daily.diary.write", "写了一篇日记", "新增 1 篇本地日记")
                        onSnackbar("日记已留在本机")
                        onDone()
                    }
                }
            }
        }
    }
}

private data class DiaryDraft(
    val date: String,
    val weather: String,
    val mood: String,
    val tags: List<String>,
    val text: String
)

@Composable
private fun DiaryEditor(entry: LocalDiary, onDismiss: () -> Unit, onSave: (DiaryDraft) -> Unit) {
    var date by remember(entry.id) { mutableStateOf(entry.date) }
    var weather by remember(entry.id) { mutableStateOf(entry.weather) }
    var mood by remember(entry.id) { mutableStateOf(entry.mood) }
    var tags by remember(entry.id) { mutableStateOf(entry.tags.joinToString(", ")) }
    var text by remember(entry.id) { mutableStateOf(entry.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑日记") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WolfTextField("日期", date, { date = it })
                WolfTextField("天气", weather, { weather = it })
                WolfTextField("心情", mood, { mood = it })
                WolfTextField("正文", text, { text = it }, minLines = 5)
                WolfTextField("标签", tags, { tags = it })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(DiaryDraft(date, weather, mood, splitTags(tags), text)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun splitTags(raw: String): List<String> = raw
    .split(',', '\n')
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()
