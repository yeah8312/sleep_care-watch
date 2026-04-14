package com.sleepcare.watch.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SleepCareColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFBDC2FF),
    onPrimary = Color(0xFF1B247F),
    primaryContainer = Color(0xFF1A237E),
    onPrimaryContainer = Color(0xFFBDC2FF),
    secondary = Color(0xFFBAC3FF),
    onSecondary = Color(0xFF08218A),
    secondaryContainer = Color(0xFF2C3EA3),
    onSecondaryContainer = Color(0xFFA8B4FF),
    tertiary = Color(0xFF44D8F1),
    onTertiary = Color(0xFF00363E),
    tertiaryContainer = Color(0xFF00353D),
    onTertiaryContainer = Color(0xFFA1EFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111415),
    onBackground = Color(0xFFE2E2E4),
    surface = Color(0xFF111415),
    onSurface = Color(0xFFE2E2E4),
    surfaceVariant = Color(0xFF333537),
    onSurfaceVariant = Color(0xFFC6C5D4),
    outline = Color(0xFF908F9D),
    outlineVariant = Color(0xFF454652),
    surfaceContainer = Color(0xFF1E2021),
    surfaceContainerLow = Color(0xFF1A1C1D),
    surfaceContainerHigh = Color(0xFF282A2C),
    surfaceContainerHighest = Color(0xFF333537),
)

private val SleepCareShapes = Shapes()

@Composable
fun SleepCareWatchTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SleepCareColorScheme,
        shapes = SleepCareShapes,
        content = content,
    )
}
