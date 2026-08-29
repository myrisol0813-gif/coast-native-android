/*
 * Streaming state ownership is derived in part from MiniiChat's MIT-licensed
 * ChatViewModel approach: one generation Job, transient in-memory text, and
 * explicit stop/cancel ownership. Provider, API-key, assistant, attachment,
 * and persistence logic were intentionally not carried over.
 * See THIRD_PARTY_NOTICES.md.
 */
package com.elementeracoast.app.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elementeracoast.app.core.model.ChatMessage
import com.elementeracoast.app.core.model.CoastShellState
import com.elementeracoast.app.core.model.MessageRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoastShellViewModel : ViewModel() {
    private val _state = MutableStateFlow(CoastShellState())
    val state: StateFlow<CoastShellState> = _state.asStateFlow()

    private var generationJob: Job? = null
    private var nextId = 10L

    fun setPassword(value: String) {
        _state.update { it.copy(password = value) }
    }

    fun enterLocalShell() {
        if (_state.value.password.isBlank()) return
        _state.update { it.copy(authenticated = true, password = "") }
    }

    fun openModelPicker() {
        _state.update { it.copy(showModelPicker = true) }
    }

    fun dismissModelPicker() {
        _state.update { it.copy(showModelPicker = false) }
    }

    fun selectModel(model: String) {
        _state.update { it.copy(currentModel = model, showModelPicker = false) }
    }

    fun sendFakeMessage(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _state.value.isStreaming || generationJob?.isActive == true) return

        val userId = nextId++
        val assistantId = nextId++
        _state.update {
            it.copy(
                messages = it.messages +
                    ChatMessage(userId, MessageRole.User, clean) +
                    ChatMessage(assistantId, MessageRole.Assistant, ""),
                isStreaming = true
            )
        }

        generationJob = viewModelScope.launch {
            val chunks = listOf(
                "This is local fake streaming. ",
                "The send button and stop button share one place, ",
                "the assistant reply grows in the message flow, ",
                "and no Coast or provider endpoint is contacted. ",
                "The final colors and spacing still belong to the future PWA reference pack."
            )
            try {
                for (chunk in chunks) {
                    delay(320)
                    appendAssistantDelta(assistantId, chunk)
                }
            } catch (cancelled: CancellationException) {
                appendAssistantDelta(assistantId, "\n\n[stopped locally]")
                throw cancelled
            } finally {
                _state.update { it.copy(isStreaming = false) }
                generationJob = null
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
    }

    private fun appendAssistantDelta(messageId: Long, delta: String) {
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id == messageId) message.copy(text = message.text + delta) else message
                }
            )
        }
    }
}
