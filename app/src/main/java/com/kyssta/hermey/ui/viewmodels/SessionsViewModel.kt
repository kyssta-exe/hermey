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
class SessionsViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSessions() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // order=recent keeps long-running chats at the top across compression.
                val response = authManager.api.getSessions(mapOf("limit" to "50", "order" to "recent"))
                _sessions.value = response.sessions
            } catch (e: Exception) {
                _error.value = apiErrorMessage(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                authManager.api.deleteSession(sessionId)
                loadSessions()
            } catch (e: Exception) {
                _error.value = apiErrorMessage(e)
            }
        }
    }

    fun togglePin(sessionId: String, pinned: Boolean) {
        viewModelScope.launch {
            try {
                authManager.api.updateSession(sessionId, com.google.gson.JsonObject().apply {
                    addProperty("pinned", pinned)
                })
                loadSessions()
            } catch (e: Exception) {
                _error.value = apiErrorMessage(e)
            }
        }
    }

    fun archiveSession(sessionId: String) {
        viewModelScope.launch {
            try {
                authManager.api.updateSession(sessionId, com.google.gson.JsonObject().apply {
                    addProperty("archived", true)
                })
                loadSessions()
            } catch (e: Exception) {
                _error.value = apiErrorMessage(e)
            }
        }
    }
}
