package com.elementeracoast.app.feature.daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MomentActionContractTest {
    @Test
    fun myriCommentActionIsVisibleButExplicitlyOffline() {
        assertEquals("叫 Myri 来评论", MyriCommentActionLabel)
        assertTrue(MyriCommentOfflineMessage.contains("后端接线后启用"))
    }
}
