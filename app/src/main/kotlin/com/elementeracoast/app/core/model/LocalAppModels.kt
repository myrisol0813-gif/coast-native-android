package com.elementeracoast.app.core.model

data class LocalProfile(
    val nickname: String = "小寒",
    val signature: String = "小寒",
    val userBubble: String = "default",
    val accent: String = "orange"
)

data class RunControlSettings(
    val recentTurns: Int = 8,
    val comfortTokens: Int = 6000,
    val outputLength: String = "auto",
    val maxOutputTokens: Int = 8000,
    val expression: String = "balanced",
    val streamingEnabled: Boolean = false,
    val soilBudget: Int = 1800,
    val seedCooldownTurns: Int = 2,
    val worldbookEnabled: Boolean = true,
    val worldbookLimit: Int = 6,
    val memoryLimit: Int = 8
)

data class LocalPreferencesState(
    val theme: CoastThemeMode = CoastThemeMode.Light,
    val profile: LocalProfile = LocalProfile(),
    val runControl: RunControlSettings = RunControlSettings(),
    val currentModel: String = DEFAULT_LOCAL_MODELS.first(),
    val models: List<String> = DEFAULT_LOCAL_MODELS
)

val DEFAULT_LOCAL_MODELS = listOf(
    "Free: North Mini",
    "GPT-5.6 Sol",
    "GPT-5.5 Thinking",
    "o3"
)

data class FurnitureItem(
    val kind: String,
    val title: String
)

data class FurnitureSummary(
    val actionId: String,
    val actionKey: String,
    val label: String,
    val status: String = "success",
    val count: Int = 1,
    val items: List<FurnitureItem> = emptyList(),
    val errorType: String? = null
)

data class ActionLogRecord(
    val actionId: String,
    val actionKey: String,
    val label: String,
    val status: String,
    val roomType: RoomType,
    val conversationId: String,
    val createdAt: Long,
    val finishedAt: Long,
    val inputSummary: String,
    val outputSummary: String,
    val errorMessage: String? = null
)

data class ActionLogState(
    val records: List<ActionLogRecord> = emptyList(),
    val statusFilter: String = "",
    val actionFilter: String = "",
    val conversationFilter: String = "",
    val focusedActionIds: Set<String> = emptySet()
)

data class DailyProfile(
    val avatarUri: String = "",
    val coverUri: String = ""
)

data class DailyMoment(
    val id: String,
    val content: String,
    val liked: Boolean = false,
    val comments: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long = createdAt
)

data class DiaryEntry(
    val id: String,
    val date: String,
    val weather: String,
    val mood: String,
    val tags: List<String>,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt
)

enum class PetStatus(val label: String) {
    Resting("休息中"),
    Active("活跃"),
    Sleepy("困倦")
}

data class DailyState(
    val moments: List<DailyMoment> = emptyList(),
    val diaries: List<DiaryEntry> = emptyList(),
    val profile: DailyProfile = DailyProfile(),
    val petStatus: PetStatus = PetStatus.Resting,
    val petNote: String = "未来接全局状态"
)

data class MemoryRecord(
    val id: String,
    val title: String,
    val lifeCore: String,
    val content: String,
    val usageHint: String,
    val avoidHint: String,
    val tags: List<String>
)

enum class SeedStatus { Active, Dormant }

data class SeedRecord(
    val id: String,
    val title: String,
    val content: String,
    val status: SeedStatus = SeedStatus.Active
)

data class WorldbookRecord(
    val id: String,
    val title: String,
    val content: String,
    val enabled: Boolean = true
)

data class MemoryState(
    val memories: List<MemoryRecord> = emptyList(),
    val seeds: List<SeedRecord> = emptyList(),
    val worldbook: List<WorldbookRecord> = emptyList(),
    val customInstructions: String = ""
)
