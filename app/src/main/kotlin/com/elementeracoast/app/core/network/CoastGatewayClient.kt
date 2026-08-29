package com.elementeracoast.app.core.network

import kotlinx.coroutines.flow.Flow

const val COAST_BASE_URL = "https://app.elementeracoast.com"

/**
 * Future Coast backend boundary. This PoC intentionally provides no network
 * implementation and the AndroidManifest declares no INTERNET permission.
 */
interface CoastGatewayClient {
    suspend fun login(password: String): CoastSession
    suspend fun getSession(): CoastSession
    suspend fun listModels(): List<CoastModel>
    suspend fun getProfile(): CoastProfile
    fun sendMessageStream(request: CoastChatRequest): Flow<CoastStreamEvent>
    fun stopGeneration()
}

data class CoastSession(val authenticated: Boolean)
data class CoastModel(val id: String, val label: String = id)
data class CoastProfile(val currentModel: String)
data class CoastChatRequest(val conversationId: String, val text: String)

sealed interface CoastStreamEvent {
    data class Delta(val text: String) : CoastStreamEvent
    data class Error(val message: String) : CoastStreamEvent
    data object Done : CoastStreamEvent
}
