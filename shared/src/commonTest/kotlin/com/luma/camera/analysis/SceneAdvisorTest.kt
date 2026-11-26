package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneAdvisorTest {
    @Test
    fun portraitSuggestsZoomFromDetectedFaceArea() {
        val advice = SceneAdvisor.advise(
            SceneAdviceInput(
                intent = ShootingIntent.PORTRAIT,
                subjectAreaRatio = 0.04,
                currentZoomRatio = 1f,
                maxZoomRatio = 4f
            )
        )
        assertEquals(2f, advice.suggestedZoomRatio)
        assertTrue(advice.recommendations.any { it.contains("추천 배율") })
    }

    @Test
    fun zoomRecommendationNeverExceedsCameraCapability() {
        val advice = SceneAdvisor.advise(
            SceneAdviceInput(
                intent = ShootingIntent.PORTRAIT,
                subjectAreaRatio = 0.01,
                currentZoomRatio = 1f,
                maxZoomRatio = 1.5f
            )
        )
        assertEquals(1.5f, advice.suggestedZoomRatio)
    }

    @Test
    fun landscapeDoesNotInventFaceBasedZoom() {
        val advice = SceneAdvisor.advise(
            SceneAdviceInput(
                intent = ShootingIntent.LANDSCAPE,
                subjectAreaRatio = 0.3,
                horizonTiltDegrees = 3.0,
                currentZoomRatio = 2f,
                maxZoomRatio = 8f
            )
        )
        assertNull(advice.suggestedZoomRatio)
        assertTrue(advice.recommendations.any { it.contains("수평") })
    }

    @Test
    fun unsupportedZoomRangeDoesNotOfferZoomAction() {
        val advice = SceneAdvisor.advise(
            SceneAdviceInput(
                intent = ShootingIntent.FOOD,
                subjectAreaRatio = 0.05,
                currentZoomRatio = 1f,
                maxZoomRatio = 1f
            )
        )
        assertNull(advice.suggestedZoomRatio)
        assertTrue(advice.recommendations.isNotEmpty())
    }

    @Test
    fun architectureAdviceMentionsVerticalLines() {
        val advice = SceneAdvisor.advise(
            SceneAdviceInput(intent = ShootingIntent.ARCHITECTURE, horizonTiltDegrees = 2.0)
        )
        assertTrue(advice.recommendations.any { it.contains("수직선") })
    }
}
