package com.sleepcare.watch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.sleepcare.watch.ui.components.StatusRing
import com.sleepcare.watch.ui.components.WatchActionButton
import com.sleepcare.watch.ui.components.WatchInfoCard
import com.sleepcare.watch.ui.components.WatchSecondaryActionButton

@Composable
fun ActiveSessionScreen(
    state: ActiveSessionUiState,
    onStopSession: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SleepCareWatchFrame(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SleepCareHeader(
                title = "Active session",
                subtitle = "Live heart rate and link status.",
            )
            StatusPill(
                text = state.sessionStateLabel,
                accent = MaterialTheme.colorScheme.tertiary,
            )
            StatusRing(
                valueText = state.bpm?.toString() ?: "--",
                labelText = "BPM",
                secondary = "IBI ${state.ibiMs?.let { "${it} ms" } ?: "--"}",
                accent = MaterialTheme.colorScheme.primary,
                progress = when {
                    state.qualityLabel.equals("ok", ignoreCase = true) -> 0.82f
                    state.qualityLabel.equals("motion_or_weak", ignoreCase = true) -> 0.56f
                    state.qualityLabel.equals("detached", ignoreCase = true) -> 0.24f
                    else -> 0.42f
                },
            )
            Text(
                text = state.elapsedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            WatchInfoCard(
                title = "Sensor",
                value = state.sensorStatusLabel,
                subtitle = "Quality: ${state.qualityLabel}",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
            WatchInfoCard(
                title = "Phone link",
                value = state.phoneLinkStatusLabel,
                subtitle = state.lastSentLabel + " • " + state.lastAckLabel,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Keep the watch on and the phone nearby for stable sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            WatchActionButton(
                text = "Stop session",
                onClick = onStopSession,
            )
            WatchSecondaryActionButton(
                text = "Settings",
                onClick = onOpenSettings,
            )
        }
    }
}
