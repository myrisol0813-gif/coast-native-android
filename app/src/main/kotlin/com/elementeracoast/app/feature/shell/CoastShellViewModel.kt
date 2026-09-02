package com.elementeracoast.app.feature.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.local.LocalPersistence
import com.elementeracoast.app.core.local.MemoryLocalPersistence
import com.elementeracoast.app.core.local.SharedPreferencesLocalPersistence
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.feature.chat.LocalChatStore
import com.elementeracoast.app.feature.chat.LocalFurnitureOrchestrator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoastShellViewModel(
    persistence: LocalPersistence = MemoryLocalPersistence()
) : ViewModel() {
    private val initial = CoastShellState()
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<CoastShellState> = _state.asStateFlow()

    val local = LocalFeatureServices(persistence)
    private val chat = LocalChatStore(initial.conversations, initial.activeConversationId, initial.messages)
    private val furniture = LocalFurnitureOrchestrator(local.daily, local.memory, local.actionLog)
    private var generationJob: Job? = null

    init { syncAppearance() }

    fun setPassword(value: String) { _state.update { it.copy(password = value) } }
    fun enterLocalShell() {
        if (_state.value.password.isBlank()) return
        _state.update { it.copy(authenticated = true, password = "") }
    }

    fun syncAppearance() {
        val appearance = local.wolf.state.value.appearance
        _state.update { it.copy(theme = appearance.theme, userBubbleHex = appearance.userBubbleHex, accentHex = appearance.accentHex) }
    }

    fun cycleTheme() {
        local.wolf.cycleTheme()
        syncAppearance()
    }

    fun openRoomType(roomType: RoomType) {
        stopGeneration()
        val target = chat.conversations().firstOrNull { it.roomType == roomType }
            ?: chat.create(roomType, roomType.drawerLabel)
        activateConversation(target)
    }

    fun selectConversation(id: String) {
        stopGeneration()
        chat.conversations().firstOrNull { it.id == id }?.let(::activateConversation)
    }

    fun newConversation() {
        stopGeneration()
        val roomType = _state.value.activeRoomType
        val count = chat.conversations().count { it.roomType == roomType } + 1
        activateConversation(chat.create(roomType, "新聊天 $count"))
    }

    fun renameConversation(id: String, rawTitle: String) {
        if (chat.rename(id, rawTitle) == null) return
        _state.update { it.copy(conversations = chat.conversations()) }
    }

    fun deleteConversation(id: String) {
        val current = _state.value
        val target = chat.conversations().firstOrNull { it.id == id } ?: return
        if (target.id == current.activeConversationId) stopGeneration()
        chat.delete(id)
        val remaining = chat.conversations()

        if (remaining.isEmpty()) {
            val fallback = chat.create(RoomType.Main, "新聊天 1", empty = true)
            _state.update {
                it.copy(
                    conversations = chat.conversations(), activeRoomType = RoomType.Main,
                    activeFeature = null, activeConversationId = fallback.id, messages = emptyList(),
                    showModelPicker = false, isStreaming = false, snackbarMessage = "已清空最后一个窗口"
                )
            }
            return
        }
        if (target.id != current.activeConversationId) {
            _state.update { it.copy(conversations = remaining) }
            return
        }
        val replacement = remaining.firstOrNull { it.roomType == target.roomType }
            ?: remaining.firstOrNull { it.roomType == RoomType.Main }
            ?: remaining.first()
        activateConversation(replacement)
    }

    fun openFeature(destination: FeatureDestination) {
        stopGeneration()
        _state.update {
            it.copy(
                activeFeature = destination,
                actionLogFocusIds = if (destination == FeatureDestination.ActionLog) emptySet() else it.actionLogFocusIds,
                showModelPicker = false
            )
        }
    }

    fun openActionLog(actionIds: Set<String>) {
        stopGeneration()
        _state.update { it.copy(activeFeature = FeatureDestination.ActionLog, actionLogFocusIds = actionIds, showModelPicker = false) }
    }

    fun backToChat() { _state.update { it.copy(activeFeature = null, actionLogFocusIds = emptySet()) } }
    fun openModelPicker() { if (_state.value.activeFeature == null) _state.update { it.copy(showModelPicker = true) } }
    fun dismissModelPicker() { _state.update { it.copy(showModelPicker = false) } }

    fun selectModel(model: String) {
        if (model !in _state.value.models) return
        _state.update { it.copy(currentModel = model, showModelPicker = false, snackbarMessage = "模型已在本地模型箱中切换") }
        logAction("model.switch", "切换模型", "当前：${model.substringAfterLast('/')}")
    }

    fun importMessages(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        val id = _state.value.activeConversationId
        chat.replaceMessages(id, messages)
        _state.update { it.copy(messages = messages, snackbarMessage = "已恢复到当前本地窗口") }
    }

    fun logLocalAction(actionKey: String, label: String, summary: String) {
        logAction(actionKey, label, summary)
    }

    fun sendFakeMessage(text: String) {
        val clean = text.trim()
        val current = _state.value
        if (clean.isEmpty() || current.isStreaming || generationJob?.isActive == true) return
        val conversationId = current.activeConversationId
        val userId = chat.nextMessageId()
        val assistantId = chat.nextMessageId()
        val furnitureRuns = furniture.runForPrompt(clean, current.activeRoomType, conversationId, assistantId)
        val next = chat.mutate(conversationId) { messages ->
            messages + ChatMessage(userId, MessageRole.User, clean) + ChatMessage(
                id = assistantId,
                role = MessageRole.Assistant,
                text = "",
                modelId = current.currentModel,
                generationSource = localGenerationLabel(),
                furnitureRuns = furnitureRuns
            )
        }
        _state.update { it.copy(messages = next, isStreaming = true) }
        startFakeStreaming(conversationId, assistantId, current.activeRoomType, regenerated = false)
    }

    fun handleMessageAction(action: MessageAction) {
        val current = _state.value
        val message = current.messages.firstOrNull { it.id == action.messageId } ?: return
        when (action) {
            is MessageAction.Copy -> logAction("message.copy", "复制消息", "${message.role.name.lowercase()} · ${message.text.length} 字")
            is MessageAction.ToggleLike -> {
                if (message.role != MessageRole.Assistant) return
                mutateCurrent { list -> list.map { if (it.id == message.id) it.copy(liked = !it.liked) else it } }
                logAction("message.like", "切换消息点赞", "assistant message ${message.id}")
            }
            is MessageAction.ToggleFavorite -> {
                if (message.role != MessageRole.Assistant) return
                mutateCurrent { list -> list.map { if (it.id == message.id) it.copy(favorite = !it.favorite) else it } }
                logAction("message.favorite", "切换消息收藏", "assistant message ${message.id}")
            }
            is MessageAction.Delete -> {
                if (current.isStreaming) { showPlaceholder("生成中请先停止，再删除消息。"); return }
                if (message.role == MessageRole.User && message.variantCount > 1) {
                    mutateCurrent { list -> list.map { item -> if (item.id == message.id) removeCurrentVariant(item) else item } }
                    logAction("message.variant.delete", "删除当前消息版本", "user message ${message.id}")
                    showPlaceholder("已删除当前版本；其他版本与前后消息保持不变")
                } else {
                    mutateCurrent { it.filterNot { item -> item.id == message.id } }
                    logAction("message.delete", "删除消息", "${message.role.name.lowercase()} message ${message.id}")
                    showPlaceholder("已从当前本地窗口删除这条消息")
                }
            }
            is MessageAction.Edit -> {
                if (message.role != MessageRole.User) return
                val clean = action.text.trim()
                if (clean.isBlank()) return
                mutateCurrent { list -> list.map { item ->
                    if (item.id != message.id) item else appendUserVariant(item, clean)
                } }
                showPlaceholder("已生成新的本地消息版本")
            }
            is MessageAction.SelectVariant -> {
                if (message.role != MessageRole.User || message.variantCount <= 1) return
                mutateCurrent { list -> list.map { item ->
                    if (item.id != message.id) item else selectVariant(item, action.index)
                } }
            }
            is MessageAction.Regenerate -> regenerate(message)
        }
    }

    fun stopGeneration() { generationJob?.cancel() }
    fun showPlaceholder(message: String) { _state.update { it.copy(snackbarMessage = message) } }
    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }

    private fun appendUserVariant(message: ChatMessage, text: String): ChatMessage {
        val variants = message.normalizedVariants() + text
        return message.copy(
            text = text,
            variants = variants,
            variantIndex = variants.lastIndex,
            variantCount = variants.size
        )
    }

    private fun selectVariant(message: ChatMessage, requestedIndex: Int): ChatMessage {
        val variants = message.normalizedVariants()
        val index = requestedIndex.coerceIn(0, variants.lastIndex)
        return message.copy(text = variants[index], variants = variants, variantIndex = index, variantCount = variants.size)
    }

    private fun removeCurrentVariant(message: ChatMessage): ChatMessage {
        val variants = message.normalizedVariants().toMutableList()
        if (variants.size <= 1) return message
        val index = message.variantIndex.coerceIn(0, variants.lastIndex)
        variants.removeAt(index)
        val nextIndex = index.coerceAtMost(variants.lastIndex)
        return message.copy(
            text = variants[nextIndex],
            variants = variants,
            variantIndex = nextIndex,
            variantCount = variants.size
        )
    }

    private fun regenerate(message: ChatMessage) {
        val current = _state.value
        if (message.role != MessageRole.Assistant) return
        if (current.isStreaming || generationJob?.isActive == true) { showPlaceholder("请先停止当前生成，再重新生成。"); return }
        mutateCurrent { list -> list.map {
            if (it.id == message.id) it.copy(
                text = "", modelId = current.currentModel, generationSource = "local-regenerate · ${localGenerationLabel()}",
                liked = false, favorite = false, errorDetail = null, variantIndex = 0, variantCount = 1, variants = emptyList()
            ) else it
        } }
        logAction("chat.regenerate", "重新生成本地回复", "assistant message ${message.id}", assistantMessageId = message.id)
        _state.update { it.copy(isStreaming = true) }
        startFakeStreaming(current.activeConversationId, message.id, current.activeRoomType, regenerated = true)
    }

    private fun startFakeStreaming(conversationId: String, assistantId: Long, roomType: RoomType, regenerated: Boolean) {
        generationJob = viewModelScope.launch {
            try {
                fakeChunks(roomType, regenerated).forEach { chunk -> delay(240); appendDelta(conversationId, assistantId, chunk) }
            } catch (cancelled: CancellationException) {
                appendDelta(conversationId, assistantId, "\n\n[本地演示已停止]")
                throw cancelled
            } finally {
                if (_state.value.activeConversationId == conversationId) _state.update { it.copy(isStreaming = false) }
                generationJob = null
            }
        }
    }

    private fun fakeChunks(roomType: RoomType, regenerated: Boolean): List<String> {
        val basic = local.wolf.state.value.basic
        val instruction = local.memory.state.value.customInstructions.trim().take(70)
        val opening = if (regenerated) "我把这一轮重新铺开。 " else ""
        val room = when (roomType) {
            RoomType.Main -> "主聊天"
            RoomType.Radio -> "无线电波"
            RoomType.Lighthouse -> "灯塔来信"
        }
        return listOf(
            "$opening$room 仍与其他 room 共用这一副 ChatWindow。 ",
            "这次 fake generation 读取的是本地设置：${basic.outputLength} / ${basic.creativity} / max ${basic.maxOutputTokens}。 ",
            if (instruction.isBlank()) "没有读取真实后端上下文。" else "本地自定义指令预览：$instruction……（仅本地展示）"
        )
    }

    private fun localGenerationLabel(): String {
        val basic = local.wolf.state.value.basic
        return "local-mock · ${basic.outputLength} · ${basic.creativity}"
    }

    private fun mutateCurrent(transform: (List<ChatMessage>) -> List<ChatMessage>) {
        val id = _state.value.activeConversationId
        val updated = chat.mutate(id, transform)
        _state.update { it.copy(messages = updated) }
    }

    private fun appendDelta(conversationId: String, messageId: Long, delta: String) {
        val updated = chat.mutate(conversationId) { list -> list.map { if (it.id == messageId) it.copy(text = it.text + delta) else it } }
        if (_state.value.activeConversationId == conversationId) _state.update { it.copy(messages = updated) }
    }

    private fun activateConversation(conversation: ConversationSummary) {
        _state.update {
            it.copy(
                conversations = chat.conversations(), activeRoomType = conversation.roomType, activeFeature = null,
                actionLogFocusIds = emptySet(), activeConversationId = conversation.id,
                messages = chat.messages(conversation.id), showModelPicker = false
            )
        }
    }

    private fun logAction(actionKey: String, label: String, output: String, assistantMessageId: Long? = null) {
        val current = _state.value
        local.actionLog.record(
            actionKey = actionKey,
            label = label,
            roomType = current.activeRoomType,
            conversationId = current.activeConversationId,
            outputSummary = output,
            assistantMessageId = assistantMessageId
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CoastShellViewModel(SharedPreferencesLocalPersistence(context.applicationContext)) as T
            }
        }
    }
}
