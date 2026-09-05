package com.elementeracoast.app.feature.dogtalk

import com.elementeracoast.app.core.model.CrossWindowLimits
import com.elementeracoast.app.core.model.CrossWindowMode
import com.elementeracoast.app.core.model.CrossWindowSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossWindowContractTest {
    @Test
    fun threeModesKeepStableWireValues() {
        assertEquals(listOf("off", "manual", "model_decides"), CrossWindowMode.entries.map { it.wireValue })
        assertEquals(listOf("关闭", "手动选择窗口", "让模型决定"), CrossWindowMode.entries.map { it.label })
    }

    @Test
    fun manualRequestContainsOnlyReadableCheckedSourcesWithoutLowTurnClamp() {
        val coast = source("coast-1", "普通窗口", "coast", readable = true)
        val rikka = source("rikka-1", "旧导入", "rikkahub", readable = true)
        val disabled = source("disabled-1", "当前窗口", "coast", readable = false)
        val state = CrossWindowUiState(
            mode = CrossWindowMode.Manual,
            sources = listOf(coast, rikka, disabled),
            selections = mapOf(
                coast.conversationId to CrossWindowSelectionUi(checked = true, turns = 50),
                rikka.conversationId to CrossWindowSelectionUi(checked = true, turns = 100),
                disabled.conversationId to CrossWindowSelectionUi(checked = true, turns = 999)
            ),
            limits = CrossWindowLimits(defaultTurns = 4, technicalMaxTurnsPerSource = 9999)
        )

        val request = state.request()

        assertEquals(CrossWindowMode.Manual, request.mode)
        assertEquals(listOf("coast-1", "rikka-1"), request.sources.map { it.conversationId })
        assertEquals(listOf(50, 100), request.sources.map { it.turns })
        assertTrue(state.sources.any { it.source == "rikkahub" })
    }

    @Test
    fun offAndModelDecidesDoNotPreloadSourceBodies() {
        assertTrue(CrossWindowUiState().request().sources.isEmpty())
        val deciding = CrossWindowUiState(mode = CrossWindowMode.ModelDecides).request()
        assertEquals(CrossWindowMode.ModelDecides, deciding.mode)
        assertTrue(deciding.sources.isEmpty())
    }

    private fun source(
        id: String,
        title: String,
        source: String,
        readable: Boolean
    ) = CrossWindowSource(
        conversationId = id,
        title = title,
        roomType = "main",
        source = source,
        sourceWindowId = if (source == "rikkahub") "import-window" else null,
        updatedAt = "2026-09-05T18:00:00Z",
        messageCount = 8,
        turnCount = 4,
        readable = readable,
        disabledReason = if (readable) "" else "当前窗口已由最近上下文提供"
    )
}
