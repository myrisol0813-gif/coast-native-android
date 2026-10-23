package com.elementeracoast.app.ui.theme

import com.elementeracoast.app.core.local.MemoryLocalPersistence
import com.elementeracoast.app.feature.wolf.WolfStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastChatArtworkTest {
    @Test fun switchingAndRestartingPreservesThemeAndCustomBackgroundIndependently() {
        val persistence = MemoryLocalPersistence()
        val store = WolfStore(persistence)
        store.setChatBackground("My background", "/local/background.png")
        store.setTheme(CoastThemePreset.StarryCoast)

        val restored = WolfStore(persistence)
        assertEquals(CoastThemePreset.StarryCoast, restored.state.value.appearance.theme)
        assertEquals("/local/background.png", restored.state.value.appearance.chatBackgroundImagePath)
        assertNotNull(restored.state.value.appearance.theme.chatArtwork())

        restored.setTheme(CoastThemePreset.SnowLetter)
        val switched = WolfStore(persistence).state.value.appearance
        assertEquals(CoastThemePreset.SnowLetter, switched.theme)
        assertEquals("/local/background.png", switched.chatBackgroundImagePath)
        assertNull(switched.theme.chatArtwork())
    }

    @Test fun existingThemesNeverAcquireStarryCoastArtwork() {
        CoastThemePreset.entries.filter { it != CoastThemePreset.StarryCoast }.forEach {
            assertNull(it.name, it.chatArtwork())
        }
    }

    @Test fun completeFramesFitAndLeaveReadingSpaceOnPhoneImeLandscapeAndTablet() {
        listOf(360f to 600f, 390f to 210f, 800f to 180f, 1024f to 1000f, 320f to 80f)
            .forEach { (width, height) ->
                val layout = chatArtworkLayout(width, height)
                assertTrue(layout.frameWidth > 0f)
                assertTrue(layout.frameWidth <= width)
                assertTrue(layout.frameWidth <= 600f)
                val fullFrameHeights = layout.frameWidth * (724f / 2172f + 771f / 2039f)
                assertTrue("Complete image bounds fit in $width x $height", fullFrameHeights < height)
                assertTrue(
                    "The foreground must leave a readable center with the keyboard open",
                    layout.topScrollClearance + layout.bottomScrollClearance <= height * .55f
                )
            }
    }
}
