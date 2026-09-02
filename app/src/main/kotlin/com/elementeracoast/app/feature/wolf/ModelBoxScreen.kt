package com.elementeracoast.app.feature.wolf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ModelBoxScreen(
    models: List<String>,
    current: String,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var expandedModel by remember { mutableStateOf<String?>(null) }
    val grouped = remember(models, current) { groupedUnselectedModels(models, current) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("模型箱", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "OpenRouter 目录分区 · Native 本地壳",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text("刷新", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            Text("当前模型", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            CurrentModelCard(current)
        }

        item {
            Text("模型目录", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            ModelCatalogCard(
                grouped = grouped,
                expandedModel = expandedModel,
                onToggle = { id -> expandedModel = if (expandedModel == id) null else id },
                onSelect = { id ->
                    onSelect(id)
                    expandedModel = null
                }
            )
        }

        item {
            Text(
                "目录刷新将在后端接线后读取 OpenRouter 并自动归入对应分区；当前不会伪造网络结果。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CurrentModelCard(model: String) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, clip = false)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .10f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(model.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("正在使用", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ModelCatalogCard(
    grouped: Map<ModelSeries, List<LocalModelCatalogItem>>,
    expandedModel: String?,
    onToggle: (String) -> Unit,
    onSelect: (String) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, clip = false)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ModelSeries.entries.forEach { series ->
            val entries = grouped[series].orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(series.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (entries.isEmpty()) {
                    Text("暂无目录项", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    entries.forEach { item ->
                        ModelCatalogBubble(
                            item = item,
                            expanded = expandedModel == item.id,
                            onToggle = { onToggle(item.id) },
                            onSelect = { onSelect(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelCatalogBubble(
    item: LocalModelCatalogItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 15.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(if (expanded) "⌃" else "⌄", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(item.id, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(item.series.title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text("目录来源：本地缓存；接线后由 OpenRouter 刷新", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(9.dp))
            Text(
                "设为当前",
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .12f), RoundedCornerShape(13.dp))
                    .clickable(onClick = onSelect)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
