package com.elementeracoast.app.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.network.CoastGatewayClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(val loading: Boolean = false, val error: String? = null)

class LoginViewModel(private val gateway: CoastGatewayClient) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun login(password: String, onSuccess: () -> Unit) {
        if (password.isBlank() || _state.value.loading) return
        _state.update { LoginUiState(loading = true) }
        viewModelScope.launch {
            runCatching { gateway.login(password) }
                .onSuccess { ok ->
                    if (ok) {
                        _state.value = LoginUiState()
                        onSuccess()
                    } else {
                        _state.value = LoginUiState(error = "海岸密码不正确，或登录门暂时不可达。")
                    }
                }
                .onFailure { error ->
                    _state.value = LoginUiState(error = error.message ?: "无法连接海岸。")
                }
        }
    }
}
