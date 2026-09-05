package com.elementeracoast.app.feature.wolf

import android.content.Context
import com.elementeracoast.app.core.auth.AndroidKeystoreAuthStore
import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.network.CoastHttpClient
import com.elementeracoast.app.core.remote.RemoteProfile
import com.elementeracoast.app.core.remote.RemoteProfileResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Small editor source for chat-profile settings shared by PWA and Native. */
class ProfileSettingsRemoteDataSource(
    private val config: CoastApiConfig,
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }
) {
    suspend fun get(): RemoteProfile = request(
        Request.Builder().url(config.url("/api/chat/profile")).get().build()
    )

    suspend fun putRecentTurns(value: Int): RemoteProfile {
        val current = get()
        val payload = json.encodeToString(RemoteProfile.serializer(), current.copy(recentTurns = value.coerceIn(1, 20)))
        return request(
            Request.Builder()
                .url(config.url("/api/chat/profile"))
                .put(payload.toRequestBody(JSON_MEDIA))
                .build()
        )
    }

    private suspend fun request(request: Request): RemoteProfile = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) throw responseError(response.code, text)
                runCatching { json.decodeFromString(RemoteProfileResponse.serializer(), text).profile }
                    .getOrElse { cause ->
                        throw CoastApiException(
                            CoastApiErrorKind.Decode,
                            "invalid_json",
                            "海岸个人设置返回的数据格式无法读取。",
                            response.code,
                            cause
                        )
                    }
            }
        } catch (error: CoastApiException) {
            throw error
        } catch (error: IOException) {
            throw CoastApiException(CoastApiErrorKind.Network, "network_unreachable", "无法连接海岸后端。", cause = error)
        }
    }

    private fun responseError(status: Int, text: String): CoastApiException {
        var type = if (status == 401) "unauthorized" else "request_failed"
        var message = if (status == 401) "需要先登录海岸。" else "个人设置保存失败（$status）。"
        runCatching {
            val error = json.parseToJsonElement(text).jsonObject["error"]
            if (error is JsonObject) {
                type = error["type"]?.jsonPrimitive?.contentOrNull ?: type
                message = error["message"]?.jsonPrimitive?.contentOrNull ?: message
            } else if (error != null) {
                message = error.jsonPrimitive.contentOrNull ?: message
            }
        }
        val kind = when {
            status == 401 -> CoastApiErrorKind.Unauthorized
            status == 404 -> CoastApiErrorKind.NotFound
            status >= 500 -> CoastApiErrorKind.Server
            else -> CoastApiErrorKind.Request
        }
        return CoastApiException(kind, type, message, status)
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun production(context: Context): ProfileSettingsRemoteDataSource {
            val appContext = context.applicationContext
            val config = CoastApiConfig.production()
            val authStore = AndroidKeystoreAuthStore(appContext)
            val http = CoastHttpClient(config, authStore).client
            return ProfileSettingsRemoteDataSource(config, http)
        }
    }
}
