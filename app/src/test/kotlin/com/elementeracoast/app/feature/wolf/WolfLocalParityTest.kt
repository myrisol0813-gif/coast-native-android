package com.elementeracoast.app.feature.wolf

import com.elementeracoast.app.core.local.MemoryLocalPersistence
import com.elementeracoast.app.ui.theme.CoastThemePreset
import com.elementeracoast.app.ui.theme.CoastFontMode
import com.elementeracoast.app.ui.theme.CoastPaperMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WolfLocalParityTest {
    @Test fun wolfDenKeepsItsExistingEntrancesAndAddsVersionUpdate() {
        assertEquals(
            listOf("个人资料", "外观", "账户", "聊天记录", "模型箱", "基本设置", "关于与诊断", "版本与更新"),
            WolfDestination.entries.map { it.title }
        )
    }

    @Test fun basicSettingsExposeExactlyElevenActiveFieldsAndNoOldRunControl() {
        assertEquals(11, BasicSettings.activeFieldNames.size)
        assertEquals(
            listOf(
                "recentTurns", "contextBudget", "outputLength", "maxOutputTokens", "creativity",
                "streamingEnabled", "soilBudget", "seedCooldownTurns", "worldbookEnabled",
                "worldbookLimit", "memoryLimit"
            ), BasicSettings.activeFieldNames
        )
        val names = BasicSettings.activeFieldNames.joinToString(" ")
        listOf("autoRefreshEveryTurns", "maxHandSeeds", "conversationSeedLimit", "globalSeedLimit", "conversationMemoryLimit", "globalMemoryLimit").forEach {
            assertFalse(names.contains(it))
        }
    }

    @Test fun profileAppearanceAndSettingsPersistLocally() {
        val persistence = MemoryLocalPersistence()
        val store = WolfStore(persistence)
        store.saveProfile("Kryo", "小寒")
        store.setTheme(CoastThemePreset.BlushMyri)
        store.setUserBubble("#f5e8ee")
        store.setAccent("#ec4899")
        store.setLocalFont("方正FW筑紫明朝 简 L", "/data/user/0/com.elementeracoast.app/files/local-fonts/reading-font.ttf")
        store.setPaperMode(CoastPaperMode.Smooth)
        store.updateBasic { it.copy(memoryLimit = 5, outputLength = "long") }
        val reloaded = WolfStore(persistence).state.value
        assertEquals("Kryo", reloaded.profile.nickname)
        assertEquals(CoastThemePreset.BlushMyri, reloaded.appearance.theme)
        assertEquals("#f5e8ee", reloaded.appearance.userBubbleHex)
        assertEquals(CoastFontMode.CustomLocal, reloaded.appearance.fontMode)
        assertEquals("方正FW筑紫明朝 简 L", reloaded.appearance.localFontName)
        assertEquals("/data/user/0/com.elementeracoast.app/files/local-fonts/reading-font.ttf", reloaded.appearance.localFontPath)
        assertEquals(CoastPaperMode.Smooth, reloaded.appearance.paperMode)
        assertEquals(5, reloaded.basic.memoryLimit)
        assertEquals("long", reloaded.basic.outputLength)
    }

    @Test fun localFontSlotFallsBackWhenPathIsMissing() {
        val persistence = MemoryLocalPersistence()
        persistence.put("wolf.fontMode", CoastFontMode.CustomLocal.name)
        assertEquals(CoastFontMode.Myraes, WolfStore(persistence).state.value.appearance.fontMode)
    }

    @Test fun legacyThreeModeThemeReadsIntoPresetWardrobeOnce() {
        val persistence = MemoryLocalPersistence()
        persistence.put("wolf.theme", "Gold")
        assertEquals(CoastThemePreset.DeepBlueGold, WolfStore(persistence).state.value.appearance.theme)
    }
}
