package com.elementeracoast.app.feature.serpentdesk

import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.remote.RemoteDevRunsResponse
import com.elementeracoast.app.core.remote.RemoteDevSelfCheckResponse
import com.elementeracoast.app.core.remote.RemoteDevToolsResponse
import com.elementeracoast.app.core.remote.RemoteDevUpdateResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class DevHandsException(
    val type: String,
    override val message: String,
    val status: Int = 0,
    cause: Throwable? = null
) : Exception(message, cause)

interface DevHandsRepository {
    suspend fun selfCheck(): RemoteDevSelfCheckResponse
    suspend fun tools(): RemoteDevToolsResponse
    suspend fun latestUpdate(): RemoteDevUpdateResponse
    suspend fun logs(limit: Int = 100): RemoteDevRunsResponse
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
    override suspend fun selfCheck(): RemoteDevSelfCheckResponse =
        request(
            Request.Builder().url(config.url("/api/workbench/dev/self-check")).get().build(),
            RemoteDevSelfCheckResponse.serializer()
        )

    override suspend fun tools(): RemoteDevToolsResponse =
        request(
            Request.Builder().url(config.url("/api/workbench/tools?surface=main_chat")).get().build(),
            RemoteDevToolsResponse.serializer()
        )

    override suspend fun latestUpdate(): RemoteDevUpdateResponse =
        request(
            Request.Builder().url(config.url("/api/workbench/dev/update")).get().build(),
            RemoteDevUpdateResponse.serializer()
        )

    override suspend fun logs(limit: Int): RemoteDevRunsResponse {
        val safeLimit = limit.coerceIn(1, 200)
        return request(
            Request.Builder().url(config.url("/api/workbench/dev/logs?limit=$safeLimit")).get().build(),
            RemoteDevRunsResponse.serializer()
        )
    }

    override fun absoluteUrl(path: String): String = config.url(path)

    private suspend fun <T> request(request: Request, serializer: KSerializer<T>): T = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw responseError(response.code, text)
                runCatching { json.decodeFromString(serializer, text) }.getOrElse { cause ->
                    throw CoastApiException(
                        CoastApiErrorKind.Decode,
                        "invalid_json",
                        "开发手观察窗返回的数据格式无法读取。",
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
        var message = if (status == 401) "登录状态已失效。" else "开发手观察请求失败（$status）。"
        runCatching {
            val error = json.parseToJsonElement(text).jsonObject["error"]?.jsonObject ?: return@runCatching
            type = error["type"]?.jsonPrimitive?.contentOrNull ?: type
            message = error["message"]?.jsonPrimitive?.contentOrNull ?: message
        }
        return DevHandsException(type = type, message = message, status = status)
    }
}
