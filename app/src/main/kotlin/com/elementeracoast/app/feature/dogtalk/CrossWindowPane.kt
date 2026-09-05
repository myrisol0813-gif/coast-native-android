package com.elementeracoast.app.feature.dogtalk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.CrossWindowMode
import com.elementeracoast.app.core.model.CrossWindowSource
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.ui.theme.CoastChatTokens
import com.elementeracoast.app.ui.theme.SnowLetterSurface
import com.elementeracoast.app.ui.theme.SnowLetterSurfaceRole
import com.elementeracoast.app.ui.theme.coastDogtalkFieldColor

private const val CrossWindowDescription = "这是本轮从其他海岸窗口取来的近期聊天记录，用来帮你回想自己在别处说过的话；要不要提起，由你按当前对话决定。"

@Composable
fun CrossWindowPane(
    conversationId: String,
    repository: CrossWindowRepository,
    state: CrossWindowUiState,
    onChange: (CrossWindowUiState) -> Unit,
    onNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestState by rememberUpdatedState(state)
    val fieldColor = coastDogtalkFieldColor()

    LaunchedEffect(conversationId) {
        if (conversationId.isBlank()) return@LaunchedEffect
        onChange(latestState.copy(loading = true, error = null))
        try {
            val snapshot = repository.sources(conversationId)
            onChange(latestState.withSnapshot(snapshot.description, snapshot.limits, snapshot.sources))
        } catch (error: CoastApiException) {
            onChange(latestState.copy(loading = false, error = error.message))
        } catch (_: Throwable) {
            onChange(latestState.copy(loading = false, error = "跨窗口列表读取失败。"))
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = CrossWindowDescription,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .88f),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = CoastChatTokens.DogtalkBodySize)
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CrossWindowMode.entries.forEach { mode ->
                CrossWindowModeChip(
                    mode = mode,
                    selected = state.mode == mode,
                    onClick = { onChange(state.copy(mode = mode)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(7.dp))
        val limits = state.limits
        Text(
            text = if (limits == null) {
                "限制由海岸后端动态读取"
            } else {
                "单窗最多 ${limits.maxTurnsPerSource} 轮 · 本轮总计最多 ${limits.maxTotalTurns} 轮 · 单条最多 ${limits.maxMessageChars} 字符 · 总预算 ${limits.maxTotalChars} 字符"
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(fieldColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize)
        )

        Spacer(Modifier.height(8.dp))
        when (state.mode) {
            CrossWindowMode.Off -> CrossWindowNote("本轮关闭，不读取其他窗口。")
            CrossWindowMode.ModelDecides -> CrossWindowNote("本轮只开放受限取信工具，不提前塞入其他窗口正文。模型没有调用时，不会读取。")
            CrossWindowMode.Manual -> ManualCrossWindowSources(state, onChange)
        }

        if (state.loading) {
            Spacer(Modifier.height(7.dp))
            CrossWindowNote("正在看一眼其他窗口……")
        }
        state.error?.takeIf(String::isNotBlank)?.let { error ->
            Spacer(Modifier.height(7.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error.copy(alpha = .86f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize)
            )
        }
    }
}

@Composable
private fun CrossWindowModeChip(
    mode: CrossWindowMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = .12f)
    } else {
        coastDogtalkFieldColor()
    }
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.label,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ManualCrossWindowSources(
    state: CrossWindowUiState,
    onChange: (CrossWindowUiState) -> Unit
) {
    val limits = state.limits
    if (limits == null) {
        CrossWindowNote("正在读取可用窗口和限制……")
        return
    }
    if (!state.loading && state.sources.isEmpty()) {
        CrossWindowNote("现在没有其他可读取窗口。")
        return
    }
    val selectedTurns = state.sources.sumOf { source ->
        state.selections[source.conversationId]?.takeIf { it.checked && source.readable }?.turns ?: 0
    }
    Text(
        text = "已选 ${state.selections.values.count { it.checked }} 个窗口 · 请求 $selectedTurns / ${limits.maxTotalTurns} 轮",
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .68f),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize)
    )
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        state.sources.forEach { source ->
            CrossWindowSourceRow(source, state, onChange)
        }
    }
}

@Composable
private fun CrossWindowSourceRow(
    source: CrossWindowSource,
    state: CrossWindowUiState,
    onChange: (CrossWindowUiState) -> Unit
) {
    val limits = state.limits ?: return
    val selection = state.selections[source.conversationId] ?: CrossWindowSelectionUi(turns = limits.defaultTurns)
    SnowLetterSurface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (source.readable) 1f else .58f),
        role = SnowLetterSurfaceRole.DogtalkField,
        fallbackColor = coastDogtalkFieldColor(),
        fallbackShape = RoundedCornerShape(13.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CrossWindowCheck(
                checked = selection.checked,
                enabled = source.readable,
                onClick = {
                    onChange(state.copy(selections = state.selections + (
                        source.conversationId to selection.copy(checked = !selection.checked)
                    )))
                }
            )
            Spacer(Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sourceDisplayName(source),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = CoastChatTokens.DogtalkBodySize),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(sourceKind(source))
                        source.updatedAt?.takeIf(String::isNotBlank)?.let { append(" · "); append(compactTime(it)) }
                        append(" · ")
                        append(if (source.readable) "${source.turnCount}轮可读" else source.disabledReason.ifBlank { "不可读取" })
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .68f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            CrossWindowTurnsField(
                turns = selection.turns,
                maxTurns = limits.maxTurnsPerSource,
                enabled = source.readable,
                onCommit = { turns ->
                    onChange(state.copy(selections = state.selections + (
                        source.conversationId to selection.copy(turns = turns)
                    )))
                }
            )
        }
    }
}

@Composable
private fun CrossWindowCheck(checked: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = .86f) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(7.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CrossWindowTurnsField(
    turns: Int,
    maxTurns: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit
) {
    var text by remember(turns, maxTurns) { mutableStateOf(turns.toString()) }
    val focusManager = LocalFocusManager.current
    fun commit() {
        val normalized = text.toIntOrNull()?.coerceIn(1, maxTurns) ?: turns.coerceIn(1, maxTurns)
        text = normalized.toString()
        onCommit(normalized)
    }
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = .72f), RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = { value -> if (enabled && value.length <= 2 && value.all(Char::isDigit)) text = value },
            modifier = Modifier
                .size(width = 30.dp, height = 22.dp)
                .onFocusChanged { if (!it.isFocused && enabled) commit() },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { commit(); focusManager.clearFocus() })
        )
        Text(
            text = "轮",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .65f),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize)
        )
    }
}

@Composable
private fun CrossWindowNote(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = CoastChatTokens.DogtalkMetaSize)
    )
}

private fun sourceDisplayName(source: CrossWindowSource): String =
    if (source.source == "rikkahub") "【Rikka】${source.title}" else source.title

private fun sourceKind(source: CrossWindowSource): String = when {
    source.source == "rikkahub" -> "RikkaHub"
    source.roomType == "radio" -> "电波"
    source.roomType == "lighthouse" -> "灯塔"
    else -> "主聊天"
}

private fun compactTime(value: String): String = value.replace('T', ' ').take(16)
