package com.luma.camera.analysis

import kotlin.math.sqrt

/**
 * Lightweight offline quality heuristics over a small, uniformly sampled grayscale image.
 * Sharpness is a local-contrast proxy, not a calibrated blur measurement.
 */
data class PhotoQualityInput(
    val sampleWidth: Int,
    val sampleHeight: Int,
    val grayscaleSamples: List<Int>
)

data class PhotoQualityResult(
    val score: Int,
    val exposureScore: Int,
    val contrastScore: Int,
    val detailScore: Int,
    val darkClippedRatio: Double,
    val brightClippedRatio: Double,
    val suggestions: List<String>
)

object PhotoQualityAnalyzer {
    fun analyze(input: PhotoQualityInput): PhotoQualityResult {
        require(input.sampleWidth >= 3 && input.sampleHeight >= 3) {
            "샘플 격자는 가로·세로 각각 3 이상이어야 합니다."
        }
        require(input.grayscaleSamples.size == input.sampleWidth * input.sampleHeight) {
            "샘플 개수가 격자 크기와 일치하지 않습니다."
        }
        require(input.grayscaleSamples.all { it in 0..255 }) {
            "명도 샘플은 0~255 범위여야 합니다."
        }

        val pixels = input.grayscaleSamples
        val count = pixels.size.toDouble()
        val mean = pixels.sum().toDouble() / count
        val variance = pixels.sumOf { (it - mean) * (it - mean) } / count
        val standardDeviation = sqrt(variance)
        val darkRatio = pixels.count { it <= 8 } / count
        val brightRatio = pixels.count { it >= 247 } / count

        var neighborDifference = 0.0
        var neighborPairs = 0
        for (y in 0 until input.sampleHeight) {
            for (x in 0 until input.sampleWidth) {
                val index = y * input.sampleWidth + x
                if (x + 1 < input.sampleWidth) {
                    neighborDifference += kotlin.math.abs(pixels[index] - pixels[index + 1])
                    neighborPairs++
                }
                if (y + 1 < input.sampleHeight) {
                    neighborDifference += kotlin.math.abs(pixels[index] - pixels[index + input.sampleWidth])
                    neighborPairs++
                }
            }
        }
        val edgeContrast = if (neighborPairs == 0) 0.0 else neighborDifference / neighborPairs

        val exposurePenalty = (darkRatio * 125.0 + brightRatio * 125.0).coerceAtMost(100.0)
        val exposure = (100.0 - exposurePenalty).toInt().coerceIn(0, 100)
        val contrast = ((standardDeviation / 64.0) * 100.0).toInt().coerceIn(0, 100)
        val detail = ((edgeContrast / 32.0) * 100.0).toInt().coerceIn(0, 100)
        val total = (exposure * 0.40 + contrast * 0.25 + detail * 0.35).toInt()

        val suggestions = buildList {
            if (darkRatio >= 0.08) add("어두운 영역이 많이 뭉개졌을 수 있습니다. 노출을 조금 높여 다시 비교해 보세요.")
            if (brightRatio >= 0.08) add("밝은 영역이 날아갔을 수 있습니다. 노출을 낮추거나 하이라이트를 확인해 보세요.")
            if (contrast < 20) add("명암 차이가 작습니다. 피사체와 배경의 분리 및 조명을 확인해 보세요.")
            if (detail < 25) add("미세한 명암 변화가 적습니다. 초점을 확인하고 촬영 중 흔들림을 줄여 보세요.")
            if (isEmpty()) add("큰 노출·명암 경고는 감지되지 않았습니다. 초점과 구도를 확대해 확인해 보세요.")
        }

        return PhotoQualityResult(
            score = total.coerceIn(0, 100),
            exposureScore = exposure,
            contrastScore = contrast,
            detailScore = detail,
            darkClippedRatio = darkRatio,
            brightClippedRatio = brightRatio,
            suggestions = suggestions
        )
    }
}
