package com.sleepcare.watch.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.sleepcare.watch.domain.model.RawHeartReading
import com.sleepcare.watch.domain.model.SensorAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorManagerHeartRateSensorSource(
    context: Context,
) : HeartSensorSource {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    override suspend fun availability(hasBodySensorPermission: Boolean): SensorAvailability {
        return when {
            !hasBodySensorPermission -> SensorAvailability.PERMISSION_REQUIRED
            heartRateSensor == null -> SensorAvailability.UNSUPPORTED
            else -> SensorAvailability.AVAILABLE
        }
    }

    override fun readings(): Flow<List<RawHeartReading>> = callbackFlow {
        val sensor = heartRateSensor
        if (sensor == null) {
            close(IllegalStateException("Heart rate sensor is unavailable"))
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val bpm = event.values.firstOrNull()?.toInt() ?: 0
                trySend(
                    listOf(
                        RawHeartReading(
                            sensorTsMs = System.currentTimeMillis(),
                            bpm = bpm,
                            hrStatus = if (bpm > 0) 1 else 0,
                            ibiMs = emptyList(),
                            ibiStatus = emptyList(),
                        ),
                    ),
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    override suspend fun flush() = Unit
}

