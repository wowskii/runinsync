package com.example.runinsync

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(getApplication()).build()
    }

    var songTempo by mutableIntStateOf(0)
        private set

    val mediaSession : MediaSession by lazy { MediaSession.Builder(getApplication(), player).build() }

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
//        currentTrackDisplayName = "${getMetadataFromUri(getApplication(), mediaUri).first} - ${getMetadataFromUri(getApplication(), mediaUri).second}"
        val (title, artist) = getMetadataFromUri(getApplication(), mediaUri)
        triggerTempoFetch(title, artist)
        currentTrackDisplayName = "$title - $artist"
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

    fun triggerTempoFetch(embeddedTitle: String?, embeddedArtist: String?) {
        Log.d("PlayerViewModel", "triggerTempoFetch called with title: $embeddedTitle, artist: $embeddedArtist")
        // 1. Determine the "Fallback" logic inside the ViewModel// If we have both, search by both.
        // Otherwise, use the title we have, or the filename (currentTrackDisplayName)

        val canSearchByBoth = !embeddedTitle.isNullOrBlank() &&
                !embeddedArtist.isNullOrBlank() &&
                embeddedArtist != "Unknown Artist"

        if (canSearchByBoth) {
            // Search using "both"
            executeApiCall(title = embeddedTitle, artist = embeddedArtist)
        } else {
            // Fallback: Search using only the title or the filename
            val fallbackQuery = embeddedTitle ?: currentTrackDisplayName ?: "Unknown"
            executeApiCall(title = fallbackQuery, artist = null)
        }
    }

    private fun executeApiCall(title: String, artist: String?) {
        viewModelScope.launch {
            try {
                val isSearchByBoth = artist != null
                val response = RetrofitInstance.api.getTempo(
                    apiKey = BuildConfig.API_KEY,
                    type = if (isSearchByBoth) "both" else "song",
                    lookup = if (isSearchByBoth) "song:$title artist:$artist" else title,
                    limit = 1
                )
                if (response.isSuccessful) {
                    songTempo = response.body()?.tempo ?: -1
                }
            } catch (e: Exception) {
                songTempo = -1
            }
        }
    }
}
