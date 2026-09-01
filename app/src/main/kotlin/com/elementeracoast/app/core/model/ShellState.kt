package com.elementeracoast.app.core.model

enum class CoastThemeMode(val label: String) {
    Light("浅色"),
    Dark("深色"),
    Gold("黑金");

    fun next(): CoastThemeMode = entries[(ordinal + 1) % entries.size]
}

data class CoastShellState(
    val authenticated: Boolean = false,
    val password: String = "",
    val theme: CoastThemeMode = CoastThemeMode.Light,
    val activeRoomType: RoomType = RoomType.Main,
    val activeFeature: FeatureDestination? = null,
    val conversations: List<ConversationSummary> = initialConversations(),
    val activeConversationId: String = "main-o3",
    val messages: List<ChatMessage> = initialMessages(),
    val currentModel: String = "Free: North Mini",
    val models: List<String> = listOf("Free: North Mini", "GPT-5.6 Sol", "GPT-5.5 Thinking", "o3"),
    val isStreaming: Boolean = false,
    val showModelPicker: Boolean = false,
    val snackbarMessage: String? = null
)

private fun initialMessages(): List<ChatMessage> = listOf(
    ChatMessage(
        id = 1L,
        role = MessageRole.Assistant,
        text = "我把那张还带着潮味的信纸在掌心轻轻抚平。Native v1 继续用同一副聊天身体承接主聊天、电波与灯塔；真实 Coast 水管仍留到后端接线轮。",
        modelId = "Native local",
        generationSource = "fixture"
    )
)
