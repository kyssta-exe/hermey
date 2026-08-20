package com.kyssta.hermey.ui.screens.skills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyssta.hermey.networking.SkillInfo
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.viewmodels.SkillsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen() {
    val skillsVm: SkillsViewModel = viewModel()
    val skills by skillsVm.skills.collectAsState()
    val loading by skillsVm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch { skillsVm.loadSkills() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skills") },
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
        } else if (skills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No skills installed", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(skills) { skill ->
                    SkillCard(skill = skill)
                }
            }
        }
    }
}

@Composable
fun SkillCard(skill: SkillInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HermesColors.Glass)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = skill.name ?: "Unknown Skill",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = HermesColors.OnSurface
            )
            skill.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = HermesColors.OnSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                skill.category?.let { cat ->
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelSmall,
                        color = HermesColors.Primary
                    )
                }
                Text(
                    text = if (skill.enabled == true) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (skill.enabled == true) HermesColors.Success else HermesColors.Outline
                )
            }
        }
    }
}
