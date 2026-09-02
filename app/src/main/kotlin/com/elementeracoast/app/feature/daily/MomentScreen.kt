package com.elementeracoast.app.feature.daily

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun MomentScreen(
    store: DailyStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit,
    onCompose: () -> Unit
) {
    val state by store.state.collectAsState()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<LocalMoment?>(null) }
    var commenting by remember { mutableStateOf<LocalMoment?>(null) }

    fun keepUri(uri: Uri?, save: (String) -> Unit, message: String) {
        if (uri == null) return
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        save(uri.toString())
        onSnackbar(message)
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        keepUri(uri, store::setProfileAvatar, "小寒头像已保存在本机")
    }
    val myriPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        keepUri(uri, store::setMyriAvatar, "Myri 头像已保存在本机")
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        keepUri(uri, store::setCover, "碳硅圈封面已保存在本机")
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DailyCover(state.coverUri) { coverPicker.launch(arrayOf("image/*")) }
            Spacer(Modifier.height(14.dp))
        }
        item {
            DailyAvatarRow("小寒头像", "保存在海岸", state.profileAvatarUri, "寒") {
                avatarPicker.launch(arrayOf("image/*"))
            }
        }
        item {
            DailyAvatarRow("Myri 头像", "保存在海岸", state.myriAvatarUri, "M") {
                myriPicker.launch(arrayOf("image/*"))
            }
        }
        if (state.moments.isEmpty()) {
            item {
                Spacer(Modifier.height(18.dp))
                DailySurfaceCard {
                    Text("还没有动态。", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("想写的时候留一点潮声。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Text("＋ 写一条", modifier = Modifier.clickable(onClick = onCompose), color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            items(state.moments, key = { it.id }) { moment ->
                val isMyri = moment.author == MomentAuthor.Myri
                MomentCard(
                    moment = moment,
                    avatarUri = if (isMyri) state.myriAvatarUri else state.profileAvatarUri,
                    avatarFallback = if (isMyri) "M" else "寒",
                    authorLabel = moment.author.label,
                    onLike = { store.toggleMomentLike(moment.id) },
                    onComment = { commenting = moment },
                    onEdit = { editing = moment },
                    onDelete = {
                        store.deleteMoment(moment.id)
                        onActionLogged("daily.moment.delete", "删除了一条碳硅圈", "删除 1 条本地动态")
                    }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    editing?.let { moment ->
        TextEditDialog("编辑动态", moment.text, onDismiss = { editing = null }) { text ->
            store.editMoment(moment.id, text)
            editing = null
        }
    }
    commenting?.let { moment ->
        TextEditDialog("评论", "", onDismiss = { commenting = null }) { text ->
            store.addComment(moment.id, text)
            commenting = null
        }
    }
}

@Composable
internal fun MomentComposeScreen(
    store: DailyStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit,
    onDone: () -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().toString().replace('-', '/')) }
    var body by remember { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp)) {
        item {
            DailySurfaceCard {
                DailyField("日期", date, { date = it }, "YYYY/MM/DD")
                Spacer(Modifier.height(18.dp))
                DailyField("正文", body, { body = it }, "今天想留什么？", minLines = 10, maxLines = 18)
                Spacer(Modifier.height(18.dp))
                DailyPrimaryButton("发布动态") {
                    val saved = store.publishMoment(body, date, MomentAuthor.Xiaohan)
                    if (saved == null) {
                        onSnackbar("正文还是空的")
                    } else {
                        onActionLogged("daily.moment.write", "写了一条碳硅圈", "新增 1 条小寒本地动态")
                        onSnackbar("动态已发布到本地碳硅圈")
                        onDone()
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentCard(
    moment: LocalMoment,
    avatarUri: String,
    avatarFallback: String,
    authorLabel: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    DailySurfaceCard {
        Row {
            DailyAvatar(avatarUri, avatarFallback)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(authorLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(moment.text, style = MaterialTheme.typography.bodyLarge)
                if (moment.comments.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    moment.comments.takeLast(5).forEach { comment ->
                        Text("小寒：$comment", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(momentFooter(moment), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    ActionText(if (moment.liked) "♡ 1" else "♡ 0", onLike)
                    ActionText("评论", onComment)
                    ActionText("编辑", onEdit)
                    ActionText("删除", onDelete)
                }
            }
        }
    }
}

private fun momentFooter(moment: LocalMoment): String {
    val clock = runCatching {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(moment.createdAt))
    }.getOrDefault("")
    val date = runCatching { LocalDate.parse(moment.date).format(DateTimeFormatter.ofPattern("MM月dd日")) }.getOrDefault(moment.date)
    return if (clock.isBlank()) date else "$date · $clock"
}

@Composable
internal fun TextEditDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            BasicTextField(
                value,
                { value = it },
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                minLines = 3,
                maxLines = 8
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun SmallButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun ActionText(label: String, onClick: () -> Unit) {
    Text(label, modifier = Modifier.clickable(onClick = onClick), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
}

@Composable
internal fun QuietDailyCard(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp)).padding(17.dp)) { content() }
}
