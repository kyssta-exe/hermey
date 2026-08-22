package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.auth.AuthState
import com.kyssta.hermey.networking.AuthProvidersResponse
import com.kyssta.hermey.networking.ServerAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel() {
    val state: StateFlow<AuthState> = authManager.state
    val servers: StateFlow<List<ServerAccount>> = authManager.servers
    val activeServerId: StateFlow<String?> = authManager.activeServerId

    fun testConnection(url: String): Flow<Result<AuthProvidersResponse>> = flow {
        emit(authManager.testConnection(url))
    }

    fun login(serverUrl: String, username: String, password: String, provider: String = "basic"): Flow<Result<Unit>> = flow {
        emit(authManager.login(serverUrl, username, password, provider))
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
}
