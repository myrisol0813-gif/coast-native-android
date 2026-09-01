package com.elementeracoast.app.feature.daily

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
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
import com.elementeracoast.app.core.model.DailyMoment
import com.elementeracoast.app.core.model.RoomType

@Composable
internal fun MomentsScreen(
    store: LocalDailyStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<DailyMoment?>(null) }
    var commenting by remember { mutableStateOf<DailyMoment?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            store.setAvatarUri(uri.toString())
            onSnackbar("碳硅圈头像已保存在 Daily profile 本地源")
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            store.setCoverUri(uri.toString())
            onSnackbar("碳硅圈封面已保存在本机")
        }
    }

    fun log(key: String, label: String, output: String = "local moment updated") {
        actionLogStore.record(
            actionKey = key,
            label = label,
            roomType = roomType,
            conversationId = conversationId,
            inputSummary = "moment content omitted",
            outputSummary = output
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("碳硅圈", style = MaterialTheme.typography.headlineMedium)
            Text("海岸内部朋友圈 · local-only", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("修改小寒头像") }
                OutlinedButton(onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("修改封面") }
            }
        }
        item {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(4000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("写一条碳硅圈") },
                minLines = 3
            )
            Button(
                onClick = {
                    store.createMoment(draft)?.let {
                        draft = ""
                        log("daily.create_moment", "写了一条碳硅圈")
                        onSnackbar("碳硅圈已写入本机")
                    }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("发布") }
        }
        if (state.moments.isEmpty()) {
            item {
                Text("还没有碳硅圈。写下第一条本地动态吧。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.moments.forEach { moment ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(moment.content, style = MaterialTheme.typography.bodyLarge)
                    if (moment.comments.isNotEmpty()) {
                        Text(moment.comments.joinToString("\n") { "· $it" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            if (store.toggleMomentLike(moment.id)) log("daily.moment_like", "调整了碳硅圈点赞")
                        }) { Text(if (moment.liked) "已赞" else "点赞") }
                        TextButton(onClick = { commenting = moment }) { Text("评论") }
                        TextButton(onClick = { editing = moment }) { Text("编辑") }
                        TextButton(onClick = {
                            if (store.deleteMoment(moment.id)) {
                                log("daily.delete_moment", "删除了一条碳硅圈")
                                onSnackbar("本地动态已删除")
                            }
                        }) { Text("删除") }
                    }
                }
            }
        }
    }

    editing?.let { moment ->
        TextEditDialog(
            title = "编辑碳硅圈",
            initial = moment.content,
            onDismiss = { editing = null },
            onSave = { value ->
                if (store.editMoment(moment.id, value)) log("daily.edit_moment", "编辑了一条碳硅圈")
                editing = null
            }
        )
    }
    commenting?.let { moment ->
        TextEditDialog(
            title = "评论",
            initial = "",
            onDismiss = { commenting = null },
            onSave = { value ->
                if (store.addMomentComment(moment.id, value)) log("daily.moment_comment", "评论了一条碳硅圈")
                commenting = null
            }
        )
    }
}

@Composable
private fun TextEditDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it.take(4000) }, minLines = 3)
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
