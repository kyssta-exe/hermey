package com.kyssta.hermey.ui.screens.insights

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.InsightsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen() {
    val insightsVm: InsightsViewModel = viewModel()
    val insights by insightsVm.insights.collectAsState()
    val loading by insightsVm.loading.collectAsState()

    LaunchedEffect(Unit) { insightsVm.loadInsights(days = 30) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
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
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InsightCard(title = "Sessions (last 30 days)", value = (insights?.sessions ?: 0).toString())
                InsightCard(title = "Messages (last 30 days)", value = (insights?.messages ?: 0).toString())
            }
        }
    }
}

@Composable
fun InsightCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = HermesColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = HermesColors.Primary)
        }
    }
}
