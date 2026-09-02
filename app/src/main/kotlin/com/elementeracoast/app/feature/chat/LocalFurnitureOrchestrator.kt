package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureRun
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.feature.actionlog.ActionLogStore
import com.elementeracoast.app.feature.daily.DailyStore
import com.elementeracoast.app.feature.memory.LocalSeed
import com.elementeracoast.app.feature.memory.MemoryStore
import com.elementeracoast.app.feature.memory.SeedStatus
import java.time.LocalDate

/** Explicit local fake-tool owner. Returns action ids for the assistant message; never guesses by time. */
class LocalFurnitureOrchestrator(
    private val daily: DailyStore,
    private val memory: MemoryStore,
    private val actionLog: ActionLogStore
) {
    fun runForPrompt(
        prompt: String,
        roomType: RoomType,
        conversationId: String,
        assistantMessageId: Long
    ): List<FurnitureRun> {
        val runs = mutableListOf<FurnitureRun>()
        val text = prompt.trim()

        if (text.contains("碳硅圈") || text.contains("动态")) {
            val moment = daily.publishMoment("来自本地聊天演示：${text.take(120)}")
            if (moment != null) {
                val record = actionLog.record(
                    actionKey = "daily.moment.write",
                    label = "写了一条碳硅圈",
                    roomType = roomType,
                    conversationId = conversationId,
                    inputSummary = "本地聊天触发动态写入",
                    outputSummary = "新增 1 条本地动态",
                    assistantMessageId = assistantMessageId
                )
                runs += FurnitureRun(record.actionId, record.actionKey, record.label)
            }
        }

        if (text.contains("日记")) {
            val diary = daily.saveDiary(
                date = LocalDate.now().toString(),
                weather = "本地演示",
                mood = "记录中",
                tags = listOf("聊天演示"),
                text = "来自本地聊天演示：${text.take(240)}"
            )
            if (diary != null) {
                val record = actionLog.record(
                    actionKey = "daily.diary.write",
                    label = "写了一篇日记",
                    roomType = roomType,
                    conversationId = conversationId,
                    inputSummary = "本地聊天触发日记写入",
                    outputSummary = "新增 1 篇本地日记",
                    assistantMessageId = assistantMessageId
                )
                runs += FurnitureRun(record.actionId, record.actionKey, record.label)
            }
        }

        if (text.contains("记忆") || text.contains("搜索")) {
            val keyword = text
                .replace("记忆", "")
                .replace("搜索", "")
                .trim()
            val allHits = memory.searchMemories(keyword)
            val hits = allHits.take(5)
            val record = actionLog.record(
                actionKey = "memory.search",
                label = "搜索了记忆",
                roomType = roomType,
                conversationId = conversationId,
                inputSummary = "本地记忆检索${if (keyword.isBlank()) "" else " · ${keyword.take(60)}"}",
                outputSummary = "命中 ${allHits.size} 条",
                assistantMessageId = assistantMessageId
            )
            runs += FurnitureRun(
                actionId = record.actionId,
                actionKey = record.actionKey,
                label = record.label,
                count = allHits.size,
                items = hits.map { FurnitureItem(it.title, it.category) },
                extraCount = (allHits.size - hits.size).coerceAtLeast(0)
            )
        }

        if (text.contains("候选")) {
            val seed = memory.saveSeed(
                LocalSeed(
                    id = "",
                    title = "待确认候选",
                    content = text.take(500),
                    status = SeedStatus.Dormant
                )
            )
            val record = actionLog.record(
                actionKey = "memory.candidate.write",
                label = "写入本地待确认候选",
                roomType = roomType,
                conversationId = conversationId,
                inputSummary = "本地候选纸条",
                outputSummary = "已放入 dormant 种子 · ${seed.title}",
                assistantMessageId = assistantMessageId
            )
            runs += FurnitureRun(record.actionId, record.actionKey, record.label)
        }

        return runs
    }
}
