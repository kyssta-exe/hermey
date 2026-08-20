package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.FileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _files = MutableStateFlow<List<FileEntry>>(emptyList())
    val files: StateFlow<List<FileEntry>> = _files.asStateFlow()

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadDirectory(sessionId: String, path: String?) {
        viewModelScope.launch {
            _loading.value = true
            _currentPath.value = path ?: "/"
            try {
                val api = authManager.api
                val response = api.listDirectory(sessionId, path)
                _files.value = response.entries ?: emptyList()
            } catch (e: Exception) {
                // Error handling
            } finally {
                _loading.value = false
            }
        }
    }
}
