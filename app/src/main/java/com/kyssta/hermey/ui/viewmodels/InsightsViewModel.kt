package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.InsightsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _insights = MutableStateFlow<InsightsResponse?>(null)
    val insights: StateFlow<InsightsResponse?> = _insights.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadInsights(days: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val api = authManager.api
                _insights.value = api.getInsights(days)
            } catch (e: Exception) {
                // Error handling
            } finally {
                _loading.value = false
            }
        }
    }
}
