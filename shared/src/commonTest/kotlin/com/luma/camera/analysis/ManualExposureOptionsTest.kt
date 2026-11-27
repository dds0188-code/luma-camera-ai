package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManualExposureOptionsTest {
    @Test fun clampsIsoToReportedDeviceRange() {
        assertEquals(50, ManualExposureOptions.normalizeIso(10, 50, 3200))
        assertEquals(1600, ManualExposureOptions.normalizeIso(5000, 50, 1600))
        assertEquals(400, ManualExposureOptions.normalizeIso(400, 50, 1600))
    }

    @Test fun clampsShutterTimeToReportedDeviceRange() {
        assertEquals(2_000_000L, ManualExposureOptions.normalizeExposureTime(1L, 2_000_000L, 1_000_000_000L))
        assertEquals(1_000_000_000L, ManualExposureOptions.normalizeExposureTime(2_000_000_000L, 2_000_000L, 1_000_000_000L))
    }

    @Test fun rejectsInvalidHardwareRanges() {
        assertFailsWith<IllegalArgumentException> { ManualExposureOptions.normalizeIso(100, 0, 3200) }
        assertFailsWith<IllegalArgumentException> { ManualExposureOptions.normalizeExposureTime(10, 0, 100) }
    }
}
