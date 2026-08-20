package com.kyssta.hermey.ui.screens.insights

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.InsightsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen() {
    val insightsVm: InsightsViewModel = viewModel()
    val insights by insightsVm.insights.collectAsState()
    val loading by insightsVm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { insightsVm.loadInsights(days = 30) }
    }

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                insights?.totalSessions?.let { sessions ->
                    InsightCard(title = "Total Sessions", value = sessions.toString())
                }
                insights?.totalMessages?.let { msgs ->
                    InsightCard(title = "Total Messages", value = msgs.toString())
                }
                insights?.totalTurns?.let { turns ->
                    InsightCard(title = "Total Turns", value = turns.toString())
                }
                insights?.avgTokensPerTurn?.let { tps ->
                    InsightCard(title = "Avg Tokens/Turn", value = "%.0f".format(tps))
                }
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = HermesColors.OnSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = HermesColors.Primary)
        }
    }
}
