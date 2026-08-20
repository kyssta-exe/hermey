package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.KanbanBoard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _boards = MutableStateFlow<List<KanbanBoard>>(emptyList())
    val boards: StateFlow<List<KanbanBoard>> = _boards.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadBoards() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val api = authManager.api
                val config = api.getKanbanConfig()
                _boards.value = config.boards ?: emptyList()
            } catch (e: Exception) {
                // Error handling
            } finally {
                _loading.value = false
            }
        }
    }
}
