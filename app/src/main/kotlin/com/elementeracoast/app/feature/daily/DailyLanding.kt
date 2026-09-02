package com.elementeracoast.app.feature.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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

internal data class DailyLandingItem(val title: String, val subtitle: String)
internal enum class DailyPage { Moments, Diary, Pet }

@Composable
fun DailyLanding(
    store: DailyStore,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val (page, setPage) = remember { mutableStateOf<DailyPage?>(null) }
    when (page) {
        null -> DailyHome { setPage(it) }
        DailyPage.Moments -> MomentScreen(store, onActionLogged, onSnackbar)
        DailyPage.Diary -> DiaryScreen(store, onActionLogged, onSnackbar)
        DailyPage.Pet -> PetScreen(store, onSnackbar)
    }
}

@Composable
private fun DailyHome(onOpen: (DailyPage) -> Unit) {
    val mapping = listOf(
        DailyPage.Moments to DailyLandingItem("碳硅圈", "海岸内部朋友圈"),
        DailyPage.Diary to DailyLandingItem("日记", "留下今天的纸页"),
        DailyPage.Pet to DailyLandingItem("宠物系统", "休憩箱 · 本地状态轻壳")
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(mapping) { (destination, item) ->
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                    .clickable { onOpen(destination) }
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

internal fun dailyLandingItems(): List<DailyLandingItem> = listOf(
    DailyLandingItem("碳硅圈", "海岸内部朋友圈"),
    DailyLandingItem("日记", "留下今天的纸页"),
    DailyLandingItem("宠物系统", "休憩箱 · 本地状态轻壳")
)
