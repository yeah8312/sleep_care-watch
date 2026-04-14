package com.sleepcare.watch.data.sensor

import android.content.Context
import com.sleepcare.watch.domain.model.RawHeartReading
import com.sleepcare.watch.domain.model.SensorAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.reflect.Proxy

class SamsungHealthSensorSource(
    private val context: Context,
) : HeartSensorSource {
    private var activeTracker: Any? = null

    override suspend fun availability(hasBodySensorPermission: Boolean): SensorAvailability {
        return when {
            !hasBodySensorPermission -> SensorAvailability.PERMISSION_REQUIRED
            !isSdkPresent() -> SensorAvailability.UNSUPPORTED
            else -> SensorAvailability.AVAILABLE
        }
    }

    override fun readings(): Flow<List<RawHeartReading>> = callbackFlow {
        if (!isSdkPresent()) {
            close(IllegalStateException("Samsung Health Sensor SDK is not present"))
            return@callbackFlow
        }

        val connectionListenerClass =
            Class.forName("com.samsung.android.service.health.tracking.ConnectionListener")
        val healthTrackingServiceClass =
            Class.forName("com.samsung.android.service.health.tracking.HealthTrackingService")
        val healthTrackerTypeClass =
            Class.forName("com.samsung.android.service.health.tracking.data.HealthTrackerType")
        val trackerEventListenerClass =
            Class.forName("com.samsung.android.service.health.tracking.HealthTracker\$TrackerEventListener")
        val heartRateSetClass =
            Class.forName("com.samsung.android.service.health.tracking.data.ValueKey\$HeartRateSet")

        val healthServiceRef = arrayOfNulls<Any>(1)
        val trackerRef = arrayOfNulls<Any>(1)
        val trackerListenerRef = arrayOfNulls<Any>(1)

        val connectionListener = Proxy.newProxyInstance(
            connectionListenerClass.classLoader,
            arrayOf(connectionListenerClass),
        ) { _, method, args ->
            when (method.name) {
                "onConnectionSuccess" -> {
                    runCatching {
                        val healthService = healthServiceRef[0] ?: return@runCatching
                        val trackerType = java.lang.Enum.valueOf(
                            healthTrackerTypeClass as Class<out Enum<*>>,
                            "HEART_RATE_CONTINUOUS",
                        )
                        val tracker = healthTrackingServiceClass
                            .getMethod("getHealthTracker", healthTrackerTypeClass)
                            .invoke(healthService, trackerType)
                        trackerRef[0] = tracker
                        activeTracker = tracker

                        val trackerListener = Proxy.newProxyInstance(
                            trackerEventListenerClass.classLoader,
                            arrayOf(trackerEventListenerClass),
                        ) { _, trackerMethod, trackerArgs ->
                            when (trackerMethod.name) {
                                "onDataReceived" -> {
                                    val rawList = trackerArgs?.firstOrNull() as? List<*> ?: emptyList<Any>()
                                    val samples = rawList.mapNotNull { dataPoint ->
                                        runCatching {
                                            parseHeartRateDataPoint(dataPoint ?: return@runCatching null, heartRateSetClass)
                                        }.getOrNull()
                                    }
                                    if (samples.isNotEmpty()) {
                                        trySend(samples)
                                    }
                                    null
                                }

                                "onError" -> null
                                "onFlushCompleted" -> null
                                else -> null
                            }
                        }
                        trackerListenerRef[0] = trackerListener
                        tracker.javaClass
                            .getMethod("setEventListener", trackerEventListenerClass)
                            .invoke(tracker, trackerListener)
                    }.onFailure { close(it) }
                }

                "onConnectionEnded" -> Unit
                "onConnectionFailed" -> close(IllegalStateException(args?.firstOrNull()?.toString() ?: "Samsung SDK connection failed"))
            }
            null
        }

        runCatching {
            val service = healthTrackingServiceClass
                .getConstructor(connectionListenerClass, Context::class.java)
                .newInstance(connectionListener, context.applicationContext)
            healthServiceRef[0] = service
            healthTrackingServiceClass.getMethod("connectService").invoke(service)
        }.onFailure { close(it) }

        awaitClose {
            runCatching {
                trackerRef[0]?.javaClass?.getMethod("unsetEventListener")?.invoke(trackerRef[0])
            }
            runCatching {
                healthServiceRef[0]?.javaClass?.getMethod("disconnectService")?.invoke(healthServiceRef[0])
            }
            activeTracker = null
        }
    }

    override suspend fun flush() {
        runCatching {
            activeTracker?.javaClass?.getMethod("flush")?.invoke(activeTracker)
        }
    }

    private fun parseHeartRateDataPoint(dataPoint: Any, heartRateSetClass: Class<*>): RawHeartReading {
        val heartRateKey = heartRateSetClass.getField("HEART_RATE").get(null)
        val heartRateStatusKey = heartRateSetClass.getField("HEART_RATE_STATUS").get(null)
        val ibiListKey = heartRateSetClass.getField("IBI_LIST").get(null)
        val ibiStatusListKey = heartRateSetClass.getField("IBI_STATUS_LIST").get(null)

        val getValueMethod = dataPoint.javaClass.methods.first { it.name == "getValue" }
        val bpm = (getValueMethod.invoke(dataPoint, heartRateKey) as? Number)?.toInt() ?: 0
        val hrStatus = (getValueMethod.invoke(dataPoint, heartRateStatusKey) as? Number)?.toInt() ?: 0
        val ibiValues = (getValueMethod.invoke(dataPoint, ibiListKey) as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
            ?: emptyList()
        val ibiStatuses =
            (getValueMethod.invoke(dataPoint, ibiStatusListKey) as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
                ?: emptyList()

        return RawHeartReading(
            sensorTsMs = System.currentTimeMillis(),
            bpm = bpm,
            hrStatus = hrStatus,
            ibiMs = ibiValues,
            ibiStatus = ibiStatuses,
        )
    }

    private fun isSdkPresent(): Boolean {
        return runCatching {
            Class.forName("com.samsung.android.service.health.tracking.HealthTrackingService")
        }.isSuccess
    }
}

