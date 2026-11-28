package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneClassifierTest {
    @Test
    fun recognizesFoodFromStrongOnDeviceLabels() {
        val result = SceneClassifier.classify(listOf(SceneLabelInput("Food", 0.91), SceneLabelInput("Dish", 0.72)))
        assertEquals(ShootingIntent.FOOD, result?.intent)
        assertTrue((result?.confidence ?: 0.0) > 0.91)
    }

    @Test
    fun recognizesArchitectureAndLandscapeSeparately() {
        assertEquals(ShootingIntent.ARCHITECTURE, SceneClassifier.classify(listOf(SceneLabelInput("Building", 0.88)))?.intent)
        assertEquals(ShootingIntent.LANDSCAPE, SceneClassifier.classify(listOf(SceneLabelInput("Mountain", 0.84)))?.intent)
    }

    @Test
    fun rejectsWeakAmbiguousOrInvalidLabels() {
        assertNull(SceneClassifier.classify(listOf(SceneLabelInput("Food", 0.42))))
        assertNull(SceneClassifier.classify(listOf(SceneLabelInput("Food", 0.8), SceneLabelInput("Person", 0.77))))
        assertNull(SceneClassifier.classify(listOf(SceneLabelInput("", 0.9), SceneLabelInput("Tree", Double.NaN))))
    }

    @Test
    fun combinesSeveralRelatedLabelsForMoreStableSceneRecognition() {
        val result = SceneClassifier.classify(listOf(
            SceneLabelInput("Tree", 0.61),
            SceneLabelInput("Forest", 0.59),
            SceneLabelInput("Mountain", 0.57)
        ))
        assertEquals(ShootingIntent.LANDSCAPE, result?.intent)
        assertEquals(3, result?.matchedLabels?.size)
    }

    @Test
    fun recognizesCommonLabelVariants() {
        assertEquals(ShootingIntent.FOOD, SceneClassifier.classify(listOf(SceneLabelInput("coffee", 0.86)))?.intent)
        assertEquals(ShootingIntent.ARCHITECTURE, SceneClassifier.classify(listOf(SceneLabelInput("cityscape", 0.87)))?.intent)
        assertEquals(ShootingIntent.PORTRAIT, SceneClassifier.classify(listOf(SceneLabelInput("selfie", 0.89)))?.intent)
    }

    @Test
    fun ignoresUnsupportedGenericLabels() {
        assertNull(SceneClassifier.classify(listOf(SceneLabelInput("Object", 0.99))))
    }
}
