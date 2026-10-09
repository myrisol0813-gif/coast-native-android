package com.elementeracoast.app.feature.chatgpt

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeResponseOutputTest {
    @Test fun finalMessageIsRecoveredWhenStreamContainsNoTextDeltas() {
        val response = buildJsonObject {
            put("output", buildJsonArray {
                add(buildJsonObject {
                    put("type", "message")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "output_text")
                            put("text", "朋友圈已经写好。")
                        })
                    })
                })
            })
        }
        val outcome = readNativeResponseOutput(response)
        assertEquals("朋友圈已经写好。", outcome.finalText)
        assertEquals("朋友圈已经写好。", missingNativeFinalText("", outcome.finalText))
        assertTrue(outcome.toolCalls.isEmpty())
    }

    @Test fun finalTextDoesNotRepeatAlreadyStreamedDelta() {
        assertEquals("世界", missingNativeFinalText("你好，", "你好，世界"))
        assertEquals("", missingNativeFinalText("你好，世界", "你好，世界"))
        assertEquals("", missingNativeFinalText("不同的流", "最终文本"))
    }

    @Test fun functionOnlyResponseContinuesToToolExecutionWithoutText() {
        val response = buildJsonObject {
            put("output", buildJsonArray {
                add(buildJsonObject {
                    put("type", "function_call")
                    put("call_id", "call_001")
                    put("name", "create_moment")
                    put("namespace", "coast")
                    put("arguments", "{\"content\":\"测试\"}")
                })
            })
        }
        val outcome = readNativeResponseOutput(response)
        assertEquals("", outcome.finalText)
        assertEquals(1, outcome.toolCalls.size)
        assertEquals("create_moment", parseNativeCoastToolCall(outcome.toolCalls.first()).name)
    }

    @Test fun reasoningOnlyResponseIsDistinctFromToolCallAndFinishedText() {
        val response = buildJsonObject {
            put("output", buildJsonArray {
                add(buildJsonObject { put("type", "reasoning") })
            })
        }
        val outcome = readNativeResponseOutput(response)
        assertTrue(outcome.hasReasoning)
        assertTrue(outcome.finalText.isEmpty())
        assertTrue(outcome.toolCalls.isEmpty())
    }

    @Test fun emptyOutputDoesNotPretendToContainTextOrTools() {
        val outcome = readNativeResponseOutput(buildJsonObject { put("output", buildJsonArray {}) })
        assertFalse(outcome.hasReasoning)
        assertTrue(outcome.finalText.isEmpty())
        assertTrue(outcome.toolCalls.isEmpty())
    }
}
