package com.elementeracoast.app.feature.serpentdesk

import com.elementeracoast.app.core.auth.AuthSession
import com.elementeracoast.app.core.auth.MemoryAuthStore
import com.elementeracoast.app.core.network.CoastApiConfig
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.core.network.CoastHttpClient
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DevHandsRepositoryTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun authenticatedApkDownloadCarriesOwnerCookieAndVerifiesSha256() = runBlocking {
        val bytes = "coast-apk-binary".encodeToByteArray()
        val sha = sha256(bytes)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/vnd.android.package-archive")
                .setHeader("Content-Disposition", """attachment; filename="CoastGPT-native-test.apk"""")
                .setHeader("X-Coast-APK-SHA256", sha)
                .setBody(okio.Buffer().write(bytes))
        )
        val repository = repository()

        val downloaded = repository.downloadApk(
            "/api/workbench/dev/update/apk?release_asset_id=50",
            sha
        )

        assertEquals("CoastGPT-native-test.apk", downloaded.filename)
        assertEquals(sha, downloaded.sha256)
        assertArrayEquals(bytes, downloaded.bytes)
        val request = server.takeRequest()
        assertEquals("__Host-coast_session=owner-cookie", request.getHeader("Cookie"))
        assertEquals("/api/workbench/dev/update/apk?release_asset_id=50", request.path)
        assertEquals("native-android", request.getHeader("X-Coast-Client"))
    }

    @Test
    fun apkDownloadStopsBeforeInstallWhenExpectedChecksumDoesNotMatch() = runBlocking {
        val bytes = "coast-apk-binary".encodeToByteArray()
        val actualSha = sha256(bytes)
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/vnd.android.package-archive")
                .setHeader("X-Coast-APK-SHA256", actualSha)
                .setBody(okio.Buffer().write(bytes))
        )
        val repository = repository()

        val error = runCatching {
            repository.downloadApk(
                "/api/workbench/dev/update/apk?release_asset_id=50",
                "0".repeat(64)
            )
        }.exceptionOrNull() as CoastApiException

        assertEquals("apk_checksum_mismatch", error.type)
        assertTrue(error.message!!.contains("SHA-256"))
    }

    private fun repository(): DevHandsRepository {
        val store = MemoryAuthStore(AuthSession("__Host-coast_session=owner-cookie", 0L))
        val config = CoastApiConfig(server.url("/").toString().trimEnd('/'))
        val client = CoastHttpClient(config, store).client
        return DefaultDevHandsRepository(config, client)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
}
