package com.elementeracoast.app.feature.wolf

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalChatStore
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.RoomType

enum class WolfPage {
    Home,
    Profile,
    Appearance,
    ChatRecords,
    ModelBox,
    BasicSettings,
    Diagnostics
}

@Composable
fun WolfScreen(
    preferencesStore: LocalPreferencesStore,
    chatStore: LocalChatStore,
    actionLogStore: LocalActionLogStore,
    initialPage: WolfPage = WolfPage.Home,
    currentRoomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    var page by remember(initialPage) { mutableStateOf(initialPage) }
    when (page) {
        WolfPage.Home -> WolfHome(onOpen = { page = it })
        WolfPage.Profile -> ProfileScreen(
            store = preferencesStore,
            onAppearance = { page = WolfPage.Appearance },
            onSnackbar = onSnackbar
        )
        WolfPage.Appearance -> AppearanceScreen(preferencesStore, onSnackbar)
        WolfPage.ChatRecords -> ChatRecordsScreen(
            chatStore = chatStore,
            preferencesStore = preferencesStore,
            actionLogStore = actionLogStore,
            roomType = currentRoomType,
            conversationId = conversationId,
            onSnackbar = onSnackbar
        )
        WolfPage.ModelBox -> ModelBoxScreen(
            store = preferencesStore,
            actionLogStore = actionLogStore,
            roomType = currentRoomType,
            conversationId = conversationId,
            onSnackbar = onSnackbar
        )
        WolfPage.BasicSettings -> BasicSettingsScreen(preferencesStore)
        WolfPage.Diagnostics -> DiagnosticsScreen(
            preferencesStore = preferencesStore,
            chatStore = chatStore,
            roomType = currentRoomType,
            conversationId = conversationId
        )
    }
}

@Composable
private fun WolfHome(onOpen: (WolfPage) -> Unit) {
    val entries = listOf(
        WolfPage.Profile to ("个人资料" to "昵称、聊天署名与显示资料"),
        WolfPage.Appearance to ("外观" to "主题、用户气泡与重点色"),
        WolfPage.ChatRecords to ("聊天记录" to "导出 JSON / HTML · 导入 JSON"),
        WolfPage.ModelBox to ("模型箱" to "本地模型列表与当前模型"),
        WolfPage.BasicSettings to ("基本设置" to "回答长度、流式输出、记忆召回与世界书"),
        WolfPage.Diagnostics to ("关于与诊断" to "APK、本地状态与接线状态")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Wolf Den / 小狼窝", style = MaterialTheme.typography.headlineMedium)
            Text(
                "小寒侧显示资料 · 外观 · 聊天与运行设置",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
        entries.forEach { (page, copy) ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .clickable { onOpen(page) }
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(copy.first, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(copy.second, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
