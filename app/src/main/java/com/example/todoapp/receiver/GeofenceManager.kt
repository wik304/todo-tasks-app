package com.example.todoapp.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.todoapp.data.LocationData
import com.example.todoapp.data.TaskEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson

object GeofenceManager {

    @SuppressLint("MissingPermission")
    fun addGeofencesForTask(context: Context, task: TaskEntity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        if (task.locationsJson.isNullOrEmpty() || task.locationsJson == "[]") return

        val locations = try {
            Gson().fromJson(task.locationsJson, Array<LocationData>::class.java).toList()
        } catch (e: Exception) { emptyList() }

        if (locations.isEmpty()) return

        val geofencingClient = LocationServices.getGeofencingClient(context)
        val geofenceList = mutableListOf<Geofence>()

        locations.forEachIndexed { index, loc ->
            geofenceList.add(
                Geofence.Builder()
                    .setRequestId("${task.id}_${task.title}")
                    .setCircularRegion(loc.lat, loc.lng, loc.radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            )
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        geofencingClient.addGeofences(geofencingRequest, getPendingIntent(context, task.id.toInt()))
    }

    fun removeGeofencesForTask(context: Context, taskId: Long) {
        val geofencingClient = LocationServices.getGeofencingClient(context)
        geofencingClient.removeGeofences(getPendingIntent(context, taskId.toInt()))
    }

    private fun getPendingIntent(context: Context, taskId: Int): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}