package com.elementeracoast.app.feature.wolf

import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageRole

object ChatArchive {
    data class ImportedArchive(val nickname: String, val signature: String, val messages: List<ChatMessage>)

    fun exportJson(profile: WolfProfile, messages: List<ChatMessage>): String {
        val rows = messages.joinToString(",\n") { message ->
            """    {"role":"${if (message.role == MessageRole.User) "user" else "assistant"}","content":"${json(message.text)}"}"""
        }
        return """{
  "format":"elementera-chat-export",
  "version":"4",
  "display_profile":{"nickname":"${json(profile.nickname)}","signature":"${json(profile.signature)}"},
  "messages":[
$rows
  ]
}"""
    }

    fun exportHtml(profile: WolfProfile, messages: List<ChatMessage>): String {
        val rows = messages.joinToString("\n") { message ->
            val author = if (message.role == MessageRole.User) profile.signature else "Myri"
            val kind = if (message.role == MessageRole.User) "user" else "assistant"
            "<article class=\"m $kind\"><b>${html(author)}</b><div>${html(message.text).replace("\n", "<br>")}</div></article>"
        }
        return """<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Elementera Chat Export</title><meta name="elementera-display-name" content="${html(profile.signature)}"><style>body{font-family:system-ui,sans-serif;line-height:1.65}.w{max-width:820px;margin:auto;padding:22px 14px}.m{margin:0 0 18px}.m b{display:block;color:#777}.m div{display:inline-block;max-width:88%;padding:10px 14px;border:1px solid #ddd;border-radius:18px}.user{text-align:right}.user div{background:#f1f1f1}</style><div class="w"><h1>Elementera Chat Export</h1>$rows</div>"""
    }

    fun importJson(raw: String, nextId: () -> Long): Result<ImportedArchive> = runCatching {
        val format = Regex("\\\"format\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(raw)?.groupValues?.get(1)
        if (format != null && format != "elementera-chat-export") error("不认识的导出格式")
        val nickname = decodeJsonString(Regex("\\\"nickname\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").find(raw)?.groupValues?.get(1).orEmpty()).ifBlank { "小寒" }
        val signature = decodeJsonString(Regex("\\\"signature\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").find(raw)?.groupValues?.get(1).orEmpty()).ifBlank { nickname }
        val messageRegex = Regex("\\{\\s*\\\"role\\\"\\s*:\\s*\\\"(user|assistant)\\\"\\s*,\\s*\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*}")
        val messages = messageRegex.findAll(raw).map { match ->
            ChatMessage(
                id = nextId(),
                role = if (match.groupValues[1] == "user") MessageRole.User else MessageRole.Assistant,
                text = decodeJsonString(match.groupValues[2]),
                modelId = if (match.groupValues[1] == "assistant") "Imported local" else null,
                generationSource = if (match.groupValues[1] == "assistant") "import" else null
            )
        }.toList()
        if (messages.isEmpty()) error("没有找到 messages")
        ImportedArchive(nickname, signature, messages)
    }

    private fun json(value: String): String = buildString {
        value.forEach { char ->
            append(when (char) {
                '\\' -> "\\\\"
                '"' -> "\\\""
                '\n' -> "\\n"
                '\r' -> "\\r"
                '\t' -> "\\t"
                else -> char
            })
        }
    }

    private fun decodeJsonString(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")

    private fun html(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
