package com.example.todoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val createdAt: Long = System.currentTimeMillis(),
    val executeAt: Long? = null,
    val isCompleted: Boolean = false,
    val category: String = "Default",
    val priority: Priority,
    val isRecurring: Boolean,
    val recurrenceType: String,
    val customRecurrenceInterval: Int,
    val customRecurrenceUnit: String,
    // -----------------------------------
    val locationsJson: String?,
    val attachmentsJson: String?
)

enum class Priority { LOW, MEDIUM, HIGH }

enum class RecurrenceMode {
    NONE, DAILY, WEEKLY, MONTHLY
}