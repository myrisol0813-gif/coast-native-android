package com.elementeracoast.app.feature.serpentdesk

import org.junit.Assert.assertEquals
import org.junit.Test

class SerpentDeskSurfaceTest {
    @Test
    fun deskExposesDevHandsBesideTheExistingActionLog() {
        val items = serpentDeskItems()
        assertEquals(2, items.size)
        assertEquals(
            listOf(SerpentDeskTool.DevHands, SerpentDeskTool.ActionLog),
            items.map { it.tool }
        )
        assertEquals(
            listOf("海岸施工台", "小蛇行动日志"),
            items.map { it.title }
        )
        assertEquals("GitHub · CI / APK · Notion · 小狼窝更新", items.first().subtitle)
        assertEquals("普通海岸工具调用 · 房间 · 脱敏摘要", items.last().subtitle)
    }
}
