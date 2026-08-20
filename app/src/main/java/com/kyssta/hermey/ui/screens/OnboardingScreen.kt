package com.kyssta.hermey.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import com.kyssta.hermey.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.AuthViewModel
import com.kyssta.hermey.networking.AuthProvider
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

private enum class AuthStage { Url, Providers, Credentials }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onLoggedIn: () -> Unit,
    onError: (String) -> Unit
) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var authStage by remember { mutableStateOf(AuthStage.Url) }
    var availableProviders by remember { mutableStateOf<List<AuthProvider>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val authVm: AuthViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(authVm.lastError) {
        authVm.lastError.value?.let { onError(it) }
    }

    fun doConnect(url: String) {
        scope.launch {
            focusManager.clearFocus()
            testing = true
            errorMessage = null
            authVm.testConnection(url).collect { result ->
                result.onSuccess { providers ->
                    availableProviders = providers.providers
                    authStage = AuthStage.Providers
                }.onFailure { errorMessage = it.message ?: "Connection failed" }
            }
            testing = false
        }
    }

    fun doLogin(url: String, user: String, pass: String, prov: String) {
        scope.launch {
            focusManager.clearFocus()
            testing = true
            errorMessage = null
            authVm.login(url, user, pass, prov).collect { loginResult ->
                loginResult.onSuccess { onLoggedIn() }
                    .onFailure { errorMessage = it.message ?: "Login failed" }
            }
            testing = false
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

            when (authStage) {
                AuthStage.Url -> {
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
                            if (serverUrl.isNotEmpty()) doConnect(serverUrl)
                        })
                    )

                    Button(
                        onClick = { doConnect(serverUrl) },
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

                AuthStage.Providers -> {
                    Text(
                        "Select authentication method",
                        style = MaterialTheme.typography.headlineSmall,
                        color = HermesColors.OnBackground
                    )

                    availableProviders.forEach { provider ->
                        Button(
                            onClick = {
                                if (provider.name == "basic" && provider.supportsPassword == true) {
                                    authStage = AuthStage.Credentials
                                } else if (provider.name == "nous") {
                                    val oauthUrl = "$serverUrl/auth/login?provider=nous"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl)))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !testing
                        ) {
                            Text(provider.displayName ?: provider.name ?: "Unknown")
                        }
                    }

                    Text(
                        "Server: $serverUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesColors.OnSurfaceVariant
                    )

                    OutlinedButton(onClick = { authStage = AuthStage.Url }) {
                        Text("Back")
                    }
                }

                AuthStage.Credentials -> {
                    Text(
                        "Enter credentials",
                        style = MaterialTheme.typography.headlineSmall,
                        color = HermesColors.OnBackground
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (username.isNotEmpty() && password.isNotEmpty()) {
                                doLogin(serverUrl, username, password, "basic")
                            }
                        })
                    )

                    Button(
                        onClick = {
                            if (username.isNotEmpty() && password.isNotEmpty()) {
                                doLogin(serverUrl, username, password, "basic")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !testing && username.isNotEmpty() && password.isNotEmpty()
                    ) {
                        if (testing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HermesColors.OnPrimary)
                        } else {
                            Text("Login")
                        }
                    }

                    OutlinedButton(onClick = { authStage = AuthStage.Providers }) {
                        Text("Cancel")
                    }
                }
            }

            errorMessage?.let {
                Text(it, color = HermesColors.Error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
