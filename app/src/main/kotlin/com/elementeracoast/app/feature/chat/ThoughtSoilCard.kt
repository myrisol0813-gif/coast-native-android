package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.MessageRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThoughtSoilCard(
    messages: List<ChatMessage>,
    preferencesStore: LocalPreferencesStore,
    memoryStore: LocalMemoryStore
) {
    val prefs by preferencesStore.state.collectAsState()
    val memory by memoryStore.state.collectAsState()
    var open by remember { mutableStateOf(false) }
    val handSeeds = memory.seeds.count { it.status.name == "Active" }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 18.dp, vertical = 7.dp)
    ) {
        Text("思维壤 · $handSeeds 粒手持种", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text("  ›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
    }

    if (open) {
        val recentCount = (prefs.runControl.recentTurns * 2).coerceAtLeast(2)
        val recent = messages.takeLast(recentCount)
        val query = messages.lastOrNull { it.role == MessageRole.User }?.text.orEmpty()
        val memoryHits = memoryStore.searchMemories(query).take(prefs.runControl.memoryLimit)
        val furniture = messages.lastOrNull { it.role == MessageRole.Assistant }?.furnitureRuns.orEmpty()
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("思维壤 / 本地上下文壳", style = MaterialTheme.typography.titleLarge)
                SoilSection("本轮递给模型", query.ifBlank { "当前没有用户输入。" })
                SoilSection("最近上下文", if (recent.isEmpty()) "—" else recent.joinToString("\n") { "${it.role.name}: ${it.text.take(160)}" })
                SoilSection(
                    "基本设置",
                    "最近 ${prefs.runControl.recentTurns} 轮 · 舒服区间 ${prefs.runControl.comfortTokens} · ${prefs.runControl.outputLength} · 最大 ${prefs.runControl.maxOutputTokens} · ${prefs.runControl.expression} · 流式 ${prefs.runControl.streamingEnabled}"
                )
                SoilSection(
                    "本地记忆命中",
                    if (memoryHits.isEmpty()) "0 条" else memoryHits.joinToString("\n") { "${it.tags.firstOrNull() ?: "记忆"}｜${it.title}" }
                )
                SoilSection(
                    "本轮家具",
                    if (furniture.isEmpty()) "本轮没有本地家具动作。" else furniture.joinToString("\n") { "${it.label} · ${it.actionId}" }
                )
                Text(
                    "这里只展示 Native local state；没有真实后端上下文注入。",
                    modifier = Modifier.padding(bottom = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SoilSection(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
