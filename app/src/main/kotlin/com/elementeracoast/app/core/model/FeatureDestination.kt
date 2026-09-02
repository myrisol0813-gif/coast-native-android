package com.elementeracoast.app.core.model

/** Visible non-chat surfaces aligned to PWA app-59. */
enum class FeatureDestination(val title: String, val subtitle: String) {
    Memory("轨迹 / 记忆", "记忆库、种子库、世界书与自定义指令"),
    Daily("海岸日报", "碳硅圈、日记与宠物系统"),
    Wolf("Wolf Den / 小狼窝", "显示资料、外观、聊天与运行设置"),
    ActionLog("Serpent Action Log / 小蛇行动日志", "海岸家具 · 脱敏动作摘要 · 本地透明层")
}
