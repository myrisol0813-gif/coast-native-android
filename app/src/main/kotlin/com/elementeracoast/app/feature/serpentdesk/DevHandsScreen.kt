package com.elementeracoast.app.feature.serpentdesk

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.elementeracoast.app.core.remote.RemoteDevRun
import com.elementeracoast.app.core.remote.RemoteDevSelfCheckResponse
import com.elementeracoast.app.core.remote.RemoteDevSettings
import com.elementeracoast.app.core.remote.RemoteDevUpdate
import com.elementeracoast.app.feature.shell.FeatureLocalBackBar
import com.elementeracoast.app.ui.theme.SnowLetterSurface
import com.elementeracoast.app.ui.theme.SnowLetterSurfaceRole
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

private enum class DevHandsPage {
    Home,
    Switches,
    Update,
    Logs,
    GitHub,
    Notion
}

private data class PendingDevConfirmation(
    val target: String,
    val action: String,
    val params: JsonObject,
    val expected: String,
    val dangerous: Boolean
)

@Composable
fun DevHandsScreen(
    repository: DevHandsRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val parser = remember { Json { ignoreUnknownKeys = true } }
    var page by remember { mutableStateOf(DevHandsPage.Home) }
    var settings by remember { mutableStateOf(RemoteDevSettings()) }
    var selfCheck by remember { mutableStateOf<RemoteDevSelfCheckResponse?>(null) }
    var update by remember { mutableStateOf<RemoteDevUpdate?>(null) }
    var logs by remember { mutableStateOf<List<RemoteDevRun>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<PendingDevConfirmation?>(null) }
    var confirmText by remember { mutableStateOf("") }

    suspend fun refreshOverview() {
        busy = true
        message = null
        try {
            val check = repository.selfCheck()
            selfCheck = check
            settings = check.settings
        } catch (error: Throwable) {
            message = error.message ?: "开发手自检失败。"
        } finally {
            busy = false
        }
    }

    suspend fun refreshUpdate() {
        busy = true
        message = null
        try {
            update = repository.latestUpdate().update
        } catch (error: Throwable) {
            message = error.message ?: "更新信息读取失败。"
        } finally {
            busy = false
        }
    }

    suspend fun refreshLogs() {
        busy = true
        message = null
        try {
            logs = repository.logs(120).runs
        } catch (error: Throwable) {
            message = error.message ?: "施工日志读取失败。"
        } finally {
            busy = false
        }
    }

    suspend fun executeDev(target: String, action: String, params: JsonObject, confirmation: String? = null) {
        busy = true
        message = null
        try {
            val response = if (target == "notion") repository.notion(action, params, confirmation)
            else repository.github(action, params, confirmation)
            resultText = response.result?.toString().orEmpty()
            pending = null
            confirmText = ""
            message = "施工台动作完成。"
        } catch (error: DevHandsException) {
            if (error.status == 409 && !error.confirmationText.isNullOrBlank()) {
                pending = PendingDevConfirmation(
                    target = target,
                    action = action,
                    params = params,
                    expected = error.confirmationText,
                    dangerous = error.operationType == "dangerous"
                )
                confirmText = ""
                message = if (error.operationType == "dangerous") "这个动作需要强确认。" else "这个写操作需要确认。"
            } else message = error.message
        } catch (error: Throwable) {
            message = error.message ?: "施工台动作失败。"
        } finally {
            busy = false
        }
    }

    LaunchedEffect(Unit) { refreshOverview() }

    Column(Modifier.fillMaxSize()) {
        FeatureLocalBackBar(
            when (page) {
                DevHandsPage.Home -> "海岸施工台"
                DevHandsPage.Switches -> "工具开关"
                DevHandsPage.Update -> "版本与更新"
                DevHandsPage.Logs -> "施工日志"
                DevHandsPage.GitHub -> "GitHub 开发手"
                DevHandsPage.Notion -> "Notion 小纸条"
            }
        ) {
            if (page == DevHandsPage.Home) onBack() else {
                page = DevHandsPage.Home
                resultText = ""
                pending = null
                message = null
            }
        }
        when (page) {
            DevHandsPage.Home -> DevHandsHome(
                selfCheck = selfCheck,
                busy = busy,
                message = message,
                onRefresh = { scope.launch { refreshOverview() } },
                onOpen = { next ->
                    page = next
                    when (next) {
                        DevHandsPage.Update -> scope.launch { refreshUpdate() }
                        DevHandsPage.Logs -> scope.launch { refreshLogs() }
                        else -> Unit
                    }
                }
            )
            DevHandsPage.Switches -> DevHandsSwitches(
                settings = settings,
                busy = busy,
                message = message,
                onChange = { settings = it },
                onSave = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            settings = repository.saveSettings(settings)
                            selfCheck = null
                            message = "工具开关已经保存到海岸后端。"
                        } catch (error: Throwable) {
                            message = error.message ?: "工具开关保存失败。"
                        } finally {
                            busy = false
                        }
                    }
                }
            )
            DevHandsPage.Update -> DevHandsUpdate(
                update = update,
                busy = busy,
                message = message,
                onRefresh = { scope.launch { refreshUpdate() } },
                onDownload = { relative -> uriHandler.openUri(repository.absoluteUrl(relative)) }
            )
            DevHandsPage.Logs -> DevHandsLogs(
                logs = logs,
                busy = busy,
                message = message,
                onRefresh = { scope.launch { refreshLogs() } }
            )
            DevHandsPage.GitHub -> DevHandsCommand(
                title = "GitHub",
                defaultAction = "get_repo",
                defaultParams = "{\n  \"repo\": \"myrisol0813-gif/elementera-coast\"\n}",
                resultText = resultText,
                busy = busy,
                message = message,
                pending = pending,
                confirmText = confirmText,
                onConfirmTextChange = { confirmText = it },
                onRun = { action, text ->
                    val params = runCatching { parser.parseToJsonElement(text).jsonObject }.getOrElse {
                        message = "参数必须是 JSON 对象。"
                        return@DevHandsCommand
                    }
                    scope.launch { executeDev("github", action, params) }
                },
                onConfirm = {
                    val current = pending ?: return@DevHandsCommand
                    if (confirmText != current.expected) {
                        message = "确认文本还没有逐字写对。"
                    } else scope.launch { executeDev(current.target, current.action, current.params, confirmText) }
                },
                onCancelConfirm = { pending = null; confirmText = ""; message = "这次写入没有执行。" }
            )
            DevHandsPage.Notion -> DevHandsCommand(
                title = "Notion",
                defaultAction = "read_root_page",
                defaultParams = "{}",
                resultText = resultText,
                busy = busy,
                message = message,
                pending = pending,
                confirmText = confirmText,
                onConfirmTextChange = { confirmText = it },
                onRun = { action, text ->
                    val params = runCatching { parser.parseToJsonElement(text).jsonObject }.getOrElse {
                        message = "参数必须是 JSON 对象。"
                        return@DevHandsCommand
                    }
                    scope.launch { executeDev("notion", action, params) }
                },
                onConfirm = {
                    val current = pending ?: return@DevHandsCommand
                    if (confirmText != current.expected) {
                        message = "确认文本还没有逐字写对。"
                    } else scope.launch { executeDev(current.target, current.action, current.params, confirmText) }
                },
                onCancelConfirm = { pending = null; confirmText = ""; message = "这次写入没有执行。" }
            )
        }
    }
}

@Composable
private fun DevHandsHome(
    selfCheck: RemoteDevSelfCheckResponse?,
    busy: Boolean,
    message: String?,
    onRefresh: () -> Unit,
    onOpen: (DevHandsPage) -> Unit
) {
    val github = selfCheck?.github
    val notion = selfCheck?.notion
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoftPanel {
                Text("开发手", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text(
                    "GitHub 修家，Notion 记事。token 留在海岸后端，普通聊天不会拿到这些手。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                StatusLine("GitHub token", github?.githubTokenPresent)
                StatusLine("allowlist 仓库", github?.repoMetadataReadable)
                StatusLine("Actions", github?.canReadActions)
                StatusLine("Notion token", notion?.notionTokenPresent)
                StatusLine("工作日志 root", notion?.rootPageReadable)
                notion?.rootPageTitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) }
                Spacer(Modifier.height(10.dp))
                Button(onClick = onRefresh, enabled = !busy) { Text(if (busy) "正在自检" else "重新自检") }
            }
        }
        item { DeskEntry("GitHub 开发手", "仓库 · 文件 · branch · PR · issue") { onOpen(DevHandsPage.GitHub) } }
        item { DeskEntry("CI / APK 构建", "workflow runs 与 artifact 由同一 GitHub 手读取") { onOpen(DevHandsPage.GitHub) } }
        item { DeskEntry("Notion 小纸条", "只在 Elementera Coast 工作日志 root 下走动") { onOpen(DevHandsPage.Notion) } }
        item { DeskEntry("版本与更新", "PWA cache · Native · APK · SHA-256") { onOpen(DevHandsPage.Update) } }
        item { DeskEntry("工具开关", "PWA / APK 共用同一份后端设置") { onOpen(DevHandsPage.Switches) } }
        item { DeskEntry("本轮施工日志", "GitHub / Notion / CI / 小狼窝 · 脱敏") { onOpen(DevHandsPage.Logs) } }
        message?.let { item { StatusMessage(it) } }
    }
}

@Composable
private fun DevHandsSwitches(
    settings: RemoteDevSettings,
    busy: Boolean,
    message: String?,
    onChange: (RemoteDevSettings) -> Unit,
    onSave: () -> Unit
) {
    val rows = listOf(
        Triple("GitHub 读工具", "允许读取 allowlist 仓库", settings.githubRead) to { value: Boolean -> onChange(settings.copy(githubRead = value)) },
        Triple("GitHub 写工具", "写入仍需逐次确认", settings.githubWrite) to { value: Boolean -> onChange(settings.copy(githubWrite = value)) },
        Triple("GitHub 危险动作", "即使开启仍需精确强确认", settings.githubDangerous) to { value: Boolean -> onChange(settings.copy(githubDangerous = value)) },
        Triple("CI / Actions", "读取 runs、jobs、logs", settings.ciActions) to { value: Boolean -> onChange(settings.copy(ciActions = value)) },
        Triple("APK artifact", "读取并下载构建产物", settings.apkArtifact) to { value: Boolean -> onChange(settings.copy(apkArtifact = value)) },
        Triple("小狼窝更新", "读取最新 PWA / Native 更新", settings.wolfUpdate) to { value: Boolean -> onChange(settings.copy(wolfUpdate = value)) },
        Triple("Notion 读工具", "仅 root 与子页面", settings.notionRead) to { value: Boolean -> onChange(settings.copy(notionRead = value)) },
        Triple("Notion 写工具", "写入仍需逐次确认", settings.notionWrite) to { value: Boolean -> onChange(settings.copy(notionWrite = value)) },
        Triple("Notion 删除工具", "即使开启仍需精确强确认", settings.notionDelete) to { value: Boolean -> onChange(settings.copy(notionDelete = value)) }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rows.size) { index ->
            val row = rows[index]
            SoftPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.first.first, fontWeight = FontWeight.SemiBold)
                        Text(row.first.second, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Checkbox(checked = row.first.third, onCheckedChange = row.second)
                }
            }
        }
        item { Button(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "保存中" else "保存工具开关") } }
        item { Text("打开写工具不等于自动写入；危险开关打开后，危险动作依然需要服务器给出的精确确认文本。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        message?.let { item { StatusMessage(it) } }
    }
}

@Composable
private fun DevHandsUpdate(
    update: RemoteDevUpdate?,
    busy: Boolean,
    message: String?,
    onRefresh: () -> Unit,
    onDownload: (String) -> Unit
) {
    val native = update?.native
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoftPanel {
                Text(native?.versionName ?: "暂无可下载 Native artifact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Fact("PWA cache", update?.pwaCacheVersion ?: "—")
                Fact("versionCode", native?.versionCode?.toString() ?: "—")
                Fact("applicationId", native?.applicationId ?: "—")
                Fact("稳定签名", when (native?.stableSigning) { true -> "是"; false -> "否"; null -> "—" })
                Fact("可覆盖安装", when (native?.overwriteInstallable) { true -> "是"; false -> "否"; null -> "—" })
                Fact("APK SHA-256", native?.apkSha256 ?: "—", mono = true)
                Fact("Artifact", native?.artifactName ?: "—")
                native?.knownRisk?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onRefresh, enabled = !busy) { Text("刷新") }
                    if (update?.available == true && !native?.downloadUrl.isNullOrBlank()) {
                        Button(onClick = { onDownload(native!!.downloadUrl!!) }) { Text("下载 APK") }
                    }
                }
            }
        }
        if (update?.available != true) item { StatusMessage(update?.reason ?: message ?: "暂无 artifact。") }
        else message?.let { item { StatusMessage(it) } }
    }
}

@Composable
private fun DevHandsLogs(
    logs: List<RemoteDevRun>,
    busy: Boolean,
    message: String?,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { Button(onClick = onRefresh, enabled = !busy) { Text(if (busy) "刷新中" else "刷新日志") } }
        if (logs.isEmpty()) item { StatusMessage(message ?: "还没有开发手施工记录。") }
        items(logs, key = { it.id }) { run ->
            SoftPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(run.actionName, fontWeight = FontWeight.SemiBold)
                        Text("${run.targetSystem} · ${run.targetRef ?: "—"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(run.status, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text("${run.operationType} · ${if (run.confirmationRequired) if (run.confirmationConfirmed) "已确认" else "未确认" else "不需确认"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                run.errorSummary?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun DevHandsCommand(
    title: String,
    defaultAction: String,
    defaultParams: String,
    resultText: String,
    busy: Boolean,
    message: String?,
    pending: PendingDevConfirmation?,
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
    onRun: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirm: () -> Unit
) {
    var action by remember(defaultAction) { mutableStateOf(defaultAction) }
    var params by remember(defaultParams) { mutableStateOf(defaultParams) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SoftPanel {
                Text("$title 开发手", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("动作") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = params, onValueChange = { params = it }, label = { Text("参数 JSON") }, modifier = Modifier.fillMaxWidth(), minLines = 6, textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onRun(action.trim(), params) }, enabled = !busy && action.isNotBlank()) { Text(if (busy) "执行中" else "执行") }
            }
        }
        if (pending != null) {
            item {
                SoftPanel {
                    Text(if (pending.dangerous) "需要强确认" else "需要写入确认", fontWeight = FontWeight.SemiBold)
                    Text("服务器只接受下面这句完整文本。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(pending.expected, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = confirmText, onValueChange = onConfirmTextChange, label = { Text("逐字输入确认") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onCancelConfirm) { Text("取消") }
                        Button(onClick = onConfirm, enabled = confirmText == pending.expected && !busy) { Text("提交确认") }
                    }
                }
            }
        }
        message?.let { item { StatusMessage(it) } }
        if (resultText.isNotBlank()) item {
            SoftPanel {
                Text("本次结果", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(resultText, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SoftPanel(content: @Composable Column.() -> Unit) {
    SnowLetterSurface(
        modifier = Modifier.fillMaxWidth(),
        role = SnowLetterSurfaceRole.StatusCard,
        fallbackColor = MaterialTheme.colorScheme.surfaceVariant,
        fallbackShape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp), content = content)
    }
}

@Composable
private fun DeskEntry(title: String, subtitle: String, onClick: () -> Unit) {
    SnowLetterSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        role = SnowLetterSurfaceRole.StatusCard,
        fallbackColor = MaterialTheme.colorScheme.surfaceVariant,
        fallbackShape = RoundedCornerShape(22.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusLine(label: String, value: Boolean?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(when (value) { true -> "可用"; false -> "不可用"; null -> "未检查" }, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Fact(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Text(label, modifier = Modifier.weight(.8f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, modifier = Modifier.weight(1.5f), fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatusMessage(value: String) {
    SnowLetterSurface(
        modifier = Modifier.fillMaxWidth(),
        role = SnowLetterSurfaceRole.StatusCard,
        fallbackColor = MaterialTheme.colorScheme.surfaceVariant,
        fallbackShape = RoundedCornerShape(18.dp)
    ) {
        Text(value, Modifier.padding(horizontal = 16.dp, vertical = 13.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
