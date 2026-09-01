package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.model.RoomType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun deletingLastRoomWindowFallsBackToMain() {
        val vm = CoastShellViewModel()
        vm.openRoomType(RoomType.Radio)
        val onlyRadio = vm.state.value.activeConversationId

        vm.deleteConversation(onlyRadio)

        assertEquals(RoomType.Main, vm.state.value.activeRoomType)
        assertTrue(
            vm.state.value.conversations
                .first { it.id == vm.state.value.activeConversationId }
                .roomType == RoomType.Main
        )
    }

    @Test
    fun deletingActiveSpecialRoomCreatesMainWhenNoMainRemains() {
        val vm = CoastShellViewModel()
        vm.openRoomType(RoomType.Radio)

        val mainIds = vm.state.value.conversations
            .filter { it.roomType == RoomType.Main }
            .map { it.id }
        mainIds.forEach(vm::deleteConversation)
        assertFalse(vm.state.value.conversations.any { it.roomType == RoomType.Main })

        val activeRadio = vm.state.value.activeConversationId
        vm.deleteConversation(activeRadio)

        assertEquals(RoomType.Main, vm.state.value.activeRoomType)
        assertTrue(vm.state.value.conversations.any { it.roomType == RoomType.Main })
    }
}
