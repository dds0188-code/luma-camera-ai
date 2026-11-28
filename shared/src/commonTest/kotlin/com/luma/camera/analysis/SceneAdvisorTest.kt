package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneAdvisorTest {
    @Test fun portraitSuggestsZoomFromDetectedFaceArea() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.PORTRAIT, subjectAreaRatio = 0.04, currentZoomRatio = 1f, minZoomRatio = 0.5f, maxZoomRatio = 4f))
        assertEquals(2f, advice.suggestedZoomRatio ?: Float.NaN)
        assertTrue(advice.recommendations.any { it.contains("추천 배율") })
    }

    @Test fun zoomRecommendationNeverExceedsCameraCapability() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.PORTRAIT, subjectAreaRatio = 0.01, currentZoomRatio = 1f, minZoomRatio = 0.5f, maxZoomRatio = 1.5f))
        assertEquals(1.5f, advice.suggestedZoomRatio ?: Float.NaN)
    }

    @Test fun zoomRecommendationRespectsDeviceMinimumBelowOneX() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.PORTRAIT, subjectAreaRatio = 0.8, currentZoomRatio = 1f, minZoomRatio = 0.5f, maxZoomRatio = 4f))
        assertEquals(0.5f, advice.suggestedZoomRatio ?: Float.NaN)
    }

    @Test fun landscapeDoesNotInventFaceBasedZoom() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.LANDSCAPE, subjectAreaRatio = 0.3, horizonTiltDegrees = 3.0, currentZoomRatio = 2f, maxZoomRatio = 8f))
        assertNull(advice.suggestedZoomRatio)
        assertTrue(advice.recommendations.any { it.contains("수평") })
        assertTrue(advice.recommendations.any { it.contains("전경") })
    }

    @Test fun unsupportedZoomRangeDoesNotOfferZoomAction() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.FOOD, subjectAreaRatio = 0.05, currentZoomRatio = 1f, maxZoomRatio = 1f))
        assertNull(advice.suggestedZoomRatio)
        assertTrue(advice.recommendations.isNotEmpty())
    }

    @Test fun foodModeDoesNotMistakeFaceDetectionForFoodDetection() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.FOOD, subjectAreaRatio = 0.01, currentZoomRatio = 1f, maxZoomRatio = 8f))
        assertNull(advice.suggestedZoomRatio)
    }

    @Test fun portraitAdviceRespondsToCameraPitchAndFaceScale() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.PORTRAIT, subjectAreaRatio = 0.02, cameraPitchDegrees = 12.0, currentZoomRatio = 1f, maxZoomRatio = 4f))
        assertTrue(advice.recommendations.any { it.contains("아래를 향하고") })
        assertTrue(advice.recommendations.any { it.contains("얼굴이 작게") })
        assertEquals(2.83f, advice.suggestedZoomRatio ?: Float.NaN, 0.02f)
    }

    @Test fun foodAdviceAdaptsAngleHintToPitch() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.FOOD, cameraPitchDegrees = 60.0))
        assertTrue(advice.recommendations.any { it.contains("탑뷰") })
    }

    @Test fun architectureAdviceMentionsVerticalLinesAndPerspective() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.ARCHITECTURE, horizonTiltDegrees = 2.0, cameraPitchDegrees = -12.0))
        assertTrue(advice.recommendations.any { it.contains("수직선") })
        assertTrue(advice.recommendations.any { it.contains("수렴") })
    }

    @Test fun portraitAdviceChangesWithFacePosition() {
        val centered = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.PORTRAIT, subjectCenterX = 0.5, subjectAreaRatio = 0.12))
        val left = SceneAdvisor.advise(SceneAdviceInput(ShootingIntent.PORTRAIT, subjectCenterX = 0.12, subjectAreaRatio = 0.12))
        assertTrue(centered.recommendations.any { it.contains("삼분할 위치") })
        assertTrue(left.recommendations.any { it.contains("가장자리") })
    }

    @Test fun invalidSensorAndZoomValuesFallBackSafely() {
        val advice = SceneAdvisor.advise(SceneAdviceInput(
            intent = ShootingIntent.PORTRAIT, subjectCenterX = Double.NaN,
            subjectCenterY = Double.POSITIVE_INFINITY, subjectAreaRatio = Double.NaN,
            horizonTiltDegrees = Double.NaN, cameraPitchDegrees = Double.NaN,
            currentZoomRatio = Float.NaN, maxZoomRatio = Float.NaN
        ))
        assertNull(advice.suggestedZoomRatio)
        assertTrue(advice.recommendations.isNotEmpty())
    }
}
