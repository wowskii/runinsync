package com.example.runinsync

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var spotifyUser by mutableStateOf<SpotifyUser?>(null)
    var errorMessage by mutableStateOf<String?>(null)

    private val stepCounterManager = StepCounterManager(application)

    val totalStepsDetected: Int get() = stepCounterManager.totalSteps
    val lastStepTimestamp: Long get() = stepCounterManager.lastTimestamp
    val currentStepPace: Int get() = stepCounterManager.currentSpm

    fun startStepCounter() {
        stepCounterManager.start()
    }

    override fun onCleared() {
        super.onCleared()
        stepCounterManager.stop()
    }

    fun fetchSpotifyUser(accessToken: String) {
        viewModelScope.launch {
            try {
                val user = SpotifyApi.retrofitService.getCurrentUser("Bearer $accessToken")
                spotifyUser = user
            } catch (e: Exception) {
                errorMessage = e.message
                Log.e("MainViewModel", "Failed to get user", e)
            }
        }
    }
}
