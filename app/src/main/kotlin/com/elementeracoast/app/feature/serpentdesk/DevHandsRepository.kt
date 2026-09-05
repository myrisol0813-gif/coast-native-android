package com.elementeracoast.app.feature.serpentdesk

import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.remote.RemoteDevActionRequest
import com.elementeracoast.app.core.remote.RemoteDevActionResponse
import com.elementeracoast.app.core.remote.RemoteDevRunsResponse
import com.elementeracoast.app.core.remote.RemoteDevSelfCheckResponse
import com.elementeracoast.app.core.remote.RemoteDevSettings
import com.elementeracoast.app.core.remote.RemoteDevSettingsResponse
import com.elementeracoast.app.core.remote.RemoteDevUpdateResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DevHandsException(
    val type: String,
    override val message: String,
    val status: Int = 0,
    val confirmationText: String? = null,
    val operationType: String? = null,
    val actionLabel: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

interface DevHandsRepository {
    suspend fun settings(): RemoteDevSettings
    suspend fun saveSettings(value: RemoteDevSettings): RemoteDevSettings
    suspend fun selfCheck(): RemoteDevSelfCheckResponse
    suspend fun latestUpdate(): RemoteDevUpdateResponse
    suspend fun logs(limit: Int = 100): RemoteDevRunsResponse
    suspend fun github(action: String, params: JsonObject, confirmationText: String? = null): RemoteDevActionResponse
    suspend fun notion(action: String, params: JsonObject, confirmationText: String? = null): RemoteDevActionResponse
    fun absoluteUrl(path: String): String
}

class DefaultDevHandsRepository(
    private val config: CoastApiConfig,
    private val client: OkHttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        prettyPrint = false
    }
) : DevHandsRepository {
    override suspend fun settings(): RemoteDevSettings =
        request(Request.Builder().url(config.url("/api/workbench/dev/settings")).get().build(), RemoteDevSettingsResponse.serializer()).settings

    override suspend fun saveSettings(value: RemoteDevSettings): RemoteDevSettings {
        val payload = buildJsonObject {
            putJsonObject("settings") {
                put("github_read", value.githubRead)
                put("github_write", value.githubWrite)
                put("github_dangerous", value.githubDangerous)
                put("ci_actions", value.ciActions)
                put("apk_artifact", value.apkArtifact)
                put("wolf_update", value.wolfUpdate)
                put("notion_read", value.notionRead)
                put("notion_write", value.notionWrite)
                put("notion_delete", value.notionDelete)
            }
        }
        return request(
            Request.Builder().url(config.url("/api/workbench/dev/settings")).put(jsonBody(payload.toString())).build(),
            RemoteDevSettingsResponse.serializer()
        ).settings
    }

    override suspend fun selfCheck(): RemoteDevSelfCheckResponse =
        request(Request.Builder().url(config.url("/api/workbench/dev/self-check")).get().build(), RemoteDevSelfCheckResponse.serializer())

    override suspend fun latestUpdate(): RemoteDevUpdateResponse =
        request(Request.Builder().url(config.url("/api/workbench/dev/update")).get().build(), RemoteDevUpdateResponse.serializer())

    override suspend fun logs(limit: Int): RemoteDevRunsResponse {
        val safeLimit = limit.coerceIn(1, 200)
        return request(
            Request.Builder().url(config.url("/api/workbench/dev/logs?limit=$safeLimit")).get().build(),
            RemoteDevRunsResponse.serializer()
        )
    }

    override suspend fun github(action: String, params: JsonObject, confirmationText: String?): RemoteDevActionResponse =
        actionRequest("/api/workbench/dev/github", action, params, confirmationText)

    override suspend fun notion(action: String, params: JsonObject, confirmationText: String?): RemoteDevActionResponse =
        actionRequest("/api/workbench/dev/notion", action, params, confirmationText)

    override fun absoluteUrl(path: String): String = config.url(path)

    private suspend fun actionRequest(
        path: String,
        action: String,
        params: JsonObject,
        confirmationText: String?
    ): RemoteDevActionResponse {
        val payload = json.encodeToString(
            RemoteDevActionRequest.serializer(),
            RemoteDevActionRequest(action = action, params = params, confirmText = confirmationText)
        )
        return request(
            Request.Builder().url(config.url(path)).post(jsonBody(payload)).build(),
            RemoteDevActionResponse.serializer()
        )
    }

    private suspend fun <T> request(request: Request, serializer: KSerializer<T>): T = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw responseError(response.code, text)
                runCatching { json.decodeFromString(serializer, text) }.getOrElse { cause ->
                    throw CoastApiException(
                        CoastApiErrorKind.Decode,
                        "invalid_json",
                        "施工台返回的数据格式无法读取。",
                        response.code,
                        cause
                    )
                }
            }
        } catch (error: DevHandsException) {
            throw error
        } catch (error: CoastApiException) {
            throw error
        } catch (error: IOException) {
            throw CoastApiException(CoastApiErrorKind.Network, "network_unreachable", "无法连接海岸后端。", cause = error)
        }
    }

    private fun responseError(status: Int, text: String): DevHandsException {
        var type = if (status == 401) "unauthorized" else "request_failed"
        var message = if (status == 401) "登录状态已失效。" else "施工台请求失败（$status）。"
        var confirmation: String? = null
        var operation: String? = null
        var actionLabel: String? = null
        runCatching {
            val error = json.parseToJsonElement(text).jsonObject["error"]?.jsonObject ?: return@runCatching
            type = error["type"]?.jsonPrimitive?.contentOrNull ?: type
            message = error["message"]?.jsonPrimitive?.contentOrNull ?: message
            confirmation = error["confirmation_text"]?.jsonPrimitive?.contentOrNull
            operation = error["operation_type"]?.jsonPrimitive?.contentOrNull
            actionLabel = error["action_label"]?.jsonPrimitive?.contentOrNull
        }
        return DevHandsException(
            type = type,
            message = message,
            status = status,
            confirmationText = confirmation,
            operationType = operation,
            actionLabel = actionLabel
        )
    }

    private fun jsonBody(value: String) = value.toRequestBody(JSON_MEDIA_TYPE)

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
