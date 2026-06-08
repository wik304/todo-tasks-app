package com.example.todoapp.data

import java.util.Calendar

object RecurrenceHelper {
    fun calculateNextOccurrence(
        currentTimestamp: Long,
        type: String,
        interval: Int,
        unit: String
    ): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        val now = System.currentTimeMillis()

        do {
            when (type) {
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                "Custom" -> {
                    when (unit) {
                        "Days" -> calendar.add(Calendar.DAY_OF_YEAR, interval)
                        "Weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, interval)
                        "Months" -> calendar.add(Calendar.MONTH, interval)
                        "Years" -> calendar.add(Calendar.YEAR, interval)
                    }
                }
            }
        } while (calendar.timeInMillis <= now)

        return calendar.timeInMillis
    }
}
