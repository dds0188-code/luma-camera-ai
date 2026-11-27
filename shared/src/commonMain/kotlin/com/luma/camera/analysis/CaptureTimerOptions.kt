package com.luma.camera.analysis

/** Supported self-timer presets shared by the camera UI and platform capture layer. */
object CaptureTimerOptions {
    val seconds: List<Int> = listOf(0, 3, 10)

    fun normalize(value: Int): Int = seconds.minByOrNull { kotlin.math.abs(it - value) } ?: 0
}
