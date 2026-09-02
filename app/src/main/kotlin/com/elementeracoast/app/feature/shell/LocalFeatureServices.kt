package com.elementeracoast.app.feature.shell

import com.elementeracoast.app.core.local.LocalPersistence
import com.elementeracoast.app.feature.actionlog.ActionLogStore
import com.elementeracoast.app.feature.daily.DailyStore
import com.elementeracoast.app.feature.letters.IslandLetterStore
import com.elementeracoast.app.feature.memory.MemoryStore
import com.elementeracoast.app.feature.wolf.WolfStore

/** Explicit local feature owners used by the current Native shell. */
class LocalFeatureServices(persistence: LocalPersistence) {
    val wolf = WolfStore(persistence)
    val daily = DailyStore(persistence)
    val memory = MemoryStore(persistence)
    val actionLog = ActionLogStore(persistence)
    val islandLetter = IslandLetterStore(persistence)
}
