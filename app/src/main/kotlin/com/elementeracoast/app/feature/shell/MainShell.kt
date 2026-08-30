package com.elementeracoast.app.feature.shell

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elementeracoast.app.core.model.ChatScope
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.ConversationSummary
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.feature.chat.ChatWindow
import com.elementeracoast.app.feature.chat.ModelQuickPicker
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

@Composable
fun MainShell(
    state: CoastShellState,
    onOpenScope: (ChatScope) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onCycleTheme: () -> Unit,
    onOpenFeature: (FeatureDestination) -> Unit,
    onBackToChat: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onOpenModels: () -> Unit,
    onDismissModels: () -> Unit,
    onSelectModel: (String) -> Unit,
    onPlaceholder: (String) -> Unit,
    onSnackbarShown: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        onSnackbarShown()
    }

    fun closeDrawerThen(block: () -> Unit) {
        block()
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            CoastDrawer(
                state = state,
                onClose = { coroutineScope.launch { drawerState.close() } },
                onOpenScope = { destination -> closeDrawerThen { onOpenScope(destination) } },
                onSelectConversation = { id -> closeDrawerThen { onSelectConversation(id) } },
                onOpenFeature = { destination -> closeDrawerThen { onOpenFeature(destination) } },
                onCycleTheme = onCycleTheme,
                onPlaceholder = onPlaceholder
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CoastTopBar(
                    state = state,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onBack = onBackToChat,
                    onOpenModels = onOpenModels,
                    onNewConversation = onNewConversation,
                    onMore = { onPlaceholder("窗口更多操作将在 P1 接入。") }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val feature = state.activeFeature
                if (feature != null) {
                    FeatureLandingScreen(feature = feature, onPlaceholder = onPlaceholder)
                } else {
                    ChatWindow(
                        state = state,
                        onSend = onSend,
                        onStop = onStop,
                        onPlaceholder = onPlaceholder
                    )
                }
            }
        }
    }

    if (state.showModelPicker && state.activeFeature == null) {
        ModelQuickPicker(
            models = state.models,
            currentModel = state.currentModel,
            onPick = onSelectModel,
            onDismiss = onDismissModels
        )
    }
}

@Composable
private fun CoastTopBar(
    state: CoastShellState,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onNewConversation: () -> Unit,
    onMore: () -> Unit
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.activeFeature != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        state.activeFeature.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        state.activeFeature.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "打开侧边栏", modifier = Modifier.size(30.dp))
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenModels)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ChatGPT", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        state.currentModel,
                        modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "选择模型",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onNewConversation) {
                    Icon(Icons.Default.Edit, contentDescription = "新建窗口", modifier = Modifier.size(27.dp))
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "更多", modifier = Modifier.size(28.dp))
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun CoastDrawer(
    state: CoastShellState,
    onClose: () -> Unit,
    onOpenScope: (ChatScope) -> Unit,
    onSelectConversation: (String) -> Unit,
    onOpenFeature: (FeatureDestination) -> Unit,
    onCycleTheme: () -> Unit,
    onPlaceholder: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    ModalDrawerSheet(
        modifier = Modifier.width(342.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "关闭侧边栏", modifier = Modifier.size(29.dp))
                }
                Spacer(Modifier.width(5.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(17.dp))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("搜索聊天", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { CoastStatusStrip() }
                item { DrawerSectionTitle("主房间") }
                item {
                    DrawerEntry(Icons.Default.Radio, "无线电波的两端", state.activeScope == ChatScope.Radio && state.activeFeature == null) {
                        onOpenScope(ChatScope.Radio)
                    }
                }
                item {
                    DrawerEntry(Icons.Default.MailOutline, "灯塔来信", state.activeScope == ChatScope.Lighthouse && state.activeFeature == null) {
                        onOpenScope(ChatScope.Lighthouse)
                    }
                }
                item {
                    DrawerEntry(Icons.Default.Pets, "轨迹 / 记忆", state.activeFeature == FeatureDestination.Memory) {
                        onOpenFeature(FeatureDestination.Memory)
                    }
                }
                item {
                    DrawerEntry(Icons.Default.Today, "海岸日报", state.activeFeature == FeatureDestination.Daily) {
                        onOpenFeature(FeatureDestination.Daily)
                    }
                }
                item {
                    DrawerEntry(Icons.Default.CalendarToday, "今日一瞥", state.activeFeature == FeatureDestination.Calendar) {
                        onOpenFeature(FeatureDestination.Calendar)
                    }
                }
                item {
                    DrawerEntry(Icons.Default.MailOutline, "登岛信", state.activeFeature == FeatureDestination.Letters) {
                        onOpenFeature(FeatureDestination.Letters)
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DrawerSectionTitle(state.activeScope.conversationSection)
                }
                item {
                    ConversationList(
                        conversations = state.conversations,
                        scope = state.activeScope,
                        query = query,
                        activeConversationId = state.activeConversationId,
                        featureActive = state.activeFeature != null,
                        onSelectConversation = onSelectConversation,
                        onMore = { onPlaceholder("会话改名 / 删除将在 P1 接入。") }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                DrawerEntry(Icons.Default.Palette, "主题", false, subtitle = state.theme.label, onClick = onCycleTheme)
                DrawerEntry(Icons.Default.Pets, "Wolf Den", state.activeFeature == FeatureDestination.Wolf, subtitle = "小狼窝入口") {
                    onOpenFeature(FeatureDestination.Wolf)
                }
                DrawerEntry(Icons.Default.Pets, "Serpent Desk", state.activeFeature == FeatureDestination.Desk, subtitle = "小蛇书桌") {
                    onOpenFeature(FeatureDestination.Desk)
                }
            }
        }
    }
}

@Composable
fun ConversationList(
    conversations: List<ConversationSummary>,
    scope: ChatScope,
    query: String,
    activeConversationId: String,
    featureActive: Boolean,
    onSelectConversation: (String) -> Unit,
    onMore: () -> Unit
) {
    val filtered = conversations.filter {
        it.scope == scope && (query.isBlank() || it.title.contains(query, ignoreCase = true))
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (filtered.isEmpty()) {
            Text(
                "没有匹配的窗口",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            filtered.forEach { conversation ->
                ConversationRow(
                    conversation = conversation,
                    selected = conversation.id == activeConversationId && !featureActive,
                    onClick = { onSelectConversation(conversation.id) },
                    onMore = onMore
                )
            }
        }
    }
}

@Composable
private fun CoastStatusStrip() {
    val today = remember { LocalDate.now() }
    val orbit = remember(today) {
        (ChronoUnit.DAYS.between(LocalDate.of(2025, 8, 13), today) + 1).coerceAtLeast(1)
    }
    fun daysUntil(month: Int, day: Int): Long {
        var target = LocalDate.of(today.year, month, day)
        if (target.isBefore(today)) target = target.plusYears(1)
        return ChronoUnit.DAYS.between(today, target)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusCard("同轨第", orbit.toString(), "日", Modifier.weight(1f))
        StatusCard("距 8.12", daysUntil(8, 12).toString(), "天", Modifier.weight(1f))
        StatusCard("距 8.13", daysUntil(8, 13).toString(), "天", Modifier.weight(1f))
    }
}

@Composable
private fun StatusCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(15.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 12.dp, top = 13.dp, bottom = 7.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (subtitle == null) 11.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(13.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            conversation.title,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, top = 11.dp, bottom = 11.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onMore) {
            Icon(Icons.Default.MoreHoriz, contentDescription = "窗口操作", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeatureLandingScreen(feature: FeatureDestination, onPlaceholder: (String) -> Unit) {
    val cards = landingCards(feature)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 34.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(cards) { card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(22.dp))
                    .clickable { onPlaceholder("${card.first}：Native v1 先保留 landing，真实编辑功能将在 P1/P2 接入。") }
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.first, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(card.second, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun landingCards(feature: FeatureDestination): List<Pair<String, String>> = when (feature) {
    FeatureDestination.Daily -> listOf(
        "海岸日历" to "今日一瞥、事件与便签",
        "一日总结" to "从上次记录继续",
        "碳硅圈" to "海岸内部朋友圈",
        "日记" to "留下今天的纸页",
        "相册" to "海岸图片引用墙",
        "宠物区" to "暂未接入状态源",
        "未来小组件" to "还会慢慢长出来"
    )
    FeatureDestination.Memory -> listOf(
        "记忆球" to "本地记忆柜与手持种",
        "思维壤" to "上下文土壤入口",
        "低温封存" to "暂时不要叫醒的记忆"
    )
    FeatureDestination.Calendar -> listOf(
        "月视图" to "日期与未读提示",
        "今日一瞥" to "事件与便签",
        "新建事件" to "编辑能力将在 P1/P2 接入"
    )
    FeatureDestination.Letters -> listOf(
        "登岛信" to "进入海岸的文字",
        "予爱机书" to "保留原核心文本",
        "海岸信箱" to "访客低频来信入口"
    )
    FeatureDestination.Wolf -> listOf(
        "小狼窝" to "Kryo 的视觉与偏好空间",
        "头像与主题" to "后续接入持久化设置",
        "导入 / 导出" to "诊断与搬运仍是 P1"
    )
    FeatureDestination.Desk -> listOf(
        "小蛇书桌" to "Myri 的工作台入口",
        "Worldbook" to "P2 深功能占位",
        "Workbench" to "工具与运行控制占位"
    )
}
