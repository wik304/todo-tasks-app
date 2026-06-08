package com.example.todoapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoapp.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val sharedPrefs = context.getSharedPreferences("todo_settings", Context.MODE_PRIVATE)
            val startOnBootEnabled = sharedPrefs.getBoolean("start_on_boot", true)
            val locationNotificationsEnabled = sharedPrefs.getBoolean("location_notifications_enabled", true)

            if (!startOnBootEnabled) return

            val pendingResult = goAsync()
            val database = TaskDatabase.getDatabase(context)
            val taskDao = database.taskDao()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val allTasks = taskDao.getAllTasksSynchronously()
                    val now = System.currentTimeMillis()

                    for (task in allTasks) {
                        if (!task.isCompleted && task.executeAt != null && task.executeAt > now) {
                            NotificationScheduler.scheduleNotification(context, task)
                        }
                        
                        if (!task.isCompleted && locationNotificationsEnabled && !task.locationsJson.isNullOrEmpty()) {
                            GeofenceManager.addGeofencesForTask(context, task)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}