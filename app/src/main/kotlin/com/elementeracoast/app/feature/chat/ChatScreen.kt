package com.elementeracoast.app.feature.chat

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.feature.dogtalk.DogtalkCard
import com.elementeracoast.app.feature.dogtalk.DogtalkScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ChatWindow(
    state: CoastShellState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onMessageAction: (MessageAction) -> Unit,
    onOpenActionLog: (Set<String>) -> Unit,
    onOpenPendingMemory: () -> Unit,
    onPlaceholder: (String) -> Unit
) {
    var input by rememberSaveable(state.activeConversationId) { mutableStateOf("") }
    var avatarDialogOpen by rememberSaveable { mutableStateOf(false) }
    var editingMessage by androidx.compose.runtime.remember { mutableStateOf<ChatMessage?>(null) }
    var soilOpen by rememberSaveable { mutableStateOf(false) }
    var deskOpen by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val avatarSource = state.myriAvatarDataUrl
    val persistedDeskReceipt = state.messages.lastOrNull()
        ?.takeIf { it.role == MessageRole.Assistant }
        ?.deskReceipt
    val deskReceipt = persistedDeskReceipt ?: state.turnDeskReceipt

    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, avatarSource) {
        value = if (avatarSource.isBlank()) null else withContext(Dispatchers.IO) { decodeImageSource(context, avatarSource) }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        ChatTimeline(
            conversationId = state.activeConversationId,
            messages = state.messages,
            thoughtSoil = state.thoughtSoil,
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
            onOpenThoughtSoil = { if (state.thoughtSoil != null) soilOpen = true },
            modifier = Modifier.weight(1f)
        )

        deskReceipt?.let { receipt -> TurnDeskStatusStrip(receipt = receipt, onClick = { deskOpen = true }) }
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

    val soil = state.thoughtSoil
    if (soilOpen && soil != null) {
        SoilBottomSheet(
            soil = soil,
            onOpenPendingBag = onOpenPendingMemory,
            onDismiss = { soilOpen = false }
        )
    }

    if (deskOpen && deskReceipt != null) TurnDeskBottomSheet(receipt = deskReceipt, onDismiss = { deskOpen = false })

    if (avatarDialogOpen) {
        AvatarPickerDialog(
            onDismiss = { avatarDialogOpen = false },
            onUploadLater = {
                avatarDialogOpen = false
                onPlaceholder("请从海岸日报的头像入口更新；同一张海岸头像会回到聊天窗口。")
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
