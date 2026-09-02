package com.elementeracoast.app.feature.wolf

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatArchiveTest {
    @Test fun jsonExportAndImportRoundTripCurrentFlatMessages() {
        val source = listOf(
            ChatMessage(1, MessageRole.User, "你好\n海岸"),
            ChatMessage(2, MessageRole.Assistant, "我在。")
        )
        val json = ChatArchive.exportJson(WolfProfile("Kryo", "小寒"), source)
        var id = 100L
        val imported = ChatArchive.importJson(json) { id++ }.getOrThrow()
        assertEquals("Kryo", imported.nickname)
        assertEquals("小寒", imported.signature)
        assertEquals(source.map { it.text }, imported.messages.map { it.text })
    }

    @Test fun htmlExportContainsSignatureAndEscapesBody() {
        val html = ChatArchive.exportHtml(WolfProfile("K", "小寒"), listOf(ChatMessage(1, MessageRole.User, "<海>")))
        assertTrue(html.contains("小寒"))
        assertTrue(html.contains("&lt;海&gt;"))
    }
}
