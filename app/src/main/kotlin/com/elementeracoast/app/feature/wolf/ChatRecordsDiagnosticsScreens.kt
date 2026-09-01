package com.elementeracoast.app.feature.wolf

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.core.local.LocalActionLogStore
import com.elementeracoast.app.core.local.LocalChatStore
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.RoomType

@Composable
internal fun ChatRecordsScreen(
    chatStore: LocalChatStore,
    preferencesStore: LocalPreferencesStore,
    actionLogStore: LocalActionLogStore,
    roomType: RoomType,
    conversationId: String,
    onSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs by preferencesStore.state.collectAsState()
    var pendingJson by remember { mutableStateOf("") }
    var pendingHtml by remember { mutableStateOf("") }

    fun log(actionKey: String, label: String, output: String) {
        actionLogStore.record(
            actionKey = actionKey,
            label = label,
            roomType = roomType,
            conversationId = conversationId,
            inputSummary = "local chat records action",
            outputSummary = output
        )
    }

    val jsonExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingJson) }
                ?: error("cannot open destination")
        }.onSuccess {
            log("chat.export_json", "导出了聊天 JSON", "JSON saved locally")
            onSnackbar("JSON 已保存到本机")
        }.onFailure { onSnackbar("JSON 导出失败：${it.message}") }
    }

    val htmlExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingHtml) }
                ?: error("cannot open destination")
        }.onSuccess {
            log("chat.export_html", "导出了聊天 HTML", "HTML saved locally")
            onSnackbar("HTML 已保存到本机")
        }.onFailure { onSnackbar("HTML 导出失败：${it.message}") }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("cannot read selected file")
            chatStore.importJson(raw)
        }.onSuccess { count ->
            log("chat.import_json", "导入了聊天 JSON", "$count messages restored locally")
            onSnackbar("已导入 $count 条本地消息")
        }.onFailure { onSnackbar("导入失败：${it.message ?: "文件格式无效"}") }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SurfaceHeading("聊天记录", "当前本地窗口 · 导入导出") }
        item {
            SettingsRow("导出 JSON", "保留消息与显示资料 metadata") {
                pendingJson = chatStore.exportJson(prefs.profile.signature, prefs.currentModel)
                jsonExporter.launch("Elementera-Coast-native-chat.json")
            }
        }
        item {
            SettingsRow("导出 HTML", "生成可离线打开的本地页面") {
                pendingHtml = chatStore.exportHtml(prefs.profile.signature)
                htmlExporter.launch("Elementera-Coast-native-chat.html")
            }
        }
        item {
            SettingsRow("导入 JSON", "恢复到当前本地窗口") {
                importer.launch(arrayOf("application/json", "text/json", "text/plain"))
            }
        }
        item { InfoNote("所有导入导出只操作本机文档与当前 local conversation；不会上传海岸后端。") }
    }
}

@Composable
internal fun DiagnosticsScreen(
    preferencesStore: LocalPreferencesStore,
    chatStore: LocalChatStore,
    roomType: RoomType,
    conversationId: String
) {
    val context = LocalContext.current
    val prefs by preferencesStore.state.collectAsState()
    val internetGranted = context.packageManager.checkPermission(Manifest.permission.INTERNET, context.packageName) == PackageManager.PERMISSION_GRANTED
    val rows = listOf(
        "APK versionName" to BuildConfig.VERSION_NAME,
        "APK versionCode" to BuildConfig.VERSION_CODE.toString(),
        "当前主题" to prefs.theme.label,
        "当前 roomType" to roomType.wireValue,
        "当前 conversation id" to conversationId,
        "当前模型" to shortModelName(prefs.currentModel),
        "本地 conversation 数量" to chatStore.conversationCount().toString(),
        "本地消息数" to chatStore.messageCount().toString(),
        "INTERNET permission" to if (internetGranted) "是" else "否",
        "后端接线状态" to "未接线"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SurfaceHeading("关于与诊断", "Native app-59 local parity") }
        rows.forEach { (label, value) ->
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(14.dp)
                ) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(value, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
