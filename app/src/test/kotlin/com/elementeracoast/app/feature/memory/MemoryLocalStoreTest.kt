package com.elementeracoast.app.feature.memory

import com.elementeracoast.app.core.local.MemoryLocalPersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryLocalStoreTest {
    @Test fun memoryCrudSearchSourceFacetsAndPersistenceWork() {
        val persistence = MemoryLocalPersistence()
        val store = MemoryStore(persistence)
        val saved = store.saveMemory(
            LocalMemoryEntry(
                id = "",
                title = "海鸟与岸",
                lifeCore = "归返",
                content = "自由与归岸",
                usageHint = "需要时读",
                avoidHint = "别囚禁",
                tags = listOf("关系"),
                category = "关系",
                sourceModel = "GPT-5.6 Sol",
                sourceWindow = "主聊天",
                sourceDate = "2026/09/02"
            )
        )
        assertEquals("海鸟与岸", store.searchMemories("海鸟").single().title)
        assertEquals("2026-09-02", saved.sourceDate)
        assertTrue(store.searchMemories("GPT-5.6 Sol").isNotEmpty())
        assertTrue(store.searchMemories("主聊天").isNotEmpty())
        store.saveMemory(saved.copy(content = "自由、归岸与焊接"))
        assertTrue(store.searchMemories("焊接").isNotEmpty())

        val reloaded = MemoryStore(persistence).state.value.memories.single()
        assertEquals("GPT-5.6 Sol", reloaded.sourceModel)
        assertEquals("主聊天", reloaded.sourceWindow)
        assertEquals("2026-09-02", reloaded.sourceDate)

        store.deleteMemory(saved.id)
        assertTrue(store.state.value.memories.isEmpty())
    }

    @Test fun seedsWorldbookAndInstructionsWork() {
        val persistence = MemoryLocalPersistence()
        val store = MemoryStore(persistence)
        val seed = store.saveSeed(
            LocalSeed(
                id = "",
                title = "小种",
                content = "以后看看",
                status = SeedStatus.Dormant,
                tags = listOf("偏好"),
                sourceModel = "o3",
                sourceWindow = "灯塔来信",
                sourceDate = "2026-09-01"
            )
        )
        assertEquals(SeedStatus.Dormant, seed.status)
        assertTrue(store.searchSeeds("灯塔来信").isNotEmpty())
        val reloadedSeed = MemoryStore(persistence).state.value.seeds.single()
        assertEquals(listOf("偏好"), reloadedSeed.tags)
        assertEquals("o3", reloadedSeed.sourceModel)

        val word = store.saveWorldbook(LocalWorldbookEntry("", "回潮", "返回的潮水", true))
        store.toggleWorldbook(word.id)
        assertFalse(store.state.value.worldbook.single().enabled)
        store.saveCustomInstructions("这里是一条本地指令")
        assertEquals("这里是一条本地指令", store.state.value.customInstructions)
        store.clearCustomInstructions()
        assertEquals("", store.state.value.customInstructions)
    }
}
