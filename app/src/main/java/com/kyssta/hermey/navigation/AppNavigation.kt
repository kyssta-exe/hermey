package com.kyssta.hermey.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import com.kyssta.hermey.auth.AuthState
import com.kyssta.hermey.ui.HermesColors
import com.kyssta.hermey.ui.screens.*
import com.kyssta.hermey.ui.screens.chat.ChatScreen
import com.kyssta.hermey.ui.screens.insights.InsightsScreen
import com.kyssta.hermey.ui.screens.kanban.KanbanScreen
import com.kyssta.hermey.ui.screens.memory.MemoryScreen
import com.kyssta.hermey.ui.screens.sessions.SessionsScreen
import com.kyssta.hermey.ui.screens.settings.SettingsScreen
import com.kyssta.hermey.ui.screens.skills.SkillsScreen
import com.kyssta.hermey.ui.screens.tasks.TasksScreen
import com.kyssta.hermey.ui.screens.workspace.WorkspaceScreen
import com.kyssta.hermey.ui.viewmodels.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Sessions, "Sessions", androidx.compose.material.icons.Icons.Filled.Chat),
    BottomNavItem(Screen.Tasks, "Tasks", androidx.compose.material.icons.Icons.Filled.Schedule),
    BottomNavItem(Screen.Skills, "Skills", androidx.compose.material.icons.Icons.Filled.MenuBook),
    BottomNavItem(Screen.Settings, "Settings", androidx.compose.material.icons.Icons.Filled.Settings),
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val authState by authVm.state.collectAsState()

    when (authState) {
        AuthState.Unconfigured -> {
            OnboardingScreen(
                onLoggedIn = {
                    navController.navigate(Screen.Sessions.route) {
                        popUpTo(0)
                    }
                },
                onError = {}
            )
        }
        AuthState.LoggedIn, AuthState.LoggedOut -> {
            MainNavigation(navController = navController)
        }
    }
}

@Composable
fun MainNavigation(navController: NavHostController) {
    var currentRoute by remember { mutableStateOf(Screen.Sessions.route) }

    NavHost(
        navController = navController,
        startDestination = Screen.Sessions.route
    ) {
        composable(Screen.Sessions.route) {
            SessionsScreen(
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.Chat.createRoute(sessionId))
                },
                onNewChat = {
                    navController.navigate(Screen.Chat.createRoute(""))
                }
            )
        }
        composable("${Screen.Chat.route}/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Tasks.route) { TasksScreen() }
        composable(Screen.Skills.route) { SkillsScreen() }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                onNavigateToMemory = { navController.navigate(Screen.Memory.route) }
            )
        }
        composable(Screen.Insights.route) { InsightsScreen() }
        composable(Screen.Memory.route) { MemoryScreen() }
        composable(Screen.Kanban.route) { KanbanScreen() }
    }

    // Bottom navigation
    NavigationBar(
        modifier = Modifier.padding(bottom = 8.dp),
        containerColor = HermesColors.Surface
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.screen.route,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                        currentRoute = item.screen.route
                    }
                }
            )
        }
    }
}
