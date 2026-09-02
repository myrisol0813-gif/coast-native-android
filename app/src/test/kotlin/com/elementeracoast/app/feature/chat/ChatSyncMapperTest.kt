package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.remote.RemoteAssistantBranches
import com.elementeracoast.app.core.remote.RemoteHistory
import com.elementeracoast.app.core.remote.RemoteTurn
import com.elementeracoast.app.core.remote.RemoteUserBranch
import com.elementeracoast.app.core.remote.RemoteVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSyncMapperTest {
    @Test
    fun mapperPreservesRemoteBranchCountsAndTurnIdentity() {
        val history = RemoteHistory(
            turns = listOf(
                RemoteTurn(
                    id = "turn-1",
                    user = RemoteUserBranch(
                        active = 1,
                        variants = listOf(
                            RemoteVariant("u1", "旧问题", "2026-09-02T00:00:00Z"),
                            RemoteVariant("u2", "新问题", "2026-09-02T00:01:00Z")
                        )
                    ),
                    assistant = RemoteAssistantBranches(
                        activeByUserVariant = mapOf("1" to 1),
                        variantsByUserVariant = mapOf(
                            "0" to listOf(RemoteVariant("a0", "旧分支回复", "2026-09-02T00:00:10Z")),
                            "1" to listOf(
                                RemoteVariant("a1", "第一版", "2026-09-02T00:01:10Z"),
                                RemoteVariant("a2", "第二版", "2026-09-02T00:01:20Z")
                            )
                        )
                    )
                )
            )
        )

        val messages = ChatSyncMapper.toUi(history)
        val user = messages.first { it.role == MessageRole.User }
        val assistant = messages.first { it.role == MessageRole.Assistant }

        assertEquals("turn-1", user.turnId)
        assertEquals("新问题", user.text)
        assertEquals(2, user.variantCount)
        assertEquals(1, user.variantIndex)
        assertEquals("第二版", assistant.text)
        assertEquals(2, assistant.variantCount)
        assertEquals(1, assistant.variantIndex)
    }

    @Test
    fun retryContextEndsAtTargetUserAndDoesNotIncludeFailedAssistant() {
        var history = RemoteHistory()
        val first = ChatSyncMapper.appendUser(history, "第一问")
        history = ChatSyncMapper.appendAssistant(first.history, first.turnId, "第一答", "model-a", "stop")
        val second = ChatSyncMapper.appendUser(history, "第二问")
        history = ChatSyncMapper.appendAssistant(
            second.history,
            second.turnId,
            "失败时收到的一点残片",
            "model-a",
            "error",
            errorDetail = "stream_error: broken"
        )

        val context = ChatSyncMapper.contextMessages(history, second.turnId)

        assertEquals(listOf("user", "assistant", "user"), context.map { it.role })
        assertEquals("第二问", context.last().content)
        assertFalse(context.any { it.content.contains("残片") })
    }

    @Test
    fun userFailureIsVisibleAndCanBeClearedWithoutCreatingAnotherTurn() {
        val appended = ChatSyncMapper.appendUser(RemoteHistory(), "不要重复我")
        val failed = ChatSyncMapper.markUserFailure(appended.history, appended.turnId, "network: offline")

        assertEquals(1, failed.turns.size)
        val failedUi = ChatSyncMapper.toUi(failed).single()
        assertTrue(failedUi.errorDetail!!.contains("offline"))

        val cleared = ChatSyncMapper.clearUserFailure(failed, appended.turnId)
        assertEquals(1, cleared.turns.size)
        assertEquals(null, ChatSyncMapper.toUi(cleared).single().errorDetail)
    }
}
