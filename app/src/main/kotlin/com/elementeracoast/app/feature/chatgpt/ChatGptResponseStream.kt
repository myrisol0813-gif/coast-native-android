package com.elementeracoast.app.feature.chatgpt

import com.elementeracoast.app.core.remote.RemoteChatMessage
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed interface ChatGptStreamEvent {
    data class Delta(val text: String) : ChatGptStreamEvent
    data class Completed(val inputTokens: Long?, val cachedTokens: Long?, val outputTokens: Long?) : ChatGptStreamEvent
}

internal class ChatGptInferenceException(val type: String, message: String) : IOException(message)

/** Direct phone -> OpenAI inference. Credentials never enter Coast requests or exports. */
class ChatGptResponseStream(
    private val http: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun stream(
        accessToken: String,
        model: String,
        instructions: String,
        messages: List<RemoteChatMessage>
    ): Flow<ChatGptStreamEvent> = callbackFlow {
        require(accessToken.isNotBlank() && model.isNotBlank())
        val payload = buildJsonObject {
            put("model", model)
            put("store", false)
            put("stream", true)
            if (instructions.isNotBlank()) put("instructions", instructions)
            put("input", buildJsonArray {
                for (message in messages) {
                    if (message.role !in listOf("user", "assistant", "developer")) continue
                    add(buildJsonObject {
                        put("role", message.role)
                        put("content", message.content)
                    })
                }
            })
        }.toString()
        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "text/event-stream")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val call = http.newCall(request)
        val readerJob = launch(Dispatchers.IO) {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val code = response.body?.string()?.take(8192)?.let { raw ->
                            runCatching {
                                json.parseToJsonElement(raw).jsonObject["error"]
                                    ?.jsonObject?.get("code")?.jsonPrimitive?.contentOrNull
                            }.getOrNull()
                        }.orEmpty()
                        throw ChatGptInferenceException(
                            code.ifBlank { "http_" + response.code },
                            when (code) {
                                "subscription_sharing_usage_limit_exceeded" ->
                                    "ChatGPT 套餐或本应用额度已达到上限，请到 ChatGPT 设置 → Usage 查看。"
                                "subscription_sharing_user_not_eligible" ->
                                    "当前账户或工作区尚不能调用 ChatGPT 套餐。"
                                else -> "ChatGPT 套餐请求被拒绝（HTTP " + response.code + "）。"
                            }
                        )
                    }
                    var completed = false
                    response.body?.charStream()?.buffered()?.use { reader ->
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (!line.startsWith("data: ")) continue
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            val event = runCatching { json.parseToJsonElement(data).jsonObject }.getOrNull()
                                ?: continue
                            when (event["type"]?.jsonPrimitive?.contentOrNull) {
                                "response.output_text.delta" -> {
                                    val text = event["delta"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                    if (text.isNotEmpty()) trySend(ChatGptStreamEvent.Delta(text))
                                }
                                "response.failed" -> {
                                    val body = event["response"] as? JsonObject
                                    val error = body?.get("error") as? JsonObject
                                    val code = error?.get("code")?.jsonPrimitive?.contentOrNull.orEmpty()
                                    throw ChatGptInferenceException(
                                        code.ifBlank { "response_failed" },
                                        if (code == "subscription_sharing_usage_limit_exceeded")
                                            "ChatGPT 套餐或本应用额度已达到上限，请到 ChatGPT 设置 → Usage 查看。"
                                        else "ChatGPT 返回失败，请检查套餐额度和账户授权。"
                                    )
                                }
                                "response.incomplete" -> throw ChatGptInferenceException(
                                    "response_incomplete", "ChatGPT 本轮输出不完整。"
                                )
                                "response.completed" -> {
                                    val result = event["response"] as? JsonObject
                                        ?: throw ChatGptInferenceException("invalid_response", "缺少完成回复。")
                                    val usage = result["usage"] as? JsonObject
                                    val details = usage?.get("input_tokens_details") as? JsonObject
                                    trySend(ChatGptStreamEvent.Completed(
                                        usage?.get("input_tokens")?.jsonPrimitive?.longOrNull,
                                        details?.get("cached_tokens")?.jsonPrimitive?.longOrNull,
                                        usage?.get("output_tokens")?.jsonPrimitive?.longOrNull
                                    ))
                                    completed = true
                                }
                            }
                        }
                    } ?: throw ChatGptInferenceException("empty_stream", "ChatGPT 没有返回可读的模型流。")
                    if (!completed) throw ChatGptInferenceException(
                        "stream_incomplete", "ChatGPT 模型流提前中断，回复不会标记为成功。"
                    )
                }
                close()
            } catch (error: Exception) {
                close(error)
            }
        }
        awaitClose {
            call.cancel()
            readerJob.cancel()
        }
    }
}
