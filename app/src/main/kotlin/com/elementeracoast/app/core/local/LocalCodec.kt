package com.elementeracoast.app.core.local

import java.nio.charset.StandardCharsets
import java.util.Base64

internal object LocalCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun pack(vararg fields: Any?): String = fields.joinToString(".") { field ->
        encoder.encodeToString((field ?: "").toString().toByteArray(StandardCharsets.UTF_8))
    }

    fun unpack(line: String): List<String> = if (line.isBlank()) {
        emptyList()
    } else {
        line.split('.').map { field ->
            runCatching {
                decoder.decode(field).toString(StandardCharsets.UTF_8)
            }.getOrDefault("")
        }
    }

    fun lines(value: String?): List<List<String>> = value.orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .map(::unpack)
        .toList()

    fun encodeList(items: List<String>): String = items.joinToString("\u001f")

    fun decodeList(value: String): List<String> = value
        .split('\u001f')
        .map(String::trim)
        .filter(String::isNotBlank)
}
