package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureSummary
import com.elementeracoast.app.core.model.LocalChatState
import com.elementeracoast.app.core.model.MessageRole
import org.json.JSONArray
import org.json.JSONObject

internal object ChatTransferCodec {
    fun exportJson(state: LocalChatState, signature: String, currentModel: String): String {
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

    fun exportHtml(messages: List<ChatMessage>, signature: String): String {
        val rows = messages.joinToString("\n") { message ->
            val speaker = if (message.role == MessageRole.User) signature else "Myri"
            "<article class=\"m ${message.role.name.lowercase()}\"><b>${html(speaker)}</b><div>${html(message.text).replace("\n", "<br>")}</div></article>"
        }
        return """<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Elementera Native Chat Export</title><style>body{font-family:system-ui,sans-serif;line-height:1.65}.w{max-width:820px;margin:auto;padding:22px 14px}.m{margin:0 0 18px}.m b{display:block;color:#777}.m div{display:inline-block;max-width:88%;padding:10px 14px;border:1px solid #ddd;border-radius:18px}.user{text-align:right}.user div{background:#f1f1f1}</style><div class="w"><h1>Elementera Native Chat Export</h1>$rows</div>"""
    }

    fun importMessages(raw: String): List<ChatMessage> {
        val root = JSONObject(raw)
        val array = root.optJSONArray("messages") ?: throw IllegalArgumentException("messages not found")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(jsonToMessage(item, index.toLong() + 1L))
            }
        }
    }

    fun messageToJson(message: ChatMessage): JSONObject = JSONObject().apply {
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
                        run.items.forEach { item ->
                            put(JSONObject().put("kind", item.kind).put("title", item.title))
                        }
                    })
                })
            }
        })
    }

    fun jsonToMessage(item: JSONObject, fallbackId: Long): ChatMessage {
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
