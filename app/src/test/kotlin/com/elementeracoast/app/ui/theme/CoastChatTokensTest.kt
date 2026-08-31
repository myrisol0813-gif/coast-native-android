package com.elementeracoast.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastChatTokensTest {
    @Test
    fun roomScaleUsesCompactChatOnlyTokens() {
        assertEquals(50.dp, CoastChatTokens.TopBarHeight)
        assertEquals(15.sp, CoastChatTokens.ChatBodySize)
        assertEquals(22.sp, CoastChatTokens.ChatBodyLineHeight)
        assertEquals(48.dp, CoastChatTokens.ComposerPillMinHeight)
        assertEquals(326.dp, CoastChatTokens.DrawerWidth)
        assertEquals(30.dp, CoastChatTokens.AssistantAvatarSize)
        assertTrue(CoastChatTokens.DogtalkExpandedMaxHeight <= 300.dp)
        assertEquals(30.dp, CoastChatTokens.DogtalkSaveVisualHeight)
        assertTrue(CoastChatTokens.DogtalkSaveTouchHeight >= CoastChatTokens.DogtalkSaveVisualHeight)
    }
}
