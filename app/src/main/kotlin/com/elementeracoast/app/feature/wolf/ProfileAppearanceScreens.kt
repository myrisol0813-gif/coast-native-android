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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.local.LocalPreferencesStore
import com.elementeracoast.app.core.model.CoastThemeMode

@Composable
internal fun ProfileScreen(
    store: LocalPreferencesStore,
    onAppearance: () -> Unit,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    var nickname by remember(state.profile.nickname) { mutableStateOf(state.profile.nickname) }
    var signature by remember(state.profile.signature) { mutableStateOf(state.profile.signature) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SurfaceHeading("个人资料", "小寒在海岸里的显示资料") }
        item {
            InfoNote("这些资料不会自动进入 Myri 的记忆或系统提示词。真正影响长期上下文的内容，请写进记忆或自定义指令。")
        }
        item {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.take(80) },
                label = { Text("昵称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            OutlinedTextField(
                value = signature,
                onValueChange = { signature = it.take(80) },
                label = { Text("聊天署名 / 导出显示名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            SettingsRow("用户气泡颜色", "复用外观里的同一项设置", onAppearance)
        }
        item { InfoNote("头像统一使用碳硅圈 / Daily profile 的头像源。本页不另建第二套头像系统。") }
        item {
            Button(
                onClick = {
                    store.updateProfile(nickname, signature)
                    onSnackbar("个人资料已保存在本机")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("保存个人资料") }
        }
    }
}

@Composable
internal fun AppearanceScreen(
    store: LocalPreferencesStore,
    onSnackbar: (String) -> Unit
) {
    val state by store.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { SurfaceHeading("外观", "海岸本机显示 · 改动立即生效") }
        item {
            ChoiceBlock(
                title = "主题",
                choices = CoastThemeMode.entries.map { it.name to it.label },
                current = state.theme.name,
                onSelect = { value ->
                    store.setTheme(CoastThemeMode.valueOf(value))
                    onSnackbar("主题已保存在本机")
                }
            )
        }
        item {
            ChoiceBlock(
                title = "用户气泡颜色",
                choices = listOf(
                    "default" to "跟随主题",
                    "blue-gray" to "冷蓝灰",
                    "pink-gray" to "浅粉灰",
                    "gold-gray" to "淡金灰"
                ),
                current = state.profile.userBubble,
                onSelect = store::setUserBubble
            )
        }
        item {
            ChoiceBlock(
                title = "重点色",
                choices = listOf(
                    "orange" to "橙色",
                    "gold" to "金色",
                    "blue" to "蓝色",
                    "pink" to "粉色"
                ),
                current = state.profile.accent,
                onSelect = store::setAccent
            )
        }
    }
}

@Composable
internal fun SurfaceHeading(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun InfoNote(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(14.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
internal fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text("›", color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ChoiceBlock(
    title: String,
    choices: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        choices.forEach { (value, label) ->
            Row(
                modifier = Modifier.fillMaxWidth().background(
                    if (value == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp)
                ).clickable { onSelect(value) }.padding(14.dp)
            ) {
                Text(label, modifier = Modifier.weight(1f))
                if (value == current) Text("✓", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
