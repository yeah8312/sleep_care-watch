package com.sleepcare.watch

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sleepcare.watch.ui.WatchApp

class MainActivity : ComponentActivity() {
    private val viewModel: WatchViewModel by viewModels {
        WatchViewModel.factory((application as SleepCareWatchApplication).runtime.repository)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.refreshCapabilities()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var requestedInitialPermissions by mutableStateOf(false)
            if (!requestedInitialPermissions) {
                requestedInitialPermissions = true
                requestNeededPermissions()
            }

            WatchApp(
                viewModel = viewModel,
                onRequestPermissions = { requestNeededPermissions() },
            )
        }
    }

    private fun requestNeededPermissions() {
        val permissions = buildList {
            add(Manifest.permission.BODY_SENSORS)
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.BODY_SENSORS_BACKGROUND)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

