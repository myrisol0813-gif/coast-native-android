package com.elementeracoast.app.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoastAppearanceTest {
    @Test
    fun accentPresetsParseAsArgbWithoutPackedColorCrash() {
        val presets = mapOf(
            "#ff6a21" to 0xFFFF6A21.toInt(),
            "#f28b2e" to 0xFFF28B2E.toInt(),
            "#3b82f6" to 0xFF3B82F6.toInt(),
            "#ec4899" to 0xFFEC4899.toInt()
        )

        presets.forEach { (hex, expectedArgb) ->
            assertEquals(expectedArgb, requireNotNull(parseCoastHex(hex)).toArgb())
        }
    }

    @Test
    fun invalidHexReturnsNull() {
        assertNull(parseCoastHex("#xyz"))
        assertNull(parseCoastHex(""))
    }
}
