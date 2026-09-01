package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalMemoryStore
import com.elementeracoast.app.core.model.RoomType

enum class MemoryPage { Home, Memories, Seeds, Worldbook, Instructions }

@Composable
fun MemoryScreen(
    store: LocalMemoryStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    var page by remember { mutableStateOf(MemoryPage.Home) }
    when (page) {
        MemoryPage.Home -> MemoryHome { page = it }
        MemoryPage.Memories -> MemoryRecordsScreen(store, actionLogStore, roomType, conversationId, onSnackbar)
        MemoryPage.Seeds -> SeedScreen(store, actionLogStore, roomType, conversationId, onSnackbar)
        MemoryPage.Worldbook -> WorldbookScreen(store, onSnackbar)
        MemoryPage.Instructions -> CustomInstructionsScreen(store, onSnackbar)
    }
}

@Composable
private fun MemoryHome(onOpen: (MemoryPage) -> Unit) {
    val entries = listOf(
        MemoryPage.Memories to ("记忆库" to "新增、搜索、编辑与分类"),
        MemoryPage.Seeds to ("种子库" to "active / dormant 的本地种子"),
        MemoryPage.Worldbook to ("世界书" to "词条新增、搜索与启停"),
        MemoryPage.Instructions to ("自定义指令" to "本地编辑；未来接线时参与上下文")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("轨迹 / 记忆", style = MaterialTheme.typography.headlineMedium)
            Text("PWA Memory v2 · Native local-only", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        entries.forEach { (page, copy) ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)).clickable { onOpen(page) }.padding(18.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(copy.first, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(copy.second, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
