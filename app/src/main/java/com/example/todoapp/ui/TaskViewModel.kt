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
import com.example.todoapp.data.TaskDao
import com.example.todoapp.data.TaskEntity
import com.example.todoapp.receiver.NotificationScheduler
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(TaskSort.DATE)
    val selectedSort = _selectedSort.asStateFlow()

    var appTheme by mutableStateOf("System")
        private set

    fun updateTheme(newTheme: String) {
        appTheme = newTheme
    }

    val tasksState: StateFlow<List<TaskEntity>> = combine(
        taskDao.getAllTasks(),
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { tasks, query, filter, sort ->
        var result = tasks.filter {
            it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }

        val now = System.currentTimeMillis()
        result = when (filter) {
            TaskFilter.ALL -> result
            TaskFilter.TODAY -> result.filter { it.executeAt != null && isToday(it.executeAt) }
            TaskFilter.OVERDUE -> result.filter { it.executeAt != null && it.executeAt < now && !it.isCompleted }
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
        attachments: List<AttachmentData>
    ) {
        viewModelScope.launch {
            val gson = Gson()

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
                category = "Default",
                priority = priority,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                customRecurrenceInterval = customInterval,
                customRecurrenceUnit = customUnit,
                locationsJson = if (locations.isNotEmpty()) gson.toJson(locations) else null,
                attachmentsJson = if (attachments.isNotEmpty()) gson.toJson(attachments) else null
            )
            val generatedId = taskDao.insertTask(task)
            val savedTask = task.copy(id = generatedId)
            NotificationScheduler.scheduleNotification(getApplication(), savedTask, delayMinutes = 0)
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
        attachments: List<AttachmentData>
    ) {
        viewModelScope.launch {
            val gson = Gson()

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
                category = "Default",
                priority = priority,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                customRecurrenceInterval = customInterval,
                customRecurrenceUnit = customUnit,
                locationsJson = if (locations.isNotEmpty()) gson.toJson(locations) else null,
                attachmentsJson = if (attachments.isNotEmpty()) gson.toJson(attachments) else null
            )
            taskDao.updateTask(updatedTask)

            NotificationScheduler.cancelNotification(getApplication(), updatedTask.id)
            NotificationScheduler.scheduleNotification(getApplication(), updatedTask, delayMinutes = 0)
        }
    }

    fun markAsCompleted(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = true))

            if (task.isRecurring && task.executeAt != null) {
                val nextExecuteAt = calculateNextOccurrence(
                    task.executeAt,
                    task.recurrenceType,
                    task.customRecurrenceInterval,
                    task.customRecurrenceUnit
                )

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val newDateString = dateFormat.format(Date(nextExecuteAt))
                val newTimeString = timeFormat.format(Date(nextExecuteAt))

                val nextTask = task.copy(
                    id = 0,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    executeAt = nextExecuteAt,
                    date = newDateString,
                    time = newTimeString
                )
                val nextId = taskDao.insertTask(nextTask)
                val scheduledNextTask = nextTask.copy(id = nextId)
                NotificationScheduler.scheduleNotification(getApplication(), scheduledNextTask, delayMinutes = 0)
            }
            NotificationScheduler.cancelNotification(getApplication(), task.id)
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
}