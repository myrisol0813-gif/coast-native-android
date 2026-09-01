package com.elementeracoast.app.feature.chat

import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalDailyStore
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureSummary
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.SeedStatus
import java.time.LocalDate

class LocalChatRuntime(
    private val dailyStore: LocalDailyStore,
    private val memoryStore: LocalMemoryStore,
    private val actionLogStore: LocalActionLogStore,
    private val preferencesStore: LocalPreferencesStore
) {
    fun fakeChunks(roomType: RoomType, regenerated: Boolean): List<String> {
        val run = preferencesStore.state.value.runControl
        val opening = if (regenerated) "我把这一轮重新铺开。 " else ""
        val settingLine = "本地参数：最近 ${run.recentTurns} 轮 · 舒服区间 ${run.comfortTokens} · ${run.outputLength}/${run.expression}。 "
        return when (roomType) {
            RoomType.Main -> listOf(
                opening + "这里是 app-59 对齐后的 Native 本地海岸。 ",
                settingLine,
                "这一轮只读写 APK 本机状态，真实 API、SSE、MCP 仍未接线。"
            )
            RoomType.Radio -> listOf(
                opening + "电波房继续和主聊天共用同一副 ChatWindow。 ",
                settingLine,
                "消息不会离开本机。"
            )
            RoomType.Lighthouse -> listOf(
                opening + "灯塔房也沿用同一副聊天身体。 ",
                settingLine,
                "生成足迹会明确标记 local mock。"
            )
        }
    }

    fun performLocalActions(
        text: String,
        roomType: RoomType,
        conversationId: String
    ): List<FurnitureSummary> {
        val runs = mutableListOf<FurnitureSummary>()

        if (containsAny(text, "写碳硅圈", "发碳硅圈", "写一条碳硅圈")) {
            val payload = payloadAfterColon(text).ifBlank { "来自本地 fake model 的一条碳硅圈。" }
            dailyStore.createMoment(payload)?.let {
                runs += actionLogStore.record(
                    actionKey = "daily.create_moment",
                    label = "写了一条碳硅圈",
                    roomType = roomType,
                    conversationId = conversationId,
                    inputSummary = "local fake tool · moment body omitted",
                    outputSummary = "moment saved locally"
                )
            }
        }

        if (containsAny(text, "写日记", "写一篇日记")) {
            val payload = payloadAfterColon(text).ifBlank { "来自本地 fake model 的一篇日记。" }
            dailyStore.createDiary(
                date = LocalDate.now().toString(),
                weather = "本地",
                mood = "平静",
                tags = listOf("fake-tool"),
                content = payload
            )?.let {
                runs += actionLogStore.record(
                    actionKey = "daily.create_diary",
                    label = "写了一篇日记",
                    roomType = roomType,
                    conversationId = conversationId,
                    inputSummary = "local fake tool · diary body omitted",
                    outputSummary = "diary saved locally"
                )
            }
        }

        if (containsAny(text, "搜记忆", "搜索记忆")) {
            val query = payloadAfterColon(text)
            val hits = memoryStore.searchMemories(query)
                .take(preferencesStore.state.value.runControl.memoryLimit)
            val items = hits.take(5).map { memory ->
                FurnitureItem(memory.tags.firstOrNull() ?: "记忆", memory.title)
            }
            runs += actionLogStore.record(
                actionKey = "memory.search",
                label = "搜索了记忆",
                roomType = roomType,
                conversationId = conversationId,
                inputSummary = "local memory search · query length ${query.length}",
                outputSummary = "${hits.size} local hits",
                count = hits.size.coerceAtLeast(1),
                items = items
            )
        }

        if (containsAny(text, "写待确认", "放入待确认")) {
            val payload = payloadAfterColon(text).ifBlank { "本地待确认候选" }
            memoryStore.addSeed("待确认候选", payload, SeedStatus.Dormant)?.let {
                runs += actionLogStore.record(
                    actionKey = "memory.write_candidate",
                    label = "放入本地待确认候选",
                    roomType = roomType,
                    conversationId = conversationId,
                    inputSummary = "candidate body omitted",
                    outputSummary = "stored as dormant local seed"
                )
            }
        }

        return runs
    }

    private fun containsAny(text: String, vararg needles: String): Boolean = needles.any(text::contains)

    private fun payloadAfterColon(text: String): String = text
        .substringAfter('：', text.substringAfter(':', ""))
        .trim()
        .take(4000)
}
