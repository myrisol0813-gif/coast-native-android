package com.elementeracoast.app.core.model

/** Visible non-chat surfaces kept in the app-57 Native v1 shell. */
enum class FeatureDestination(val title: String, val subtitle: String) {
    Memory("轨迹 / 记忆", "记忆库、种子库与自定义指令"),
    Daily("海岸日报", "朋友圈与日记"),
    Wolf("Wolf Den", "小狼窝入口"),
    Desk("Serpent Desk", "小蛇书桌")
}
