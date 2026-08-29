package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.elementeracoast.app.core.theme.CoastShapes
import com.elementeracoast.app.core.theme.CoastSpacing
import com.elementeracoast.app.model.CoastModel

@Composable
fun ModelSelector(
    models: List<CoastModel>,
    currentModelId: String,
    onDismiss: () -> Unit,
    onSelect: (CoastModel) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 620.dp),
            shape = CoastShapes.overlay,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 0.dp,
            shadowElevation = 18.dp,
        ) {
            Column {
                Column(Modifier.padding(horizontal = CoastSpacing.lg, vertical = CoastSpacing.md)) {
                    Text(
                        "选择模型",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "从海岸模型箱切换当前聊天线路",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = CoastSpacing.sm,
                        end = CoastSpacing.sm,
                        bottom = CoastSpacing.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(models, key = { it.id }) { model ->
                        ModelRow(
                            model = model,
                            selected = model.id == currentModelId,
                            onClick = {
                                if (model.available) {
                                    onSelect(model)
                                    onDismiss()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelRow(model: CoastModel, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.09f) else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, CoastShapes.control)
            .clickable(enabled = model.available, onClick = onClick)
            .padding(horizontal = CoastSpacing.md, vertical = CoastSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = model.name.ifBlank { model.id },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (model.available) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = model.id,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = when {
                !model.available -> "不可用"
                selected -> "当前"
                model.isFree -> "FREE"
                else -> ""
            },
            modifier = Modifier.padding(start = CoastSpacing.sm),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f),
        )
    }
}
