package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositionAnalyzerTest {
    @Test
    fun scoreIsBounded() {
        val result = CompositionAnalyzer.analyze(
            CompositionInput(0.5, 0.5, 0.2, horizonTiltDegrees = 1.0)
        )
        assertTrue(result.score in 0..100)
    }

    @Test
    fun severeTiltProducesLevelingSuggestion() {
        val result = CompositionAnalyzer.analyze(
            CompositionInput(0.66, 0.33, 0.2, horizonTiltDegrees = 12.0)
        )
        assertEquals(0, result.levelScore)
        assertTrue(result.suggestions.any { it.contains("수평계") })
    }

    @Test
    fun allInputValuesAreSafelyClamped() {
        val result = CompositionAnalyzer.analyze(
            CompositionInput(-4.0, 3.0, 2.0, horizonTiltDegrees = 90.0)
        )
        assertTrue(result.score in 0..100)
        assertTrue(result.framingScore in 0..100)
    }
}
