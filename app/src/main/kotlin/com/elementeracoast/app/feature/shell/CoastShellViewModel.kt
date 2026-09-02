package com.elementeracoast.app.feature.shell

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.auth.SessionRestoreResult
import com.elementeracoast.app.core.local.LocalPersistence
import com.elementeracoast.app.core.local.SharedPreferencesLocalPersistence
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.remote.RemoteDailyProfile
import com.elementeracoast.app.core.remote.RemoteHistory
import com.elementeracoast.app.core.remote.RemoteModelCatalogResponse
import com.elementeracoast.app.core.remote.RemoteProfile
import com.elementeracoast.app.feature.chat.ChatBranchNavigator
import com.elementeracoast.app.feature.chat.ChatProgress
import com.elementeracoast.app.feature.chat.ChatSyncMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CoastShellViewModel(
    private val persistence: LocalPersistence,
    private val backend: CoastBackendGraph,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    private val _state = MutableStateFlow(CoastShellState())
    val state: StateFlow<CoastShellState> = _state.asStateFlow()

    val local = LocalFeatureServices(persistence)
    private var generationJob: Job? = null
    private var historyJob: Job? = null

    init {
        syncAppearance()
        restoreSession()
    }

    fun setPassword(value: String) {
        _state.update { it.copy(password = value, authMessage = null) }
    }

    fun enterCoast() {
        val password = _state.value.password
        if (password.isBlank() || _state.value.authBusy) return
        viewModelScope.launch(workDispatcher) {
            _state.update { it.copy(authBusy = true, authMessage = null) }
            try {
                backend.auth.login(password)
                _state.update {
                    it.copy(
                        authenticated = true,
                        authBusy = false,
                        authMessage = null,
                        backendOffline = false,
                        password = ""
                    )
                }
                bootstrapAuthenticated()
            } catch (error: CoastApiException) {
                _state.update {
                    it.copy(
                        authenticated = false,
                        authBusy = false,
                        authMessage = error.message,
                        backendOffline = error.kind == CoastApiErrorKind.Network
                    )
                }
            }
        }
    }

    fun logout() {
        stopGeneration()
        historyJob?.cancel()
        viewModelScope.launch(workDispatcher) {
            backend.auth.logout()
            _state.update {
                CoastShellState(
                    authBusy = false,
                    theme = it.theme,
                    userBubbleHex = it.userBubbleHex,
                    accentHex = it.accentHex
                )
            }
        }
    }

    fun syncAppearance() {
        val appearance = local.wolf.state.value.appearance
        _state.update {
            it.copy(
                theme = appearance.theme,
                userBubbleHex = appearance.userBubbleHex,
                accentHex = appearance.accentHex
            )
        }
    }

    fun cycleTheme() {
        local.wolf.cycleTheme()
        syncAppearance()
    }

    fun openRoomType(roomType: RoomType) {
        stopGeneration()
        if (roomType != RoomType.Main) {
            openRoomLanding(roomType)
            return
        }
        val target = _state.value.conversations.firstOrNull { it.roomType == RoomType.Main }
        if (target == null) openRoomLanding(RoomType.Main) else selectConversation(target.id)
    }

    fun selectConversation(id: String) {
        stopGeneration()
        val conversation = _state.value.conversations.firstOrNull { it.id == id } ?: return
        activateCachedConversation(conversation)
        loadConversation(conversation)
    }

    fun newConversation() {
        stopGeneration()
        val roomType = _state.value.activeRoomType
        viewModelScope.launch(workDispatcher) {
            try {
                val created = backend.conversations.create(roomType, "新聊天")
                val empty = RemoteHistory(conversationId = created.id)
                backend.chat.cacheHistory(created.id, empty)
                _state.update {
                    it.copy(
                        conversations = listOf(created) + it.conversations.filterNot { row -> row.id == created.id },
                        backendOffline = false
                    )
                }
                activateCachedConversation(created)
            } catch (error: CoastApiException) {
                handleBackendError(error, "新建窗口失败")
            }
        }
    }

    fun renameConversation(id: String, rawTitle: String) {
        val clean = rawTitle.trim()
        if (clean.isBlank()) return
        viewModelScope.launch(workDispatcher) {
            try {
                val updated = backend.conversations.rename(id, clean)
                _state.update { state ->
                    state.copy(
                        conversations = state.conversations.map { if (it.id == id) updated else it },
                        backendOffline = false
                    )
                }
            } catch (error: CoastApiException) {
                handleBackendError(error, "窗口改名失败")
            }
        }
    }

    fun deleteConversation(id: String) {
        val target = _state.value.conversations.firstOrNull { it.id == id } ?: return
        if (target.id == _state.value.activeConversationId) stopGeneration()
        viewModelScope.launch(workDispatcher) {
            try {
                backend.conversations.delete(id)
                val remaining = _state.value.conversations.filterNot { it.id == id }
                _state.update { it.copy(conversations = remaining, backendOffline = false) }
                if (target.id != _state.value.activeConversationId) return@launch
                val replacement = remaining.firstOrNull { it.roomType == target.roomType }
                    ?: remaining.firstOrNull { it.roomType == RoomType.Main }
                    ?: remaining.firstOrNull()
                if (replacement == null) openRoomLanding(RoomType.Main) else {
                    activateCachedConversation(replacement)
                    loadConversation(replacement)
                }
            } catch (error: CoastApiException) {
                handleBackendError(error, "删除窗口失败")
            }
        }
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
        _state.update {
            it.copy(
                activeFeature = FeatureDestination.ActionLog,
                actionLogFocusIds = actionIds,
                showModelPicker = false
            )
        }
    }

    fun backToChat() {
        _state.update { it.copy(activeFeature = null, actionLogFocusIds = emptySet()) }
    }

    fun openModelPicker() {
        if (_state.value.activeFeature == null) _state.update { it.copy(showModelPicker = true) }
    }

    fun dismissModelPicker() {
        _state.update { it.copy(showModelPicker = false) }
    }

    fun selectModel(model: String) {
        if (model !in _state.value.models || model == _state.value.currentModel) {
            _state.update { it.copy(showModelPicker = false) }
            return
        }
        viewModelScope.launch(workDispatcher) {
            try {
                val profile = backend.profile.setCurrentChatModel(model)
                _state.update {
                    it.copy(
                        currentModel = profile.currentChatModel.ifBlank { model },
                        showModelPicker = false,
                        backendOffline = false,
                        snackbarMessage = "当前模型已同步到海岸后端"
                    )
                }
                logAction("model.switch", "切换模型", "当前：${model.substringAfterLast('/')}")
            } catch (error: CoastApiException) {
                handleBackendError(error, "模型切换失败")
            }
        }
    }

    fun refreshModels() {
        viewModelScope.launch(workDispatcher) {
            try {
                val catalog = backend.profile.refreshModels(force = true)
                applyModels(catalog)
                _state.update { it.copy(backendOffline = false, snackbarMessage = "模型目录已从海岸刷新") }
            } catch (error: CoastApiException) {
                handleBackendError(error, "模型目录刷新失败")
            }
        }
    }

    fun importMessages(@Suppress("UNUSED_PARAMETER") messages: List<ChatMessage>) {
        showPlaceholder("聊天记录导入尚未接后端，本轮没有写入或替换真实会话。")
    }

    fun logLocalAction(actionKey: String, label: String, summary: String) {
        logAction(actionKey, label, summary)
    }

    fun sendMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || generationJob?.isActive == true || _state.value.isStreaming) return
        if (_state.value.currentModel.isBlank() && _state.value.activeRoomType != RoomType.Lighthouse) {
            showPlaceholder("当前模型还没有从海岸载入，暂时不能发送。")
            return
        }
        generationJob = viewModelScope.launch(workDispatcher) {
            try {
                val conversation = ensureRemoteConversation()
                val conversationId = conversation.id
                val baseHistory = historyForSend(conversationId)
                val appended = ChatSyncMapper.appendUser(baseHistory, clean)
                backend.chat.cacheHistory(conversationId, appended.history)
                showHistory(conversationId, appended.history)

                val persistedUser = try {
                    backend.chat.persistHistory(conversationId, appended.history)
                } catch (error: CoastApiException) {
                    val failed = backend.chat.failedHistory(
                        appended.history,
                        appended.turnId,
                        _state.value.currentModel,
                        error
                    )
                    backend.chat.cacheHistory(conversationId, failed)
                    showHistory(conversationId, failed)
                    handleBackendError(error, "消息发送失败", keepAuthenticatedOnNetworkError = true)
                    return@launch
                }

                if (conversation.roomType == RoomType.Lighthouse) {
                    showHistory(conversationId, persistedUser)
                    _state.update { it.copy(snackbarMessage = "灯塔来信已写入海岸；这里按房间规则不触发模型回复") }
                    return@launch
                }
                generateTurn(conversationId, persistedUser, appended.turnId, _state.value.currentModel)
            } catch (error: CoastApiException) {
                handleBackendError(error, "消息发送失败", keepAuthenticatedOnNetworkError = true)
            } finally {
                generationJob = null
            }
        }
    }

    fun handleMessageAction(action: MessageAction) {
        val current = _state.value
        val message = current.messages.firstOrNull { it.id == action.messageId } ?: return
        when (action) {
            is MessageAction.Copy -> logAction(
                "message.copy",
                "复制消息",
                "${message.role.name.lowercase()} · ${message.text.length} 字"
            )
            is MessageAction.Retry -> retryMessage(message)
            is MessageAction.SelectVariant -> selectVariantForViewing(message, action.index)
            is MessageAction.ToggleLike,
            is MessageAction.ToggleFavorite,
            is MessageAction.Delete,
            is MessageAction.Edit,
            is MessageAction.Regenerate -> showPlaceholder("这项消息写回本轮尚未接线，没有修改后端数据。")
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _state.update {
            it.copy(
                isStreaming = false,
                streamingMessageId = null,
                streamingVariantIndex = null
            )
        }
    }

    fun showPlaceholder(message: String) {
        _state.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    private fun restoreSession() {
        viewModelScope.launch(workDispatcher) {
            _state.update { it.copy(authBusy = true, authMessage = null) }
            when (val restored = backend.auth.restore()) {
                SessionRestoreResult.Missing -> _state.update {
                    it.copy(authenticated = false, authBusy = false, backendOffline = false)
                }
                is SessionRestoreResult.Invalid -> _state.update {
                    it.copy(
                        authenticated = false,
                        authBusy = false,
                        authMessage = restored.message,
                        backendOffline = false
                    )
                }
                is SessionRestoreResult.Restored -> {
                    _state.update {
                        it.copy(
                            authenticated = true,
                            authBusy = false,
                            authMessage = null,
                            backendOffline = false
                        )
                    }
                    bootstrapAuthenticated()
                }
                is SessionRestoreResult.Offline -> {
                    _state.update {
                        it.copy(
                            authenticated = true,
                            authBusy = false,
                            authMessage = null,
                            backendOffline = true,
                            snackbarMessage = "暂时无法验证海岸连接，先使用本机缓存。"
                        )
                    }
                    applyCachedBootstrap()
                }
            }
        }
    }

    private suspend fun bootstrapAuthenticated() {
        applyCachedBootstrap()
        val remoteProfile = remoteOrNull("读取个人资料") { backend.profile.refreshProfile() }
        val remoteDaily = remoteOrNull("读取头像与封面") { backend.profile.refreshDailyProfile() }
        val remoteModels = remoteOrNull("读取模型目录") { backend.profile.refreshModels() }
        val remoteConversations = remoteOrNull("读取聊天窗口") { backend.conversations.refresh() }
        if (!_state.value.authenticated) return

        applyProfile(remoteProfile ?: backend.profile.cachedProfile(), remoteDaily ?: backend.profile.cachedDailyProfile())
        applyModels(remoteModels ?: backend.profile.cachedModels())
        val list = remoteConversations ?: backend.conversations.cached()
        _state.update { it.copy(conversations = list) }
        val remembered = persistence.get(KEY_CURRENT_CONVERSATION)
        val target = list.firstOrNull { it.id == _state.value.activeConversationId }
            ?: list.firstOrNull { it.id == remembered }
            ?: list.firstOrNull { it.roomType == RoomType.Main }
            ?: list.firstOrNull()
        if (target == null) {
            openRoomLanding(RoomType.Main)
        } else {
            activateCachedConversation(target)
            loadConversation(target)
        }
    }

    private fun applyCachedBootstrap() {
        val conversations = backend.conversations.cached()
        val profile = backend.profile.cachedProfile()
        val daily = backend.profile.cachedDailyProfile()
        val models = backend.profile.cachedModels()
        val remembered = persistence.get(KEY_CURRENT_CONVERSATION)
        val target = conversations.firstOrNull { it.id == remembered }
            ?: conversations.firstOrNull { it.roomType == RoomType.Main }
            ?: conversations.firstOrNull()
        val history = target?.let { backend.chat.cachedHistory(it.id) }
        _state.update {
            it.copy(
                conversations = conversations,
                activeRoomType = target?.roomType ?: RoomType.Main,
                activeConversationId = target?.id.orEmpty(),
                messages = history?.let(ChatSyncMapper::toUi).orEmpty()
            )
        }
        applyProfile(profile, daily)
        applyModels(models)
    }

    private fun applyProfile(profile: RemoteProfile?, daily: RemoteDailyProfile?) {
        if (profile == null && daily == null) return
        val current = profile?.currentChatModel.orEmpty()
        val myri = daily?.myriAvatarDataUrl.orEmpty().ifBlank { profile?.assistantAvatarDataUrl.orEmpty() }
        _state.update {
            it.copy(
                currentModel = current.ifBlank { it.currentModel },
                myriAvatarDataUrl = myri,
                xiaohanAvatarDataUrl = daily?.xiaohanAvatarDataUrl.orEmpty(),
                coverDataUrl = daily?.momentCoverDataUrl.orEmpty()
            )
        }
    }

    private fun applyModels(catalog: RemoteModelCatalogResponse?) {
        if (catalog == null) return
        val models = buildList {
            addAll(catalog.groups.openAiChat.map { it.id })
            addAll(catalog.groups.freeTest.map { it.id })
            addAll(catalog.groups.openAiImage.map { it.id })
        }.filter(String::isNotBlank).distinct()
        val current = _state.value.currentModel
        _state.update {
            it.copy(
                models = if (current.isNotBlank() && current !in models) listOf(current) + models else models,
                currentModel = current.ifBlank { models.firstOrNull().orEmpty() }
            )
        }
    }

    private fun loadConversation(conversation: ConversationSummary) {
        historyJob?.cancel()
        val cached = backend.chat.cachedHistory(conversation.id)
        if (cached != null) showHistory(conversation.id, cached)
        historyJob = viewModelScope.launch(workDispatcher) {
            _state.update { it.copy(historyLoading = true) }
            try {
                val history = backend.chat.loadHistory(conversation.id)
                showHistory(conversation.id, history)
                _state.update { it.copy(historyLoading = false, backendOffline = false) }
            } catch (error: CoastApiException) {
                _state.update { it.copy(historyLoading = false) }
                handleBackendError(error, "聊天记录载入失败", keepAuthenticatedOnNetworkError = true)
            } finally {
                historyJob = null
            }
        }
    }

    private suspend fun ensureRemoteConversation(): ConversationSummary {
        val current = _state.value
        current.conversations.firstOrNull { it.id == current.activeConversationId }?.let { return it }
        val created = backend.conversations.create(current.activeRoomType, "新聊天")
        val empty = RemoteHistory(conversationId = created.id)
        backend.chat.cacheHistory(created.id, empty)
        persistence.put(KEY_CURRENT_CONVERSATION, created.id)
        _state.update {
            it.copy(
                conversations = listOf(created) + it.conversations.filterNot { row -> row.id == created.id },
                activeConversationId = created.id,
                activeRoomType = created.roomType,
                messages = emptyList(),
                backendOffline = false
            )
        }
        return created
    }

    private suspend fun historyForSend(conversationId: String): RemoteHistory {
        backend.chat.cachedHistory(conversationId)?.let { return it }
        return backend.chat.loadHistory(conversationId)
    }

    private suspend fun generateTurn(
        conversationId: String,
        history: RemoteHistory,
        turnId: String,
        modelId: String
    ) {
        var partial = ""
        val cleared = backend.chat.clearFailure(history, turnId)
        backend.chat.cacheHistory(conversationId, cleared)
        val streamingId = ChatSyncMapper.streamingMessageId(turnId)
        showStreaming(conversationId, cleared, turnId, modelId, partial)
        _state.update {
            it.copy(
                isStreaming = true,
                streamingMessageId = streamingId,
                streamingVariantIndex = null
            )
        }
        try {
            backend.chat.streamReply(conversationId, cleared, turnId, modelId).collect { progress ->
                when (progress) {
                    is ChatProgress.Delta -> {
                        partial += progress.text
                        showStreaming(conversationId, cleared, turnId, modelId, partial)
                    }
                    is ChatProgress.Completed -> {
                        showHistory(conversationId, progress.history)
                        _state.update { it.copy(backendOffline = false) }
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            val stopped = backend.chat.cancelledHistory(cleared, turnId, modelId, partial)
            backend.chat.cacheHistory(conversationId, stopped)
            withContext(NonCancellable) {
                runCatching { backend.chat.persistHistory(conversationId, stopped) }
            }
            showHistory(conversationId, stopped)
            _state.update { it.copy(snackbarMessage = "已停止生成") }
            throw cancelled
        } catch (error: CoastApiException) {
            val failed = backend.chat.failedHistory(cleared, turnId, modelId, error, partial)
            backend.chat.cacheHistory(conversationId, failed)
            withContext(NonCancellable) {
                runCatching { backend.chat.persistHistory(conversationId, failed) }
            }
            showHistory(conversationId, failed)
            handleBackendError(error, "模型回复失败", keepAuthenticatedOnNetworkError = true)
        } finally {
            if (_state.value.streamingMessageId == streamingId) {
                _state.update {
                    it.copy(
                        isStreaming = false,
                        streamingMessageId = null,
                        streamingVariantIndex = null
                    )
                }
            }
        }
    }

    private fun retryMessage(message: ChatMessage) {
        val turnId = message.turnId ?: return
        val conversationId = _state.value.activeConversationId
        if (message.role != MessageRole.User || conversationId.isBlank() || generationJob?.isActive == true) return
        val history = backend.chat.cachedHistory(conversationId) ?: run {
            showPlaceholder("没有可重试的本机历史缓存，请先重新打开这个窗口。")
            return
        }
        val model = _state.value.currentModel
        if (model.isBlank()) {
            showPlaceholder("当前模型还没有从海岸载入，暂时不能重试。")
            return
        }
        generationJob = viewModelScope.launch(workDispatcher) {
            try {
                val cleared = backend.chat.clearFailure(history, turnId)
                val persisted = backend.chat.persistHistory(conversationId, cleared)
                showHistory(conversationId, persisted)
                generateTurn(conversationId, persisted, turnId, model)
            } catch (error: CoastApiException) {
                val failed = backend.chat.failedHistory(history, turnId, model, error)
                backend.chat.cacheHistory(conversationId, failed)
                showHistory(conversationId, failed)
                handleBackendError(error, "重试失败", keepAuthenticatedOnNetworkError = true)
            } finally {
                generationJob = null
            }
        }
    }

    private fun selectVariantForViewing(message: ChatMessage, index: Int) {
        if (_state.value.isStreaming) {
            showPlaceholder("当前回复仍在生成，完成后再查看其他版本。")
            return
        }
        val conversationId = _state.value.activeConversationId
        val turnId = message.turnId ?: return
        val history = backend.chat.cachedHistory(conversationId) ?: return
        val selected = ChatBranchNavigator.select(history, turnId, message.role, index)
        backend.chat.cacheHistory(conversationId, selected)
        showHistory(conversationId, selected)
        showPlaceholder("已在本机切换查看版本；本轮没有改写后端的 active 版本。")
    }

    private fun showStreaming(
        conversationId: String,
        history: RemoteHistory,
        turnId: String,
        modelId: String,
        partial: String
    ) {
        if (_state.value.activeConversationId != conversationId) return
        val messages = ChatSyncMapper.toUi(history) + ChatSyncMapper.streamingAssistant(turnId, modelId, partial)
        _state.update { it.copy(messages = messages) }
    }

    private fun showHistory(conversationId: String, history: RemoteHistory) {
        if (_state.value.activeConversationId != conversationId) return
        _state.update { it.copy(messages = ChatSyncMapper.toUi(history)) }
    }

    private fun activateCachedConversation(conversation: ConversationSummary) {
        persistence.put(KEY_CURRENT_CONVERSATION, conversation.id)
        val history = backend.chat.cachedHistory(conversation.id)
        _state.update {
            it.copy(
                activeRoomType = conversation.roomType,
                activeFeature = null,
                actionLogFocusIds = emptySet(),
                activeConversationId = conversation.id,
                messages = history?.let(ChatSyncMapper::toUi).orEmpty(),
                showModelPicker = false,
                isStreaming = false,
                streamingMessageId = null,
                streamingVariantIndex = null
            )
        }
    }

    private fun openRoomLanding(roomType: RoomType) {
        require(roomType in RoomType.entries)
        persistence.remove(KEY_CURRENT_CONVERSATION)
        _state.update {
            it.copy(
                activeRoomType = roomType,
                activeFeature = null,
                actionLogFocusIds = emptySet(),
                activeConversationId = "",
                messages = emptyList(),
                showModelPicker = false,
                historyLoading = false,
                isStreaming = false,
                streamingMessageId = null,
                streamingVariantIndex = null
            )
        }
    }

    private suspend fun <T> remoteOrNull(label: String, block: suspend () -> T): T? = try {
        block()
    } catch (error: CoastApiException) {
        handleBackendError(error, "$label失败", keepAuthenticatedOnNetworkError = true)
        null
    }

    private fun handleBackendError(
        error: CoastApiException,
        prefix: String,
        keepAuthenticatedOnNetworkError: Boolean = false
    ) {
        if (error.kind == CoastApiErrorKind.Unauthorized) {
            backend.auth.clearConfirmedInvalidSession()
            generationJob?.cancel()
            historyJob?.cancel()
            _state.update {
                it.copy(
                    authenticated = false,
                    authBusy = false,
                    authMessage = "登录状态已失效，请重新输入海岸密码。",
                    backendOffline = false,
                    isStreaming = false,
                    streamingMessageId = null,
                    streamingVariantIndex = null
                )
            }
            return
        }
        _state.update {
            it.copy(
                backendOffline = if (error.kind == CoastApiErrorKind.Network) true else it.backendOffline,
                snackbarMessage = "$prefix：${error.message}",
                authenticated = if (error.kind == CoastApiErrorKind.Network && keepAuthenticatedOnNetworkError) it.authenticated else it.authenticated
            )
        }
    }

    private fun logAction(
        actionKey: String,
        label: String,
        output: String,
        assistantMessageId: Long? = null
    ) {
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
        private const val KEY_CURRENT_CONVERSATION = "remote.current-conversation.v1"

        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val persistence = SharedPreferencesLocalPersistence(appContext)
                val backend = CoastBackendGraph.production(appContext, persistence)
                return CoastShellViewModel(persistence, backend) as T
            }
        }
    }
}
