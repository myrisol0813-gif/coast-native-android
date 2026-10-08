package com.elementeracoast.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowLetterSkinsTest {
    @Test
    fun wavePaperAppliesToBothConversationRoles() {
        assertTrue(SnowLetterSurfaceRole.UserBubble.usesLetterPaper(CoastPaperMode.Wave))
        assertTrue(SnowLetterSurfaceRole.AssistantBubble.usesLetterPaper(CoastPaperMode.Wave))
    }

    @Test
    fun smoothPaperKeepsBothBubblesPlain() {
        assertFalse(SnowLetterSurfaceRole.UserBubble.usesLetterPaper(CoastPaperMode.Smooth))
        assertFalse(SnowLetterSurfaceRole.AssistantBubble.usesLetterPaper(CoastPaperMode.Smooth))
    }

    @Test
    fun statusComposerAndDogtalkSurfacesStayUnaffected() {
        for (role in SnowLetterSurfaceRole.entries) {
            if (role == SnowLetterSurfaceRole.UserBubble || role == SnowLetterSurfaceRole.AssistantBubble) continue
            assertFalse(role.usesLetterPaper(CoastPaperMode.Wave))
            assertFalse(role.usesLetterPaper(CoastPaperMode.Smooth))
        }
    }
}
