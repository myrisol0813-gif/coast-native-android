package com.elementeracoast.app.feature.memory

import com.elementeracoast.app.core.remote.RemoteCacheStore
import com.elementeracoast.app.core.remote.RemoteCustomInstructions
import com.elementeracoast.app.core.remote.RemoteMemoryEntry
import com.elementeracoast.app.core.remote.RemoteMemoryEntryWriteRequest
import com.elementeracoast.app.core.remote.RemoteMemoryPocket
import com.elementeracoast.app.core.remote.RemoteMemoryPocketResolveRequest
import com.elementeracoast.app.core.remote.RemoteWorldbookEntry
import com.elementeracoast.app.core.remote.RemoteWorldbookEntryWriteRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MemoryRepository {
    val snapshot: StateFlow<MemorySnapshot>
    fun cachedSnapshot(): MemorySnapshot
    suspend fun refresh(conversationId: String)
    suspend fun refreshEntries()
    suspend fun refreshPockets(conversationId: String)
    suspend fun refreshWorldbook()
    suspend fun refreshInstructions()
    suspend fun saveEntry(entry: MemoryEntry): MemoryEntry
    suspend fun deleteEntry(id: String)
    suspend fun resolvePocket(id: String, action: String, tag: String? = null)
    suspend fun saveWorldbook(entry: WorldbookEntry): WorldbookEntry
    suspend fun deleteWorldbook(id: String)
    suspend fun testWorldbook(input: String): List<WorldbookEntry>
    suspend fun saveInstructions(content: String): CustomInstructions
}

class DefaultMemoryRepository(
    private val remote: MemoryRemoteDataSource,
    private val cache: RemoteCacheStore
) : MemoryRepository {
    private val initial = MemorySnapshot(
        memories = cache.memoryEntries("memory").map(::toEntry),
        seeds = cache.memoryEntries("seed").map(::toEntry),
        worldbook = cache.worldbookEntries().map(::toWorldbook),
        customInstructions = cache.customInstructions()?.let(::toInstructions) ?: CustomInstructions()
    )
    private val mutableSnapshot = MutableStateFlow(initial)
    override val snapshot: StateFlow<MemorySnapshot> = mutableSnapshot.asStateFlow()

    override fun cachedSnapshot(): MemorySnapshot = mutableSnapshot.value

    override suspend fun refresh(conversationId: String) = coroutineScope {
        val memories = async { remote.listEntries("memory") }
        val seeds = async { remote.listEntries("seed") }
        val pockets = async { remote.listPockets(conversationId) }
        val worldbook = async { remote.listWorldbook() }
        val instructions = async { remote.getInstructions() }
        val remoteMemories = memories.await()
        val remoteSeeds = seeds.await()
        val remotePockets = pockets.await()
        val remoteWorldbook = worldbook.await()
        val remoteInstructions = instructions.await()
        cache.putMemoryEntries("memory", remoteMemories)
        cache.putMemoryEntries("seed", remoteSeeds)
        cache.putMemoryPockets(conversationId, remotePockets)
        cache.putWorldbookEntries(remoteWorldbook)
        cache.putCustomInstructions(remoteInstructions)
        mutableSnapshot.value = MemorySnapshot(
            memories = remoteMemories.map(::toEntry),
            seeds = remoteSeeds.map(::toEntry),
            pockets = remotePockets.map(::toPocket),
            pocketConversationId = conversationId,
            worldbook = remoteWorldbook.map(::toWorldbook),
            customInstructions = remoteInstructions?.let(::toInstructions) ?: CustomInstructions()
        )
    }

    override suspend fun refreshEntries() {
        val memories = remote.listEntries("memory")
        val seeds = remote.listEntries("seed")
        cache.putMemoryEntries("memory", memories)
        cache.putMemoryEntries("seed", seeds)
        mutableSnapshot.value = mutableSnapshot.value.copy(
            memories = memories.map(::toEntry),
            seeds = seeds.map(::toEntry)
        )
    }

    override suspend fun refreshPockets(conversationId: String) {
        val pockets = remote.listPockets(conversationId)
        cache.putMemoryPockets(conversationId, pockets)
        mutableSnapshot.value = mutableSnapshot.value.copy(
            pockets = pockets.map(::toPocket),
            pocketConversationId = conversationId
        )
    }

    override suspend fun refreshWorldbook() {
        val entries = remote.listWorldbook()
        cache.putWorldbookEntries(entries)
        mutableSnapshot.value = mutableSnapshot.value.copy(worldbook = entries.map(::toWorldbook))
    }

    override suspend fun refreshInstructions() {
        val instructions = remote.getInstructions()
        cache.putCustomInstructions(instructions)
        mutableSnapshot.value = mutableSnapshot.value.copy(
            customInstructions = instructions?.let(::toInstructions) ?: CustomInstructions()
        )
    }

    override suspend fun saveEntry(entry: MemoryEntry): MemoryEntry {
        val tag = entry.tag.ifBlank { entry.tags.firstOrNull { it in canonicalMemoryTags }.orEmpty() }
        val request = RemoteMemoryEntryWriteRequest(
            entryType = entry.entryType,
            title = entry.title.trim(),
            lifeCore = entry.lifeCore.trim(),
            content = entry.content.trim(),
            usageHint = entry.usageHint.trim(),
            avoidHint = entry.avoidHint.trim(),
            memoryLevel = if (entry.entryType == "memory") entry.memoryLevel else "ordinary",
            status = entry.status,
            tag = tag,
            memoryTags = if (tag.isBlank()) emptyList() else listOf(tag),
            sourceModel = entry.sourceModel.trim(),
            sourceWindow = entry.sourceWindow.trim(),
            sourceTime = entry.sourceDate.takeIf(String::isNotBlank)
        )
        val saved = if (entry.id.isBlank()) remote.createEntry(request) else remote.patchEntry(entry.id, request)
        refreshEntries()
        return toEntry(saved)
    }

    override suspend fun deleteEntry(id: String) {
        remote.deleteEntry(id)
        refreshEntries()
    }

    override suspend fun resolvePocket(id: String, action: String, tag: String?) {
        remote.resolvePocket(
            id,
            RemoteMemoryPocketResolveRequest(action = action, tag = if (action == "discard") null else tag)
        )
        val conversationId = mutableSnapshot.value.pocketConversationId
        if (!conversationId.isNullOrBlank()) refreshPockets(conversationId)
        if (action != "discard") refreshEntries()
    }

    override suspend fun saveWorldbook(entry: WorldbookEntry): WorldbookEntry {
        val request = RemoteWorldbookEntryWriteRequest(
            title = entry.title.trim(),
            content = entry.content.trim(),
            keywords = entry.keywords.map(String::trim).filter(String::isNotBlank).distinct(),
            useRegex = entry.useRegex,
            caseSensitive = entry.caseSensitive,
            constantActive = entry.constantActive,
            priority = entry.priority,
            scanDepth = entry.scanDepth,
            enabled = entry.enabled,
            scope = entry.scope,
            visitorSafe = entry.visitorSafe
        )
        val saved = if (entry.id.isBlank()) remote.createWorldbook(request) else remote.patchWorldbook(entry.id, request)
        refreshWorldbook()
        return toWorldbook(saved)
    }

    override suspend fun deleteWorldbook(id: String) {
        remote.deleteWorldbook(id)
        refreshWorldbook()
    }

    override suspend fun testWorldbook(input: String): List<WorldbookEntry> = remote.testWorldbook(input).map(::toWorldbook)

    override suspend fun saveInstructions(content: String): CustomInstructions {
        val saved = remote.putInstructions(content)
        cache.putCustomInstructions(saved)
        val mapped = saved?.let(::toInstructions) ?: CustomInstructions()
        mutableSnapshot.value = mutableSnapshot.value.copy(customInstructions = mapped)
        return mapped
    }

    private companion object {
        fun toEntry(value: RemoteMemoryEntry) = MemoryEntry(
            id = value.id,
            entryType = value.entryType,
            title = value.title,
            lifeCore = value.lifeCore,
            content = value.content,
            usageHint = value.usageHint,
            avoidHint = value.avoidHint,
            memoryLevel = value.memoryLevel,
            status = value.status,
            tags = value.memoryTags,
            sourceModel = value.sourceModel,
            sourceWindow = value.sourceWindow,
            sourceDate = value.sourceDate,
            tag = value.tag
        )

        fun toPocket(value: RemoteMemoryPocket) = MemoryPocket(
            id = value.id,
            conversationId = value.conversationId,
            title = value.title,
            lifeCore = value.lifeCore,
            content = value.content,
            usageHint = value.usageHint,
            avoidHint = value.avoidHint,
            sourceExcerpt = value.sourceExcerpt,
            status = value.status
        )

        fun toWorldbook(value: RemoteWorldbookEntry) = WorldbookEntry(
            id = value.id,
            title = value.title,
            content = value.content,
            keywords = value.keywords,
            useRegex = value.useRegex,
            caseSensitive = value.caseSensitive,
            constantActive = value.constantActive,
            priority = value.priority,
            scanDepth = value.scanDepth,
            enabled = value.enabled,
            scope = value.scope,
            visitorSafe = value.visitorSafe
        )

        fun toInstructions(value: RemoteCustomInstructions) = CustomInstructions(
            content = value.content,
            status = value.status,
            updatedAt = value.updatedAt,
            updatedBy = value.updatedBy,
            source = value.source
        )
    }
}
