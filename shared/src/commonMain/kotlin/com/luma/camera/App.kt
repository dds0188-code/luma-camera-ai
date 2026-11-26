package com.luma.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.luma.camera.analysis.ShootingIntent
import kotlin.math.abs

private val Background = Color(0xFF0B0D10)
private val Panel = Color(0xFF171B21)
private val Accent = Color(0xFF70D6C2)
private val Muted = Color(0xFF9AA4B2)

@Composable
fun App(
    cameraPreview: @Composable () -> Unit = { PreviewPlaceholder() },
    onCaptureClick: () -> Unit = {},
    captureStatus: String = "카메라 연결 대기 중",
    zoomRatio: Float = 1f,
    maxZoomRatio: Float = 1f,
    onZoomChange: (Float) -> Unit = {},
    exposureIndex: Int = 0,
    minExposureIndex: Int = 0,
    maxExposureIndex: Int = 0,
    onExposureChange: (Int) -> Unit = {},
    horizonTiltDegrees: Float = 0f,
    compositionGuidance: String = "피사체를 찾는 중입니다.",
    shootingIntent: ShootingIntent = ShootingIntent.PORTRAIT,
    onShootingIntentChange: (ShootingIntent) -> Unit = {},
    adviceSummary: String = "",
    adviceLines: List<String> = emptyList(),
    suggestedZoomRatio: Float? = null,
    onApplySuggestedZoom: (Float) -> Unit = {}
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LUMA", color = Accent, style = MaterialTheme.typography.labelLarge)
                        Text("CAMERA AI", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    }
                    Text("ON-DEVICE ASSIST", color = Muted, style = MaterialTheme.typography.labelSmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShootingIntent.values().forEach { intent ->
                        Button(
                            onClick = { onShootingIntentChange(intent) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (shootingIntent == intent) Accent else Panel,
                                contentColor = if (shootingIntent == intent) Color(0xFF07110F) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(intent.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .background(Panel, RoundedCornerShape(24.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        cameraPreview()
                        CompositionGrid(Modifier.fillMaxSize())
                        Column(
                            modifier = Modifier.align(Alignment.TopStart).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("실시간 구도 가이드", color = Accent, style = MaterialTheme.typography.labelLarge)
                            Text("${shootingIntent.label} · 실시간 촬영 가이드", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(
                            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            val levelText = when {
                                abs(horizonTiltDegrees) < 1.5f -> "수평 양호"
                                horizonTiltDegrees > 0f -> "오른쪽 기울기"
                                else -> "왼쪽 기울기"
                            }
                            Text(levelText, color = if (abs(horizonTiltDegrees) < 1.5f) Accent else Color(0xFFFFC078))
                            Text("${"%.1f".format(kotlin.math.abs(horizonTiltDegrees))}°", color = Color.White)
                        }
                        Text(
                            compositionGuidance,
                            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                .background(Color(0xCC101419)).padding(12.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Panel, RoundedCornerShape(20.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("카메라 설정", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Text("기기 지원 범위 적용", color = Accent, style = MaterialTheme.typography.labelSmall)
                    }
                    if (adviceSummary.isNotBlank()) {
                        Text(adviceSummary, color = Accent, style = MaterialTheme.typography.bodySmall)
                    }
                    adviceLines.take(2).forEach { line ->
                        Text("• $line", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (suggestedZoomRatio != null) {
                        Button(
                            onClick = { onApplySuggestedZoom(suggestedZoomRatio) },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF07110F))
                        ) {
                            Text("추천 줌 ${"%.1f".format(suggestedZoomRatio)}× 적용")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("줌  ${"%.1f".format(zoomRatio)}×", color = Color.White, modifier = Modifier.weight(1f))
                        Text("최대 ${"%.1f".format(maxZoomRatio)}×", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Slider(
                        value = zoomRatio.coerceIn(1f, maxZoomRatio.coerceAtLeast(1f)),
                        onValueChange = onZoomChange,
                        valueRange = 1f..maxZoomRatio.coerceAtLeast(1.01f),
                        enabled = maxZoomRatio > 1.01f
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("노출 보정  $exposureIndex", color = Color.White, modifier = Modifier.weight(1f))
                        Text("기기 지원 범위", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    if (maxExposureIndex > minExposureIndex) {
                        Slider(
                            value = exposureIndex.toFloat().coerceIn(minExposureIndex.toFloat(), maxExposureIndex.toFloat()),
                            onValueChange = { onExposureChange(it.toInt()) },
                            valueRange = minExposureIndex.toFloat()..maxExposureIndex.toFloat(),
                            steps = (maxExposureIndex - minExposureIndex - 1).coerceAtLeast(0)
                        )
                    } else {
                        Text("이 카메라는 노출 보정 조절을 지원하지 않습니다.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onCaptureClick,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF07110F)),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("  촬영  ", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text(captureStatus, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "사진은 기기에 저장됩니다. 얼굴 구도 분석은 기기 내에서 처리됩니다.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CompositionGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val lineColor = Color.White.copy(alpha = 0.48f)
        val stroke = 1.dp.toPx()
        for (i in 1..2) {
            val x = size.width * i / 3f
            val y = size.height * i / 3f
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), stroke, cap = StrokeCap.Round)
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun PreviewPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("카메라 미리보기", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Windows 카메라 연동 준비 중", color = Muted)
    }
}
