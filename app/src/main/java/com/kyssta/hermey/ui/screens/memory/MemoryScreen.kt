package com.kyssta.hermey.ui.screens.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.networking.MemoryEntry
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.MemoryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen() {
    val memoryVm: MemoryViewModel = viewModel()
    val facts by memoryVm.facts.collectAsState()
    val loading by memoryVm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { memoryVm.loadFacts() }
    }

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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (facts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No memory facts stored", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(facts) { fact ->
                    FactCard(fact = fact)
                }
            }
        }
    }
}

@Composable
fun FactCard(fact: MemoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = fact.entity ?: "General",
                style = MaterialTheme.typography.labelSmall,
                color = HermesColors.Primary
            )
            Text(
                text = fact.content ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = HermesColors.OnSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                fact.category?.let { cat ->
                    Text(text = cat, style = MaterialTheme.typography.labelSmall, color = HermesColors.OnSurfaceVariant)
                }
                fact.trust?.let { trust ->
                    Text(
                        text = "Trust: %.1f".format(trust),
                        style = MaterialTheme.typography.labelSmall,
                        color = HermesColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}
