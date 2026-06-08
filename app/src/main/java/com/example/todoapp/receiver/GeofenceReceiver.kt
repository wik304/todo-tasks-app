package com.example.todoapp.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPrefs = context.getSharedPreferences("todo_settings", Context.MODE_PRIVATE)
        val locationNotificationsEnabled = sharedPrefs.getBoolean("location_notifications_enabled", true)

        if (!locationNotificationsEnabled) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null || geofencingEvent.hasError()) return

        if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            val firstGeofenceId = triggeringGeofences[0].requestId
            val parts = firstGeofenceId.split("_", limit = 2)
            val taskId = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val taskTitle = parts.getOrNull(1) ?: "You reached a location!"

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(context, "todo_tasks_alerts")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(taskTitle)
                .setContentText("You are near the location for this task.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(taskId + 1000, notification)
        }
    }
}