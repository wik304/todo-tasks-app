package com.example.todoapp.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.AttachmentData
import com.example.todoapp.data.LocationData
import com.example.todoapp.data.Priority
import com.example.todoapp.data.RecurrenceHelper
import com.example.todoapp.data.TaskDao
import com.example.todoapp.data.TaskEntity
import com.example.todoapp.receiver.GeofenceManager
import com.example.todoapp.receiver.NotificationScheduler
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.io.File
import java.security.MessageDigest
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class TaskFilter { ALL, TODAY, OVERDUE }
enum class TaskSort { DATE, PRIORITY }

class TaskViewModel(application: Application, private val taskDao: TaskDao) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("todo_settings", Context.MODE_PRIVATE)

    var notificationsEnabled by mutableStateOf(sharedPrefs.getBoolean("notifications_enabled", true))
        private set

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        sharedPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    var keepAwake by mutableStateOf(sharedPrefs.getBoolean("keep_awake", false))
        private set

    fun toggleKeepAwake(enabled: Boolean) {
        keepAwake = enabled
        sharedPrefs.edit().putBoolean("keep_awake", enabled).apply()
    }

    var startOnBoot by mutableStateOf(sharedPrefs.getBoolean("start_on_boot", true))
        private set

    fun toggleStartOnBoot(enabled: Boolean) {
        startOnBoot = enabled
        sharedPrefs.edit().putBoolean("start_on_boot", enabled).apply()
    }

    var notifyBefore by mutableStateOf(sharedPrefs.getBoolean("notify_before", false))
        private set

    var notifyBeforeTime by mutableStateOf(sharedPrefs.getString("notify_before_time", "15 min") ?: "15 min")
        private set

    fun toggleNotifyBefore(enabled: Boolean) {
        notifyBefore = enabled
        sharedPrefs.edit().putBoolean("notify_before", enabled).apply()
    }

    fun updateNotifyBeforeTime(time: String) {
        notifyBeforeTime = time
        sharedPrefs.edit().putString("notify_before_time", time).apply()
    }

    var locationNotificationsEnabled by mutableStateOf(sharedPrefs.getBoolean("location_notifications_enabled", true))
        private set

    fun toggleLocationNotifications(enabled: Boolean) {
        locationNotificationsEnabled = enabled
        sharedPrefs.edit().putBoolean("location_notifications_enabled", enabled).apply()
    }

    private val defaultCategories = listOf("Work", "Personal", "Shopping", "Health", "Education")

    var categoriesList by mutableStateOf(loadCategories())
        private set

    fun addCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isNotBlank() && !categoriesList.contains(trimmed)) {
            val updated = categoriesList + trimmed
            categoriesList = updated
            saveCategories(updated)
        }
    }

    private fun loadCategories(): List<String> {
        val json = sharedPrefs.getString("custom_categories", null) ?: return defaultCategories
        return try {
            val custom = Gson().fromJson(json, Array<String>::class.java).toList()
            (defaultCategories + custom).distinct()
        } catch (e: Exception) {
            defaultCategories
        }
    }

    private fun saveCategories(list: List<String>) {
        val custom = list.filter { !defaultCategories.contains(it) }
        sharedPrefs.edit().putString("custom_categories", Gson().toJson(custom)).apply()
    }

    fun isDefaultCategory(category: String): Boolean {
        return defaultCategories.contains(category)
    }

    fun deleteCategory(category: String) {
        if (!isDefaultCategory(category)) {
            val updated = categoriesList - category
            categoriesList = updated
            saveCategories(updated)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(TaskSort.DATE)
    val selectedSort = _selectedSort.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    fun onCategoryFilterChange(category: String) {
        _selectedCategoryFilter.value = category
    }

    var appTheme by mutableStateOf("System")
        private set

    fun updateTheme(newTheme: String) {
        appTheme = newTheme
    }

    val tasksState: StateFlow<List<TaskEntity>> = combine(
        taskDao.getAllTasks(),
        _searchQuery,
        _selectedFilter,
        _selectedSort,
        _selectedCategoryFilter
    ) { tasks, query, filter, sort, categoryFilter ->
        var result = tasks.filter {
            it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }

        val now = System.currentTimeMillis()
        result = when (filter) {
            TaskFilter.ALL -> result
            TaskFilter.TODAY -> result.filter { it.executeAt != null && isToday(it.executeAt) }
            TaskFilter.OVERDUE -> result.filter { it.executeAt != null && it.executeAt < now && !it.isCompleted }
        }

        if (categoryFilter != "All") {
            result = result.filter { it.category == categoryFilter }
        }

        when (sort) {
            TaskSort.DATE -> result.sortedBy { it.executeAt ?: Long.MAX_VALUE }
            TaskSort.PRIORITY -> result.sortedByDescending { it.priority.ordinal }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onFilterChange(filter: TaskFilter) { _selectedFilter.value = filter }
    fun onSortChange(sort: TaskSort) { _selectedSort.value = sort }

    fun addTask(
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
    ) {
        viewModelScope.launch {
            val gson = Gson()

            val processedAttachments = withContext(Dispatchers.IO) {
                attachments.map { copyAttachmentToInternal(it) }
            }

            var executeAtTimestamp: Long? = null
            if (date.isNotBlank() && time.isNotBlank()) {
                try {
                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val parsedDate = format.parse("$date $time")
                    executeAtTimestamp = parsedDate?.time
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val task = TaskEntity(
                title = title,
                description = description,
                date = date,
                time = time,
                createdAt = System.currentTimeMillis(),
                executeAt = executeAtTimestamp,
                isCompleted = false,
                category = category,
                priority = priority,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                customRecurrenceInterval = customInterval,
                customRecurrenceUnit = customUnit,
                locationsJson = if (locations.isNotEmpty()) gson.toJson(locations) else null,
                attachmentsJson = if (processedAttachments.isNotEmpty()) gson.toJson(processedAttachments) else null
            )
            val generatedId = taskDao.insertTask(task)
            val savedTask = task.copy(id = generatedId)
            NotificationScheduler.scheduleNotification(getApplication(), savedTask, delayMinutes = 0)
            GeofenceManager.addGeofencesForTask(getApplication(), savedTask)

            withContext(Dispatchers.IO) {
                cleanupOrphanedAttachments()
            }
        }
    }

    fun updateTaskDetails(
        id: Long,
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
    ) {
        viewModelScope.launch {
            val gson = Gson()

            val processedAttachments = withContext(Dispatchers.IO) {
                attachments.map { copyAttachmentToInternal(it) }
            }

            var executeAtTimestamp: Long? = null
            if (date.isNotBlank() && time.isNotBlank()) {
                try {
                    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val parsedDate = format.parse("$date $time")
                    executeAtTimestamp = parsedDate?.time
                } catch (e: Exception) { e.printStackTrace() }
            }

            val updatedTask = TaskEntity(
                id = id,
                title = title,
                description = description,
                date = date,
                time = time,
                createdAt = System.currentTimeMillis(),
                executeAt = executeAtTimestamp,
                isCompleted = false,
                category = category,
                priority = priority,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                customRecurrenceInterval = customInterval,
                customRecurrenceUnit = customUnit,
                locationsJson = if (locations.isNotEmpty()) gson.toJson(locations) else null,
                attachmentsJson = if (processedAttachments.isNotEmpty()) gson.toJson(processedAttachments) else null
            )
            taskDao.updateTask(updatedTask)

            NotificationScheduler.cancelNotification(getApplication(), updatedTask.id)
            NotificationScheduler.scheduleNotification(getApplication(), updatedTask, delayMinutes = 0)
            GeofenceManager.removeGeofencesForTask(getApplication(), updatedTask.id)
            GeofenceManager.addGeofencesForTask(getApplication(), updatedTask)

            withContext(Dispatchers.IO) {
                cleanupOrphanedAttachments()
            }
        }
    }

    fun markAsCompleted(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = true))

            NotificationScheduler.cancelNotification(getApplication(), task.id)
            GeofenceManager.removeGeofencesForTask(getApplication(), task.id)

            if (task.isRecurring && task.executeAt != null) {
                val nextExecuteAt = RecurrenceHelper.calculateNextOccurrence(
                    task.executeAt,
                    task.recurrenceType,
                    task.customRecurrenceInterval,
                    task.customRecurrenceUnit
                )

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val nextTask = task.copy(
                    id = 0,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    executeAt = nextExecuteAt,
                    date = dateFormat.format(Date(nextExecuteAt)),
                    time = timeFormat.format(Date(nextExecuteAt))
                )

                val nextId = taskDao.insertTask(nextTask)
                val savedNextTask = nextTask.copy(id = nextId)

                NotificationScheduler.scheduleNotification(getApplication(), savedNextTask)
                if (locationNotificationsEnabled) {
                    GeofenceManager.addGeofencesForTask(getApplication(), savedNextTask)
                }
            }
        }
    }

    private fun calculateNextOccurrence(
        currentTimestamp: Long,
        type: String,
        customInterval: Int,
        customUnit: String
    ): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        when (type) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "Monthly" -> calendar.add(Calendar.MONTH, 1)
            "Custom" -> {
                when (customUnit) {
                    "Days" -> calendar.add(Calendar.DAY_OF_YEAR, customInterval)
                    "Weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, customInterval)
                    "Months" -> calendar.add(Calendar.MONTH, customInterval)
                    "Years" -> calendar.add(Calendar.YEAR, customInterval)
                }
            }
        }
        return calendar.timeInMillis
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            NotificationScheduler.cancelNotification(getApplication(), task.id)
            GeofenceManager.removeGeofencesForTask(getApplication(), task.id)

            withContext(Dispatchers.IO) {
                cleanupOrphanedAttachments()
            }
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown_file"
        val cursor = context.contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }


    private fun copyAttachmentToInternal(attachment: AttachmentData): AttachmentData {
        val context = getApplication<Application>()
        val uri = Uri.parse(attachment.uriString)

        if (uri.authority == "com.example.todoapp.fileprovider") {
            return attachment
        }

        try {
            val contentResolver = context.contentResolver

            val hash = contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = inputStream.read(buffer)
                }
                val hashBytes = digest.digest()
                hashBytes.joinToString("") { "%02x".format(it) }
            } ?: return attachment

            val extension = getFileExtension(attachment.name)
            val filename = if (extension.isNotEmpty()) "$hash.$extension" else hash

            val attachmentsDir = File(context.filesDir, "attachments")
            if (!attachmentsDir.exists()) {
                attachmentsDir.mkdirs()
            }

            val targetFile = File(attachmentsDir, filename)

            if (!targetFile.exists()) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "com.example.todoapp.fileprovider",
                targetFile
            )

            return attachment.copy(uriString = fileProviderUri.toString())
        } catch (e: Exception) {
            e.printStackTrace()
            return attachment
        }
    }

    private fun getFileExtension(filename: String): String {
        val lastDotIndex = filename.lastIndexOf('.')
        return if (lastDotIndex != -1 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    private suspend fun cleanupOrphanedAttachments() {
        val allTasks = taskDao.getAllTasksList()
        val referencedFiles = mutableSetOf<String>()
        val gson = Gson()

        for (task in allTasks) {
            if (!task.attachmentsJson.isNullOrEmpty() && task.attachmentsJson != "[]") {
                try {
                    val attachments = gson.fromJson(task.attachmentsJson, Array<AttachmentData>::class.java)
                    for (att in attachments) {
                        val uri = Uri.parse(att.uriString)
                        if (uri.authority == "com.example.todoapp.fileprovider") {
                            val filename = uri.lastPathSegment
                            if (filename != null) {
                                referencedFiles.add(filename)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val attachmentsDir = File(getApplication<Application>().filesDir, "attachments")
        if (attachmentsDir.exists() && attachmentsDir.isDirectory) {
            val files = attachmentsDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile && !referencedFiles.contains(file.name)) {
                        file.delete()
                    }
                }
            }
        }
    }
}
