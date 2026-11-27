package com.luma.camera.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneClassifierTest {
    @Test
    fun recognizesFoodFromStrongOnDeviceLabels() {
        val result = SceneClassifier.classify(listOf(SceneLabelInput("Food", 0.91), SceneLabelInput("Dish", 0.72)))
        assertEquals(ShootingIntent.FOOD, result?.intent)
        assertEquals(0.91, result?.confidence)
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
    fun ignoresUnsupportedGenericLabels() {
        assertNull(SceneClassifier.classify(listOf(SceneLabelInput("Object", 0.99))))
    }
}
