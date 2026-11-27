package com.luma.camera.analysis

import kotlin.math.abs
import kotlin.math.sqrt

enum class ShootingIntent(val label: String) {
    PORTRAIT("인물"), LANDSCAPE("풍경"), ARCHITECTURE("건축"), FOOD("음식")
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
 * Offline, explainable shooting guidance. Scene labels come from the on-device classifier;
 * geometry comes from face detection and device sensors. This is not a trained aesthetic model.
 */
object SceneAdvisor {
    fun advise(input: SceneAdviceInput): SceneAdvice {
        fun safe(value: Double, fallback: Double) = value.takeIf { it.isFinite() } ?: fallback
        val area = safe(input.subjectAreaRatio, 0.0).coerceIn(0.0, 1.0)
        val x = safe(input.subjectCenterX, 0.5).coerceIn(0.0, 1.0)
        val y = safe(input.subjectCenterY, 0.5).coerceIn(0.0, 1.0)
        val tilt = abs(safe(input.horizonTiltDegrees, 0.0))
        val pitch = safe(input.cameraPitchDegrees, 0.0).coerceIn(-90.0, 90.0)
        val maxZoom = input.maxZoomRatio.takeIf { it.isFinite() && it >= 1f } ?: 1f
        val currentZoom = input.currentZoomRatio.takeIf { it.isFinite() }?.coerceIn(1f, maxZoom) ?: 1f
        val recommendations = mutableListOf<String>()
        val title: String
        val summary: String

        when (input.intent) {
            ShootingIntent.PORTRAIT -> {
                title = "인물 촬영"
                summary = if (area <= 0.001) "얼굴을 인식하면 인물 크기와 위치에 맞춰 프레이밍을 안내합니다."
                    else "얼굴 크기와 화면 위치를 분석해 인물 프레이밍을 안내합니다."
                if (area > 0.001) {
                    when {
                        x < 0.18 || x > 0.82 -> recommendations += "얼굴이 화면 가장자리에 가까워요 · 안쪽으로 이동해 머리와 어깨 여백을 확보하세요."
                        x in 0.42..0.58 -> recommendations += "얼굴이 중앙에 있어요 · 좌우 삼분할 위치로 옮긴 구도도 비교해 보세요."
                        else -> recommendations += "얼굴이 삼분할 영역에 가까워요 · 반대쪽에 시선 여백을 남겨보세요."
                    }
                    when {
                        y > 0.68 -> recommendations += "얼굴이 낮게 배치됐어요 · 카메라를 눈높이에 맞추고 머리 위 여백을 확인하세요."
                        y < 0.22 -> recommendations += "얼굴이 위쪽에 가까워요 · 머리 위 여백이 잘리지 않는지 확인하세요."
                        else -> recommendations += "눈·얼굴 주변에 충분한 여백이 있는지 확인하세요."
                    }
                } else {
                    recommendations += "인물의 눈을 화면 상단 삼분할선 부근에 두고 머리 위 여백을 남겨보세요."
                }
                when {
                    pitch > 8.0 -> recommendations += "카메라가 아래를 향하고 있어요 · 인물 눈높이에서 촬영한 결과와 비교하세요."
                    pitch < -8.0 -> recommendations += "카메라가 위를 향하고 있어요 · 눈높이 또는 약간 높은 위치와 비교하세요."
                    else -> recommendations += "눈높이를 기본으로 약간 높은 각도와 낮은 각도를 비교해 보세요."
                }
                when {
                    area in 0.001..0.025 -> recommendations += "얼굴이 작게 잡혀요 · 안전한 위치에서 가까이 이동하거나 광학 화각을 좁혀보세요."
                    area > 0.38 -> recommendations += "얼굴이 크게 잡혀요 · 한 걸음 물러나 머리와 어깨 여백을 확보해 보세요."
                }
                recommendations += "배경이 복잡하면 피사체와 배경 사이 거리를 벌리고 배경 정리를 확인하세요."
            }
            ShootingIntent.LANDSCAPE -> {
                title = "풍경 촬영"
                summary = "수평선과 전경·중경·원경의 층을 기준으로 풍경 구도를 안내합니다."
                recommendations += "넓은 화각부터 시작하고 전경·중경·원경이 각각 보이는 위치를 찾아보세요."
                recommendations += "하늘을 강조하면 수평선을 아래 삼분할선, 지면을 강조하면 위 삼분할선 부근에 배치하세요."
                if (tilt > 1.5) recommendations += "수평이 기울어져 있어요 · 수평계를 기준으로 바로잡아 보세요."
                when {
                    pitch > 8.0 -> recommendations += "카메라가 아래를 향하고 있어요 · 전경 강조가 목적이 아니라면 수평에 가까운 각도와 비교하세요."
                    pitch < -8.0 -> recommendations += "카메라가 위를 향하고 있어요 · 하늘 강조가 목적이 아니라면 카메라를 조금 낮춰 비교하세요."
                }
                recommendations += "나뭇잎·바위·길처럼 시선을 이끄는 전경 요소를 프레임 한쪽에 배치해 보세요."
                recommendations += "1.0×와 광학 망원 화각을 비교해 배경 압축감과 가장자리 왜곡을 확인하세요."
            }
            ShootingIntent.ARCHITECTURE -> {
                title = "건축 촬영"
                summary = "수직선·수평선과 카메라 높이를 기준으로 건물 왜곡을 줄이도록 안내합니다."
                recommendations += "건물 모서리의 수직선이 화면 가장자리와 평행한지 확인하세요."
                if (tilt > 1.0) recommendations += "카메라 롤이 있어요 · 수평을 먼저 맞춰보세요."
                recommendations += "건물 전체를 담으려고 카메라를 과하게 위로 기울이기보다 촬영 위치를 뒤로 옮겨보세요."
                if (pitch < -6.0) recommendations += "현재 위쪽으로 기울어진 각도는 수직선 수렴을 키울 수 있어요 · 카메라를 낮추거나 뒤로 이동해 비교하세요."
                recommendations += "초광각 가장자리 왜곡을 확인하고 1.0×~1.5× 광학 화각도 비교하세요."
                recommendations += "정면 대칭 구도와 한쪽으로 치우친 사선 구도를 각각 비교해 보세요."
            }
            ShootingIntent.FOOD -> {
                title = "음식 촬영"
                summary = "음식의 높이·형태·배치에 맞는 촬영 각도와 프레이밍을 안내합니다."
                recommendations += "그릇 전체 배치가 중요하면 탑뷰, 높이와 층이 중요하면 30~60도, 단면 강조에는 낮은 각도를 비교하세요."
                when {
                    pitch > 55.0 -> recommendations += "현재는 탑뷰에 가까운 방향이에요 · 접시 전체 배치와 패턴을 강조할 때 유리해요."
                    pitch > 25.0 -> recommendations += "현재 각도는 45도 연출에 가까워요 · 음식의 높이와 질감을 함께 보여주기 좋아요."
                    pitch < 8.0 -> recommendations += "낮은 각도예요 · 층과 단면을 강조할 수 있지만 접시 전체가 잘리는지 확인하세요."
                    else -> recommendations += "현재 각도를 유지하면서 탑뷰와 45도 구도를 비교해 보세요."
                }
                recommendations += "주 피사체를 화면 중앙에 크게 두되 접시 가장자리와 주변 여백을 잘라내지 마세요."
                recommendations += "창가 등 한쪽 방향의 부드러운 빛을 활용하고 강한 반사광은 노출을 낮춰 보존하세요."
            }
        }

        // Face-area zoom is only valid for portrait mode; never treat a face box as a generic object.
        if (input.intent == ShootingIntent.PORTRAIT && area > 0.001 && maxZoom > 1.01f) {
            val targetFaceArea = 0.16
            val ratio = (currentZoom * sqrt(targetFaceArea / area).toFloat()).coerceIn(1f, maxZoom)
            if (abs(ratio - currentZoom) >= 0.08f) {
                val direction = if (ratio > currentZoom) "줌 인" else "줌 아웃"
                recommendations += "현재 얼굴 크기 기준 추천 배율 ${"%.1f".format(ratio)}× · $direction"
                return SceneAdvice(title, summary, ratio, recommendations)
            }
        }
        return SceneAdvice(title, summary, null, recommendations)
    }
}
