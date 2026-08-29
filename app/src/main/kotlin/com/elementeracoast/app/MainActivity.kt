package com.elementeracoast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elementeracoast.app.feature.chat.ChatScreen
import com.elementeracoast.app.feature.login.LoginScreen
import com.elementeracoast.app.feature.shell.CoastShellViewModel
import com.elementeracoast.app.ui.theme.CoastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoastTheme {
                val vm: CoastShellViewModel = viewModel()
                val state by vm.state.collectAsState()

                if (!state.authenticated) {
                    LoginScreen(
                        password = state.password,
                        onPasswordChange = vm::setPassword,
                        onEnter = vm::enterLocalShell
                    )
                } else {
                    ChatScreen(
                        state = state,
                        onSend = vm::sendFakeMessage,
                        onStop = vm::stopGeneration,
                        onOpenModels = vm::openModelPicker,
                        onDismissModels = vm::dismissModelPicker,
                        onSelectModel = vm::selectModel
                    )
                }
            }
        }
    }
}
