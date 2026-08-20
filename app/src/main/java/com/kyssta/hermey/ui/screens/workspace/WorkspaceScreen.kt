package com.kyssta.hermey.ui.screens.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.networking.FileEntry
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.WorkspaceViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(sessionId: String, onNavigateBack: () -> Unit) {
    val workspaceVm: WorkspaceViewModel = viewModel()
    val files by workspaceVm.files.collectAsState()
    val currentPath by workspaceVm.currentPath.collectAsState()
    val loading by workspaceVm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        scope.launch { workspaceVm.loadDirectory(sessionId, path = null) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workspace: $currentPath") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files) { file ->
                    FileCard(file = file) {
                        if (file.type == "dir") {
                            scope.launch { workspaceVm.loadDirectory(sessionId, path = file.path ?: "") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileCard(file: FileEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = if (file.type == "dir") "📁" else "📄"
            Text(text = icon)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = file.name ?: file.path ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    color = HermesColors.OnSurface
                )
                file.size?.let { size ->
                    Text(
                        text = formatSize(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = HermesColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
