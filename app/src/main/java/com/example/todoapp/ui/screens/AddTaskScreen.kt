package com.example.todoapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.AttachmentData
import com.example.todoapp.data.LocationData
import com.example.todoapp.data.Priority
import com.example.todoapp.data.TaskEntity
import com.example.todoapp.ui.TaskViewModel
import com.example.todoapp.ui.components.DoubleTextSection
import com.example.todoapp.ui.components.MapSelectionDialog
import com.example.todoapp.ui.components.TextSection
import com.example.todoapp.ui.components.WheelPicker
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    taskToEdit: TaskEntity? = null,
    onSaveClick: (
        id: Long?,
        title: String,
        description: String,
        date: String,
        time: String,
        priority: Priority,
        isRecurring: Boolean,
        recurrenceType: String,
        customInterval: Int,
        customUnit: String,
        locations: List<LocationData>,
        attachments: List<AttachmentData>,
        category: String
    ) -> Unit,
    viewModel: TaskViewModel
) {
    val scrollState = rememberScrollState()

    val gson = remember { Gson() }

    val locationsSaver = Saver<List<LocationData>, String>(
        save = { gson.toJson(it) },
        restore = { gson.fromJson(it, Array<LocationData>::class.java).toList() }
    )

    val attachmentsSaver = Saver<List<AttachmentData>, String>(
        save = { gson.toJson(it) },
        restore = { gson.fromJson(it, Array<AttachmentData>::class.java).toList() }
    )

    var isRecurring by rememberSaveable { mutableStateOf(taskToEdit?.isRecurring ?: false) }
    var selectedRecurrenceOption by rememberSaveable { mutableStateOf(taskToEdit?.recurrenceType ?: "Daily") }
    var customInterval by rememberSaveable { mutableStateOf(taskToEdit?.customRecurrenceInterval?.toString() ?: "1") }
    var customUnit by rememberSaveable { mutableStateOf(taskToEdit?.customRecurrenceUnit ?: "Days") }

    var title by rememberSaveable { mutableStateOf(taskToEdit?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedPriority by rememberSaveable { mutableStateOf(taskToEdit?.priority ?: Priority.MEDIUM) }

    val defaultCalendar = rememberSaveable {
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

    var selectedDateMillis by rememberSaveable {
        mutableStateOf(
            if (taskToEdit?.date?.isNotBlank() == true) {
                try {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(taskToEdit.date)?.time
                } catch (e: Exception) { defaultCalendar.timeInMillis }
            } else defaultCalendar.timeInMillis
        )
    }

    var selectedTime by rememberSaveable {
        mutableStateOf(
            if (taskToEdit?.time?.isNotBlank() == true) {
                try {
                    val parts = taskToEdit.time.split(":")
                    Pair(parts[0].toInt(), parts[1].toInt())
                } catch (e: Exception) { Pair(15, 0) }
            } else Pair(15, 0)
        )
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    var showMapDialog by rememberSaveable { mutableStateOf(false) }

    var selectedLocations by rememberSaveable(stateSaver = locationsSaver) {
        mutableStateOf(
            if (!taskToEdit?.locationsJson.isNullOrEmpty() && taskToEdit?.locationsJson != "[]") {
                gson.fromJson(taskToEdit.locationsJson, Array<LocationData>::class.java).toList()
            } else emptyList()
        )
    }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen) {
            focusManager.clearFocus()
        }
    }

    var selectedAttachments by rememberSaveable(stateSaver = attachmentsSaver) {
        mutableStateOf(
            if (!taskToEdit?.attachmentsJson.isNullOrEmpty() && taskToEdit?.attachmentsJson != "[]") {
                gson.fromJson(taskToEdit.attachmentsJson, Array<AttachmentData>::class.java).toList()
            } else emptyList()
        )
    }

    var selectedCategory by rememberSaveable {
        mutableStateOf(taskToEdit?.category ?: viewModel.categoriesList.firstOrNull() ?: "Default")
    }
    var showAddCategoryField by rememberSaveable { mutableStateOf(false) }
    var newCategoryText by rememberSaveable { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val remainingSlots = 10 - selectedAttachments.size

        if (uris.size > remainingSlots) {
            Toast.makeText(context, "Attachment limit reached (Max 10)", Toast.LENGTH_SHORT).show()
        }

        val filesToAdd = uris.take(remainingSlots)

        val newAttachments = filesToAdd.map { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

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
                        text = if (taskToEdit == null) "Add New Task" else "Edit Task",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    if (taskToEdit == null) {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                isRecurring = false
                                selectedRecurrenceOption = "Daily"
                                customInterval = "1"
                                customUnit = "Days"
                                title = ""
                                description = ""
                                selectedPriority = Priority.MEDIUM
                                selectedDateMillis = defaultCalendar.timeInMillis
                                selectedTime = Pair(15, 0)
                                selectedLocations = emptyList()
                                selectedAttachments = emptyList()
                                selectedCategory = viewModel.categoriesList.firstOrNull() ?: "Default"
                                showAddCategoryField = false
                                newCategoryText = ""
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                val dateString = selectedDateMillis?.let {
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                                } ?: ""

                                val timeString = selectedTime?.let {
                                    String.format(Locale.getDefault(), "%02d:%02d", it.first, it.second)
                                } ?: ""

                                onSaveClick(
                                    taskToEdit?.id,
                                    title,
                                    description,
                                    dateString,
                                    timeString,
                                    selectedPriority,
                                    isRecurring,
                                    selectedRecurrenceOption,
                                    customInterval.toIntOrNull() ?: 1,
                                    customUnit,
                                    selectedLocations,
                                    selectedAttachments,
                                    selectedCategory
                                )
                            } else {
                                Toast.makeText(context, "Task title is required", Toast.LENGTH_SHORT).show()
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
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextSection(title = "Task Type") {
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
                TextSection("Recurrence") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    val initialNumberIndex = numbers.indexOf(customInterval).coerceAtLeast(0)

                                    WheelPicker(
                                        items = numbers,
                                        modifier = Modifier.width(50.dp),
                                        initialIndex = initialNumberIndex,
                                        onItemSelected = { customInterval = it }
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    val units = listOf("Days", "Weeks", "Months", "Years")
                                    val initialUnitIndex = units.indexOf(customUnit).coerceAtLeast(0)

                                    WheelPicker(
                                        items = units,
                                        modifier = Modifier.width(100.dp),
                                        initialIndex = initialUnitIndex,
                                        onItemSelected = { customUnit = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            TextSection("Title") {
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

            TextSection("Description") {
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

            TextSection("Category") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(viewModel.categoriesList) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    selectedCategory = cat
                                    showAddCategoryField = false
                                },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                trailingIcon = if (!viewModel.isDefaultCategory(cat)) {
                                    {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCategory(cat)
                                                if (selectedCategory == cat) {
                                                    selectedCategory = viewModel.categoriesList.firstOrNull() ?: "Default"
                                                }
                                            },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete category",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }

                        item {
                            FilterChip(
                                selected = showAddCategoryField,
                                onClick = { showAddCategoryField = !showAddCategoryField },
                                label = { Text("+ Add Custom") },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }

                    if (showAddCategoryField) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = newCategoryText,
                                onValueChange = { newCategoryText = it },
                                placeholder = { Text("New category name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            Button(
                                onClick = {
                                    if (newCategoryText.isNotBlank()) {
                                        val trimmed = newCategoryText.trim()
                                        viewModel.addCategory(trimmed)
                                        selectedCategory = trimmed
                                        newCategoryText = ""
                                        showAddCategoryField = false
                                    } else {
                                        Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            DoubleTextSection(
                titleLeft = "Date",
                titleRight = "Time",
                contentLeft = {
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
                },
                contentRight = {
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
            )

            TextSection("Priority") {
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

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            DoubleTextSection(
                titleLeft = "Locations",
                titleRight = "Attachments",
                contentLeft = {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            showMapDialog = true
                        },
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
                },
                contentRight = {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            launcher.launch(arrayOf("*/*"))
                        },
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
            )

            if (selectedLocations.isNotEmpty()) {
                TextSection("Added locations (${selectedLocations.size}/5):") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        items(selectedLocations) { loc ->
                            InputChip(
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
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
                                        onClick = {
                                            selectedLocations = selectedLocations - loc
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (selectedAttachments.isNotEmpty()) {
                TextSection("Added attachments (${selectedAttachments.size}/10):") {
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

            Spacer(modifier = Modifier.height(8.dp))
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
                    Toast.makeText(context, "Location limit reached (Max 5)", Toast.LENGTH_SHORT).show()
                }
                showMapDialog = false
            }
        )
    }
}
