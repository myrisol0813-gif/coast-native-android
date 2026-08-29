package com.elementeracoast.app.core.network

import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.model.CoastModel
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class CoastGatewayClient(
    private val sessionStore: CoastSessionStore,
    baseUrl: String = BuildConfig.COAST_BASE_URL,
) {
    private val baseUrl = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val client = OkHttpClient.Builder()
        .cookieJar(sessionStore)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val activeGeneration = AtomicReference<Call?>(null)

    suspend fun login(password: String): Boolean = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().add("password", password).build()
        val request = Request.Builder()
            .url("$baseUrl/login")
            .header("Origin", baseUrl)
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
        }
        getSessionStatus().authenticated
    }

    suspend fun getSessionStatus(): SessionEnvelope = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/session").get().build()
        client.newCall(request).execute().use { response ->
            if (response.code == 401) return@withContext SessionEnvelope()
            response.requireSuccessJson<SessionEnvelope>()
        }
    }

    suspend fun listModels(): List<CoastModel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/models").get().build()
        client.newCall(request).execute().use { response ->
            val catalog = response.requireSuccessJson<ModelCatalogEnvelope>()
            (catalog.groups.openAiChat + catalog.groups.freeTest)
                .distinctBy { it.id }
                .filter { it.id.isNotBlank() }
                .map { CoastModel(it.id, it.name.ifBlank { it.id }, it.isFree, it.available) }
        }
    }

    suspend fun getProfile(): ChatProfile = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/chat/profile").get().build()
        client.newCall(request).execute().use { it.requireSuccessJson<ProfileEnvelope>().profile }
    }

    suspend fun getCurrentModel(): String = getProfile().currentChatModel.ifBlank { sessionStore.selectedModel() }

    suspend fun setCurrentModel(modelId: String): ChatProfile = withContext(Dispatchers.IO) {
        val current = getProfile()
        val next = current.copy(currentChatModel = modelId)
        val payload = json.encodeToString(ProfileEnvelope(ok = true, profile = next))
        val request = mutableJsonRequest("$baseUrl/api/chat/profile", "PUT", payload)
        client.newCall(request).execute().use { response ->
            val saved = response.requireSuccessJson<ProfileEnvelope>().profile
            sessionStore.saveSelectedModel(saved.currentChatModel)
            saved
        }
    }

    suspend fun ensureConversation(): ConversationDto = withContext(Dispatchers.IO) {
        val listRequest = Request.Builder().url("$baseUrl/api/chat/conversations").get().build()
        val existing = client.newCall(listRequest).execute().use {
            it.requireSuccessJson<ConversationsEnvelope>().conversations.firstOrNull()
        }
        if (existing != null) return@withContext existing
        val request = mutableJsonRequest(
            "$baseUrl/api/chat/conversations",
            "POST",
            "{\"title\":\"新聊天\"}",
        )
        client.newCall(request).execute().use { it.requireSuccessJson<ConversationEnvelope>().conversation }
    }

    suspend fun loadHistory(conversationId: String): ChatHistoryDto = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$baseUrl/api/chat/history?conversation_id=$conversationId")
            .get()
            .build()
        client.newCall(request).execute().use { it.requireSuccessJson<HistoryEnvelope>().history }
    }

    suspend fun saveHistory(conversationId: String, history: ChatHistoryDto) = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(history)
        val request = mutableJsonRequest(
            "$baseUrl/api/chat/history?conversation_id=$conversationId",
            "PUT",
            payload,
        )
        client.newCall(request).execute().use { it.requireSuccessJson<HistoryEnvelope>() }
        Unit
    }

    suspend fun sendChatMessage(request: ChatRequestDto): JsonObject = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(request.copy(stream = false))
        val call = client.newCall(mutableJsonRequest("$baseUrl/api/chat", "POST", payload))
        activeGeneration.set(call)
        try {
            call.execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw CoastGatewayException(errorMessage(response.code, text))
                json.parseToJsonElement(text).jsonObject
            }
        } finally {
            activeGeneration.compareAndSet(call, null)
        }
    }

    fun streamChatMessage(request: ChatRequestDto): Flow<ChatStreamEvent> = callbackFlow {
        val payload = json.encodeToString(request.copy(stream = true))
        val call = client.newCall(
            mutableJsonRequest("$baseUrl/api/chat", "POST", payload, accept = "text/event-stream"),
        )
        activeGeneration.set(call)
        val job = launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val text = response.body?.string().orEmpty()
                        throw CoastGatewayException(errorMessage(response.code, text))
                    }
                    val source = response.body?.source() ?: throw CoastGatewayException("海岸没有返回可读取的流。")
                    val parser = CoastSseParser()
                    while (!source.exhausted()) {
                        parser.accept(source.readUtf8Line())?.let { block ->
                            trySend(decodeStreamEvent(block))
                        }
                    }
                    parser.accept(null)?.let { trySend(decodeStreamEvent(it)) }
                }
                close()
            } catch (error: Throwable) {
                if (call.isCanceled()) close(CancellationIOException()) else close(error)
            } finally {
                activeGeneration.compareAndSet(call, null)
            }
        }
        awaitClose {
            call.cancel()
            job.cancel()
            activeGeneration.compareAndSet(call, null)
        }
    }

    fun stopGeneration() {
        activeGeneration.getAndSet(null)?.cancel()
    }

    fun clearSession() = sessionStore.clearSession()

    fun requestTemplate(
        conversationId: String,
        sourceTurnId: String,
        modelId: String,
        messages: List<SimpleChatMessageDto>,
    ): ChatRequestDto {
        val settings = buildJsonObject {
            put("streamingEnabled", true)
            put("temperature", 0.7)
        }
        return ChatRequestDto(
            conversationId = conversationId,
            sourceTurnId = sourceTurnId,
            localDate = LocalDate.now().toString(),
            localDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            model = modelId,
            messages = messages.takeLast(20),
            settings = settings,
        )
    }

    private fun mutableJsonRequest(url: String, method: String, payload: String, accept: String = "application/json"): Request {
        val body = payload.toRequestBody(JSON_MEDIA)
        return Request.Builder()
            .url(url)
            .header("Origin", baseUrl)
            .header("Accept", accept)
            .method(method, body)
            .build()
    }

    private inline fun <reified T> okhttp3.Response.requireSuccessJson(): T {
        val text = body?.string().orEmpty()
        if (!isSuccessful) throw CoastGatewayException(errorMessage(code, text))
        return json.decodeFromString(text)
    }

    private fun errorMessage(status: Int, raw: String): String {
        val fallback = "海岸请求失败（$status）"
        return runCatching {
            val root = json.parseToJsonElement(raw).jsonObject
            val error = root["error"]
            when {
                error == null -> fallback
                error is kotlinx.serialization.json.JsonPrimitive -> error.content
                else -> error.jsonObject["message"]?.jsonPrimitive?.content ?: fallback
            }
        }.getOrDefault(fallback)
    }

    private fun decodeStreamEvent(block: SseBlock): ChatStreamEvent {
        val data = runCatching { json.parseToJsonElement(block.data).jsonObject }.getOrElse { JsonObject(emptyMap()) }
        return when (block.event) {
            "meta" -> ChatStreamEvent.Meta(data["model"]?.jsonPrimitive?.content.orEmpty())
            "delta" -> ChatStreamEvent.Delta(data["content"]?.jsonPrimitive?.content.orEmpty())
            "usage" -> ChatStreamEvent.Usage(json.decodeFromJsonElement(UsageDto.serializer(), data))
            "done" -> ChatStreamEvent.Done(data["finish_reason"]?.jsonPrimitive?.content.orEmpty())
            "error" -> ChatStreamEvent.Error(
                data["type"]?.jsonPrimitive?.content ?: "stream_error",
                data["message"]?.jsonPrimitive?.content ?: "流式生成中断。",
            )
            "tool" -> ChatStreamEvent.Tool
            "desk_slip" -> ChatStreamEvent.DeskSlip
            else -> ChatStreamEvent.Tool
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

class CoastGatewayException(message: String) : IOException(message)
class CancellationIOException : IOException("generation_cancelled")
