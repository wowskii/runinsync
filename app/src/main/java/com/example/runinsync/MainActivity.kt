package com.example.runinsync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.runinsync.ui.theme.RuninsyncTheme
import kotlinx.coroutines.delay
import com.example.runinsync.BuildConfig

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;





class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE = 1337

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val redirectUri = BuildConfig.SPOTIFY_REDIRECT_URI
    private var spotifyAppRemote: SpotifyAppRemote? = null

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
                AppContent(
                    viewModel = viewModel,
                    onConnectClick = { connectToSpotify() }
                )
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

    fun connectToSpotify() {
        val builder = com.spotify.sdk.android.auth.AuthorizationRequest.Builder(
            clientId,
            com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN,
            redirectUri
        )

        // These scopes are necessary for the App Remote to work
        builder.setScopes(arrayOf("app-remote-control", "streaming", "playlist-read-private"))
        val request = builder.build()

        // This opens the Spotify Login Activity
        com.spotify.sdk.android.auth.AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            val response = com.spotify.sdk.android.auth.AuthorizationClient.getResponse(resultCode, data)

            when (response.type) {
                com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN -> {
                    // LOGIN SUCCESS! Now that we are authorized, connect the Remote
                    Log.d("MainActivity", "Auth successful, connecting App Remote...")
                    actuallyConnectRemote()
                }
                com.spotify.sdk.android.auth.AuthorizationResponse.Type.ERROR -> {
                    Log.e("MainActivity", "Auth error: ${response.error}")
                }
                else -> {
                    Log.e("MainActivity", "Auth result: ${response.type}")
                }
            }
        }
    }

    private fun actuallyConnectRemote() {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("MainActivity", "SUCCESS: Connected!")
                connected()
            }
            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", "Remote connection failed: ${throwable.message}")
            }
        })
    }

    private fun connected() {
        Log.d("MainActivity", "Attempting to play playlist...")
        // Play a playlist
        spotifyAppRemote?.playerApi?.play("spotify:playlist:37i9dQZF1DX2sUQwD7tbmL")
            ?.setResultCallback {
                Log.d("MainActivity", "Play command accepted!")
            }
            ?.setErrorCallback { throwable ->
                // This is where you will see the REAL error
                Log.e("MainActivity", "Playback failed: ${throwable.message}")
            }
    }

    override fun onStop() {
        super.onStop()
    }
}

@Composable
fun AppContent(viewModel: MainViewModel, onConnectClick: () -> Unit) {
    // State that updates every second. AppContent will read it every time it updates, and update itself (useful for time ago)
    var ticks by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            ticks++
        }
    }

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

            //this line is essential for the scaffold to update the text
            Text("Ticks: $ticks")
            //Log.d("Debugging","Last Step Timestamp: ${viewModel.lastStepTimestamp} which is ${System.currentTimeMillis() - viewModel.lastStepTimestamp}ms ago")
            val timeAgo = if (viewModel.lastStepTimestamp > 0) {
                "${(System.currentTimeMillis() - viewModel.lastStepTimestamp) / 1000}s ago"
            } else "Never"
            Text("Last Step: $timeAgo")
            Button(onClick = { onConnectClick() }) {
                Text("Connect to Spotify")
            }
        }
    }
}
