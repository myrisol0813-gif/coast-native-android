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
    val userBubbleHex: String = "",
    val accentHex: String = "",
    val activeRoomType: RoomType = RoomType.Main,
    val activeFeature: FeatureDestination? = null,
    val actionLogFocusIds: Set<String> = emptySet(),
    val conversations: List<ConversationSummary> = initialConversations(),
    val activeConversationId: String = "main-o3",
    val messages: List<ChatMessage> = emptyList(),
    val currentModel: String = "Free: North Mini",
    val models: List<String> = listOf("Free: North Mini", "GPT-5.6 Sol", "GPT-5.5 Thinking", "o3"),
    val isStreaming: Boolean = false,
    val streamingMessageId: Long? = null,
    val streamingVariantIndex: Int? = null,
    val showModelPicker: Boolean = false,
    val snackbarMessage: String? = null
)
