package com.elementeracoast.app.feature.gate

import com.elementeracoast.app.ui.brand.CoastGateMotionSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class GateContractTest {
    @Test
    fun pwaMotionTimingIsPreserved() {
        assertEquals(50, CoastGateMotionSpec.LoopAStartMs)
        assertEquals(160, CoastGateMotionSpec.LoopBStartMs)
        assertEquals(270, CoastGateMotionSpec.LoopCStartMs)
        assertEquals(720, CoastGateMotionSpec.LoopDurationMs)
        assertEquals(620, CoastGateMotionSpec.HornStartMs)
        assertEquals(760, CoastGateMotionSpec.WolfStartMs)
        assertEquals(980, CoastGateMotionSpec.SettleStartMs)
        assertEquals(1020, CoastGateMotionSpec.BrandStartMs)
        assertEquals(1140, CoastGateMotionSpec.TaglineStartMs)
        assertEquals(1380, CoastGateMotionSpec.GateFormStartMs)
        assertEquals(1780, CoastGateMotionSpec.TotalDurationMs)
    }

    @Test
    fun mailboxStateNeverCarriesCredentials() {
        val opened = MailboxEntryState().open().show(MailboxEntryPage.Register)
        assertEquals(true, opened.visible)
        assertEquals(MailboxEntryPage.Register, opened.page)
        assertEquals(MailboxEntryState(), opened.close())
    }
}
