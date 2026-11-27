package com.luma.camera.analysis

/** Lightweight luminance histogram for live preview guidance (not a RAW exposure meter). */
data class ExposureMeterResult(
    val histogram: List<Int>,
    val darkClippedPercent: Int,
    val brightClippedPercent: Int,
    val warning: String
)

object ExposureMeterAnalyzer {
    fun analyze(samples: List<Int>): ExposureMeterResult {
        require(samples.isNotEmpty()) { "노출 샘플이 비어 있습니다." }
        require(samples.all { it in 0..255 }) { "밝기 샘플은 0~255 범위여야 합니다." }
        val bins = MutableList(16) { 0 }
        var dark = 0
        var bright = 0
        samples.forEach { value ->
            bins[(value / 16).coerceIn(0, 15)]++
            if (value <= 5) dark++
            if (value >= 250) bright++
        }
        val darkPercent = (dark * 100.0 / samples.size).toInt()
        val brightPercent = (bright * 100.0 / samples.size).toInt()
        val warning = when {
            darkPercent >= 12 && brightPercent >= 12 -> "주의 · 어두운 영역과 밝은 영역의 디테일 손실 가능성"
            darkPercent >= 12 -> "주의 · 어두운 영역의 디테일 손실 가능성"
            brightPercent >= 12 -> "주의 · 밝은 영역의 하이라이트 손실 가능성"
            else -> "노출 분포 양호 · 구도와 피사체를 확인하세요"
        }
        return ExposureMeterResult(bins, darkPercent, brightPercent, warning)
    }
}
