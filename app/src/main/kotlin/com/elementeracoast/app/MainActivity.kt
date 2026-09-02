package com.elementeracoast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elementeracoast.app.feature.gate.GateScreen
import com.elementeracoast.app.feature.shell.CoastShellViewModel
import com.elementeracoast.app.feature.shell.MainShell
import com.elementeracoast.app.ui.theme.CoastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: CoastShellViewModel = viewModel(factory = CoastShellViewModel.factory(applicationContext))
            val state by vm.state.collectAsState()
            val wolf by vm.local.wolf.state.collectAsState()
            LaunchedEffect(wolf.appearance) { vm.syncAppearance() }

            CoastTheme(
                mode = wolf.appearance.theme,
                accentHex = wolf.appearance.accentHex,
                userBubbleHex = wolf.appearance.userBubbleHex
            ) {
                if (!state.authenticated) {
                    GateScreen(
                        password = state.password,
                        authBusy = state.authBusy,
                        authMessage = state.authMessage,
                        onPasswordChange = vm::setPassword,
                        onEnter = vm::enterCoast
                    )
                } else {
                    MainShell(
                        state = state,
                        services = vm.local,
                        daily = vm.daily,
                        onOpenRoomType = vm::openRoomType,
                        onSelectConversation = vm::selectConversation,
                        onNewConversation = vm::newConversation,
                        onRenameConversation = vm::renameConversation,
                        onDeleteConversation = vm::deleteConversation,
                        onCycleTheme = vm::cycleTheme,
                        onOpenFeature = vm::openFeature,
                        onBackToChat = vm::backToChat,
                        onSend = vm::sendMessage,
                        onStop = vm::stopGeneration,
                        onMessageAction = vm::handleMessageAction,
                        onOpenActionLog = vm::openActionLog,
                        onImportMessages = vm::importMessages,
                        onLocalActionLogged = vm::logLocalAction,
                        onOpenModels = vm::openModelPicker,
                        onDismissModels = vm::dismissModelPicker,
                        onSelectModel = vm::selectModel,
                        onRefreshModels = vm::refreshModels,
                        onPlaceholder = vm::showPlaceholder,
                        onSnackbarShown = vm::clearSnackbar
                    )
                }
            }
        }
    }
}
