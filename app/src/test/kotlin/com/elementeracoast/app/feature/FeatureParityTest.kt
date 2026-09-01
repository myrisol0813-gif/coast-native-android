package com.elementeracoast.app.feature

import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.feature.daily.dailyLandingItems
import com.elementeracoast.app.feature.memory.memoryLandingItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FeatureParityTest {
    @Test
    fun featureDestinationsContainNoRetiredCalendarSurface() {
        assertEquals(
            listOf(
                FeatureDestination.Memory,
                FeatureDestination.Daily,
                FeatureDestination.Wolf,
                FeatureDestination.Desk
            ),
            FeatureDestination.entries
        )
        assertFalse(FeatureDestination.entries.any { it.name.contains("Calendar", ignoreCase = true) })
    }

    @Test
    fun dailyLandingOnlyKeepsCurrentApp57Entries() {
        assertEquals(
            listOf("碳硅圈", "日记", "宠物系统"),
            dailyLandingItems().map { it.title }
        )
    }

    @Test
    fun memoryLandingMatchesMemoryV2FourEntrances() {
        assertEquals(
            listOf("记忆库", "种子库", "世界书", "自定义指令"),
            memoryLandingItems().map { it.title }
        )
    }
}
