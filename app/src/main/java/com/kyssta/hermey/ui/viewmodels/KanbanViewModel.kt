package com.kyssta.hermey.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.kyssta.hermey.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class KanbanViewModel @Inject constructor(
    val authManager: AuthManager
) : ViewModel()
