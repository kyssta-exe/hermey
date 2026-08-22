package com.kyssta.hermey.ui.screens.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.MemoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen() {
    val memoryVm: MemoryViewModel = viewModel()
    val memory by memoryVm.memory.collectAsState()
    val loading by memoryVm.loading.collectAsState()
    val error by memoryVm.error.collectAsState()

    LaunchedEffect(Unit) { memoryVm.loadMemory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HermesColors.Surface,
                    titleContentColor = HermesColors.OnSurface
                )
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                error?.let {
                    item { Text(it, color = HermesColors.Error, style = MaterialTheme.typography.bodySmall) }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Active provider", style = MaterialTheme.typography.labelSmall, color = HermesColors.Primary)
                            Text(
                                text = memory?.active?.ifBlank { null } ?: "none",
                                style = MaterialTheme.typography.titleMedium,
                                color = HermesColors.OnSurface
                            )
                        }
                    }
                }
                val files = memory?.builtinFiles.orEmpty()
                if (files.isEmpty()) {
                    item {
                        Text("No built-in memory files yet — they grow as the agent remembers.", color = HermesColors.OnSurfaceVariant)
                    }
                } else {
                    items(files.size) { i ->
                        val (name, size) = files.entries.sortedBy { it.key }[i]
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, color = HermesColors.OnSurface)
                                Text("${size / 1024} KB", style = MaterialTheme.typography.bodySmall, color = HermesColors.OnSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
