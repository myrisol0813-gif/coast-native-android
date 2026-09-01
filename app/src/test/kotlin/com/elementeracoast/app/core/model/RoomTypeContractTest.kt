package com.elementeracoast.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTypeContractTest {
    @Test
    fun radioAndLighthouseTitlesKeepRequiredPrefixes() {
        assertEquals("【电波】新聊天 2", roomConversationTitle(RoomType.Radio, 2))
        assertEquals("【灯塔】新聊天 3", roomConversationTitle(RoomType.Lighthouse, 3))
        assertEquals("新聊天 4", roomConversationTitle(RoomType.Main, 4))
        assertEquals("【电波】夜航", roomConversationTitle(RoomType.Radio, "【灯塔】夜航"))
        assertEquals("新的窗口", roomConversationTitle(RoomType.Main, "【灯塔】新的窗口"))
    }

    @Test
    fun nativeV1HasExactlyThreePwaRoomTypes() {
        assertEquals(
            listOf(RoomType.Main, RoomType.Radio, RoomType.Lighthouse),
            RoomType.entries
        )
        assertEquals(listOf("main", "radio", "lighthouse"), RoomType.entries.map { it.wireValue })
        assertTrue(RoomType.Radio.titlePrefix.isNotBlank())
        assertTrue(RoomType.Lighthouse.titlePrefix.isNotBlank())
    }

    @Test
    fun nativeV1ThemeContractIsLightDarkGold() {
        assertEquals(
            listOf(CoastThemeMode.Light, CoastThemeMode.Dark, CoastThemeMode.Gold),
            CoastThemeMode.entries
        )
    }
}
