package com.kyssta.hermey.ui.screens.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(sessionId: String, onNavigateBack: () -> Unit) {
    // File browsing needs the gateway file API (/api/fs/list), not wired yet.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                "Workspace browsing coming soon",
                style = MaterialTheme.typography.titleMedium,
                color = HermesColors.OnSurfaceVariant
            )
        }
    }
}
