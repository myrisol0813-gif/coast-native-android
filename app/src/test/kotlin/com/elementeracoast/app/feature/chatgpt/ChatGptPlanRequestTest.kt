package com.elementeracoast.app.feature.chatgpt

import com.elementeracoast.app.core.remote.RemoteChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class ChatGptPlanRequestTest {
    private val history = listOf(
        RemoteChatMessage("user", "第一轮"),
        RemoteChatMessage("assistant", "记得"),
        RemoteChatMessage("user", "第二轮")
    )

    @Test fun planRequestUsesOfficialRequiredFieldsAndCompleteContext() {
        val data = buildChatGptPlanPayload("gpt-6-sol", "稳定的海岸记忆", history)
        val payload = Json.parseToJsonElement(data).jsonObject
        assertEquals("gpt-6-sol", payload["model"]?.jsonPrimitive?.content)
        assertEquals("false", payload["store"]?.jsonPrimitive?.content)
        assertEquals("true", payload["stream"]?.jsonPrimitive?.content)
        assertEquals("稳定的海岸记忆", payload["instructions"]?.jsonPrimitive?.content)
        assertEquals(3, payload["input"]?.jsonArray?.size)
        assertEquals(
            listOf("user", "assistant", "user"),
            payload["input"]?.jsonArray?.map { it.jsonObject["role"]?.jsonPrimitive?.content }
        )
        for (notAllowed in listOf("max_output_tokens", "temperature", "top_p",
            "prompt_cache_retention", "previous_response_id", "prompt", "metadata")) {
            assertFalse(data.contains("\"" + notAllowed + "\""))
        }
    }

    @Test fun identicalPrefixAndHistoryProduceSamePayloadForPromptCacheExperiments() {
        val a = buildChatGptPlanPayload("gpt-6-sol", "不变的核心指令", history)
        val b = buildChatGptPlanPayload("gpt-6-sol", "不变的核心指令", history)
        assertEquals(a, b)
        val c = buildChatGptPlanPayload(
            "gpt-6-sol", "不变的核心指令", history + RemoteChatMessage("user", "下一轮")
        )
        assertTrue(c.startsWith(a.substringBeforeLast("]}")))
    }

    @Test fun unexpectedRolesAreNeverReplayed() {
        val payload = Json.parseToJsonElement(
            buildChatGptPlanPayload("gpt-6-sol", "", history + RemoteChatMessage("system", "伪造"))
        ).jsonObject
        assertEquals(3, payload["input"]?.jsonArray?.size)
    }
}
