package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureSummary
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
        val updated = threads[id].orEmpty() + messages
        setThread(id, updated)
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

    fun currentMessage(messageId: Long): ChatMessage? = _state.value.messages.firstOrNull { it.id == messageId }

    fun conversationCount(): Int = _state.value.conversations.size

    fun messageCount(): Int = threads.values.sumOf { it.size }

    fun exportJson(signature: String, currentModel: String): String {
        val state = _state.value
        val conversation = state.conversations.first { it.id == state.activeConversationId }
        return JSONObject().apply {
            put("format", "elementera-native-chat-export")
            put("version", 26)
            put("display_profile", JSONObject().put("signature", signature))
            put("current_model", currentModel)
            put("conversation", JSONObject().apply {
                put("id", conversation.id)
                put("title", conversation.title)
                put("room_type", conversation.roomType.wireValue)
            })
            put("messages", JSONArray().apply {
                state.messages.forEach { put(messageToJson(it)) }
            })
        }.toString(2)
    }

    fun exportHtml(signature: String): String {
        val rows = _state.value.messages.joinToString("\n") { message ->
            val speaker = if (message.role == MessageRole.User) signature else "Myri"
            "<article class=\"m ${message.role.name.lowercase()}\"><b>${html(speaker)}</b><div>${html(message.text).replace("\n", "<br>")}</div></article>"
        }
        return """<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Elementera Native Chat Export</title><style>body{font-family:system-ui,sans-serif;line-height:1.65}.w{max-width:820px;margin:auto;padding:22px 14px}.m{margin:0 0 18px}.m b{display:block;color:#777}.m div{display:inline-block;max-width:88%;padding:10px 14px;border:1px solid #ddd;border-radius:18px}.user{text-align:right}.user div{background:#f1f1f1}</style><div class="w"><h1>Elementera Native Chat Export</h1>$rows</div>"""
    }

    fun importJson(raw: String): Int {
        val root = JSONObject(raw)
        val array = root.optJSONArray("messages") ?: throw IllegalArgumentException("messages not found")
        val imported = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(jsonToMessage(item, index.toLong() + 1L))
            }
        }
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
                        add(jsonToMessage(item, index.toLong() + 1L))
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
                        threads[conversation.id].orEmpty().forEach { put(messageToJson(it)) }
                    })
                }
            })
        }
        persistence.write(key, root.toString())
    }

    private fun messageToJson(message: ChatMessage): JSONObject = JSONObject().apply {
        put("id", message.id)
        put("role", message.role.name.lowercase())
        put("text", message.text)
        put("model_id", message.modelId.orEmpty())
        put("generation_source", message.generationSource.orEmpty())
        put("liked", message.liked)
        put("favorite", message.favorite)
        put("variant_index", message.variantIndex)
        put("variant_count", message.variantCount)
        put("furniture", JSONArray().apply {
            message.furnitureRuns.forEach { run ->
                put(JSONObject().apply {
                    put("action_id", run.actionId)
                    put("action_key", run.actionKey)
                    put("label", run.label)
                    put("status", run.status)
                    put("count", run.count)
                    put("error_type", run.errorType.orEmpty())
                    put("items", JSONArray().apply {
                        run.items.forEach { item -> put(JSONObject().put("kind", item.kind).put("title", item.title)) }
                    })
                })
            }
        })
    }

    private fun jsonToMessage(item: JSONObject, fallbackId: Long): ChatMessage {
        val role = if (item.optString("role").equals("user", true)) MessageRole.User else MessageRole.Assistant
        val furniture = buildList {
            val array = item.optJSONArray("furniture") ?: JSONArray()
            for (index in 0 until array.length()) {
                val run = array.optJSONObject(index) ?: continue
                val items = buildList {
                    val details = run.optJSONArray("items") ?: JSONArray()
                    for (detailIndex in 0 until details.length()) {
                        val detail = details.optJSONObject(detailIndex) ?: continue
                        add(FurnitureItem(detail.optString("kind"), detail.optString("title")))
                    }
                }
                add(
                    FurnitureSummary(
                        actionId = run.optString("action_id"),
                        actionKey = run.optString("action_key"),
                        label = run.optString("label"),
                        status = run.optString("status", "success"),
                        count = run.optInt("count", 1),
                        items = items,
                        errorType = run.optString("error_type").ifBlank { null }
                    )
                )
            }
        }
        return ChatMessage(
            id = item.optLong("id", fallbackId),
            role = role,
            text = item.optString("text"),
            modelId = item.optString("model_id").ifBlank { null },
            generationSource = item.optString("generation_source").ifBlank { null },
            liked = item.optBoolean("liked", false),
            favorite = item.optBoolean("favorite", false),
            variantIndex = item.optInt("variant_index", 0),
            variantCount = item.optInt("variant_count", 1).coerceAtLeast(1),
            furnitureRuns = furniture
        )
    }

    private fun html(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
