package com.elementeracoast.app.feature.chat

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.feature.dogtalk.DogtalkCard
import com.elementeracoast.app.feature.dogtalk.DogtalkScope
import com.elementeracoast.app.feature.memory.SeedStatus
import com.elementeracoast.app.feature.shell.LocalFeatureServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChatWindow(
    state: CoastShellState,
    services: LocalFeatureServices,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onMessageAction: (MessageAction) -> Unit,
    onOpenActionLog: (Set<String>) -> Unit,
    onPlaceholder: (String) -> Unit
) {
    var input by rememberSaveable(state.activeConversationId) { mutableStateOf("") }
    var avatarDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editingMessage by androidx.compose.runtime.remember { mutableStateOf<ChatMessage?>(null) }
    var soilOpen by rememberSaveable { mutableStateOf(false) }
    val wolfState by services.wolf.state.collectAsState()
    val memoryState by services.memory.state.collectAsState()
    val dailyState by services.daily.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val avatarUri = dailyState.myriAvatarUri

    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, avatarUri) {
        value = if (avatarUri.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(avatarUri)).use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
            }.getOrNull()
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            services.daily.setMyriAvatar(uri.toString())
            avatarDialogOpen = false
            onPlaceholder("已换成本机 Myri 头像；和碳硅圈共用同一来源。")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ChatTimeline(
            conversationId = state.activeConversationId,
            messages = state.messages,
            isStreaming = state.isStreaming,
            streamingMessageId = state.streamingMessageId,
            avatarBitmap = avatarBitmap,
            onAvatarClick = { avatarDialogOpen = true },
            onCopy = { message ->
                clipboard.setText(AnnotatedString(message.text))
                onMessageAction(MessageAction.Copy(message.id))
                onPlaceholder("已复制")
            },
            onEdit = { message -> editingMessage = message },
            onAction = onMessageAction,
            onFootprint = { message ->
                val model = message.modelId ?: "Native local"
                val source = message.generationSource ?: "local"
                onPlaceholder("生成足迹：$model · $source；这是本地 UI 信息。")
            },
            onOpenActionLog = onOpenActionLog,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "思维壤 · ${memoryState.seeds.count { it.status == SeedStatus.Active }} 粒手持种",
            modifier = Modifier.fillMaxWidth().clickable { soilOpen = true }.padding(horizontal = 32.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        DogtalkCard(
            scope = DogtalkScope.from(state.activeRoomType),
            onSaved = { onPlaceholder("狗话已暂存在本地小抽屉；没有写后端。") }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
        CoastComposer(
            value = input,
            onValueChange = { input = it },
            isStreaming = state.isStreaming,
            enabled = true,
            onSend = { val outgoing = input; input = ""; onSend(outgoing) },
            onStop = onStop,
            onPlaceholder = onPlaceholder
        )
    }

    if (soilOpen) {
        SoilBottomSheet(
            recentMessages = state.messages.takeLast(wolfState.basic.recentTurns * 2),
            settings = wolfState.basic,
            memoryHits = memoryState.memories.take(wolfState.basic.memoryLimit),
            furnitureCount = state.messages.sumOf { it.furnitureRuns.size },
            onDismiss = { soilOpen = false }
        )
    }

    if (avatarDialogOpen) {
        AvatarPickerDialog(
            onDismiss = { avatarDialogOpen = false },
            onPickLocalImage = { avatarPicker.launch(arrayOf("image/*")) },
            onReset = {
                services.daily.setMyriAvatar("")
                avatarDialogOpen = false
                onPlaceholder("已恢复 Native 默认 Myri 头像。")
            },
            onFutureSync = { onPlaceholder("profile 后端同步仍未接线；当前只保存本机。") }
        )
    }

    editingMessage?.let { message ->
        EditMessageDialog(
            message = message,
            onDismiss = { editingMessage = null },
            onSave = { text -> onMessageAction(MessageAction.Edit(message.id, text)); editingMessage = null }
        )
    }
}
