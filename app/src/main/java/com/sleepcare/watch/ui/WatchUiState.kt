package com.sleepcare.watch.ui

import androidx.compose.runtime.Immutable

@Immutable
data class ConnectionWaitingUiState(
    val phoneConnected: Boolean = false,
    val sessionReady: Boolean = false,
    val phoneConnectionStatus: String = "Waiting for phone",
    val sessionStatus: String = "Session idle",
    val lastAttemptStatus: String = "No connection attempt yet",
    val primaryActionLabel: String = "Open on Phone",
)

@Immutable
data class ActiveSessionUiState(
    val sessionStateLabel: String = "Running",
    val bpm: Int? = null,
    val ibiMs: Int? = null,
    val sensorStatusLabel: String = "Sensor stable",
    val phoneLinkStatusLabel: String = "Phone connected",
    val lastSentLabel: String = "Live sync just now",
    val lastAckLabel: String = "ACK pending",
    val qualityLabel: String = "ok",
    val elapsedLabel: String = "00:00:00",
)

@Immutable
data class AlertingUiState(
    val levelLabel: String = "Warning",
    val reasonLabel: String = "Drowsiness risk detected",
    val vibrationStatusLabel: String = "Vibration completed",
    val didVibrate: Boolean = true,
    val dismissLabel: String = "Dismiss",
)

@Immutable
data class WatchSettingsUiState(
    val permissionsLabel: String = "Granted",
    val sensorSupportLabel: String = "Supported",
    val syncStatusLabel: String = "In sync",
    val lastSyncLabel: String = "Last sync just now",
    val sleepLogLabel: String = "Connected on phone",
    val openOnPhoneLabel: String = "Open on Phone",
    val permissionHintLabel: String = "Permissions and sensor access are ready.",
)
