package com.elementeracoast.app.core.model

enum class MessageRole { User, Assistant }

/**
 * Native local UI state only.
 *
 * This is intentionally separate from core/contract/CoastMessage and the future API variant
 * skeletons. These fields exist so the local-only Native shell can render and exercise the
 * current PWA message affordances before backend wiring.
 */
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
    val createdAtLabel: String? = null
)

/** Local-only message mutations. Clipboard ownership stays in the Compose UI layer. */
sealed interface MessageAction {
    val messageId: Long

    data class ToggleLike(override val messageId: Long) : MessageAction
    data class ToggleFavorite(override val messageId: Long) : MessageAction
    data class Regenerate(override val messageId: Long) : MessageAction
    data class Delete(override val messageId: Long) : MessageAction
    data class Edit(override val messageId: Long, val text: String) : MessageAction
}
