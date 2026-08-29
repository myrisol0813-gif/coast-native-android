package com.elementeracoast.app.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.model.CoastModel

@Composable
fun ModelSelector(
    models: List<CoastModel>,
    currentModelId: String,
    onDismiss: () -> Unit,
    onSelect: (CoastModel) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("选择海岸模型", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(models, key = { it.id }) { model ->
                Column(
                    Modifier.fillMaxWidth().clickable(enabled = model.available) { onSelect(model); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                ) {
                    Text(
                        model.name.ifBlank { model.id },
                        fontWeight = if (model.id == currentModelId) FontWeight.Bold else FontWeight.Normal,
                        color = if (model.available) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        buildString { append(model.id); if (model.isFree) append(" · free"); if (!model.available) append(" · unavailable") },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
