package com.luma.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luma.camera.analysis.ShootingIntent
import com.luma.camera.analysis.CaptureTimerOptions
import com.luma.camera.analysis.CaptureBurstOptions
import kotlin.math.abs

private val Background = Color(0xFF080B0F)
private val Panel = Color(0xFF141A21)
private val PanelRaised = Color(0xFF1C252E)
private val Accent = Color(0xFFB7F36B)
private val Muted = Color(0xFF98A5B3)
private val TextMain = Color(0xFFF4F7F9)
private val Warning = Color(0xFFFFC477)

@Composable
fun App(
    cameraPreview: @Composable () -> Unit = { PreviewPlaceholder() },
    isFrontCamera: Boolean = false,
    onToggleCamera: () -> Unit = {},
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
    onApplySuggestedZoom: (Float) -> Unit = {},
    photoQualitySummary: String = "",
    photoQualityLines: List<String> = emptyList(),
    cameraCapabilitySummary: String = "",
    sceneRecognitionSummary: String = "",
    recognizedShootingIntent: ShootingIntent? = null,
    onApplyRecognizedScene: (ShootingIntent) -> Unit = {},
    torchEnabled: Boolean = false,
    onTorchToggle: (Boolean) -> Unit = {},
    captureTimerSeconds: Int = 0,
    onCaptureTimerChange: (Int) -> Unit = {},
    captureBurstCount: Int = 1,
    onCaptureBurstCountChange: (Int) -> Unit = {},
    gridMode: String = "삼분할",
    onGridModeChange: (String) -> Unit = {}
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("L", color = Color(0xFF10170B), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                        Text("LUMA CAMERA", color = TextMain, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("온디바이스 촬영 어시스턴트", color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onToggleCamera,
                        modifier = Modifier.height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 11.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = Accent),
                        shape = CircleShape
                    ) { Text(if (isFrontCamera) "전면" else "후면", style = MaterialTheme.typography.labelSmall) }
                    Row(
                        modifier = Modifier.clip(CircleShape).background(Panel).padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Accent))
                        Text("LOCAL AI", color = Accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ShootingIntent.values().forEach { intent ->
                        val selected = shootingIntent == intent
                        Button(
                            onClick = { onShootingIntentChange(intent) },
                            modifier = Modifier.height(38.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 17.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Accent else Panel,
                                contentColor = if (selected) Color(0xFF10170B) else Muted
                            ),
                            shape = CircleShape
                        ) {
                            Text(intent.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text("타이머", color = Muted, style = MaterialTheme.typography.labelSmall)
                    CaptureTimerOptions.seconds.forEach { seconds ->
                        val selected = captureTimerSeconds == seconds
                        Button(
                            onClick = { onCaptureTimerChange(seconds) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Accent else Panel,
                                contentColor = if (selected) Color(0xFF10170B) else Muted
                            ),
                            shape = CircleShape
                        ) { Text(if (seconds == 0) "끔" else "${seconds}초", style = MaterialTheme.typography.labelSmall) }
                    }
                    Text("연사", color = Muted, style = MaterialTheme.typography.labelSmall)
                    CaptureBurstOptions.counts.forEach { count ->
                        val selected = captureBurstCount == count
                        Button(
                            onClick = { onCaptureBurstCountChange(count) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Accent else Panel,
                                contentColor = if (selected) Color(0xFF10170B) else Muted
                            ),
                            shape = CircleShape
                        ) { Text(if (count == 1) "1장" else "${count}연사", style = MaterialTheme.typography.labelSmall) }
                    }
                    Spacer(Modifier.weight(1f))
                    listOf("삼분할", "중앙", "끄기").forEach { mode ->
                        val selected = gridMode == mode
                        Button(
                            onClick = { onGridModeChange(mode) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) PanelRaised else Panel,
                                contentColor = if (selected) Accent else Muted
                            ),
                            shape = CircleShape
                        ) { Text(mode, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1.18f)
                        .clip(RoundedCornerShape(25.dp)).background(PanelRaised)
                ) {
                    cameraPreview()
                    CompositionGrid(Modifier.fillMaxSize(), gridMode)
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                            .clip(CircleShape).background(Color(0x99080B0F)).padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF625F)))
                        Text("LIVE GUIDE", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                            .clip(RoundedCornerShape(13.dp)).background(Color(0x99080B0F)).padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        val levelOk = abs(horizonTiltDegrees) < 1.5f
                        Text(if (levelOk) "수평 양호" else "수평 조정", color = if (levelOk) Accent else Warning, style = MaterialTheme.typography.labelSmall)
                        Text("${"%.1f".format(abs(horizonTiltDegrees))}°", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        compositionGuidance,
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                            .background(Color(0xD9080B0F)).padding(horizontal = 13.dp, vertical = 11.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("촬영 제안", color = TextMain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(adviceSummary.ifBlank { "장면에 맞는 촬영 구도를 분석합니다." }, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        if (suggestedZoomRatio != null) {
                            Button(
                                onClick = { onApplySuggestedZoom(suggestedZoomRatio) },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10170B)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                                shape = CircleShape
                            ) { Text("${"%.1f".format(suggestedZoomRatio)}× 적용", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    adviceLines.take(2).forEach { line ->
                        Text("· $line", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }

                    if (sceneRecognitionSummary.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                                .background(PanelRaised).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("온디바이스 장면 인식", color = Accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(sceneRecognitionSummary, color = TextMain, style = MaterialTheme.typography.bodySmall)
                            }
                            if (recognizedShootingIntent != null) {
                                Button(
                                    onClick = { onApplyRecognizedScene(recognizedShootingIntent) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10170B)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                                    shape = CircleShape
                                ) { Text("모드 적용", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("렌즈 · 노출", color = TextMain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("기기 지원 범위", color = Accent, style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("줌  ${"%.1f".format(zoomRatio)}×", color = TextMain, modifier = Modifier.weight(1f))
                            Text("최대 ${"%.1f".format(maxZoomRatio)}×", color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                        Slider(
                            value = zoomRatio.coerceIn(1f, maxZoomRatio.coerceAtLeast(1f)),
                            onValueChange = onZoomChange,
                            valueRange = 1f..maxZoomRatio.coerceAtLeast(1.01f),
                            enabled = maxZoomRatio > 1.01f
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("노출 보정", color = TextMain, modifier = Modifier.weight(1f))
                            Text(if (exposureIndex > 0) "+$exposureIndex" else exposureIndex.toString(), color = Accent, fontWeight = FontWeight.Bold)
                        }
                        if (maxExposureIndex > minExposureIndex) {
                            Slider(
                                value = exposureIndex.toFloat().coerceIn(minExposureIndex.toFloat(), maxExposureIndex.toFloat()),
                                onValueChange = { onExposureChange(it.toInt()) },
                                valueRange = minExposureIndex.toFloat()..maxExposureIndex.toFloat(),
                                steps = (maxExposureIndex - minExposureIndex - 1).coerceAtLeast(0)
                            )
                        } else {
                            Text("이 기기에서 노출 보정 범위를 제공하지 않습니다.", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("플래시", color = TextMain, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(if (torchEnabled) "켜짐" else "꺼짐", color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { onTorchToggle(!torchEnabled) },
                                enabled = cameraCapabilitySummary.contains("플래시 있음"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (torchEnabled) Accent else PanelRaised,
                                    contentColor = if (torchEnabled) Color(0xFF10170B) else TextMain
                                ),
                                shape = CircleShape
                            ) { Text(if (torchEnabled) "켜짐" else "끔") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("촬영 준비 완료", color = TextMain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(captureStatus, color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = onCaptureClick,
                                modifier = Modifier.size(68.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10170B)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Box(Modifier.size(50.dp).border(3.dp, Color(0xFF10170B), CircleShape).padding(5.dp).background(Color(0xFF10170B), CircleShape))
                            }
                        }
                    }

                    if (cameraCapabilitySummary.isNotBlank()) {
                        InfoCard("기기 카메라 정보", cameraCapabilitySummary)
                    }
                    if (photoQualitySummary.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text("촬영 결과 분석", color = TextMain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(photoQualitySummary, color = Accent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            photoQualityLines.forEach { Text("· $it", color = Muted, style = MaterialTheme.typography.bodySmall) }
                            Text("디테일 점수는 초점 정확도를 직접 측정한 값이 아닌 참고 지표입니다.", color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("사진과 AI 분석은 기본적으로 기기 안에서 처리됩니다.", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = TextMain, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(content, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CompositionGrid(modifier: Modifier = Modifier, mode: String) {
    if (mode == "끄기") return
    Canvas(modifier = modifier) {
        val lineColor = Color.White.copy(alpha = 0.42f)
        val stroke = 1.dp.toPx()
        if (mode == "중앙") {
            drawLine(lineColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), stroke, cap = StrokeCap.Round)
            drawLine(lineColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), stroke, cap = StrokeCap.Round)
        } else {
            for (i in 1..2) {
                val x = size.width * i / 3f
                val y = size.height * i / 3f
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), stroke, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), stroke, cap = StrokeCap.Round)
            }
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
