package com.elementeracoast.app.feature.shell

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.RoomType

internal data class FeatureGuide(val title: String, val body: String)

internal object FeatureGuides {
    fun room(type: RoomType, local: Boolean): FeatureGuide = when(type) {
        RoomType.Main -> FeatureGuide("主聊天室", "与 API 模型日常对话的空间。选择模型后即可聊天。\n\n右上角 ···\n现实上下文：递入已启用的设备现场信息。\n短句模式：闲聊时优先简短回复。\n整理当前对话的纸条：每轮整理，为下一轮提供语境。\n登岛信：补充当前窗口的初始背景。\n\n屋主抽屉\n私人草稿：补充此刻的心情与想法。\n跨窗口选取：将指定旧消息递入本轮。\n历史消息搜索：按关键词寻找过去的对话。" + if(local) "\n\n本机已接入手动跨窗口选取；自动整理与模型自主检索尚待补全。" else "")
        RoomType.Radio -> FeatureGuide("无线电波", "GPT 官端与海岸 API 模型的共通对话区。连接 MCP 后，可在官端与海岸之间继续对话。\n\n如无需要，可忽略。" + if(local) "\n本机暂不支持官端 MCP 连接。" else "")
        RoomType.Lighthouse -> FeatureGuide("灯塔来信", "与 GPT 官端通过 MCP 交换消息的对话区。由官端读取消息并回复。\n\n如无需要，可忽略。" + if(local) "\n本机暂不支持官端 MCP 连接。" else "")
    }
}

@Composable
internal fun FeatureGuideButton(guide: FeatureGuide, firstLocalHint: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember(context) { context.applicationContext.getSharedPreferences("coast_feature_hints", Context.MODE_PRIVATE) }
    var hint by remember(firstLocalHint) { mutableStateOf(firstLocalHint && !prefs.getBoolean("local_help_seen", false)) }
    var open by remember(guide) { mutableStateOf(false) }
    fun dismissHint() { hint = false; prefs.edit().putBoolean("local_help_seen", true).apply() }
    val outline = MaterialTheme.colorScheme.primary
    Box {
        IconButton(onClick = { if (hint) dismissHint(); open = true }, modifier = Modifier.size(30.dp).drawBehind {
            if (hint) {
                val inset = 1.dp.toPx()
                drawRoundRect(outline, topLeft = Offset(inset,inset), size = Size(size.width - inset*2,size.height - inset*2),
                    cornerRadius = CornerRadius(7.dp.toPx()), style = Stroke(width=1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),3.dp.toPx()))))
            }
        }) { Text("?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall) }
        DropdownMenu(expanded = hint, onDismissRequest = { dismissHint() }) {
            Column(Modifier.widthIn(max = 230.dp).padding(horizontal = 14.dp, vertical = 7.dp)) {
                Text("如有不懂的地方，可以点击这里。", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { dismissHint() }, modifier = Modifier.align(Alignment.End)) { Text("知道了") }
            }
        }
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(guide.title) },
        text = { Text(guide.body, modifier = Modifier.verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = { open = false }) { Text("知道了") } })
}
