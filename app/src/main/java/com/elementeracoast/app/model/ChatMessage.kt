package com.elementeracoast.app.model

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val modelId: String? = null,
    val totalTokens: Int? = null,
) {
    enum class Role { USER, ASSISTANT }
}
