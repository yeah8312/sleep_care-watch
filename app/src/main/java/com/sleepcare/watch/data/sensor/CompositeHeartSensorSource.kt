package com.sleepcare.watch.data.sensor

import com.sleepcare.watch.domain.model.RawHeartReading
import com.sleepcare.watch.domain.model.SensorAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class CompositeHeartSensorSource(
    private val samsungSource: SamsungHealthSensorSource,
    private val fallbackSource: SensorManagerHeartRateSensorSource,
) : HeartSensorSource {
    private var activeSource: HeartSensorSource? = null

    override suspend fun availability(hasBodySensorPermission: Boolean): SensorAvailability {
        val samsungAvailability = samsungSource.availability(hasBodySensorPermission)
        if (samsungAvailability == SensorAvailability.AVAILABLE) {
            activeSource = samsungSource
            return samsungAvailability
        }

        val fallbackAvailability = fallbackSource.availability(hasBodySensorPermission)
        if (fallbackAvailability == SensorAvailability.AVAILABLE) {
            activeSource = fallbackSource
            return fallbackAvailability
        }

        activeSource = null
        return if (samsungAvailability == SensorAvailability.PERMISSION_REQUIRED ||
            fallbackAvailability == SensorAvailability.PERMISSION_REQUIRED
        ) {
            SensorAvailability.PERMISSION_REQUIRED
        } else {
            SensorAvailability.UNSUPPORTED
        }
    }

    override fun readings(): Flow<List<RawHeartReading>> {
        return activeSource?.readings() ?: emptyFlow()
    }

    override suspend fun flush() {
        activeSource?.flush()
    }
}

