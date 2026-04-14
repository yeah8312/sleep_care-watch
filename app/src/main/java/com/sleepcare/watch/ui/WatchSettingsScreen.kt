package com.sleepcare.watch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepcare.watch.ui.components.SleepCareHeader
import com.sleepcare.watch.ui.components.SleepCareWatchFrame
import com.sleepcare.watch.ui.components.StatusPill
import com.sleepcare.watch.ui.components.WatchActionButton
import com.sleepcare.watch.ui.components.WatchInfoCard
import com.sleepcare.watch.ui.components.WatchSecondaryActionButton

@Composable
fun WatchSettingsScreen(
    state: WatchSettingsUiState,
    onOpenOnPhone: () -> Unit,
    onRequestPermissions: () -> Unit,
    onRetrySync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SleepCareWatchFrame(modifier = modifier) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SleepCareHeader(
                title = "Watch settings",
                subtitle = "Connection, permissions, and sync status.",
            )
            StatusPill(
                text = state.permissionsLabel,
                accent = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = state.permissionHintLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            WatchInfoCard(
                title = "Permissions",
                value = state.permissionsLabel,
                subtitle = "Sensor access required for session capture.",
                accent = MaterialTheme.colorScheme.tertiary,
            )
            WatchInfoCard(
                title = "Sensor",
                value = state.sensorSupportLabel,
                subtitle = "Current device capability check.",
                accent = MaterialTheme.colorScheme.primary,
            )
            WatchInfoCard(
                title = "Sleep Log",
                value = state.sleepLogLabel,
                subtitle = state.syncStatusLabel + " • " + state.lastSyncLabel,
                accent = MaterialTheme.colorScheme.secondary,
            )
            WatchActionButton(
                text = state.openOnPhoneLabel,
                onClick = onOpenOnPhone,
                enabled = true,
            )
            WatchSecondaryActionButton(
                text = "Request permissions",
                onClick = onRequestPermissions,
            )
            WatchSecondaryActionButton(
                text = "Retry sync",
                onClick = onRetrySync,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sleep Care keeps the watch lightweight and uses the phone for deep sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
