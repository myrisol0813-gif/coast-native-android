package com.elementeracoast.app.core.model

enum class CrossWindowMode(val wireValue: String, val label: String) {
    Off("off", "关闭"),
    Manual("manual", "手动选择窗口"),
    ModelDecides("model_decides", "让模型决定");

    companion object {
        fun fromWire(value: String): CrossWindowMode = entries.firstOrNull { it.wireValue == value } ?: Off
    }
}

data class CrossWindowLimits(
    val defaultTurns: Int,
    val maxTurnsPerSource: Int,
    val maxTotalTurns: Int,
    val maxMessageChars: Int,
    val maxTotalChars: Int
)

data class CrossWindowSource(
    val conversationId: String,
    val title: String,
    val roomType: String,
    val source: String,
    val sourceWindowId: String?,
    val updatedAt: String?,
    val messageCount: Int,
    val turnCount: Int,
    val readable: Boolean,
    val disabledReason: String
)

data class CrossWindowSelection(
    val conversationId: String,
    val turns: Int
)

data class CrossWindowRequest(
    val mode: CrossWindowMode = CrossWindowMode.Off,
    val sources: List<CrossWindowSelection> = emptyList()
)

data class CrossWindowSourceSnapshot(
    val description: String,
    val limits: CrossWindowLimits,
    val sources: List<CrossWindowSource>
)
