package com.elementeracoast.app.feature.chat

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
    val avatarSource = state.myriAvatarDataUrl.ifBlank { dailyState.myriAvatarUri }

    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, avatarSource) {
        value = if (avatarSource.isBlank()) null else withContext(Dispatchers.IO) {
            decodeImageSource(context, avatarSource)
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
                val model = message.modelId ?: "未知模型"
                val source = message.generationSource ?: "unknown"
                onPlaceholder("生成足迹：$model · $source")
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
            onSaved = { onPlaceholder("狗话仍属于本轮未接的本地小抽屉，没有写入后端。") }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
        CoastComposer(
            value = input,
            onValueChange = { input = it },
            isStreaming = state.isStreaming,
            enabled = !state.historyLoading,
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
            onPickLocalImage = {
                avatarDialogOpen = false
                onPlaceholder("头像上传本轮未接线；当前继续显示海岸后端已有头像，没有只改本机。")
            },
            onReset = {
                avatarDialogOpen = false
                onPlaceholder("头像写回本轮未接线，没有修改海岸后端资料。")
            },
            onFutureSync = {
                avatarDialogOpen = false
                onPlaceholder("当前头像已经从海岸 profile 读取；上传写回留到后续接线。")
            }
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

private fun decodeImageSource(context: android.content.Context, source: String): ImageBitmap? = runCatching {
    val bitmap = if (source.startsWith("data:image/", ignoreCase = true)) {
        val encoded = source.substringAfter(',', "")
        if (encoded.isBlank()) null else {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } else {
        context.contentResolver.openInputStream(Uri.parse(source)).use { stream -> BitmapFactory.decodeStream(stream) }
    }
    bitmap?.asImageBitmap()
}.getOrNull()
