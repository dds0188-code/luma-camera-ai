package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaptureTimerOptionsTest {
    @Test
    fun exposesOnlySupportedPresets() {
        assertEquals(listOf(0, 3, 10), CaptureTimerOptions.seconds)
    }

    @Test
    fun normalizesUnsupportedValuesToNearestPreset() {
        assertEquals(0, CaptureTimerOptions.normalize(-2))
        assertEquals(3, CaptureTimerOptions.normalize(4))
        assertEquals(10, CaptureTimerOptions.normalize(9))
    }

    @Test
    fun allPresetsNormalizeToThemselves() {
        CaptureTimerOptions.seconds.forEach { assertEquals(it, CaptureTimerOptions.normalize(it)) }
        assertTrue(CaptureTimerOptions.seconds.all { it in 0..10 })
    }
}
