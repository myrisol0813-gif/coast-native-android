package com.elementeracoast.app.feature.serpentdesk

import org.junit.Assert.assertEquals
import org.junit.Test

class SerpentDeskSurfaceTest {
    @Test
    fun deskCurrentlyExposesActionLogAsItsOnlyTool() {
        val items = serpentDeskItems()
        assertEquals(1, items.size)
        assertEquals(SerpentDeskTool.ActionLog, items.single().tool)
        assertEquals("小蛇行动日志", items.single().title)
        assertEquals("工具调用成功 / 失败 · 房间 · 脱敏摘要", items.single().subtitle)
    }
}
