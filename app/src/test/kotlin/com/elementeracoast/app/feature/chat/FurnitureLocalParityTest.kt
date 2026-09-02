package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.local.MemoryLocalPersistence
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.feature.actionlog.ActionLogStore
import com.elementeracoast.app.feature.daily.DailyStore
import com.elementeracoast.app.feature.memory.LocalMemoryEntry
import com.elementeracoast.app.feature.memory.MemoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FurnitureLocalParityTest {
    @Test fun noTriggerProducesNoFurniture() {
        val p = MemoryLocalPersistence()
        val runner = LocalFurnitureOrchestrator(DailyStore(p), MemoryStore(p), ActionLogStore(p))
        assertTrue(runner.runForPrompt("普通聊天", RoomType.Main, "main-1", 10L).isEmpty())
    }

    @Test fun explicitLocalActionsProduceSafeFurnitureAndBoundActionIds() {
        val p = MemoryLocalPersistence()
        val daily = DailyStore(p)
        val memory = MemoryStore(p)
        memory.saveMemory(LocalMemoryEntry("", "海鸟与岸", "归返", "内容", "", "", listOf("关系"), "关系"))
        val log = ActionLogStore(p)
        val runs = LocalFurnitureOrchestrator(daily, memory, log)
            .runForPrompt("写日记，再搜索记忆 海鸟", RoomType.Main, "main-1", 88L)
        assertEquals(2, runs.size)
        assertTrue(runs.all { it.actionId.isNotBlank() })
        assertTrue(runs.any { it.actionKey == "daily.diary.write" })
        assertTrue(runs.any { it.actionKey == "memory.search" })
        assertTrue(log.records.value.all { it.assistantMessageId == 88L })
        assertTrue(log.records.value.all { it.inputSummary.length <= 360 && it.outputSummary.length <= 360 })
    }
}
