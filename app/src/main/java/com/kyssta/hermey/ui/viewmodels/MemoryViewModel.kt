package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.MemoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _facts = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val facts: StateFlow<List<MemoryEntry>> = _facts.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadFacts() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val api = authManager.api
                val response = api.getMemory()
                _facts.value = response.facts ?: emptyList()
            } catch (e: Exception) {
                // Error handling
            } finally {
                _loading.value = false
            }
        }
    }
}
