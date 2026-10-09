package com.elementeracoast.app.feature.chatgpt

import com.elementeracoast.app.core.network.CoastApiClient
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.remote.RemoteChatRequest
import com.elementeracoast.app.core.remote.RemoteDeskSlip
import com.elementeracoast.app.core.remote.RemoteFurnitureRun
import com.elementeracoast.app.core.remote.RemoteNativeChatContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal sealed interface NativeChatEvent {
    data class Delta(val text: String) : NativeChatEvent
    data class Tool(val id: String, val name: String, val ok: Boolean) : NativeChatEvent
    data class Completed(
        val text: String,
        val usage: NativeChatUsage,
        val deskSlip: RemoteDeskSlip,
        val furnitureRuns: List<RemoteFurnitureRun>
    ) : NativeChatEvent
}

internal data class NativeChatUsage(
    val input: Long = 0,
    val cached: Long = 0,
    val output: Long = 0,
    val reported: Boolean = false
)

/** All inference is local-to-OpenAI; Coast receives neither OAuth access nor refresh tokens. */
internal class ChatGptNativeInference(
    private val account: ChatGptPlanRepository,
    private val coast: CoastApiClient,
    client: OkHttpClient = OkHttpClient()
) {
    private val http = client.newBuilder().readTimeout(120, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    fun stream(
        model: String,
        request: RemoteChatRequest,
        prepared: RemoteNativeChatContext
    ): Flow<NativeChatEvent> = callbackFlow {
        val activeCall = AtomicReference<Call?>()
        val job = launch(Dispatchers.IO) {
            try {
                val access = account.accessTokenForInference()
                val instructions = prepared.modelMessages.mapNotNull { message ->
                    val obj = message.jsonObject
                    obj["content"]?.jsonPrimitive?.contentOrNull?.takeIf {
                        obj["role"]?.jsonPrimitive?.contentOrNull in listOf("system", "developer")
                    }
                }.joinToString("\n\n")
                val history: MutableList<JsonElement> = prepared.modelMessages.mapNotNull { message ->
                    val obj = message.jsonObject
                    if (obj["role"]?.jsonPrimitive?.contentOrNull in listOf("system", "developer")) null
                    else normalizeInput(obj)
                }.toMutableList()
                val text = StringBuilder()
                val runs = mutableListOf<RemoteFurnitureRun>()
                var desk = prepared.deskSlip
                var usageTotal = NativeChatUsage()
                var callsMade = 0

                for (round in 0..12) {
                    val body = buildJsonObject {
                        put("model", model)
                        put("input", JsonArray(history))
                        put("store", false)
                        put("stream", true)
                        if (instructions.isNotBlank()) put("instructions", instructions)
                        if (prepared.tools.isNotEmpty()) put("tools", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "namespace")
                                put("name", "coast")
                                put("description", "海岸获准的记忆、日记、跨窗口与开发工具")
                                put("tools", prepared.tools)
                            })
                        })
                    }
                    val call = http.newCall(Request.Builder()
                        .url("https://api.openai.com/v1/responses")
                        .header("Authorization", "Bearer " + access)
                        .header("Accept", "text/event-stream")
                        .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build())
                    activeCall.set(call)
                    var completed: JsonObject? = null
                    call.execute().use { response ->
                        if (!response.isSuccessful) throw CoastApiException(
                            CoastApiErrorKind.Model,
                            "chatgpt_plan_http_" + response.code,
                            "官端 GPT 未接受请求（HTTP " + response.code + "），请检查套餐额度、模型和功能权限。",
                            response.code
                        )
                        response.body?.charStream()?.buffered()?.use { reader ->
                            while (true) {
                                val line = reader.readLine() ?: break
                                if (!line.startsWith("data: ")) continue
                                val raw = line.removePrefix("data: ").trim()
                                if (raw == "[DONE]") break
                                val event = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                                    ?: continue
                                when (event["type"]?.jsonPrimitive?.contentOrNull) {
                                    "response.output_text.delta" -> {
                                        val delta = event["delta"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                        text.append(delta)
                                        trySend(NativeChatEvent.Delta(delta))
                                    }
                                    "response.completed" -> completed = event["response"] as? JsonObject
                                    "response.failed" -> {
                                        val error = (event["response"] as? JsonObject)?.get("error") as? JsonObject
                                        val code = error?.get("code")?.jsonPrimitive?.contentOrNull.orEmpty()
                                        throw CoastApiException(
                                            CoastApiErrorKind.Model,
                                            code.ifBlank { "chatgpt_plan_failed" }.take(80),
                                            "官端 GPT 返回失败；请检查账户额度或模型权限。"
                                        )
                                    }
                                    "response.incomplete" -> throw CoastApiException(
                                        CoastApiErrorKind.Stream, "chatgpt_plan_incomplete", "模型回复未完整结束。"
                                    )
                                }
                            }
                        }
                    }
                    activeCall.set(null)
                    val response = completed ?: throw CoastApiException(
                        CoastApiErrorKind.Stream, "chatgpt_plan_incomplete", "官端 GPT 回复流提前中断。"
                    )
                    val usage = response["usage"] as? JsonObject
                    val detail = usage?.get("input_tokens_details") as? JsonObject
                    usageTotal = usageTotal.copy(
                        input = usageTotal.input + (usage?.get("input_tokens")?.jsonPrimitive?.longOrNull ?: 0),
                        cached = usageTotal.cached + (detail?.get("cached_tokens")?.jsonPrimitive?.longOrNull ?: 0),
                        output = usageTotal.output + (usage?.get("output_tokens")?.jsonPrimitive?.longOrNull ?: 0),
                        reported = usageTotal.reported || usage != null
                    )
                    val output = response["output"] as? JsonArray ?: JsonArray(emptyList())
                    val pending = output.mapNotNull { it as? JsonObject }.filter {
                        it["type"]?.jsonPrimitive?.contentOrNull == "function_call"
                    }
                    if (pending.isEmpty()) {
                        if (text.isBlank()) throw CoastApiException(
                            CoastApiErrorKind.Model, "chatgpt_plan_empty", "官端 GPT 没有返回正文。"
                        )
                        trySend(NativeChatEvent.Completed(text.toString(), usageTotal, desk, runs))
                        break
                    }
                    if (round == 12 || callsMade + pending.size > 12) throw CoastApiException(
                        CoastApiErrorKind.Model, "tool_limit", "这轮的工具调用超过 12 次。"
                    )
                    history.addAll(output)
                    for (tool in pending) {
                        val callId = tool["call_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                        val name = tool["name"]?.jsonPrimitive?.contentOrNull.orEmpty().removePrefix("coast.")
                        if (callId.isBlank() || name.isBlank()) throw CoastApiException(
                            CoastApiErrorKind.Decode, "invalid_tool_call", "官端 GPT 工具调用信息不完整。"
                        )
                        val params = buildJsonObject {
                            put("id", callId)
                            put("type", "function")
                            put("function", buildJsonObject {
                                put("name", name)
                                put("arguments", tool["arguments"]?.jsonPrimitive?.contentOrNull ?: "{}")
                            })
                        }
                        val result = coast.executeNativeChatGptTool(request, params, callsMade++)
                        runs.addAll(result.furnitureRuns)
                        desk = result.deskSlip
                        trySend(NativeChatEvent.Tool(callId, name, result.ok))
                        history.add(buildJsonObject {
                            put("type", "function_call_output")
                            put("call_id", callId)
                            put("output", result.output)
                        })
                    }
                }
            } catch (error: Exception) {
                close(if (error is CoastApiException) error else CoastApiException(
                    CoastApiErrorKind.Network,
                    "chatgpt_plan_network",
                    "官端 GPT 网络请求未完成。",
                    cause = error
                ))
                return@launch
            } finally {
                activeCall.getAndSet(null)?.cancel()
                close()
            }
        }
        awaitClose { activeCall.getAndSet(null)?.cancel(); job.cancel() }
    }

    private fun normalizeInput(obj: JsonObject): JsonObject {
        val content = obj["content"]
        if (content !is JsonArray) return obj
        return buildJsonObject {
            put("role", obj["role"] ?: JsonPrimitive("user"))
            put("content", buildJsonArray {
                content.forEach { part ->
                    val item = part.jsonObject
                    when (item["type"]?.jsonPrimitive?.contentOrNull) {
                        "text" -> add(buildJsonObject {
                            put("type", "input_text")
                            put("text", item["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
                        })
                        "image_url" -> add(buildJsonObject {
                            put("type", "input_image")
                            put("image_url", item["image_url"]?.jsonObject
                                ?.get("url")?.jsonPrimitive?.contentOrNull.orEmpty())
                        })
                        else -> throw CoastApiException(
                            CoastApiErrorKind.Request,
                            "unsupported_native_input",
                            "此附件格式不被官端 GPT 认可。"
                        )
                    }
                }
            })
        }
    }
}
