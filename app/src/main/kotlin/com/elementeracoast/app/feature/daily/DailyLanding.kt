package com.elementeracoast.app.feature.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.network.CoastApiException
import com.elementeracoast.app.feature.shell.FeaturePageTopBar

internal data class DailyLandingItem(val title: String, val subtitle: String)
internal enum class DailyPage { Home, Moments, MomentCompose, Diary, DiaryCompose, Pet }

@Composable
fun DailyLanding(
    repository: DailyRepository,
    onBackToChat: () -> Unit,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    var page by remember { mutableStateOf(DailyPage.Home) }

    LaunchedEffect(repository) {
        try {
            repository.refresh()
        } catch (error: CoastApiException) {
            onSnackbar("海岸日报载入失败：${error.message}")
        }
    }

    Column(Modifier.fillMaxSize()) {
        when (page) {
            DailyPage.Home -> FeaturePageTopBar("海岸日报", "朋友圈与日记", onBackToChat)
            DailyPage.Moments -> FeaturePageTopBar("碳硅圈", "海岸内部朋友圈", { page = DailyPage.Home }, "+ 动态") { page = DailyPage.MomentCompose }
            DailyPage.MomentCompose -> FeaturePageTopBar("写碳硅圈", "直接写入海岸正式条目", { page = DailyPage.Moments })
            DailyPage.Diary -> FeaturePageTopBar("日记", "海岸里的正式纸页", { page = DailyPage.Home }, "+ 日记") { page = DailyPage.DiaryCompose }
            DailyPage.DiaryCompose -> FeaturePageTopBar("写日记", "直接写入海岸正式日记", { page = DailyPage.Diary })
            DailyPage.Pet -> FeaturePageTopBar("宠物系统", "休憩箱尚未展开", { page = DailyPage.Home })
        }

        Box(Modifier.weight(1f)) {
            when (page) {
                DailyPage.Home -> DailyHome(onOpen = { page = it }, onFutureWidgets = {
                    onSnackbar("未来小组件还没有长出来；这里只保留 PWA 母版入口。")
                })
                DailyPage.Moments -> MomentScreen(repository, onActionLogged, onSnackbar) { page = DailyPage.MomentCompose }
                DailyPage.MomentCompose -> MomentComposeScreen(repository, onActionLogged, onSnackbar) { page = DailyPage.Moments }
                DailyPage.Diary -> DiaryScreen(repository, onActionLogged, onSnackbar) { page = DailyPage.DiaryCompose }
                DailyPage.DiaryCompose -> DiaryComposeScreen(repository, onActionLogged, onSnackbar) { page = DailyPage.Diary }
                DailyPage.Pet -> PetScreen()
            }
        }
    }
}

@Composable
private fun DailyHome(onOpen: (DailyPage) -> Unit, onFutureWidgets: () -> Unit) {
    val mapping = listOf(
        Triple(DailyPage.Moments, DailyLandingItem("碳硅圈", "海岸内部朋友圈"), Icons.Default.FavoriteBorder),
        Triple(DailyPage.Diary, DailyLandingItem("日记", "留下今天的纸页"), Icons.Default.Edit),
        Triple(DailyPage.Pet, DailyLandingItem("宠物系统", "还在准备休憩箱"), Icons.Default.Pets)
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 30.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        items(mapping) { (destination, item, icon) -> DailyHomeCard(item, icon) { onOpen(destination) } }
        item { DailyHomeCard(DailyLandingItem("未来小组件", "以后再慢慢长出来"), Icons.Default.Add, onFutureWidgets) }
    }
}

@Composable
private fun DailyHomeCard(item: DailyLandingItem, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, shape, clip = false).background(MaterialTheme.colorScheme.surfaceVariant, shape).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.size(16.dp))
        Column {
            Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Active Daily destinations. Future widgets is a visual PWA-mother placeholder, not an active page. */
internal fun dailyLandingItems(): List<DailyLandingItem> = listOf(
    DailyLandingItem("碳硅圈", "海岸内部朋友圈"),
    DailyLandingItem("日记", "留下今天的纸页"),
    DailyLandingItem("宠物系统", "还在准备休憩箱")
)
