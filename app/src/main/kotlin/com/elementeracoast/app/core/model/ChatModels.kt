package com.elementeracoast.app.core.model

enum class MessageRole { User, Assistant }

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String,
    val modelId: String? = null,
    val generationSource: String? = null,
    val liked: Boolean = false,
    val favorite: Boolean = false,
    val errorDetail: String? = null,
    val variantIndex: Int = 0,
    val variantCount: Int = 1,
    val createdAtLabel: String? = null,
    val furnitureRuns: List<FurnitureSummary> = emptyList()
)

sealed interface MessageAction {
    val messageId: Long

    data class Copy(override val messageId: Long) : MessageAction
    data class ToggleLike(override val messageId: Long) : MessageAction
    data class ToggleFavorite(override val messageId: Long) : MessageAction
    data class Regenerate(override val messageId: Long) : MessageAction
    data class Delete(override val messageId: Long) : MessageAction
    data class Edit(override val messageId: Long, val text: String) : MessageAction
}
