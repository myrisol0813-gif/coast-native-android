package com.elementeracoast.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarryCloudShapeTest {
    @Test fun onlyStarryCoastMessageBubblesUseClouds() {
        val bubbleRoles = listOf(SnowLetterSurfaceRole.AssistantBubble, SnowLetterSurfaceRole.UserBubble)
        bubbleRoles.forEach { role ->
            assertTrue(role.usesStarryCloud(CoastThemePreset.StarryCoast))
            CoastThemePreset.entries.filterNot { it == CoastThemePreset.StarryCoast }
                .forEach { preset -> assertFalse("$preset/$role", role.usesStarryCloud(preset)) }
        }
        SnowLetterSurfaceRole.entries.filterNot { it in bubbleRoles }
            .forEach { role -> assertFalse(role.name, role.usesStarryCloud(CoastThemePreset.StarryCoast)) }
    }

    @Test fun invalidOrTinyEdgeLengthsNeverCreateExcessCloudLobes() {
        assertEquals(1, starryCloudLobeCount(-5f, 80f, 5))
        assertEquals(1, starryCloudLobeCount(100f, 0f, 5))
        assertEquals(1, starryCloudLobeCount(100f, 100f, 0))
    }

    @Test fun cloudSegmentsAdaptWithoutExplodingOnLongMessages() {
        assertEquals(1, starryCloudLobeCount(0f, 80f, 5))
        assertEquals(1, starryCloudLobeCount(40f, 80f, 5))
        assertEquals(3, starryCloudLobeCount(230f, 80f, 5))
        assertEquals(5, starryCloudLobeCount(1200f, 80f, 5))
        assertEquals(9, starryCloudLobeCount(2400f, 110f, 9))
    }
}
