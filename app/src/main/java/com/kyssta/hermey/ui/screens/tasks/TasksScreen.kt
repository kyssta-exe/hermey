package com.kyssta.hermey.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.networking.CronJob
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.TasksViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val tasksVm: TasksViewModel = viewModel()
    val jobs by tasksVm.jobs.collectAsState()
    val loading by tasksVm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { tasksVm.loadCrons() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
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
        } else if (jobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scheduled tasks", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(jobs) { job ->
                    TaskCard(job = job)
                }
            }
        }
    }
}

@Composable
fun TaskCard(job: CronJob) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.name ?: "Untitled Task",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = HermesColors.OnSurface
            )
            Text(
                text = job.prompt ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = HermesColors.OnSurfaceVariant,
                maxLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Schedule: ${job.schedule ?: "manual"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = HermesColors.OnSurfaceVariant
                )
                Text(
                    text = if (job.enabled == true) "Active" else "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (job.enabled == true) HermesColors.Success else HermesColors.Outline
                )
            }
        }
    }
}
