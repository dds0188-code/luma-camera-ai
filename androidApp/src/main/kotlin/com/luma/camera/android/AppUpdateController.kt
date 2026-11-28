package com.luma.camera.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val LATEST_RELEASE_API =
    "https://api.github.com/repos/dds0188-code/luma-camera-ai/releases/latest"

private data class CameraAppUpdate(
    val versionName: String,
    val downloadUrl: String,
    val sha256: String
)

@Composable
internal fun AppUpdateController() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<CameraAppUpdate?>(null) }
    var status by remember { mutableStateOf("업데이트 확인 중…") }
    var busy by remember { mutableStateOf(false) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }

    suspend fun checkForUpdate() {
        busy = true
        status = "최신 버전 확인 중…"
        try {
            val result = withContext(Dispatchers.IO) {
                val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
                val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    setRequestProperty("User-Agent", "Luma-Camera-AI-Android")
                }
                try {
                    if (connection.responseCode !in 200..299) {
                        throw IllegalStateException("업데이트 서버 응답 오류 (${connection.responseCode})")
                    }
                    val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val tag = release.optString("tag_name").removePrefix("v")
                    val assets = release.optJSONArray("assets") ?: throw IllegalStateException("업데이트 파일이 없습니다.")
                    var apkAsset: JSONObject? = null
                    for (index in 0 until assets.length()) {
                        val asset = assets.optJSONObject(index) ?: continue
                        if (asset.optString("name") == "luma-camera-ai.apk") {
                            apkAsset = asset
                            break
                        }
                    }
                    val asset = apkAsset ?: return@withContext Pair(null, "현재 배포된 업데이트가 없습니다.")
                    val digest = asset.optString("digest")
                    if (!digest.startsWith("sha256:")) {
                        throw IllegalStateException("배포 파일의 SHA-256 검증 정보가 없습니다.")
                    }
                    if (compareVersions(tag, current) <= 0) {
                        Pair(null, "현재 최신 버전입니다 · v$current")
                    } else {
                        Pair(
                            CameraAppUpdate(
                                versionName = tag,
                                downloadUrl = asset.getString("browser_download_url"),
                                sha256 = digest.removePrefix("sha256:").lowercase()
                            ),
                            "새 버전 v$tag를 찾았습니다."
                        )
                    }
                } finally {
                    connection.disconnect()
                }
            }
            update = result.first
            status = result.second
        } catch (error: Exception) {
            status = "업데이트 확인 실패 · 인터넷 연결을 확인해 주세요."
        } finally {
            busy = false
        }
    }

    suspend fun downloadUpdate(info: CameraAppUpdate): File {
        return withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(directory, "luma-camera-ai-update.apk")
            val connection = (URL(info.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Luma-Camera-AI-Android")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("APK 다운로드 서버 응답 오류")
                }
                connection.inputStream.use { input ->
                    apk.outputStream().use { output -> input.copyTo(output) }
                }
            } finally {
                connection.disconnect()
            }
            val actual = MessageDigest.getInstance("SHA-256")
                .digest(apk.readBytes())
                .joinToString("") { "%02x".format(it) }
            if (!actual.equals(info.sha256, ignoreCase = true)) {
                apk.delete()
                throw SecurityException("다운로드 파일 검증에 실패했습니다.")
            }
            apk
        }
    }

    fun installDownloadedApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            status = "앱 설치 권한을 허용한 뒤 다시 설치를 눌러주세요."
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        status = "Android 설치 확인 화면에서 업데이트를 완료해 주세요."
    }

    LaunchedEffect(Unit) { checkForUpdate() }

    Column(modifier = Modifier.padding(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    if (!busy) {
                        update = null
                        downloadedApk = null
                        scope.launch { checkForUpdate() }
                    }
                },
                enabled = !busy
            ) { Text("업데이트 확인") }
        }
        val available = update
        if (available != null) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Luma Camera AI v${available.versionName} 업데이트", style = MaterialTheme.typography.titleSmall)
                    Text("새 버전을 내려받고 파일 무결성을 확인한 뒤 Android 설치 화면을 엽니다.", style = MaterialTheme.typography.bodySmall)
                    Text(status, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !busy && downloadedApk == null,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    status = "업데이트 다운로드 중…"
                                    try {
                                        downloadedApk = downloadUpdate(available)
                                        status = "다운로드 및 SHA-256 검증 완료 · 설치를 진행할 수 있습니다."
                                    } catch (error: Exception) {
                                        status = error.message ?: "업데이트 다운로드에 실패했습니다."
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        ) { Text(if (busy) "처리 중…" else "다운로드") }
                        val ready = downloadedApk
                        if (ready != null) {
                            Button(onClick = { installDownloadedApk(ready) }) { Text("설치") }
                        }
                    }
                }
            }
        }
    }
}

private fun compareVersions(left: String, right: String): Int {
    val a = Regex("\\d+").findAll(left).mapNotNull { it.value.toIntOrNull() }.toList()
    val b = Regex("\\d+").findAll(right).mapNotNull { it.value.toIntOrNull() }.toList()
    for (index in 0 until maxOf(a.size, b.size)) {
        val av = a.getOrElse(index) { 0 }
        val bv = b.getOrElse(index) { 0 }
        if (av != bv) return av.compareTo(bv)
    }
    return 0
}
