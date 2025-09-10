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


// 1. Create ViewModel to hold player state
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    val player: ExoPlayer by lazy {
        ExoPlayer.Builder(getApplication()).build()
    }

    override fun onCleared() {
        player.release()  // Cleanup when ViewModel is destroyed
        super.onCleared()
    }
}

// 2. Modified MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RuninsyncTheme {
                val viewModel: PlayerViewModel = viewModel()

                // 3. Lifecycle-aware player management
                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.player.stop()  // Pause playback when leaving screen
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding),
                        player = viewModel.player
                    )
                }
            }
        }
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