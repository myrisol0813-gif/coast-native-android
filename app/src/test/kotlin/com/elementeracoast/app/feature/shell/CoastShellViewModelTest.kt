package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastShellViewModelTest {
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
        val messageId = vm.state.value.messages.single().id

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
    fun userEditCreatesVariantSwitchesAndDeletesOnlyCurrentVariant() {
        val vm = CoastShellViewModel()
        val messageId = 500L
        vm.importMessages(listOf(ChatMessage(messageId, MessageRole.User, "旧消息")))

        vm.handleMessageAction(MessageAction.Edit(messageId, "新消息"))
        var message = vm.state.value.messages.single()
        assertEquals(2, message.variantCount)
        assertEquals(1, message.variantIndex)
        assertEquals("新消息", message.text)

        vm.handleMessageAction(MessageAction.SelectVariant(messageId, 0))
        message = vm.state.value.messages.single()
        assertEquals(0, message.variantIndex)
        assertEquals("旧消息", message.text)

        vm.handleMessageAction(MessageAction.SelectVariant(messageId, 1))
        vm.handleMessageAction(MessageAction.Delete(messageId))
        message = vm.state.value.messages.single()
        assertEquals(1, message.variantCount)
        assertEquals(0, message.variantIndex)
        assertEquals("旧消息", message.text)
        assertTrue(message.variants.size == 1)
    }

    @Test
    fun deleteMessageDoesNotMutateAnotherConversation() {
        val vm = CoastShellViewModel()
        val firstConversation = vm.state.value.activeConversationId
        val firstMessage = vm.state.value.messages.single().id
        val other = vm.state.value.conversations.first { it.id != firstConversation }

        vm.handleMessageAction(MessageAction.Delete(firstMessage))
        assertTrue(vm.state.value.messages.isEmpty())

        vm.selectConversation(other.id)
        assertTrue(vm.state.value.messages.isNotEmpty())

        vm.selectConversation(firstConversation)
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun regenerateActionIsExplicitlyLocalMessageAction() {
        val action = MessageAction.Regenerate(42L)
        assertEquals(42L, action.messageId)
    }
}
