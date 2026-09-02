package com.elementeracoast.app.feature.memory

import com.elementeracoast.app.core.local.LocalPersistence
import com.elementeracoast.app.core.local.LocalTextCodec
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryStore(private val persistence: LocalPersistence) {
    private val sequence = AtomicLong(1)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<MemoryState> = _state.asStateFlow()

    fun saveMemory(entry: LocalMemoryEntry): LocalMemoryEntry {
        val normalized = entry.copy(
            id = entry.id.ifBlank { id("memory") },
            title = entry.title.trim().ifBlank { "未命名记忆" }.take(120),
            lifeCore = entry.lifeCore.trim().take(1000),
            content = entry.content.trim().take(12000),
            usageHint = entry.usageHint.trim().take(1200),
            avoidHint = entry.avoidHint.trim().take(1200),
            tags = entry.tags.map(String::trim).filter(String::isNotBlank).distinct().take(16),
            category = entry.category.trim().ifBlank { "未分类" }.take(60),
            sourceModel = entry.sourceModel.trim().ifBlank { "手动整理" }.take(120),
            sourceWindow = entry.sourceWindow.trim().ifBlank { "本地" }.take(180),
            sourceDate = normalizeDate(entry.sourceDate)
        )
        update { state ->
            val exists = state.memories.any { it.id == normalized.id }
            state.copy(memories = if (exists) state.memories.map { if (it.id == normalized.id) normalized else it } else listOf(normalized) + state.memories)
        }
        return normalized
    }

    fun deleteMemory(id: String) = update { it.copy(memories = it.memories.filterNot { memory -> memory.id == id }) }

    fun searchMemories(query: String): List<LocalMemoryEntry> {
        val needle = query.trim()
        if (needle.isBlank()) return _state.value.memories
        return _state.value.memories.filter { entry ->
            listOf(
                entry.title,
                entry.lifeCore,
                entry.content,
                entry.usageHint,
                entry.avoidHint,
                entry.category,
                entry.tags.joinToString(" "),
                entry.sourceModel,
                entry.sourceWindow,
                entry.sourceDate
            ).any { it.contains(needle, ignoreCase = true) }
        }
    }

    fun saveSeed(seed: LocalSeed): LocalSeed {
        val normalized = seed.copy(
            id = seed.id.ifBlank { id("seed") },
            title = seed.title.trim().ifBlank { "未命名种子" }.take(120),
            content = seed.content.trim().take(8000),
            tags = seed.tags.map(String::trim).filter(String::isNotBlank).distinct().take(16),
            sourceModel = seed.sourceModel.trim().ifBlank { "手动整理" }.take(120),
            sourceWindow = seed.sourceWindow.trim().ifBlank { "本地" }.take(180),
            sourceDate = normalizeDate(seed.sourceDate)
        )
        update { state ->
            val exists = state.seeds.any { it.id == normalized.id }
            state.copy(seeds = if (exists) state.seeds.map { if (it.id == normalized.id) normalized else it } else listOf(normalized) + state.seeds)
        }
        return normalized
    }

    fun deleteSeed(id: String) = update { it.copy(seeds = it.seeds.filterNot { seed -> seed.id == id }) }

    fun searchSeeds(query: String): List<LocalSeed> {
        val needle = query.trim()
        if (needle.isBlank()) return _state.value.seeds
        return _state.value.seeds.filter { seed ->
            listOf(seed.title, seed.content, seed.tags.joinToString(" "), seed.sourceModel, seed.sourceWindow, seed.sourceDate)
                .any { it.contains(needle, true) }
        }
    }

    fun saveWorldbook(entry: LocalWorldbookEntry): LocalWorldbookEntry {
        val normalized = entry.copy(
            id = entry.id.ifBlank { id("worldbook") },
            term = entry.term.trim().ifBlank { "未命名词条" }.take(120),
            content = entry.content.trim().take(10000)
        )
        update { state ->
            val exists = state.worldbook.any { it.id == normalized.id }
            state.copy(worldbook = if (exists) state.worldbook.map { if (it.id == normalized.id) normalized else it } else listOf(normalized) + state.worldbook)
        }
        return normalized
    }

    fun deleteWorldbook(id: String) = update { it.copy(worldbook = it.worldbook.filterNot { entry -> entry.id == id }) }
    fun toggleWorldbook(id: String) = update { state ->
        state.copy(worldbook = state.worldbook.map { if (it.id == id) it.copy(enabled = !it.enabled) else it })
    }

    fun searchWorldbook(query: String): List<LocalWorldbookEntry> {
        val needle = query.trim()
        if (needle.isBlank()) return _state.value.worldbook
        return _state.value.worldbook.filter { it.term.contains(needle, true) || it.content.contains(needle, true) }
    }

    fun saveCustomInstructions(value: String) = update { it.copy(customInstructions = value.take(20000)) }
    fun clearCustomInstructions() = saveCustomInstructions("")

    private fun update(transform: (MemoryState) -> MemoryState) {
        _state.value = transform(_state.value)
        persist(_state.value)
    }

    private fun persist(state: MemoryState) {
        persistence.put(KEY_MEMORIES, state.memories.joinToString("\n") { entry ->
            LocalTextCodec.encodeFields(
                entry.id,
                entry.title,
                entry.lifeCore,
                entry.content,
                entry.usageHint,
                entry.avoidHint,
                entry.tags.joinToString("\u001f"),
                entry.category,
                entry.sourceModel,
                entry.sourceWindow,
                entry.sourceDate
            )
        })
        persistence.put(KEY_SEEDS, state.seeds.joinToString("\n") { seed ->
            LocalTextCodec.encodeFields(
                seed.id,
                seed.title,
                seed.content,
                seed.status.name,
                seed.tags.joinToString("\u001f"),
                seed.sourceModel,
                seed.sourceWindow,
                seed.sourceDate
            )
        })
        persistence.put(KEY_WORLDBOOK, state.worldbook.joinToString("\n") { entry ->
            LocalTextCodec.encodeFields(entry.id, entry.term, entry.content, entry.enabled.toString())
        })
        persistence.put(KEY_INSTRUCTIONS, state.customInstructions)
    }

    private fun load(): MemoryState = MemoryState(
        memories = persistence.get(KEY_MEMORIES).lineSequence().filter(String::isNotBlank).mapNotNull { line ->
            val f = LocalTextCodec.decodeFields(line)
            if (f.size < 8) null else LocalMemoryEntry(
                id = f[0],
                title = f[1],
                lifeCore = f[2],
                content = f[3],
                usageHint = f[4],
                avoidHint = f[5],
                tags = f[6].split('\u001f').filter(String::isNotBlank),
                category = f[7],
                sourceModel = f.getOrNull(8)?.ifBlank { "手动整理" } ?: "手动整理",
                sourceWindow = f.getOrNull(9)?.ifBlank { "本地" } ?: "本地",
                sourceDate = f.getOrNull(10)?.ifBlank { LocalDate.now().toString() } ?: LocalDate.now().toString()
            )
        }.toList(),
        seeds = persistence.get(KEY_SEEDS).lineSequence().filter(String::isNotBlank).mapNotNull { line ->
            val f = LocalTextCodec.decodeFields(line)
            if (f.size < 4) null else LocalSeed(
                id = f[0],
                title = f[1],
                content = f[2],
                status = runCatching { SeedStatus.valueOf(f[3]) }.getOrDefault(SeedStatus.Active),
                tags = f.getOrNull(4)?.split('\u001f')?.filter(String::isNotBlank).orEmpty(),
                sourceModel = f.getOrNull(5)?.ifBlank { "手动整理" } ?: "手动整理",
                sourceWindow = f.getOrNull(6)?.ifBlank { "本地" } ?: "本地",
                sourceDate = f.getOrNull(7)?.ifBlank { LocalDate.now().toString() } ?: LocalDate.now().toString()
            )
        }.toList(),
        worldbook = persistence.get(KEY_WORLDBOOK).lineSequence().filter(String::isNotBlank).mapNotNull { line ->
            val f = LocalTextCodec.decodeFields(line)
            if (f.size < 4) null else LocalWorldbookEntry(f[0], f[1], f[2], f[3].toBoolean())
        }.toList(),
        customInstructions = persistence.get(KEY_INSTRUCTIONS)
    )

    private fun normalizeDate(value: String): String {
        val clean = value.trim().replace('/', '-')
        return runCatching { LocalDate.parse(clean).toString() }.getOrDefault(LocalDate.now().toString())
    }

    private fun id(prefix: String) = "$prefix-${System.currentTimeMillis()}-${sequence.getAndIncrement()}"

    companion object {
        private const val KEY_MEMORIES = "memory.entries"
        private const val KEY_SEEDS = "memory.seeds"
        private const val KEY_WORLDBOOK = "memory.worldbook"
        private const val KEY_INSTRUCTIONS = "memory.customInstructions"
    }
}
