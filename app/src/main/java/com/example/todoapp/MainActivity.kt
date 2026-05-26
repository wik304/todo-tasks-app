package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.todoapp.data.TaskDatabase
import com.example.todoapp.ui.navigation.Navigation
import com.example.todoapp.ui.TaskViewModel
import com.example.todoapp.ui.theme.ToDoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java, "task-database"
        )
            .fallbackToDestructiveMigration()
            .build()

        val taskDao = db.taskDao()

        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(application, taskDao) as T
            }
        }

        setContent {
            ToDoAppTheme {
                val viewModel: TaskViewModel = viewModel(factory = viewModelFactory)
                Navigation(viewModel = viewModel)
            }
        }
    }
}