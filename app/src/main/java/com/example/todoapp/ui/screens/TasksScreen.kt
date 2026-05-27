package com.example.todoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoapp.ui.TaskViewModel
import com.example.todoapp.ui.TaskFilter
import com.example.todoapp.ui.TaskSort
import com.example.todoapp.ui.components.TaskItem

enum class ActiveSection { NONE, SEARCH, FILTER, SORT }

@Composable
fun TasksScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasksState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()

    var activeSection by remember { mutableStateOf(ActiveSection.NONE) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        activeSection = if (activeSection == ActiveSection.SEARCH) ActiveSection.NONE else ActiveSection.SEARCH
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        activeSection = if (activeSection == ActiveSection.FILTER) ActiveSection.NONE else ActiveSection.FILTER
                    }
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filter")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        activeSection = if (activeSection == ActiveSection.SORT) ActiveSection.NONE else ActiveSection.SORT
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sort")
                }
            }

            AnimatedVisibility(visible = activeSection == ActiveSection.SEARCH) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        label = { Text("Search tasks") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            AnimatedVisibility(visible = activeSection == ActiveSection.FILTER) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                ) {
                    Text("Filter:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All", "Today", "Overdue")
                        filters.forEach { filter ->
                            val targetFilter = when (filter) {
                                "All" -> TaskFilter.ALL
                                "Today" -> TaskFilter.TODAY
                                else -> TaskFilter.OVERDUE
                            }
                            FilterChip(
                                selected = selectedFilter == targetFilter,
                                onClick = { viewModel.onFilterChange(targetFilter) },
                                label = { Text(filter) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            AnimatedVisibility(visible = activeSection == ActiveSection.SORT) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                ) {
                    Text("Sort by:", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sorts = listOf("Date", "Priority")
                        sorts.forEach { sort ->
                            val targetSort = when (sort) {
                                "Date" -> TaskSort.DATE
                                else -> TaskSort.PRIORITY
                            }
                            InputChip(
                                selected = selectedSort == targetSort,
                                onClick = { viewModel.onSortChange(targetSort) },
                                label = { Text(sort) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            HorizontalDivider()

//            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onComplete = { viewModel.markAsCompleted(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }
}