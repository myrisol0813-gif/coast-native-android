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

        if (target.id != current.activeConversationId) {
            _state.update { it.copy(conversations = remaining) }
            return
        }

        val resolvedReplacement = remaining.firstOrNull { it.roomType == target.roomType }
            ?: remaining.firstOrNull { it.roomType == RoomType.Main }
            ?: newConversationRecord(RoomType.Main, "新聊天 1").also { fallback ->
                remaining.add(0, fallback)
                threads[fallback.id] = greetingFor(RoomType.Main)
            }

        _state.update {
            it.copy(
                conversations = remaining,
                activeRoomType = resolvedReplacement.roomType,
                activeFeature = null,
                activeConversationId = resolvedReplacement.id,
                messages = threads[resolvedReplacement.id].orEmpty(),
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
                snackbarMessage = "模型选择已在 Native v1 本地壳中切换；真实 profile PUT 将在后端接入阶段启用。"
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
            ChatMessage(assistantId, MessageRole.Assistant, "")
        threads[conversationId] = nextThread
        _state.update { it.copy(messages = nextThread, isStreaming = true) }

        generationJob = viewModelScope.launch {
            val roomType = _state.value.conversations
                .firstOrNull { it.id == conversationId }
                ?.roomType
                ?: RoomType.Main
            val chunks = when (roomType) {
                RoomType.Main -> listOf(
                    "主聊天继续沿用 Coast 的普通窗口标题。 ",
                    "这一轮只验证统一 ChatWindow、时间线、狗话行与底部输入框。 ",
                    "真实 SSE 与历史保存仍然没有接回。"
                )
                RoomType.Radio -> listOf(
                    "这里是同一副聊天身体里的电波 room。 ",
                    "新建窗口会固定带上【电波】前缀， ",
                    "这轮仍然只跑本地 mock。"
                )
                RoomType.Lighthouse -> listOf(
                    "这里是同一副聊天身体里的灯塔 room。 ",
                    "新建窗口会固定带上【灯塔】前缀， ",
                    "不会生成真实 API 回复。"
                )
            }

            try {
                for (chunk in chunks) {
                    delay(280)
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

    fun stopGeneration() {
        generationJob?.cancel()
    }

    fun showPlaceholder(message: String) {
        _state.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
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
            nextMessageId++,
            MessageRole.Assistant,
            when (roomType) {
                RoomType.Main -> "海岸主聊天已就位。这个 Native v1 仍是本地视觉壳，不会向 Coast 或任何模型端点发送内容。"
                RoomType.Radio -> "电波房与主聊天共用同一 ChatWindow。这里先用【电波】标题前缀区分 room_type。"
                RoomType.Lighthouse -> "灯塔房与主聊天共用同一 ChatWindow。这里先用【灯塔】标题前缀区分 room_type。"
            }
        )
    )
}
