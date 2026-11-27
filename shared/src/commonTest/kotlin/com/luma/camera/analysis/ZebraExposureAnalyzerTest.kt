package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ZebraExposureAnalyzerTest {
    @Test fun marksOnlyPixelsAtOrAboveHighlightThreshold() {
        val marks = ZebraExposureAnalyzer.analyze(
            samples = listOf(244, 245, 255, 10, 245, 200),
            width = 3,
            height = 2,
            threshold = 245
        )
        assertEquals(3, marks.size)
        assertTrue(marks.all { it.x in 0f..1f && it.y in 0f..1f })
    }

    @Test fun appliesRotationAndFrontCameraMirror() {
        val samples = List(16) { 255 }
        val normal = ZebraExposureAnalyzer.analyze(samples, 4, 4)
        val transformed = ZebraExposureAnalyzer.analyze(
            samples, 4, 4, rotationDegrees = 90, mirrorHorizontally = true
        )
        assertEquals(normal.size, transformed.size)
        assertTrue(transformed.zip(normal).any { (a, b) -> a.x != b.x || a.y != b.y })
    }

    @Test fun validatesInputAndBoundsOutput() {
        assertFailsWith<IllegalArgumentException> { ZebraExposureAnalyzer.analyze(emptyList(), 0, 0) }
        assertFailsWith<IllegalArgumentException> { ZebraExposureAnalyzer.analyze(listOf(256), 1, 1) }
        assertEquals(3, ZebraExposureAnalyzer.analyze(List(100) { 255 }, 10, 10, maxMarks = 3).size)
    }
}
