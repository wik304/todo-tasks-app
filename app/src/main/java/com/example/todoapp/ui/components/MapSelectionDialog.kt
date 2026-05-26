package com.example.todoapp.ui.components

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun MapSelectionDialog(
    onDismiss: () -> Unit,
    onLocationSelected: (lat: Double, lng: Double, radiusMeters: Float, addressName: String) -> Unit
) {
    var markerPosition by remember { mutableStateOf(LatLng(51.7592, 19.4560)) }
    var radius by remember { mutableStateOf(200f) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerPosition, 13f)
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isConfirming by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                GoogleMap(
                    modifier = Modifier.weight(1f),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { newLatLng ->
                        markerPosition = newLatLng
                    }
                ) {
                    Marker(
                        state = MarkerState(position = markerPosition),
                        title = "Selected Location"
                    )
                    Circle(
                        center = markerPosition,
                        radius = radius.toDouble(),
                        fillColor = Color(0x330000FF),
                        strokeColor = Color.Blue,
                        strokeWidth = 2f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Radius: ${radius.toInt()} meters",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 50f..2000f,
                        steps = 39
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isConfirming = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    var addressName = "Selected Area"
                                    try {
                                        val addresses = geocoder.getFromLocation(
                                            markerPosition.latitude,
                                            markerPosition.longitude,
                                            1
                                        )
                                        if (!addresses.isNullOrEmpty()) {
                                            val address = addresses[0]
                                            addressName = address.thoroughfare ?: address.locality ?: address.getAddressLine(0) ?: "Selected Area"
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    withContext(Dispatchers.Main) {
                                        onLocationSelected(markerPosition.latitude, markerPosition.longitude, radius, addressName)
                                        isConfirming = false
                                    }
                                }
                            },
                            enabled = !isConfirming
                        ) {
                            if (isConfirming) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("Confirm Area")
                            }
                        }
                    }
                }
            }
        }
    }
}