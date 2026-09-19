package com.elementeracoast.app.feature.serpentdesk

import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.network.coastErrorKind
import com.elementeracoast.app.core.remote.RemoteDevRunsResponse
import com.elementeracoast.app.core.remote.RemoteDevSelfCheckResponse
import com.elementeracoast.app.core.remote.RemoteDevUpdateResponse
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class NativeApkDownload(
    val filename: String,
    val bytes: ByteArray,
    val sha256: String
)

interface DevHandsRepository {
    suspend fun selfCheck(): RemoteDevSelfCheckResponse
    suspend fun latestUpdate(): RemoteDevUpdateResponse
    suspend fun logs(limit: Int = 100): RemoteDevRunsResponse
    suspend fun downloadApk(path: String, expectedSha256: String? = null): NativeApkDownload
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

    override suspend fun latestUpdate(): RemoteDevUpdateResponse =
        request(
            Request.Builder().url(config.url("/api/workbench/dev/update")).get().build(),
            RemoteDevUpdateResponse.serializer()
        )

    override suspend fun logs(limit: Int): RemoteDevRunsResponse {
        val requested = limit.coerceAtLeast(1)
        return request(
            Request.Builder().url(config.url("/api/workbench/dev/logs?limit=$requested")).get().build(),
            RemoteDevRunsResponse.serializer()
        )
    }

    override suspend fun downloadApk(path: String, expectedSha256: String?): NativeApkDownload =
        withContext(Dispatchers.IO) {
            try {
                client.newCall(Request.Builder().url(config.url(path)).get().build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        val text = response.body?.string().orEmpty()
                        throw responseError(response.code, text)
                    }
                    val bytes = response.body?.bytes()
                        ?: throw CoastApiException(
                            CoastApiErrorKind.Decode,
                            "apk_body_missing",
                            "海岸没有返回 APK 文件。",
                            response.code
                        )
                    if (bytes.isEmpty()) {
                        throw CoastApiException(
                            CoastApiErrorKind.Decode,
                            "apk_body_empty",
                            "海岸返回的 APK 是空文件。",
                            response.code
                        )
                    }
                    if (bytes.size > MAX_APK_BYTES) {
                        throw CoastApiException(
                            CoastApiErrorKind.Decode,
                            "apk_too_large",
                            "APK 超过 Native 下载保护上限。",
                            response.code
                        )
                    }
                    val actual = sha256(bytes)
                    val expected = expectedSha256
                        ?.removePrefix("sha256:")
                        ?.trim()
                        ?.lowercase()
                        ?.takeIf(String::isNotBlank)
                    val headerSha = response.header("X-Coast-APK-SHA256")
                        ?.removePrefix("sha256:")
                        ?.trim()
                        ?.lowercase()
                        ?.takeIf(String::isNotBlank)
                    if ((expected != null && expected != actual) || (headerSha != null && headerSha != actual)) {
                        throw CoastApiException(
                            CoastApiErrorKind.Decode,
                            "apk_checksum_mismatch",
                            "APK SHA-256 校验失败，已停止安装。",
                            response.code
                        )
                    }
                    NativeApkDownload(
                        filename = response.header("Content-Disposition")
                            ?.let(::filenameFromContentDisposition)
                            ?.takeIf(String::isNotBlank)
                            ?: "Elementera-Coast-update.apk",
                        bytes = bytes,
                        sha256 = actual
                    )
                }
            } catch (error: CoastApiException) {
                throw error
            } catch (error: IOException) {
                throw CoastApiException(
                    CoastApiErrorKind.Network,
                    "network_unreachable",
                    "APK 下载时无法连接海岸后端。",
                    cause = error
                )
            }
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
        } catch (error: CoastApiException) {
            throw error
        } catch (error: IOException) {
            throw CoastApiException(CoastApiErrorKind.Network, "network_unreachable", "无法连接海岸后端。", cause = error)
        }
    }

    private fun filenameFromContentDisposition(value: String): String? {
        val raw = Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE)
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: return null
        return raw.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("""[^A-Za-z0-9._()\-]+"""), "_")
            .take(160)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

    private fun responseError(status: Int, text: String): CoastApiException {
        var type = if (status == 401) "unauthorized" else "request_failed"
        var message = if (status == 401) "登录状态已失效。" else "开发手观察窗请求失败（$status）。"
        runCatching {
            val error = json.parseToJsonElement(text).jsonObject["error"]?.jsonObject ?: return@runCatching
            type = error["type"]?.jsonPrimitive?.contentOrNull ?: type
            message = error["message"]?.jsonPrimitive?.contentOrNull ?: message
        }
        return CoastApiException(
            kind = coastErrorKind(status, type),
            type = type,
            message = message,
            status = status
        )
    }

    private companion object {
        const val MAX_APK_BYTES = 80 * 1024 * 1024
    }
}
