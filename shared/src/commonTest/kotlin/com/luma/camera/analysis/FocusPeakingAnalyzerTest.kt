package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FocusPeakingAnalyzerTest {
    @Test fun findsHighContrastEdgesWithinNormalizedBounds() {
        val width = 8
        val height = 8
        val samples = List(width * height) { index ->
            if (index % width >= 4) 220 else 20
        }
        val points = FocusPeakingAnalyzer.analyze(samples, width, height, threshold = 40)
        assertTrue(points.isNotEmpty())
        assertTrue(points.all { it.x in 0f..1f && it.y in 0f..1f })
    }

    @Test fun rotatesAndMirrorsNormalizedCoordinates() {
        val samples = List(25) { index -> if (index % 5 >= 2) 230 else 10 }
        val normal = FocusPeakingAnalyzer.analyze(samples, 5, 5, threshold = 30)
        val rotated = FocusPeakingAnalyzer.analyze(samples, 5, 5, threshold = 30, rotationDegrees = 90, mirrorHorizontally = true)
        assertEquals(normal.size, rotated.size)
        assertTrue(rotated.all { it.x in 0f..1f && it.y in 0f..1f })
        assertTrue(rotated.zip(normal).any { (a, b) -> a.x != b.x || a.y != b.y })
    }

    @Test fun rejectsInvalidGridAndRotation() {
        assertFailsWith<IllegalArgumentException> { FocusPeakingAnalyzer.analyze(emptyList(), 2, 2) }
        assertFailsWith<IllegalArgumentException> { FocusPeakingAnalyzer.analyze(List(9) { 0 }, 3, 3, rotationDegrees = 45) }
    }
}
