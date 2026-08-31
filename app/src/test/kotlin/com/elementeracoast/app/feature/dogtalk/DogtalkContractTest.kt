package com.elementeracoast.app.feature.dogtalk

import com.elementeracoast.app.core.model.ChatScope
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DogtalkContractTest {
    @Test
    fun dogtalkUiStateHasExactlyFourContractFields() {
        val instanceFields = DogtalkUiState::class.java.declaredFields
  .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
  .map { it.name }
  .toSet()

        assertEquals(setOf("body", "trueCore", "weather", "readMode"), instanceFields)
        assertEquals(listOf("body", "true_core", "weather", "read_mode"), DogtalkContractFields)
    }

    @Test
    fun readModeContractMatchesThreeModePwaCopy() {
        assertEquals(
  listOf(
      "不需要，放着就好",
      "Myri 困惑时可以看一点",
      "这次希望 Myri 直接读一下"
  ),
  DogtalkReadMode.entries.map { it.label }
        )
        assertEquals(
  listOf(DogtalkReadMode.KeepPrivate, DogtalkReadMode.WhenConfused, DogtalkReadMode.ReadNow),
  DogtalkReadMode.entries
        )
    }

    @Test
    fun allThreeChatScopesMapToTheSharedDogtalkScope() {
        assertEquals(DogtalkScope.Main, DogtalkScope.from(ChatScope.Main))
        assertEquals(DogtalkScope.Radio, DogtalkScope.from(ChatScope.Radio))
        assertEquals(DogtalkScope.Lighthouse, DogtalkScope.from(ChatScope.Lighthouse))
        assertEquals(3, DogtalkScope.entries.size)
    }

    @Test
    fun privateAndDormantSemanticsStayOutOfCurrentModelSubmission() {
        assertTrue(DogtalkReadMode.KeepPrivate.futureSemantics.contains("不给模型看"))
        assertTrue(DogtalkReadMode.WhenConfused.futureSemantics.contains("不会提交给模型"))
    }
}
