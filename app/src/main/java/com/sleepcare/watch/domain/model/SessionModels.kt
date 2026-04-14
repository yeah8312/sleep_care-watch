package com.sleepcare.watch.domain.model

enum class AppSessionState {
    IDLE,
    WAITING_PHONE,
    STARTING,
    RUNNING,
    DEGRADED,
    ALERTING,
    STOPPING,
    ERROR_PERMISSION,
    ERROR_SENSOR,
    ERROR_LINK,
}

enum class PhoneConnectionState {
    WAITING,
    CONNECTED,
    DISCONNECTED,
}

enum class SensorAvailability {
    UNKNOWN,
    AVAILABLE,
    PERMISSION_REQUIRED,
    UNSUPPORTED,
}

enum class VibrationResult {
    NOT_ATTEMPTED,
    SUCCESS,
    FAILED,
}

enum class SleepSyncState {
    PENDING,
    SYNCED,
    LIMITED,
}

enum class HeartQualityLabel(val wireValue: String) {
    OK("ok"),
    MOTION_OR_WEAK("motion_or_weak"),
    DETACHED("detached"),
    BUSY_OR_INITIAL("busy_or_initial"),
    ;

    companion object {
        fun fromStatuses(hrStatus: Int, ibiStatuses: List<Int>): HeartQualityLabel {
            return when {
                hrStatus == 1 && ibiStatuses.all { it == 0 } -> OK
                hrStatus in listOf(-2, -8, -10) -> MOTION_OR_WEAK
                hrStatus == -3 -> DETACHED
                hrStatus in listOf(0, -999) -> BUSY_OR_INITIAL
                else -> BUSY_OR_INITIAL
            }
        }
    }
}

data class FlushPolicy(
    val normalSec: Int = 15,
    val suspectSec: Int = 5,
    val alertSec: Int = 2,
    val recoverySec: Int = 15,
) {
    fun currentIntervalSec(appState: AppSessionState): Int {
        return when (appState) {
            AppSessionState.ALERTING -> alertSec
            AppSessionState.DEGRADED -> suspectSec
            else -> normalSec
        }
    }
}

data class HeartSample(
    val sampleSeq: Long,
    val sensorTsMs: Long,
    val bpm: Int,
    val hrStatus: Int,
    val ibiMs: List<Int>,
    val ibiStatus: List<Int>,
    val qualityLabel: HeartQualityLabel,
)

data class RawHeartReading(
    val sensorTsMs: Long,
    val bpm: Int,
    val hrStatus: Int,
    val ibiMs: List<Int>,
    val ibiStatus: List<Int>,
)

data class SessionContext(
    val sessionId: String,
    val studyMode: String,
    val hrRequired: Boolean,
    val watchVibrationEnabled: Boolean,
)

data class AlertState(
    val level: Int,
    val reason: String,
    val pattern: String,
    val vibrationResult: VibrationResult = VibrationResult.NOT_ATTEMPTED,
    val acknowledgedLocally: Boolean = false,
)

data class WatchUiState(
    val appState: AppSessionState = AppSessionState.WAITING_PHONE,
    val sessionContext: SessionContext? = null,
    val sessionStartedAtMs: Long? = null,
    val latestSample: HeartSample? = null,
    val phoneConnectionState: PhoneConnectionState = PhoneConnectionState.WAITING,
    val sensorAvailability: SensorAvailability = SensorAvailability.UNKNOWN,
    val lastSyncAtMs: Long? = null,
    val lastAckAtMs: Long? = null,
    val lastSampleAtMs: Long? = null,
    val lastConnectionAttempt: String = "Waiting for mobile session",
    val flushPolicy: FlushPolicy = FlushPolicy(),
    val alertState: AlertState? = null,
    val settingsVisible: Boolean = false,
    val sleepSyncState: SleepSyncState = SleepSyncState.PENDING,
    val lastSleepSyncAtMs: Long? = null,
    val vibrationFailed: Boolean = false,
    val lastErrorMessage: String? = null,
    val ackSampleSeq: Long = 0L,
    val bufferedSampleCount: Int = 0,
    val hasBodySensorPermission: Boolean = false,
    val hasActivityRecognitionPermission: Boolean = false,
) {
    val isAlertVisible: Boolean
        get() = alertState != null && !alertState.acknowledgedLocally
}
