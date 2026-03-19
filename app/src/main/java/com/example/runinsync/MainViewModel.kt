package com.example.runinsync

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {
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
}
