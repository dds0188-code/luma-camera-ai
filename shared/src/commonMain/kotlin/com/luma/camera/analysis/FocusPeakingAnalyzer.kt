package com.luma.camera.analysis

import kotlin.math.abs

data class FocusPeakingPoint(val x: Float, val y: Float)

/**
 * Finds high local luminance transitions as a focus-assist visualization.
 * This is edge emphasis, not a true optical focus-confidence measurement.
 */
object FocusPeakingAnalyzer {
    fun analyze(
        samples: List<Int>,
        width: Int,
        height: Int,
        threshold: Int = 42,
        rotationDegrees: Int = 0,
        mirrorHorizontally: Boolean = false,
        maxPoints: Int = 420
    ): List<FocusPeakingPoint> {
        require(width >= 3 && height >= 3) { "샘플 격자는 가로·세로 각각 3 이상이어야 합니다." }
        require(samples.size == width * height) { "샘플 개수가 격자 크기와 일치하지 않습니다." }
        require(samples.all { it in 0..255 }) { "명도 샘플은 0~255 범위여야 합니다." }
        require(threshold in 1..255) { "임계값은 1~255 범위여야 합니다." }
        require(rotationDegrees in setOf(0, 90, 180, 270)) { "회전값은 0, 90, 180, 270만 지원합니다." }
        require(maxPoints > 0) { "최대 표시 점 수는 1 이상이어야 합니다." }

        val points = ArrayList<FocusPeakingPoint>()
        val stride = if (width * height > maxPoints * 3) 2 else 1
        for (y in 1 until height - 1 step stride) {
            for (x in 1 until width - 1 step stride) {
                val index = y * width + x
                val horizontal = abs(samples[index + 1] - samples[index - 1])
                val vertical = abs(samples[index + width] - samples[index - width])
                if (horizontal + vertical >= threshold) {
                    var nx = (x + 0.5f) / width
                    var ny = (y + 0.5f) / height
                    when (rotationDegrees) {
                        90 -> { val oldX = nx; nx = 1f - ny; ny = oldX }
                        180 -> { nx = 1f - nx; ny = 1f - ny }
                        270 -> { val oldX = nx; nx = ny; ny = 1f - oldX }
                    }
                    if (mirrorHorizontally) nx = 1f - nx
                    points += FocusPeakingPoint(nx.coerceIn(0f, 1f), ny.coerceIn(0f, 1f))
                    if (points.size >= maxPoints) return points
                }
            }
        }
        return points
    }
}
