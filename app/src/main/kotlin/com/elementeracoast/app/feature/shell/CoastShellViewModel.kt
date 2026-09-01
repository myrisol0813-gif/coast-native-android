/*
 * Streaming state ownership keeps the small MIT-licensed MiniiChat-derived
 * single-Job/cancel pattern from the existing PoC. Coast-specific room types,
 * titles, state and visuals are original to this client. See THIRD_PARTY_NOTICES.md.
 */
package com.elementeracoast.app.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.roomConversationTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoastShellViewModel : ViewModel() {
    private val _state = MutableStateFlow(CoastShellState())
    val state: StateFlow<CoastShellState> = _state.asStateFlow()

    private val threads = mutableMapOf<String, List<ChatMessage>>()
    private var generationJob: Job? = null
    private var nextMessageId = 100L
    private var nextConversationId = 10L

    init {
        _state.value.conversations.forEach { threads[it.id] = greetingFor(it.roomType) }
        threads[_state.value.activeConversationId] = _state.value.messages
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun enterLocalShell() {
        if (_state.value.password.isBlank()) return
        _state.update { it.copy(authenticated = true, password = "") }
    }

    fun cycleTheme() {
        _state.update { it.copy(theme = it.theme.next()) }
    }

    fun openRoomType(roomType: RoomType) {
        stopGeneration()
        val target = _state.value.conversations.firstOrNull { it.roomType == roomType }
            ?: createLocalConversation(roomType, roomType.drawerLabel)
        activateConversation(target)
    }

    fun selectConversation(id: String) {
        stopGeneration()
        val target = _state.value.conversations.firstOrNull { it.id == id } ?: return
        activateConversation(target)
    }

    fun newConversation() {
        stopGeneration()
        val current = _state.value
        val roomType = current.activeRoomType
        val count = current.conversations.count { it.roomType == roomType } + 1
        val conversation = createLocalConversation(roomType, "新聊天 $count")
        activateConversation(conversation)
    }

    fun renameConversation(id: String, rawTitle: String) {
        val target = _state.value.conversations.firstOrNull { it.id == id } ?: return
        val title = roomConversationTitle(target.roomType, rawTitle)
        if (title.isBlank()) return
        _state.update { state ->
            state.copy(
                conversations = state.conversations.map {
                    if (it.id == id) it.copy(title = title) else it
                }
            )
        }
    }

    fun deleteConversation(id: String) {
        val current = _state.value
        val target = current.conversations.firstOrNull { it.id == id } ?: return
        if (target.id == current.activeConversationId) stopGeneration()

        val remaining = current.conversations.filterNot { it.id == id }.toMutableList()
        threads.remove(id)

        if (remaining.isEmpty()) {
            val fallback = newConversationRecord(RoomType.Main, "新聊天 1")
            threads[fallback.id] = emptyList()
            _state.update {
                it.copy(
                    conversations = listOf(fallback),
                    activeRoomType = RoomType.Main,
                    activeFeature = null,
                    activeConversationId = fallback.id,
                    messages = emptyList(),
                    showModelPicker = false,
                    isStreaming = false,
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

        _state.update {
            it.copy(
                conversations = remaining,
                activeRoomType = replacement.roomType,
                activeFeature = null,
                activeConversationId = replacement.id,
                messages = threads[replacement.id].orEmpty(),
                showModelPicker = false,
                isStreaming = false
            )
        }
    }

    fun openFeature(destination: FeatureDestination) {
        stopGeneration()
        _state.update { it.copy(activeFeature = destination, showModelPicker = false) }
    }

    fun backToChat() {
        _state.update { it.copy(activeFeature = null) }
    }

    fun openModelPicker() {
        if (_state.value.activeFeature != null) return
        _state.update { it.copy(showModelPicker = true) }
    }

    fun dismissModelPicker() {
        _state.update { it.copy(showModelPicker = false) }
    }

    fun selectModel(model: String) {
        if (model !in _state.value.models) return
        _state.update {
            it.copy(
                currentModel = model,
                showModelPicker = false,
                snackbarMessage = "模型已在本地壳中切换；尚未同步 profile。"
            )
        }
    }

    fun sendFakeMessage(text: String) {
        val clean = text.trim()
        val current = _state.value
        if (clean.isEmpty() || current.isStreaming || generationJob?.isActive == true) return

        val conversationId = current.activeConversationId
        val userId = nextMessageId++
        val assistantId = nextMessageId++
        val nextThread = current.messages +
            ChatMessage(userId, MessageRole.User, clean) +
            ChatMessage(
                id = assistantId,
                role = MessageRole.Assistant,
                text = "",
                modelId = current.currentModel,
                generationSource = "local-mock"
            )
        threads[conversationId] = nextThread
        _state.update { it.copy(messages = nextThread, isStreaming = true) }

        startFakeStreaming(
            conversationId = conversationId,
            assistantId = assistantId,
            roomType = current.activeRoomType,
            regenerated = false
        )
    }

    fun handleMessageAction(action: MessageAction) {
        val current = _state.value
        val conversationId = current.activeConversationId
        val message = current.messages.firstOrNull { it.id == action.messageId } ?: return

        when (action) {
            is MessageAction.ToggleLike -> {
                if (message.role != MessageRole.Assistant) return
                mutateThread(conversationId) { messages ->
                    messages.map { if (it.id == message.id) it.copy(liked = !it.liked) else it }
                }
            }

            is MessageAction.ToggleFavorite -> {
                if (message.role != MessageRole.Assistant) return
                mutateThread(conversationId) { messages ->
                    messages.map { if (it.id == message.id) it.copy(favorite = !it.favorite) else it }
                }
            }

            is MessageAction.Delete -> {
                if (current.isStreaming) {
                    showPlaceholder("生成中请先停止，再删除消息。")
                    return
                }
                mutateThread(conversationId) { messages -> messages.filterNot { it.id == message.id } }
                showPlaceholder("已从当前本地窗口删除这条消息")
            }

            is MessageAction.Edit -> {
                if (message.role != MessageRole.User) return
                val clean = action.text.trim()
                if (clean.isBlank()) return
                mutateThread(conversationId) { messages ->
                    messages.map { if (it.id == message.id) it.copy(text = clean) else it }
                }
                showPlaceholder("已在当前本地窗口修改")
            }

            is MessageAction.Regenerate -> regenerateFakeMessage(message)
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun showPlaceholder(message: String) {
        _state.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    private fun regenerateFakeMessage(message: ChatMessage) {
        val current = _state.value
        if (message.role != MessageRole.Assistant) return
        if (current.isStreaming || generationJob?.isActive == true) {
            showPlaceholder("请先停止当前生成，再重新生成。")
            return
        }

        val conversationId = current.activeConversationId
        mutateThread(conversationId) { messages ->
            messages.map {
                if (it.id == message.id) {
                    it.copy(
                        text = "",
                        modelId = current.currentModel,
                        generationSource = "local-regenerate",
                        liked = false,
                        favorite = false,
                        errorDetail = null,
                        variantIndex = 0,
                        variantCount = 1
                    )
                } else {
                    it
                }
            }
        }
        _state.update { it.copy(isStreaming = true) }
        startFakeStreaming(
            conversationId = conversationId,
            assistantId = message.id,
            roomType = current.activeRoomType,
            regenerated = true
        )
    }

    private fun startFakeStreaming(
        conversationId: String,
        assistantId: Long,
        roomType: RoomType,
        regenerated: Boolean
    ) {
        generationJob = viewModelScope.launch {
            val chunks = fakeChunks(roomType, regenerated)
            try {
                for (chunk in chunks) {
                    delay(260)
                    appendAssistantDelta(conversationId, assistantId, chunk)
                }
            } catch (cancelled: CancellationException) {
                appendAssistantDelta(conversationId, assistantId, "\n\n[本地演示已停止]")
                throw cancelled
            } finally {
                if (_state.value.activeConversationId == conversationId) {
                    _state.update { it.copy(isStreaming = false) }
                }
                generationJob = null
            }
        }
    }

    private fun fakeChunks(roomType: RoomType, regenerated: Boolean): List<String> {
        val opening = if (regenerated) "我把这一轮重新铺开。 " else ""
        return when (roomType) {
            RoomType.Main -> listOf(
                opening + "这里仍是海岸的本地聊天身体。 ",
                "复制、喜欢、收藏、编辑、删除与重新生成都只作用在这一扇本地窗口。 ",
                "真实历史与 SSE 仍然没有接线。"
            )

            RoomType.Radio -> listOf(
                opening + "电波房继续和主聊天共用同一副 ChatWindow。 ",
                "这一轮的重新生成只是本地潮声， ",
                "不会向任何服务器发送消息。"
            )

            RoomType.Lighthouse -> listOf(
                opening + "灯塔房也仍在同一副聊天身体里。 ",
                "生成足迹会标记 local mock， ",
                "不会伪装成真实 API 回复。"
            )
        }
    }

    private fun mutateThread(
        conversationId: String,
        transform: (List<ChatMessage>) -> List<ChatMessage>
    ) {
        val updated = transform(threads[conversationId].orEmpty())
        threads[conversationId] = updated
        if (_state.value.activeConversationId == conversationId) {
            _state.update { it.copy(messages = updated) }
        }
    }

    private fun createLocalConversation(roomType: RoomType, rawTitle: String): ConversationSummary {
        val conversation = newConversationRecord(roomType, rawTitle)
        threads[conversation.id] = greetingFor(roomType)
        _state.update { state ->
            state.copy(conversations = listOf(conversation) + state.conversations)
        }
        return conversation
    }

    private fun newConversationRecord(roomType: RoomType, rawTitle: String): ConversationSummary {
        val id = "${roomType.wireValue}-${nextConversationId++}"
        return ConversationSummary(
            id = id,
            title = roomConversationTitle(roomType, rawTitle),
            roomType = roomType
        )
    }

    private fun activateConversation(conversation: ConversationSummary) {
        _state.update {
            it.copy(
                activeRoomType = conversation.roomType,
                activeFeature = null,
                activeConversationId = conversation.id,
                messages = threads[conversation.id].orEmpty(),
                showModelPicker = false
            )
        }
    }

    private fun appendAssistantDelta(conversationId: String, messageId: Long, delta: String) {
        val updated = threads[conversationId].orEmpty().map {
            if (it.id == messageId) it.copy(text = it.text + delta) else it
        }
        threads[conversationId] = updated
        if (_state.value.activeConversationId == conversationId) {
            _state.update { it.copy(messages = updated) }
        }
    }

    private fun greetingFor(roomType: RoomType): List<ChatMessage> = listOf(
        ChatMessage(
            id = nextMessageId++,
            role = MessageRole.Assistant,
            text = when (roomType) {
                RoomType.Main -> "海岸主聊天已就位。这个 Native v1 仍是本地视觉壳，不会向 Coast 或任何模型端点发送内容。"
                RoomType.Radio -> "电波房与主聊天共用同一 ChatWindow。这里先用【电波】标题前缀区分 room_type。"
                RoomType.Lighthouse -> "灯塔房与主聊天共用同一 ChatWindow。这里先用【灯塔】标题前缀区分 room_type。"
            },
            modelId = "Native local",
            generationSource = "fixture"
        )
    )
}
