package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.daily.SmallButton
import com.elementeracoast.app.feature.wolf.WolfTextField

@Composable
internal fun CustomInstructionsScreen(store: MemoryStore, onSnackbar: (String) -> Unit) {
    val state by store.state.collectAsState()
    var draft by remember(state.customInstructions) { mutableStateOf(state.customInstructions) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("自定义指令", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("会在未来后端接线时参与上下文。本轮不会发送给模型。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { WolfTextField("本地指令草稿", draft, { draft = it }, minLines = 8) }
        item { SmallButton("保存") { store.saveCustomInstructions(draft); onSnackbar("自定义指令已保存在本机") } }
        item { SmallButton("清空") { draft = ""; store.clearCustomInstructions(); onSnackbar("本地自定义指令已清空") } }
        item { Text("fake generation 只会读取很短的本地片段用于 UI 展示，不会冒充真实上下文注入。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}
