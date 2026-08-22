package com.kyssta.hermey.ui.screens.kanban

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanban") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                "No boards on this server",
                style = MaterialTheme.typography.titleMedium,
                color = HermesColors.OnSurfaceVariant
            )
        }
    }
}
