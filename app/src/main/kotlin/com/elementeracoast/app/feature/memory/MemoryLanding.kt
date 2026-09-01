package com.elementeracoast.app.feature.memory

import androidx.compose.runtime.Composable
import com.elementeracoast.app.feature.shell.CoastLandingCards
import com.elementeracoast.app.feature.shell.LandingCardSpec

@Composable
fun MemoryLanding(onPlaceholder: (String) -> Unit) {
    CoastLandingCards(
        cards = memoryLandingItems(),
        onPlaceholder = onPlaceholder
    )
}

internal fun memoryLandingItems(): List<LandingCardSpec> = listOf(
    LandingCardSpec("记忆库", "已经确认的长期纸条"),
    LandingCardSpec("种子库", "暂时沉睡、以后可能发芽的内容"),
    LandingCardSpec("世界书", "海岸稳定世界设定"),
    LandingCardSpec("自定义指令", "当前长期回应约定")
)
