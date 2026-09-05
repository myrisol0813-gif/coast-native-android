package com.elementeracoast.app.feature.dogtalk

import com.elementeracoast.app.core.model.CrossWindowLimits
import com.elementeracoast.app.core.model.CrossWindowMode
import com.elementeracoast.app.core.model.CrossWindowRequest
import com.elementeracoast.app.core.model.CrossWindowSelection
import com.elementeracoast.app.core.model.CrossWindowSource

data class CrossWindowSelectionUi(
    val checked: Boolean = false,
    val turns: Int = 0
)

data class CrossWindowUiState(
    val mode: CrossWindowMode = CrossWindowMode.Off,
    val sources: List<CrossWindowSource> = emptyList(),
    val selections: Map<String, CrossWindowSelectionUi> = emptyMap(),
    val limits: CrossWindowLimits? = null,
    val loading: Boolean = false,
    val error: String? = null
) {
    fun request(): CrossWindowRequest = when (mode) {
        CrossWindowMode.Off -> CrossWindowRequest()
        CrossWindowMode.ModelDecides -> CrossWindowRequest(mode = CrossWindowMode.ModelDecides)
        CrossWindowMode.Manual -> CrossWindowRequest(
            mode = CrossWindowMode.Manual,
            sources = sources.asSequence()
                .filter { it.readable }
                .mapNotNull { source ->
                    val selected = selections[source.conversationId] ?: return@mapNotNull null
                    if (!selected.checked) return@mapNotNull null
                    CrossWindowSelection(source.conversationId, selected.turns)
                }
                .toList()
        )
    }
}

fun CrossWindowUiState.withSnapshot(
    description: String,
    limits: CrossWindowLimits,
    sources: List<CrossWindowSource>
): CrossWindowUiState {
    val nextSelections = sources.associate { source ->
        val old = selections[source.conversationId]
        source.conversationId to (old ?: CrossWindowSelectionUi(turns = limits.defaultTurns))
    }
    return copy(
        sources = sources,
        selections = nextSelections,
        limits = limits,
        loading = false,
        error = null
    )
}
