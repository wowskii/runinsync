package com.example.runinsync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runinsync.ui.theme.RuninsyncTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.startStepCounter()
        } else {
            Log.d("Permissions", "Activity Recognition permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissionsAndStartStepCounter()

        enableEdgeToEdge()
        setContent {
            RuninsyncTheme {
                AppContent(viewModel = viewModel)
            }
        }
    }

    private fun checkPermissionsAndStartStepCounter() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val permission = android.Manifest.permission.ACTIVITY_RECOGNITION
            when {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this, permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    viewModel.startStepCounter()
                }
                else -> {
                    requestPermissionLauncher.launch(permission)
                }
            }
        } else {
            viewModel.startStepCounter()
        }
    }
}

@Composable
fun AppContent(viewModel: MainViewModel) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("--- Pedometer ---")
            Spacer(modifier = Modifier.height(16.dp))

            Text("Total Steps: ${viewModel.totalStepsDetected}")
            Text("Current SPM: ${viewModel.currentStepPace}")

            val timeAgo = if (viewModel.lastStepTimestamp > 0) {
                "${(System.currentTimeMillis() - viewModel.lastStepTimestamp) / 1000}s ago"
            } else "Never"
            Text("Last Step: $timeAgo")
        }
    }
}
