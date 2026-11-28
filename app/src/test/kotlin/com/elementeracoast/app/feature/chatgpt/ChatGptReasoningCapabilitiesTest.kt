package com.elementeracoast.app.feature.chatgpt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatGptReasoningCapabilitiesTest {
    @Test fun readsOnlyProviderDeclaredEffortsInStandardOrder() {
        val catalogItem = Json.parseToJsonElement(
            """{"slug":"test","supported_reasoning_efforts":["high","low","invented","medium"]}"""
        ).jsonObject
        assertEquals(listOf("low", "medium", "high"), decodeChatGptReasoningEfforts(catalogItem))
    }

    @Test fun unsupportedOrMissingEffortsStayUnavailable() {
        val generic = Json.parseToJsonElement(
            """{"slug":"test","supported_parameters":["reasoning"]}"""
        ).jsonObject
        assertEquals(emptyList<String>(), decodeChatGptReasoningEfforts(generic))
    }
    @Test fun visionIsAvailableOnlyWhenExplicitlyAdvertised() {
        fun decode(value:String)=decodeChatGptVision(Json.parseToJsonElement(value).jsonObject)
        assertEquals(true,decode("""{"slug":"fixture","input_modalities":["text","image"]}"""))
        assertEquals(false,decode("""{"slug":"fixture","input_modalities":["text"]}"""))
        assertEquals(true,decode("""{"slug":"fixture","architecture":{"input_modalities":["image"]}}"""))
        assertEquals(null,decode("""{"slug":"gpt-6.2-sol"}"""))
        assertEquals(null,decode("""{"input_modalities":[]}"""))
    }
}
