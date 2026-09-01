package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalDailyStore
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.local.LocalPersistence
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureSummary
import com.elementeracoast.app.core.model.RoomType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalChatRuntimeTest {
    @Test
    fun furnitureIsHiddenWhenThereAreNoRuns() {
        assertNull(furnitureHeadline(emptyList()))
        assertTrue(furnitureActionIds(emptyList()).isEmpty())
        assertTrue(furnitureSafeSummaryLines(emptyList()).isEmpty())
    }

    @Test
    fun furnitureShowsCountSafeDetailsAndExactActionIds() {
        val runs = listOf(
            FurnitureSummary(
                actionId = "a-memory",
                actionKey = "memory.search",
                label = "搜索了记忆",
                count = 3,
                items = listOf(FurnitureItem("关系", "海鸟与岸"))
            ),
            FurnitureSummary(
                actionId = "a-diary",
                actionKey = "daily.create_diary",
                label = "写了一篇日记"
            )
        )
        assertEquals("小蛇摆弄了 2 件家具", furnitureHeadline(runs))
        assertEquals(listOf("a-memory", "a-diary"), furnitureActionIds(runs))
        assertEquals(
            listOf("✓ 搜索了记忆：3 条", "· 关系｜海鸟与岸", "✓ 写了一篇日记"),
            furnitureSafeSummaryLines(runs)
        )
    }

    @Test
    fun fakeChatActionsWriteDailyMemoryAndActionLogThroughFeatureOwner() {
        val db = RuntimeFakePersistence()
        val daily = LocalDailyStore(db, now = { 100L })
        val memory = LocalMemoryStore(db)
        val log = LocalActionLogStore(db, now = { 200L })
        val preferences = LocalPreferencesStore(db)
        memory.addMemory("海鸟与岸", "核心", "归返关系", "使用", "避免", listOf("关系"))
        val runtime = LocalChatRuntime(daily, memory, log, preferences)

        val runs = buildList {
            addAll(runtime.performLocalActions("写碳硅圈：潮声", RoomType.Main, "main-local"))
            addAll(runtime.performLocalActions("写日记：今天很好", RoomType.Main, "main-local"))
            addAll(runtime.performLocalActions("搜索记忆：海鸟", RoomType.Main, "main-local"))
        }

        assertEquals(1, daily.state.value.moments.size)
        assertEquals(1, daily.state.value.diaries.size)
        assertEquals(3, runs.size)
        assertEquals(3, log.state.value.records.size)
        assertTrue(runs.any { it.actionKey == "memory.search" && it.items.any { item -> item.title == "海鸟与岸" } })
    }
}

private class RuntimeFakePersistence : LocalPersistence {
    private val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}
