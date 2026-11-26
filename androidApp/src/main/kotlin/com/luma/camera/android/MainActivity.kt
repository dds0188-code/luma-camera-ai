package com.luma.camera.android

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.luma.camera.App
import com.luma.camera.analysis.CompositionAnalyzer
import com.luma.camera.analysis.CompositionInput
import com.luma.camera.analysis.PhotoQualityAnalyzer
import com.luma.camera.analysis.PhotoQualityInput
import com.luma.camera.analysis.SceneAdviceInput
import com.luma.camera.analysis.SceneAdvisor
import com.luma.camera.analysis.ShootingIntent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

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
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var captureAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var zoomAction by remember { mutableStateOf<((Float) -> Unit)?>(null) }
    var exposureAction by remember { mutableStateOf<((Int) -> Unit)?>(null) }
    var captureStatus by remember { mutableStateOf("카메라 초기화 중…") }
    var zoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
    var exposureIndex by remember { mutableStateOf(0) }
    var minExposureIndex by remember { mutableStateOf(0) }
    var maxExposureIndex by remember { mutableStateOf(0) }
    var faceGuidance by remember { mutableStateOf("사람 얼굴을 찾는 중입니다.") }
    var faceCenterX by remember { mutableStateOf(0.5) }
    var faceCenterY by remember { mutableStateOf(0.5) }
    var faceArea by remember { mutableStateOf(0.0) }
    var horizonTilt by remember { mutableStateOf(0f) }
    var shootingIntent by remember { mutableStateOf(ShootingIntent.PORTRAIT) }
    var photoQualitySummary by remember { mutableStateOf("") }
    var photoQualityLines by remember { mutableStateOf(emptyList<String>()) }
    val qualityExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(qualityExecutor) { onDispose { qualityExecutor.shutdownNow() } }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        captureStatus = if (granted) "카메라 권한 허용됨" else "카메라 권한이 거부되었습니다."
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val sensorManager = remember(context) { context.getSystemService(SensorManager::class.java) }
    DisposableEffect(sensorManager) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            private val rotation = FloatArray(9)
            private val remapped = FloatArray(9)
            private val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                val displayRotation = (context.getSystemService(android.view.WindowManager::class.java)?.defaultDisplay?.rotation ?: Surface.ROTATION_0)
                val axes = when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                if (SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, remapped)) {
                    SensorManager.getOrientation(remapped, orientation)
                    val degrees = Math.toDegrees(orientation[2].toDouble()).toFloat()
                    horizonTilt = if (abs(degrees) > 90f) (if (degrees > 0) degrees - 180f else degrees + 180f) else degrees
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val composition = CompositionAnalyzer.analyze(
        CompositionInput(faceCenterX, faceCenterY, faceArea, horizonTilt.toDouble())
    )
    val compositionHint = composition.suggestions.firstOrNull() ?: "현재 구도를 유지하며 촬영해 보세요."
    val sceneAdvice = SceneAdvisor.advise(
        SceneAdviceInput(
            intent = shootingIntent,
            subjectCenterX = faceCenterX,
            subjectCenterY = faceCenterY,
            subjectAreaRatio = faceArea,
            horizonTiltDegrees = horizonTilt.toDouble(),
            currentZoomRatio = zoomRatio,
            maxZoomRatio = maxZoomRatio
        )
    )
    val guidance = if (faceArea <= 0.0) faceGuidance else {
        val framingHint = when {
            faceArea < 0.025 -> "피사체가 작아요 · 한 단계 가까이 또는 줌 인"
            faceArea > 0.22 -> "얼굴이 화면을 많이 차지해요 · 한 걸음 뒤로 또는 줌 아웃"
            else -> "얼굴 크기가 적절해요 · 삼분할 위치를 확인하세요"
        }
        val tiltHint = when {
            abs(horizonTilt) > 4f -> "수평을 맞춰보세요"
            else -> "수평 양호"
        }
        "$framingHint  ·  $tiltHint  ·  $compositionHint"
    }

    App(
        onCaptureClick = { captureAction?.invoke() },
        captureStatus = captureStatus,
        zoomRatio = zoomRatio,
        maxZoomRatio = maxZoomRatio,
        onZoomChange = { zoomAction?.invoke(it) },
        exposureIndex = exposureIndex,
        minExposureIndex = minExposureIndex,
        maxExposureIndex = maxExposureIndex,
        onExposureChange = { exposureAction?.invoke(it) },
        horizonTiltDegrees = horizonTilt,
        compositionGuidance = guidance,
        shootingIntent = shootingIntent,
        onShootingIntentChange = { shootingIntent = it },
        adviceSummary = sceneAdvice.summary,
        adviceLines = sceneAdvice.recommendations,
        suggestedZoomRatio = sceneAdvice.suggestedZoomRatio,
        onApplySuggestedZoom = { zoomAction?.invoke(it) },
        photoQualitySummary = photoQualitySummary,
        photoQualityLines = photoQualityLines,
        cameraPreview = {
            if (hasCameraPermission) {
                CameraPreview(
                    onCaptureActionAvailable = { captureAction = it },
                    onCaptureStatus = { captureStatus = it },
                    onZoomActionAvailable = { zoomAction = it },
                    onExposureActionAvailable = { exposureAction = it },
                    onZoomInfo = { current, maximum -> zoomRatio = current; maxZoomRatio = maximum },
                    onExposureInfo = { current, minimum, maximum ->
                        exposureIndex = current; minExposureIndex = minimum; maxExposureIndex = maximum
                    },
                    qualityExecutor = qualityExecutor,
                    onPhotoQualitySummary = { photoQualitySummary = it },
                    onPhotoQualityLines = { photoQualityLines = it },
                    onFaceDetected = { x, y, area ->
                        faceCenterX = x; faceCenterY = y; faceArea = area
                        faceGuidance = "얼굴 피사체 감지 · 구도 분석 중"
                    },
                    onFaceLost = { faceArea = 0.0; faceGuidance = "사람 얼굴을 찾는 중입니다." }
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFF101419)), contentAlignment = Alignment.Center) {
                    Text("카메라 권한이 필요합니다.", color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun CameraPreview(
    onCaptureActionAvailable: ((() -> Unit)?) -> Unit,
    onCaptureStatus: (String) -> Unit,
    onZoomActionAvailable: (((Float) -> Unit)?) -> Unit,
    onExposureActionAvailable: (((Int) -> Unit)?) -> Unit,
    onZoomInfo: (Float, Float) -> Unit,
    onExposureInfo: (Int, Int, Int) -> Unit,
    qualityExecutor: java.util.concurrent.ExecutorService,
    onPhotoQualitySummary: (String) -> Unit,
    onPhotoQualityLines: (List<String>) -> Unit,
    onFaceDetected: (Double, Double, Double) -> Unit,
    onFaceLost: () -> Unit
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
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build()
        )
    }

    DisposableEffect(lifecycleOwner, cameraProviderFuture, previewView, detector) {
        var imageCapture: ImageCapture? = null
        var isCapturing = false
        var isDisposed = false
        val analysisBusy = AtomicBoolean(false)
        val listener = Runnable {
            if (isDisposed) return@Runnable
            try {
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null || !analysisBusy.compareAndSet(false, true)) {
                        proxy.close()
                    } else {
                        val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        detector.process(input)
                            .addOnSuccessListener(mainExecutor) { faces ->
                                val face = faces.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height().toLong() }
                                if (face == null) {
                                    onFaceLost()
                                } else {
                                    val bounds = face.boundingBox
                                    val width = mediaImage.width.toDouble().coerceAtLeast(1.0)
                                    val height = mediaImage.height.toDouble().coerceAtLeast(1.0)
                                    val centerX = (bounds.centerX() / width).coerceIn(0.0, 1.0)
                                    val centerY = (bounds.centerY() / height).coerceIn(0.0, 1.0)
                                    val area = (bounds.width().toDouble() * bounds.height().toDouble() / (width * height)).coerceIn(0.0, 1.0)
                                    onFaceDetected(centerX, centerY, area)
                                }
                            }
                            .addOnFailureListener(mainExecutor) { error ->
                                Log.w(TAG, "On-device face detection failed", error)
                            }
                            .addOnCompleteListener {
                                analysisBusy.set(false)
                                proxy.close()
                            }
                    }
                }

                provider.unbindAll()
                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, analysis
                )
                imageCapture = capture
                val zoomState = boundCamera.cameraInfo.zoomState.value
                val maxZoom = zoomState?.maxZoomRatio ?: 1f
                val currentZoom = zoomState?.zoomRatio ?: 1f
                onZoomInfo(currentZoom, maxZoom)
                onZoomActionAvailable { ratio ->
                    val safeRatio = ratio.coerceIn(1f, maxZoom)
                    boundCamera.cameraControl.setZoomRatio(safeRatio)
                    onZoomInfo(safeRatio, maxZoom)
                }
                val exposureRange = boundCamera.cameraInfo.exposureState.exposureCompensationRange
                val currentExposure = boundCamera.cameraInfo.exposureState.exposureCompensationIndex
                onExposureInfo(currentExposure, exposureRange.lower, exposureRange.upper)
                onExposureActionAvailable { index ->
                    val safeIndex = index.coerceIn(exposureRange.lower, exposureRange.upper)
                    boundCamera.cameraControl.setExposureCompensationIndex(safeIndex)
                    onExposureInfo(safeIndex, exposureRange.lower, exposureRange.upper)
                }
                previewView.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> true
                        MotionEvent.ACTION_UP -> {
                            val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            boundCamera.cameraControl.startFocusAndMetering(action)
                                .addListener({ onCaptureStatus("선택한 위치에 초점을 맞추는 중…") }, mainExecutor)
                            true
                        }
                        else -> true
                    }
                }
                onCaptureStatus("후면 카메라 연결됨 · 얼굴 분석 및 터치 초점 준비")
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
                                onCaptureStatus("저장 완료 · 사진 품질 분석 중…")
                                onPhotoQualitySummary("저장된 사진을 기기 안에서 분석하고 있습니다.")
                                onPhotoQualityLines(emptyList())
                                analyzeSavedPhoto(uri, context, qualityExecutor, mainExecutor,
                                    onResult = { result ->
                                        onPhotoQualitySummary("종합 ${result.score}점 · 노출 ${result.exposureScore}점 · 명암 ${result.contrastScore}점 · 디테일 ${result.detailScore}점")
                                        onPhotoQualityLines(result.suggestions)
                                        onCaptureStatus("저장 및 사진 품질 분석 완료 · 갤러리에서도 확인할 수 있습니다.")
                                    },
                                    onFailure = { message ->
                                        onPhotoQualitySummary("사진은 저장됐지만 분석하지 못했습니다.")
                                        onPhotoQualityLines(listOf(message))
                                        onCaptureStatus("사진 저장 완료 · 품질 분석은 실패했습니다.")
                                    }
                                )
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
                onZoomActionAvailable(null)
                onExposureActionAvailable(null)
                onCaptureStatus("카메라 연결 실패: ${error.localizedMessage ?: "알 수 없는 오류"}")
                Log.e(TAG, "Camera binding failed", error)
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)
        onDispose {
            isDisposed = true
            onCaptureActionAvailable(null)
            onZoomActionAvailable(null)
            onExposureActionAvailable(null)
            previewView.setOnTouchListener(null)
            imageCapture = null
            detector.close()
            if (cameraProviderFuture.isDone) runCatching { cameraProviderFuture.get().unbindAll() }
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
        resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
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

private fun analyzeSavedPhoto(
    uri: Uri,
    context: android.content.Context,
    worker: java.util.concurrent.ExecutorService,
    mainExecutor: Executor,
    onResult: (com.luma.camera.analysis.PhotoQualityResult) -> Unit,
    onFailure: (String) -> Unit
) {
    worker.execute {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: throw IllegalStateException("저장된 사진을 열 수 없습니다.")
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > 128 || bounds.outHeight / sampleSize > 128) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IllegalStateException("사진 미리보기를 생성할 수 없습니다.")
            val sample = Bitmap.createScaledBitmap(decoded, 64, 64, true)
            if (sample !== decoded) decoded.recycle()
            try {
                val pixels = IntArray(64 * 64)
                sample.getPixels(pixels, 0, 64, 0, 0, 64, 64)
                val grayscale = pixels.map { color ->
                    val red = (color shr 16) and 0xFF
                    val green = (color shr 8) and 0xFF
                    val blue = color and 0xFF
                    (red * 299 + green * 587 + blue * 114) / 1000
                }
                val result = PhotoQualityAnalyzer.analyze(PhotoQualityInput(64, 64, grayscale))
                mainExecutor.execute { onResult(result) }
            } finally {
                sample.recycle()
            }
        } catch (error: Exception) {
            val message = error.localizedMessage ?: "알 수 없는 분석 오류"
            mainExecutor.execute { onFailure(message) }
        }
    }
}
