package com.elementeracoast.app.feature.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.FeatureDestination
import com.elementeracoast.app.core.model.RoomType
import com.elementeracoast.app.feature.chat.ChatWindow
import com.elementeracoast.app.feature.chat.ModelQuickPicker
import kotlinx.coroutines.launch

@Composable
fun MainShell(
    state: CoastShellState,
    onOpenRoomType: (RoomType) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onDeleteConversation: (String) -> Unit,
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
                onOpenRoomType = { roomType -> closeDrawerThen { onOpenRoomType(roomType) } },
                onSelectConversation = { id -> closeDrawerThen { onSelectConversation(id) } },
                onRenameConversation = onRenameConversation,
                onDeleteConversation = onDeleteConversation,
                onOpenFeature = { destination -> closeDrawerThen { onOpenFeature(destination) } },
                onCycleTheme = onCycleTheme
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CoastTopBar(
                    state = state,
                    onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                    onBack = onBackToChat,
                    onOpenModels = onOpenModels,
                    onNewConversation = onNewConversation,
                    onMore = { onPlaceholder("窗口更多操作暂未接线。") }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val feature = state.activeFeature
                if (feature == null) {
                    ChatWindow(
                        state = state,
                        onSend = onSend,
                        onStop = onStop,
                        onPlaceholder = onPlaceholder
                    )
                } else {
                    FeatureLandingScreen(feature = feature, onPlaceholder = onPlaceholder)
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
