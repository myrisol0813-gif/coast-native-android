package com.elementeracoast.app.core.model

data class LocalChatState(
    val conversations: List<ConversationSummary>,
    val activeConversationId: String,
    val activeRoomType: RoomType,
    val messages: List<ChatMessage>
)
