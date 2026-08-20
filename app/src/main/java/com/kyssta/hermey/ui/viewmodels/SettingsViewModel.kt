package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.ServerAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    val servers: StateFlow<List<ServerAccount>> = authManager.servers
    val activeServerId: StateFlow<String?> = authManager.activeServerId

    fun setActiveServer(serverId: String) {
        authManager.setActiveServer(serverId)
    }

    fun removeServer(serverId: String) {
        authManager.removeServer(serverId)
    }

    fun logout() {
        authManager.logout()
    }
}
