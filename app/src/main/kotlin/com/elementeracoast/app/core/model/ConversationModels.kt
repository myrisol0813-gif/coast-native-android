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
