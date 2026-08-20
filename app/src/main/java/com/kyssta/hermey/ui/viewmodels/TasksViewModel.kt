package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.networking.CronJob
import com.kyssta.hermey.networking.CronsResponse
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
                val api = authManager.api
                val response = api.getCrons()
                _jobs.value = response.jobs ?: emptyList()
            } catch (e: Exception) {
                // Error handling
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleJob(jobId: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val api = authManager.api
                val action = if (enabled) "resume" else "pause"
                when (action) {
                    "pause" -> api.pauseCron(jobId)
                    "resume" -> api.resumeCron(jobId)
                }
                loadCrons()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
}
