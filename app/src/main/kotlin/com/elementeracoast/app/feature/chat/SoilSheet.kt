package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            Text("思维壤 · 本地结构预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("不会发往真实后端；这里只展示 app-59 的上下文结构。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            SoilSection("本轮递给模型", "本地 fake generation · 当前输入 + 最近纸条")
            SoilSection("最近上下文", "最近 ${settings.recentTurns} 轮 · 舒服区间上沿 ${settings.contextBudget}")
            SoilSection("基本设置", "${settings.outputLength} · max ${settings.maxOutputTokens} · ${settings.creativity} · ${if (settings.streamingEnabled) "流式" else "非流式"}")
            SoilSection("本地记忆命中", if (memoryHits.isEmpty()) "本轮没有本地命中" else memoryHits.take(settings.memoryLimit).joinToString(" · ") { it.title })
            SoilSection("本轮家具", if (furnitureCount == 0) "没有动用本地家具" else "$furnitureCount 件本地家具记录")
            SoilSection("当前纸条数", "${recentMessages.size} 条消息仅作为本地展示")
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
