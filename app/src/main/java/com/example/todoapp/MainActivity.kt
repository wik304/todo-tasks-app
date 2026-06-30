package com.example.todoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.todoapp.data.TaskDatabase
import com.example.todoapp.ui.navigation.Navigation
import com.example.todoapp.ui.TaskViewModel
import com.example.todoapp.ui.theme.ToDoAppTheme
import android.view.WindowManager
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun KeepScreenOnEffect(keepAwake: Boolean) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(keepAwake) {
        if (keepAwake) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = TaskDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()

        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(application, taskDao) as T
            }
        }
        val viewModel = ViewModelProvider(this, viewModelFactory)[TaskViewModel::class.java]
        handleIntent(intent, viewModel)

        setContent {
            val context = LocalContext.current
            val viewModel: TaskViewModel = viewModel(factory = viewModelFactory)

            KeepScreenOnEffect(keepAwake = viewModel.keepAwake)

            val backgroundLocationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { _ -> }
            )

            val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissions ->
                    val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                    if (fineLocationGranted || coarseLocationGranted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        }
                    }
                }
            )

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }

                val permissionsNotGranted = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }

                if (permissionsNotGranted.isNotEmpty()) {
                    multiplePermissionsLauncher.launch(permissionsNotGranted.toTypedArray())
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                }
            }

            ToDoAppTheme(themeMode = viewModel.appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val db = TaskDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()
        val viewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(application, taskDao) as T
            }
        }
        val viewModel = ViewModelProvider(this, viewModelFactory)[TaskViewModel::class.java]
        handleIntent(intent, viewModel)
    }

    private fun handleIntent(intent: Intent?, viewModel: TaskViewModel) {
        val taskId = intent?.getLongExtra("TASK_ID", -1L) ?: -1L
        if (taskId != -1L) {
            viewModel.setExpandedTaskId(taskId)
        }
    }
}