package com.elementeracoast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            val vm: CoastShellViewModel = viewModel()
            val state by vm.state.collectAsState()
            CoastTheme(mode = state.theme) {
                if (!state.authenticated) {
                    GateScreen(
                        password = state.password,
                        onPasswordChange = vm::setPassword,
                        onEnter = vm::enterLocalShell
                    )
                } else {
                    MainShell(
                        state = state,
                        onOpenRoomType = vm::openRoomType,
                        onSelectConversation = vm::selectConversation,
                        onNewConversation = vm::newConversation,
                        onRenameConversation = vm::renameConversation,
                        onDeleteConversation = vm::deleteConversation,
                        onCycleTheme = vm::cycleTheme,
                        onOpenFeature = vm::openFeature,
                        onBackToChat = vm::backToChat,
                        onSend = vm::sendFakeMessage,
                        onStop = vm::stopGeneration,
                        onMessageAction = vm::handleMessageAction,
                        onOpenModels = vm::openModelPicker,
                        onDismissModels = vm::dismissModelPicker,
                        onSelectModel = vm::selectModel,
                        onPlaceholder = vm::showPlaceholder,
                        onSnackbarShown = vm::clearSnackbar
                    )
                }
            }
        }
    }
}
