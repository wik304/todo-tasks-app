package com.example.todoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit
) {
    var keepAwake by remember { mutableStateOf(false) }
    var startOnBoot by remember { mutableStateOf(true) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrateEnabled by remember { mutableStateOf(false) }

    var use24HourFormat by remember { mutableStateOf(true) }
    var startWeekOnMonday by remember { mutableStateOf(true) }

    var swipeToCompleteEnabled by remember { mutableStateOf(true) }
    var swipeToDeleteEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = "App Theme") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val themes = listOf("System", "Light", "Dark")
                    themes.forEachIndexed { index, theme ->
                        val shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            themes.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            else -> RectangleShape
                        }

                        FilterChip(
                            selected = selectedTheme == theme,
                            onClick = { onThemeSelected(theme) },
                            label = {
                                Text(
                                    text = theme,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = shape,
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsSection(title = "App Behavior") {
                SwitchSettingItem(
                    icon = Icons.Default.Smartphone,
                    title = "Keep screen awake",
                    subtitle = "Prevent screen from turning off while using the app",
                    checked = keepAwake,
                    onCheckedChange = { keepAwake = it }
                )
                SwitchSettingItem(
                    icon = Icons.Default.RocketLaunch,
                    title = "Start on boot",
                    subtitle = "Initialize background tasks when device starts",
                    checked = startOnBoot,
                    onCheckedChange = { startOnBoot = it }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsSection(title = "Task Gestures") {
                SwitchSettingItem(
                    icon = Icons.Default.Check,
                    title = "Swipe to complete",
                    subtitle = "Allow completing tasks by swiping right",
                    checked = swipeToCompleteEnabled,
                    onCheckedChange = { swipeToCompleteEnabled = it }
                )
                SwitchSettingItem(
                    icon = Icons.Default.Delete,
                    title = "Swipe to delete",
                    subtitle = "Allow deleting tasks by swiping left",
                    checked = swipeToDeleteEnabled,
                    onCheckedChange = { swipeToDeleteEnabled = it }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsSection(title = "Notifications") {
                SwitchSettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Enable notifications",
                    subtitle = "Receive alerts for your upcoming tasks",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )

                if (notificationsEnabled) {
                    SwitchSettingItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Play sound",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                    SwitchSettingItem(
                        icon = Icons.Default.Vibration,
                        title = "Vibrate",
                        checked = vibrateEnabled,
                        onCheckedChange = { vibrateEnabled = it }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsSection(title = "User Preferences") {
                SwitchSettingItem(
                    icon = Icons.Default.Schedule,
                    title = "Use 24-hour format",
                    checked = use24HourFormat,
                    onCheckedChange = { use24HourFormat = it }
                )
                SwitchSettingItem(
                    icon = Icons.Default.CalendarToday,
                    title = "Start week on Monday",
                    checked = startWeekOnMonday,
                    onCheckedChange = { startWeekOnMonday = it }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        content()
    }
}

@Composable
fun SwitchSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}