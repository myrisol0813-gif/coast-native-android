package com.elementeracoast.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopeContractTest {
    @Test
    fun radioAndLighthouseNewConversationTitlesKeepRequiredPrefixes() {
        assertEquals("【电波】新聊天 2", scopedConversationTitle(ChatScope.Radio, 2))
        assertEquals("【灯塔】新聊天 3", scopedConversationTitle(ChatScope.Lighthouse, 3))
        assertEquals("新聊天 4", scopedConversationTitle(ChatScope.Main, 4))
    }

    @Test
    fun nativeV1HasExactlyThreeChatScopes() {
        assertEquals(listOf(ChatScope.Main, ChatScope.Radio, ChatScope.Lighthouse), ChatScope.entries)
        assertTrue(ChatScope.Radio.titlePrefix.isNotBlank())
        assertTrue(ChatScope.Lighthouse.titlePrefix.isNotBlank())
    }

    @Test
    fun nativeV1ThemeContractIsLightDarkGold() {
        assertEquals(
            listOf(CoastThemeMode.Light, CoastThemeMode.Dark, CoastThemeMode.Gold),
            CoastThemeMode.entries
        )
    }
}
