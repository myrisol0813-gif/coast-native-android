package com.elementeracoast.app.feature.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.shell.FeaturePageTopBar

@Composable
fun IslandLetterScreen(
    store: IslandLetterStore,
    conversationId: String,
    modelName: String,
    onBack: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    var text by remember(conversationId, modelName) {
        mutableStateOf(store.read(conversationId, modelName))
    }

    Column {
        FeaturePageTopBar(
            title = "登岛信",
            subtitle = "$modelName · 当前窗口独立保存",
            onBack = onBack
        )
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .padding(15.dp)
                ) {
                    Text("一封给当前模型的入住信", fontWeight = FontWeight.Bold)
                    Text(
                        "这不是记忆库。它按当前窗口与当前模型独立保存；真正递给模型并等待回复，要等后端接线。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(IslandLetterStore.MAX_LENGTH) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 420.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    LetterButton("递出登岛信") {
                        text = store.save(conversationId, modelName, text)
                        onSnackbar("登岛信已保存在当前窗口；Native 读信接口尚未接线。")
                    }
                    LetterButton("保存") {
                        text = store.save(conversationId, modelName, text)
                        onSnackbar("登岛信已保存在当前窗口与当前模型")
                    }
                    LetterButton("恢复默认") {
                        text = store.reset(conversationId, modelName)
                        onSnackbar("已恢复当前模型的默认登岛信")
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
}
