package com.sleepcare.watch.domain.session

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sleepcare.watch.data.device.PhoneLauncher
import com.sleepcare.watch.data.device.VibrationController
import com.sleepcare.watch.data.sensor.HeartSensorSource
import com.sleepcare.watch.data.session.WatchSessionForegroundService
import com.sleepcare.watch.data.transport.CommandBus
import com.sleepcare.watch.data.transport.WearDataLayerGateway
import com.sleepcare.watch.domain.model.AlertState
import com.sleepcare.watch.domain.model.AppSessionState
import com.sleepcare.watch.domain.model.FlushPolicy
import com.sleepcare.watch.domain.model.HeartQualityLabel
import com.sleepcare.watch.domain.model.HeartSample
import com.sleepcare.watch.domain.model.PhoneConnectionState
import com.sleepcare.watch.domain.model.RawHeartReading
import com.sleepcare.watch.domain.model.SensorAvailability
import com.sleepcare.watch.domain.model.SessionContext
import com.sleepcare.watch.domain.model.SleepSyncState
import com.sleepcare.watch.domain.model.VibrationResult
import com.sleepcare.watch.domain.model.WatchUiState
import com.sleepcare.watch.protocol.IncomingCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchSessionRepository(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val commandBus: CommandBus,
    private val gateway: WearDataLayerGateway,
    private val sensorSource: HeartSensorSource,
    private val phoneLauncher: PhoneLauncher,
    private val vibrationController: VibrationController,
) {
    private val mutableUiState = MutableStateFlow(WatchUiState())
    val uiState: StateFlow<WatchUiState> = mutableUiState.asStateFlow()

    private val sampleBuffer = SampleBuffer()
    private var sampleJob: Job? = null
    private var batchJob: Job? = null
    private var nextSampleSeq = 1L

    init {
        scope.launch {
            refreshCapabilities()
            commandBus.commands.collect(::handleCommand)
        }
    }

    suspend fun refreshCapabilities() {
        val hasBodySensorPermission = hasPermission(Manifest.permission.BODY_SENSORS)
        val hasActivityRecognitionPermission = hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        val sensorAvailability = sensorSource.availability(hasBodySensorPermission)

        mutableUiState.value = mutableUiState.value.copy(
            hasBodySensorPermission = hasBodySensorPermission,
            hasActivityRecognitionPermission = hasActivityRecognitionPermission,
            sensorAvailability = sensorAvailability,
            appState = when {
                mutableUiState.value.sessionContext != null && sensorAvailability == SensorAvailability.PERMISSION_REQUIRED ->
                    AppSessionState.ERROR_PERMISSION
                mutableUiState.value.sessionContext != null && sensorAvailability == SensorAvailability.UNSUPPORTED ->
                    AppSessionState.ERROR_SENSOR
                mutableUiState.value.sessionContext == null -> AppSessionState.WAITING_PHONE
                mutableUiState.value.appState == AppSessionState.ALERTING -> AppSessionState.ALERTING
                mutableUiState.value.phoneConnectionState == PhoneConnectionState.DISCONNECTED -> AppSessionState.DEGRADED
                else -> AppSessionState.RUNNING
            },
        )
    }

    fun dismissAlert() {
        val state = mutableUiState.value
        val alert = state.alertState ?: return
        val nextAppState = when {
            state.sessionContext == null -> AppSessionState.WAITING_PHONE
            state.phoneConnectionState == PhoneConnectionState.DISCONNECTED -> AppSessionState.DEGRADED
            else -> AppSessionState.RUNNING
        }
        mutableUiState.value = state.copy(
            appState = nextAppState,
            alertState = alert.copy(acknowledgedLocally = true),
        )
        scope.launch {
            publishStatus()
        }
    }

    fun showSettings() {
        mutableUiState.value = mutableUiState.value.copy(settingsVisible = true)
    }

    fun hideSettings() {
        mutableUiState.value = mutableUiState.value.copy(settingsVisible = false)
    }

    suspend fun openOnPhone() {
        val success = phoneLauncher.openOnPhone()
        mutableUiState.value = mutableUiState.value.copy(
            lastConnectionAttempt = if (success) {
                "Open on phone requested"
            } else {
                "Phone launch failed"
            },
        )
    }

    fun stopFromWatch() {
        scope.launch {
            stopSession(reason = "watch_user_stop")
        }
    }

    fun retrySync() {
        scope.launch {
            publishStatus()
            flushAndSendPending()
        }
    }

    private suspend fun handleCommand(command: IncomingCommand) {
        mutableUiState.value = mutableUiState.value.copy(
            phoneConnectionState = PhoneConnectionState.CONNECTED,
            lastConnectionAttempt = "Phone command received",
        )

        when (command) {
            is IncomingCommand.StartSession -> startSession(command.context, command.flushPolicy)
            is IncomingCommand.StopSession -> stopSession(command.reason)
            is IncomingCommand.UpdateFlushPolicy -> updateFlushPolicy(command.flushPolicy)
            is IncomingCommand.RequestBackfill -> sendBackfill(command.fromSampleSeq)
            is IncomingCommand.TriggerAlert -> triggerAlert(command.alert)
            is IncomingCommand.AckReceived -> acknowledge(command.ackSampleSeq)
        }
    }

    private suspend fun startSession(
        context: SessionContext,
        flushPolicy: FlushPolicy,
    ) {
        mutableUiState.value = mutableUiState.value.copy(
            appState = AppSessionState.STARTING,
            sessionContext = context,
            sessionStartedAtMs = System.currentTimeMillis(),
            flushPolicy = flushPolicy,
            lastErrorMessage = null,
            phoneConnectionState = PhoneConnectionState.CONNECTED,
            settingsVisible = false,
        )

        refreshCapabilities()
        val state = mutableUiState.value
        when (state.sensorAvailability) {
            SensorAvailability.PERMISSION_REQUIRED -> {
                setErrorState(AppSessionState.ERROR_PERMISSION, "Sensor permission is required")
                return
            }

            SensorAvailability.UNSUPPORTED -> {
                setErrorState(AppSessionState.ERROR_SENSOR, "Heart rate sensor is not supported")
                return
            }

            else -> Unit
        }

        sampleBuffer.acknowledgeThrough(Long.MAX_VALUE)
        nextSampleSeq = 1L
        WatchSessionForegroundService.start(appContext)
        startSensorCollection()
        restartBatchLoop()
        mutableUiState.value = mutableUiState.value.copy(appState = AppSessionState.RUNNING)
        publishStatus()
    }

    private suspend fun updateFlushPolicy(flushPolicy: FlushPolicy) {
        mutableUiState.value = mutableUiState.value.copy(flushPolicy = flushPolicy)
        restartBatchLoop()
        publishStatus()
    }

    private suspend fun stopSession(reason: String?) {
        val state = mutableUiState.value
        if (state.sessionContext == null) {
            mutableUiState.value = state.copy(appState = AppSessionState.WAITING_PHONE)
            return
        }

        mutableUiState.value = state.copy(appState = AppSessionState.STOPPING)
        sensorSource.flush()
        flushAndSendPending()
        sampleJob?.cancel()
        batchJob?.cancel()
        WatchSessionForegroundService.stop(appContext)

        mutableUiState.value = WatchUiState(
            appState = AppSessionState.WAITING_PHONE,
            lastConnectionAttempt = reason ?: "Session stopped",
            sensorAvailability = mutableUiState.value.sensorAvailability,
            hasBodySensorPermission = mutableUiState.value.hasBodySensorPermission,
            hasActivityRecognitionPermission = mutableUiState.value.hasActivityRecognitionPermission,
            sleepSyncState = SleepSyncState.PENDING,
            phoneConnectionState = PhoneConnectionState.WAITING,
        )
    }

    private fun startSensorCollection() {
        sampleJob?.cancel()
        sampleJob = scope.launch {
            sensorSource.readings().collect { readings ->
                val samples = readings.map(::toHeartSample)
                if (samples.isEmpty()) return@collect
                sampleBuffer.addAll(samples)

                val latest = samples.last()
                mutableUiState.value = mutableUiState.value.copy(
                    latestSample = latest,
                    lastSampleAtMs = latest.sensorTsMs,
                    bufferedSampleCount = sampleBuffer.size(),
                    appState = if (mutableUiState.value.alertState?.acknowledgedLocally == false) {
                        AppSessionState.ALERTING
                    } else if (mutableUiState.value.phoneConnectionState == PhoneConnectionState.DISCONNECTED) {
                        AppSessionState.DEGRADED
                    } else {
                        AppSessionState.RUNNING
                    },
                )

                val sessionId = mutableUiState.value.sessionContext?.sessionId ?: return@collect
                val sent = gateway.sendLiveSample(sessionId, latest)
                onOutboundAttempt(sent)
            }
        }
    }

    private fun restartBatchLoop() {
        batchJob?.cancel()
        batchJob = scope.launch {
            while (true) {
                val delayMs = mutableUiState.value.flushPolicy.currentIntervalSec(mutableUiState.value.appState) * 1_000L
                delay(delayMs)
                flushAndSendPending()
            }
        }
    }

    private suspend fun flushAndSendPending() {
        val sessionId = mutableUiState.value.sessionContext?.sessionId ?: return
        sensorSource.flush()
        val pending = sampleBuffer.pending()
        if (pending.isEmpty()) {
            publishStatus()
            return
        }

        val sent = gateway.sendBatch(sessionId, pending)
        onOutboundAttempt(sent)
    }

    private suspend fun sendBackfill(fromSampleSeq: Long) {
        val sessionId = mutableUiState.value.sessionContext?.sessionId ?: return
        val missing = sampleBuffer.fromSampleSeq(fromSampleSeq)
        val sent = gateway.sendBatch(sessionId, missing)
        onOutboundAttempt(sent)
    }

    private suspend fun triggerAlert(alert: AlertState) {
        val pattern = VibrationPatternParser.parse(alert.pattern)
        val didVibrate = if (mutableUiState.value.sessionContext?.watchVibrationEnabled == false) {
            false
        } else {
            vibrationController.vibrate(pattern)
        }
        mutableUiState.value = mutableUiState.value.copy(
            appState = AppSessionState.ALERTING,
            alertState = alert.copy(
                vibrationResult = if (didVibrate) VibrationResult.SUCCESS else VibrationResult.FAILED,
            ),
            vibrationFailed = !didVibrate,
            lastErrorMessage = if (didVibrate) null else "Watch vibration failed or was blocked",
        )
        publishStatus()
    }

    private suspend fun acknowledge(ackSampleSeq: Long) {
        sampleBuffer.acknowledgeThrough(ackSampleSeq)
        mutableUiState.value = mutableUiState.value.copy(
            ackSampleSeq = ackSampleSeq,
            lastAckAtMs = System.currentTimeMillis(),
            bufferedSampleCount = sampleBuffer.size(),
            phoneConnectionState = PhoneConnectionState.CONNECTED,
            appState = if (mutableUiState.value.alertState?.acknowledgedLocally == false) {
                AppSessionState.ALERTING
            } else {
                AppSessionState.RUNNING
            },
        )
        publishStatus()
    }

    private suspend fun publishStatus() {
        runCatching {
            gateway.publishStatus(mutableUiState.value)
        }
    }

    private suspend fun onOutboundAttempt(sent: Boolean) {
        val state = mutableUiState.value
        mutableUiState.value = if (sent) {
            state.copy(
                phoneConnectionState = PhoneConnectionState.CONNECTED,
                lastSyncAtMs = System.currentTimeMillis(),
                bufferedSampleCount = sampleBuffer.size(),
                appState = if (state.alertState?.acknowledgedLocally == false) AppSessionState.ALERTING else AppSessionState.RUNNING,
                lastConnectionAttempt = "Phone sync OK",
            )
        } else {
            state.copy(
                phoneConnectionState = PhoneConnectionState.DISCONNECTED,
                appState = AppSessionState.DEGRADED,
                lastConnectionAttempt = "Phone link unavailable, buffering data",
            )
        }
        publishStatus()
    }

    private suspend fun setErrorState(appState: AppSessionState, message: String) {
        mutableUiState.value = mutableUiState.value.copy(
            appState = appState,
            lastErrorMessage = message,
        )
        mutableUiState.value.sessionContext?.sessionId?.let { sessionId ->
            gateway.sendError(sessionId, appState.name, message)
        }
        publishStatus()
    }

    private fun toHeartSample(reading: RawHeartReading): HeartSample {
        return HeartSample(
            sampleSeq = nextSampleSeq++,
            sensorTsMs = reading.sensorTsMs,
            bpm = reading.bpm,
            hrStatus = reading.hrStatus,
            ibiMs = reading.ibiMs,
            ibiStatus = reading.ibiStatus,
            qualityLabel = HeartQualityLabel.fromStatuses(
                hrStatus = reading.hrStatus,
                ibiStatuses = if (reading.ibiStatus.isEmpty()) listOf(0) else reading.ibiStatus,
            ),
        )
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }
}
