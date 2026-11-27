package com.luma.camera.analysis

/** Validates manual lens-focus requests using the active lens' reported diopter range. */
object ManualFocusOptions {
    fun normalizeDiopters(requested: Float, maximum: Float): Float {
        require(maximum.isFinite() && maximum > 0f) { "렌즈 초점 범위가 올바르지 않습니다." }
        require(requested.isFinite()) { "초점 값이 올바르지 않습니다." }
        return requested.coerceIn(0f, maximum)
    }
}
