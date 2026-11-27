package com.luma.camera.analysis

/** Camera2 white-balance targets exposed only when the active lens reports support. */
enum class WhiteBalancePreset(val label: String) {
    AUTO("자동"),
    INCANDESCENT("백열등"),
    FLUORESCENT("형광등"),
    WARM_FLUORESCENT("온백색"),
    DAYLIGHT("주광"),
    CLOUDY_DAYLIGHT("흐림"),
    TWILIGHT("황혼"),
    SHADE("그늘")
}
