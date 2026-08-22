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
class SkillsViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _skills = MutableStateFlow<List<SkillInfo>>(emptyList())
    val skills: StateFlow<List<SkillInfo>> = _skills.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadSkills() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _skills.value = authManager.api.getSkills()
            } catch (_: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleSkill(name: String) {
        viewModelScope.launch {
            try {
                val skill = _skills.value.find { it.name == name }
                val enabled = skill?.enabled != true
                authManager.api.toggleSkill(com.google.gson.JsonObject().apply {
                    addProperty("name", name)
                    addProperty("enabled", enabled)
                })
                loadSkills()
            } catch (_: Exception) {
            }
        }
    }
}
