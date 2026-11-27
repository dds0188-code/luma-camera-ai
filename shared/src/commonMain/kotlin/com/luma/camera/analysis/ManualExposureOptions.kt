package com.luma.camera.analysis

/** Safe normalization helpers for optional Camera2 manual sensor controls. */
object ManualExposureOptions {
    val shutterPresetsNanos: List<Long> = listOf(
        33_333_333L, 16_666_667L, 8_000_000L, 4_000_000L, 2_000_000L
    )

    fun normalizeIso(requested: Int, minimum: Int, maximum: Int): Int {
        require(minimum > 0 && maximum >= minimum) { "ISO 범위가 올바르지 않습니다." }
        return requested.coerceIn(minimum, maximum)
    }

    fun normalizeExposureTime(requestedNanos: Long, minimumNanos: Long, maximumNanos: Long): Long {
        require(minimumNanos > 0L && maximumNanos >= minimumNanos) { "셔터 시간 범위가 올바르지 않습니다." }
        return requestedNanos.coerceIn(minimumNanos, maximumNanos)
    }
}
