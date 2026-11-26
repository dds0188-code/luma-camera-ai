package com.luma.camera.android

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luma.camera.App
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

private const val TAG = "LumaCamera"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LumaCameraApplication() }
    }
}

@Composable
private fun LumaCameraApplication() {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var captureAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var captureStatus by remember { mutableStateOf("카메라 초기화 중…") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        captureStatus = if (granted) "카메라 권한 허용됨" else "카메라 권한이 거부되었습니다."
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    App(
        onCaptureClick = { captureAction?.invoke() },
        captureStatus = captureStatus,
        cameraPreview = {
            if (hasCameraPermission) {
                CameraPreview(
                    onCaptureActionAvailable = { captureAction = it },
                    onCaptureStatus = { captureStatus = it }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF101419)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("카메라 권한이 필요합니다.", color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun CameraPreview(
    onCaptureActionAvailable: ((() -> Unit)?) -> Unit,
    onCaptureStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(lifecycleOwner, cameraProviderFuture, previewView) {
        var imageCapture: ImageCapture? = null
        var isCapturing = false
        val listener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val captureUseCase = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                .build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    captureUseCase
                )
                imageCapture = captureUseCase
                onCaptureStatus("후면 카메라 연결됨 · 촬영 준비 완료")
                onCaptureActionAvailable {
                    val activeCapture = imageCapture
                    if (activeCapture == null) {
                        onCaptureStatus("카메라가 아직 준비되지 않았습니다.")
                    } else if (isCapturing) {
                        onCaptureStatus("사진 저장 중입니다…")
                    } else {
                        isCapturing = true
                        activeCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                        onCaptureStatus("사진 촬영 중…")
                        saveCapture(activeCapture, context, mainExecutor,
                            onSaved = { uri ->
                                isCapturing = false
                                onCaptureStatus("저장 완료 · 갤러리에서 확인할 수 있습니다.")
                                Log.i(TAG, "Photo saved: $uri")
                            },
                            onFailure = { message ->
                                isCapturing = false
                                onCaptureStatus("촬영 실패: $message")
                                Log.e(TAG, "Capture failed: $message")
                            }
                        )
                    }
                }
            } catch (error: Exception) {
                onCaptureActionAvailable(null)
                onCaptureStatus("카메라 연결 실패: ${error.localizedMessage ?: "알 수 없는 오류"}")
                Log.e(TAG, "Camera binding failed", error)
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)
        onDispose {
            onCaptureActionAvailable(null)
            imageCapture = null
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

private fun saveCapture(
    imageCapture: ImageCapture,
    context: android.content.Context,
    executor: Executor,
    onSaved: (Uri) -> Unit,
    onFailure: (String) -> Unit
) {
    val resolver = context.contentResolver
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "LUMA_$timestamp.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LumaCameraAI")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        resolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    ).build()
    imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            val uri = output.savedUri
            if (uri == null) {
                onFailure("저장된 사진 위치를 확인할 수 없습니다.")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val ready = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                runCatching { resolver.update(uri, ready, null, null) }
            }
            onSaved(uri)
        }

        override fun onError(exception: ImageCaptureException) {
            onFailure(exception.localizedMessage ?: "사진을 저장하지 못했습니다.")
        }
    })
}
