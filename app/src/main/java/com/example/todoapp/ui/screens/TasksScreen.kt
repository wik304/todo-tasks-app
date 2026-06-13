package com.example.todoapp.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.example.todoapp.ui.TaskViewModel
import com.example.todoapp.ui.TaskFilter
import com.example.todoapp.ui.TaskSort
import com.example.todoapp.ui.components.TaskItem
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp

@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    onEditClick: (Long) -> Unit
) {
    val focusManager = LocalFocusManager.current

    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen) {
            focusManager.clearFocus()
        }
    }

    val tasks by viewModel.tasksState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.onSearchQueryChange("")
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = "Filter:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                val filters = listOf("All", "Today", "Overdue")
                items(filters) { filter ->
                    val targetFilter = when (filter) {
                        "All" -> TaskFilter.ALL
                        "Today" -> TaskFilter.TODAY
                        else -> TaskFilter.OVERDUE
                    }
                    FilterChip(
                        selected = selectedFilter == targetFilter,
                        onClick = { 
                            focusManager.clearFocus()
                            viewModel.onFilterChange(targetFilter) 
                        },
                        label = { Text(filter) },
                        leadingIcon = if (selectedFilter == targetFilter) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                item {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()

                    Box {
                        FilterChip(
                            selected = selectedCategoryFilter != "All",
                            onClick = { expanded = true },
                            label = { Text(if (selectedCategoryFilter == "All") "Category: All" else selectedCategoryFilter) },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = {
                                    viewModel.onCategoryFilterChange("All")
                                    expanded = false
                                }
                            )
                            viewModel.categoriesList.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.onCategoryFilterChange(category)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.width(4.dp))
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                item {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                val sorts = listOf("Date", "Priority")
                items(sorts) { sort ->
                    val targetSort = when (sort) {
                        "Date" -> TaskSort.DATE
                        else -> TaskSort.PRIORITY
                    }
                    InputChip(
                        selected = selectedSort == targetSort,
                        onClick = { 
                            focusManager.clearFocus()
                            viewModel.onSortChange(targetSort) 
                        },
                        label = { Text(sort) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onComplete = {
                            focusManager.clearFocus()
                            viewModel.markAsCompleted(task)
                        },
                        onDelete = {
                            focusManager.clearFocus()
                            viewModel.deleteTask(task)
                        },
                        onEdit = {
                            focusManager.clearFocus()
                            onEditClick(task.id)
                        }
                    )
                }
            }
        }
    }
}
