package com.elementeracoast.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoastSseParserTest {
    @Test fun parsesNamedEventAndJsonData() {
        val parser = CoastSseParser()
        assertNull(parser.accept("event: delta"))
        assertNull(parser.accept("data: {\"content\":\"潮声\"}"))
        val block = parser.accept("")
        assertEquals("delta", block?.event)
        assertEquals("{\"content\":\"潮声\"}", block?.data)
    }

    @Test fun joinsMultipleDataLines() {
        val parser = CoastSseParser()
        parser.accept("event: message")
        parser.accept("data: one")
        parser.accept("data: two")
        assertEquals("one\ntwo", parser.accept("")?.data)
    }
}
