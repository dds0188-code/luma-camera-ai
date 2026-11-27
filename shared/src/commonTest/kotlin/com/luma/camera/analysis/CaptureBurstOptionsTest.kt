package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureBurstOptionsTest {
    @Test
    fun exposesSingleAndThreeShotModes() {
        assertEquals(listOf(1, 3), CaptureBurstOptions.counts)
    }

    @Test
    fun normalizesUnsupportedShotCounts() {
        assertEquals(1, CaptureBurstOptions.normalize(0))
        assertEquals(1, CaptureBurstOptions.normalize(2))
        assertEquals(3, CaptureBurstOptions.normalize(4))
    }
}
