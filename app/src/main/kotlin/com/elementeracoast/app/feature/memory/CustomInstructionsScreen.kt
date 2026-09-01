package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalMemoryStore

@Composable
internal fun CustomInstructionsScreen(store: LocalMemoryStore, onSnackbar: (String) -> Unit) {
    val state by store.state.collectAsState()
    var draft by remember(state.customInstructions) { mutableStateOf(state.customInstructions) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("自定义指令", style = MaterialTheme.typography.headlineMedium)
            Text("未来后端接线时参与上下文", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text(
                "本轮只保存在 APK 本机，不会发送给真实模型。local fake generation 只把它视作演示资料，不伪装成真实上下文注入。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.take(16000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("指令正文") },
                minLines = 10
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        store.saveCustomInstructions(draft)
                        onSnackbar("自定义指令已保存在本机")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存") }
                OutlinedButton(
                    onClick = {
                        draft = ""
                        store.clearCustomInstructions()
                        onSnackbar("本地自定义指令已清空")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("清空") }
            }
        }
    }
}
