package com.elementeracoast.app.core.network

import com.elementeracoast.app.core.auth.AuthSession
import com.elementeracoast.app.core.auth.MemoryAuthStore
import com.elementeracoast.app.core.remote.RemoteChatMessage
import com.elementeracoast.app.core.remote.RemoteChatRequest
import com.elementeracoast.app.core.remote.RemoteHistory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CoastApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var store: MemoryAuthStore
    private lateinit var api: CoastApiClient
    private lateinit var config: CoastApiConfig

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        store = MemoryAuthStore(AuthSession("__Host-coast_session=test-cookie", 4_000_000_000L))
        config = CoastApiConfig(server.url("/").toString().trimEnd('/'))
        api = CoastApiClient(config, CoastHttpClient(config, store).client)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun authenticatedWritesInjectCookieAndOriginWithoutLoggingSecrets() = runBlocking {
        server.enqueue(jsonResponse("""{"ok":true,"conversation":{"id":"c1","title":"新聊天","room_type":"main"}}"""))

        api.createConversation("新聊天", "main")

        val request = server.takeRequest()
        assertEquals("__Host-coast_session=test-cookie", request.getHeader("Cookie"))
        assertEquals(config.origin, request.getHeader("Origin"))
        assertEquals("native-android", request.getHeader("X-Coast-Client"))
        assertEquals("POST", request.method)
        assertEquals("/api/chat/conversations", request.path)
    }

    @Test
    fun profileConversationAndHistoryDecodeCurrentBackendShape() = runBlocking {
        server.enqueue(jsonResponse("""{
          "ok":true,
          "profile":{
            "assistant_avatar_dataurl":"data:image/png;base64,AA==",
            "current_chat_model":"openai/gpt-5.6",
            "current_image_model":"",
            "model_box":{"chat":["openai/gpt-5.6"],"free":[],"image":[]}
          }
        }"""))
        server.enqueue(jsonResponse("""{
          "ok":true,
          "conversations":[{"id":"c1","title":"PWA 窗口","room_type":"radio","updated_at":"2026-09-02T10:00:00Z"}]
        }"""))
        server.enqueue(jsonResponse("""{
          "ok":true,
          "source":"d1-json-v4",
          "history":{
            "version":4,
            "conversation_id":"c1",
            "updated_at":"2026-09-02T10:00:00Z",
            "turns":[{
              "id":"t1",
              "user":{"active":0,"variants":[{"id":"u1","content":"hello","created_at":"2026-09-02T10:00:00Z"}]},
              "assistant":{"activeByUserVariant":{"0":0},"variantsByUserVariant":{"0":[{"id":"a1","content":"hi","created_at":"2026-09-02T10:00:01Z","model_id":"openai/gpt-5.6"}]}}
            }]
          }
        }"""))

        val profile = api.getProfile()
        val conversations = api.listConversations()
        val history = api.getHistory("c1")

        assertEquals("openai/gpt-5.6", profile.currentChatModel)
        assertEquals(listOf("openai/gpt-5.6"), profile.modelBox.chat)
        assertEquals("PWA 窗口", conversations.single().title)
        assertEquals("radio", conversations.single().roomType)
        assertEquals("hello", history.turns.single().user.variants.single().content)
        assertEquals("hi", history.turns.single().assistant.variantsByUserVariant.getValue("0").single().content)
    }

    @Test
    fun historyPutUsesSharedV4EndpointAndOrigin() = runBlocking {
        server.enqueue(jsonResponse("""{"ok":true,"source":"d1-json-v4","history":{"version":4,"conversation_id":"c1","updated_at":"","turns":[]}}"""))

        api.putHistory("c1", RemoteHistory(version = 4, conversationId = "c1"))

        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/api/chat/history?conversation_id=c1"))
        assertEquals("PUT", request.method)
        assertEquals(config.origin, request.getHeader("Origin"))
        assertTrue(request.body.readUtf8().contains("\"version\":4"))
    }

    @Test
    fun chatSseDecodesDeltaDoneFurnitureAndErrorEvents() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    "event: meta\ndata: {\"model\":\"openai/gpt-5.6\"}\n\n" +
                        "event: delta\ndata: {\"content\":\"海\"}\n\n" +
                        "event: delta\ndata: {\"content\":\"岸\"}\n\n" +
                        "event: furniture_runs\ndata: []\n\n" +
                        "event: done\ndata: {\"finish_reason\":\"stop\"}\n\n"
                )
        )
        val request = chatRequest()

        val events = api.streamChat(request).toList()

        assertTrue(events[0] is ApiStreamEvent.Meta)
        assertEquals(listOf("海", "岸"), events.filterIsInstance<ApiStreamEvent.Delta>().map { it.text })
        assertTrue(events.any { it is ApiStreamEvent.FurnitureRuns })
        assertEquals("stop", events.filterIsInstance<ApiStreamEvent.Done>().single().finishReason)
        val recorded = server.takeRequest()
        assertEquals("text/event-stream", recorded.getHeader("Accept"))
        assertEquals(config.origin, recorded.getHeader("Origin"))
    }

    @Test
    fun chatSseBackpressurePreservesDoneFurnitureAndDeskAfterManyDeltas() = runBlocking {
        val deltas = (0 until 160).joinToString(separator = "") { index ->
            "event: delta\ndata: {\"content\":\"$index,\"}\n\n"
        }
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    "event: meta\ndata: {\"model\":\"openai/gpt-5.6\"}\n\n" +
                        deltas +
                        "event: done\ndata: {\"finish_reason\":\"stop\"}\n\n" +
                        "event: furniture_runs\ndata: [{\"id\":\"run-1\",\"label\":\"记忆搜索\"}]\n\n" +
                        "event: desk_slip\ndata: {\"summary\":\"本轮递给模型\",\"comfort\":\"已保持在舒服区间\"}\n\n"
                )
        )

        val events = api.streamChat(chatRequest())
            .onEach { delay(2) }
            .toList()

        val receivedDeltas = events.filterIsInstance<ApiStreamEvent.Delta>()
        assertEquals(160, receivedDeltas.size)
        assertEquals("159,", receivedDeltas.last().text)
        assertEquals("stop", events.filterIsInstance<ApiStreamEvent.Done>().single().finishReason)
        assertEquals(1, events.filterIsInstance<ApiStreamEvent.FurnitureRuns>().size)
        assertEquals(1, events.filterIsInstance<ApiStreamEvent.DeskSlip>().size)
    }

    @Test
    fun structuredBackendErrorKeepsTypeStatusAndMessage() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"ok":false,"error":{"type":"chat_db_not_configured","message":"主聊天 D1 存储未配置。"}}""")
        )

        val error = runCatching { api.listConversations() }.exceptionOrNull() as CoastApiException

        assertEquals(CoastApiErrorKind.Server, error.kind)
        assertEquals("chat_db_not_configured", error.type)
        assertEquals(503, error.status)
        assertEquals("主聊天 D1 存储未配置。", error.message)
    }

    private fun chatRequest() = RemoteChatRequest(
        conversationId = "c1",
        sourceTurnId = "t1",
        model = "openai/gpt-5.6",
        messages = listOf(RemoteChatMessage("user", "hello")),
        localDate = "2026-09-02",
        localDateTime = "2026-09-02 20:00"
    )

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
