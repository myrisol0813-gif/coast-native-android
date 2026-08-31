package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.model.ChatScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastShellViewModelTest {
    @Test fun radioNewConversationDoesNotHideOrRetagOtherScopes() {
        val vm = CoastShellViewModel(); vm.openScope(ChatScope.Radio); vm.newConversation()
        val state = vm.state.value; val created = state.conversations.first()
        assertEquals(ChatScope.Radio, created.scope)
        assertTrue(created.title.startsWith("【电波】"))
        assertTrue(state.conversations.any { it.scope == ChatScope.Main })
        assertTrue(state.conversations.any { it.scope == ChatScope.Lighthouse })
    }
    @Test fun renameKeepsScopePrefixAndDeleteRemovesOnlyTarget() {
        val vm = CoastShellViewModel(); vm.openScope(ChatScope.Radio); vm.newConversation()
        val id = vm.state.value.activeConversationId
        vm.renameConversation(id, "夜航")
        assertEquals("【电波】夜航", vm.state.value.conversations.first { it.id == id }.title)
        val mainBefore = vm.state.value.conversations.filter { it.scope == ChatScope.Main }.map { it.id }.toSet()
        vm.deleteConversation(id)
        assertFalse(vm.state.value.conversations.any { it.id == id })
        assertEquals(mainBefore, vm.state.value.conversations.filter { it.scope == ChatScope.Main }.map { it.id }.toSet())
    }
}
