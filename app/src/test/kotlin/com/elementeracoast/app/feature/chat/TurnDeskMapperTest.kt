package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.remote.RemoteChatMessage
import com.elementeracoast.app.core.remote.RemoteDeskCurrentMessage
import com.elementeracoast.app.core.remote.RemoteDeskExternalTide
import com.elementeracoast.app.core.remote.RemoteDeskMemory
import com.elementeracoast.app.core.remote.RemoteDeskRecentContext
import com.elementeracoast.app.core.remote.RemoteDeskSlip
import com.elementeracoast.app.core.remote.RemoteDeskThinkingSoil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnDeskMapperTest {
    @Test fun mapperProducesSameNineSourceSectionsAsPwa() {
        val receipt = TurnDeskMapper.toUi(
            RemoteDeskSlip(
                currentMessage = RemoteDeskCurrentMessage(
                    description = "当前来源说明",
                    status = "已递给",
                    delivered = true,
                    content = "这一句话"
                ),
                recentContext = RemoteDeskRecentContext(
                    description = "最近来源说明",
                    status = "已递给",
                    statusDetail = "1 轮",
                    turns = 1,
                    messages = listOf(RemoteChatMessage(role = "user", content = "上一轮"))
                ),
                thinkingSoil = RemoteDeskThinkingSoil(
                    description = "思维壤说明",
                    status = "已递给",
                    pocketCandidatesCount = 2,
                    pocketCandidatesStatus = "待确认"
                ),
                relatedMemory = RemoteDeskMemory(
                    description = "记忆说明",
                    status = "未命中"
                ),
                externalTide = RemoteDeskExternalTide(
                    description = "外来说明",
                    status = "未递给",
                    content = "本轮没有递入外部材料。"
                )
            )
        )

        assertEquals(
            listOf("当前消息", "最近上下文", "核心自定义", "思维壤", "相关记忆", "世界书", "神秘狗话", "工作台 / 工具回执", "外来潮汐"),
            receipt.sections.map { it.title }
        )
        assertEquals("已递给 · 1 轮", receipt.sections[1].status)
        assertTrue(receipt.sections[3].details.any { it.label == "待确认候选" && it.text.contains("待确认 · 2 条 · 未递给正文") })
        assertEquals("未命中", receipt.sections[4].status)
        assertTrue(receipt.sections.last().details.any { it.text == "本轮没有递入外部材料。" })
        assertTrue(receipt.sections.all { section -> section.details.any { it.label == "来源说明" } || section.details.any { it.text == "本轮未递入" } })
    }
}
