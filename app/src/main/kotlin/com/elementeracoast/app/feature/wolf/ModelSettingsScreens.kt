package com.elementeracoast.app.feature.wolf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.RunControlSettings

@Composable
internal fun ModelBoxScreen(
    store: LocalPreferencesStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SurfaceHeading("模型箱", "当前：${shortModelName(state.currentModel)}") }
        item { InfoNote("本地壳，真实模型列表等后端接线后同步。") }
        state.models.forEach { model ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(
                        if (model == state.currentModel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(18.dp)
                    ).clickable {
                        store.setModel(model)
                        actionLogStore.record(
                            actionKey = "model.switch",
                            label = "切换了模型",
                            roomType = roomType,
                            conversationId = conversationId,
                            inputSummary = "local model selection",
                            outputSummary = "current model updated locally"
                        )
                        onSnackbar("已切换到 ${shortModelName(model)} · 仅本地")
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(shortModelName(model), fontWeight = FontWeight.SemiBold)
                        Text(model, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    if (model == state.currentModel) Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
internal fun BasicSettingsScreen(store: LocalPreferencesStore) {
    val state by store.state.collectAsState()
    val run = state.runControl
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SurfaceHeading("基本设置", "回答长度、流式输出、记忆召回与世界书") }
        item { InfoNote("这些参数只进入 Native 本地 state，并影响 fake generation 的展示参数；本轮不会发送给真实模型。") }
        item { CycleSetting("最近聊天轮数", run.recentTurns.toString(), listOf(2, 4, 8, 12)) { value -> store.patch { it.copy(recentTurns = value) } } }
        item { CycleSetting("舒服区间上沿", run.comfortTokens.toString(), listOf(2000, 6000, 12000)) { value -> store.patch { it.copy(comfortTokens = value) } } }
        item { TextCycleSetting("回答长度", run.outputLength, listOf("auto", "short", "long")) { value -> store.patch { it.copy(outputLength = value) } } }
        item { CycleSetting("最大输出 token", run.maxOutputTokens.toString(), listOf(2048, 4096, 8000, 16000, 32768)) { value -> store.patch { it.copy(maxOutputTokens = value) } } }
        item { TextCycleSetting("表达倾向", run.expression, listOf("stable", "balanced", "expansive")) { value -> store.patch { it.copy(expression = value) } } }
        item { ToggleSetting("流式输出", run.streamingEnabled) { value -> store.patch { it.copy(streamingEnabled = value) } } }
        item { CycleSetting("思维壤最多字数", run.soilBudget.toString(), listOf(600, 1200, 1800, 2600, 4000)) { value -> store.patch { it.copy(soilBudget = value) } } }
        item { CycleSetting("种子冷却轮数", run.seedCooldownTurns.toString(), (0..8).toList()) { value -> store.patch { it.copy(seedCooldownTurns = value) } } }
        item { ToggleSetting("世界书 / 海岸词典", run.worldbookEnabled) { value -> store.patch { it.copy(worldbookEnabled = value) } } }
        item { CycleSetting("每轮最多词条", run.worldbookLimit.toString(), (0..6).toList()) { value -> store.patch { it.copy(worldbookLimit = value) } } }
        item { CycleSetting("本轮记忆召回上限", run.memoryLimit.toString(), (0..12).toList()) { value -> store.patch { it.copy(memoryLimit = value) } } }
    }
}

private fun LocalPreferencesStore.patch(transform: (RunControlSettings) -> RunControlSettings) {
    updateRunControl(transform)
}

@Composable
private fun CycleSetting(title: String, value: String, choices: List<Int>, onSelect: (Int) -> Unit) {
    SettingCard(title, value) {
        val current = value.toIntOrNull()
        val index = choices.indexOf(current).takeIf { it >= 0 } ?: 0
        onSelect(choices[(index + 1) % choices.size])
    }
}

@Composable
private fun TextCycleSetting(title: String, value: String, choices: List<String>, onSelect: (String) -> Unit) {
    SettingCard(title, value) {
        val index = choices.indexOf(value).takeIf { it >= 0 } ?: 0
        onSelect(choices[(index + 1) % choices.size])
    }
}

@Composable
private fun ToggleSetting(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingCard(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}

internal fun shortModelName(value: String): String = value.substringAfterLast('/').removePrefix("Free: ").trim()
