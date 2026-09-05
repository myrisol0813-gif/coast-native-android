package com.elementeracoast.app.feature.wolf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.feature.shell.FeatureLocalBackBar
import com.elementeracoast.app.ui.theme.SnowLetterSurface
import com.elementeracoast.app.ui.theme.SnowLetterSurfaceRole

@Composable
fun WolfScreen(
    store: WolfStore,
    shellState: CoastShellState,
    messages: List<ChatMessage>,
    onSelectModel: (String) -> Unit,
    onRefreshModels: () -> Unit,
    onImportMessages: (List<ChatMessage>) -> Unit,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var page by remember { mutableStateOf<WolfDestination?>(null) }
    val current = page

    if (current == null) {
        WolfHome(
            state = state,
            model = shellState.currentModel,
            onOpen = { page = it },
            onUpdate = {
                onSnackbar("当前版本 ${BuildConfig.VERSION_NAME}。固定签名与 APK 更新源接通前，请继续使用测试安装包更新。")
            }
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        FeatureLocalBackBar("小狼窝") { page = null }
        Box(Modifier.weight(1f)) {
            when (current) {
                WolfDestination.Profile -> ProfileScreen(state, store, onAppearance = { page = WolfDestination.Appearance }, onSnackbar = onSnackbar)
                WolfDestination.Appearance -> AppearanceScreen(state, store, onSnackbar)
                WolfDestination.ChatRecords -> ChatRecordsScreen(
                    profile = state.profile,
                    messages = messages,
                    onImportMessages = onImportMessages,
                    onActionLogged = onActionLogged,
                    onSnackbar = onSnackbar
                )
                WolfDestination.ModelBox -> ModelBoxScreen(
                    models = shellState.models,
                    current = shellState.currentModel,
                    onSelect = onSelectModel,
                    onRefresh = onRefreshModels
                )
                WolfDestination.BasicSettings -> BasicSettingsScreen(state.basic, store, onSnackbar)
                WolfDestination.Diagnostics -> DiagnosticsScreen(shellState, state)
            }
        }
    }
}

@Composable
private fun WolfHome(
    state: WolfState,
    model: String,
    onOpen: (WolfDestination) -> Unit,
    onUpdate: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("小寒侧", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
        }
        items(WolfDestination.entries) { destination ->
            val subtitle = when (destination) {
                WolfDestination.Profile -> "${state.profile.nickname} · ${state.profile.signature}"
                WolfDestination.Appearance -> "${state.appearance.theme.label} · 用户气泡 · 重点色"
                WolfDestination.ModelBox -> "当前：${model.substringAfterLast('/').take(32)}"
                else -> destination.subtitle
            }
            WolfRow(destination.title, subtitle) { onOpen(destination) }
        }
        item {
            WolfRow(
                title = "版本与更新",
                subtitle = "当前版本：${BuildConfig.VERSION_NAME}",
                onClick = onUpdate
            )
        }
    }
}

@Composable
internal fun WolfRow(title: String, subtitle: String, onClick: () -> Unit) {
    SnowLetterSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        role = SnowLetterSurfaceRole.StatusCard,
        fallbackColor = MaterialTheme.colorScheme.surfaceVariant,
        fallbackShape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 19.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
