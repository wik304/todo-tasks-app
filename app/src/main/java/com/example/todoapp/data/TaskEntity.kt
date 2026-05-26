package com.example.todoapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val createdAt: Long,
    val executeAt: Long?,
    val isCompleted: Boolean,
    val category: String,
    val priority: Priority,
    val recurrence: RecurrenceMode = RecurrenceMode.NONE,
    val locationsJson: String?,
    val attachmentsJson: String?
)

enum class Priority { LOW, MEDIUM, HIGH }

enum class RecurrenceMode {
    NONE, DAILY, WEEKLY, MONTHLY
}
