package com.example.todoapp.ui.navigation

sealed class Screen(val route: String) {
    object TasksScreen : Screen("tasks_screen")
    object AddTaskScreen : Screen("add_task_screen")
    object SettingsScreen : Screen("settings_screen")
}