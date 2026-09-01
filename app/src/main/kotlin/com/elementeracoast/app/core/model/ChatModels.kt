package com.elementeracoast.app.core.model

enum class MessageRole { User, Assistant }

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String
)
