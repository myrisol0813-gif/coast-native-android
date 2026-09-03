package com.elementeracoast.app.feature.memory

data class MemoryEntry(
    val id: String = "",
    val entryType: String = "memory",
    val title: String = "",
    val lifeCore: String = "",
    val content: String = "",
    val usageHint: String = "",
    val avoidHint: String = "",
    val memoryLevel: String = "ordinary",
    val status: String = "active",
    val tags: List<String> = emptyList(),
    val sourceModel: String = "",
    val sourceWindow: String = "",
    val sourceDate: String = "",
    val tag: String = ""
)

data class MemoryPocket(
    val id: String,
    val conversationId: String,
    val title: String,
    val lifeCore: String,
    val content: String,
    val usageHint: String,
    val avoidHint: String,
    val sourceExcerpt: String,
    val status: String
)

data class WorldbookEntry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val keywords: List<String> = emptyList(),
    val useRegex: Boolean = false,
    val caseSensitive: Boolean = false,
    val constantActive: Boolean = false,
    val priority: Int = 0,
    val scanDepth: Int = 4,
    val enabled: Boolean = true,
    val scope: String = "owner",
    val visitorSafe: Boolean = false
)

data class CustomInstructions(
    val content: String = "",
    val status: String = "active",
    val updatedAt: String? = null,
    val updatedBy: String = "xiaohan",
    val source: String = "小寒手动编辑"
)

data class MemorySnapshot(
    val memories: List<MemoryEntry> = emptyList(),
    val seeds: List<MemoryEntry> = emptyList(),
    val pockets: List<MemoryPocket> = emptyList(),
    val pocketConversationId: String? = null,
    val worldbook: List<WorldbookEntry> = emptyList(),
    val customInstructions: CustomInstructions = CustomInstructions()
)

internal val canonicalMemoryTags = listOf(
    "关系",
    "历史锚点",
    "偏好",
    "人物档案",
    "海岸世界观",
    "工程技术"
)
