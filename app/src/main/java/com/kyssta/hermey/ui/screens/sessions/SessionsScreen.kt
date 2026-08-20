package com.kyssta.hermey.ui.screens.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.auth.AuthManager
import com.kyssta.hermey.navigation.Screen
import com.kyssta.hermey.networking.SessionSummary
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.SessionsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onSessionClick: (String) -> Unit,
    onNewChat: () -> Unit
) {
    val sessionsVm: SessionsViewModel = viewModel()
    val sessions by sessionsVm.sessions.collectAsState()
    val loading by sessionsVm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { sessionsVm.loadSessions() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                ),
                actions = {
                    IconButton(onClick = { scope.launch { sessionsVm.loadSessions() } }) {
                        Text("⟳", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChat,
                containerColor = HermesColors.Primary
            ) {
                androidx.compose.material3.Text("+", style = MaterialTheme.typography.headlineMedium, color = HermesColors.OnPrimary)
            }
        }
    ) { padding ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No sessions yet", style = MaterialTheme.typography.titleMedium)
                    Text("Start a new chat to begin", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.sessionId ?: "") }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    session: SessionSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = session.title ?: "New Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = HermesColors.OnSurface
            )
            Text(
                text = "${session.model ?: "default"} · ${session.messageCount ?: 0} messages",
                style = MaterialTheme.typography.bodySmall,
                color = HermesColors.OnSurfaceVariant
            )
        }
    }
}
