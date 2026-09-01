package com.elementeracoast.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationContractTest {
    @Test
    fun emptySearchKeepsAllRoomTypesVisible() {
        val conversations = CoastShellState().conversations
        val visible = filterConversations(conversations, "")
        assertEquals(conversations, visible)
        assertTrue(visible.any { it.roomType == RoomType.Main })
        assertTrue(visible.any { it.roomType == RoomType.Radio })
        assertTrue(visible.any { it.roomType == RoomType.Lighthouse })
    }

    @Test
    fun roomTitlesKeepExactlyOneRequiredPrefix() {
        assertEquals("【电波】新的窗口", roomConversationTitle(RoomType.Radio, "新的窗口"))
        assertEquals("【电波】新的窗口", roomConversationTitle(RoomType.Radio, "【电波】新的窗口"))
        assertEquals("【灯塔】新的窗口", roomConversationTitle(RoomType.Lighthouse, "【电波】新的窗口"))
        assertEquals("新的窗口", roomConversationTitle(RoomType.Main, "【灯塔】新的窗口"))
    }
}
