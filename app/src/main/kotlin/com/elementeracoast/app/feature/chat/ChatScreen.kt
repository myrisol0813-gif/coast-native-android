package com.elementeracoast.app.feature.chat

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
    var avatarUri by rememberSaveable { mutableStateOf<String?>(null) }
    var editingMessage by androidx.compose.runtime.remember { mutableStateOf<ChatMessage?>(null) }
    var soilOpen by rememberSaveable { mutableStateOf(false) }
    val wolfState by services.wolf.state.collectAsState()
    val memoryState by services.memory.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, avatarUri) {
        val uri = avatarUri
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri)).use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
            }.getOrNull()
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
            avatarDialogOpen = false
            onPlaceholder("已换成本机 Myri 头像；没有上传到海岸。")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ChatTimeline(
            conversationId = state.activeConversationId,
            messages = state.messages,
            isStreaming = state.isStreaming,
            avatarBitmap = avatarBitmap,
            onAvatarClick = { avatarDialogOpen = true },
            onCopy = { message ->
                clipboard.setText(AnnotatedString(message.text))
                onMessageAction(MessageAction.Copy(message.id))
                onPlaceholder("已复制")
            },
            onEdit = { message -> editingMessage = message },
            onAction = onMessageAction,
            onVariantPlaceholder = { onPlaceholder("当前本地消息只有 1/1；真实变体留到后端接线。") },
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
            onPickLocalImage = { avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onReset = { avatarUri = null; avatarDialogOpen = false; onPlaceholder("已恢复 Native 默认 Myri 头像。") },
            onFutureSync = { onPlaceholder("profile 同步仍未接线；本轮不会上传头像。") }
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
