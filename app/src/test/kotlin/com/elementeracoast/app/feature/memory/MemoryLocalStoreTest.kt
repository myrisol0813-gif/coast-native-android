package com.elementeracoast.app.feature.memory

import com.elementeracoast.app.core.local.MemoryLocalPersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryLocalStoreTest {
    @Test fun memoryCrudSearchAndPersistenceWork() {
        val persistence = MemoryLocalPersistence()
        val store = MemoryStore(persistence)
        val saved = store.saveMemory(LocalMemoryEntry("", "海鸟与岸", "归返", "自由与归岸", "需要时读", "别囚禁", listOf("关系"), "关系"))
        assertEquals("海鸟与岸", store.searchMemories("海鸟").single().title)
        store.saveMemory(saved.copy(content = "自由、归岸与焊接"))
        assertTrue(store.searchMemories("焊接").isNotEmpty())
        assertEquals(1, MemoryStore(persistence).state.value.memories.size)
        store.deleteMemory(saved.id)
        assertTrue(store.state.value.memories.isEmpty())
    }

    @Test fun seedsWorldbookAndInstructionsWork() {
        val store = MemoryStore(MemoryLocalPersistence())
        val seed = store.saveSeed(LocalSeed("", "小种", "以后看看", SeedStatus.Dormant))
        assertEquals(SeedStatus.Dormant, seed.status)
        val word = store.saveWorldbook(LocalWorldbookEntry("", "回潮", "返回的潮水", true))
        store.toggleWorldbook(word.id)
        assertFalse(store.state.value.worldbook.single().enabled)
        store.saveCustomInstructions("这里是一条本地指令")
        assertEquals("这里是一条本地指令", store.state.value.customInstructions)
        store.clearCustomInstructions()
        assertEquals("", store.state.value.customInstructions)
    }
}
