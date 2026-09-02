package com.elementeracoast.app.feature.memory

data class LocalMemoryEntry(
    val id: String,
    val title: String,
    val lifeCore: String,
    val content: String,
    val usageHint: String,
    val avoidHint: String,
    val tags: List<String>,
    val category: String = "未分类"
)

enum class SeedStatus { Active, Dormant }

data class LocalSeed(
    val id: String,
    val title: String,
    val content: String,
    val status: SeedStatus = SeedStatus.Active
)

data class LocalWorldbookEntry(
    val id: String,
    val term: String,
    val content: String,
    val enabled: Boolean = true
)

data class MemoryState(
    val memories: List<LocalMemoryEntry> = emptyList(),
    val seeds: List<LocalSeed> = emptyList(),
    val worldbook: List<LocalWorldbookEntry> = emptyList(),
    val customInstructions: String = ""
)
