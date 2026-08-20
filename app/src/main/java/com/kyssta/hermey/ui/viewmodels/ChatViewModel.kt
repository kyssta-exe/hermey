package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _streaming = MutableStateFlow(false)
    val streaming: StateFlow<Boolean> = _streaming.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _currentTitle = MutableStateFlow<String?>(null)
    val currentTitle: StateFlow<String?> = _currentTitle.asStateFlow()

    private var currentSessionId: String? = null
    private var sseJob: kotlinx.coroutines.Job? = null

    fun updateInput(text: String) {
        _input.value = text
    }

    fun loadSession(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            _loading.value = true
            try {
                val api = authManager.api
                val session = api.getSession(sessionId, messages = true)
                _messages.value = session.messages ?: emptyList()
                _currentTitle.value = session.title
            } catch (e: Exception) {
                // Error handling
            } finally {
                _loading.value = false
            }
        }
    }

    fun sendMessage() {
        val text = _input.value.trim()
        if (text.isEmpty() || _streaming.value) return

        viewModelScope.launch {
            _input.value = ""
            _streaming.value = true

            // Add user message
            val userMsg = ChatMessage(role = "user", content = text, timestamp = System.currentTimeMillis() / 1000.0)
            _messages.value = _messages.value + userMsg

            try {
                val api = authManager.api
                val body = com.google.gson.JsonObject().apply {
                    addProperty("message", text)
                    currentSessionId?.let { addProperty("session_id", it) }
                    addProperty("model", "default")
                }

                val startResp = api.startChat(body)
                val streamId = startResp.streamId ?: return@launch

                // Start streaming
                startStreaming(streamId)
            } catch (e: Exception) {
                _messages.value = _messages.value.dropLast(1) // Remove user msg on error
                _streaming.value = false
            }
        }
    }

    private fun startStreaming(streamId: String) {
        sseJob = viewModelScope.launch {
            val baseUrl = authManager.baseUrl ?: run {
                _streaming.value = false
                return@launch
            }
            val sseClient = SSEClient(baseUrl, customHeaders = { authManager.customHeaders.value })
            sseClient.stream(
                path = Endpoints.CHAT_STREAM,
                query = mapOf("stream_id" to streamId),
                onEvent = { event ->
                    when (event) {
                        is SSEEvent.Token -> {
                            // Accumulate token text into a pending assistant message
                        }
                        SSEEvent.StreamEnd -> _streaming.value = false
                        is SSEEvent.Error -> {
                            _messages.value = _messages.value.dropLast(1)
                            _streaming.value = false
                        }
                        else -> {}
                    }
                },
                onComplete = { _streaming.value = false },
                onError = { e ->
                    _messages.value = _messages.value.dropLast(1)
                    _streaming.value = false
                }
            )
        }
    }

    fun cancelStream() {
        sseJob?.cancel()
        _streaming.value = false
    }

    override fun onCleared() {
        super.onCleared()
        sseJob?.cancel()
    }
}
