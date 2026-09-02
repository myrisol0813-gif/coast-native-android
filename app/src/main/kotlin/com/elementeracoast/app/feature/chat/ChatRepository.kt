package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.network.ApiStreamEvent
import com.elementeracoast.app.core.network.CoastApiClient
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.remote.RemoteChatRequest
import com.elementeracoast.app.core.remote.RemoteFurnitureRun
import com.elementeracoast.app.core.remote.RemoteHistory
import com.elementeracoast.app.core.remote.RemoteCacheStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface ChatProgress {
    data class Delta(val text: String) : ChatProgress
    data class Completed(val history: RemoteHistory, val modelId: String, val finishReason: String) : ChatProgress
}

class ChatRepository(
    private val api: CoastApiClient,
    private val cache: RemoteCacheStore,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false }
) {
    fun cachedHistory(conversationId: String): RemoteHistory? = cache.history(conversationId)

    suspend fun loadHistory(conversationId: String): RemoteHistory =
        api.getHistory(conversationId).also { cache.putHistory(conversationId, it) }

    suspend fun persistHistory(conversationId: String, history: RemoteHistory): RemoteHistory =
        api.putHistory(conversationId, history).also { cache.putHistory(conversationId, it) }

    fun streamReply(
        conversationId: String,
        historyWithUser: RemoteHistory,
        turnId: String,
        modelId: String
    ): Flow<ChatProgress> = flow {
        var content = ""
        var actualModel = modelId
        var finishReason = ""
        var furnitureRuns = emptyList<RemoteFurnitureRun>()
        var done = false
        val request = RemoteChatRequest(
            conversationId = conversationId,
            sourceTurnId = turnId,
            model = modelId,
            messages = ChatSyncMapper.activeMessages(historyWithUser),
            localDate = LocalDate.now().toString(),
            localDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            stream = true
        )
        api.streamChat(request).collect { event ->
            when (event) {
                is ApiStreamEvent.Meta -> {
                    actualModel = event.data.runCatching {
                        jsonObject["model"]?.jsonPrimitive?.content
                    }.getOrNull().orEmpty().ifBlank { actualModel }
                }
                is ApiStreamEvent.Delta -> {
                    content += event.text
                    emit(ChatProgress.Delta(event.text))
                }
                is ApiStreamEvent.FurnitureRuns -> {
                    furnitureRuns = runCatching {
                        json.decodeFromJsonElement(ListSerializer(RemoteFurnitureRun.serializer()), event.data)
                    }.getOrDefault(emptyList())
                }
                is ApiStreamEvent.Done -> {
                    finishReason = event.finishReason
                    done = true
                }
                is ApiStreamEvent.Error -> throw event.error
                is ApiStreamEvent.Tool,
                is ApiStreamEvent.DeskSlip,
                is ApiStreamEvent.Usage -> Unit
            }
        }
        if (!done) throw com.elementeracoast.app.core.network.CoastApiException(
            com.elementeracoast.app.core.network.CoastApiErrorKind.Stream,
            "stream_incomplete",
            "海岸回复流提前中断。"
        )
        val completed = ChatSyncMapper.appendAssistant(
            history = historyWithUser,
            turnId = turnId,
            content = content,
            modelId = actualModel,
            finishReason = finishReason,
            furnitureRuns = furnitureRuns
        )
        val saved = persistHistory(conversationId, completed)
        emit(ChatProgress.Completed(saved, actualModel, finishReason))
    }

    fun failedHistory(
        historyWithUser: RemoteHistory,
        turnId: String,
        modelId: String,
        error: CoastApiException,
        partialContent: String = ""
    ): RemoteHistory = ChatSyncMapper.appendAssistant(
        history = historyWithUser,
        turnId = turnId,
        content = partialContent,
        modelId = modelId,
        finishReason = "error",
        errorDetail = "${error.type}: ${error.message}"
    )

    fun cacheHistory(conversationId: String, history: RemoteHistory) = cache.putHistory(conversationId, history)
}
