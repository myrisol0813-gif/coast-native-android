package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.auth.AuthRepository
import com.elementeracoast.app.core.auth.AuthSession
import com.elementeracoast.app.core.auth.SessionRestoreResult
import com.elementeracoast.app.core.local.MemoryLocalPersistence
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.ThoughtSeedSnapshot
import com.elementeracoast.app.core.model.ThoughtSoilSnapshot
import com.elementeracoast.app.core.model.TurnDeskReceipt
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.remote.RemoteHistory
import com.elementeracoast.app.core.remote.RemoteModelCatalogItem
import com.elementeracoast.app.core.remote.RemoteModelCatalogResponse
import com.elementeracoast.app.core.remote.RemoteModelGroups
import com.elementeracoast.app.core.remote.RemoteProfile
import com.elementeracoast.app.feature.chat.ChatProgress
import com.elementeracoast.app.feature.chat.ChatRepository
import com.elementeracoast.app.feature.chat.ChatSyncMapper
import com.elementeracoast.app.feature.daily.DailyDiary
import com.elementeracoast.app.feature.daily.DailyMoment
import com.elementeracoast.app.feature.daily.DailyProfile
import com.elementeracoast.app.feature.daily.DailyProfileImageField
import com.elementeracoast.app.feature.daily.DailyRepository
import com.elementeracoast.app.feature.daily.DailySnapshot
import com.elementeracoast.app.feature.memory.ThoughtSoilRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastShellViewModelTest {
    @Test
    fun restoredSessionLoadsSharedConversationHistoryAndProfile() {
        val fixture = Fixture()
        val existing = fixture.conversations.seed(RoomType.Main, "PWA 已有窗口")
        val user = ChatSyncMapper.appendUser(RemoteHistory(conversationId = existing.id), "PWA 留下的问题")
        fixture.chat.histories[existing.id] = ChatSyncMapper.appendAssistant(
            user.history,
            user.turnId,
            "PWA 已有回复",
            "openai/gpt-5.6",
            "stop"
        )

        val vm = fixture.vm()
        val state = vm.state.value

        assertTrue(state.authenticated)
        assertFalse(state.authBusy)
        assertEquals(existing.id, state.activeConversationId)
        assertEquals("openai/gpt-5.6", state.currentModel)
        assertEquals(listOf("PWA 留下的问题", "PWA 已有回复"), state.messages.map { it.text })
        assertTrue(state.models.contains("openai/gpt-5.6"))
        assertEquals("chat://myri", state.myriAvatarDataUrl)
        assertEquals("daily://xiaohan", state.xiaohanAvatarDataUrl)
        assertEquals("daily://cover", state.coverDataUrl)
    }

    @Test
    fun MyriAvatarWritesBackThroughChatProfileOwner() {
        val fixture = Fixture()
        fixture.conversations.seed(RoomType.Main, "主聊天")
        val vm = fixture.vm()

        vm.updateMyriAvatar("data:image/webp;base64,MYRI")

        assertEquals("data:image/webp;base64,MYRI", fixture.profile.current.assistantAvatarDataUrl)
        assertEquals("data:image/webp;base64,MYRI", vm.state.value.myriAvatarDataUrl)
        assertEquals("daily://myri", fixture.daily.cachedProfile().myriAvatarDataUrl)
    }

    @Test
    fun radioLandingDoesNotPrecreateThreadAndFirstSendCreatesRemoteConversation() {
        val fixture = Fixture()
        fixture.conversations.seed(RoomType.Main, "主聊天")
        val vm = fixture.vm()

        vm.openRoomType(RoomType.Radio)
        assertEquals("", vm.state.value.activeConversationId)
        assertFalse(vm.state.value.conversations.any { it.roomType == RoomType.Radio })

        vm.sendMessage("电波测试")

        val state = vm.state.value
        assertEquals(RoomType.Radio, state.activeRoomType)
        assertTrue(state.activeConversationId.isNotBlank())
        assertTrue(state.conversations.any { it.id == state.activeConversationId && it.roomType == RoomType.Radio })
        assertEquals(listOf(MessageRole.User, MessageRole.Assistant), state.messages.map { it.role })
        assertEquals("电波测试", state.messages.first().text)
        assertEquals("真实流回复", state.messages.last().text)
        assertEquals(1, fixture.chat.streamCalls)
    }

    @Test
    fun lighthouseFirstSendPersistsUserTurnWithoutModelGeneration() {
        val fixture = Fixture()
        fixture.conversations.seed(RoomType.Main, "主聊天")
        val vm = fixture.vm()

        vm.openRoomType(RoomType.Lighthouse)
        vm.sendMessage("灯塔只收信")

        val state = vm.state.value
        assertEquals(RoomType.Lighthouse, state.activeRoomType)
        assertTrue(state.activeConversationId.isNotBlank())
        assertEquals(1, state.messages.size)
        assertEquals(MessageRole.User, state.messages.single().role)
        assertEquals("灯塔只收信", state.messages.single().text)
        assertEquals(0, fixture.chat.streamCalls)
        assertEquals(1, fixture.chat.persistCalls)
    }

    @Test
    fun failedStreamMarksSameUserTurnAndRetryDoesNotDuplicateUserMessage() {
        val fixture = Fixture()
        fixture.conversations.seed(RoomType.Main, "主聊天")
        fixture.chat.failStream = true
        val vm = fixture.vm()

        vm.sendMessage("只发一次")
        var state = vm.state.value
        val failedUser = state.messages.single { it.role == MessageRole.User }
        assertTrue(failedUser.errorDetail!!.contains("network_unreachable"))
        assertEquals(1, state.messages.count { it.role == MessageRole.User })

        fixture.chat.failStream = false
        vm.handleMessageAction(MessageAction.Retry(failedUser.id))
        state = vm.state.value

        assertEquals(1, state.messages.count { it.role == MessageRole.User })
        assertEquals(1, state.messages.count { it.role == MessageRole.Assistant })
        assertEquals("只发一次", state.messages.first { it.role == MessageRole.User }.text)
        assertEquals("真实流回复", state.messages.first { it.role == MessageRole.Assistant }.text)
        assertEquals(2, fixture.chat.streamCalls)
    }

    @Test
    fun completedReplyExposesDeskReceiptAndOrganizesThoughtSoil() {
        val fixture = Fixture()
        fixture.conversations.seed(RoomType.Main, "主聊天")
        val vm = fixture.vm()

        vm.sendMessage("把本轮上下文收好")

        assertEquals("本轮递给模型", vm.state.value.turnDeskReceipt?.summary)
        assertEquals(1, fixture.thoughtSoil.organizeCalls)
        assertEquals(listOf("新芽"), vm.state.value.thoughtSoil?.handSeeds?.map { it.name })
    }

    @Test
    fun backendConversationDeleteNeverCreatesLocalFallbackConversation() {
        val fixture = Fixture()
        val only = fixture.conversations.seed(RoomType.Main, "唯一窗口")
        val vm = fixture.vm()
        assertEquals(only.id, vm.state.value.activeConversationId)

        vm.deleteConversation(only.id)

        assertTrue(vm.state.value.conversations.isEmpty())
        assertEquals("", vm.state.value.activeConversationId)
        assertEquals(RoomType.Main, vm.state.value.activeRoomType)
    }

    @Test
    fun localVariantViewingChangesCacheOnlyAndDoesNotPersistFakeSuccess() {
        val fixture = Fixture()
        val existing = fixture.conversations.seed(RoomType.Main, "分支窗口")
        val first = ChatSyncMapper.appendUser(RemoteHistory(conversationId = existing.id), "问题")
        var history = ChatSyncMapper.appendAssistant(first.history, first.turnId, "版本一", "model", "stop")
        history = ChatSyncMapper.appendAssistant(history, first.turnId, "版本二", "model", "stop")
        fixture.chat.histories[existing.id] = history
        val vm = fixture.vm()
        val assistant = vm.state.value.messages.first { it.role == MessageRole.Assistant }
        val beforePersist = fixture.chat.persistCalls

        vm.handleMessageAction(MessageAction.SelectVariant(assistant.id, 0))

        assertEquals("版本一", vm.state.value.messages.first { it.role == MessageRole.Assistant }.text)
        assertEquals(beforePersist, fixture.chat.persistCalls)
    }

    private class Fixture {
        val auth = FakeAuthRepository()
        val conversations = FakeConversationRepository()
        val profile = FakeProfileRepository()
        val chat = FakeChatRepository()
        val thoughtSoil = FakeThoughtSoilRepository()
        val daily = FakeDailyRepository()
        val persistence = MemoryLocalPersistence()

        fun vm(): CoastShellViewModel = CoastShellViewModel(
            persistence = persistence,
            backend = CoastBackendGraph(
                auth = auth,
                conversations = conversations,
                profile = profile,
                chat = chat,
                thoughtSoil = thoughtSoil,
                daily = daily
            ),
            workDispatcher = Dispatchers.Unconfined
        )
    }

    private class FakeAuthRepository : AuthRepository {
        private val session = AuthSession("__Host-coast_session=test", Long.MAX_VALUE / 1000)
        var loggedOut = false

        override suspend fun login(password: String): AuthSession = session
        override suspend fun restore(): SessionRestoreResult = SessionRestoreResult.Restored(session)
        override suspend fun logout() { loggedOut = true }
        override fun current(): AuthSession = session
        override fun clearConfirmedInvalidSession() = Unit
    }

    private class FakeConversationRepository : ConversationRepository {
        private var next = 1
        private val values = mutableListOf<com.elementeracoast.app.core.model.ConversationSummary>()

        fun seed(roomType: RoomType, title: String) = com.elementeracoast.app.core.model.ConversationSummary(
            "${roomType.wireValue}-${next++}", title, roomType
        ).also { values += it }

        override fun cached() = values.toList()
        override suspend fun refresh() = values.toList()
        override suspend fun create(roomType: RoomType, title: String) = seed(roomType, title)
        override suspend fun rename(id: String, title: String): com.elementeracoast.app.core.model.ConversationSummary {
            val index = values.indexOfFirst { it.id == id }
            val updated = values[index].copy(title = title)
            values[index] = updated
            return updated
        }
        override suspend fun delete(id: String) { values.removeAll { it.id == id } }
    }

    private class FakeProfileRepository : ProfileRepository {
        var current = RemoteProfile(
            assistantAvatarDataUrl = "chat://myri",
            currentChatModel = "openai/gpt-5.6",
            modelBox = com.elementeracoast.app.core.remote.RemoteModelBox(chat = listOf("openai/gpt-5.6"))
        )
        private val catalog = RemoteModelCatalogResponse(
            ok = true,
            groups = RemoteModelGroups(
                openAiChat = listOf(RemoteModelCatalogItem("openai/gpt-5.6", "GPT-5.6"))
            )
        )

        override fun cachedProfile() = current
        override fun cachedModels() = catalog
        override suspend fun refreshProfile() = current
        override suspend fun refreshModels(force: Boolean) = catalog
        override suspend fun setCurrentChatModel(modelId: String): RemoteProfile {
            current = current.copy(currentChatModel = modelId)
            return current
        }
        override suspend fun setAssistantAvatar(dataUrl: String): RemoteProfile {
            current = current.copy(assistantAvatarDataUrl = dataUrl)
            return current
        }
    }

    private class FakeDailyRepository : DailyRepository {
        private val state = MutableStateFlow(
            DailySnapshot(
                profile = DailyProfile(
                    xiaohanAvatarDataUrl = "daily://xiaohan",
                    myriAvatarDataUrl = "daily://myri",
                    momentCoverDataUrl = "daily://cover"
                )
            )
        )
        override val snapshot: StateFlow<DailySnapshot> = state
        override fun cachedProfile(): DailyProfile = state.value.profile
        override suspend fun refresh(): DailySnapshot = state.value
        override suspend fun refreshProfile(): DailyProfile = state.value.profile
        override suspend fun createMoment(date: String, text: String): DailyMoment = unsupported()
        override suspend fun patchMoment(id: String, date: String?, text: String?): DailyMoment = unsupported()
        override suspend fun deleteMoment(id: String) = unsupported<Unit>()
        override suspend fun setMomentLike(id: String, liked: Boolean): DailyMoment = unsupported()
        override suspend fun addMomentComment(id: String, text: String): DailyMoment = unsupported()
        override suspend fun deleteMomentComment(id: String, commentId: String): DailyMoment = unsupported()
        override suspend fun requestMyriComment(id: String): DailyMoment = unsupported()
        override suspend fun createDiary(date: String, weather: String, mood: String, tags: List<String>, text: String): DailyDiary = unsupported()
        override suspend fun patchDiary(id: String, date: String, weather: String, mood: String, tags: List<String>, text: String): DailyDiary = unsupported()
        override suspend fun deleteDiary(id: String) = unsupported<Unit>()
        override suspend fun updateProfile(field: DailyProfileImageField, dataUrl: String): DailyProfile = unsupported()

        private fun <T> unsupported(): T = throw UnsupportedOperationException("Daily mutation is not used by shell tests")
    }

    private class FakeThoughtSoilRepository : ThoughtSoilRepository {
        var organizeCalls = 0

        override suspend fun load(conversationId: String): ThoughtSoilSnapshot = soil(conversationId, emptyList())

        override suspend fun organizeAfterReply(conversationId: String, modelId: String): ThoughtSoilSnapshot {
            organizeCalls += 1
            return soil(
                conversationId,
                listOf(ThoughtSeedSnapshot("新芽", "本轮新承接", "下一轮继续", "不要机械复读"))
            )
        }

        private fun soil(conversationId: String, seeds: List<ThoughtSeedSnapshot>) = ThoughtSoilSnapshot(
            conversationId = conversationId,
            currentText = "当前窗口继续承接本轮。",
            handSeeds = seeds,
            doNotRepeat = "",
            pocketCandidates = emptyList(),
            manualLocked = false,
            revision = 1,
            organizer = "GPT-5.6",
            updatedAt = ""
        )
    }

    private class FakeChatRepository : ChatRepository {
        val histories = linkedMapOf<String, RemoteHistory>()
        var streamCalls = 0
        var persistCalls = 0
        var failStream = false

        override fun cachedHistory(conversationId: String) = histories[conversationId]

        override suspend fun loadHistory(conversationId: String): RemoteHistory =
            histories.getOrPut(conversationId) { RemoteHistory(conversationId = conversationId) }

        override suspend fun persistHistory(conversationId: String, history: RemoteHistory): RemoteHistory {
            persistCalls += 1
            val saved = history.copy(conversationId = conversationId)
            histories[conversationId] = saved
            return saved
        }

        override fun streamReply(
            conversationId: String,
            historyWithUser: RemoteHistory,
            turnId: String,
            modelId: String
        ): Flow<ChatProgress> = flow {
            streamCalls += 1
            if (failStream) throw CoastApiException(
                CoastApiErrorKind.Network,
                "network_unreachable",
                "无法连接海岸后端。"
            )
            emit(ChatProgress.Delta("真实流"))
            emit(ChatProgress.Delta("回复"))
            val completed = ChatSyncMapper.appendAssistant(
                ChatSyncMapper.clearUserFailure(historyWithUser, turnId),
                turnId,
                "真实流回复",
                modelId,
                "stop"
            ).copy(conversationId = conversationId)
            histories[conversationId] = completed
            emit(
                ChatProgress.Completed(
                    completed,
                    modelId,
                    "stop",
                    TurnDeskReceipt("本轮递给模型", "上下文已在舒服区间", emptyList())
                )
            )
        }

        override fun failedHistory(
            historyWithUser: RemoteHistory,
            turnId: String,
            modelId: String,
            error: CoastApiException,
            partialContent: String
        ): RemoteHistory = ChatSyncMapper.markUserFailure(
            historyWithUser,
            turnId,
            "${error.type}: ${error.message}"
        )

        override fun cancelledHistory(
            historyWithUser: RemoteHistory,
            turnId: String,
            modelId: String,
            partialContent: String
        ): RemoteHistory = ChatSyncMapper.clearUserFailure(historyWithUser, turnId)

        override fun clearFailure(history: RemoteHistory, turnId: String) = ChatSyncMapper.clearUserFailure(history, turnId)
        override fun cacheHistory(conversationId: String, history: RemoteHistory) { histories[conversationId] = history }
    }
}
