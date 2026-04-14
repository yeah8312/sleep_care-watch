package com.sleepcare.watch.data.runtime

import android.content.Context
import com.sleepcare.watch.data.device.AndroidVibrationController
import com.sleepcare.watch.data.device.WearPhoneLauncher
import com.sleepcare.watch.data.sensor.CompositeHeartSensorSource
import com.sleepcare.watch.data.sensor.SamsungHealthSensorSource
import com.sleepcare.watch.data.sensor.SensorManagerHeartRateSensorSource
import com.sleepcare.watch.data.transport.CommandBus
import com.sleepcare.watch.data.transport.WearDataLayerGateway
import com.sleepcare.watch.domain.session.WatchSessionRepository
import com.sleepcare.watch.protocol.ProtocolCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

data class WatchRuntime(
    val scope: CoroutineScope,
    val codec: ProtocolCodec,
    val commandBus: CommandBus,
    val repository: WatchSessionRepository,
) {
    companion object {
        fun create(context: Context): WatchRuntime {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            val codec = ProtocolCodec()
            val commandBus = CommandBus()
            val gateway = WearDataLayerGateway(context, codec)
            val sensorSource = CompositeHeartSensorSource(
                samsungSource = SamsungHealthSensorSource(context),
                fallbackSource = SensorManagerHeartRateSensorSource(context),
            )
            val repository = WatchSessionRepository(
                appContext = context.applicationContext,
                scope = scope,
                commandBus = commandBus,
                gateway = gateway,
                sensorSource = sensorSource,
                phoneLauncher = WearPhoneLauncher(context),
                vibrationController = AndroidVibrationController(context),
            )
            return WatchRuntime(
                scope = scope,
                codec = codec,
                commandBus = commandBus,
                repository = repository,
            )
        }
    }
}

