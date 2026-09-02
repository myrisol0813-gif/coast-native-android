package com.elementeracoast.app.feature

import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.feature.daily.dailyLandingItems
import com.elementeracoast.app.feature.memory.memoryLandingItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FeatureParityTest {
    @Test
    fun featureDestinationsMatchApp59WithoutRetiredSurfaces() {
        assertEquals(
            listOf(FeatureDestination.Memory, FeatureDestination.Daily, FeatureDestination.Wolf, FeatureDestination.ActionLog),
            FeatureDestination.entries
        )
        val labels = FeatureDestination.entries.joinToString(" ") { "${it.name} ${it.title} ${it.subtitle}" }
        listOf("Calendar", "今日一瞥", "一日总结", "相册", "Serpent Desk", "小蛇书桌").forEach { retired ->
            assertFalse(labels.contains(retired, ignoreCase = true))
        }
    }

    @Test fun dailyLandingKeepsApp59ThreeEntrances() = assertEquals(
        listOf("碳硅圈", "日记", "宠物系统"), dailyLandingItems().map { it.title }
    )

    @Test fun memoryLandingMatchesMemoryV2FourEntrances() = assertEquals(
        listOf("记忆库", "种子库", "世界书", "自定义指令"), memoryLandingItems().map { it.title }
    )
}
