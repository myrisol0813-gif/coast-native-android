package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.remote.RemoteChatMessage
import com.elementeracoast.app.core.remote.RemoteDeskAttachmentItem
import com.elementeracoast.app.core.remote.RemoteDeskAttachments
import com.elementeracoast.app.core.remote.RemoteDeskAttachmentVision
import com.elementeracoast.app.core.remote.RemoteDeskCurrentMessage
import com.elementeracoast.app.core.remote.RemoteDeskExternalTide
import com.elementeracoast.app.core.remote.RemoteDeskMemory
import com.elementeracoast.app.core.remote.RemoteDeskRecentContext
import com.elementeracoast.app.core.remote.RemoteDeskSlip
import com.elementeracoast.app.core.remote.RemoteDeskThinkingSoil
import com.elementeracoast.app.core.remote.RemoteDeskWebSearch
import com.elementeracoast.app.core.remote.RemoteDeskWebSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnDeskMapperTest {
    @Test fun mapperProducesSameTenSourceSectionsAsPwa() {
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
            listOf("当前消息", "最近上下文", "核心自定义", "思维壤", "相关记忆", "世界书", "狗话", "跨窗口取信", "工作台 / 工具回执", "外来潮汐"),
            receipt.sections.map { it.title }
        )
        assertEquals("已递给 · 1 轮", receipt.sections[1].status)
        assertTrue(receipt.sections[3].details.any { it.label == "待确认候选" && it.text.contains("待确认 · 2 条 · 未递给正文") })
        assertEquals("未命中", receipt.sections[4].status)
        assertTrue(receipt.sections[7].details.any { it.text == "本轮未递入" })
        assertTrue(receipt.sections.last().details.any { it.text == "本轮没有递入外部材料。" })
        assertTrue(receipt.sections.all { section -> section.details.any { it.label == "来源说明" } || section.details.any { it.text == "本轮未递入" } })
    }

    @Test fun mapperAddsAttachmentAndSearchReceiptsWhenPresent() {
        val receipt = TurnDeskMapper.toUi(
            RemoteDeskSlip(
                attachments = RemoteDeskAttachments(
                    uploaded = 2,
                    deliveredToModel = 1,
                    delivered = listOf(
                        RemoteDeskAttachmentItem(
                            id = "a1",
                            name = "图.png",
                            type = "image",
                            mode = "vision"
                        )
                    ),
                    notDelivered = listOf(
                        RemoteDeskAttachmentItem(
                            id = "a2",
                            name = "书.pdf",
                            reason = "file_type_unsupported"
                        )
                    ),
                    vision = RemoteDeskAttachmentVision(
                        supported = true,
                        imagesDelivered = 1
                    )
                ),
                webSearch = RemoteDeskWebSearch(
                    available = true,
                    used = true,
                    requestedQuery = "搜一下最新资料",
                    requests = 1,
                    resultsCount = 1,
                    results = listOf(
                        RemoteDeskWebSearchResult(
                            title = "来源",
                            url = "https://example.com",
                            content = "摘要"
                        )
                    )
                )
            )
        )

        val attachment = receipt.sections.first { it.title == "本轮附件" }
        val search = receipt.sections.first { it.title == "本轮搜索" }
        assertTrue(attachment.status.contains("上传 2"))
        assertTrue(attachment.details.any { it.text.contains("识图") })
        assertTrue(attachment.details.any { it.text.contains("file_type_unsupported") })
        assertTrue(search.status.contains("已搜索"))
        assertTrue(search.details.any { it.text.contains("https://example.com") })
    }
}
