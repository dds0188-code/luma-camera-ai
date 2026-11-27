package com.luma.camera.analysis

/** A bounded set of preview regions whose luma exceeds a highlight threshold. */
data class ZebraMark(val x: Float, val y: Float)

object ZebraExposureAnalyzer {
    fun analyze(
        samples: List<Int>,
        width: Int,
        height: Int,
        threshold: Int = 245,
        rotationDegrees: Int = 0,
        mirrorHorizontally: Boolean = false,
        maxMarks: Int = 360
    ): List<ZebraMark> {
        require(width > 0 && height > 0) { "샘플 격자 크기는 1 이상이어야 합니다." }
        require(samples.size == width * height) { "샘플 개수가 격자 크기와 일치하지 않습니다." }
        require(samples.all { it in 0..255 }) { "밝기 샘플은 0~255 범위여야 합니다." }
        require(threshold in 1..255) { "하이라이트 임계값은 1~255 범위여야 합니다." }
        require(rotationDegrees in setOf(0, 90, 180, 270)) { "회전값은 0, 90, 180, 270만 지원합니다." }
        require(maxMarks > 0) { "최대 표시 수는 1 이상이어야 합니다." }

        val marks = ArrayList<ZebraMark>()
        val stride = if (width * height > maxMarks * 3) 2 else 1
        for (y in 0 until height step stride) {
            for (x in 0 until width step stride) {
                if (samples[y * width + x] < threshold) continue
                var nx = (x + 0.5f) / width
                var ny = (y + 0.5f) / height
                when (rotationDegrees) {
                    90 -> { val oldX = nx; nx = 1f - ny; ny = oldX }
                    180 -> { nx = 1f - nx; ny = 1f - ny }
                    270 -> { val oldX = nx; nx = ny; ny = 1f - oldX }
                }
                if (mirrorHorizontally) nx = 1f - nx
                marks += ZebraMark(nx.coerceIn(0f, 1f), ny.coerceIn(0f, 1f))
                if (marks.size >= maxMarks) return marks
            }
        }
        return marks
    }
}
