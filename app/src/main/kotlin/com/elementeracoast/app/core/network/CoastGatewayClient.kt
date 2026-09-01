package com.elementeracoast.app.core.network

import com.elementeracoast.app.core.contract.CoastConversation
import com.elementeracoast.app.core.contract.CoastMessage
import com.elementeracoast.app.core.contract.DailyDiary
import com.elementeracoast.app.core.contract.DailyMoment
import com.elementeracoast.app.core.contract.DogtalkSubmission
import com.elementeracoast.app.core.contract.MemoryEntry
import com.elementeracoast.app.core.contract.ModelProfile
import com.elementeracoast.app.core.model.RoomType
import kotlinx.coroutines.flow.Flow

/**
 * Future Elementera Coast backend boundary.
 *
 * This interface has no implementation in COAST-NATIVE-PARITY-CLEAN-24. The app still has no
 * INTERNET permission, no base URL, no credential, and no real request path wired to Compose.
 */
interface CoastGatewayClient {
    suspend fun login(password: String): CoastSession
    suspend fun getProfile(): ModelProfile
    suspend fun listConversations(roomType: RoomType? = null): List<CoastConversation>
    suspend fun getHistory(conversationId: String): List<CoastMessage>
    fun sendChat(request: CoastChatRequest): Flow<CoastStreamEvent>
    suspend fun listModels(): List<String>
    suspend fun listMoments(): List<DailyMoment>
    suspend fun listDiaries(): List<DailyDiary>
    suspend fun searchMemory(query: String): List<MemoryEntry>
}

data class CoastSession(val authenticated: Boolean)

data class CoastChatRequest(
    val conversationId: String,
    val sourceTurnId: String,
    val modelId: String,
    val messages: List<CoastMessage>,
    val dogtalk: DogtalkSubmission? = null
)

sealed interface CoastStreamEvent {
    data class Delta(val text: String) : CoastStreamEvent
    data class Error(val message: String) : CoastStreamEvent
    data object Done : CoastStreamEvent
}
