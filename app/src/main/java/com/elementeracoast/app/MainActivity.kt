package com.elementeracoast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elementeracoast.app.core.network.CoastGatewayClient
import com.elementeracoast.app.core.network.CoastSessionStore
import com.elementeracoast.app.core.theme.CoastTheme
import com.elementeracoast.app.core.theme.CoastThemeState
import com.elementeracoast.app.feature.chat.ChatScreen
import com.elementeracoast.app.feature.chat.ChatViewModel
import com.elementeracoast.app.feature.login.LoginScreen
import com.elementeracoast.app.feature.login.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionStore = CoastSessionStore(applicationContext)
        val gateway = CoastGatewayClient(sessionStore)
        val themeState = CoastThemeState(applicationContext)
        setContent { CoastApp(gateway, themeState) }
    }
}

private enum class AuthState { CHECKING, LOGGED_OUT, LOGGED_IN }

@Composable
fun CoastApp(gateway: CoastGatewayClient, themeState: CoastThemeState) {
    val theme by themeState.mode.collectAsStateWithLifecycle()
    var auth by remember { mutableStateOf(AuthState.CHECKING) }
    LaunchedEffect(Unit) {
        auth = runCatching { gateway.getSessionStatus().authenticated }
            .getOrDefault(false)
            .let { if (it) AuthState.LOGGED_IN else AuthState.LOGGED_OUT }
    }

    CoastTheme(theme) {
        when (auth) {
            AuthState.CHECKING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            AuthState.LOGGED_OUT -> {
                val login: LoginViewModel = viewModel(factory = simpleFactory { LoginViewModel(gateway) })
                val state by login.state.collectAsStateWithLifecycle()
                LoginScreen(state) { password -> login.login(password) { auth = AuthState.LOGGED_IN } }
            }
            AuthState.LOGGED_IN -> {
                val chat: ChatViewModel = viewModel(factory = simpleFactory { ChatViewModel(gateway) })
                val state by chat.state.collectAsStateWithLifecycle()
                ChatScreen(
                    state = state,
                    onSend = chat::send,
                    onStop = chat::stopGeneration,
                    onModelSelect = chat::selectModel,
                    onThemeCycle = themeState::cycle,
                )
            }
        }
    }
}

private fun <T : androidx.lifecycle.ViewModel> simpleFactory(create: () -> T) =
    object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : androidx.lifecycle.ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
