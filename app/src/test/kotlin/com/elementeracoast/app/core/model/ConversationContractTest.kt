package com.elementeracoast.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContractTest {
    @Test fun emptySearchKeepsAllScopesVisible() {
        val conversations = CoastShellState().conversations
        val visible = filterConversations(conversations, "")
        assertEquals(conversations, visible)
        assertTrue(visible.any { it.scope == ChatScope.Main })
        assertTrue(visible.any { it.scope == ChatScope.Radio })
        assertTrue(visible.any { it.scope == ChatScope.Lighthouse })
    }
    @Test fun scopedTitlesKeepExactlyOneRoomPrefix() {
        assertEquals("【电波】新的窗口", scopedConversationTitle(ChatScope.Radio, "新的窗口"))
        assertEquals("【电波】新的窗口", scopedConversationTitle(ChatScope.Radio, "【电波】新的窗口"))
        assertEquals("【灯塔】新的窗口", scopedConversationTitle(ChatScope.Lighthouse, "【电波】新的窗口"))
        assertEquals("新的窗口", scopedConversationTitle(ChatScope.Main, "【灯塔】新的窗口"))
    }
}
