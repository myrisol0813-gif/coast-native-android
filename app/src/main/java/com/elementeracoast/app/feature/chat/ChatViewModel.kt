package com.elementeracoast.app.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.network.AssistantBranchDto
import com.elementeracoast.app.core.network.CancellationIOException
import com.elementeracoast.app.core.network.ChatHistoryDto
import com.elementeracoast.app.core.network.ChatStreamEvent
import com.elementeracoast.app.core.network.ChatTurnDto
import com.elementeracoast.app.core.network.ChatVariantDto
import com.elementeracoast.app.core.network.CoastGatewayClient
import com.elementeracoast.app.core.network.SimpleChatMessageDto
import com.elementeracoast.app.core.network.UserBranchDto
import com.elementeracoast.app.core.network.UsageDto
import com.elementeracoast.app.model.ChatMessage
import com.elementeracoast.app.model.CoastModel
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val loading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val models: List<CoastModel> = emptyList(),
    val currentModelId: String = "",
    val currentModelName: String = "正在读取模型…",
    val generating: Boolean = false,
    val error: String? = null,
    val footprint: String = "脚印尚未留下",
)

class ChatViewModel(private val gateway: CoastGatewayClient) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private var conversationId = ""
    private var history = ChatHistoryDto()
    private var generationJob: Job? = null
    private var stopRequested = false

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val conversation = gateway.ensureConversation()
                conversationId = conversation.id
                history = gateway.loadHistory(conversationId)
                val models = gateway.listModels()
                var profile = gateway.getProfile()
                var modelId = profile.currentChatModel
                if (modelId.isBlank()) {
                    modelId = models.firstOrNull { it.available }?.id.orEmpty()
                    if (modelId.isNotBlank()) profile = gateway.setCurrentModel(modelId)
                }
                Triple(models, profile.currentChatModel.ifBlank { modelId }, flatten(history))
            }.onSuccess { (models, modelId, messages) ->
                _state.value = ChatUiState(
                    loading = false,
                    messages = messages,
                    models = models,
                    currentModelId = modelId,
                    currentModelName = modelName(models, modelId),
                    footprint = messages.lastOrNull { it.role == ChatMessage.Role.ASSISTANT }?.let(::footprintFor)
                        ?: "脚印尚未留下",
                )
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.message ?: "主聊天载入失败。") }
            }
        }
    }

    fun selectModel(model: CoastModel) {
        if (_state.value.generating || model.id == _state.value.currentModelId) return
        viewModelScope.launch {
            runCatching { gateway.setCurrentModel(model.id) }
                .onSuccess {
                    _state.update { state -> state.copy(currentModelId = model.id, currentModelName = model.name, error = null) }
                }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "模型切换失败。") } }
        }
    }

    fun send(text: String) {
        val content = text.trim()
        val current = _state.value
        if (content.isBlank() || current.generating || conversationId.isBlank() || current.currentModelId.isBlank()) return

        val turnId = UUID.randomUUID().toString()
        val userVariant = ChatVariantDto(id = UUID.randomUUID().toString(), content = content, createdAt = Instant.now().toString())
        history = history.copy(
            updatedAt = Instant.now().toString(),
            turns = history.turns + ChatTurnDto(
                id = turnId,
                user = UserBranchDto(variants = listOf(userVariant)),
                assistant = AssistantBranchDto(
                    activeByUserVariant = mapOf("0" to 0),
                    variantsByUserVariant = mapOf("0" to emptyList()),
                ),
            ),
        )
        _state.update {
            it.copy(
                messages = it.messages + ChatMessage(userVariant.id, ChatMessage.Role.USER, content),
                generating = true,
                error = null,
            )
        }
        stopRequested = false
        generationJob = viewModelScope.launch {
            var partial = ""
            var actualModel = current.currentModelId
            var usage: UsageDto? = null
            var finishReason = ""
            val request = gateway.requestTemplate(
                conversationId = conversationId,
                sourceTurnId = turnId,
                modelId = current.currentModelId,
                messages = contextMessages(history),
            )
            try {
                gateway.streamChatMessage(request).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Meta -> if (event.model.isNotBlank()) actualModel = event.model
                        is ChatStreamEvent.Delta -> {
                            partial += event.content
                            showStreamingAssistant(turnId, partial, actualModel, usage)
                        }
                        is ChatStreamEvent.Usage -> {
                            usage = event.usage
                            showStreamingAssistant(turnId, partial, actualModel, usage)
                        }
                        is ChatStreamEvent.Done -> finishReason = event.finishReason
                        is ChatStreamEvent.Error -> throw IllegalStateException("${event.type}: ${event.message}")
                        ChatStreamEvent.Tool, ChatStreamEvent.DeskSlip -> Unit
                    }
                }
                commitAssistant(turnId, partial.ifBlank { "模型没有返回文本。" }, actualModel, usage, finishReason.ifBlank { "stop" })
            } catch (error: Throwable) {
                val cancelled = stopRequested || error is CancellationIOException || error.message == "generation_cancelled"
                if (partial.isNotBlank() || cancelled) {
                    commitAssistant(turnId, partial.ifBlank { "已停止生成。" }, actualModel, usage, if (cancelled) "cancelled" else "error")
                }
                if (!cancelled) _state.update { it.copy(error = error.message ?: "流式生成失败。") }
            } finally {
                _state.update { it.copy(generating = false) }
                stopRequested = false
                generationJob = null
            }
        }
    }

    fun stopGeneration() {
        if (!_state.value.generating) return
        stopRequested = true
        gateway.stopGeneration()
    }

    private suspend fun commitAssistant(turnId: String, content: String, modelId: String, usage: UsageDto?, finishReason: String) {
        val variant = ChatVariantDto(
            id = UUID.randomUUID().toString(),
            content = content,
            createdAt = Instant.now().toString(),
            modelId = modelId,
            usage = usage,
            finishReason = finishReason,
            generationSource = "chat",
        )
        history = history.copy(
            updatedAt = Instant.now().toString(),
            turns = history.turns.map { turn ->
                if (turn.id != turnId) turn
                else turn.copy(
                    assistant = turn.assistant.copy(
                        activeByUserVariant = mapOf("0" to 0),
                        variantsByUserVariant = mapOf("0" to listOf(variant)),
                    ),
                )
            },
        )
        val messages = flatten(history)
        _state.update {
            it.copy(messages = messages, footprint = footprintFor(messages.last { m -> m.role == ChatMessage.Role.ASSISTANT }))
        }
        gateway.saveHistory(conversationId, history)
    }

    private fun showStreamingAssistant(turnId: String, content: String, modelId: String, usage: UsageDto?) {
        val transientId = "stream-$turnId"
        val message = ChatMessage(transientId, ChatMessage.Role.ASSISTANT, content.ifBlank { "正在连接当前模型……" }, modelId, usage?.totalTokens)
        _state.update { state ->
            val without = state.messages.filterNot { it.id == transientId }
            state.copy(messages = without + message, footprint = footprintFor(message))
        }
    }

    private fun flatten(value: ChatHistoryDto): List<ChatMessage> = buildList {
        value.turns.forEach { turn ->
            val userIndex = turn.user.active.coerceAtLeast(0)
            turn.user.variants.getOrNull(userIndex)?.let {
                add(ChatMessage(it.id, ChatMessage.Role.USER, it.content))
            }
            val assistants = turn.assistant.variantsByUserVariant[userIndex.toString()].orEmpty()
            val active = turn.assistant.activeByUserVariant[userIndex.toString()] ?: 0
            assistants.getOrNull(active)?.let {
                add(ChatMessage(it.id, ChatMessage.Role.ASSISTANT, it.content, it.modelId, it.usage?.totalTokens))
            }
        }
    }

    private fun contextMessages(value: ChatHistoryDto): List<SimpleChatMessageDto> = buildList {
        value.turns.forEach { turn ->
            turn.user.variants.getOrNull(turn.user.active)?.let { add(SimpleChatMessageDto("user", it.content, turn.id)) }
            val key = turn.user.active.toString()
            val assistants = turn.assistant.variantsByUserVariant[key].orEmpty()
            assistants.getOrNull(turn.assistant.activeByUserVariant[key] ?: 0)?.let {
                add(SimpleChatMessageDto("assistant", it.content, turn.id))
            }
        }
    }.takeLast(20)

    private fun modelName(models: List<CoastModel>, id: String): String =
        models.firstOrNull { it.id == id }?.name ?: id.substringAfterLast('/').ifBlank { "未选择模型" }

    private fun footprintFor(message: ChatMessage): String {
        val model = message.modelId?.substringAfterLast('/')?.removeSuffix(":free") ?: "未知模型"
        return if (message.totalTokens != null) "$model · ${message.totalTokens} tok" else model
    }
}
