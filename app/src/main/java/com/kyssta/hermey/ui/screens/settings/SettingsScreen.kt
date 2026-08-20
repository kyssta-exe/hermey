package com.kyssta.hermey.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.navigation.Screen
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToInsights: () -> Unit,
    onNavigateToMemory: () -> Unit
) {
    val settingsVm: SettingsViewModel = viewModel()
    val servers by settingsVm.servers.collectAsState()
    val activeServerId by settingsVm.activeServerId.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Server management
            item {
                Text("Servers", style = MaterialTheme.typography.titleSmall, color = HermesColors.Primary)
            }
            items(servers) { server ->
                ServerRow(
                    server = server,
                    isActive = server.id == activeServerId,
                    onSelect = { settingsVm.setActiveServer(server.id) },
                    onRemove = { scope.launch { settingsVm.removeServer(server.id) } }
                )
            }
            item {
                Button(
                    onClick = { /* Add server dialog */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = servers.size < 5
                ) {
                    Text("Add Server")
                }
            }

            // Feature navigation
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Text("Features", style = MaterialTheme.typography.titleSmall, color = HermesColors.Primary)
            }
            item {
                SettingsButton(
                    title = "Insights",
                    description = "View usage analytics and statistics",
                    onClick = onNavigateToInsights
                )
            }
            item {
                SettingsButton(
                    title = "Memory",
                    description = "Browse and manage agent memory",
                    onClick = onNavigateToMemory
                )
            }

            // Account
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Text("Account", style = MaterialTheme.typography.titleSmall, color = HermesColors.Primary)
            }
            item {
                SettingsButton(
                    title = "Sign Out",
                    description = "Clear saved credentials",
                    onClick = { scope.launch { settingsVm.logout() } },
                    isError = true
                )
            }
        }
    }
}

@Composable
fun ServerRow(
    server: com.kyssta.hermey.networking.ServerAccount,
    isActive: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) HermesColors.Primary.copy(alpha = 0.2f) else HermesColors.Glass
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = server.displayName.ifEmpty { server.urlString },
                    style = MaterialTheme.typography.titleMedium,
                    color = HermesColors.OnSurface
                )
                Text(
                    text = server.urlString,
                    style = MaterialTheme.typography.bodySmall,
                    color = HermesColors.OnSurfaceVariant
                )
            }
            Row {
                if (!isActive) {
                    Text(
                        text = "Select",
                        style = MaterialTheme.typography.labelLarge,
                        color = HermesColors.Primary,
                        modifier = Modifier.clickable(onClick = onSelect)
                    )
                } else {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = HermesColors.Success
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = HermesColors.Error,
                    modifier = Modifier.clickable(onClick = onRemove)
                )
            }
        }
    }
}

@Composable
fun SettingsButton(
    title: String,
    description: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isError) HermesColors.Error else HermesColors.OnSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = HermesColors.OnSurfaceVariant
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = HermesColors.OnSurfaceVariant)
        }
    }
}
