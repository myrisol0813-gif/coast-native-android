package com.elementeracoast.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoastThemePresetTest {
    @Test fun wardrobeContainsTwelveNamedPresetsWithDefaultFirst() {
        assertEquals(12, CoastThemePreset.entries.size)
        assertEquals(CoastThemePreset.CoastDefault, CoastThemePreset.entries.first())
        assertEquals(
            listOf("默认海岸", "深蓝旧金", "粉嫩 Myri", "白狼雪野", "火烧云", "极光夜航", "像素电子宠物", "苏式旧纸", "极简海雾", "美式卡通", "草地绿洲", "紫色梦潮"),
            CoastThemePreset.entries.map { it.label }
        )
    }

    @Test fun legacyModesMapIntoWardrobeWithoutCreatingSecondOwner() {
        assertEquals(CoastThemePreset.CoastDefault, CoastThemePreset.fromStored("Light"))
        assertEquals(CoastThemePreset.AuroraNight, CoastThemePreset.fromStored("Dark"))
        assertEquals(CoastThemePreset.DeepBlueGold, CoastThemePreset.fromStored("Gold"))
        assertEquals(CoastThemePreset.BlushMyri, CoastThemePreset.fromStored("BlushMyri"))
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
}
