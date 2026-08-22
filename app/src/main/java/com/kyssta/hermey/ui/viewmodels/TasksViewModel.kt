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
class TasksViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _jobs = MutableStateFlow<List<CronJob>>(emptyList())
    val jobs: StateFlow<List<CronJob>> = _jobs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadCrons() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _jobs.value = authManager.api.getCrons()
            } catch (_: Exception) {
                // leave the current list; next refresh retries
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleJob(jobId: String, currentlyPaused: Boolean) {
        viewModelScope.launch {
            try {
                val api = authManager.api
                if (currentlyPaused) api.resumeCron(jobId) else api.pauseCron(jobId)
                loadCrons()
            } catch (_: Exception) {
            }
        }
    }
}
