package com.sleepcare.watch.data.device

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

interface VibrationController {
    suspend fun vibrate(pattern: LongArray): Boolean
}

class AndroidVibrationController(
    context: Context,
) : VibrationController {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override suspend fun vibrate(pattern: LongArray): Boolean {
        if (!vibrator.hasVibrator()) return false
        if (pattern.isEmpty()) return false
        return runCatching {
            val timings = if (pattern.firstOrNull() == 0L) pattern else longArrayOf(0L, *pattern)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        }.isSuccess
    }
}

