package com.elementeracoast.app.feature.daily

import android.content.Context
import com.elementeracoast.app.core.auth.AndroidKeystoreAuthStore
import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.network.CoastApiErrorKind
import com.elementeracoast.app.core.network.CoastHttpClient
import com.elementeracoast.app.core.network.coastErrorKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder

/**
 * Native counterpart of the Coast PWA calendar, voice and library frontends.
 * Same canonical API, same owner auth cookie, no independent local database.
 */
class SideRoomsRepository(private val config: CoastApiConfig, private val client: okhttp3.OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    private fun path(value: String): String = encode(value)
    private val contentType = "application/json; charset=utf-8".toMediaType()

    private suspend fun request(path: String, method: String = "GET", payload: JsonElement? = null): JsonObject =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(config.url(path))
            when (method) {
                "POST" -> builder.post((payload ?: JsonObject(emptyMap())).toString().toRequestBody(contentType))
                "PATCH" -> builder.patch((payload ?: JsonObject(emptyMap())).toString().toRequestBody(contentType))
                "DELETE" -> builder.delete()
                else -> builder.get()
            }
            try {
                client.newCall(builder.build()).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    val objectValue = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                    if (!response.isSuccessful || objectValue?.get("ok")?.toString() == "false") {
                        val error = objectValue?.get("error") as? JsonObject
                        val type = error?.get("type")?.jsonPrimitive?.contentOrNull ?: "request_failed"
                        val message = error?.get("message")?.jsonPrimitive?.contentOrNull
                            ?: "海岸请求失败（${response.code}）。"
                        throw CoastApiException(coastErrorKind(response.code, type), type, message, response.code)
                    }
                    objectValue ?: throw CoastApiException(CoastApiErrorKind.Decode, "invalid_json", "海岸没有返回可读取的 JSON。")
                }
            } catch (error: CoastApiException) {
                throw error
            } catch (error: IOException) {
                throw CoastApiException(CoastApiErrorKind.Network, "network_unreachable", "无法连接海岸后端。", cause = error)
            }
        }

    suspend fun calendar(month: String): JsonObject = request("/api/calendar/entries?month=${encode(month)}")
    suspend fun saveCalendar(id: String?, input: JsonObject): JsonObject =
        request(id?.let { "/api/calendar/entries/${path(it)}" } ?: "/api/calendar/entries",
            if (id == null) "POST" else "PATCH", input)
    suspend fun deleteCalendar(id: String) { request("/api/calendar/entries/${path(id)}", "DELETE") }

    suspend fun books(): JsonObject = request("/api/library/books")
    suspend fun book(id: String): JsonObject = request("/api/library/books/${path(id)}")
    suspend fun chapter(id: String, index: Int, start: Int = 0): JsonObject =
        request("/api/library/books/${path(id)}/chapters/$index?start=$start&limit=12")
    suspend fun notes(id: String): JsonObject = request("/api/library/books/${path(id)}/notes")
    suspend fun importBook(payload: JsonObject): JsonObject = request("/api/library/books", "POST", payload)
    suspend fun deleteBook(id: String) { request("/api/library/books/${path(id)}", "DELETE") }
    suspend fun progress(id: String, index: Int, paragraph: Int) {
        request("/api/library/books/${path(id)}/progress", "POST",
            kotlinx.serialization.json.buildJsonObject { put("chapter_index", kotlinx.serialization.json.JsonPrimitive(index)); put("paragraph_index", kotlinx.serialization.json.JsonPrimitive(paragraph)) })
    }
    suspend fun writeNote(bookId: String, payload: JsonObject): JsonObject =
        request("/api/library/books/${path(bookId)}/notes", "POST", payload)
    suspend fun editNote(id: String, payload: JsonObject): JsonObject =
        request("/api/library/notes/${path(id)}", "PATCH", payload)
    suspend fun deleteNote(id: String) { request("/api/library/notes/${path(id)}", "DELETE") }

    suspend fun voiceClips(conversationId: String): JsonObject =
        request("/api/voice/clips?conversation_id=${encode(conversationId)}")
    suspend fun generateVoice(payload: JsonObject): JsonObject = request("/api/voice/clips", "POST", payload)
    suspend fun audio(id: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(config.url("/api/voice/clips/${path(id)}/audio")).get().build()).execute().use { response ->
                if (!response.isSuccessful) throw CoastApiException(coastErrorKind(response.code, "voice_unavailable"), "voice_unavailable", "这段声音暂时无法播放。", response.code)
                val bytes = response.body?.bytes() ?: ByteArray(0)
                if (bytes.isEmpty() || bytes.size > 4 * 1024 * 1024) throw CoastApiException(CoastApiErrorKind.Decode, "voice_bytes_invalid", "语音文件不完整或超过 4 MB。")
                bytes
            }
        } catch (error: CoastApiException) { throw error }
        catch (error: IOException) { throw CoastApiException(CoastApiErrorKind.Network, "voice_network_failed", "无法连接海岸声音存储。", cause = error) }
    }

    companion object {
        fun production(context: Context): SideRoomsRepository {
            val app = context.applicationContext
            val config = CoastApiConfig.production()
            return SideRoomsRepository(config, CoastHttpClient(config, AndroidKeystoreAuthStore(app)).client)
        }
    }
}
