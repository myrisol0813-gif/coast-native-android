package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.MemoryRecord
import com.elementeracoast.app.core.model.MessageRole
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.SeedStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Native26LocalStoresTest {
    @Test
    fun preferencesPersistProfileModelThemeAndElevenSettings() {
        val db = FakePersistence()
        val store = LocalPreferencesStore(db)
        store.updateProfile("Kryo", "小寒署名")
        store.setUserBubble("gold-gray")
        store.setAccent("blue")
        store.setModel("GPT-5.6 Sol")
        store.updateRunControl { it.copy(recentTurns = 12, memoryLimit = 4, streamingEnabled = true) }

        val restored = LocalPreferencesStore(db).state.value
        assertEquals("Kryo", restored.profile.nickname)
        assertEquals("小寒署名", restored.profile.signature)
        assertEquals("gold-gray", restored.profile.userBubble)
        assertEquals("blue", restored.profile.accent)
        assertEquals("GPT-5.6 Sol", restored.currentModel)
        assertEquals(12, restored.runControl.recentTurns)
        assertEquals(4, restored.runControl.memoryLimit)
        assertTrue(restored.runControl.streamingEnabled)
    }

    @Test
    fun dailySupportsMomentCommentLikeDeleteAndDiaryCrud() {
        val store = LocalDailyStore(FakePersistence(), now = { 100L })
        val moment = store.createMoment("潮声")!!
        assertTrue(store.toggleMomentLike(moment.id))
        assertTrue(store.addMomentComment(moment.id, "收到"))
        assertEquals(listOf("收到"), store.state.value.moments.single().comments)
        assertTrue(store.editMoment(moment.id, "新的潮声"))
        assertTrue(store.deleteMoment(moment.id))
        assertTrue(store.state.value.moments.isEmpty())

        val diary = store.createDiary("2026-09-02", "晴", "平静", listOf("海岸"), "日记正文")!!
        assertTrue(store.editDiary(diary.copy(content = "改过的日记")))
        assertEquals("改过的日记", store.state.value.diaries.single().content)
        assertTrue(store.deleteDiary(diary.id))
        assertTrue(store.state.value.diaries.isEmpty())
    }

    @Test
    fun memorySeedWorldbookAndInstructionsAreRealLocalCrud() {
        val store = LocalMemoryStore(FakePersistence())
        val memory = store.addMemory("海鸟与岸", "归返", "关系结构", "需要时使用", "不要简化", listOf("关系"))!!
        assertEquals(1, store.searchMemories("海鸟").size)
        assertTrue(store.editMemory(memory.copy(content = "更新后的关系结构")))
        assertTrue(store.deleteMemory(memory.id))

        val seed = store.addSeed("种子", "可能继续长", SeedStatus.Active)!!
        assertTrue(store.editSeed(seed.copy(status = SeedStatus.Dormant)))
        assertEquals(SeedStatus.Dormant, store.searchSeeds("Dormant").single().status)
        assertTrue(store.deleteSeed(seed.id))

        val world = store.addWorldbook("回潮", "海岸中的归返")!!
        assertTrue(store.toggleWorldbook(world.id))
        assertFalse(store.state.value.worldbook.single().enabled)
        assertTrue(store.deleteWorldbook(world.id))

        store.saveCustomInstructions("local instructions")
        assertEquals("local instructions", store.state.value.customInstructions)
        store.clearCustomInstructions()
        assertEquals("", store.state.value.customInstructions)
    }

    @Test
    fun actionLogFiltersExactIdsWithoutPrivateBody() {
        val store = LocalActionLogStore(FakePersistence(), now = { 42L })
        val first = store.record(
            actionKey = "memory.search",
            label = "搜索了记忆",
            roomType = RoomType.Main,
            conversationId = "main-a",
            inputSummary = "query length 4",
            outputSummary = "3 local hits",
            count = 3,
            items = listOf(FurnitureItem("关系", "海鸟与岸"))
        )
        val second = store.record(
            actionKey = "daily.create_diary",
            label = "写了一篇日记",
            roomType = RoomType.Main,
            conversationId = "main-a"
        )
        store.setFilters(conversation = "main-a", focusedIds = setOf(first.actionId))
        assertEquals(listOf(first.actionId), store.visibleRecords().map { it.actionId })
        assertFalse(store.visibleRecords().single().inputSummary.contains("正文"))
        assertTrue(second.actionId != first.actionId)
    }

    @Test
    fun chatCreateRenameDeleteFallbackAndImportExportStayLocal() {
        val db = FakePersistence()
        val store = LocalChatStore(db)
        store.openRoomType(RoomType.Radio)
        val created = store.newConversation(RoomType.Radio)
        assertTrue(store.renameConversation(created.id, "【灯塔】夜航"))
        assertTrue(store.state.value.conversations.first { it.id == created.id }.title.startsWith("【电波】"))

        val json = """{
          "messages":[
            {"id":11,"role":"user","text":"hello"},
            {"id":12,"role":"assistant","text":"world","model_id":"GPT-5.6 Sol"}
          ]
        }""".trimIndent()
        assertEquals(2, store.importJson(json))
        assertEquals(MessageRole.User, store.state.value.messages.first().role)
        assertTrue(store.exportJson("Kryo", "GPT-5.6 Sol").contains("elementera-native-chat-export"))
        assertTrue(store.exportHtml("Kryo").contains("<b>Kryo</b>"))

        store.state.value.conversations.map { it.id }.toList().forEach { id -> store.deleteConversation(id) }
        assertEquals(1, store.state.value.conversations.size)
        assertEquals(RoomType.Main, store.state.value.activeRoomType)
    }
}

private class FakePersistence : LocalPersistence {
    private val values = mutableMapOf<String, String>()
    override fun read(key: String): String? = values[key]
    override fun write(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}
