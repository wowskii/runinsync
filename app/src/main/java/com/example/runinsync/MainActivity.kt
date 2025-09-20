package com.example.runinsync

import android.app.Application
import android.content.Context
import android.icu.util.TimeUnit
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
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit as JavaTimeUnit


// 1. Create ViewModel to hold player state
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(getApplication()).build()
    }
    val mediaSession = MediaSession.Builder(getApplication(), player).build()

    // Flag to track if the player is currently prepared with media
    var isPlayerPrepared by mutableStateOf(false)
        private set // Only allow modification within this ViewModel


    var currentTrackDisplayName by mutableStateOf<String?>(null)
        private set

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
        currentTrackDisplayName = "${getMetadataFromUri(getApplication(), mediaUri).first} - ${getMetadataFromUri(getApplication(), mediaUri).second}"
    }


    /**
     * Adds a media item to the current playlist.
     * If the player is not yet prepared, this will also prepare it.
     *
     * @param mediaUri The URI of the media to add.
     */
    fun addMediaItemToPlaylist(mediaUri: Uri) {
        // You might want to handle display names for playlists differently
        // e.g., store a list of display names or update currentTrackDisplayName
        // when the current media item changes in the playlist.
        // For simplicity, this example only focuses on the single prepared item.
        if (currentTrackDisplayName == null) { // Only set if not already set by preparePlayer
            currentTrackDisplayName = "${getMetadataFromUri(getApplication(), mediaUri).first} - ${getMetadataFromUri(getApplication(), mediaUri).second}"
        }

        Log.d("PlayerViewModel", "Adding media item to playlist: $mediaUri")
        val mediaItem = MediaItem.fromUri(mediaUri)
        player.addMediaItem(mediaItem)
        if (!isPlayerPrepared) {
            player.prepare()
        }
        isPlayerPrepared = true
        Log.d("PlayerViewModel", "Player is now prepared (after adding to playlist): $isPlayerPrepared")
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
        Log.d("PlayerViewModel", "onCleared called, releasing player and media session.")
        player.release()  // Cleanup when ViewModel is destroyed
        mediaSession.release()
        isPlayerPrepared = false
        super.onCleared()
        currentTrackDisplayName = null
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
        }
        // Fallback for file URIs or if content resolver fails
        return uri.lastPathSegment
    }

    private fun getMetadataFromUri(context: Context, uri: Uri): Pair<String, String> {
        val filename = getFileNameFromUri(context, uri)
        if (filename != null) {
            val stripped = filename.split('-')
            val title = stripped[0]
            val artist = if ('(' in stripped[1]) {
                stripped[1].split('(')[0]
            } else if ('.' in stripped[1]) {
                stripped[1].split('.')[0]
            } else {
                stripped[1]
            }
            return Pair(title, artist)
        }
        return Pair("Unknown Title", "Unknown Artist")
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

                val embeddedTitle = viewModel.player.mediaMetadata.title?.toString()
                val embeddedArtist = viewModel.player.mediaMetadata.artist?.toString()
                val finalDisplayTitle = if (embeddedTitle != null && embeddedArtist != null) {
                    "$embeddedTitle - $embeddedArtist"
                } else embeddedTitle
                    ?: (embeddedArtist
                        ?: viewModel.currentTrackDisplayName
                            ?: "No Title Available")
                var currentPositionMs by remember { mutableLongStateOf(0L) }
                val durationMs = viewModel.player.duration // Duration usually doesn't change once loaded

                LaunchedEffect(viewModel.player.isPlaying, viewModel.isPlayerPrepared) {
                    if (viewModel.isPlayerPrepared && viewModel.player.isPlaying) {
                        while (true) {
                            currentPositionMs = viewModel.player.currentPosition
                            delay(1000) // Update every second
                        }
                    }
                }

                val currentPositionFormatted = formatMillisecondsToMinSec(currentPositionMs)
                val durationFormatted = formatMillisecondsToMinSec(durationMs)

                Text("Player State: ${playerStateToString(currentPlaybackState)}")
                Text(if(playWhenReady) "Playing" else "Paused/Stopped")
                Text(finalDisplayTitle)
                Text("$currentPositionFormatted / $durationFormatted")
                Text("Playing at BPM: TODO")
            } else {
                Text("Player not yet prepared. Select an audio file.")
            }

            // You would add more UI controls here (seek bar, song title, etc.)
        }
    }
}

/**
 * Formats milliseconds into a "MM:SS" or "HH:MM:SS" string.
 */
fun formatMillisecondsToMinSec(milliseconds: Long): String {
    if (milliseconds < 0) return "00:00" // Or handle error appropriately

    val totalSeconds = JavaTimeUnit.MILLISECONDS.toSeconds(milliseconds)
    val hours = JavaTimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = JavaTimeUnit.SECONDS.toMinutes(totalSeconds) % 60 // Minutes part of HH:MM:SS or MM:SS
    val seconds = totalSeconds % 60 // Seconds part

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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