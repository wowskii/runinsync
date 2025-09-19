package com.example.runinsync

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import com.example.runinsync.ui.theme.RuninsyncTheme
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession


// 1. Create ViewModel to hold player state
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(getApplication()).build()
    }
    val mediaSession = MediaSession.Builder(getApplication(), player).build()

    // Flag to track if the player is currently prepared with media
    var isPlayerPrepared = false
        private set // Only allow modification within this ViewModel

    /**
     * Prepares the player with a single media item.
     * Replaces any existing playlist.
     *
     * @param mediaUri The URI of the media to play (e.g., from a local file).
     */
    fun preparePlayer(mediaUri: Uri) {
        Log.d("PlayerViewModel", "Preparing player with URI: $mediaUri")
        val mediaItem = MediaItem.fromUri(mediaUri)
        player.setMediaItem(mediaItem) // Set the media item to play
        player.prepare()               // Prepare the player
        // player.playWhenReady = true // Optional: Start playback immediately when ready
        // Or you can call player.play() later
        isPlayerPrepared = true
        // Note: Playback will start asynchronously once the player is ready and
        // if playWhenReady is true or if you explicitly call player.play().
        // You might want to observe player state changes (e.g., Player.STATE_READY)
        // to update UI or take further actions.
    }


    /**
     * Adds a media item to the current playlist.
     * If the player is not yet prepared, this will also prepare it.
     *
     * @param mediaUri The URI of the media to add.
     */
    fun addMediaItemToPlaylist(mediaUri: Uri) {
        val mediaItem = MediaItem.fromUri(mediaUri)
        player.addMediaItem(mediaItem)
        if (!isPlayerPrepared) { // If player wasn't prepared, prepare it now
            player.prepare()
            isPlayerPrepared = true
        }
    }

    // Call this when you want to start or resume playback
    fun play() {
        if (isPlayerPrepared) {
            player.play()
        }
    }

    // Call this to pause playback
    fun pause() {
        player.pause()
    }

    override fun onCleared() {
        player.release()  // Cleanup when ViewModel is destroyed
        isPlayerPrepared = false
        super.onCleared()
    }
}



// 2. Modified MainActivity
class MainActivity : ComponentActivity() {


    // Activity Result Launcher for picking an audio file
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // Get the ViewModel instance
            val viewModel: PlayerViewModel = viewModels<PlayerViewModel>().value // More explicit way to get it in Activity
            viewModel.preparePlayer(it)
            // Optionally, tell the player to start playing immediately after preparation
            viewModel.player.playWhenReady = true
            // Or call viewModel.play() after some user action
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuninsyncTheme {
                // Get the ViewModel instance using the compose helper
                val viewModel: PlayerViewModel = viewModel()

                // Request necessary permissions (READ_MEDIA_AUDIO or READ_EXTERNAL_STORAGE for older APIs)
                // You'll need to handle permission requests properly.
                // For now, this is just a placeholder.

                AppContent(
                    viewModel = viewModel,
                    onPickAudio = {
                        pickAudioLauncher.launch("audio/*") // Launch file picker
                    }
                )
            }
        }
    }
}
@Composable
fun AppContent(viewModel: PlayerViewModel, onPickAudio: () -> Unit) {
    var currentPlaybackState by remember { mutableStateOf(viewModel.player.playbackState) }
    var playWhenReady by remember { mutableStateOf(viewModel.player.playWhenReady) }

    // Observe player state changes
    DisposableEffect(viewModel.player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                currentPlaybackState = playbackState
            }

            override fun onPlayWhenReadyChanged(playWhenReadyValue: Boolean, reason: Int) {
                playWhenReady = playWhenReadyValue
            }
        }
        viewModel.player.addListener(listener)
        onDispose {
            viewModel.player.removeListener(listener)
            // Consider if player should be stopped or just paused when screen is left
            // viewModel.player.stop() //
            // Or viewModel.pause()
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
            Button(onClick = onPickAudio) {
                Text("Select Audio File")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Log.d("AppContent", "Is it prepared? ${viewModel.isPlayerPrepared}")
            if (viewModel.isPlayerPrepared) {
                Button(onClick = {
                    if (viewModel.player.isPlaying) {
                        viewModel.pause()
                    } else {
                        viewModel.play()
                    }
                }) {
                    Text(if (playWhenReady && currentPlaybackState != androidx.media3.common.Player.STATE_IDLE && currentPlaybackState != androidx.media3.common.Player.STATE_ENDED) "Pause" else "Play")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Player State: ${playerStateToString(currentPlaybackState)}")
                Text(if(playWhenReady) "Playing" else "Paused/Stopped")

            } else {
                Text("Player not yet prepared. Select an audio file.")
            }

            // You would add more UI controls here (seek bar, song title, etc.)
        }
    }
}

fun playerStateToString(state: Int): String {
    return when (state) {
        androidx.media3.common.Player.STATE_IDLE -> "IDLE"
        androidx.media3.common.Player.STATE_BUFFERING -> "BUFFERING"
        androidx.media3.common.Player.STATE_READY -> "READY"
        androidx.media3.common.Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN"
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RuninsyncTheme {
        // For preview, we can't easily instantiate a ViewModel with Application context
        // So, this preview might show the "Player not yet prepared" state.
        AppContent(
            viewModel = viewModel<PlayerViewModel>(),
            onPickAudio = {}
        )
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RuninsyncTheme {
        Greeting("Android")
    }
}