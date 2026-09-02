package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureRun
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.remote.RemoteAssistantBranches
import com.elementeracoast.app.core.remote.RemoteChatMessage
import com.elementeracoast.app.core.remote.RemoteFurnitureRun
import com.elementeracoast.app.core.remote.RemoteHistory
import com.elementeracoast.app.core.remote.RemoteTurn
import com.elementeracoast.app.core.remote.RemoteUserBranch
import com.elementeracoast.app.core.remote.RemoteVariant
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

object ChatSyncMapper {
    data class AppendedUser(val history: RemoteHistory, val turnId: String)

    fun appendUser(history: RemoteHistory, text: String): AppendedUser {
        val now = Instant.now().toString()
        val turnId = "turn_${UUID.randomUUID()}"
        val variant = RemoteVariant(
            id = "user_variant_${UUID.randomUUID()}",
            content = text,
            createdAt = now,
            displayAuthor = "小寒"
        )
        val turn = RemoteTurn(
            id = turnId,
            user = RemoteUserBranch(active = 0, variants = listOf(variant)),
            assistant = RemoteAssistantBranches(
                activeByUserVariant = mapOf("0" to 0),
                variantsByUserVariant = mapOf("0" to emptyList())
            )
        )
        return AppendedUser(
            history.copy(version = 4, updatedAt = now, turns = (history.turns + turn).takeLast(100)),
            turnId
        )
    }

    fun appendAssistant(
        history: RemoteHistory,
        turnId: String,
        content: String,
        modelId: String,
        finishReason: String,
        errorDetail: String = "",
        furnitureRuns: List<RemoteFurnitureRun> = emptyList()
    ): RemoteHistory {
        val turns = history.turns.map { turn ->
            if (turn.id != turnId) return@map turn
            val userIndex = turn.user.active.coerceIn(0, (turn.user.variants.size - 1).coerceAtLeast(0))
            val key = userIndex.toString()
            val list = turn.assistant.variantsByUserVariant[key].orEmpty() + RemoteVariant(
                id = "assistant_variant_${UUID.randomUUID()}",
                content = content,
                createdAt = Instant.now().toString(),
                modelId = modelId,
                finishReason = finishReason,
                generationSource = "chat",
                errorDetail = errorDetail.ifBlank { null },
                furnitureRuns = furnitureRuns
            )
            turn.copy(
                assistant = turn.assistant.copy(
                    activeByUserVariant = turn.assistant.activeByUserVariant + (key to list.lastIndex),
                    variantsByUserVariant = turn.assistant.variantsByUserVariant + (key to list)
                )
            )
        }
        return history.copy(updatedAt = Instant.now().toString(), turns = turns)
    }

    fun activeMessages(history: RemoteHistory): List<RemoteChatMessage> = buildList {
        history.turns.forEach { turn ->
            val userIndex = turn.user.active.coerceIn(0, (turn.user.variants.size - 1).coerceAtLeast(0))
            val user = turn.user.variants.getOrNull(userIndex)
            if (user != null && user.content.isNotBlank() && !user.hidden) add(RemoteChatMessage("user", user.content))
            val key = userIndex.toString()
            val assistants = turn.assistant.variantsByUserVariant[key].orEmpty()
            val assistantIndex = (turn.assistant.activeByUserVariant[key] ?: 0)
                .coerceIn(0, (assistants.size - 1).coerceAtLeast(0))
            val assistant = assistants.getOrNull(assistantIndex)
            if (assistant != null && assistant.content.isNotBlank()) add(RemoteChatMessage("assistant", assistant.content))
        }
    }

    fun toUi(history: RemoteHistory): List<ChatMessage> = buildList {
        history.turns.forEach { turn ->
            val userIndex = turn.user.active.coerceIn(0, (turn.user.variants.size - 1).coerceAtLeast(0))
            val user = turn.user.variants.getOrNull(userIndex)
            if (user != null && !user.hidden) {
                add(
                    ChatMessage(
                        id = stableLong(user.id),
                        role = MessageRole.User,
                        text = user.content,
                        createdAtLabel = user.createdAt
                    )
                )
            }
            val key = userIndex.toString()
            val assistants = turn.assistant.variantsByUserVariant[key].orEmpty()
            val assistantIndex = (turn.assistant.activeByUserVariant[key] ?: 0)
                .coerceIn(0, (assistants.size - 1).coerceAtLeast(0))
            val assistant = assistants.getOrNull(assistantIndex)
            if (assistant != null) {
                add(
                    ChatMessage(
                        id = stableLong(assistant.id),
                        role = MessageRole.Assistant,
                        text = assistant.content,
                        modelId = assistant.modelId,
                        generationSource = assistant.generationSource,
                        liked = assistant.liked,
                        favorite = assistant.favorite,
                        errorDetail = assistant.errorDetail,
                        createdAtLabel = assistant.createdAt,
                        furnitureRuns = assistant.furnitureRuns.map(::toFurnitureRun)
                    )
                )
            }
        }
    }

    private fun toFurnitureRun(value: RemoteFurnitureRun): FurnitureRun = FurnitureRun(
        actionId = value.id,
        actionKey = value.toolKey,
        label = value.label,
        success = value.status != "error",
        count = value.count,
        items = value.items.map { FurnitureItem(it.title, it.kind) },
        extraCount = value.extraCount,
        errorType = value.errorType.orEmpty()
    )

    private fun stableLong(value: String): Long {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        var result = 0L
        repeat(8) { index -> result = (result shl 8) or (bytes[index].toLong() and 0xffL) }
        return result and Long.MAX_VALUE
    }
}
