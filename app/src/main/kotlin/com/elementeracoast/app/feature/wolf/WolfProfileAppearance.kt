package com.elementeracoast.app.feature.wolf

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.ui.theme.CoastThemePreset
import com.elementeracoast.app.ui.theme.CoastFontMode
import com.elementeracoast.app.ui.theme.CoastPaperMode
import com.elementeracoast.app.ui.theme.CoastReadingWeight
import java.io.File

@Composable
internal fun ProfileScreen(
    state: WolfState,
    store: WolfStore,
    onAppearance: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    var nickname by remember(state.profile.nickname) { mutableStateOf(state.profile.nickname) }
    var signature by remember(state.profile.signature) { mutableStateOf(state.profile.signature) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Text("个人资料", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "这些显示资料不会自动进入 Myri 的记忆或系统提示词。真正长期参与理解的内容仍属于自定义指令或记忆库。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        item { WolfTextField("昵称", nickname, { nickname = it }) }
        item { WolfTextField("聊天署名 / 导出显示名", signature, { signature = it }) }
        item { WolfRow("用户气泡颜色", "复用外观里的同一项设置", onAppearance) }
        item {
            PrimaryLocalButton("保存个人资料") {
                store.saveProfile(nickname, signature)
                onSnackbar("个人资料已保存在本机")
            }
        }
    }
}

@Composable
internal fun AppearanceScreen(state: WolfState, store: WolfStore, onSnackbar: (String) -> Unit) {
    val context = LocalContext.current
    val localFontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { importLocalFont(context, uri) }
            .onSuccess { imported ->
                store.setLocalFont(imported.displayName, imported.path)
                onSnackbar("本机字体已导入：${imported.displayName}")
            }
            .onFailure { error -> onSnackbar(error.message ?: "本机字体导入失败") }
    }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text("外观", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item {
            SettingGroup("主题衣柜") {
                CoastThemePreset.entries.forEach { preset ->
                    ChoiceRow("${preset.label}｜${preset.tag}", state.appearance.theme == preset) {
                        store.setTheme(preset)
                        onSnackbar("主题已切换为${preset.label}")
                    }
                }
            }
        }
        item {
            SettingGroup("用户气泡颜色") {
                listOf("" to "默认 · 跟随主题", "#eaf0f7" to "冷蓝灰", "#f5e8ee" to "浅粉灰", "#f1ead8" to "淡金灰").forEach { (value, label) ->
                    ChoiceRow(label, state.appearance.userBubbleHex == value) { store.setUserBubble(value) }
                }
            }
        }
        item {
            SettingGroup("重点色") {
                listOf("" to "默认 · 跟随主题", "#ff6a21" to "橙色", "#f28b2e" to "金色", "#3b82f6" to "蓝色", "#ec4899" to "粉色").forEach { (value, label) ->
                    ChoiceRow(label, state.appearance.accentHex == value) { store.setAccent(value) }
                }
            }
        }
        item {
            SettingGroup("文字气质") {
                CoastFontMode.entries.forEach { mode ->
                    if (mode == CoastFontMode.CustomLocal) {
                        val imported = state.appearance.localFontPath.isNotBlank()
                        ChoiceRow(
                            label = mode.label,
                            selected = state.appearance.fontMode == mode,
                            subtitle = if (imported) state.appearance.localFontName.ifBlank { "已导入本机字体" } else "点此选择已购买或授权的 .ttf / .otf"
                        ) {
                            if (imported) store.setFontMode(mode) else localFontLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
                        }
                        if (imported) {
                            ChoiceRow("更换本机字体", false, "重新选择 .ttf / .otf，只保存在此设备") {
                                localFontLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
                            }
                            ChoiceRow("移除本机字体", false, "删除 APP 私有目录里的字体副本") {
                                removeLocalFont(context, state.appearance.localFontPath)
                                store.clearLocalFont()
                                onSnackbar("本机字体已移除，已回到朱雀仿宋")
                            }
                        }
                    } else {
                        ChoiceRow(mode.label, state.appearance.fontMode == mode) {
                            store.setFontMode(mode)
                        }
                    }
                }
            }
        }
        item {
            SettingGroup("正文字重") {
                CoastReadingWeight.entries.forEach { weight ->
                    ChoiceRow(
                        label = weight.label,
                        selected = state.appearance.readingWeight == weight,
                        subtitle = when (weight) {
                            CoastReadingWeight.Normal -> "默认。最清瘦，也最不容易假加粗。"
                            CoastReadingWeight.Medium -> "稍稍加重。适合过细的字体。"
                            CoastReadingWeight.Bold -> "明显加粗。部分字体可能会被系统假加粗。"
                        }
                    ) { store.setReadingWeight(weight) }
                }
            }
        }
        item {
            SettingGroup("纸页形态") {
                CoastPaperMode.entries.forEach { mode ->
                    ChoiceRow(mode.label, state.appearance.paperMode == mode) {
                        store.setPaperMode(mode)
                    }
                }
            }
        }
        item { Text("以上都只保存在本机，不接后端。本机字体只保存于此设备；覆盖安装会保留，卸载或清除数据后需要重新导入。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
internal fun WolfTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(5.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(13.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            minLines = minLines,
            maxLines = if (minLines > 1) 8 else 1
        )
    }
}

@Composable
internal fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp)).padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
internal fun ChoiceRow(label: String, selected: Boolean, subtitle: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)) {
        Text(if (selected) "●" else "○", color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(horizontal = 5.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun PrimaryLocalButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(14.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.SemiBold
    )
}

private data class ImportedLocalFont(val displayName: String, val path: String)

private fun importLocalFont(context: Context, uri: Uri): ImportedLocalFont {
    val displayName = readDisplayName(context, uri).ifBlank { "本机字体" }.take(120)
    val extension = displayName.substringAfterLast('.', "").lowercase()
    val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
    val targetExtension = when {
        extension == "ttf" || mime.contains("ttf") || mime.contains("truetype") -> "ttf"
        extension == "otf" || mime.contains("otf") || mime.contains("opentype") -> "otf"
        else -> throw IllegalArgumentException("请选择 .ttf 或 .otf 字体文件")
    }
    val directory = File(context.filesDir, "local-fonts").apply { mkdirs() }
    directory.listFiles()?.forEach { it.delete() }
    val temp = File(directory, "reading-font.tmp")
    val target = File(directory, "reading-font.$targetExtension")
    context.contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
        ?: throw IllegalArgumentException("无法读取所选字体文件")
    check(temp.length() > 0L) { "字体文件为空" }
    runCatching { Typeface.createFromFile(temp) }
        .getOrElse { throw IllegalArgumentException("字体文件无法被 Android 读取") }
    temp.copyTo(target, overwrite = true)
    temp.delete()
    return ImportedLocalFont(displayName.removeSuffix(".ttf").removeSuffix(".TTF").removeSuffix(".otf").removeSuffix(".OTF"), target.absolutePath)
}

private fun readDisplayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index).orEmpty()
    }
    return uri.lastPathSegment.orEmpty()
}

private fun removeLocalFont(context: Context, path: String) {
    runCatching { File(path).delete() }
    runCatching { File(context.filesDir, "local-fonts").listFiles()?.forEach { it.delete() } }
}
