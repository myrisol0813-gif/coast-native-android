package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.roomConversationTitle

/** Owns Native-only conversations and threads. Shell ViewModel coordinates, but does not store thread internals. */
class LocalChatStore(
    initialConversations: List<ConversationSummary>,
    initialConversationId: String,
    initialMessages: List<ChatMessage>
) {
    private val threads = linkedMapOf<String, List<ChatMessage>>()
    private var conversations = initialConversations
    private var nextMessageId = 100L
    private var nextConversationId = 10L

    init {
        conversations.forEach { threads[it.id] = emptyList() }
        threads[initialConversationId] = initialMessages
    }

    fun conversations(): List<ConversationSummary> = conversations
    fun messages(id: String): List<ChatMessage> = threads[id].orEmpty()
    fun nextMessageId(): Long = nextMessageId++

    fun create(roomType: RoomType, rawTitle: String): ConversationSummary {
        val record = newRecord(roomType, rawTitle)
        conversations = listOf(record) + conversations
        threads[record.id] = emptyList()
        return record
    }

    fun rename(id: String, rawTitle: String): ConversationSummary? {
        val target = conversations.firstOrNull { it.id == id } ?: return null
        val title = roomConversationTitle(target.roomType, rawTitle)
        if (title.isBlank()) return null
        val updated = target.copy(title = title)
        conversations = conversations.map { if (it.id == id) updated else it }
        return updated
    }

    fun delete(id: String): ConversationSummary? {
        val target = conversations.firstOrNull { it.id == id } ?: return null
        conversations = conversations.filterNot { it.id == id }
        threads.remove(id)
        return target
    }

    fun replaceConversations(value: List<ConversationSummary>) { conversations = value }

    fun mutate(conversationId: String, transform: (List<ChatMessage>) -> List<ChatMessage>): List<ChatMessage> {
        val updated = transform(threads[conversationId].orEmpty())
        threads[conversationId] = updated
        return updated
    }

    fun replaceMessages(conversationId: String, messages: List<ChatMessage>) {
        threads[conversationId] = messages
    }

    private fun newRecord(roomType: RoomType, rawTitle: String): ConversationSummary {
        val id = "${roomType.wireValue}-${nextConversationId++}"
        return ConversationSummary(id, roomConversationTitle(roomType, rawTitle), roomType)
    }
}
