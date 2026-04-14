package com.sleepcare.watch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sleepcare.watch.WatchViewModel
import com.sleepcare.watch.domain.model.AppSessionState
import com.sleepcare.watch.domain.model.PhoneConnectionState
import com.sleepcare.watch.domain.model.SensorAvailability
import com.sleepcare.watch.domain.model.SleepSyncState
import com.sleepcare.watch.domain.model.VibrationResult
import com.sleepcare.watch.domain.model.WatchUiState
import com.sleepcare.watch.ui.theme.SleepCareWatchTheme

@Composable
fun WatchApp(
    viewModel: WatchViewModel,
    onRequestPermissions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    SleepCareWatchTheme {
        if (state.settingsVisible) {
            WatchSettingsScreen(
                state = state.toSettingsUiState(),
                onOpenOnPhone = viewModel::openOnPhone,
                onRequestPermissions = onRequestPermissions,
                onRetrySync = viewModel::retrySync,
            )
            return@SleepCareWatchTheme
        }

        if (state.isAlertVisible) {
            AlertingScreen(
                state = state.toAlertingUiState(),
                onDismiss = viewModel::dismissAlert,
                onOpenSettings = viewModel::openSettings,
            )
            return@SleepCareWatchTheme
        }

        when {
            state.sessionContext != null &&
                state.appState !in setOf(
                    AppSessionState.WAITING_PHONE,
                    AppSessionState.IDLE,
                ) -> {
                ActiveSessionScreen(
                    state = state.toActiveSessionUiState(),
                    onStopSession = viewModel::stopSession,
                    onOpenSettings = viewModel::openSettings,
                )
            }

            else -> {
                ConnectionWaitingScreen(
                    state = state.toConnectionWaitingUiState(),
                    onOpenOnPhone = viewModel::openOnPhone,
                    onOpenSettings = viewModel::openSettings,
                )
            }
        }
    }
}

private fun WatchUiState.toConnectionWaitingUiState(): ConnectionWaitingUiState {
    return ConnectionWaitingUiState(
        phoneConnected = phoneConnectionState == PhoneConnectionState.CONNECTED,
        sessionReady = sessionContext == null,
        phoneConnectionStatus = when (phoneConnectionState) {
            PhoneConnectionState.CONNECTED -> "Phone connected"
            PhoneConnectionState.DISCONNECTED -> "Phone disconnected"
            PhoneConnectionState.WAITING -> "Waiting for phone"
        },
        sessionStatus = when (appState) {
            AppSessionState.ERROR_PERMISSION -> "Permission required"
            AppSessionState.ERROR_SENSOR -> "Sensor unavailable"
            AppSessionState.ERROR_LINK -> "Link error"
            AppSessionState.DEGRADED -> "Running without phone sync"
            else -> "Session idle"
        },
        lastAttemptStatus = lastConnectionAttempt,
    )
}

private fun WatchUiState.toActiveSessionUiState(): ActiveSessionUiState {
    val now = System.currentTimeMillis()
    return ActiveSessionUiState(
        sessionStateLabel = appState.name.replace('_', ' '),
        bpm = latestSample?.bpm,
        ibiMs = latestSample?.ibiMs?.firstOrNull(),
        sensorStatusLabel = when (sensorAvailability) {
            SensorAvailability.AVAILABLE -> "Sensor stable"
            SensorAvailability.PERMISSION_REQUIRED -> "Permission required"
            SensorAvailability.UNSUPPORTED -> "Sensor unavailable"
            SensorAvailability.UNKNOWN -> "Checking sensor"
        },
        phoneLinkStatusLabel = when (phoneConnectionState) {
            PhoneConnectionState.CONNECTED -> "Phone connected"
            PhoneConnectionState.DISCONNECTED -> "Buffered locally"
            PhoneConnectionState.WAITING -> "Waiting for phone"
        },
        lastSentLabel = "Last send ${formatRelative(now, lastSyncAtMs)}",
        lastAckLabel = "Last ACK ${formatRelative(now, lastAckAtMs)}",
        qualityLabel = latestSample?.qualityLabel?.wireValue ?: "busy_or_initial",
        elapsedLabel = sessionStartedAtMs?.let { formatElapsed(now - it) } ?: "--:--:--",
    )
}

private fun WatchUiState.toAlertingUiState(): AlertingUiState {
    val alert = alertState
    return AlertingUiState(
        levelLabel = if (alert == null) "Alert" else "Level ${alert.level}",
        reasonLabel = alert?.reason ?: "Watch alert received",
        vibrationStatusLabel = when (alert?.vibrationResult ?: VibrationResult.NOT_ATTEMPTED) {
            VibrationResult.SUCCESS -> "Vibration completed"
            VibrationResult.FAILED -> "Vibration failed"
            VibrationResult.NOT_ATTEMPTED -> "Waiting for vibration"
        },
        didVibrate = alert?.vibrationResult == VibrationResult.SUCCESS,
    )
}

private fun WatchUiState.toSettingsUiState(): WatchSettingsUiState {
    return WatchSettingsUiState(
        permissionsLabel = when {
            hasBodySensorPermission && hasActivityRecognitionPermission -> "Granted"
            hasBodySensorPermission || hasActivityRecognitionPermission -> "Partial"
            else -> "Missing"
        },
        sensorSupportLabel = when (sensorAvailability) {
            SensorAvailability.AVAILABLE -> "Supported"
            SensorAvailability.PERMISSION_REQUIRED -> "Permission required"
            SensorAvailability.UNSUPPORTED -> "Unsupported"
            SensorAvailability.UNKNOWN -> "Checking"
        },
        syncStatusLabel = when (phoneConnectionState) {
            PhoneConnectionState.CONNECTED -> "In sync"
            PhoneConnectionState.DISCONNECTED -> "Buffered offline"
            PhoneConnectionState.WAITING -> "Waiting for phone"
        },
        lastSyncLabel = "Last sync ${formatRelative(System.currentTimeMillis(), lastSyncAtMs)}",
        sleepLogLabel = when (sleepSyncState) {
            SleepSyncState.SYNCED -> "Connected on phone"
            SleepSyncState.LIMITED -> "Limited on phone"
            SleepSyncState.PENDING -> "Pending on phone"
        },
        permissionHintLabel = when {
            !hasBodySensorPermission || !hasActivityRecognitionPermission ->
                "Grant watch permissions to capture heart rate during a study session."
            vibrationFailed -> "The last vibration failed. Check DND or system haptics."
            else -> "Permissions and sensor access are ready."
        },
    )
}

private fun formatRelative(now: Long, timestamp: Long?): String {
    if (timestamp == null) return "not yet"
    val diffSeconds = ((now - timestamp).coerceAtLeast(0L) / 1_000L).toInt()
    return when {
        diffSeconds < 5 -> "just now"
        diffSeconds < 60 -> "${diffSeconds}s ago"
        diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
        else -> "${diffSeconds / 3600}h ago"
    }
}

private fun formatElapsed(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1_000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

