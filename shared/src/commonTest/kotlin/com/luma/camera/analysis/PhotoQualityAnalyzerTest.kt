package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PhotoQualityAnalyzerTest {
    @Test
    fun rejectsInvalidGridDimensionsAndSampleCount() {
        assertFailsWith<IllegalArgumentException> {
            PhotoQualityAnalyzer.analyze(PhotoQualityInput(2, 2, listOf(1, 2, 3, 4)))
        }
        assertFailsWith<IllegalArgumentException> {
            PhotoQualityAnalyzer.analyze(PhotoQualityInput(3, 3, List(8) { 100 }))
        }
    }

    @Test
    fun rejectsSamplesOutsideEightBitRange() {
        assertFailsWith<IllegalArgumentException> {
            PhotoQualityAnalyzer.analyze(PhotoQualityInput(3, 3, List(9) { if (it == 0) 256 else 100 }))
        }
    }

    @Test
    fun reportsClippedHighlightsAndShadows() {
        val samples = List(25) { index ->
            when {
                index < 6 -> 0
                index < 12 -> 255
                else -> 110 + (index % 5)
            }
        }
        val result = PhotoQualityAnalyzer.analyze(PhotoQualityInput(5, 5, samples))
        assertTrue(result.darkClippedRatio > 0.0)
        assertTrue(result.brightClippedRatio > 0.0)
        assertTrue(result.suggestions.any { it.contains("어두운") })
        assertTrue(result.suggestions.any { it.contains("밝은") })
    }

    @Test
    fun returnsBoundedScoresForUniformImage() {
        val result = PhotoQualityAnalyzer.analyze(PhotoQualityInput(4, 4, List(16) { 128 }))
        assertEquals(0, result.contrastScore)
        assertEquals(0, result.detailScore)
        assertTrue(result.score in 0..100)
    }

    @Test
    fun detailedTextureScoresHigherThanFlatImage() {
        val flat = PhotoQualityAnalyzer.analyze(PhotoQualityInput(5, 5, List(25) { 120 }))
        val textured = PhotoQualityAnalyzer.analyze(
            PhotoQualityInput(5, 5, List(25) { index -> if ((index + index / 5) % 2 == 0) 30 else 220 })
        )
        assertTrue(textured.detailScore > flat.detailScore)
        assertTrue(textured.contrastScore > flat.contrastScore)
    }
}
