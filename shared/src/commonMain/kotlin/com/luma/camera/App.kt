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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

private val Background = Color(0xFF0B0D10)
private val Panel = Color(0xFF171B21)
private val Accent = Color(0xFF70D6C2)
private val Muted = Color(0xFF9AA4B2)

@Composable
fun App(
    cameraPreview: @Composable () -> Unit = { PreviewPlaceholder() },
    onCaptureClick: () -> Unit = {},
    captureStatus: String = "카메라 연결 대기 중"
) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LUMA", color = Accent, style = MaterialTheme.typography.labelLarge)
                        Text("CAMERA AI", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    }
                    Text("LOCAL AI · OFFLINE", color = Muted, style = MaterialTheme.typography.labelSmall)
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
                            Text("구도 가이드", color = Accent, style = MaterialTheme.typography.labelLarge)
                            Text("삼분할 기준선", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            "피사체 위치와 여백을 확인하세요",
                            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Panel, RoundedCornerShape(20.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("촬영 어시스턴트", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Text("ANDROID CAMERA", color = Accent, style = MaterialTheme.typography.labelSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoTile(Modifier.weight(1f), "구도", "삼분할")
                        InfoTile(Modifier.weight(1f), "노출", "자동")
                        InfoTile(Modifier.weight(1f), "초점", "자동")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onCaptureClick,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Color(0xFF07110F)
                            ),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("  촬영  ", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Text(captureStatus, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "사진은 기기의 사진 앱 > LumaCameraAI 앨범에 저장됩니다.",
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
private fun InfoTile(modifier: Modifier, title: String, detail: String) {
    Column(
        modifier = modifier.background(Color(0xFF222832), RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = Accent, style = MaterialTheme.typography.labelMedium)
        Text(detail, color = Color.White, style = MaterialTheme.typography.bodySmall)
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
