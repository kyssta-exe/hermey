package com.kyssta.hermey.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    object Sessions : Screen("sessions")
    object Tasks : Screen("tasks")
    object Skills : Screen("skills")
    object Workspace : Screen("workspace/{sessionId}") {
        fun createRoute(sessionId: String) = "workspace/$sessionId"
    }
    object Kanban : Screen("kanban")
    object Memory : Screen("memory")
    object Insights : Screen("insights")
    object Settings : Screen("settings")
}
