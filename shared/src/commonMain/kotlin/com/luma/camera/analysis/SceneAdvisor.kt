package com.luma.camera.analysis

import kotlin.math.abs
import kotlin.math.sqrt

/** User-selected shooting intent. This is a profile, not an automatically classified scene. */
enum class ShootingIntent(val label: String) {
    PORTRAIT("인물"),
    LANDSCAPE("풍경"),
    ARCHITECTURE("건축"),
    FOOD("음식")
}

data class SceneAdviceInput(
    val intent: ShootingIntent,
    val subjectCenterX: Double = 0.5,
    val subjectCenterY: Double = 0.5,
    val subjectAreaRatio: Double = 0.0,
    val horizonTiltDegrees: Double = 0.0,
    val currentZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    /** Display-relative pitch estimate from the rotation-vector sensor; guidance only. */
    val cameraPitchDegrees: Double = 0.0
)

data class SceneAdvice(
    val title: String,
    val summary: String,
    val suggestedZoomRatio: Float?,
    val recommendations: List<String>
)

/**
 * Transparent, offline shooting guidance. It does not claim to classify scenes:
 * the user chooses an intent and the engine adapts framing guidance to available signals.
 */
object SceneAdvisor {
    fun advise(input: SceneAdviceInput): SceneAdvice {
        val area = input.subjectAreaRatio.coerceIn(0.0, 1.0)
        val x = input.subjectCenterX.coerceIn(0.0, 1.0)
        val y = input.subjectCenterY.coerceIn(0.0, 1.0)
        val tilt = abs(input.horizonTiltDegrees)
        val pitch = input.cameraPitchDegrees.takeIf { it.isFinite() }?.coerceIn(-90.0, 90.0) ?: 0.0
        val maxZoom = input.maxZoomRatio.coerceAtLeast(1f)
        val currentZoom = input.currentZoomRatio.coerceIn(1f, maxZoom)
        val recommendations = mutableListOf<String>()
        val targetArea = if (input.intent == ShootingIntent.PORTRAIT) 0.16 else null
        val title: String
        val summary: String

        when (input.intent) {
            ShootingIntent.PORTRAIT -> {
                title = "인물 촬영"
                summary = if (area <= 0.001) "얼굴이 화면에 들어오도록 상반신 또는 전신을 배치하세요."
                    else "얼굴 크기와 시선 방향을 기준으로 인물 프레이밍을 맞춰보세요."
                if (area > 0.001 && (x < 0.22 || x > 0.78)) {
                    recommendations += "얼굴이 화면 가장자리에 가까워요 · 안쪽으로 여백을 확보하세요."
                } else if (area > 0.001) {
                    recommendations += "시선이 향하는 쪽에 여백을 남겨보세요."
                }
                if (y > 0.62) recommendations += "카메라를 눈높이에 가깝게 올려 촬영해 보세요."
                if (y < 0.25 && area > 0.001) recommendations += "머리 위 여백을 조금 확보해 보세요."
                when {
                    pitch > 8.0 -> recommendations += "카메라가 아래로 기울어져 있어요 · 인물 눈높이에 맞춰 수평에 가깝게 비교하세요."
                    pitch < -8.0 -> recommendations += "카메라가 위로 기울어져 있어요 · 인물 눈높이에서 촬영한 결과와 비교하세요."
                    else -> recommendations += "눈높이 촬영을 기준으로 약간 위·아래 각도도 비교해 보세요."
                }
                when {
                    area in 0.001..0.025 -> recommendations += "얼굴이 작게 잡혀요 · 안전한 위치에서 한 걸음 가까이 가거나 광학 화각을 좁혀보세요."
                    area > 0.38 -> recommendations += "얼굴이 크게 잡혀요 · 한 걸음 물러나 머리와 어깨 여백을 확보해 보세요."
                }
                recommendations += "배경이 복잡하면 피사체와 배경 사이 거리를 벌려보세요."
            }
            ShootingIntent.LANDSCAPE -> {
                title = "풍경 촬영"
                summary = "넓은 화각과 안정적인 수평으로 장면의 깊이를 담아보세요."
                recommendations += "가능하면 1.0× 부근의 넓은 화각부터 비교하세요."
                recommendations += "하늘과 지면 중 강조할 영역에 따라 수평선을 위·아래 삼분할선에 배치하세요."
                if (tilt > 1.5) recommendations += "풍경에서는 수평이 특히 중요해요 · 기울기를 줄이세요."
                when {
                    pitch > 8.0 -> recommendations += "카메라가 아래를 향하고 있어요 · 전경 강조가 목적이 아니라면 수평에 가까운 각도와 비교하세요."
                    pitch < -8.0 -> recommendations += "카메라가 위를 향하고 있어요 · 하늘 강조가 목적이 아니라면 카메라를 조금 낮춰 비교하세요."
                }
                recommendations += "전경에 나뭇잎·바위 등 기준 피사체를 넣으면 깊이감이 살아나요."
                recommendations += "같은 장면을 1.0×와 한 단계 좁은 화각으로 비교해 원근감과 가장자리 왜곡을 확인하세요."
            }
            ShootingIntent.ARCHITECTURE -> {
                title = "건축 촬영"
                summary = "수직선과 수평선을 반듯하게 유지해 건물 형태를 정돈하세요."
                recommendations += "건물 모서리의 수직선이 화면 가장자리와 평행한지 확인하세요."
                if (tilt > 1.0) recommendations += "카메라 롤을 줄이고 수평계를 기준으로 맞추세요."
                recommendations += "가능하면 카메라를 위로 과하게 기울이지 말고 촬영 위치를 조정하세요."
                if (pitch < -6.0) recommendations += "현재 위쪽으로 기울어진 각도는 건물 상단 수렴을 키울 수 있어요 · 촬영 위치를 뒤로 옮겨 비교하세요."
                recommendations += "초광각 가장자리의 형태 왜곡을 확인하고 1.0×~1.5×도 비교하세요."
            }
            ShootingIntent.FOOD -> {
                title = "음식 촬영"
                summary = "음식의 질감과 형태가 보이도록 주 피사체 크기를 확보하세요."
                recommendations += "정면·45도·탑뷰를 비교해 음식 높이와 형태가 잘 보이는 각도를 선택하세요."
                when {
                    pitch > 18.0 -> recommendations += "현재 탑뷰에 가까운 각도예요 · 접시 전체 형태와 배치를 강조할 때 유리해요."
                    pitch in 5.0..18.0 -> recommendations += "현재 각도는 45도 연출에 가까워요 · 음식의 높이와 질감을 함께 보여주기 좋아요."
                    else -> recommendations += "음식의 층과 높이가 중요하면 카메라를 조금 낮춘 45도 각도도 비교하세요."
                }
                recommendations += "빛이 한쪽에서 들어오도록 하고 접시 주변 여백을 정리하세요."
                recommendations += "음식 표면의 반사광이 과하면 노출을 조금 낮춰 질감을 보존하세요."
            }
        }

        if (input.intent == ShootingIntent.PORTRAIT && area > 0.001) {
            val target = targetArea ?: 0.16
            val ratio = (currentZoom * sqrt(target / area).toFloat()).coerceIn(1f, maxZoom)
            if (maxZoom > 1.01f && abs(ratio - currentZoom) >= 0.08f) {
                val direction = if (ratio > currentZoom) "줌 인" else "줌 아웃"
                recommendations += "현재 감지 크기 기준 추천 배율 ${"%.1f".format(ratio)}× · $direction"
                return SceneAdvice(title, summary, ratio, recommendations)
            }
        }
        if (recommendations.isEmpty()) recommendations += "현재 구도를 유지하며 촬영해 보세요."
        return SceneAdvice(title, summary, null, recommendations)
    }
}
