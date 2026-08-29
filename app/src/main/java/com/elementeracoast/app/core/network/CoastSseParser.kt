package com.elementeracoast.app.core.network

data class SseBlock(val event: String, val data: String)

class CoastSseParser {
    private var event = "message"
    private val data = mutableListOf<String>()

    fun accept(line: String?): SseBlock? {
        if (line == null || line.isEmpty()) {
            if (data.isEmpty()) {
                reset()
                return null
            }
            val block = SseBlock(event, data.joinToString("\n"))
            reset()
            return block
        }
        if (line.startsWith(":")) return null
        val separator = line.indexOf(':')
        val field = if (separator < 0) line else line.substring(0, separator)
        var value = if (separator < 0) "" else line.substring(separator + 1)
        if (value.startsWith(" ")) value = value.substring(1)
        when (field) {
            "event" -> event = value.ifBlank { "message" }
            "data" -> data += value
        }
        return null
    }

    private fun reset() {
        event = "message"
        data.clear()
    }
}
