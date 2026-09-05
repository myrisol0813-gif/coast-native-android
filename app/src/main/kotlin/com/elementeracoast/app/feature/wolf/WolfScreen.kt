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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.remote.RemoteDevUpdate
import com.elementeracoast.app.feature.serpentdesk.DevHandsRepository
import com.elementeracoast.app.feature.shell.FeatureLocalBackBar
import com.elementeracoast.app.ui.theme.SnowLetterSurface
import com.elementeracoast.app.ui.theme.SnowLetterSurfaceRole
import kotlinx.coroutines.launch

@Composable
fun WolfScreen(
    store: WolfStore,
    shellState: CoastShellState,
    messages: List<ChatMessage>,
    devHands: DevHandsRepository,
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
            onOpen = { page = it }
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
                WolfDestination.Update -> WolfUpdateScreen(devHands, onSnackbar)
            }
        }
    }
}

@Composable
private fun WolfHome(
    state: WolfState,
    model: String,
    onOpen: (WolfDestination) -> Unit
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
                WolfDestination.Update -> "当前 Native：${BuildConfig.VERSION_NAME}"
                else -> destination.subtitle
            }
            WolfRow(destination.title, subtitle) { onOpen(destination) }
        }
    }
}

@Composable
private fun WolfUpdateScreen(
    repository: DevHandsRepository,
    onSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var update by remember { mutableStateOf<RemoteDevUpdate?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                update = repository.latestUpdate().update
            } catch (cause: Throwable) {
                error = cause.message ?: "更新信息读取失败。"
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val native = update?.native
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SnowLetterSurface(
                modifier = Modifier.fillMaxWidth(),
                role = SnowLetterSurfaceRole.StatusCard,
                fallbackColor = MaterialTheme.colorScheme.surfaceVariant,
                fallbackShape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                    Text(native?.versionName ?: "版本与更新", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    WolfUpdateFact("PWA cache", update?.pwaCacheVersion ?: "—")
                    WolfUpdateFact("versionCode", native?.versionCode?.toString() ?: "—")
                    WolfUpdateFact("applicationId", native?.applicationId ?: "—")
                    WolfUpdateFact("稳定签名", when (native?.stableSigning) { true -> "是"; false -> "否"; null -> "—" })
                    WolfUpdateFact("可覆盖安装", when (native?.overwriteInstallable) { true -> "是"; false -> "否"; null -> "—" })
                    WolfUpdateFact("APK SHA-256", native?.apkSha256 ?: "—", mono = true)
                    WolfUpdateFact("Artifact", native?.artifactName ?: "—")
                    native?.updateTime?.let { WolfUpdateFact("更新时间", it) }
                    native?.updateNotes?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    native?.knownRisk?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("已知风险：$it", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    update?.reason?.takeIf { update?.available != true }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = ::refresh, enabled = !busy) { Text(if (busy) "刷新中" else "刷新") }
                        if (update?.available == true && !native?.downloadUrl.isNullOrBlank()) {
                            Button(onClick = {
                                runCatching { uriHandler.openUri(repository.absoluteUrl(native!!.downloadUrl!!)) }
                                    .onFailure { onSnackbar("APK 下载入口暂时无法打开：${it.message ?: "未知错误"}") }
                            }) { Text("下载 APK") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WolfUpdateFact(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.weight(.8f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, modifier = Modifier.weight(1.5f), fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default, style = MaterialTheme.typography.bodySmall)
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
