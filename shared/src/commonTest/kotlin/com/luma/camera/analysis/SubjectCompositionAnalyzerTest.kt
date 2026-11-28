package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubjectCompositionAnalyzerTest {
    @Test
    fun recommendsThirdsForOffCenterSubject() {
        val result = SubjectCompositionAnalyzer.analyze(listOf(
            DetectedSubjectInput(0.55, 0.25, 0.85, 0.75, "Plant", 0.88)
        ))
        assertEquals(0.7, result?.centerX ?: 0.0, 0.001)
        assertTrue(result?.guidance?.contains("삼분할") == true)
        assertEquals("Plant", result?.label)
    }

    @Test
    fun warnsWhenSubjectIsTooSmallOrNearFrameEdge() {
        val result = SubjectCompositionAnalyzer.analyze(listOf(
            DetectedSubjectInput(0.0, 0.0, 0.12, 0.12, "Object", 0.9)
        ))
        assertTrue(result?.guidance?.contains("가장자리") == true)
        assertTrue(result?.guidance?.contains("작게") == true)
    }

    @Test
    fun selectsLargestReliableSubject() {
        val result = SubjectCompositionAnalyzer.analyze(listOf(
            DetectedSubjectInput(0.1, 0.1, 0.2, 0.2, "Small", 0.99),
            DetectedSubjectInput(0.3, 0.2, 0.8, 0.8, "Main", 0.75)
        ))
        assertEquals("Main", result?.label)
    }

    @Test
    fun rejectsInvalidAndLowConfidenceBoxes() {
        assertNull(SubjectCompositionAnalyzer.analyze(listOf(
            DetectedSubjectInput(0.2, 0.2, 0.1, 0.1, "Invalid", 0.99),
            DetectedSubjectInput(0.1, 0.1, 0.9, 0.9, "Weak", 0.1),
            DetectedSubjectInput(Double.NaN, 0.1, 0.9, 0.9, "NaN", 0.9)
        )))
    }
}
