package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ThoughtSoilSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoilBottomSheet(
    soil: ThoughtSoilSnapshot,
    onOpenPendingBag: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text("思维壤", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                if (soil.manualLocked) "手动内容已锁定" else "当前窗口的滚动工作上下文",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(14.dp))

            SoilSection("当前", soil.currentText.ifBlank { "还没有整理当前方向。" })

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .62f))
            Column(modifier = Modifier.padding(vertical = 11.dp)) {
                Text("手持种 · ${soil.handSeeds.size}/7", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (soil.handSeeds.isEmpty()) {
                    Text("还没有手持种。", modifier = Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    soil.handSeeds.forEach { seed ->
                        Column(modifier = Modifier.padding(top = 9.dp)) {
                            Text(seed.name.ifBlank { seed.lifeCore }, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            if (seed.lifeCore.isNotBlank()) Text(seed.lifeCore, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            if (seed.usageHint.isNotBlank()) Text("使用：${seed.usageHint}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            if (seed.avoidHint.isNotBlank()) Text("避免：${seed.avoidHint}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            SoilSection("勿复读", soil.doNotRepeat.ifBlank { "暂无。" })

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .62f))
            Column(modifier = Modifier.padding(vertical = 11.dp)) {
                Text("可落袋", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                if (soil.pocketCandidates.isEmpty()) {
                    Text("还没有可落袋内容。", modifier = Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    soil.pocketCandidates.forEach { candidate ->
                        Column(modifier = Modifier.padding(top = 9.dp)) {
                            Text(candidate.title.ifBlank { candidate.lifeCore }, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            if (candidate.lifeCore.isNotBlank()) Text(candidate.lifeCore, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            if (candidate.sourceExcerpt.isNotBlank()) Text("来源：${candidate.sourceExcerpt}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        "这些内容会停在待确认袋；确认前不会参与召回。",
                        modifier = Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                TextButton(onClick = {
                    onDismiss()
                    onOpenPendingBag()
                }) { Text("打开待确认袋") }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .62f))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("revision ${soil.revision}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("整理来源 · ${soil.organizer}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SoilSection(title: String, value: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .62f))
    Column(modifier = Modifier.padding(vertical = 11.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
