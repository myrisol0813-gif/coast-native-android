package com.elementeracoast.app.feature.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.feature.shell.FeatureLocalBackBar

internal data class MemoryLandingItem(val title: String, val subtitle: String)
internal enum class MemoryPage { Library, Seeds, Worldbook, Instructions }

@Composable
fun MemoryLanding(
    store: MemoryStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val (page, setPage) = remember { mutableStateOf<MemoryPage?>(null) }
    if (page == null) {
        MemoryHome { setPage(it) }
        return
    }

    Column(Modifier.fillMaxSize()) {
        FeatureLocalBackBar("轨迹 / 记忆") { setPage(null) }
        Box(Modifier.weight(1f)) {
            when (page) {
                MemoryPage.Library -> MemoryLibraryScreen(store, onActionLogged)
                MemoryPage.Seeds -> SeedLibraryScreen(store)
                MemoryPage.Worldbook -> WorldbookScreen(store)
                MemoryPage.Instructions -> CustomInstructionsScreen(store, onSnackbar)
                null -> Unit
            }
        }
    }
}

@Composable
private fun MemoryHome(onOpen: (MemoryPage) -> Unit) {
    val mapping = listOf(
        MemoryPage.Library to MemoryLandingItem("记忆库", "已经确认的长期纸条"),
        MemoryPage.Seeds to MemoryLandingItem("种子库", "active / dormant 的待发芽内容"),
        MemoryPage.Worldbook to MemoryLandingItem("世界书", "海岸稳定词条与启停"),
        MemoryPage.Instructions to MemoryLandingItem("自定义指令", "未来后端接线时参与上下文")
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(mapping) { (destination, item) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                    .clickable { onOpen(destination) }
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

internal fun memoryLandingItems(): List<MemoryLandingItem> = listOf(
    MemoryLandingItem("记忆库", "已经确认的长期纸条"),
    MemoryLandingItem("种子库", "active / dormant 的待发芽内容"),
    MemoryLandingItem("世界书", "海岸稳定词条与启停"),
    MemoryLandingItem("自定义指令", "未来后端接线时参与上下文")
)
