package com.elementeracoast.app.feature.gate

import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastApiException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class MailboxRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var sessions: MemoryMailboxSessionStore
    private lateinit var repository: MailboxRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        sessions = MemoryMailboxSessionStore()
        repository = MailboxRepository(
            config = CoastApiConfig(server.url("/").toString()),
            sessionStore = sessions
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loginStoresMailboxCookieWithoutOwnerSession() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .addHeader(
                    "Set-Cookie",
                    "__Host-coast_mailbox=visitor-token; Max-Age=2592000; Path=/; HttpOnly; Secure; SameSite=Strict"
                )
                .setBody(
                    """{"ok":true,"visitor_id":"visitor-1","display_name":"海鸥","preferred_name":null,"allow_memory":true,"privacy_level":"sealed","session":"secure_cookie"}"""
                )
        )

        val visitor = repository.login("moon-word")

        assertEquals("visitor-1", visitor.visitor_id)
        assertEquals("__Host-coast_mailbox=visitor-token", sessions.load())
        val request = server.takeRequest()
        assertEquals("/api/mailbox/login", request.path)
        assertEquals(server.url("/").toString().trimEnd('/'), request.getHeader("Origin"))
        assertNull(request.getHeader("Cookie"))
    }

    @Test
    fun authenticatedRequestUsesOnlyMailboxCookie() = runBlocking {
        sessions.save("__Host-coast_mailbox=visitor-token")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                    """{"ok":true,"visitor_id":"visitor-1","display_name":"海鸥","preferred_name":"鸥鸥","allow_memory":true,"privacy_level":"sealed"}"""
                )
        )

        val visitor = repository.me()

        assertEquals("鸥鸥", visitor.preferred_name)
        val request = server.takeRequest()
        assertEquals("__Host-coast_mailbox=visitor-token", request.getHeader("Cookie"))
        assertEquals(null, request.getHeader("Origin"))
    }

    @Test
    fun unauthorizedResponseClearsMailboxSession() {
        sessions.save("__Host-coast_mailbox=expired")
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody("""{"ok":false,"error":{"type":"mailbox_session_required","message":"请先输入访客暗号。"}}""")
        )

        assertThrows(CoastApiException::class.java) {
            runBlocking { repository.me() }
        }
        assertNull(sessions.load())
    }
}
