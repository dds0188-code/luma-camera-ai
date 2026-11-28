package com.luma.camera.analysis

import kotlin.math.abs

data class DetectedSubjectInput(
    val left: Double,
    val top: Double,
    val right: Double,
    val bottom: Double,
    val label: String? = null,
    val confidence: Double = 0.0
)

data class SubjectCompositionResult(
    val centerX: Double,
    val centerY: Double,
    val areaRatio: Double,
    val label: String?,
    val guidance: String
)

/**
 * Converts normalized on-device object boxes into simple, explainable framing advice.
 * This is geometric guidance, not an aesthetic-quality prediction.
 */
object SubjectCompositionAnalyzer {
    fun analyze(subjects: List<DetectedSubjectInput>): SubjectCompositionResult? {
        val valid = subjects.mapNotNull { subject ->
            val left = subject.left.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: return@mapNotNull null
            val top = subject.top.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: return@mapNotNull null
            val right = subject.right.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: return@mapNotNull null
            val bottom = subject.bottom.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: return@mapNotNull null
            val width = right - left
            val height = bottom - top
            if (width <= 0.015 || height <= 0.015) return@mapNotNull null
            val confidence = subject.confidence.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0
            if (confidence < 0.20) return@mapNotNull null
            val area = width * height
            Triple(subject.copy(left = left, top = top, right = right, bottom = bottom, confidence = confidence), area, confidence)
        }
        val chosen = valid.maxByOrNull { (_, area, confidence) -> area * (0.7 + confidence * 0.3) } ?: return null
        val subject = chosen.first
        val centerX = (subject.left + subject.right) / 2.0
        val centerY = (subject.top + subject.bottom) / 2.0
        val area = chosen.second.coerceIn(0.0, 1.0)
        val position = when {
            centerX < 0.18 || centerX > 0.82 -> "피사체가 가장자리에 가까워요 · 프레임 안쪽으로 여백을 확보하세요."
            centerX in 0.42..0.58 -> "피사체가 중앙에 있어요 · 좌우 삼분할 위치의 구도도 비교해 보세요."
            else -> "피사체가 삼분할 영역에 가까워요 · 진행 방향이나 시선 쪽 여백을 남겨보세요."
        }
        val scale = when {
            area < 0.025 -> "피사체가 작게 잡혀요 · 안전한 위치에서 조금 가까이 이동해 보세요."
            area > 0.55 -> "피사체가 프레임을 많이 차지해요 · 가장자리 잘림을 확인하고 한 걸음 물러나 보세요."
            else -> "피사체 크기는 무난해요 · 주변 배경과 가장자리 여백을 확인하세요."
        }
        val vertical = when {
            centerY < 0.18 -> "위쪽 여백이 부족할 수 있어요."
            centerY > 0.82 -> "아래쪽 여백이 부족할 수 있어요."
            else -> null
        }
        return SubjectCompositionResult(
            centerX = centerX,
            centerY = centerY,
            areaRatio = area,
            label = subject.label?.takeIf { it.isNotBlank() },
            guidance = listOfNotNull(position, scale, vertical).joinToString(" ")
        )
    }
}
