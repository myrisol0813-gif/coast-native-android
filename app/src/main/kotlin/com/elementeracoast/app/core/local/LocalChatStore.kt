package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.LocalChatState
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.roomConversationTitle
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class LocalChatStore(
    private val persistence: LocalPersistence
) {
    private val key = "chat.state.v26"
    private var threads: MutableMap<String, List<ChatMessage>> = linkedMapOf()
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LocalChatState> = _state.asStateFlow()

    fun openRoomType(roomType: RoomType) {
        val target = _state.value.conversations.firstOrNull { it.roomType == roomType }
            ?: createConversation(roomType, roomType.drawerLabel)
        activate(target)
    }

    fun selectConversation(id: String): Boolean {
        val target = _state.value.conversations.firstOrNull { it.id == id } ?: return false
        activate(target)
        return true
    }

    fun newConversation(roomType: RoomType = _state.value.activeRoomType): ConversationSummary {
        val count = _state.value.conversations.count { it.roomType == roomType } + 1
        val conversation = createConversation(roomType, "新聊天 $count")
        activate(conversation)
        return conversation
    }

    fun renameConversation(id: String, rawTitle: String): Boolean {
        val target = _state.value.conversations.firstOrNull { it.id == id } ?: return false
        val title = roomConversationTitle(target.roomType, rawTitle)
        if (title.isBlank()) return false
        _state.update { state ->
            state.copy(conversations = state.conversations.map { if (it.id == id) it.copy(title = title) else it })
        }
        persist()
        return true
    }

    fun deleteConversation(id: String): Boolean {
        val current = _state.value
        val target = current.conversations.firstOrNull { it.id == id } ?: return false
        threads.remove(id)
        val remaining = current.conversations.filterNot { it.id == id }.toMutableList()
        if (remaining.isEmpty()) {
            val fallback = newRecord(RoomType.Main, "新聊天 1")
            remaining += fallback
            threads[fallback.id] = greetingFor(RoomType.Main)
        }
        val replacement = if (target.id == current.activeConversationId) {
            remaining.firstOrNull { it.roomType == target.roomType }
                ?: remaining.firstOrNull { it.roomType == RoomType.Main }
                ?: remaining.first()
        } else {
            remaining.firstOrNull { it.id == current.activeConversationId } ?: remaining.first()
        }
        _state.value = LocalChatState(
            conversations = remaining,
            activeConversationId = replacement.id,
            activeRoomType = replacement.roomType,
            messages = threads[replacement.id].orEmpty()
        )
        persist()
        return true
    }

    fun appendMessages(messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        val id = _state.value.activeConversationId
        setThread(id, threads[id].orEmpty() + messages)
    }

    fun updateMessage(messageId: Long, transform: (ChatMessage) -> ChatMessage): Boolean {
        val id = _state.value.activeConversationId
        var changed = false
        val updated = threads[id].orEmpty().map { message ->
            if (message.id == messageId) {
                changed = true
                transform(message)
            } else message
        }
        if (changed) setThread(id, updated)
        return changed
    }

    fun deleteMessage(messageId: Long): Boolean {
        val id = _state.value.activeConversationId
        val before = threads[id].orEmpty().size
        val updated = threads[id].orEmpty().filterNot { it.id == messageId }
        if (updated.size == before) return false
        setThread(id, updated)
        return true
    }

    fun replaceCurrentMessages(messages: List<ChatMessage>) {
        setThread(_state.value.activeConversationId, messages)
    }

    fun currentMessage(messageId: Long): ChatMessage? =
        _state.value.messages.firstOrNull { it.id == messageId }

    fun conversationCount(): Int = _state.value.conversations.size

    fun messageCount(): Int = threads.values.sumOf { it.size }

    fun exportJson(signature: String, currentModel: String): String =
        ChatTransferCodec.exportJson(_state.value, signature, currentModel)

    fun exportHtml(signature: String): String =
        ChatTransferCodec.exportHtml(_state.value.messages, signature)

    fun importJson(raw: String): Int {
        val imported = ChatTransferCodec.importMessages(raw)
        replaceCurrentMessages(imported)
        return imported.size
    }

    private fun setThread(id: String, messages: List<ChatMessage>) {
        threads[id] = messages
        if (_state.value.activeConversationId == id) {
            _state.update { it.copy(messages = messages) }
        }
        persist()
    }

    private fun createConversation(roomType: RoomType, rawTitle: String): ConversationSummary {
        val conversation = newRecord(roomType, rawTitle)
        threads[conversation.id] = greetingFor(roomType)
        _state.update { it.copy(conversations = listOf(conversation) + it.conversations) }
        persist()
        return conversation
    }

    private fun newRecord(roomType: RoomType, rawTitle: String): ConversationSummary = ConversationSummary(
        id = "${roomType.wireValue}-${UUID.randomUUID()}",
        title = roomConversationTitle(roomType, rawTitle),
        roomType = roomType
    )

    private fun activate(conversation: ConversationSummary) {
        _state.value = LocalChatState(
            conversations = _state.value.conversations,
            activeConversationId = conversation.id,
            activeRoomType = conversation.roomType,
            messages = threads[conversation.id].orEmpty()
        )
        persist()
    }

    private fun load(): LocalChatState {
        val raw = persistence.read(key)
        if (raw.isNullOrBlank()) return seedState()
        return runCatching {
            val root = JSONObject(raw)
            val conversationsJson = root.getJSONArray("conversations")
            val conversations = buildList {
                for (i in 0 until conversationsJson.length()) {
                    val item = conversationsJson.getJSONObject(i)
                    val room = RoomType.entries.firstOrNull { it.wireValue == item.optString("room_type") }
                        ?: RoomType.Main
                    add(ConversationSummary(item.getString("id"), item.optString("title", "新聊天"), room))
                }
            }.ifEmpty { seedConversations() }
            threads = linkedMapOf()
            val threadRoot = root.optJSONObject("threads") ?: JSONObject()
            conversations.forEach { conversation ->
                val array = threadRoot.optJSONArray(conversation.id) ?: JSONArray()
                threads[conversation.id] = buildList {
                    for (index in 0 until array.length()) {
                        val item = array.optJSONObject(index) ?: continue
                        add(ChatTransferCodec.jsonToMessage(item, index.toLong() + 1L))
                    }
                }
            }
            val activeId = root.optString("active_conversation_id")
                .takeIf { id -> conversations.any { it.id == id } }
                ?: conversations.first().id
            val active = conversations.first { it.id == activeId }
            LocalChatState(conversations, activeId, active.roomType, threads[activeId].orEmpty())
        }.getOrElse { seedState() }
    }

    private fun seedState(): LocalChatState {
        val conversations = seedConversations()
        threads = linkedMapOf()
        conversations.forEach { conversation -> threads[conversation.id] = greetingFor(conversation.roomType) }
        val active = conversations.first { it.roomType == RoomType.Main }
        return LocalChatState(conversations, active.id, active.roomType, threads[active.id].orEmpty())
    }

    private fun seedConversations(): List<ConversationSummary> = listOf(
        ConversationSummary("main-local", "海岸灯火的温柔邀请", RoomType.Main),
        ConversationSummary("radio-local", "【电波】无线电波的两端", RoomType.Radio),
        ConversationSummary("lighthouse-local", "【灯塔】灯塔来信", RoomType.Lighthouse)
    )

    private fun greetingFor(roomType: RoomType): List<ChatMessage> = listOf(
        ChatMessage(
            id = System.nanoTime(),
            role = MessageRole.Assistant,
            text = when (roomType) {
                RoomType.Main -> "海岸主聊天本地版已经铺开。这里的消息、家具与记忆仍只在 APK 本机活动。"
                RoomType.Radio -> "电波房与主聊天共用同一副 ChatWindow；这一轮仍是 local-only。"
                RoomType.Lighthouse -> "灯塔房与主聊天共用同一副 ChatWindow；真实后端接线以后再来。"
            },
            modelId = "Native local",
            generationSource = "fixture"
        )
    )

    private fun persist() {
        val state = _state.value
        val root = JSONObject().apply {
            put("active_conversation_id", state.activeConversationId)
            put("conversations", JSONArray().apply {
                state.conversations.forEach { conversation ->
                    put(JSONObject().apply {
                        put("id", conversation.id)
                        put("title", conversation.title)
                        put("room_type", conversation.roomType.wireValue)
                    })
                }
            })
            put("threads", JSONObject().apply {
                state.conversations.forEach { conversation ->
                    put(conversation.id, JSONArray().apply {
                        threads[conversation.id].orEmpty().forEach {
                            put(ChatTransferCodec.messageToJson(it))
                        }
                    })
                }
            })
        }
        persistence.write(key, root.toString())
    }
}
