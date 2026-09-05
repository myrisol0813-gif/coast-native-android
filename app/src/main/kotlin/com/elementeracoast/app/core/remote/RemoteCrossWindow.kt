package com.elementeracoast.app.core.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RemoteCrossWindowLimits(
    @SerialName("default_turns") val defaultTurns: Int = 4,
    @SerialName("max_turns_per_source") val maxTurnsPerSource: Int = 20,
    @SerialName("max_total_turns") val maxTotalTurns: Int = 40,
    @SerialName("max_message_chars") val maxMessageChars: Int = 6000,
    @SerialName("max_total_chars") val maxTotalChars: Int = 24000
)

@Serializable
data class RemoteCrossWindowSource(
    @SerialName("conversation_id") val conversationId: String,
    val title: String = "",
    @SerialName("room_type") val roomType: String = "main",
    val source: String = "coast",
    @SerialName("source_window_id") val sourceWindowId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("message_count") val messageCount: Int = 0,
    @SerialName("turn_count") val turnCount: Int = 0,
    val readable: Boolean = false,
    @SerialName("disabled_reason") val disabledReason: String = ""
)

@Serializable
data class RemoteCrossWindowSourcesResponse(
    val ok: Boolean = false,
    val description: String = "",
    val limits: RemoteCrossWindowLimits = RemoteCrossWindowLimits(),
    val sources: List<RemoteCrossWindowSource> = emptyList()
)

@Serializable
data class RemoteCrossWindowSelection(
    @SerialName("conversation_id") val conversationId: String,
    val turns: Int
)

@Serializable
data class RemoteCrossWindowRequest(
    val mode: String = "off",
    val sources: List<RemoteCrossWindowSelection> = emptyList()
)
