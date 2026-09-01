package com.elementeracoast.app.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.core.model.filterConversations
import com.elementeracoast.app.ui.theme.CoastChatTokens
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
internal fun CoastDrawer(
    state: CoastShellState,
    onClose: () -> Unit,
    onOpenRoomType: (RoomType) -> Unit,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onOpenFeature: (FeatureDestination) -> Unit,
    onCycleTheme: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    ModalDrawerSheet(
        modifier = Modifier.width(CoastChatTokens.DrawerWidth),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxSize()) {
            DrawerSearch(query, { query = it }, onClose)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = CoastChatTokens.DrawerOuterHorizontalPadding,
                    vertical = CoastChatTokens.DrawerContentVerticalPadding
                ),
                verticalArrangement = Arrangement.spacedBy(CoastChatTokens.DrawerItemGap)
            ) {
                item { CoastStatusStrip() }
                item { SectionLabel("海岸入口") }
                item { DrawerEntry(Icons.Default.Edit, "主聊天", state.activeRoomType == RoomType.Main && state.activeFeature == null) { onOpenRoomType(RoomType.Main) } }
                item { DrawerEntry(Icons.Default.Radio, "无线电波", state.activeRoomType == RoomType.Radio && state.activeFeature == null) { onOpenRoomType(RoomType.Radio) } }
                item { DrawerEntry(Icons.Default.MailOutline, "灯塔来信", state.activeRoomType == RoomType.Lighthouse && state.activeFeature == null) { onOpenRoomType(RoomType.Lighthouse) } }
                item { DrawerEntry(Icons.Default.Today, "海岸日报", state.activeFeature == FeatureDestination.Daily) { onOpenFeature(FeatureDestination.Daily) } }
                item { DrawerEntry(Icons.Default.Memory, "轨迹 / 记忆", state.activeFeature == FeatureDestination.Memory) { onOpenFeature(FeatureDestination.Memory) } }
                item { DrawerEntry(Icons.Default.Pets, "Wolf Den / 小狼窝", state.activeFeature == FeatureDestination.Wolf) { onOpenFeature(FeatureDestination.Wolf) } }
                item { DrawerEntry(Icons.Default.ListAlt, "小蛇行动日志", state.activeFeature == FeatureDestination.ActionLog) { onOpenFeature(FeatureDestination.ActionLog) } }
                item { DrawerEntry(Icons.Default.Palette, "外观", state.activeFeature == FeatureDestination.Appearance, state.theme.label) { onOpenFeature(FeatureDestination.Appearance) } }
                item {
                    DrawerEntry(Icons.Default.Tune, "快速切换主题", false, "当前：${state.theme.label}", onCycleTheme)
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                item { SectionLabel("聊天窗口") }
                item {
                    ConversationList(
                        conversations = state.conversations,
                        query = query,
                        activeConversationId = state.activeConversationId,
                        featureActive = state.activeFeature != null,
                        onSelectConversation = onSelectConversation,
                        onRenameConversation = onRenameConversation,
                        onDeleteConversation = onDeleteConversation
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSearch(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            start = CoastChatTokens.DrawerOuterHorizontalPadding,
            end = CoastChatTokens.DrawerOuterHorizontalPadding,
            top = CoastChatTokens.DrawerHeaderTopPadding,
            bottom = CoastChatTokens.DrawerHeaderBottomPadding
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Default.Close, "关闭侧边栏") }
        Row(
            modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(CoastChatTokens.DrawerSearchRadius)).padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isBlank()) Text("搜索聊天", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
}

@Composable
fun ConversationList(
    conversations: List<ConversationSummary>,
    query: String,
    activeConversationId: String,
    featureActive: Boolean,
    onSelectConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    val filtered = filterConversations(conversations, query)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (filtered.isEmpty()) {
            Text("没有匹配的窗口", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            filtered.forEach { conversation ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(
                        if (conversation.id == activeConversationId && !featureActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(CoastChatTokens.DrawerEntryRadius)
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        conversation.title,
                        modifier = Modifier.weight(1f).clickable { onSelectConversation(conversation.id) }.padding(start = 13.dp, top = 11.dp, bottom = 11.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (conversation.id == activeConversationId && !featureActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                    ConversationActionsButton(conversation, onRenameConversation, onDeleteConversation)
                }
            }
        }
    }
}

@Composable
private fun DrawerEntry(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(
            if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            RoundedCornerShape(CoastChatTokens.DrawerEntryRadius)
        ).clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = if (subtitle == null) 10.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun CoastStatusStrip() {
    val today = remember { LocalDate.now() }
    val orbit = remember(today) { (ChronoUnit.DAYS.between(LocalDate.of(2025, 8, 13), today) + 1).coerceAtLeast(1) }
    fun daysUntil(month: Int, day: Int): Long {
        var target = LocalDate.of(today.year, month, day)
        if (target.isBefore(today)) target = target.plusYears(1)
        return ChronoUnit.DAYS.between(today, target)
    }
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusCard("同轨第", orbit.toString(), "日", Modifier.weight(1f))
        StatusCard("距 8.12", daysUntil(8, 12).toString(), "天", Modifier.weight(1f))
        StatusCard("距 8.13", daysUntil(8, 13).toString(), "天", Modifier.weight(1f))
    }
}

@Composable
private fun StatusCard(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier = modifier.heightIn(min = 58.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.Bold)
        Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
