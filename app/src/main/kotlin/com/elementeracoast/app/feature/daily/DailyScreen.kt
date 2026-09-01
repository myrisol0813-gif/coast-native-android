package com.elementeracoast.app.feature.daily

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
import com.elementeracoast.app.core.local.LocalDailyStore
import com.elementeracoast.app.core.model.RoomType

enum class DailyPage { Home, Moments, Diaries, Pet }

@Composable
fun DailyScreen(
    store: LocalDailyStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    var page by remember { mutableStateOf(DailyPage.Home) }
    when (page) {
        DailyPage.Home -> DailyHome { page = it }
        DailyPage.Moments -> MomentsScreen(store, actionLogStore, roomType, conversationId, onSnackbar)
        DailyPage.Diaries -> DiaryScreen(store, actionLogStore, roomType, conversationId, onSnackbar)
        DailyPage.Pet -> PetScreen(store, onSnackbar)
    }
}

@Composable
private fun DailyHome(onOpen: (DailyPage) -> Unit) {
    val cards = listOf(
        DailyPage.Moments to ("碳硅圈" to "发布、编辑、点赞与评论"),
        DailyPage.Diaries to ("日记" to "日期、天气、心情与标签"),
        DailyPage.Pet to ("宠物系统" to "休息、陪睡、喂故事与回箱")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("海岸日报", style = MaterialTheme.typography.headlineMedium)
            Text("Native local-only", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        cards.forEach { (page, copy) ->
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
