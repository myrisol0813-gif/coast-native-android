package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastShellViewModelTest {
    @Test
    fun startupAndNewConversationsContainNoFixtureReplies() {
        val vm = CoastShellViewModel()
        assertTrue(vm.state.value.messages.isEmpty())

        vm.state.value.conversations.map { it.id }.forEach { id ->
            vm.selectConversation(id)
            assertTrue(vm.state.value.messages.isEmpty())
        }

        vm.newConversation()
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun newConversationUsesActiveRoomTypeWithoutRetaggingOthers() {
        val vm = CoastShellViewModel()
        vm.openRoomType(RoomType.Radio)
        vm.newConversation()

        val state = vm.state.value
        val created = state.conversations.first { it.id == state.activeConversationId }
        assertEquals(RoomType.Radio, state.activeRoomType)
        assertEquals(RoomType.Radio, created.roomType)
        assertTrue(created.title.startsWith("【电波】"))
        assertTrue(state.conversations.any { it.roomType == RoomType.Main })
        assertTrue(state.conversations.any { it.roomType == RoomType.Lighthouse })
    }

    @Test
    fun renameKeepsRoomPrefixAndDeletePrefersSameRoom() {
        val vm = CoastShellViewModel()
        vm.openRoomType(RoomType.Radio)
        val originalRadioId = vm.state.value.activeConversationId
        vm.newConversation()
        val createdId = vm.state.value.activeConversationId

        vm.renameConversation(createdId, "【灯塔】夜航")
        assertEquals(
            "【电波】夜航",
            vm.state.value.conversations.first { it.id == createdId }.title
        )

        vm.deleteConversation(createdId)
        assertFalse(vm.state.value.conversations.any { it.id == createdId })
        assertEquals(originalRadioId, vm.state.value.activeConversationId)
        assertEquals(RoomType.Radio, vm.state.value.activeRoomType)
    }

    @Test
    fun deleteActiveRadioFallsBackToMainWhenNoOtherRadioExists() {
        val vm = CoastShellViewModel()
        vm.openRoomType(RoomType.Radio)
        val onlyRadio = vm.state.value.activeConversationId

        vm.deleteConversation(onlyRadio)

        assertEquals(RoomType.Main, vm.state.value.activeRoomType)
        assertEquals(
            RoomType.Main,
            vm.state.value.conversations
                .first { it.id == vm.state.value.activeConversationId }
                .roomType
        )
    }

    @Test
    fun deletingLastConversationCreatesFreshEmptyMainFallback() {
        val vm = CoastShellViewModel()
        val oldId = vm.state.value.activeConversationId
        vm.importMessages(listOf(ChatMessage(9L, MessageRole.User, "旧内容")))
        assertTrue(vm.state.value.messages.isNotEmpty())

        vm.state.value.conversations
            .map { it.id }
            .filterNot { it == oldId }
            .forEach(vm::deleteConversation)
        assertEquals(1, vm.state.value.conversations.size)

        vm.deleteConversation(oldId)

        val state = vm.state.value
        assertEquals(1, state.conversations.size)
        assertNotEquals(oldId, state.activeConversationId)
        assertEquals(RoomType.Main, state.activeRoomType)
        assertEquals("新聊天 1", state.conversations.single().title)
        assertTrue(state.messages.isEmpty())
        assertEquals("已清空最后一个窗口", state.snackbarMessage)
    }

    @Test
    fun deletingNonActiveConversationDoesNotMoveActiveWindow() {
        val vm = CoastShellViewModel()
        val activeBefore = vm.state.value.activeConversationId
        val target = vm.state.value.conversations.first { it.id != activeBefore }.id

        vm.deleteConversation(target)

        assertEquals(activeBefore, vm.state.value.activeConversationId)
        assertFalse(vm.state.value.conversations.any { it.id == target })
    }

    @Test
    fun likeAndFavoriteToggleOnlyCurrentLocalMessage() {
        val vm = CoastShellViewModel()
        val messageId = 77L
        vm.importMessages(listOf(ChatMessage(messageId, MessageRole.Assistant, "本地回复")))

        vm.handleMessageAction(MessageAction.ToggleLike(messageId))
        vm.handleMessageAction(MessageAction.ToggleFavorite(messageId))

        val message = vm.state.value.messages.single()
        assertTrue(message.liked)
        assertTrue(message.favorite)

        vm.handleMessageAction(MessageAction.ToggleLike(messageId))
        vm.handleMessageAction(MessageAction.ToggleFavorite(messageId))
        val reset = vm.state.value.messages.single()
        assertFalse(reset.liked)
        assertFalse(reset.favorite)
    }

    @Test
    fun userEditCreatesPairedAssistantVariantAndCurrentDeleteOnlyRemovesOneVersion() {
        val vm = CoastShellViewModel(generationDispatcher = Dispatchers.Unconfined)
        val userId = 500L
        val assistantId = 501L
        vm.importMessages(
            listOf(
                ChatMessage(userId, MessageRole.User, "旧消息"),
                ChatMessage(assistantId, MessageRole.Assistant, "旧回复", modelId = "Native local")
            )
        )

        vm.handleMessageAction(MessageAction.Edit(userId, "新消息"))
        var user = vm.state.value.messages.first { it.id == userId }
        var assistant = vm.state.value.messages.first { it.id == assistantId }
        assertEquals(2, user.variantCount)
        assertEquals(1, user.variantIndex)
        assertEquals("新消息", user.text)
        assertEquals(2, assistant.variantCount)
        assertEquals(1, assistant.variantIndex)
        assertEquals("", assistant.text)
        assertEquals(assistantId, vm.state.value.streamingMessageId)
        assertTrue(vm.state.value.isStreaming)

        vm.stopGeneration()
        vm.handleMessageAction(MessageAction.SelectVariant(userId, 0))
        vm.handleMessageAction(MessageAction.SelectVariant(assistantId, 0))
        user = vm.state.value.messages.first { it.id == userId }
        assistant = vm.state.value.messages.first { it.id == assistantId }
        assertEquals("旧消息", user.text)
        assertEquals("旧回复", assistant.text)
        assertEquals(0, user.variantIndex)
        assertEquals(0, assistant.variantIndex)

        vm.handleMessageAction(MessageAction.SelectVariant(userId, 1))
        vm.handleMessageAction(MessageAction.Delete(userId))
        user = vm.state.value.messages.first { it.id == userId }
        assertEquals(1, user.variantCount)
        assertEquals(0, user.variantIndex)
        assertEquals("旧消息", user.text)
        assertEquals(1, user.variants.size)
        assertEquals(2, vm.state.value.messages.first { it.id == assistantId }.variantCount)
    }

    @Test
    fun deleteMessageDoesNotMutateAnotherConversation() {
        val vm = CoastShellViewModel()
        val firstConversation = vm.state.value.activeConversationId
        val firstMessage = 201L
        vm.importMessages(listOf(ChatMessage(firstMessage, MessageRole.Assistant, "第一窗口")))

        val other = vm.state.value.conversations.first { it.id != firstConversation }
        vm.selectConversation(other.id)
        vm.importMessages(listOf(ChatMessage(202L, MessageRole.Assistant, "第二窗口")))
        vm.selectConversation(firstConversation)

        vm.handleMessageAction(MessageAction.Delete(firstMessage))
        assertTrue(vm.state.value.messages.isEmpty())

        vm.selectConversation(other.id)
        assertEquals("第二窗口", vm.state.value.messages.single().text)

        vm.selectConversation(firstConversation)
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun regenerateActionIsExplicitlyLocalMessageAction() {
        val action = MessageAction.Regenerate(42L)
        assertEquals(42L, action.messageId)
    }
}
