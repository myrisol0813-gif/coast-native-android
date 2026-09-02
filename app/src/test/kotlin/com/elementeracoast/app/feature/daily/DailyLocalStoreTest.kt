package com.elementeracoast.app.feature.daily

import com.elementeracoast.app.core.local.MemoryLocalPersistence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLocalStoreTest {
    @Test fun momentPublishLikeCommentEditDeleteAndPersistenceWork() {
        val persistence = MemoryLocalPersistence()
        val store = DailyStore(persistence)
        val moment = store.publishMoment("第一条潮声")!!
        store.toggleMomentLike(moment.id)
        store.addComment(moment.id, "收到")
        store.editMoment(moment.id, "改过的潮声")
        val updated = store.state.value.moments.single()
        assertTrue(updated.liked)
        assertEquals(listOf("收到"), updated.comments)
        assertEquals("改过的潮声", updated.text)
        assertEquals(1, DailyStore(persistence).state.value.moments.size)
        store.deleteMoment(moment.id)
        assertTrue(store.state.value.moments.isEmpty())
    }

    @Test fun diaryAddEditDeleteAndPetStateWork() {
        val store = DailyStore(MemoryLocalPersistence())
        val diary = store.saveDiary(weather = "风", mood = "松", tags = listOf("海"), text = "纸页")!!
        store.saveDiary(diary.id, diary.date, "雨", "亮", listOf("岸"), "新纸页")
        assertEquals("新纸页", store.state.value.diaries.single().text)
        store.feedStory()
        assertEquals(PetMood.Active, store.state.value.petMood)
        store.returnToBox()
        assertEquals(PetMood.Resting, store.state.value.petMood)
        store.deleteDiary(diary.id)
        assertFalse(store.state.value.diaries.any { it.id == diary.id })
    }
}
