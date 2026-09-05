from pathlib import Path


def replace(path, old, new, expected=1):
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != expected:
        raise SystemExit(f"{path}: expected {expected} anchors, found {count}: {old[:90]!r}")
    p.write_text(text.replace(old, new, expected))


vm = "app/src/main/kotlin/com/elementeracoast/app/feature/shell/CoastShellViewModel.kt"
replace(vm,
    "import com.elementeracoast.app.core.model.ConversationSummary\n",
    "import com.elementeracoast.app.core.model.ConversationSummary\nimport com.elementeracoast.app.core.model.CrossWindowRequest\n"
)
replace(vm,
    "import com.elementeracoast.app.feature.dogtalk.DogtalkRepository\n",
    "import com.elementeracoast.app.feature.dogtalk.CrossWindowRepository\nimport com.elementeracoast.app.feature.dogtalk.CrossWindowUiState\nimport com.elementeracoast.app.feature.dogtalk.DogtalkRepository\n"
)
replace(vm,
    "    private val _state = MutableStateFlow(CoastShellState())\n    val state: StateFlow<CoastShellState> = _state.asStateFlow()\n",
    "    private val _state = MutableStateFlow(CoastShellState())\n    val state: StateFlow<CoastShellState> = _state.asStateFlow()\n    private val _crossWindow = MutableStateFlow(CrossWindowUiState())\n    val crossWindow: StateFlow<CrossWindowUiState> = _crossWindow.asStateFlow()\n"
)
replace(vm,
    "    val dogtalk: DogtalkRepository get() = backend.dogtalk\n",
    "    val dogtalk: DogtalkRepository get() = backend.dogtalk\n    val crossWindowRepository: CrossWindowRepository get() = backend.crossWindow\n"
)
replace(vm,
    "    fun logout() {\n        stopGeneration()\n",
    "    fun logout() {\n        stopGeneration()\n        resetCrossWindow()\n"
)
replace(vm,
    "    fun logLocalAction(actionKey: String, label: String, summary: String) {\n        logAction(actionKey, label, summary)\n    }\n\n    fun sendMessage(text: String) {",
    "    fun logLocalAction(actionKey: String, label: String, summary: String) {\n        logAction(actionKey, label, summary)\n    }\n\n    fun updateCrossWindow(value: CrossWindowUiState) {\n        _crossWindow.value = value\n    }\n\n    fun sendMessage(text: String) {"
)
replace(vm,
    '''                if (conversation.roomType == RoomType.Lighthouse) {
                    showHistory(conversationId, persistedUser)
                    _state.update { it.copy(snackbarMessage = "灯塔来信已写入海岸；这里按房间规则不触发模型回复") }
                    return@launch
                }
                generateTurn(conversationId, persistedUser, appended.turnId, _state.value.currentModel)''',
    '''                if (conversation.roomType == RoomType.Lighthouse) {
                    resetCrossWindow()
                    showHistory(conversationId, persistedUser)
                    _state.update { it.copy(snackbarMessage = "灯塔来信已写入海岸；这里按房间规则不触发模型回复") }
                    return@launch
                }
                generateTurn(
                    conversationId,
                    persistedUser,
                    appended.turnId,
                    _state.value.currentModel,
                    consumeCrossWindowRequest()
                )'''
)
replace(vm,
    '''    private suspend fun generateTurn(
        conversationId: String,
        history: RemoteHistory,
        turnId: String,
        modelId: String
    ) {''',
    '''    private suspend fun generateTurn(
        conversationId: String,
        history: RemoteHistory,
        turnId: String,
        modelId: String,
        crossWindowRequest: CrossWindowRequest
    ) {'''
)
replace(vm,
    '''                local.wolf.state.value.basic.recentTurns,
                local.wolf.state.value.basic.contextBudget
            ).collect { progress ->''',
    '''                local.wolf.state.value.basic.recentTurns,
                local.wolf.state.value.basic.contextBudget,
                crossWindowRequest
            ).collect { progress ->'''
)
replace(vm,
    "                generateTurn(conversationId, persisted, turnId, model)\n",
    "                generateTurn(conversationId, persisted, turnId, model, consumeCrossWindowRequest())\n",
    2
)
replace(vm,
    "                generateTurn(conversationId, history, turnId, model)\n",
    "                generateTurn(conversationId, history, turnId, model, consumeCrossWindowRequest())\n"
)
replace(vm,
    "    private fun activateCachedConversation(conversation: ConversationSummary) {\n        persistence.put(KEY_CURRENT_CONVERSATION, conversation.id)\n",
    "    private fun activateCachedConversation(conversation: ConversationSummary) {\n        resetCrossWindow()\n        persistence.put(KEY_CURRENT_CONVERSATION, conversation.id)\n"
)
replace(vm,
    "    private fun openRoomLanding(roomType: RoomType) {\n        require(roomType in RoomType.entries)\n",
    "    private fun openRoomLanding(roomType: RoomType) {\n        require(roomType in RoomType.entries)\n        resetCrossWindow()\n"
)
replace(vm,
    "    private suspend fun <T> remoteOrNull(label: String, block: suspend () -> T): T? = try {",
    '''    private fun consumeCrossWindowRequest(): CrossWindowRequest {
        val request = _crossWindow.value.request()
        _crossWindow.value = CrossWindowUiState()
        return request
    }

    private fun resetCrossWindow() {
        _crossWindow.value = CrossWindowUiState()
    }

    private suspend fun <T> remoteOrNull(label: String, block: suspend () -> T): T? = try {'''
)

chat = "app/src/main/kotlin/com/elementeracoast/app/feature/chat/ChatScreen.kt"
replace(chat,
    "import com.elementeracoast.app.feature.dogtalk.DogtalkCard\n",
    "import com.elementeracoast.app.feature.dogtalk.CrossWindowRepository\nimport com.elementeracoast.app.feature.dogtalk.CrossWindowUiState\nimport com.elementeracoast.app.feature.dogtalk.DogtalkCard\n"
)
replace(chat,
    "    state: CoastShellState,\n    dogtalk: DogtalkRepository,\n",
    "    state: CoastShellState,\n    dogtalk: DogtalkRepository,\n    crossWindowRepository: CrossWindowRepository,\n    crossWindow: CrossWindowUiState,\n    onCrossWindowChange: (CrossWindowUiState) -> Unit,\n"
)
replace(chat,
    "                repository = dogtalk,\n                historyLoading = state.historyLoading,",
    "                repository = dogtalk,\n                crossWindowRepository = crossWindowRepository,\n                crossWindow = crossWindow,\n                onCrossWindowChange = onCrossWindowChange,\n                historyLoading = state.historyLoading,"
)

shell = "app/src/main/kotlin/com/elementeracoast/app/feature/shell/MainShell.kt"
replace(shell,
    "import com.elementeracoast.app.feature.dogtalk.DogtalkRepository\n",
    "import com.elementeracoast.app.feature.dogtalk.CrossWindowRepository\nimport com.elementeracoast.app.feature.dogtalk.CrossWindowUiState\nimport com.elementeracoast.app.feature.dogtalk.DogtalkRepository\n"
)
replace(shell,
    "    memory: MemoryRepository,\n    dogtalk: DogtalkRepository,\n",
    "    memory: MemoryRepository,\n    dogtalk: DogtalkRepository,\n    crossWindowRepository: CrossWindowRepository,\n    crossWindow: CrossWindowUiState,\n    onCrossWindowChange: (CrossWindowUiState) -> Unit,\n"
)
replace(shell,
    "                            state = state,\n                            dogtalk = dogtalk,\n",
    "                            state = state,\n                            dogtalk = dogtalk,\n                            crossWindowRepository = crossWindowRepository,\n                            crossWindow = crossWindow,\n                            onCrossWindowChange = onCrossWindowChange,\n"
)

activity = "app/src/main/kotlin/com/elementeracoast/app/MainActivity.kt"
replace(activity,
    "            val state by vm.state.collectAsState()\n            val wolf by vm.local.wolf.state.collectAsState()\n",
    "            val state by vm.state.collectAsState()\n            val crossWindow by vm.crossWindow.collectAsState()\n            val wolf by vm.local.wolf.state.collectAsState()\n"
)
replace(activity,
    "                        dogtalk = vm.dogtalk,\n",
    "                        dogtalk = vm.dogtalk,\n                        crossWindowRepository = vm.crossWindowRepository,\n                        crossWindow = crossWindow,\n                        onCrossWindowChange = vm::updateCrossWindow,\n"
)

test = "app/src/test/kotlin/com/elementeracoast/app/feature/shell/CoastShellViewModelTest.kt"
replace(test,
    "import com.elementeracoast.app.core.model.MessageAction\n",
    "import com.elementeracoast.app.core.model.CrossWindowLimits\nimport com.elementeracoast.app.core.model.CrossWindowMode\nimport com.elementeracoast.app.core.model.CrossWindowRequest\nimport com.elementeracoast.app.core.model.CrossWindowSource\nimport com.elementeracoast.app.core.model.CrossWindowSourceSnapshot\nimport com.elementeracoast.app.core.model.MessageAction\n"
)
replace(test,
    "import com.elementeracoast.app.feature.dogtalk.DogtalkRepository\n",
    "import com.elementeracoast.app.feature.dogtalk.CrossWindowRepository\nimport com.elementeracoast.app.feature.dogtalk.CrossWindowSelectionUi\nimport com.elementeracoast.app.feature.dogtalk.CrossWindowUiState\nimport com.elementeracoast.app.feature.dogtalk.DogtalkRepository\n"
)
insert_test = '''    @Test
    fun crossWindowSelectionIsForwardedOnceAndThenResets() {
        val fixture = Fixture()
        fixture.conversations.seed(RoomType.Main, "主聊天")
        val vm = fixture.vm()
        val source = CrossWindowSource(
            conversationId = "other-window",
            title = "旧窗口",
            roomType = "main",
            source = "coast",
            sourceWindowId = null,
            updatedAt = null,
            messageCount = 8,
            turnCount = 4,
            readable = true,
            disabledReason = ""
        )
        vm.updateCrossWindow(
            CrossWindowUiState(
                mode = CrossWindowMode.Manual,
                sources = listOf(source),
                selections = mapOf("other-window" to CrossWindowSelectionUi(checked = true, turns = 4)),
                limits = CrossWindowLimits(4, 20, 40, 6000, 24000)
            )
        )

        vm.sendMessage("请带上另一窗")

        assertEquals(CrossWindowMode.Manual, fixture.chat.lastCrossWindow.mode)
        assertEquals("other-window", fixture.chat.lastCrossWindow.sources.single().conversationId)
        assertEquals(4, fixture.chat.lastCrossWindow.sources.single().turns)
        assertEquals(CrossWindowMode.Off, vm.crossWindow.value.mode)

        vm.sendMessage("下一轮不要偷读")
        assertEquals(CrossWindowMode.Off, fixture.chat.lastCrossWindow.mode)
    }

'''
replace(test,
    "    @Test\n    fun lighthouseFirstSendPersistsUserTurnWithoutModelGeneration() {",
    insert_test + "    @Test\n    fun lighthouseFirstSendPersistsUserTurnWithoutModelGeneration() {"
)
replace(test,
    "        val dogtalk = FakeDogtalkRepository()\n        val persistence = MemoryLocalPersistence()\n",
    "        val dogtalk = FakeDogtalkRepository()\n        val crossWindow = FakeCrossWindowRepository()\n        val persistence = MemoryLocalPersistence()\n"
)
replace(test,
    "                memory = memory,\n                dogtalk = dogtalk\n",
    "                memory = memory,\n                dogtalk = dogtalk,\n                crossWindow = crossWindow\n"
)
fake_cross = '''    private class FakeCrossWindowRepository : CrossWindowRepository {
        override suspend fun sources(currentConversationId: String) = CrossWindowSourceSnapshot(
            description = "跨窗口测试",
            limits = CrossWindowLimits(4, 20, 40, 6000, 24000),
            sources = emptyList()
        )
    }

'''
replace(test,
    "    private class FakeThoughtSoilRepository : ThoughtSoilRepository {",
    fake_cross + "    private class FakeThoughtSoilRepository : ThoughtSoilRepository {"
)
replace(test,
    "        var lastContextBudget = 0\n",
    "        var lastContextBudget = 0\n        var lastCrossWindow = CrossWindowRequest()\n"
)
replace(test,
    "            recentTurns: Int,\n            contextBudget: Int\n        ): Flow<ChatProgress> = flow {",
    "            recentTurns: Int,\n            contextBudget: Int,\n            crossWindow: CrossWindowRequest\n        ): Flow<ChatProgress> = flow {"
)
replace(test,
    "            lastContextBudget = contextBudget\n",
    "            lastContextBudget = contextBudget\n            lastCrossWindow = crossWindow\n"
)

gradle = "app/build.gradle.kts"
replace(gradle,
    '        versionCode = 40\n        versionName = "0.1.38-snow-letter-context-cap-fix"\n',
    '        versionCode = 41\n        versionName = "0.1.39-dogtalk-crosswindow-01"\n'
)
