package com.example.runinsync

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class StepCounterManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    public val lastTimestamp: Long
        get() = stepTimestamps.lastOrNull() ?: 0L

    public val totalSteps: Int
        get() = stepTimestamps.size



    var currentSpm by mutableIntStateOf(0)
    private val stepTimestamps = mutableListOf<Long>()

    fun start() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            val currentTime = System.currentTimeMillis()
            stepTimestamps.add(currentTime)
            calculateSpm(currentTime)
        }
    }

    private fun calculateSpm(currentTime: Long) {
        // Remove steps older than 10 seconds to calculate "Current" pace
        val windowMs = 10000L
        stepTimestamps.removeAll { it < currentTime - windowMs }

        if (stepTimestamps.size > 1) {
            // formula: (steps in window / window seconds) * 60
            val stepsInWindow = stepTimestamps.size
            val spm = (stepsInWindow.toFloat() / (windowMs / 1000f)) * 60
            currentSpm = spm.toInt()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}