package com.luma.camera.analysis

/** Supported multi-shot capture counts. Each shot is saved as an individual JPEG. */
object CaptureBurstOptions {
    val counts: List<Int> = listOf(1, 3)

    fun normalize(value: Int): Int = counts.minByOrNull { kotlin.math.abs(it - value) } ?: 1
}
