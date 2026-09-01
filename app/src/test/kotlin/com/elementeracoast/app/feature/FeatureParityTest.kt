package com.elementeracoast.app.feature

import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.RunControlSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureParityTest {
    @Test
    fun activeDestinationsContainApp59SurfacesWithoutOldSerpentDesk() {
        assertEquals(
            listOf(
                FeatureDestination.Memory,
                FeatureDestination.Daily,
                FeatureDestination.Wolf,
                FeatureDestination.Appearance,
                FeatureDestination.ActionLog
            ),
            FeatureDestination.entries
        )
        val names = FeatureDestination.entries.joinToString(" ") { "${it.name} ${it.title} ${it.subtitle}" }
        assertFalse(names.contains("Calendar", true))
        assertFalse(names.contains("Summary", true))
        assertFalse(names.contains("Album", true))
        assertFalse(names.contains("Serpent Desk", true))
        assertTrue(names.contains("小蛇行动日志"))
    }

    @Test
    fun runControlHasOnlyElevenApp59ActiveFields() {
        val fields = RunControlSettings::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        assertEquals(
            setOf(
                "recentTurns", "comfortTokens", "outputLength", "maxOutputTokens", "expression",
                "streamingEnabled", "soilBudget", "seedCooldownTurns", "worldbookEnabled",
                "worldbookLimit", "memoryLimit"
            ),
            fields
        )
        assertFalse(fields.any { it.contains("currentWindow", true) || it.contains("total", true) })
    }
}
