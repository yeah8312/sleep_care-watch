package com.sleepcare.watch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepcare.watch.ui.components.SleepCareHeader
import com.sleepcare.watch.ui.components.SleepCareWatchFrame
import com.sleepcare.watch.ui.components.StatusPill
import com.sleepcare.watch.ui.components.WatchActionButton
import com.sleepcare.watch.ui.components.WatchInfoCard
import com.sleepcare.watch.ui.components.WatchSecondaryActionButton

@Composable
fun ConnectionWaitingScreen(
    state: ConnectionWaitingUiState,
    onOpenOnPhone: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SleepCareWatchFrame(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SleepCareHeader(
                title = "Waiting for phone",
                subtitle = "Wear app is ready to sync.",
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusPill(
                text = if (state.phoneConnected) "Phone connected" else "Phone offline",
                accent = if (state.phoneConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            WatchInfoCard(
                title = "Phone link",
                value = state.phoneConnectionStatus,
                subtitle = state.lastAttemptStatus,
                accent = if (state.phoneConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            WatchInfoCard(
                title = "Session",
                value = state.sessionStatus,
                subtitle = if (state.sessionReady) "Ready to start" else "Waiting for start command",
            )
            Text(
                text = "Open the phone app to start or resume your session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            WatchActionButton(
                text = state.primaryActionLabel,
                onClick = onOpenOnPhone,
                enabled = true,
            )
            WatchSecondaryActionButton(
                text = "Settings",
                onClick = onOpenSettings,
            )
        }
    }
}
