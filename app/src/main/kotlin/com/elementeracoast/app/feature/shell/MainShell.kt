package com.elementeracoast.app.feature.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.ChatSearchHit
import com.elementeracoast.app.core.model.ConversationContextSettings
import com.elementeracoast.app.feature.chatgpt.ChatGptConnectState
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.MessageAction
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.feature.chat.ChatWindow
import com.elementeracoast.app.feature.chat.ModelQuickPicker
import com.elementeracoast.app.feature.daily.DailyRepository
import com.elementeracoast.app.feature.dogtalk.CrossWindowRepository
import com.elementeracoast.app.feature.dogtalk.CrossWindowUiState
import com.elementeracoast.app.feature.dogtalk.DogtalkRepository
import com.elementeracoast.app.feature.memory.MemoryRepository
import com.elementeracoast.app.feature.serpentdesk.DevHandsRepository
import com.elementeracoast.app.feature.wolf.GlobalArchiveRepository
import com.elementeracoast.app.ui.theme.CoastThemePreset
import com.elementeracoast.app.ui.theme.LocalSnowLetterVisuals
import com.elementeracoast.app.ui.theme.SnowLetterVisualSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.feature.wolf.LocalGithubUpdater
import com.elementeracoast.app.feature.wolf.LocalRelease
import com.elementeracoast.app.ui.theme.SnowLetterSurface
import com.elementeracoast.app.ui.theme.SnowLetterSurfaceRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainShell(
    state: CoastShellState,
    chatGpt: ChatGptConnectState,
    services: LocalFeatureServices,
    daily: DailyRepository,
    memory: MemoryRepository,
    dogtalk: DogtalkRepository,
    devHands: DevHandsRepository,
    archive: GlobalArchiveRepository,
    crossWindowRepository: CrossWindowRepository,
    crossWindow: CrossWindowUiState,
    onCrossWindowChange: (CrossWindowUiState) -> Unit,
    onOpenRoomType: (RoomType) -> Unit,
    onSelectConversation: (String) -> Unit,
    onSelectSearchHit: (ChatSearchHit) -> Unit,
    onNewConversation: () -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSelectTheme: (CoastThemePreset) -> Unit,
    onOpenFeature: (FeatureDestination) -> Unit,
    onBackToChat: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateMyriAvatar: (String) -> Unit,
    onUploadAttachment: (String, String, ByteArray) -> Unit,
    onRemovePendingAttachment: (String) -> Unit,
    onComposerDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onSendImage: (String) -> Unit = {},
    onStop: () -> Unit,
    onMessageAction: (MessageAction) -> Unit,
    onOpenActionLog: (Set<String>) -> Unit,
    onSetThoughtSoilEnabled: (Boolean) -> Unit,
    onSaveThoughtSoil: (String, List<String>) -> Unit,
    onResumeThoughtSoil: () -> Unit,
    onSetContextSettings: (ConversationContextSettings) -> Unit,
    onImportMessages: (List<ChatMessage>) -> Unit,
    onLocalActionLogged: (String, String, String) -> Unit,
    onOpenModels: () -> Unit,
    onDismissModels: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectImageModel: (String) -> Unit = {},
    onRefreshModels: () -> Unit,
    onChatGptConnect: () -> Unit,
    onChatGptDisconnect: () -> Unit,
    onChatGptRefresh: () -> Unit,
    onChatGptProbe: (String) -> Unit,
    onEnterLocalWorkspace: () -> Unit,
    onLogout: () -> Unit,
    onPlaceholder: (String) -> Unit,
    onSnackbarShown: () -> Unit,
    dreamApi: com.elementeracoast.app.core.network.CoastApiClient? = null
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var openPendingMemoryOnLanding by remember { mutableStateOf(false) }
    var openModelBoxOnLanding by rememberSaveable { mutableStateOf(false) }
    var showThemeWardrobe by remember { mutableStateOf(false) }
    var showWindowContext by rememberSaveable { mutableStateOf(false) }
    var openUpdateOnLanding by rememberSaveable { mutableStateOf(false) }
    val noticeContext = LocalContext.current
    // Device-only, shared by both workspace modes. Never interrupt a conversation or install automatically.
    val noticePrefs = remember(noticeContext) { noticeContext.getSharedPreferences("coast_release_notice_v1", android.content.Context.MODE_PRIVATE) }
    val noticeLifecycle = (LocalContext.current as? LifecycleOwner)?.lifecycle
    var pendingRelease by remember { mutableStateOf<LocalRelease?>(null) }
    var shownRelease by remember { mutableStateOf<LocalRelease?>(null) }
    val canShowRelease = state.activeFeature == null && state.activeRoomType == RoomType.Main &&
        state.activeConversationId.isBlank() && state.composerDraft.isBlank() &&
        state.messages.isEmpty() && state.pendingAttachments.isEmpty() &&
        !state.isStreaming && !state.isFinalizing && !state.backgroundGeneration &&
        !state.attachmentUploading && !state.historyLoading && !state.showModelPicker &&
        !drawerState.isOpen && !showWindowContext && !showThemeWardrobe && state.snackbarMessage == null
    LaunchedEffect(noticeLifecycle) {
        suspend fun checkRelease() {
            try {
                val release = LocalGithubUpdater().latest()
                if (release.code > BuildConfig.VERSION_CODE &&
                    release.code > noticePrefs.getInt("shown_version_code", 0)) {
                    pendingRelease = release
                } else pendingRelease = null
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Optional notice: offline and rate limits must stay silent. */ }
        }
        if (noticeLifecycle == null) checkRelease()
        else noticeLifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Throttle resumes as well as long foreground sessions.
            while (true) {
                val now = android.os.SystemClock.elapsedRealtime()
                val last = noticePrefs.getLong("last_check_elapsed", -1L)
                if (last < 0L || now < last || now - last >= 15 * 60 * 1000L) {
                    noticePrefs.edit().putLong("last_check_elapsed", now).apply()
                    checkRelease()
                }
                delay(15 * 60 * 1000L)
            }
        }
    }
    LaunchedEffect(canShowRelease, pendingRelease?.code) {
        if (!canShowRelease) {
            shownRelease = null
        } else {
            val release = pendingRelease
            if (release != null && release.code > noticePrefs.getInt("shown_version_code", 0)) {
                noticePrefs.edit().putInt("shown_version_code", release.code).apply()
                shownRelease = release
                pendingRelease = null
            }
        }
    }
    val defaultSnowLetterVisuals = remember { SnowLetterVisualSettings() }
    var snowLetterBackgroundAlpha by rememberSaveable { mutableStateOf(defaultSnowLetterVisuals.chatBackgroundAlpha) }
    var snowLetterDecorationAlpha by rememberSaveable { mutableStateOf(defaultSnowLetterVisuals.decorationAlpha) }
    var snowLetterPaperTextureAlpha by rememberSaveable { mutableStateOf(defaultSnowLetterVisuals.paperTextureAlpha) }
    val snowLetterVisuals = remember(
        snowLetterBackgroundAlpha,
        snowLetterDecorationAlpha,
        snowLetterPaperTextureAlpha
    ) {
        SnowLetterVisualSettings(
            chatBackgroundAlpha = snowLetterBackgroundAlpha,
            decorationAlpha = snowLetterDecorationAlpha,
            paperTextureAlpha = snowLetterPaperTextureAlpha
        )
    }

    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        onSnackbarShown()
    }

    fun closeDrawerThen(block: () -> Unit) {
        block()
        coroutineScope.launch { drawerState.close() }
    }

    CompositionLocalProvider(LocalSnowLetterVisuals provides snowLetterVisuals) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                CoastDrawer(
                    state = state,
                    drawerOpen = drawerState.isOpen,
                    onSnackbar = onPlaceholder,
                    onClose = { coroutineScope.launch { drawerState.close() } },
                    onOpenRoomType = { roomType -> closeDrawerThen { onOpenRoomType(roomType) } },
                    onSelectConversation = { id -> closeDrawerThen { onSelectConversation(id) } },
                    onSelectSearchHit = { hit -> closeDrawerThen { onSelectSearchHit(hit) } },
                    onRenameConversation = onRenameConversation,
                    onDeleteConversation = onDeleteConversation,
                    onOpenFeature = { destination -> closeDrawerThen { openModelBoxOnLanding = false; openUpdateOnLanding = false; onOpenFeature(destination) } },
                    onCycleTheme = { closeDrawerThen { showThemeWardrobe = true } }
                )
            }
        ) {
            val ownsPageChrome = state.activeFeature == FeatureDestination.Memory ||
                state.activeFeature == FeatureDestination.Daily ||
                state.activeFeature == FeatureDestination.IslandLetter ||
                state.activeFeature == FeatureDestination.Wolf
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    Column {
                        if (!ownsPageChrome) {
                            CoastTopBar(
                                state = state,
                                onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                                onBack = onBackToChat,
                                onOpenModels = onOpenModels,
                                onRefresh = onRefresh,
                                onNewConversation = onNewConversation,
                                onMore = { showWindowContext = true }
                            )
                        }
                        if (canShowRelease) shownRelease?.let { release ->
                            SnowLetterSurface(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                role = SnowLetterSurfaceRole.StatusCard,
                                fallbackColor = MaterialTheme.colorScheme.surface,
                                fallbackBorder = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
                            ) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text("发现新版本 · ${release.version}", style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text("可以现在查看，也可以稍后在「版本与更新」里安装。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { shownRelease = null },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("稍后") }
                                        TextButton(onClick = {
                                            shownRelease = null
                                            openModelBoxOnLanding = false
                                            openUpdateOnLanding = true
                                            onOpenFeature(FeatureDestination.Wolf)
                                        }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Text("查看更新") }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(Modifier.fillMaxSize().padding(innerPadding)) {
                    val feature = state.activeFeature
                    if (feature == null) {
                        ChatWindow(
                            state = state,
                            dogtalk = dogtalk,
                            crossWindowRepository = crossWindowRepository,
                            crossWindow = crossWindow,
                            onCrossWindowChange = onCrossWindowChange,
                            onUploadAttachment = onUploadAttachment,
                            onRemovePendingAttachment = onRemovePendingAttachment,
                            onComposerDraftChange = onComposerDraftChange,
                            onSend = onSend,
                            onSendImage = onSendImage,
                            onStop = onStop,
                            onMessageAction = onMessageAction,
                            onOpenActionLog = onOpenActionLog,
                            onSetThoughtSoilEnabled = onSetThoughtSoilEnabled,
                            onSaveThoughtSoil = onSaveThoughtSoil,
                            onResumeThoughtSoil = onResumeThoughtSoil,
                            onOpenPendingMemory = {
                                openPendingMemoryOnLanding = true
                                onOpenFeature(FeatureDestination.Memory)
                            },
                            onPlaceholder = onPlaceholder
                        )
                    } else {
                        FeatureLandingScreen(
                            feature = feature,
                            shellState = state,
                            chatGpt = chatGpt,
                            services = services,
                            daily = daily,
                            memory = memory,
                            devHands = devHands,
                            archive = archive,
                            openMemoryPending = openPendingMemoryOnLanding,
                            openModelBoxInitially = openModelBoxOnLanding,
                            openUpdateInitially = openUpdateOnLanding,
                            onMemoryPendingConsumed = { openPendingMemoryOnLanding = false },
                            messages = state.messages,
                            onBackToChat = { openModelBoxOnLanding = false; openUpdateOnLanding = false; onBackToChat() },
                            onOpenSearchHit = onSelectSearchHit,
                            onRefresh = onRefresh,
                            onUpdateMyriAvatar = onUpdateMyriAvatar,
                            onSelectModel = onSelectModel,
                            onSelectImageModel = onSelectImageModel,
                            onRefreshModels = onRefreshModels,
                            onChatGptConnect = onChatGptConnect,
                            onChatGptDisconnect = onChatGptDisconnect,
                            onChatGptRefresh = onChatGptRefresh,
                            onChatGptProbe = onChatGptProbe,
                            onEnterLocalWorkspace = onEnterLocalWorkspace,
                            onLogout = onLogout,
                            onImportMessages = onImportMessages,
                            onLocalActionLogged = onLocalActionLogged,
                            onPlaceholder = onPlaceholder
                        )
                    }
                }
            }
        }

        if (state.showModelPicker && state.activeFeature == null) {
            ModelQuickPicker(
                models = state.models,
                modelReasoningChoices = state.modelReasoningChoices,
                currentModel = state.currentModel,
                onPick = onSelectModel,
                onDismiss = onDismissModels,
                chatGpt = chatGpt,
                onChatGptConnect = onChatGptConnect,
                onChatGptRefresh = onChatGptRefresh,
                localMode = state.localMode,
                onOpenModelBox = { onDismissModels(); openUpdateOnLanding = false; openModelBoxOnLanding = true; onOpenFeature(FeatureDestination.Wolf) }
            )
        }
        if (showWindowContext && state.activeFeature == null) {
            WindowContextSheet(
                conversationId = state.activeConversationId,
                settings = state.contextSettings,
                thoughtSoilEnabled = state.thoughtSoil?.autoRefreshEnabled ?: true,
                dreamApi = dreamApi,
                onSettingsChange = onSetContextSettings,
                onSetThoughtSoilEnabled = onSetThoughtSoilEnabled,
                onOpenIslandLetter = {
                    showWindowContext = false
                    onOpenFeature(FeatureDestination.IslandLetter)
                },
                onDismiss = { showWindowContext = false }
            )
        }
        if (showThemeWardrobe) {
            ThemeWardrobeSheet(
                current = state.theme,
                snowLetterVisuals = snowLetterVisuals,
                onSnowLetterVisualsChange = { next ->
                    snowLetterBackgroundAlpha = next.chatBackgroundAlpha
                    snowLetterDecorationAlpha = next.decorationAlpha
                    snowLetterPaperTextureAlpha = next.paperTextureAlpha
                },
                onSelect = onSelectTheme,
                onDismiss = { showThemeWardrobe = false }
            )
        }
    }
}