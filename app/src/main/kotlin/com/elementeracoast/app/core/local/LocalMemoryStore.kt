package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.MemoryRecord
import com.elementeracoast.app.core.model.MemoryState
import com.elementeracoast.app.core.model.SeedRecord
import com.elementeracoast.app.core.model.SeedStatus
import com.elementeracoast.app.core.model.WorldbookRecord
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalMemoryStore(
    private val persistence: LocalPersistence
) {
    private val memoryKey = "memory.records.v26"
    private val seedKey = "memory.seeds.v26"
    private val worldbookKey = "memory.worldbook.v26"
    private val instructionKey = "memory.instructions.v26"

    private val _state = MutableStateFlow(load())
    val state: StateFlow<MemoryState> = _state.asStateFlow()

    fun addMemory(
        title: String,
        lifeCore: String,
        content: String,
        usageHint: String,
        avoidHint: String,
        tags: List<String>
    ): MemoryRecord? {
        val cleanTitle = title.trim().take(120)
        val cleanContent = content.trim().take(12000)
        if (cleanTitle.isBlank() || cleanContent.isBlank()) return null
        val record = MemoryRecord(
            id = "memory-${UUID.randomUUID()}",
            title = cleanTitle,
            lifeCore = lifeCore.trim().take(1000),
            content = cleanContent,
            usageHint = usageHint.trim().take(2000),
            avoidHint = avoidHint.trim().take(2000),
            tags = tags.map(String::trim).filter(String::isNotBlank).take(20)
        )
        _state.update { it.copy(memories = listOf(record) + it.memories) }
        persistMemories()
        return record
    }

    fun editMemory(record: MemoryRecord): Boolean {
        if (record.title.isBlank() || record.content.isBlank()) return false
        var changed = false
        _state.update { state ->
            state.copy(memories = state.memories.map { current ->
                if (current.id == record.id) {
                    changed = true
                    record.copy(
                        title = record.title.trim().take(120),
                        lifeCore = record.lifeCore.trim().take(1000),
                        content = record.content.trim().take(12000),
                        usageHint = record.usageHint.trim().take(2000),
                        avoidHint = record.avoidHint.trim().take(2000),
                        tags = record.tags.map(String::trim).filter(String::isNotBlank).take(20)
                    )
                } else current
            })
        }
        if (changed) persistMemories()
        return changed
    }

    fun deleteMemory(id: String): Boolean {
        val before = _state.value.memories.size
        _state.update { it.copy(memories = it.memories.filterNot { record -> record.id == id }) }
        val changed = before != _state.value.memories.size
        if (changed) persistMemories()
        return changed
    }

    fun searchMemories(query: String): List<MemoryRecord> {
        val q = query.trim()
        if (q.isBlank()) return _state.value.memories
        return _state.value.memories.filter { memory ->
            listOf(memory.title, memory.lifeCore, memory.content, memory.usageHint, memory.avoidHint)
                .any { it.contains(q, ignoreCase = true) } ||
                memory.tags.any { it.contains(q, ignoreCase = true) }
        }
    }

    fun addSeed(title: String, content: String, status: SeedStatus = SeedStatus.Active): SeedRecord? {
        val cleanTitle = title.trim().take(120)
        val cleanContent = content.trim().take(8000)
        if (cleanTitle.isBlank() || cleanContent.isBlank()) return null
        val seed = SeedRecord("seed-${UUID.randomUUID()}", cleanTitle, cleanContent, status)
        _state.update { it.copy(seeds = listOf(seed) + it.seeds) }
        persistSeeds()
        return seed
    }

    fun editSeed(seed: SeedRecord): Boolean {
        var changed = false
        _state.update { state ->
            state.copy(seeds = state.seeds.map { current ->
                if (current.id == seed.id) {
                    changed = true
                    seed.copy(title = seed.title.trim().take(120), content = seed.content.trim().take(8000))
                } else current
            })
        }
        if (changed) persistSeeds()
        return changed
    }

    fun deleteSeed(id: String): Boolean {
        val before = _state.value.seeds.size
        _state.update { it.copy(seeds = it.seeds.filterNot { seed -> seed.id == id }) }
        val changed = before != _state.value.seeds.size
        if (changed) persistSeeds()
        return changed
    }

    fun searchSeeds(query: String): List<SeedRecord> {
        val q = query.trim()
        if (q.isBlank()) return _state.value.seeds
        return _state.value.seeds.filter {
            it.title.contains(q, true) || it.content.contains(q, true) || it.status.name.contains(q, true)
        }
    }

    fun addWorldbook(title: String, content: String): WorldbookRecord? {
        val cleanTitle = title.trim().take(120)
        val cleanContent = content.trim().take(12000)
        if (cleanTitle.isBlank() || cleanContent.isBlank()) return null
        val entry = WorldbookRecord("world-${UUID.randomUUID()}", cleanTitle, cleanContent, true)
        _state.update { it.copy(worldbook = listOf(entry) + it.worldbook) }
        persistWorldbook()
        return entry
    }

    fun editWorldbook(entry: WorldbookRecord): Boolean {
        var changed = false
        _state.update { state ->
            state.copy(worldbook = state.worldbook.map { current ->
                if (current.id == entry.id) {
                    changed = true
                    entry.copy(title = entry.title.trim().take(120), content = entry.content.trim().take(12000))
                } else current
            })
        }
        if (changed) persistWorldbook()
        return changed
    }

    fun toggleWorldbook(id: String): Boolean {
        var changed = false
        _state.update { state ->
            state.copy(worldbook = state.worldbook.map { current ->
                if (current.id == id) {
                    changed = true
                    current.copy(enabled = !current.enabled)
                } else current
            })
        }
        if (changed) persistWorldbook()
        return changed
    }

    fun deleteWorldbook(id: String): Boolean {
        val before = _state.value.worldbook.size
        _state.update { it.copy(worldbook = it.worldbook.filterNot { entry -> entry.id == id }) }
        val changed = before != _state.value.worldbook.size
        if (changed) persistWorldbook()
        return changed
    }

    fun searchWorldbook(query: String): List<WorldbookRecord> {
        val q = query.trim()
        if (q.isBlank()) return _state.value.worldbook
        return _state.value.worldbook.filter { it.title.contains(q, true) || it.content.contains(q, true) }
    }

    fun saveCustomInstructions(value: String) {
        val clean = value.take(16000)
        _state.update { it.copy(customInstructions = clean) }
        persistence.write(instructionKey, clean)
    }

    fun clearCustomInstructions() = saveCustomInstructions("")

    private fun load(): MemoryState = MemoryState(
        memories = loadMemories(),
        seeds = loadSeeds(),
        worldbook = loadWorldbook(),
        customInstructions = persistence.read(instructionKey).orEmpty()
    )

    private fun persistMemories() {
        persistence.write(memoryKey, _state.value.memories.joinToString("\n") { record ->
            LocalCodec.pack(
                record.id,
                record.title,
                record.lifeCore,
                record.content,
                record.usageHint,
                record.avoidHint,
                LocalCodec.encodeList(record.tags)
            )
        })
    }

    private fun loadMemories(): List<MemoryRecord> = LocalCodec.lines(persistence.read(memoryKey)).mapNotNull { f ->
        if (f.size < 7) return@mapNotNull null
        MemoryRecord(f[0], f[1], f[2], f[3], f[4], f[5], LocalCodec.decodeList(f[6]))
    }

    private fun persistSeeds() {
        persistence.write(seedKey, _state.value.seeds.joinToString("\n") { seed ->
            LocalCodec.pack(seed.id, seed.title, seed.content, seed.status.name)
        })
    }

    private fun loadSeeds(): List<SeedRecord> = LocalCodec.lines(persistence.read(seedKey)).mapNotNull { f ->
        if (f.size < 4) return@mapNotNull null
        runCatching { SeedRecord(f[0], f[1], f[2], SeedStatus.valueOf(f[3])) }.getOrNull()
    }

    private fun persistWorldbook() {
        persistence.write(worldbookKey, _state.value.worldbook.joinToString("\n") { entry ->
            LocalCodec.pack(entry.id, entry.title, entry.content, entry.enabled)
        })
    }

    private fun loadWorldbook(): List<WorldbookRecord> = LocalCodec.lines(persistence.read(worldbookKey)).mapNotNull { f ->
        if (f.size < 4) return@mapNotNull null
        WorldbookRecord(f[0], f[1], f[2], f[3].toBoolean())
    }
}
