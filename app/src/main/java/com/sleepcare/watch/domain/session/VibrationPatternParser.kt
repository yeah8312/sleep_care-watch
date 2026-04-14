package com.sleepcare.watch.domain.session

object VibrationPatternParser {
    fun parse(pattern: String): LongArray {
        return pattern.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it >= 0L }
            .toLongArray()
    }
}

