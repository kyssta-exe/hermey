package com.kyssta.hermey.ui.screens.kanban

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.KanbanViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanbanScreen() {
    val kanbanVm: KanbanViewModel = viewModel()
    val boards by kanbanVm.boards.collectAsState()
    val loading by kanbanVm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { kanbanVm.loadBoards() }
    }

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
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (boards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No boards yet", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                boards.forEach { board ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = board.name ?: board.slug ?: "Untitled",
                                style = MaterialTheme.typography.titleMedium,
                                color = HermesColors.OnSurface
                            )
                            Text(
                                text = "${board.columns?.size ?: 0} columns",
                                style = MaterialTheme.typography.bodySmall,
                                color = HermesColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
