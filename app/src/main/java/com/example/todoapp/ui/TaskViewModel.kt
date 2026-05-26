package com.example.todoapp.ui

import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.LocationData
import com.example.todoapp.data.RecurrenceMode
import com.example.todoapp.data.TaskDao
import com.example.todoapp.data.TaskEntity
import com.example.todoapp.data.Priority
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import com.example.todoapp.data.AttachmentData

enum class TaskFilter { ALL, TODAY, OVERDUE }
enum class TaskSort { DATE, PRIORITY }

class TaskViewModel(application: Application, private val taskDao: TaskDao) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(TaskSort.DATE)
    val selectedSort = _selectedSort.asStateFlow()

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

    fun addTask(title: String, description: String, priority: Priority, locations: List<LocationData>, attachments: List<AttachmentData>) {
        viewModelScope.launch {
            val gson = Gson()

            val task = TaskEntity(
                title = title,
                description = description,
                createdAt = System.currentTimeMillis(),
                executeAt = null,
                isCompleted = false,
                category = "",
                priority = priority,
                locationsJson = gson.toJson(locations),
                attachmentsJson = gson.toJson(attachments)
            )
            taskDao.insertTask(task)
        }
    }

    fun markAsCompleted(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = true))

            if (task.recurrence != RecurrenceMode.NONE && task.executeAt != null) {
                val nextExecuteAt = calculateNextOccurrence(task.executeAt, task.recurrence)

                val nextTask = task.copy(
                    id = 0,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    executeAt = nextExecuteAt
                )
                taskDao.insertTask(nextTask)
            }
        }
    }

    private fun calculateNextOccurrence(currentTimestamp: Long, mode: RecurrenceMode): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        when (mode) {
            RecurrenceMode.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrenceMode.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceMode.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceMode.NONE -> {}
        }
        return calendar.timeInMillis
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
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