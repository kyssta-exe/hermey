package com.kyssta.hermey.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.kyssta.hermey.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onLoggedIn: () -> Unit,
    onError: (String) -> Unit
) {
    var serverUrl by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var needsPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val authVm: AuthViewModel = viewModel()

    LaunchedEffect(authVm.lastError) {
        authVm.lastError.value?.let { onError(it) }
    }

    fun doLogin(url: String, pass: String, requirePass: Boolean) {
        scope.launch {
            focusManager.clearFocus()
            testing = true
            errorMessage = null
            try {
                authVm.testConnection(url).collect { result ->
                    result.onSuccess { authStatus ->
                        if (authStatus.authEnabled == true && authStatus.passwordAuthEnabled == false) {
                            errorMessage = "This server uses passkey auth only"
                        } else {
                            needsPassword = authStatus.authEnabled == true
                            if (!needsPassword || pass.isNotEmpty()) {
                                authVm.login(url, pass).collect { loginResult ->
                                    loginResult.onSuccess { onLoggedIn() }
                                        .onFailure { errorMessage = it.message ?: "Connection failed" }
                                }
                            }
                        }
                    }
                    result.onFailure { errorMessage = it.message ?: "Connection failed" }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Connection failed"
            } finally {
                testing = false
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("HERMEY") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HermesColors.Surface,
                titleContentColor = HermesColors.OnSurface
            )
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.hermey_logo),
                contentDescription = "HERMEY",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                "Connect to your Hermes server",
                style = MaterialTheme.typography.headlineSmall,
                color = HermesColors.OnBackground
            )
            Text(
                "Enter the URL of your self-hosted hermes-webui server",
                style = MaterialTheme.typography.bodyMedium,
                color = HermesColors.OnSurfaceVariant
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://hermes.yourdomain.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    doLogin(serverUrl, password, needsPassword)
                })
            )

            if (needsPassword) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        doLogin(serverUrl, password, true)
                    })
                )
            }

            errorMessage?.let {
                Text(it, color = HermesColors.Error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { doLogin(serverUrl, password, needsPassword) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !testing && serverUrl.isNotEmpty()
            ) {
                if (testing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HermesColors.OnPrimary)
                } else {
                    Text("Connect")
                }
            }
        }
    }
}
