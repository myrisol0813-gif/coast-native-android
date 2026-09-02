package com.elementeracoast.app.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun PetScreen(store: DailyStore, onSnackbar: (String) -> Unit) {
    val state by store.state.collectAsState()
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("宠物系统", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("休憩箱 · 当前只保存本地状态，未来接全局状态。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            QuietDailyCard {
                Text(state.petMood.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(androidx.compose.ui.Modifier.height(4.dp))
                Text(state.petNote, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("摸摸") { store.pet(); onSnackbar("摸摸完成 · 只改本地状态") }
                SmallButton("陪睡") { store.sleepTogether(); onSnackbar("一起回到休憩箱") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("喂故事") { store.feedStory(); onSnackbar("喂了一个本地故事") }
                SmallButton("回箱") { store.returnToBox(); onSnackbar("已回箱休息") }
            }
        }
        item { Text("这里不是复杂养成系统，也不会强制把机叫回箱。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}
