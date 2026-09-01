package com.elementeracoast.app.feature.shell

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalChatStore
import com.elementeracoast.app.core.local.LocalDailyStore
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.local.SharedPreferencesPersistence
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureSummary
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.SeedStatus
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoastShellViewModel(application: Application) : AndroidViewModel(application) {
    private val persistence = SharedPreferencesPersistence(application.applicationContext)

    val preferencesStore = LocalPreferencesStore(persistence)
    val dailyStore = LocalDailyStore(persistence)
    val memoryStore = LocalMemoryStore(persistence)
    val actionLogStore = LocalActionLogStore(persistence)
    val chatStore = LocalChatStore(persistence)

    private val _state = MutableStateFlow(CoastShellState())
    val state: StateFlow<CoastShellState> = _state.asStateFlow()

    private var generationJob: Job? = null
    private var nextMessageId = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            combine(chatStore.state, preferencesStore.state) { chat, prefs -> chat to prefs }
                .collect { (chat, prefs) ->
                    _state.update { shell ->
                        shell.copy(
                            theme = prefs.theme,
                            userBubble = prefs.profile.userBubble,
                            accent = prefs.profile.accent,
                            activeRoomType = chat.activeRoomType,
                            conversations = chat.conversations,
                            activeConversationId = chat.activeConversationId,
                            messages = chat.messages,
                            currentModel = prefs.currentModel,
                            models = prefs.models
                        )
                    }
                }
        }
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun enterLocalShell() {
        if (_state.value.password.isBlank()) return
        _state.update { it.copy(authenticated = true, password = "") }
    }

    fun cycleTheme() {
        preferencesStore.setTheme(preferencesStore.state.value.theme.next())
    }

    fun openRoomType(roomType: RoomType) {
        stopGeneration()
        chatStore.openRoomType(roomType)
        _state.update { it.copy(activeFeature = null, showModelPicker = false) }
    }

    fun selectConversation(id: String) {
        stopGeneration()
        if (chatStore.selectConversation(id)) {
            _state.update { it.copy(activeFeature = null, showModelPicker = false) }
        }
    }

    fun newConversation() {
        stopGeneration()
        chatStore.newConversation()
        _state.update { it.copy(activeFeature = null, showModelPicker = false) }
    }

    fun renameConversation(id: String, title: String) {
        chatStore.renameConversation(id, title)
    }

    fun deleteConversation(id: String) {
        stopGeneration()
        if (chatStore.deleteConversation(id)) showPlaceholder("本地窗口已删除")
        _state.update { it.copy(activeFeature = null, showModelPicker = false) }
    }

    fun openFeature(destination: FeatureDestination) {
        stopGeneration()
        _state.update { it.copy(activeFeature = destination, showModelPicker = false) }
    }

    fun openActionLog(actionIds: List<String>, conversationId: String) {
        actionLogStore.setFilters(
            conversation = conversationId,
            focusedIds = actionIds.filter(String::isNotBlank).toSet()
        )
        openFeature(FeatureDestination.ActionLog)
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
        if (model !in preferencesStore.state.value.models) return
        preferencesStore.setModel(model)
        actionLogStore.record(
            actionKey = "model.switch",
            label = "切换了模型",
            roomType = _state.value.activeRoomType,
            conversationId = _state.value.activeConversationId,
            inputSummary = "model selection",
            outputSummary = "current model updated locally"
        )
        _state.update { it.copy(showModelPicker = false, snackbarMessage = "模型已在本地壳中切换；真实模型列表后端接线后同步。") }
    }

    fun sendFakeMessage(text: String) {
        val clean = text.trim()
        val current = _state.value
        if (clean.isBlank() || current.isStreaming || generationJob?.isActive == true) return

        val userId = nextMessageId++
        val assistantId = nextMessageId++
        val furniture = performFakeLocalActions(clean)
        val run = preferencesStore.state.value.runControl
        chatStore.appendMessages(
            listOf(
                ChatMessage(userId, MessageRole.User, clean),
                ChatMessage(
                    id = assistantId,
                    role = MessageRole.Assistant,
                    text = "",
                    modelId = current.currentModel,
                    generationSource = "local-mock:${run.outputLength}:${run.expression}",
                    furnitureRuns = furniture
                )
            )
        )
        _state.update { it.copy(isStreaming = true) }
        startFakeStreaming(assistantId, regenerated = false)
    }

    fun handleMessageAction(action: MessageAction) {
        val current = _state.value
        val message = chatStore.currentMessage(action.messageId) ?: return

        when (action) {
            is MessageAction.Copy -> {
                logMessageAction("chat.copy", "复制了消息", message)
            }

            is MessageAction.ToggleLike -> {
                if (message.role != MessageRole.Assistant) return
                chatStore.updateMessage(message.id) { it.copy(liked = !it.liked) }
                logMessageAction("chat.like", "调整了点赞", message)
            }

            is MessageAction.ToggleFavorite -> {
                if (message.role != MessageRole.Assistant) return
                chatStore.updateMessage(message.id) { it.copy(favorite = !it.favorite) }
                logMessageAction("chat.favorite", "调整了收藏", message)
            }

            is MessageAction.Delete -> {
                if (current.isStreaming) {
                    showPlaceholder("生成中请先停止，再删除消息。")
                    return
                }
                if (chatStore.deleteMessage(message.id)) {
                    logMessageAction("chat.delete", "删除了消息", message)
                    showPlaceholder("已从当前本地窗口删除这条消息")
                }
            }

            is MessageAction.Edit -> {
                if (message.role != MessageRole.User) return
                val clean = action.text.trim()
                if (clean.isBlank()) return
                if (chatStore.updateMessage(message.id) { it.copy(text = clean) }) {
                    logMessageAction("chat.edit", "编辑了用户消息", message)
                    showPlaceholder("已在当前本地窗口修改")
                }
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
        if (message.role != MessageRole.Assistant) return
        if (_state.value.isStreaming || generationJob?.isActive == true) {
            showPlaceholder("请先停止当前生成，再重新生成。")
            return
        }
        val furniture = actionLogStore.record(
            actionKey = "chat.regenerate",
            label = "重新生成了一次本地回复",
            roomType = _state.value.activeRoomType,
            conversationId = _state.value.activeConversationId,
            inputSummary = "assistant message id only",
            outputSummary = "local regenerate started"
        )
        val run = preferencesStore.state.value.runControl
        chatStore.updateMessage(message.id) {
            it.copy(
                text = "",
                modelId = _state.value.currentModel,
                generationSource = "local-regenerate:${run.outputLength}:${run.expression}",
                liked = false,
                favorite = false,
                errorDetail = null,
                variantIndex = 0,
                variantCount = 1,
                furnitureRuns = listOf(furniture)
            )
        }
        _state.update { it.copy(isStreaming = true) }
        startFakeStreaming(message.id, regenerated = true)
    }

    private fun startFakeStreaming(assistantId: Long, regenerated: Boolean) {
        val conversationId = _state.value.activeConversationId
        val roomType = _state.value.activeRoomType
        generationJob = viewModelScope.launch {
            try {
                fakeChunks(roomType, regenerated).forEach { chunk ->
                    delay(220)
                    if (_state.value.activeConversationId == conversationId) {
                        chatStore.updateMessage(assistantId) { it.copy(text = it.text + chunk) }
                    }
                }
            } catch (cancelled: CancellationException) {
                chatStore.updateMessage(assistantId) { it.copy(text = it.text + "\n\n[本地演示已停止]") }
                throw cancelled
            } finally {
                _state.update { it.copy(isStreaming = false) }
                generationJob = null
            }
        }
    }

    private fun fakeChunks(roomType: RoomType, regenerated: Boolean): List<String> {
        val run = preferencesStore.state.value.runControl
        val opening = if (regenerated) "我把这一轮重新铺开。 " else ""
        val settingLine = "本地参数：最近 ${run.recentTurns} 轮 · 舒服区间 ${run.comfortTokens} · ${run.outputLength}/${run.expression}。 "
        return when (roomType) {
            RoomType.Main -> listOf(
                opening + "这里是 app-59 对齐后的 Native 本地海岸。 ",
                settingLine,
                "这一轮只读写 APK 本机状态，真实 API、SSE、MCP 仍未接线。"
            )
            RoomType.Radio -> listOf(
                opening + "电波房继续和主聊天共用同一副 ChatWindow。 ",
                settingLine,
                "消息不会离开本机。"
            )
            RoomType.Lighthouse -> listOf(
                opening + "灯塔房也沿用同一副聊天身体。 ",
                settingLine,
                "生成足迹会明确标记 local mock。"
            )
        }
    }

    private fun performFakeLocalActions(text: String): List<FurnitureSummary> {
        val runs = mutableListOf<FurnitureSummary>()
        val room = _state.value.activeRoomType
        val conversation = _state.value.activeConversationId

        if (containsAny(text, "写碳硅圈", "发碳硅圈", "写一条碳硅圈")) {
            val payload = payloadAfterColon(text).ifBlank { "来自本地 fake model 的一条碳硅圈。" }
            dailyStore.createMoment(payload)?.let {
                runs += actionLogStore.record(
                    actionKey = "daily.create_moment",
                    label = "写了一条碳硅圈",
                    roomType = room,
                    conversationId = conversation,
                    inputSummary = "local fake tool · moment body omitted",
                    outputSummary = "moment saved locally"
                )
            }
        }

        if (containsAny(text, "写日记", "写一篇日记")) {
            val payload = payloadAfterColon(text).ifBlank { "来自本地 fake model 的一篇日记。" }
            dailyStore.createDiary(
                date = LocalDate.now().toString(),
                weather = "本地",
                mood = "平静",
                tags = listOf("fake-tool"),
                content = payload
            )?.let {
                runs += actionLogStore.record(
                    actionKey = "daily.create_diary",
                    label = "写了一篇日记",
                    roomType = room,
                    conversationId = conversation,
                    inputSummary = "local fake tool · diary body omitted",
                    outputSummary = "diary saved locally"
                )
            }
        }

        if (containsAny(text, "搜记忆", "搜索记忆")) {
            val query = payloadAfterColon(text)
            val hits = memoryStore.searchMemories(query).take(preferencesStore.state.value.runControl.memoryLimit)
            val items = hits.take(5).map { memory ->
                FurnitureItem(memory.tags.firstOrNull() ?: "记忆", memory.title)
            }
            runs += actionLogStore.record(
                actionKey = "memory.search",
                label = "搜索了记忆",
                roomType = room,
                conversationId = conversation,
                inputSummary = "local memory search · query length ${query.length}",
                outputSummary = "${hits.size} local hits",
                count = hits.size.coerceAtLeast(1),
                items = items
            )
        }

        if (containsAny(text, "写待确认", "放入待确认")) {
            val payload = payloadAfterColon(text).ifBlank { "本地待确认候选" }
            memoryStore.addSeed("待确认候选", payload, SeedStatus.Dormant)?.let {
                runs += actionLogStore.record(
                    actionKey = "memory.write_candidate",
                    label = "放入本地待确认候选",
                    roomType = room,
                    conversationId = conversation,
                    inputSummary = "candidate body omitted",
                    outputSummary = "stored as dormant local seed"
                )
            }
        }

        return runs
    }

    private fun logMessageAction(actionKey: String, label: String, message: ChatMessage) {
        actionLogStore.record(
            actionKey = actionKey,
            label = label,
            roomType = _state.value.activeRoomType,
            conversationId = _state.value.activeConversationId,
            inputSummary = "${message.role.name.lowercase()} message id only",
            outputSummary = "local action completed"
        )
    }

    private fun containsAny(text: String, vararg needles: String): Boolean = needles.any(text::contains)

    private fun payloadAfterColon(text: String): String = text
        .substringAfter('：', text.substringAfter(':', ""))
        .trim()
        .take(4000)
}
