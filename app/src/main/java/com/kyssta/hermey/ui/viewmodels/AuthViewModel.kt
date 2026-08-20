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
class AuthViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel() {
    val state: StateFlow<AuthState> = authManager.state
    val servers: StateFlow<List<com.kyssta.hermey.networking.ServerAccount>> = authManager.servers
    val activeServerId: StateFlow<String?> = authManager.activeServerId
    val lastError: StateFlow<String?> = authManager.lastError
    val customHeaders: StateFlow<List<CustomHeader>> = authManager.customHeaders

    fun testConnection(url: String): Flow<Result<AuthStatusResponse>> = flow {
        emit(authManager.testConnection(url))
    }

    fun login(serverUrl: String, password: String): Flow<Result<Unit>> = flow {
        emit(authManager.login(serverUrl, password))
    }

    fun logout() {
        authManager.logout()
    }

    fun setActiveServer(serverId: String) {
        authManager.setActiveServer(serverId)
    }

    fun removeServer(serverId: String) {
        authManager.removeServer(serverId)
    }

    fun setCustomHeaders(headers: List<CustomHeader>) {
        authManager.setCustomHeaders(headers)
    }
}
