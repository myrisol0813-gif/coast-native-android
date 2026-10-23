package com.elementeracoast.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowLetterPaperModeTest {
    @Test
    fun waveAppliesToPaperSurfacesButNotCompactControls() {
        assertTrue(SnowLetterSurfaceRole.AssistantBubble.usesLetterPaper(CoastPaperMode.Wave))
        assertTrue(SnowLetterSurfaceRole.StatusCard.usesLetterPaper(CoastPaperMode.Wave))
        assertTrue(SnowLetterSurfaceRole.DogtalkCard.usesLetterPaper(CoastPaperMode.Wave))
        assertFalse(SnowLetterSurfaceRole.ComposerField.usesLetterPaper(CoastPaperMode.Wave))
        assertFalse(SnowLetterSurfaceRole.ActionButton.usesLetterPaper(CoastPaperMode.Wave))
        assertFalse(SnowLetterSurfaceRole.DogtalkField.usesLetterPaper(CoastPaperMode.Wave))
    }

    @Test
    fun smoothKeepsEverySurfaceOnItsFallbackShape() {
        SnowLetterSurfaceRole.entries.forEach { role ->
            assertFalse(role.usesLetterPaper(CoastPaperMode.Smooth))
        }
    }
}
