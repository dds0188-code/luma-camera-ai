package com.luma.camera

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Background = Color(0xFF0B0D10)
private val Panel = Color(0xFF171B21)
private val Accent = Color(0xFF70D6C2)
private val Muted = Color(0xFF9AA4B2)

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("LUMA", color = Accent, style = MaterialTheme.typography.labelLarge)
                        Text("CAMERA AI", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    }
                    Text("LOCAL AI  ·  OFFLINE", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .background(Panel, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("카메라 미리보기", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("카메라 연결 모듈 준비 중", color = Muted)
                        Spacer(Modifier.height(18.dp))
                        Text("구도 가이드  ·  수평계  ·  AI 장면 분석", color = Accent)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("촬영 어시스턴트", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("실제 카메라 기능이 연결되면 기기에서 지원하는 설정만 표시합니다.", color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoTile("구도", "삼분할 · 대칭")
                        InfoTile("노출", "ISO · 셔터")
                        InfoTile("초점", "수동 · 자동")
                    }
                }
                Text("사진은 기본적으로 기기 내부에 보관됩니다.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoTile(title: String, detail: String) {
    Column(
        modifier = Modifier.weight(1f).background(Color(0xFF222832), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, color = Accent, style = MaterialTheme.typography.labelMedium)
        Text(detail, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
