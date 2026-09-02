package com.elementeracoast.app.feature.daily

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun MomentScreen(
    store: DailyStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<LocalMoment?>(null) }
    var commenting by remember { mutableStateOf<LocalMoment?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { store.setProfileAvatar(uri.toString()); onSnackbar("小寒头像已保存为 Daily profile 本地来源") }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { store.setCover(uri.toString()); onSnackbar("碳硅圈封面已保存在本机") }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("碳硅圈", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("海岸内部朋友圈 · 本地版", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton(if (state.profileAvatarUri.isBlank()) "设置小寒头像" else "修改小寒头像") {
                    avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                SmallButton(if (state.coverUri.isBlank()) "设置封面" else "修改封面") {
                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
        }
        item {
            MomentComposer(draft, { draft = it }) {
                val moment = store.publishMoment(draft)
                if (moment != null) {
                    draft = ""
                    onActionLogged("daily.moment.write", "写了一条碳硅圈", "新增 1 条本地动态")
                    onSnackbar("动态已发布到本地碳硅圈")
                }
            }
        }
        if (state.moments.isEmpty()) {
            item { QuietDailyCard { Text("还没有动态。想写的时候留一点潮声。", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(state.moments, key = { it.id }) { moment ->
                QuietDailyCard {
                    Text(moment.text, style = MaterialTheme.typography.bodyLarge)
                    if (moment.comments.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        moment.comments.takeLast(4).forEach { Text("小寒：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ActionText(if (moment.liked) "♥ 已赞" else "♡ 点赞") { store.toggleMomentLike(moment.id) }
                        ActionText("评论") { commenting = moment }
                        ActionText("编辑") { editing = moment }
                        ActionText("删除") {
                            store.deleteMoment(moment.id)
                            onActionLogged("daily.moment.delete", "删除了一条碳硅圈", "删除 1 条本地动态")
                        }
                    }
                }
            }
        }
    }

    editing?.let { moment ->
        TextEditDialog("编辑动态", moment.text, onDismiss = { editing = null }) { text ->
            store.editMoment(moment.id, text); editing = null
        }
    }
    commenting?.let { moment ->
        TextEditDialog("评论", "", onDismiss = { commenting = null }) { text ->
            store.addComment(moment.id, text); commenting = null
        }
    }
}

@Composable
private fun MomentComposer(value: String, onValueChange: (String) -> Unit, onPost: () -> Unit) {
    QuietDailyCard {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(90.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (value.isBlank()) Text("今天想留一点什么？", color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SmallButton("＋ 动态", onPost) }
    }
}

@Composable
internal fun TextEditDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { BasicTextField(value, { value = it }, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(12.dp), textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), minLines = 3, maxLines = 8) },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun SmallButton(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun ActionText(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.clickable(onClick = onClick), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
}

@Composable
internal fun QuietDailyCard(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp)).padding(17.dp)) { content() }
}
