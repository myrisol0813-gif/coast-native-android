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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoastShellViewModel(
    persistence: LocalPersistence = MemoryLocalPersistence(),
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
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
        if (roomType == RoomType.Main) {
            val target = chat.conversations().firstOrNull { it.roomType == RoomType.Main }
                ?: chat.create(RoomType.Main, RoomType.Main.drawerLabel)
            activateConversation(target)
            return
        }
        openRoomLanding(roomType)
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
                    showModelPicker = false, isStreaming = false, streamingMessageId = null,
                    streamingVariantIndex = null,
                    snackbarMessage = "已清空最后一个窗口"
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
        val current = ensureActiveConversation()
        val id = current.activeConversationId
        chat.replaceMessages(id, messages)
        _state.update { it.copy(conversations = chat.conversations(), messages = messages, snackbarMessage = "已恢复到当前本地窗口") }
    }

    fun logLocalAction(actionKey: String, label: String, summary: String) {
        logAction(actionKey, label, summary)
    }

    fun sendFakeMessage(text: String) {
        val clean = text.trim()
        var current = _state.value
        if (clean.isEmpty() || current.isStreaming || generationJob?.isActive == true) return
        current = ensureActiveConversation()
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
        _state.update {
            it.copy(
                conversations = chat.conversations(),
                messages = next,
                isStreaming = true,
                streamingMessageId = assistantId
            )
        }
        startFakeStreaming(conversationId, assistantId, current.activeRoomType, regenerated = false, targetVariantIndex = null)
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
                if (message.variantCount > 1) {
                    mutateCurrent { list -> list.map { item -> if (item.id == message.id) removeCurrentVariant(item) else item } }
                    logAction("message.variant.delete", "删除当前消息版本", "${message.role.name.lowercase()} message ${message.id}")
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
                editUserAndRegenerate(message, clean)
            }
            is MessageAction.SelectVariant -> {
                if (message.variantCount <= 1) return
                if (current.isStreaming && current.streamingMessageId == message.id) {
                    showPlaceholder("当前回复还在生成，停止后再切换版本。")
                    return
                }
                mutateCurrent { list -> list.map { item ->
                    if (item.id != message.id) item else selectVariant(item, action.index)
                } }
            }
            is MessageAction.Regenerate -> regenerate(message)
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _state.update { it.copy(isStreaming = false, streamingMessageId = null, streamingVariantIndex = null) }
    }
    fun showPlaceholder(message: String) { _state.update { it.copy(snackbarMessage = message) } }
    fun clearSnackbar() { _state.update { it.copy(snackbarMessage = null) } }

    private fun editUserAndRegenerate(message: ChatMessage, text: String) {
        val current = _state.value
        if (current.isStreaming || generationJob?.isActive == true) {
            showPlaceholder("请先停止当前生成，再编辑消息。")
            return
        }
        val conversationId = current.activeConversationId
        val existing = current.messages
        val userIndex = existing.indexOfFirst { it.id == message.id }
        if (userIndex < 0) return
        val assistantIndex = (userIndex + 1).takeIf { index -> existing.getOrNull(index)?.role == MessageRole.Assistant }
        val assistantId = assistantIndex?.let { existing[it].id } ?: chat.nextMessageId()
        val runs = furniture.runForPrompt(text, current.activeRoomType, conversationId, assistantId)
        val updated = existing.toMutableList()
        updated[userIndex] = appendUserVariant(message, text)
        var targetVariantIndex: Int? = null

        if (assistantIndex != null) {
            val assistant = existing[assistantIndex]
            val variants = assistant.normalizedVariants() + ""
            targetVariantIndex = variants.lastIndex
            updated[assistantIndex] = assistant.copy(
                text = "",
                modelId = current.currentModel,
                generationSource = "local-edit-regenerate · ${localGenerationLabel()}",
                liked = false,
                favorite = false,
                errorDetail = null,
                variantIndex = variants.lastIndex,
                variantCount = variants.size,
                variants = variants,
                furnitureRuns = runs
            )
        } else {
            updated.add(
                userIndex + 1,
                ChatMessage(
                    id = assistantId,
                    role = MessageRole.Assistant,
                    text = "",
                    modelId = current.currentModel,
                    generationSource = "local-edit-regenerate · ${localGenerationLabel()}",
                    furnitureRuns = runs
                )
            )
        }

        chat.replaceMessages(conversationId, updated)
        _state.update {
            it.copy(
                messages = updated,
                isStreaming = true,
                streamingMessageId = assistantId,
                streamingVariantIndex = targetVariantIndex,
                snackbarMessage = "已保留旧版本，并为新消息生成新的本地回复"
            )
        }
        logAction("message.edit.regenerate", "编辑消息并生成新回复版本", "user ${message.id} → assistant $assistantId", assistantMessageId = assistantId)
        startFakeStreaming(conversationId, assistantId, current.activeRoomType, regenerated = true, targetVariantIndex = targetVariantIndex)
    }

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
        val targetVariantIndex = message.variantIndex.takeIf { message.variantCount > 1 }
        mutateCurrent { list -> list.map {
            if (it.id != message.id) it else clearCurrentAssistantVariant(it, current.currentModel)
        } }
        logAction("chat.regenerate", "重新生成本地回复", "assistant message ${message.id}", assistantMessageId = message.id)
        _state.update { it.copy(isStreaming = true, streamingMessageId = message.id, streamingVariantIndex = targetVariantIndex) }
        startFakeStreaming(current.activeConversationId, message.id, current.activeRoomType, regenerated = true, targetVariantIndex = targetVariantIndex)
    }

    private fun clearCurrentAssistantVariant(message: ChatMessage, model: String): ChatMessage {
        if (message.variantCount <= 1) {
            return message.copy(
                text = "", modelId = model, generationSource = "local-regenerate · ${localGenerationLabel()}",
                liked = false, favorite = false, errorDetail = null
            )
        }
        val variants = message.normalizedVariants().toMutableList()
        val index = message.variantIndex.coerceIn(0, variants.lastIndex)
        variants[index] = ""
        return message.copy(
            text = "",
            modelId = model,
            generationSource = "local-regenerate · ${localGenerationLabel()}",
            liked = false,
            favorite = false,
            errorDetail = null,
            variants = variants
        )
    }

    private fun startFakeStreaming(
        conversationId: String,
        assistantId: Long,
        roomType: RoomType,
        regenerated: Boolean,
        targetVariantIndex: Int?
    ) {
        generationJob = viewModelScope.launch(generationDispatcher) {
            try {
                fakeChunks(roomType, regenerated).forEach { chunk ->
                    delay(240)
                    appendDelta(conversationId, assistantId, chunk, targetVariantIndex)
                }
            } catch (cancelled: CancellationException) {
                appendDelta(conversationId, assistantId, "\n\n[本地演示已停止]", targetVariantIndex)
                throw cancelled
            } finally {
                if (_state.value.activeConversationId == conversationId && _state.value.streamingMessageId == assistantId) {
                    _state.update { it.copy(isStreaming = false, streamingMessageId = null, streamingVariantIndex = null) }
                }
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
        if (id.isBlank()) return
        val updated = chat.mutate(id, transform)
        _state.update { it.copy(messages = updated) }
    }

    private fun appendDelta(conversationId: String, messageId: Long, delta: String, targetVariantIndex: Int?) {
        val updated = chat.mutate(conversationId) { list ->
            list.map { message ->
                if (message.id != messageId) message
                else if (targetVariantIndex != null && message.variantCount > 1) {
                    val variants = message.normalizedVariants().toMutableList()
                    val target = targetVariantIndex.coerceIn(0, variants.lastIndex)
                    variants[target] = variants[target] + delta
                    val visibleIndex = message.variantIndex.coerceIn(0, variants.lastIndex)
                    message.copy(text = variants[visibleIndex], variants = variants)
                } else {
                    message.copy(text = message.text + delta)
                }
            }
        }
        if (_state.value.activeConversationId == conversationId) _state.update { it.copy(messages = updated) }
    }

    private fun openRoomLanding(roomType: RoomType) {
        require(roomType != RoomType.Main) { "Main uses a concrete conversation, not a room landing" }
        _state.update {
            it.copy(
                conversations = chat.conversations(),
                activeRoomType = roomType,
                activeFeature = null,
                actionLogFocusIds = emptySet(),
                activeConversationId = "",
                messages = emptyList(),
                showModelPicker = false,
                isStreaming = false,
                streamingMessageId = null,
                streamingVariantIndex = null
            )
        }
    }

    private fun ensureActiveConversation(): CoastShellState {
        val current = _state.value
        if (current.activeConversationId.isNotBlank()) return current
        val roomType = current.activeRoomType
        val count = chat.conversations().count { it.roomType == roomType } + 1
        val created = chat.create(roomType, "新聊天 $count")
        activateConversation(created)
        return _state.value
    }

    private fun activateConversation(conversation: ConversationSummary) {
        _state.update {
            it.copy(
                conversations = chat.conversations(), activeRoomType = conversation.roomType, activeFeature = null,
                actionLogFocusIds = emptySet(), activeConversationId = conversation.id,
                messages = chat.messages(conversation.id), showModelPicker = false,
                isStreaming = false, streamingMessageId = null, streamingVariantIndex = null
            )
        }
    }

    private fun logAction(actionKey: String, label: String, output: String, assistantMessageId: Long? = null) {
        val current = _state.value
        if (current.activeConversationId.isBlank()) return
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
