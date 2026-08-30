/*
 * Streaming state ownership keeps the small MIT-licensed MiniiChat-derived
 * single-Job/cancel pattern from the existing PoC. Coast-specific scopes,
 * titles, state and visuals are original to this client. See THIRD_PARTY_NOTICES.md.
 */
package com.elementeracoast.app.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.ChatScope
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.scopedConversationTitle
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
        _state.value.conversations.forEach { conversation ->
            threads[conversation.id] = greetingFor(conversation.scope)
        }
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

    fun openScope(scope: ChatScope) {
        stopGeneration()
        val current = _state.value
        val target = current.conversations.firstOrNull { it.scope == scope }
            ?: return showPlaceholder("${scope.drawerLabel}还没有可用窗口。")
        _state.update {
            it.copy(
                activeScope = scope,
                activeFeature = null,
                activeConversationId = target.id,
                messages = threads[target.id].orEmpty(),
                showModelPicker = false
            )
        }
    }

    fun selectConversation(id: String) {
        stopGeneration()
        val target = _state.value.conversations.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                activeScope = target.scope,
                activeFeature = null,
                activeConversationId = target.id,
                messages = threads[target.id].orEmpty(),
                showModelPicker = false
            )
        }
    }

    fun newConversation() {
        stopGeneration()
        val scope = _state.value.activeScope
        val count = _state.value.conversations.count { it.scope == scope } + 1
        val id = "${scope.name.lowercase()}-${nextConversationId++}"
        val title = scopedConversationTitle(scope, count)
        val conversation = com.elementeracoast.app.core.model.ConversationSummary(id, title, scope)
        threads[id] = greetingFor(scope)
        _state.update {
            it.copy(
                conversations = listOf(conversation) + it.conversations,
                activeFeature = null,
                activeConversationId = id,
                messages = threads[id].orEmpty()
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
            val scope = _state.value.conversations.firstOrNull { it.id == conversationId }?.scope ?: ChatScope.Main
            val chunks = when (scope) {
                ChatScope.Main -> listOf(
                    "主聊天仍沿用 Coast 的普通窗口标题。 ",
                    "这轮先验证统一 ChatWindow、时间线、狗话行与底部输入框。 ",
                    "真实 SSE 与历史保存暂时没有接回。"
                )
                ChatScope.Radio -> listOf(
                    "这里是同一副聊天身体里的电波 scope。 ",
                    "新建窗口会固定带上【电波】前缀， ",
                    "旧 radio API 暂不参与 Native v1。"
                )
                ChatScope.Lighthouse -> listOf(
                    "这里是同一副聊天身体里的灯塔 scope。 ",
                    "新建窗口会固定带上【灯塔】前缀， ",
                    "后续再决定是否映射旧 lighthouse API。"
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

    private fun appendAssistantDelta(conversationId: String, messageId: Long, delta: String) {
        val updated = threads[conversationId].orEmpty().map { message ->
            if (message.id == messageId) message.copy(text = message.text + delta) else message
        }
        threads[conversationId] = updated
        if (_state.value.activeConversationId == conversationId) {
            _state.update { it.copy(messages = updated) }
        }
    }

    private fun greetingFor(scope: ChatScope): List<ChatMessage> = listOf(
        ChatMessage(
            id = nextMessageId++,
            role = MessageRole.Assistant,
            text = when (scope) {
                ChatScope.Main -> "海岸主聊天已就位。这个 Native v1 仍是本地视觉壳，不会向 Coast 或任何模型端点发送内容。"
                ChatScope.Radio -> "电波房已经换上与主聊天完全相同的 ChatWindow。这里先用【电波】标题前缀区分 scope。"
                ChatScope.Lighthouse -> "灯塔房已经换上与主聊天完全相同的 ChatWindow。这里先用【灯塔】标题前缀区分 scope。"
            }
        )
    )
}
