package com.example.todoapp.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.todoapp.data.AttachmentData
import com.example.todoapp.data.LocationData
import com.example.todoapp.data.Priority
import com.example.todoapp.data.TaskEntity
import com.google.gson.Gson
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: TaskEntity,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    swipeToCompleteEnabled: Boolean = true,
    swipeToDeleteEnabled: Boolean = true
) {
    val context = LocalContext.current
    val gson = remember { Gson() }

    var expanded by remember { mutableStateOf(false) }

    val locations = remember(task.locationsJson) {
        if (!task.locationsJson.isNullOrEmpty() && task.locationsJson != "[]") {
            gson.fromJson(task.locationsJson, Array<LocationData>::class.java).toList()
        } else emptyList()
    }

    val attachments = remember(task.attachmentsJson) {
        if (!task.attachmentsJson.isNullOrEmpty() && task.attachmentsJson != "[]") {
            gson.fromJson(task.attachmentsJson, Array<AttachmentData>::class.java).toList()
        } else emptyList()
    }

    val priorityColor = when (task.priority) {
        Priority.HIGH -> Color(0xFFDC2626)
        Priority.MEDIUM -> Color(0xFFCA8A04)
        Priority.LOW -> Color(0xFF16A34A)
    }

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onComplete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
            }
            SwipeToDismissBoxValue.Settled -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = swipeToCompleteEnabled,
        enableDismissFromEndToStart = swipeToDeleteEnabled,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFC62828)
                    else -> Color.Transparent
                },
                label = "BackgroundColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.Check, contentDescription = "Complete", tint = Color.White)
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        },
        content = {
            Card(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                border = null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                color = priorityColor,
                                size = Size(width = 5.dp.toPx(), height = size.height)
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .fillMaxHeight()
                            .background(priorityColor)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 16.dp, bottom = 16.dp, end = 12.dp)
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp, top = 2.dp)
                                    .size(22.dp)
                                    .clickable { onComplete() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.isCompleted) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF2E7D32), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${task.date} ${task.time}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (task.isRecurring) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Repeat,
                                                contentDescription = "Recurring task",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))

                                            val recurrenceText = if (task.recurrenceType == "Custom") {
                                                "Every ${task.customRecurrenceInterval} ${task.customRecurrenceUnit}"
                                            } else {
                                                task.recurrenceType
                                            }

                                            Text(
                                                text = recurrenceText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (!expanded) {
                                        if (attachments.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachFile,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${attachments.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (locations.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${locations.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                if (expanded && task.description.isNotEmpty()) {
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (expanded) {
                            if (locations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Locations:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    locations.forEach { loc ->
                                        AssistChip(
                                            onClick = {
                                                try {
                                                    val mapUri = "google.navigation:q=${Uri.encode(loc.name)}".toUri()
                                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                                                        setPackage("com.google.android.apps.maps")
                                                    }
                                                    context.startActivity(mapIntent)
                                                } catch (_: Exception) {
                                                    try {
                                                        val browserUri = "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(loc.name)}".toUri()
                                                        val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                                                        context.startActivity(browserIntent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Can not open map", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            label = { Text(loc.name) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            if (attachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Attachments:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    attachments.forEach { file ->
                                        Card(
                                            onClick = {
                                                try {
                                                    val uri = file.uriString.toUri()
                                                    val systemMimeType = context.contentResolver.getType(uri) ?: when (file.type) {
                                                        "IMAGE" -> "image/*"
                                                        "PDF" -> "application/pdf"
                                                        "AUDIO" -> "audio/*"
                                                        else -> "*/*"
                                                    }

                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, systemMimeType)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                                                    }

                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(70.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                Icon(
                                                    imageVector = when (file.type) {
                                                        "IMAGE" -> Icons.Default.Image
                                                        "PDF" -> Icons.Default.PictureAsPdf
                                                        "AUDIO" -> Icons.Default.Mic
                                                        else -> Icons.Default.Description
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp).align(Alignment.Center).padding(bottom = 8.dp)
                                                )
                                                Text(
                                                    text = file.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        onDelete()
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete task",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete")
                                }

                                TextButton(
                                    onClick = {
                                        onComplete()
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete task",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Complete")
                                }

                                TextButton(
                                    onClick = {
                                        onEdit()
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit task",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}