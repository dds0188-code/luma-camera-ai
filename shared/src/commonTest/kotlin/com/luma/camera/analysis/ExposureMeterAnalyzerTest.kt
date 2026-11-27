package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExposureMeterAnalyzerTest {
    @Test fun buildsSixteenBinHistogram() {
        val result = ExposureMeterAnalyzer.analyze(listOf(0, 15, 16, 31, 255))
        assertEquals(16, result.histogram.size)
        assertEquals(2, result.histogram[0])
        assertEquals(2, result.histogram[1])
        assertEquals(1, result.histogram[15])
    }

    @Test fun warnsAboutDarkAndBrightClipping() {
        val dark = ExposureMeterAnalyzer.analyze(List(20) { 0 } + List(80) { 100 })
        val bright = ExposureMeterAnalyzer.analyze(List(20) { 255 } + List(80) { 100 })
        assertTrue(dark.warning.contains("어두운"))
        assertTrue(bright.warning.contains("하이라이트"))
    }

    @Test fun rejectsEmptyOrInvalidSamples() {
        assertFailsWith<IllegalArgumentException> { ExposureMeterAnalyzer.analyze(emptyList()) }
        assertFailsWith<IllegalArgumentException> { ExposureMeterAnalyzer.analyze(listOf(-1, 300)) }
    }
}
