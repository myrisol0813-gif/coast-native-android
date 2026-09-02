package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.shell.FeaturePageTopBar

internal data class MemoryLandingItem(val title: String, val subtitle: String)

@Composable
fun MemoryLanding(
    store: MemoryStore,
    onBackToChat: () -> Unit,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    var tab by remember { mutableStateOf(MemoryTab.Memory) }
    var pendingCreate by remember { mutableStateOf<MemoryTab?>(null) }

    val subtitle = when (tab) {
        MemoryTab.Memory -> "记忆库 · 已经确认的长期纸条"
        MemoryTab.Seed -> "种子库 · 未完成但有生长性的意象，不是已确认事实"
        MemoryTab.Instructions -> "自定义指令 · 独立的单份 active 文档"
        MemoryTab.Worldbook -> "专有名词按关键词出现，不和记忆库混放"
    }
    val title = if (tab == MemoryTab.Worldbook) "海岸词典" else "轨迹记忆"
    val action = when (tab) {
        MemoryTab.Memory, MemoryTab.Seed -> "新增"
        MemoryTab.Worldbook -> "+ 词条"
        MemoryTab.Instructions -> null
    }
    val consumeCreate: () -> Unit = { if (pendingCreate == tab) pendingCreate = null }

    Column(Modifier.fillMaxSize()) {
        FeaturePageTopBar(
            title = title,
            subtitle = subtitle,
            onBack = {
                pendingCreate = null
                if (tab == MemoryTab.Worldbook) tab = MemoryTab.Memory else onBackToChat()
            },
            actionLabel = action,
            onAction = action?.let { { pendingCreate = tab } }
        )

        Box(Modifier.weight(1f)) {
            when (tab) {
                MemoryTab.Memory -> Column(Modifier.fillMaxSize()) {
                    MemoryTabs(tab) { next -> pendingCreate = null; tab = next }
                    MemoryLibraryScreen(
                        store = store,
                        createRequested = pendingCreate == MemoryTab.Memory,
                        onCreateConsumed = consumeCreate,
                        onActionLogged = onActionLogged,
                        onSnackbar = onSnackbar
                    )
                }
                MemoryTab.Seed -> Column(Modifier.fillMaxSize()) {
                    MemoryTabs(tab) { next -> pendingCreate = null; tab = next }
                    SeedLibraryScreen(
                        store = store,
                        createRequested = pendingCreate == MemoryTab.Seed,
                        onCreateConsumed = consumeCreate,
                        onSnackbar = onSnackbar
                    )
                }
                MemoryTab.Instructions -> Column(Modifier.fillMaxSize()) {
                    MemoryTabs(tab) { next -> pendingCreate = null; tab = next }
                    Spacer(Modifier.height(4.dp))
                    CustomInstructionsScreen(store, onSnackbar)
                }
                MemoryTab.Worldbook -> WorldbookScreen(
                    store = store,
                    createRequested = pendingCreate == MemoryTab.Worldbook,
                    onCreateConsumed = consumeCreate,
                    onSnackbar = onSnackbar
                )
            }
        }
    }
}

internal fun memoryLandingItems(): List<MemoryLandingItem> = listOf(
    MemoryLandingItem("记忆库", "已经确认的长期纸条"),
    MemoryLandingItem("种子库", "未完成但有生长性的意象"),
    MemoryLandingItem("世界书", "海岸词典"),
    MemoryLandingItem("自定义指令", "独立的单份 active 文档")
)
