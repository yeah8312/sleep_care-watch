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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepcare.watch.ui.components.SleepCareHeader
import com.sleepcare.watch.ui.components.SleepCareWatchFrame
import com.sleepcare.watch.ui.components.StatusPill
import com.sleepcare.watch.ui.components.WatchActionButton
import com.sleepcare.watch.ui.components.WatchInfoCard
import com.sleepcare.watch.ui.components.WatchSecondaryActionButton

@Composable
fun AlertingScreen(
    state: AlertingUiState,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SleepCareWatchFrame(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SleepCareHeader(
                title = "Alerting",
                subtitle = "Local vibration confirmation only.",
            )
            StatusPill(
                text = state.levelLabel,
                accent = MaterialTheme.colorScheme.error,
            )
            WatchInfoCard(
                title = "Reason",
                value = state.reasonLabel,
                subtitle = if (state.didVibrate) "Vibration was sent to the watch" else "No vibration executed yet",
                accent = MaterialTheme.colorScheme.error,
            )
            WatchInfoCard(
                title = "Vibration",
                value = state.vibrationStatusLabel,
                subtitle = "Dismiss only closes this screen locally.",
                accent = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = "If the risk continues, the alert can return from the phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            WatchActionButton(
                text = state.dismissLabel,
                onClick = onDismiss,
            )
            WatchSecondaryActionButton(
                text = "Settings",
                onClick = onOpenSettings,
            )
        }
    }
}
