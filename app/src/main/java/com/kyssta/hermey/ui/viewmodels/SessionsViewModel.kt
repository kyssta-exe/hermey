package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.auth.AuthState
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
                val api = authManager.api
                val response = api.getSessions(mapOf("include_archived" to "0"))
                _sessions.value = response.sessions ?: emptyList()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val api = authManager.api
                api.deleteSession(com.google.gson.JsonObject().apply {
                    addProperty("session_id", sessionId)
                })
                loadSessions()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun archiveSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val api = authManager.api
                api.archiveSession(com.google.gson.JsonObject().apply {
                    addProperty("session_id", sessionId)
                })
                loadSessions()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
