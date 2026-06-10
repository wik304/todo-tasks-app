package com.example.todoapp.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.example.todoapp.data.TaskEntity

object NotificationScheduler {
    private const val BEFOREHAND_ID_OFFSET = 1000000

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(context: Context, task: TaskEntity, delayMinutes: Long = 0) {
        val executeTime = task.executeAt ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val mainTriggerTime = executeTime - (delayMinutes * 60 * 1000)

        if (mainTriggerTime >= System.currentTimeMillis()) {
            val intentMain = Intent(context, TaskNotificationReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
                putExtra("TASK_DESC", task.description)
            }

            val pendingIntentMain = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intentMain,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, mainTriggerTime, pendingIntentMain)
            } catch (e: SecurityException) {
                e.printStackTrace()
                alarmManager.set(AlarmManager.RTC_WAKEUP, mainTriggerTime, pendingIntentMain)
            }
        }

        val sharedPrefs = context.getSharedPreferences("todo_settings", Context.MODE_PRIVATE)
        val notifyBefore = sharedPrefs.getBoolean("notify_before", false)
        val notifyBeforeTimeStr = sharedPrefs.getString("notify_before_time", "15 min") ?: "15 min"

        if (notifyBefore) {
            val minutesBefore = when (notifyBeforeTimeStr) {
                "5 min" -> 5
                "15 min" -> 15
                "1 h" -> 60
                "1 day" -> 1440
                else -> 0
            }

            val earlyTriggerTime = executeTime - (minutesBefore * 60 * 1000L)

            if (earlyTriggerTime >= System.currentTimeMillis()) {
                val intentEarly = Intent(context, TaskNotificationReceiver::class.java).apply {
                    putExtra("TASK_ID", task.id)
                    putExtra("TASK_TITLE", "Upcoming: ${task.title}")
                    putExtra("TASK_DESC", "Starts in $notifyBeforeTimeStr")
                }

                val pendingIntentEarly = PendingIntent.getBroadcast(
                    context,
                    task.id.toInt() + BEFOREHAND_ID_OFFSET,
                    intentEarly,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, earlyTriggerTime, pendingIntentEarly)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    alarmManager.set(AlarmManager.RTC_WAKEUP, earlyTriggerTime, pendingIntentEarly)
                }
            }
        }
    }

    fun cancelNotification(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskNotificationReceiver::class.java)

        val pendingIntentMain = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntentMain != null) {
            alarmManager.cancel(pendingIntentMain)
        }

        val pendingIntentEarly = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + BEFOREHAND_ID_OFFSET,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntentEarly != null) {
            alarmManager.cancel(pendingIntentEarly)
        }
    }
}