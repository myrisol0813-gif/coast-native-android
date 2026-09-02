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
        val moment = store.publishMoment("第一条潮声", "2026/09/02", MomentAuthor.Xiaohan)!!
        store.toggleMomentLike(moment.id)
        store.addComment(moment.id, "收到")
        store.editMoment(moment.id, "改过的潮声")
        val updated = store.state.value.moments.single()
        assertTrue(updated.liked)
        assertEquals(listOf("收到"), updated.comments)
        assertEquals("改过的潮声", updated.text)
        assertEquals("2026-09-02", updated.date)
        assertEquals(MomentAuthor.Xiaohan, updated.author)
        val reloaded = DailyStore(persistence).state.value.moments.single()
        assertEquals(MomentAuthor.Xiaohan, reloaded.author)
        store.deleteMoment(moment.id)
        assertTrue(store.state.value.moments.isEmpty())
    }

    @Test fun momentAuthorPersistsForMyriAndXiaohan() {
        val persistence = MemoryLocalPersistence()
        val store = DailyStore(persistence)
        store.publishMoment("小寒写的", author = MomentAuthor.Xiaohan)
        store.publishMoment("Myri 写的", author = MomentAuthor.Myri)

        val reloaded = DailyStore(persistence).state.value.moments
        assertEquals(MomentAuthor.Myri, reloaded[0].author)
        assertEquals(MomentAuthor.Xiaohan, reloaded[1].author)
    }

    @Test fun myriCommentSurfaceDoesNotInventLocalCommentData() {
        val store = DailyStore(MemoryLocalPersistence())
        val moment = store.publishMoment("等 Myri 真正来评论", author = MomentAuthor.Xiaohan)!!

        assertTrue(store.state.value.moments.single { it.id == moment.id }.comments.isEmpty())
        assertEquals("叫 Myri 来评论", MyriCommentActionLabel)
        assertTrue(MyriCommentOfflineMessage.contains("后端接线后启用"))
    }

    @Test fun diaryAndDailyProfileFieldsPersistWithoutHiddenPetState() {
        val persistence = MemoryLocalPersistence()
        val store = DailyStore(persistence)
        val diary = store.saveDiary(weather = "风", mood = "松", tags = listOf("海"), text = "纸页")!!
        store.saveDiary(diary.id, diary.date, "雨", "亮", listOf("岸"), "新纸页")
        store.setProfileAvatar("content://xiaohan")
        store.setMyriAvatar("content://myri")
        store.setCover("content://cover")

        val reloaded = DailyStore(persistence).state.value
        assertEquals("新纸页", reloaded.diaries.single().text)
        assertEquals("content://xiaohan", reloaded.profileAvatarUri)
        assertEquals("content://myri", reloaded.myriAvatarUri)
        assertEquals("content://cover", reloaded.coverUri)

        store.deleteDiary(diary.id)
        assertFalse(store.state.value.diaries.any { it.id == diary.id })
    }
}
