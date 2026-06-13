package com.example.todoapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.todoapp.ui.TaskViewModel
import com.example.todoapp.ui.screens.AddTaskScreen
import com.example.todoapp.ui.screens.SettingsScreen
import com.example.todoapp.ui.screens.TasksScreen

data class BottomNavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun Navigation(viewModel: TaskViewModel) {
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val items = listOf(
        BottomNavigationItem(
            route = Screen.TasksScreen.route,
            title = "Tasks",
            selectedIcon = Icons.Filled.List,
            unselectedIcon = Icons.Outlined.List
        ),
        BottomNavigationItem(
            route = Screen.AddTaskScreen.route,
            title = "Add task",
            selectedIcon = Icons.Filled.AddCircle,
            unselectedIcon = Icons.Outlined.AddCircle
        ),
        BottomNavigationItem(
            route = Screen.SettingsScreen.route,
            title = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Row(modifier = Modifier.fillMaxSize()) {
        if (isTablet) {
            NavigationRail(
                modifier = Modifier.padding(top = 8.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                items.forEach { item ->
                    NavigationRailItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(text = item.title) },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        }
                    )
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (!isTablet) {
                    NavigationBar {
                        items.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                label = { Text(text = item.title) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.TasksScreen.route,
                    modifier = Modifier
                        .widthIn(max = 800.dp)
                        .fillMaxSize()
                        .imePadding()
                ) {
                    composable(route = Screen.TasksScreen.route) {
                        TasksScreen(
                            viewModel = viewModel,
                            onEditClick = { taskId ->
                                navController.navigate("edit_task/$taskId")
                            }
                        )
                    }

                    composable(route = Screen.SettingsScreen.route) {
                        SettingsScreen(
                            selectedTheme = viewModel.appTheme,
                            onThemeSelected = { newTheme -> viewModel.updateTheme(newTheme) },
                            notificationsEnabled = viewModel.notificationsEnabled,
                            onNotificationsToggled = { viewModel.toggleNotifications(it) },
                            notifyBefore = viewModel.notifyBefore,
                            onNotifyBeforeToggled = { viewModel.toggleNotifyBefore(it) },
                            notifyBeforeTime = viewModel.notifyBeforeTime,
                            onNotifyBeforeTimeSelected = { viewModel.updateNotifyBeforeTime(it) },
                            locationNotificationsEnabled = viewModel.locationNotificationsEnabled,
                            onLocationNotificationsToggled = { viewModel.toggleLocationNotifications(it) },
                            keepAwake = viewModel.keepAwake,
                            onKeepAwakeToggled = { viewModel.toggleKeepAwake(it) },
                            startOnBoot = viewModel.startOnBoot,
                            onStartOnBootToggled = { viewModel.toggleStartOnBoot(it) }
                        )
                    }

                    composable(route = Screen.AddTaskScreen.route) {
                        AddTaskScreen(
                            onSaveClick = { _, title, description, date, time, priority, isRecurring, recurrenceType, customInterval, customUnit, locations, attachments, category ->
                                viewModel.addTask(
                                    title = title,
                                    description = description,
                                    date = date,
                                    time = time,
                                    priority = priority,
                                    isRecurring = isRecurring,
                                    recurrenceType = recurrenceType,
                                    customInterval = customInterval,
                                    customUnit = customUnit,
                                    locations = locations,
                                    attachments = attachments,
                                    category = category
                                )
                                navController.navigate(Screen.TasksScreen.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                    composable(route = "edit_task/{taskId}") { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")?.toLongOrNull()
                        val tasks by viewModel.tasksState.collectAsState()

                        val taskToEdit = tasks.find { it.id == taskId }

                        if (taskToEdit != null) {
                            if (taskToEdit.isCompleted) {
                                androidx.compose.runtime.LaunchedEffect(Unit) {
                                    navController.popBackStack()
                                }
                            } else {
                                AddTaskScreen(
                                    taskToEdit = taskToEdit,
                                    onSaveClick = { id, title, description, date, time, priority, isRecurring, recurrenceType, customInterval, customUnit, locations, attachments, category ->
                                        if (id != null) {
                                            viewModel.updateTaskDetails(id, title, description, date, time, priority, isRecurring, recurrenceType, customInterval, customUnit, locations, attachments, category)
                                        }
                                        navController.popBackStack()
                                    },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}