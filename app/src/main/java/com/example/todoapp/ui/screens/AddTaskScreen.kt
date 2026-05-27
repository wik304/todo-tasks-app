package com.example.todoapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.LocationData
import com.example.todoapp.data.Priority
import com.example.todoapp.ui.components.MapSelectionDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.net.Uri
import androidx.compose.ui.text.style.TextOverflow
import com.example.todoapp.data.AttachmentData
import com.example.todoapp.ui.TaskViewModel
import com.example.todoapp.ui.components.WheelPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onSaveClick: (String, String, Priority, List<LocationData>, List<AttachmentData>) -> Unit,
    viewModel: TaskViewModel
) {
    var isRecurring by remember { mutableStateOf(false) }
    var selectedRecurrenceOption by remember { mutableStateOf("Daily") }

    var customInterval by remember { mutableStateOf("1") }
    var customUnit by remember { mutableStateOf("Days") }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }

    val defaultCalendar = remember {
        Calendar.getInstance().apply {
            val now = Calendar.getInstance()
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (now.after(this)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    var selectedDateMillis by remember { mutableStateOf<Long?>(defaultCalendar.timeInMillis) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(Pair(15, 0)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    var showMapDialog by remember { mutableStateOf(false) }
    var selectedLocations by remember { mutableStateOf(listOf<LocationData>()) }

    val context = LocalContext.current
    var selectedAttachments by remember { mutableStateOf(listOf<AttachmentData>()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val remainingSlots = 10 - selectedAttachments.size
        val filesToAdd = uris.take(remainingSlots)

        val newAttachments = filesToAdd.map { uri ->
            val fileName = viewModel.getFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val type = when {
                mimeType.startsWith("image") -> "IMAGE"
                mimeType.startsWith("application/pdf") -> "PDF"
                mimeType.startsWith("audio") -> "AUDIO"
                else -> "FILE"
            }
            AttachmentData(uri.toString(), fileName, type)
        }
        selectedAttachments = selectedAttachments + newAttachments
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Add New Task",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            title = ""
                            description = ""
                            selectedPriority = Priority.MEDIUM
                            selectedDateMillis = defaultCalendar.timeInMillis
                            selectedTime = Pair(15, 0)
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSaveClick(title, description, selectedPriority, selectedLocations, selectedAttachments)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp, 0.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Task Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = !isRecurring,
                        onClick = { isRecurring = false },
                        label = {
                            Text(
                                text = "One-time task",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    FilterChip(
                        selected = isRecurring,
                        onClick = { isRecurring = true },
                        label = {
                            Text(
                                text = "Recurring task",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
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

            if (isRecurring) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Recurrence", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val recurrenceOptions = listOf("Daily", "Weekly", "Monthly", "Custom")
                        recurrenceOptions.forEachIndexed { index, option ->
                            val shape = when (index) {
                                0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                recurrenceOptions.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                else -> RectangleShape
                            }

                            FilterChip(
                                selected = selectedRecurrenceOption == option,
                                onClick = { selectedRecurrenceOption = option },
                                label = {
                                    Text(
                                        text = option,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall
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

                    if (selectedRecurrenceOption == "Custom") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Repeat every:", style = MaterialTheme.typography.titleMedium)

                                Spacer(modifier = Modifier.width(24.dp))

                                val numbers = (1..99).map { it.toString() }
                                WheelPicker(
                                    items = numbers,
                                    modifier = Modifier.width(50.dp),
                                    onItemSelected = { customInterval = it }
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                val units = listOf("Days", "Weeks", "Months", "Years")
                                WheelPicker(
                                    items = units,
                                    modifier = Modifier.width(100.dp),
                                    onItemSelected = { customUnit = it }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Title", style = MaterialTheme.typography.labelLarge)
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Enter the title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Description", style = MaterialTheme.typography.labelLarge)
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Enter the description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Date", style = MaterialTheme.typography.labelLarge)
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dateText = selectedDateMillis?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                            } ?: "Date"
                            Text(dateText)
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Time", style = MaterialTheme.typography.labelLarge)
                    Button(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val timeText = selectedTime?.let {
                                String.format(Locale.getDefault(), "%02d:%02d", it.first, it.second)
                            } ?: "Time"
                            Text(timeText)
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val priorities = Priority.entries
                    priorities.forEachIndexed { index, priority ->
                        val shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            priorities.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            else -> RectangleShape
                        }

                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = {
                                Text(
                                    text = priority.name.lowercase().replaceFirstChar { it.uppercase() },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Locations", style = MaterialTheme.typography.labelLarge)
                    Button(
                        onClick = { showMapDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Add location")
                            Icon(Icons.Default.AddLocation, contentDescription = null)
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Attachments", style = MaterialTheme.typography.labelLarge)
                    Button(
                        onClick = { launcher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Attach file")
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                        }
                    }
                }
            }

            if (selectedLocations.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Added locations (${selectedLocations.size}/5):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        items(selectedLocations) { loc ->
                            InputChip(
                                selected = true,
                                onClick = {},
                                label = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = loc.name,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { selectedLocations = selectedLocations - loc },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (selectedAttachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Added attachments (${selectedAttachments.size}/10):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedAttachments) { file ->
                            Card(
                                modifier = Modifier.size(70.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = when(file.type) {
                                            "IMAGE" -> Icons.Default.Image
                                            "PDF" -> Icons.Default.PictureAsPdf
                                            "AUDIO" -> Icons.Default.Mic
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp).align(Alignment.Center)
                                    )

                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp)
                                    )

                                    IconButton(
                                        onClick = { selectedAttachments = selectedAttachments - file },
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val initialDate = remember(selectedDateMillis) {
            val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = selectedDateMillis ?: System.currentTimeMillis()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime?.first ?: 15,
            initialMinute = selectedTime?.second ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = Pair(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    if (showMapDialog) {
        MapSelectionDialog(
            onDismiss = { showMapDialog = false },
            onLocationSelected = { lat, lng, radius, addressName ->
                if (selectedLocations.size < 5) {
                    val newLoc = LocationData(lat, lng, radius, addressName)
                    selectedLocations = selectedLocations + newLoc
                } else {
                    // Opcjonalnie: Pokaż Snackbar "Limit lokalizacji osiągnięty"
                }
                showMapDialog = false
            }
        )
    }
}