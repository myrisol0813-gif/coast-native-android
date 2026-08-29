package com.elementeracoast.app.core.model

enum class MessageRole { User, Assistant }

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String
)

data class CoastShellState(
    val authenticated: Boolean = false,
    val password: String = "",
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 1L,
            role = MessageRole.Assistant,
            text = "This is a local shell PoC. No network request is being made. The long assistant message is intentionally rendered as natural page text instead of a large announcement card, so we can verify the basic chat body before the PWA reference pack defines the final Coast visual language."
        )
    ),
    val currentModel: String = "GPT-5.6 Sol",
    val models: List<String> = listOf("GPT-5.6 Sol", "o3", "Coast placeholder"),
    val isStreaming: Boolean = false,
    val showModelPicker: Boolean = false
)
