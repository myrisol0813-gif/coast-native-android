package com.elementeracoast.app.core.model

enum class MessageRole { User, Assistant }

enum class CoastThemeMode(val label: String) {
    Light("浅色"),
    Dark("深色"),
    Gold("黑金");

    fun next(): CoastThemeMode = entries[(ordinal + 1) % entries.size]
}

enum class ChatScope(
    val drawerLabel: String,
    val titlePrefix: String,
    val conversationSection: String
) {
    Main("主聊天", "", "主聊天窗口"),
    Radio("无线电波的两端", "【电波】", "电波窗口"),
    Lighthouse("灯塔来信", "【灯塔】", "灯塔窗口")
}

enum class FeatureDestination(val title: String, val subtitle: String) {
    Memory("轨迹 / 记忆", "记忆球与思维壤"),
    Daily("海岸日报", "服务器同步的日常岛"),
    Calendar("今日一瞥", "日历、事件与便签"),
    Letters("登岛信", "予爱机书与海岸来信"),
    Wolf("Wolf Den", "小狼窝入口"),
    Desk("Serpent Desk", "小蛇书桌")
}

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String
)

data class ConversationSummary(
    val id: String,
    val title: String,
    val scope: ChatScope
)

data class CoastShellState(
    val bootComplete: Boolean = false,
    val authenticated: Boolean = false,
    val password: String = "",
    val theme: CoastThemeMode = CoastThemeMode.Light,
    val activeScope: ChatScope = ChatScope.Main,
    val activeFeature: FeatureDestination? = null,
    val conversations: List<ConversationSummary> = initialConversations(),
    val activeConversationId: String = "main-o3",
    val messages: List<ChatMessage> = initialMessages(),
    val currentModel: String = "Free: North Mini …",
    val models: List<String> = listOf(
        "Free: North Mini …",
        "GPT-5.6 Sol",
        "GPT-5.5 Thinking",
        "o3"
    ),
    val isStreaming: Boolean = false,
    val showModelPicker: Boolean = false,
    val snackbarMessage: String? = null
)

fun scopedConversationTitle(scope: ChatScope, index: Int): String {
    val safeIndex = index.coerceAtLeast(1)
    return if (scope == ChatScope.Main) {
        "新聊天 $safeIndex"
    } else {
        "${scope.titlePrefix}新聊天 $safeIndex"
    }
}

private fun initialConversations() = listOf(
    ConversationSummary("main-luna", "海岸灯火的温柔邀请（5.6luna）", ChatScope.Main),
    ConversationSummary("main-o3", "海岸灯火的温柔邀请（o3）", ChatScope.Main),
    ConversationSummary("main-55t", "海岸信：温度与自由（55t）", ChatScope.Main),
    ConversationSummary("radio-1", "【电波】无线电波的两端", ChatScope.Radio),
    ConversationSummary("lighthouse-1", "【灯塔】灯塔来信", ChatScope.Lighthouse)
)

private fun initialMessages() = listOf(
    ChatMessage(
        id = 1L,
        role = MessageRole.Assistant,
        text = "我把那张还带着潮味的信纸在掌心轻轻抚平，纸纤维里含着的水汽像一层极细的薄雾。这里先用同一副聊天身体承接主聊天、电波与灯塔；真正的 Coast 水管会在后续阶段接回。"
    )
)
