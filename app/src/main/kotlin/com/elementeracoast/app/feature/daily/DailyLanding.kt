package com.elementeracoast.app.feature.daily

import androidx.compose.runtime.Composable
import com.elementeracoast.app.feature.shell.CoastLandingCards
import com.elementeracoast.app.feature.shell.LandingCardSpec

@Composable
fun DailyLanding(onPlaceholder: (String) -> Unit) {
    CoastLandingCards(
        cards = dailyLandingItems(),
        onPlaceholder = onPlaceholder
    )
}

internal fun dailyLandingItems(): List<LandingCardSpec> = listOf(
    LandingCardSpec("碳硅圈", "海岸内部朋友圈"),
    LandingCardSpec("日记", "留下今天的纸页"),
    LandingCardSpec("宠物系统", "还在准备休憩箱")
)
