package com.elementeracoast.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowLetterSkinsTest {
    @Test
    fun embossedModeKeepsTheExistingRoundedConversationPaper() {
        assertTrue(SnowLetterSurfaceRole.UserBubble.usesEmbossedPaper(CoastPaperMode.Wave))
        assertTrue(SnowLetterSurfaceRole.AssistantBubble.usesEmbossedPaper(CoastPaperMode.Wave))

        for (role in SnowLetterSurfaceRole.entries) {
            if (role == SnowLetterSurfaceRole.UserBubble || role == SnowLetterSurfaceRole.AssistantBubble) continue
            assertFalse(role.usesEmbossedPaper(CoastPaperMode.Wave))
        }
    }

    @Test
    fun cutModeIsIndependentAndAvailableToSnowLetterSurfaces() {
        for (role in SnowLetterSurfaceRole.entries) {
            assertTrue(role.usesCutPaper(CoastPaperMode.Cut))
            assertFalse(role.usesEmbossedPaper(CoastPaperMode.Cut))
        }
    }

    @Test
    fun cloudPaperModeDoesNotStealExistingPaperModes() {
        for (role in SnowLetterSurfaceRole.entries) {
            assertFalse(role.usesCutPaper(CoastPaperMode.Cloud))
            assertFalse(role.usesEmbossedPaper(CoastPaperMode.Cloud))
            assertFalse(role.usesCloudPaper(CoastPaperMode.Cut))
            assertFalse(role.usesCloudPaper(CoastPaperMode.Wave))
            assertFalse(role.usesCloudPaper(CoastPaperMode.Smooth))
        }
    }

    @Test
    fun smoothModeUsesOnlyFallbackShapes() {
        for (role in SnowLetterSurfaceRole.entries) {
            assertFalse(role.usesEmbossedPaper(CoastPaperMode.Smooth))
            assertFalse(role.usesCutPaper(CoastPaperMode.Smooth))
        }
    }
}
