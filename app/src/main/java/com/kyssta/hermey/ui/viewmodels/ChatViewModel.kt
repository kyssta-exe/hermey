package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentTitle = MutableStateFlow<String?>(null)
    val currentTitle: StateFlow<String?> = _currentTitle.asStateFlow()

    // The live WS session id (differs from the stored transcript id on resume).
    private var liveSessionId: String? = null
    private var wsJob: kotlinx.coroutines.Job? = null
    private var rpcId = 0

    fun updateInput(text: String) { _input.value = text }
    fun clearError() { _error.value = null }

    /** Load an existing conversation's transcript via REST. */
    fun loadSession(sessionId: String) {
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = authManager.api.getSessionMessages(sessionId, limit = 200, offset = 0)
                _messages.value = resp.messages.filter { it.role == "user" || it.role == "assistant" }
            } catch (e: Exception) {
                _error.value = apiErrorMessage(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun sendMessage() {
        val text = _input.value.trim()
        if (text.isEmpty() || _streaming.value) return
        _input.value = ""
        _streaming.value = true
        _error.value = null

        wsJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                GatewayChat(
                    authManager = authManager,
                    storedSessionId = currentTranscriptId,
                    existingLiveSid = liveSessionId,
                    nextRpcId = { ++rpcId },
                ).run(text).collect { update ->
                    when (update) {
                        is ChatUpdate.LiveSession -> liveSessionId = update.id
                        is ChatUpdate.Title -> if (_currentTitle.value == null) _currentTitle.value = update.title
                        is ChatUpdate.UserMessage -> _messages.value += update.message
                        is ChatUpdate.Delta -> appendDelta(update.text)
                        is ChatUpdate.Done -> {
                            replacePendingAssistant(update.text)
                            _streaming.value = false
                        }
                        is ChatUpdate.Failed -> {
                            _error.value = update.reason
                            _streaming.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Send failed"
                _streaming.value = false
            }
        }
    }

    /** Accumulate streamed tokens into a trailing assistant bubble. */
    private fun appendDelta(text: String) {
        val list = _messages.value.toMutableList()
        val last = list.lastOrNull()
        if (last != null && pendingAssistant) {
            list[list.size - 1] = last.copy(content = (last.content ?: "") + text)
        } else {
            list.add(ChatMessage(role = "assistant", content = text))
            pendingAssistant = true
        }
        _messages.value = list
    }

    private var pendingAssistant = false

    private fun replacePendingAssistant(finalText: String?) {
        if (!pendingAssistant) return
        val finalContent = finalText?.takeIf { it.isNotBlank() } ?: ""
        val list = _messages.value.toMutableList()
        if (list.isNotEmpty()) {
            val idx = list.size - 1
            list[idx] = list[idx].copy(content = finalContent.ifBlank { list[idx].content })
        }
        _messages.value = list
        pendingAssistant = false
    }

    private var currentTranscriptId: String? = null

    fun cancelStream() {
        wsJob?.cancel()
        _streaming.value = false
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
    }

    fun startNewChat() {
        _messages.value = emptyList()
        liveSessionId = null
        currentTranscriptId = null
        _currentTitle.value = null
        _error.value = null
    }
}

sealed class ChatUpdate {
    data class LiveSession(val id: String) : ChatUpdate()
    data class Title(val title: String) : ChatUpdate()
    data class UserMessage(val message: ChatMessage) : ChatUpdate()
    data class Delta(val text: String) : ChatUpdate()
    data class Done(val text: String?) : ChatUpdate()
    data class Failed(val reason: String) : ChatUpdate()
}

/**
 * One send = one short-lived /api/ws connection:
 * connect → session.create|session.resume → prompt.submit → drain events until message.complete.
 * Verified against the live gateway protocol.
 */
private class GatewayChat(
    private val authManager: AuthManager,
    private val storedSessionId: String?,
    private val existingLiveSid: String?,
    private val nextRpcId: () -> Int,
) {
    suspend fun run(text: String) = flow {
        val ticketResult = authManager.mintWsTicket()
        val ticket = ticketResult.getOrElse { emit(ChatUpdate.Failed(it.message ?: "auth failed")); return@flow }
        val baseUrl = authManager.baseUrl ?: run { emit(ChatUpdate.Failed("No server connected")); return@flow }

        val client = GatewayWsClient(baseUrl, ticket)
        var opened = false
        client.connect { opened = true }
        try {
            // Wait for the socket to open (first server frame is gateway.ready).
            val deadline = System.currentTimeMillis() + 20_000
            while (!opened && System.currentTimeMillis() < deadline) {
                val f = client.nextFrame(1) ?: break
                if (frameEvent(f)?.first == "gateway.ready") { opened = true; break }
            }
            if (!opened) {
                emit(ChatUpdate.Failed(client.lastError() ?: "Could not reach gateway stream"))
                return@flow
            }

            // Attach to a session: resume existing or create new.
            var sid = existingLiveSid
            if (sid == null) {
                val id = nextRpcId()
                if (storedSessionId.isNullOrBlank()) {
                    client.rpc("session.create", mapOf("title" to ""), id)
                } else {
                    client.rpc("session.resume", mapOf("session_id" to storedSessionId), id)
                }
                val reply = awaitReply(client, id)
                    ?: run { emit(ChatUpdate.Failed("Gateway did not respond")); return@flow }
                @Suppress("UNCHECKED_CAST")
                val result = reply["result"] as? Map<String, Any?>
                sid = (result?.get("session_id"))?.toString()
                if (sid.isNullOrBlank()) {
                    emit(ChatUpdate.Failed(reply.errMessage() ?: "Could not open session"))
                    return@flow
                }
                emit(ChatUpdate.LiveSession(sid))
                @Suppress("UNCHECKED_CAST")
                val info = (reply["result"] as? Map<String, Any?>)?.get("info") as? Map<String, Any?>
                (info?.get("model"))?.let { emit(ChatUpdate.Title(it.toString())) }
            }
            emit(ChatUpdate.UserMessage(ChatMessage(role = "user", content = text)))

            // Submit the prompt.
            val submitId = nextRpcId()
            client.rpc("prompt.submit", mapOf("session_id" to sid, "text" to text), submitId)

            // Drain events until the terminal message.complete.
            val buf = StringBuilder()
            var done = false
            val hardDeadline = System.currentTimeMillis() + 300_000L
            while (!done && System.currentTimeMillis() < hardDeadline) {
                val frame = client.nextFrame(30) ?: break
                if (frameReply(frame, submitId)) continue // {"status":"streaming"} ack
                val event = frameEvent(frame) ?: continue
                when (event.first) {
                    "message.delta" -> {
                        val t = event.second["text"]?.toString() ?: ""
                        buf.append(t)
                        emit(ChatUpdate.Delta(t))
                    }
                    "message.complete" -> {
                        val finalText = event.second["text"]?.toString()
                        done = true
                        emit(ChatUpdate.Done(finalText ?: buf.toString()))
                    }
                    "message.interim" -> {} // sealed into complete by the server
                    "status.update", "thinking.delta", "reasoning.delta",
                    "tool.start", "tool.complete", "metering", "heartbeat",
                    "title.update", "session.info", "session.usage" -> {}
                    else -> {}
                }
            }
            if (!done) emit(ChatUpdate.Failed("Stream ended unexpectedly"))
        } finally {
            client.close()
        }
    }

    private suspend inline fun awaitReply(
        client: GatewayWsClient,
        id: Int,
    ): Map<String, Any?>? {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val frame = client.nextFrame(5) ?: return null
            if (frameReply(frame, id)) return frame
        }
        return null
    }
}
