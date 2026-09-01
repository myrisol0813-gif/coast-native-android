package com.elementeracoast.app.feature.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalDailyStore
import com.elementeracoast.app.core.model.PetStatus

@Composable
internal fun PetScreen(store: LocalDailyStore, onSnackbar: (String) -> Unit) {
    val state by store.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("宠物系统", style = MaterialTheme.typography.headlineMedium)
            Text("本轮只做休憩箱本地状态轻壳", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("当前：${state.petStatus.label}", fontWeight = FontWeight.SemiBold)
                Text("未来接全局状态", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { store.setPetStatus(PetStatus.Active); onSnackbar("摸摸完成 · 本地状态变为活跃") }) { Text("摸摸") }
                Button(onClick = { store.setPetStatus(PetStatus.Sleepy); onSnackbar("陪睡中 · 本地状态变为困倦") }) { Text("陪睡") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { store.setPetStatus(PetStatus.Active); onSnackbar("故事喂进休憩箱啦 · 仅本地") }) { Text("喂故事") }
                Button(onClick = { store.setPetStatus(PetStatus.Resting); onSnackbar("已经回箱休息") }) { Text("回箱") }
            }
        }
    }
}
