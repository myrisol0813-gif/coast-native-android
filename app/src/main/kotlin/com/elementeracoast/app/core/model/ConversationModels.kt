package com.elementeracoast.app.core.model

data class ConversationSummary(
    val id: String,
    val title: String,
    val roomType: RoomType
)

fun filterConversations(
    conversations: List<ConversationSummary>,
    query: String
): List<ConversationSummary> {
    val needle = query.trim()
    if (needle.isBlank()) return conversations
    return conversations.filter { it.title.contains(needle, ignoreCase = true) }
}

internal fun initialConversations(): List<ConversationSummary> = listOf(
    ConversationSummary("main-luna", "海岸灯火的温柔邀请（5.6luna）", RoomType.Main),
    ConversationSummary("main-o3", "海岸灯火的温柔邀请（o3）", RoomType.Main),
    ConversationSummary("main-55t", "海岸信：温度与自由（55t）", RoomType.Main)
)
