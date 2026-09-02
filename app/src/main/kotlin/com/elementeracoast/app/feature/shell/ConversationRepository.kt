package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.network.CoastApiClient
import com.elementeracoast.app.core.remote.RemoteCacheStore
import com.elementeracoast.app.core.remote.RemoteConversation

class ConversationRepository(
    private val api: CoastApiClient,
    private val cache: RemoteCacheStore
) {
    fun cached(): List<ConversationSummary> = cache.conversations().map(::toSummary)

    suspend fun refresh(): List<ConversationSummary> {
        val remote = api.listConversations()
        cache.putConversations(remote)
        return remote.map(::toSummary)
    }

    suspend fun create(roomType: RoomType, title: String = "新聊天"): ConversationSummary {
        val created = api.createConversation(title, roomType.wireValue)
        replaceCached(created)
        return toSummary(created)
    }

    suspend fun rename(id: String, title: String): ConversationSummary {
        val updated = api.renameConversation(id, title.trim().ifBlank { "新聊天" })
        replaceCached(updated)
        return toSummary(updated)
    }

    suspend fun delete(id: String) {
        api.deleteConversation(id)
        cache.putConversations(cache.conversations().filterNot { it.id == id })
        cache.removeHistory(id)
    }

    private fun replaceCached(value: RemoteConversation) {
        cache.putConversations(listOf(value) + cache.conversations().filterNot { it.id == value.id })
    }

    private fun toSummary(value: RemoteConversation): ConversationSummary = ConversationSummary(
        id = value.id,
        title = value.title,
        roomType = RoomType.fromWire(value.roomType)
    )
}
