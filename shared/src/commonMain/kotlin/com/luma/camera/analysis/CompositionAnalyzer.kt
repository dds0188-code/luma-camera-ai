package com.luma.camera.analysis

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Fast, deterministic composition heuristics. All coordinates are normalized to 0..1.
 * This is a guidance score, not a claim of artistic quality or a trained AI prediction.
 */
data class CompositionInput(
    val subjectCenterX: Double,
    val subjectCenterY: Double,
    val subjectAreaRatio: Double,
    val horizonTiltDegrees: Double = 0.0
)

data class CompositionResult(
    val score: Int,
    val thirdsScore: Int,
    val framingScore: Int,
    val levelScore: Int,
    val suggestions: List<String>
)

object CompositionAnalyzer {
    fun analyze(input: CompositionInput): CompositionResult {
        val x = input.subjectCenterX.coerceIn(0.0, 1.0)
        val y = input.subjectCenterY.coerceIn(0.0, 1.0)
        val area = input.subjectAreaRatio.coerceIn(0.0, 1.0)
        val nearestThirdsDistance = listOf(
            hypot(x - 1.0 / 3.0, y - 1.0 / 3.0),
            hypot(x - 2.0 / 3.0, y - 1.0 / 3.0),
            hypot(x - 1.0 / 3.0, y - 2.0 / 3.0),
            hypot(x - 2.0 / 3.0, y - 2.0 / 3.0)
        ).minOrNull() ?: 0.0

        val thirds = (100.0 * (1.0 - (nearestThirdsDistance / 0.745).coerceIn(0.0, 1.0))).toInt()
        val framing = when {
            area < 0.03 -> 45
            area < 0.10 -> 72
            area <= 0.38 -> 95
            area <= 0.60 -> 78
            else -> 52
        }
        val level = (100.0 * (1.0 - (abs(input.horizonTiltDegrees) / 8.0).coerceIn(0.0, 1.0))).toInt()
        val total = (thirds * 0.40 + framing * 0.35 + level * 0.25).toInt()

        val suggestions = buildList {
            if (thirds < 65) add("피사체를 삼분할 교차점 근처로 옮겨 구도를 비교해 보세요.")
            if (framing < 70) add("피사체 크기를 조절해 주변 여백과 화면 점유율을 확인해 보세요.")
            if (level < 85) add("수평계를 켜고 카메라 기울기를 줄여 보세요.")
            if (isEmpty()) add("현재 프레이밍을 유지하고 다른 화각도 비교해 보세요.")
        }

        return CompositionResult(total.coerceIn(0, 100), thirds, framing, level, suggestions)
    }
}
