package com.sleepcare.watch.data.sensor

import com.sleepcare.watch.domain.model.RawHeartReading
import com.sleepcare.watch.domain.model.SensorAvailability
import kotlinx.coroutines.flow.Flow

interface HeartSensorSource {
    suspend fun availability(hasBodySensorPermission: Boolean): SensorAvailability
    fun readings(): Flow<List<RawHeartReading>>
    suspend fun flush()
}

