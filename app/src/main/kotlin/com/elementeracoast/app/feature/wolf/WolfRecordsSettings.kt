package com.elementeracoast.app.feature.wolf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun ChatRecordsScreen(
    profile: WolfProfile,
    messages: List<ChatMessage>,
    onImportMessages: (List<ChatMessage>) -> Unit,
    onActionLogged: (String, String, String) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val stamp = remember { DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()) }
    var pendingText = remember { "" }

    fun write(uri: Uri?, text: String, ok: String) {
        if (uri == null) return
        runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) } }
            .onSuccess { onActionLogged("chat.export", ok, "当前窗口 ${messages.size} 条消息"); onSnackbar(ok) }
            .onFailure { onSnackbar("导出失败：${it.message ?: "无法写入文件"}") }
    }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        write(uri, pendingText, "已导出 JSON")
    }
    val htmlExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        write(uri, pendingText, "已导出 HTML")
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("文件为空")
            var id = 9_000_000L
            ChatArchive.importJson(raw) { id++ }.getOrThrow()
        }.onSuccess { archive ->
            onImportMessages(archive.messages)
            onActionLogged("chat.import", "导入聊天记录", "恢复 ${archive.messages.size} 条消息")
            onSnackbar("聊天记录已导入当前窗口")
        }.onFailure { onSnackbar("导入失败：${it.message ?: "无法解析 JSON"}") }
    }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("聊天记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item { WolfRow("导出 JSON", "保留消息与显示资料 metadata") { pendingText = ChatArchive.exportJson(profile, messages); jsonExport.launch("elementera-chat-$stamp.json") } }
        item { WolfRow("导出 HTML", "离线打开查看") { pendingText = ChatArchive.exportHtml(profile, messages); htmlExport.launch("elementera-chat-$stamp.html") } }
        item { WolfRow("导入 JSON", "恢复到当前本地窗口") { importer.launch(arrayOf("application/json", "text/plain")) } }
        item { Text("导入只替换当前本地窗口的消息，不接历史后端。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun ModelBoxScreen(models: List<String>, current: String, onSelect: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("模型箱", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("当前：${current.substringAfterLast('/')}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("本地壳，真实模型列表等后端接线后同步。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        items(models) { model -> WolfRow(model.substringAfterLast('/'), if (model == current) "当前模型" else "本地 mock 模型") { onSelect(model) } }
    }
}

@Composable
internal fun BasicSettingsScreen(settings: BasicSettings, store: WolfStore) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("基本设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item { SettingGroup("上下文舒服区间") {
            ChoiceSet("最近聊天轮数", settings.recentTurns, listOf(2,4,8,12)) { store.updateBasic { b -> b.copy(recentTurns = it) } }
            ChoiceSet("舒服区间上沿", settings.contextBudget, listOf(2000,6000,12000)) { store.updateBasic { b -> b.copy(contextBudget = it) } }
        } }
        item { SettingGroup("输出偏好") {
            Text("回答长度", fontWeight = FontWeight.Medium)
            listOf("auto" to "自然", "short" to "偏短", "long" to "长信").forEach { (v,l) -> ChoiceRow(l, settings.outputLength == v) { store.updateBasic { it.copy(outputLength = v) } } }
            NumberStepper("最大输出 token", settings.maxOutputTokens, 64, 65536, 512) { store.updateBasic { b -> b.copy(maxOutputTokens = it) } }
            Text("表达倾向", fontWeight = FontWeight.Medium)
            listOf("stable" to "稳定", "balanced" to "自然", "expansive" to "发散").forEach { (v,l) -> ChoiceRow(l, settings.creativity == v) { store.updateBasic { it.copy(creativity = v) } } }
        } }
        item { SettingGroup("生成方式") { BooleanRow("流式输出", settings.streamingEnabled) { store.updateBasic { b -> b.copy(streamingEnabled = it) } } } }
        item { SettingGroup("思维壤与记忆") {
            NumberStepper("思维壤最多字数", settings.soilBudget, 300, 4000, 100) { store.updateBasic { b -> b.copy(soilBudget = it) } }
            NumberStepper("种子冷却轮数", settings.seedCooldownTurns, 0, 8, 1) { store.updateBasic { b -> b.copy(seedCooldownTurns = it) } }
            NumberStepper("本轮记忆召回上限", settings.memoryLimit, 0, 12, 1) { store.updateBasic { b -> b.copy(memoryLimit = it) } }
        } }
        item { SettingGroup("海岸词典") {
            BooleanRow("世界书 / 海岸词典", settings.worldbookEnabled) { store.updateBasic { b -> b.copy(worldbookEnabled = it) } }
            NumberStepper("每轮最多词条", settings.worldbookLimit, 0, 6, 1) { store.updateBasic { b -> b.copy(worldbookLimit = it) } }
        } }
        item { Text("11 项都保存在本地。fake generation 会读取这些参数做本地展示；真实请求尚未接线。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ChoiceSet(label: String, current: Int, values: List<Int>, onPick: (Int) -> Unit) {
    Text(label, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value -> Text(value.toString(), modifier = Modifier.background(if (value == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).clickable { onPick(value) }.padding(horizontal = 10.dp, vertical = 7.dp)) }
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun BooleanRow(label: String, value: Boolean, onPick: (Boolean) -> Unit) {
    ChoiceRow("$label · ${if (value) "开启" else "关闭"}", value) { onPick(!value) }
}

@Composable
private fun NumberStepper(label: String, value: Int, min: Int, max: Int, step: Int, onPick: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Column(modifier = Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.Medium); Text(value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("－", modifier = Modifier.clickable { onPick((value-step).coerceAtLeast(min)) }.padding(10.dp))
        Text("＋", modifier = Modifier.clickable { onPick((value+step).coerceAtMost(max)) }.padding(10.dp))
    }
}

@Composable
internal fun DiagnosticsScreen(shell: CoastShellState, wolf: WolfState) {
    val rows = listOf(
        "APK versionName" to BuildConfig.VERSION_NAME,
        "APK versionCode" to BuildConfig.VERSION_CODE.toString(),
        "当前主题" to wolf.appearance.theme.label,
        "当前 roomType" to shell.activeRoomType.wireValue,
        "当前 conversation id" to shell.activeConversationId,
        "当前模型" to shell.currentModel,
        "本地 conversation 数量" to shell.conversations.size.toString(),
        "当前本地消息数" to shell.messages.size.toString(),
        "INTERNET permission" to "否",
        "后端接线状态" to "未接线"
    )
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("关于与诊断", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(rows) { (label,value) ->
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(13.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value) }
        }
    }
}
