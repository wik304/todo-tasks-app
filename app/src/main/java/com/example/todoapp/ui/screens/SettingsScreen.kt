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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todoapp.ui.components.SwitchSettingItem
import com.example.todoapp.ui.components.TextSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggled: (Boolean) -> Unit,
    notifyBefore: Boolean,
    onNotifyBeforeToggled: (Boolean) -> Unit,
    notifyBeforeTime: String,
    onNotifyBeforeTimeSelected: (String) -> Unit,
    locationNotificationsEnabled: Boolean,
    onLocationNotificationsToggled: (Boolean) -> Unit,
    keepAwake: Boolean,
    onKeepAwakeToggled: (Boolean) -> Unit,
    startOnBoot: Boolean,
    onStartOnBootToggled: (Boolean) -> Unit
) {
    var notifyOnTime by remember { mutableStateOf(true) }

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
            TextSection(title = "App Theme") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val themes = listOf("System", "Light", "Dark")
                    themes.forEachIndexed { index, theme ->
                        val shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            themes.lastIndex -> RoundedCornerShape(
                                topEnd = 12.dp,
                                bottomEnd = 12.dp
                            )

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

            TextSection(title = "App Behavior") {
                SwitchSettingItem(
                    icon = Icons.Default.Smartphone,
                    title = "Keep screen awake",
                    subtitle = "Prevent screen from turning off while using the app",
                    checked = keepAwake,
                    onCheckedChange = { onKeepAwakeToggled(it) }
                )
                SwitchSettingItem(
                    icon = Icons.Default.RocketLaunch,
                    title = "Start on boot",
                    subtitle = "Initialize background tasks when device starts",
                    checked = startOnBoot,
                    onCheckedChange = { onStartOnBootToggled(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            TextSection(title = "Notifications") {
                SwitchSettingItem(
                    icon = Icons.Default.Notifications,
                    title = "Enable notifications",
                    subtitle = "Receive alerts for your upcoming tasks",
                    checked = notificationsEnabled,
                    onCheckedChange = { onNotificationsToggled(it) }
                )

                if (notificationsEnabled) {
                    SwitchSettingItem(
                        icon = Icons.Default.AlarmOn,
                        title = "Notify exactly on time",
                        subtitle = "Alert me at the exact task time",
                        checked = notifyOnTime,
                        onCheckedChange = { notifyOnTime = it }
                    )

                    SwitchSettingItem(
                        icon = Icons.Default.Timer,
                        title = "Notify beforehand",
                        subtitle = "Receive an additional early reminder",
                        checked = notifyBefore,
                        onCheckedChange = { onNotifyBeforeToggled(it) }
                    )

                    if (notifyBefore) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 0.dp, vertical = 8.dp)
                        ) {
                            val timeOptions = listOf("5 min", "15 min", "1 h", "1 day")
                            timeOptions.forEachIndexed { index, time ->
                                val shape = when (index) {
                                    0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                    timeOptions.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                    else -> RectangleShape
                                }

                                FilterChip(
                                    selected = notifyBeforeTime == time,
                                    onClick = { onNotifyBeforeTimeSelected(time) },
                                    label = {
                                        Text(
                                            text = time,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
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

                    SwitchSettingItem(
                        icon = Icons.Default.LocationOn,
                        title = "Location notifications",
                        subtitle = "Notify me when I arrive at a task's location",
                        checked = locationNotificationsEnabled,
                        onCheckedChange = { onLocationNotificationsToggled(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}