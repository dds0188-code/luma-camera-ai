package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManualFocusOptionsTest {
    @Test fun clampsFocusDistanceToLensRange() {
        assertEquals(0f, ManualFocusOptions.normalizeDiopters(-1f, 10f))
        assertEquals(10f, ManualFocusOptions.normalizeDiopters(12f, 10f))
        assertEquals(4.5f, ManualFocusOptions.normalizeDiopters(4.5f, 10f))
    }

    @Test fun rejectsInvalidLensRangeAndNan() {
        assertFailsWith<IllegalArgumentException> { ManualFocusOptions.normalizeDiopters(1f, 0f) }
        assertFailsWith<IllegalArgumentException> { ManualFocusOptions.normalizeDiopters(Float.NaN, 10f) }
    }
}
