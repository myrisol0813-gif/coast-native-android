package com.elementeracoast.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastThemePresetTest {
    @Test fun wardrobeContainsFourteenNamedPresetsWithDefaultFirst() {
        assertEquals(14, CoastThemePreset.entries.size)
        assertEquals(CoastThemePreset.CoastDefault, CoastThemePreset.entries.first())
        assertEquals(
            listOf(
                "默认海岸",
                "深蓝旧金",
                "粉嫩 Myri",
                "白狼雪野",
                "雪地来信",
                "星夜海岸",
                "火烧云",
                "极光夜航",
                "像素电子宠物",
                "苏式旧纸",
                "极简海雾",
                "美式卡通",
                "草地绿洲",
                "紫色梦潮"
            ),
            CoastThemePreset.entries.map { it.label }
        )
    }

    @Test fun legacyModesMapIntoWardrobeWithoutCreatingSecondOwner() {
        assertEquals(CoastThemePreset.CoastDefault, CoastThemePreset.fromStored("Light"))
        assertEquals(CoastThemePreset.AuroraNight, CoastThemePreset.fromStored("Dark"))
        assertEquals(CoastThemePreset.DeepBlueGold, CoastThemePreset.fromStored("Gold"))
        assertEquals(CoastThemePreset.BlushMyri, CoastThemePreset.fromStored("BlushMyri"))
        assertEquals(CoastThemePreset.SnowLetter, CoastThemePreset.fromStored("SnowLetter"))
        assertEquals(CoastThemePreset.StarryCoast, CoastThemePreset.fromStored("StarryCoast"))
        assertEquals(CoastThemePreset.CoastDefault, CoastThemePreset.fromStored("unknown"))
    }

    @Test fun palettesExposeRequiredTokensAndAvoidFlatWhiteOrBlack() {
        CoastThemePreset.entries.forEach { preset ->
            val palette = preset.palette()
            assertNotEquals(palette.background, palette.primary)
            assertNotEquals(palette.card, palette.textPrimary)
            assertTrue(palette.textPrimary.alpha > .9f)
            assertTrue(palette.textSecondary.alpha > .9f)
            assertTrue(palette.border.alpha > .9f)
            assertTrue(palette.accent.alpha > .9f)
        }
    }

    @Test fun starryCoastKeepsReadableTextAndControlsOnItsDarkSurfaces() {
        val palette = CoastThemePreset.StarryCoast.palette()
        assertFalse(palette.isLight)
        assertNotEquals(CoastThemePreset.DeepBlueGold.palette(), palette)
        listOf(palette.background, palette.surface, palette.card).forEach { surface ->
            assertTrue("Primary text must remain readable", contrast(palette.textPrimary, surface) >= 4.5f)
            assertTrue("Secondary text must remain readable", contrast(palette.textSecondary, surface) >= 4.5f)
            assertTrue("Gold controls must remain distinguishable", contrast(palette.primary, surface) >= 3f)
        }
        val colors = palette.toColorScheme()
        assertTrue("Text on gold buttons must remain readable", contrast(colors.onPrimary, colors.primary) >= 4.5f)
    }

    private fun contrast(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (maxOf(firstLuminance, secondLuminance) + .05f) /
            (minOf(firstLuminance, secondLuminance) + .05f)
    }
}
