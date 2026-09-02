package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.feature.memory.LocalMemoryEntry
import com.elementeracoast.app.feature.wolf.BasicSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoilBottomSheet(
    recentMessages: List<ChatMessage>,
    settings: BasicSettings,
    memoryHits: List<LocalMemoryEntry>,
    furnitureCount: Int,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp)) {
            Text("思维壤 · Native 可见结构", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("真实上下文由 Coast 后端组装；这里仅展示 Native 当前可见的本地提示。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            SoilSection("本轮递给后端", "当前输入与会话标识；记忆、世界书与工具上下文由 Coast 后端负责组装")
            SoilSection("最近上下文", "最近 ${settings.recentTurns} 轮 · 舒服区间上沿 ${settings.contextBudget}")
            SoilSection("基本设置", "${settings.outputLength} · max ${settings.maxOutputTokens} · ${settings.creativity} · ${if (settings.streamingEnabled) "流式" else "非流式"}")
            SoilSection("本地记忆预览", if (memoryHits.isEmpty()) "本轮没有本地预览项" else memoryHits.take(settings.memoryLimit).joinToString(" · ") { it.title })
            SoilSection("本轮家具", if (furnitureCount == 0) "没有本地家具展示" else "$furnitureCount 件本地家具记录")
            SoilSection("当前纸条数", "${recentMessages.size} 条消息仅作为 Native 界面展示；后端历史是 source of truth")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoilSection(title: String, value: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
    Column(modifier = Modifier.padding(vertical = 11.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
