package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.MemoryStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _memory = MutableStateFlow<MemoryStatus?>(null)
    val memory: StateFlow<MemoryStatus?> = _memory.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadMemory() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _memory.value = authManager.api.getMemory()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }
}
