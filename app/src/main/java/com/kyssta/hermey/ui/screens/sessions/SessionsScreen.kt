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
import com.kyssta.hermey.networking.SessionSummary
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.SessionsViewModel
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
    val error by sessionsVm.error.collectAsState()

    LaunchedEffect(Unit) { sessionsVm.loadSessions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                ),
                actions = {
                    TextButton(onClick = { sessionsVm.loadSessions() }) { Text("Refresh") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChat,
                containerColor = HermesColors.Primary
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium, color = HermesColors.OnPrimary)
            }
        }
    ) { padding ->
        if (loading && sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    error?.let { Text(it, color = HermesColors.Error, style = MaterialTheme.typography.bodySmall) }
                    Text("No sessions yet", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to start a new chat", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id ?: "?" }) { session ->
                    SessionCard(
                        session = session,
                        onClick = { session.id?.let(onSessionClick) }
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: SessionSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.title?.ifBlank { null } ?: "New Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = HermesColors.OnSurface,
                maxLines = 1
            )
            if (!session.preview.isNullOrBlank()) {
                Text(
                    text = session.preview!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = HermesColors.OnSurfaceVariant,
                    maxLines = 2
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                session.model?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = HermesColors.Primary, maxLines = 1)
                }
                Text(
                    text = "${session.messageCount ?: 0} messages",
                    style = MaterialTheme.typography.labelSmall,
                    color = HermesColors.OnSurfaceVariant
                )
            }
        }
    }
}
