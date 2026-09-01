package com.elementeracoast.app.core.local

import com.elementeracoast.app.core.model.ActionLogRecord
import com.elementeracoast.app.core.model.ActionLogState
import com.elementeracoast.app.core.model.FurnitureItem
import com.elementeracoast.app.core.model.FurnitureSummary
import com.elementeracoast.app.core.model.RoomType
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LocalActionLogStore(
    private val persistence: LocalPersistence,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val key = "actionlog.records.v26"
    private val _state = MutableStateFlow(ActionLogState(records = load()))
    val state: StateFlow<ActionLogState> = _state.asStateFlow()

    fun record(
        actionKey: String,
        label: String,
        roomType: RoomType,
        conversationId: String,
        inputSummary: String = "",
        outputSummary: String = "",
        status: String = "success",
        errorMessage: String? = null,
        count: Int = 1,
        items: List<FurnitureItem> = emptyList()
    ): FurnitureSummary {
        val created = now()
        val actionId = "local-${UUID.randomUUID()}"
        val record = ActionLogRecord(
            actionId = actionId,
            actionKey = actionKey.take(120),
            label = label.take(120),
            status = if (status == "error") "error" else "success",
            roomType = roomType,
            conversationId = conversationId.take(160),
            createdAt = created,
            finishedAt = now(),
            inputSummary = safe(inputSummary),
            outputSummary = safe(outputSummary),
            errorMessage = errorMessage?.let(::safe)
        )
        _state.update { it.copy(records = (listOf(record) + it.records).take(500)) }
        save(_state.value.records)
        return FurnitureSummary(
            actionId = actionId,
            actionKey = record.actionKey,
            label = if (record.status == "error") "某件家具没有摆好" else record.label,
            status = record.status,
            count = count.coerceAtLeast(1),
            items = items.take(5),
            errorType = if (record.status == "error") errorMessage?.take(80) ?: "local_action_failed" else null
        )
    }

    fun setFilters(
        status: String = _state.value.statusFilter,
        action: String = _state.value.actionFilter,
        conversation: String = _state.value.conversationFilter,
        focusedIds: Set<String> = _state.value.focusedActionIds
    ) {
        _state.update {
            it.copy(
                statusFilter = status,
                actionFilter = action,
                conversationFilter = conversation,
                focusedActionIds = focusedIds
            )
        }
    }

    fun clearFocus() = setFilters(focusedIds = emptySet())

    fun visibleRecords(): List<ActionLogRecord> {
        val state = _state.value
        return state.records.filter { record ->
            (state.statusFilter.isBlank() || record.status == state.statusFilter) &&
                (state.actionFilter.isBlank() || record.actionKey == state.actionFilter) &&
                (state.conversationFilter.isBlank() || record.conversationId == state.conversationFilter) &&
                (state.focusedActionIds.isEmpty() || record.actionId in state.focusedActionIds)
        }
    }

    private fun safe(value: String): String = value.replace(Regex("\\s+"), " ").trim().take(240)

    private fun save(records: List<ActionLogRecord>) {
        persistence.write(
            key,
            records.joinToString("\n") { record ->
                LocalCodec.pack(
                    record.actionId,
                    record.actionKey,
                    record.label,
                    record.status,
                    record.roomType.name,
                    record.conversationId,
                    record.createdAt,
                    record.finishedAt,
                    record.inputSummary,
                    record.outputSummary,
                    record.errorMessage.orEmpty()
                )
            }
        )
    }

    private fun load(): List<ActionLogRecord> = LocalCodec.lines(persistence.read(key)).mapNotNull { fields ->
        if (fields.size < 11) return@mapNotNull null
        runCatching {
            ActionLogRecord(
                actionId = fields[0],
                actionKey = fields[1],
                label = fields[2],
                status = fields[3],
                roomType = RoomType.valueOf(fields[4]),
                conversationId = fields[5],
                createdAt = fields[6].toLong(),
                finishedAt = fields[7].toLong(),
                inputSummary = fields[8],
                outputSummary = fields[9],
                errorMessage = fields[10].ifBlank { null }
            )
        }.getOrNull()
    }
}
