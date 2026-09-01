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
    val userBubble: String = "default",
    val accent: String = "orange",
    val activeRoomType: RoomType = RoomType.Main,
    val activeFeature: FeatureDestination? = null,
    val conversations: List<ConversationSummary> = emptyList(),
    val activeConversationId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val currentModel: String = DEFAULT_LOCAL_MODELS.first(),
    val models: List<String> = DEFAULT_LOCAL_MODELS,
    val isStreaming: Boolean = false,
    val showModelPicker: Boolean = false,
    val snackbarMessage: String? = null
)
