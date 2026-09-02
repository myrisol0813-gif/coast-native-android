package com.elementeracoast.app.feature.wolf

import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var pendingText by remember { mutableStateOf("") }

    fun write(uri: Uri?, text: String, ok: String) {
        if (uri == null) return
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                ?: error("无法打开目标文件")
        }
            .onSuccess {
                onActionLogged("chat.export", ok, "当前窗口 ${messages.size} 条消息")
                onSnackbar(ok)
            }
            .onFailure { onSnackbar("导出失败：${it.message ?: "无法写入文件"}") }
    }

    val jsonExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        write(uri, pendingText, "已导出 JSON")
    }
    val htmlExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        write(uri, pendingText, "已导出 HTML")
    }

    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("聊天记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item {
            WolfRow("导出 JSON", "保留当前显示消息与资料 metadata") {
                pendingText = ChatArchive.exportJson(profile, messages)
                jsonExport.launch("elementera-chat-$stamp.json")
            }
        }
        item {
            WolfRow("导出 HTML", "离线打开查看") {
                pendingText = ChatArchive.exportHtml(profile, messages)
                htmlExport.launch("elementera-chat-$stamp.html")
            }
        }
        item {
            WolfRow("导入 JSON", "真实 history 写回本轮未接；暂不导入") {
                // Keep the callback in the screen contract for the already-reviewed shell shape,
                // but never call it until a real remote-history import policy exists.
                @Suppress("UNUSED_EXPRESSION")
                onImportMessages
                onSnackbar("聊天记录导入暂未接后端；没有修改当前真实会话。")
            }
        }
        item {
            Text(
                "当前聊天记录的真实来源是海岸 D1。导出是本机副本；导入不会在没有后端写回规则时伪装成功。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun BasicSettingsScreen(settings: BasicSettings, store: WolfStore) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("基本设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item {
            SettingGroup("上下文舒服区间") {
                ChoiceSet("最近聊天轮数", settings.recentTurns, listOf(2, 4, 8, 12)) { value ->
                    store.updateBasic { it.copy(recentTurns = value) }
                }
                ChoiceSet("舒服区间上沿", settings.contextBudget, listOf(2000, 6000, 12000)) { value ->
                    store.updateBasic { it.copy(contextBudget = value) }
                }
            }
        }
        item {
            SettingGroup("输出偏好") {
                Text("回答长度", fontWeight = FontWeight.Medium)
                listOf("auto" to "自然", "short" to "偏短", "long" to "长信").forEach { (value, label) ->
                    ChoiceRow(label, settings.outputLength == value) { store.updateBasic { it.copy(outputLength = value) } }
                }
                NumberStepper("最大输出 token", settings.maxOutputTokens, 64, 65536, 512) { value ->
                    store.updateBasic { it.copy(maxOutputTokens = value) }
                }
                Text("表达倾向", fontWeight = FontWeight.Medium)
                listOf("stable" to "稳定", "balanced" to "自然", "expansive" to "发散").forEach { (value, label) ->
                    ChoiceRow(label, settings.creativity == value) { store.updateBasic { it.copy(creativity = value) } }
                }
            }
        }
        item {
            SettingGroup("生成方式") {
                BooleanRow("流式输出", settings.streamingEnabled) { value -> store.updateBasic { it.copy(streamingEnabled = value) } }
            }
        }
        item {
            SettingGroup("思维壤与记忆") {
                NumberStepper("思维壤最多字数", settings.soilBudget, 300, 4000, 100) { value -> store.updateBasic { it.copy(soilBudget = value) } }
                NumberStepper("种子冷却轮数", settings.seedCooldownTurns, 0, 8, 1) { value -> store.updateBasic { it.copy(seedCooldownTurns = value) } }
                NumberStepper("本轮记忆召回上限", settings.memoryLimit, 0, 12, 1) { value -> store.updateBasic { it.copy(memoryLimit = value) } }
            }
        }
        item {
            SettingGroup("海岸词典") {
                BooleanRow("世界书 / 海岸词典", settings.worldbookEnabled) { value -> store.updateBasic { it.copy(worldbookEnabled = value) } }
                NumberStepper("每轮最多词条", settings.worldbookLimit, 0, 6, 1) { value -> store.updateBasic { it.copy(worldbookLimit = value) } }
            }
        }
        item {
            Text(
                "这些设置仍是 Native 本地偏好。本轮真实聊天由海岸后端组装上下文；尚未明确映射到后端的字段不会偷偷塞进请求。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ChoiceSet(label: String, current: Int, values: List<Int>, onPick: (Int) -> Unit) {
    Text(label, fontWeight = FontWeight.Medium)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEach { value ->
            Text(
                value.toString(),
                modifier = Modifier
                    .background(
                        if (value == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onPick(value) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun BooleanRow(label: String, value: Boolean, onPick: (Boolean) -> Unit) {
    ChoiceRow("$label · ${if (value) "开启" else "关闭"}", value) { onPick(!value) }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    onPick: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("－", modifier = Modifier.clickable { onPick((value - step).coerceAtLeast(min)) }.padding(10.dp))
        Text("＋", modifier = Modifier.clickable { onPick((value + step).coerceAtMost(max)) }.padding(10.dp))
    }
}

@Composable
internal fun DiagnosticsScreen(shell: CoastShellState, wolf: WolfState) {
    val context = LocalContext.current
    val packageInfo = remember(context.packageName) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    @Suppress("DEPRECATION")
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
    val rows = listOf(
        "APK versionName" to (packageInfo.versionName ?: "unknown"),
        "APK versionCode" to versionCode.toString(),
        "当前主题" to wolf.appearance.theme.label,
        "当前 roomType" to shell.activeRoomType.wireValue,
        "当前 conversation id" to shell.activeConversationId,
        "当前模型" to shell.currentModel.ifBlank { "尚未载入" },
        "共享 conversation 数量" to shell.conversations.size.toString(),
        "当前消息数" to shell.messages.size.toString(),
        "INTERNET permission" to "是",
        "后端接线状态" to if (shell.backendOffline) "缓存可读 · 后端暂不可达" else "海岸聊天主链已接线",
        "聊天 source of truth" to "Coast D1 / shared history v4"
    )
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("关于与诊断", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(rows) { (label, value) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(13.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value)
            }
        }
    }
}
