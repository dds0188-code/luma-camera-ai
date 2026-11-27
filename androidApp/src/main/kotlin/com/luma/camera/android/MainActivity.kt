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
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCharacteristics
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.luma.camera.App
import com.luma.camera.analysis.CaptureTimerOptions
import com.luma.camera.analysis.CaptureBurstOptions
import com.luma.camera.analysis.ManualExposureOptions
import com.luma.camera.analysis.ManualFocusOptions
import com.luma.camera.analysis.ExposureMeterAnalyzer
import com.luma.camera.analysis.WhiteBalancePreset
import com.luma.camera.analysis.FocusPeakingAnalyzer
import com.luma.camera.analysis.FocusPeakingPoint
import com.luma.camera.analysis.ZebraExposureAnalyzer
import com.luma.camera.analysis.ZebraMark
import com.luma.camera.analysis.CompositionAnalyzer
import com.luma.camera.analysis.CompositionInput
import com.luma.camera.analysis.PhotoQualityAnalyzer
import com.luma.camera.analysis.PhotoQualityInput
import com.luma.camera.analysis.SceneAdviceInput
import com.luma.camera.analysis.SceneAdvisor
import com.luma.camera.analysis.SceneClassifier
import com.luma.camera.analysis.SceneLabelInput
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
    var captureAction by remember { mutableStateOf<((Int, Int) -> Unit)?>(null) }
    var zoomAction by remember { mutableStateOf<((Float) -> Unit)?>(null) }
    var exposureAction by remember { mutableStateOf<((Int) -> Unit)?>(null) }
    var manualExposureAction by remember { mutableStateOf<((Boolean, Int, Long) -> Unit)?>(null) }
    var manualExposureSupported by remember { mutableStateOf(false) }
    var manualExposureEnabled by remember { mutableStateOf(false) }
    var manualIsoRange by remember { mutableStateOf(100..800) }
    var manualIso by remember { mutableStateOf(100) }
    var manualShutterNs by remember { mutableStateOf(16_666_667L) }
    var manualFocusAction by remember { mutableStateOf<((Boolean, Float) -> Unit)?>(null) }
    var manualFocusSupported by remember { mutableStateOf(false) }
    var manualFocusEnabled by remember { mutableStateOf(false) }
    var manualFocusDistance by remember { mutableStateOf(0f) }
    var minimumFocusDistance by remember { mutableStateOf(0f) }
    var exposureHistogram by remember { mutableStateOf(emptyList<Float>()) }
    var exposureWarning by remember { mutableStateOf("노출 분포 분석 중…") }
    var whiteBalanceAction by remember { mutableStateOf<((WhiteBalancePreset) -> Unit)?>(null) }
    var whiteBalancePresets by remember { mutableStateOf(emptyList<WhiteBalancePreset>()) }
    var selectedWhiteBalancePreset by remember { mutableStateOf(WhiteBalancePreset.AUTO) }
    var focusPeakingEnabled by remember { mutableStateOf(false) }
    var focusPeakingPoints by remember { mutableStateOf(emptyList<FocusPeakingPoint>()) }
    var zebraExposureEnabled by remember { mutableStateOf(false) }
    var zebraMarks by remember { mutableStateOf(emptyList<ZebraMark>()) }
    var torchAction by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
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
    var cameraCapabilitySummary by remember { mutableStateOf("카메라 지원 기능 확인 중…") }
    var sceneRecognitionSummary by remember { mutableStateOf("온디바이스 장면 분석 준비 중…") }
    var recognizedShootingIntent by remember { mutableStateOf<ShootingIntent?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var captureTimerSeconds by remember { mutableStateOf(0) }
    var captureBurstCount by remember { mutableStateOf(1) }
    var gridMode by remember { mutableStateOf("삼분할") }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val qualityExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(qualityExecutor) { onDispose { qualityExecutor.shutdownNow() } }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        captureStatus = if (granted) "카메라 권한 허용됨" else "카메라 권한이 거부되었습니다."
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(lensFacing) {
        manualExposureEnabled = false
        manualExposureSupported = false
        manualExposureAction = null
        manualFocusEnabled = false
        manualFocusSupported = false
        manualFocusAction = null
        whiteBalanceAction = null
        whiteBalancePresets = emptyList()
        selectedWhiteBalancePreset = WhiteBalancePreset.AUTO
        focusPeakingPoints = emptyList()
        zebraMarks = emptyList()
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
        isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
        onToggleCamera = { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK },
        onCaptureClick = { captureAction?.invoke(captureTimerSeconds, captureBurstCount) },
        captureTimerSeconds = captureTimerSeconds,
        onCaptureTimerChange = { captureTimerSeconds = it },
        captureBurstCount = captureBurstCount,
        onCaptureBurstCountChange = { captureBurstCount = it },
        gridMode = gridMode,
        onGridModeChange = { gridMode = it },
        captureStatus = captureStatus,
        zoomRatio = zoomRatio,
        maxZoomRatio = maxZoomRatio,
        onZoomChange = { zoomAction?.invoke(it) },
        exposureIndex = exposureIndex,
        minExposureIndex = minExposureIndex,
        maxExposureIndex = maxExposureIndex,
        onExposureChange = { exposureAction?.invoke(it) },
        manualExposureSupported = manualExposureSupported,
        manualExposureEnabled = manualExposureEnabled,
        manualIso = manualIso,
        manualIsoMin = manualIsoRange.first,
        manualIsoMax = manualIsoRange.last,
        manualShutterNs = manualShutterNs,
        onManualIsoChange = { manualIso = it },
        onManualShutterChange = { manualShutterNs = it },
        onManualExposureToggle = { enabled ->
            manualExposureEnabled = enabled
            manualExposureAction?.invoke(enabled, manualIso, manualShutterNs)
        },
        onApplyManualExposure = { manualExposureAction?.invoke(true, manualIso, manualShutterNs) },
        manualFocusSupported = manualFocusSupported,
        manualFocusEnabled = manualFocusEnabled,
        manualFocusDistance = manualFocusDistance,
        minimumFocusDistance = minimumFocusDistance,
        onManualFocusToggle = { enabled ->
            manualFocusEnabled = enabled
            manualFocusAction?.invoke(enabled, manualFocusDistance)
        },
        onManualFocusDistanceChange = { manualFocusDistance = it },
        onApplyManualFocus = { manualFocusAction?.invoke(true, manualFocusDistance) },
        whiteBalancePresets = whiteBalancePresets,
        selectedWhiteBalancePreset = selectedWhiteBalancePreset,
        onWhiteBalancePresetChange = { preset ->
            selectedWhiteBalancePreset = preset
            whiteBalanceAction?.invoke(preset)
        },
        exposureHistogram = exposureHistogram,
        exposureWarning = exposureWarning,
        focusPeakingEnabled = focusPeakingEnabled,
        focusPeakingPoints = focusPeakingPoints,
        onFocusPeakingToggle = { enabled ->
            focusPeakingEnabled = enabled
            if (!enabled) focusPeakingPoints = emptyList()
        },
        zebraExposureEnabled = zebraExposureEnabled,
        zebraMarks = zebraMarks,
        onZebraExposureToggle = { enabled ->
            zebraExposureEnabled = enabled
            if (!enabled) zebraMarks = emptyList()
        },
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
        cameraCapabilitySummary = cameraCapabilitySummary,
        sceneRecognitionSummary = sceneRecognitionSummary,
        recognizedShootingIntent = recognizedShootingIntent,
        onApplyRecognizedScene = { shootingIntent = it },
        torchEnabled = torchEnabled,
        onTorchToggle = { torchAction?.invoke(it) },
        cameraPreview = {
            if (hasCameraPermission) {
                CameraPreview(
                    onCaptureActionAvailable = { captureAction = it },
                    onCaptureStatus = { captureStatus = it },
                    onZoomActionAvailable = { zoomAction = it },
                    onExposureActionAvailable = { exposureAction = it },
                    onManualExposureActionAvailable = { manualExposureAction = it },
                    onManualFocusActionAvailable = { manualFocusAction = it },
                    onWhiteBalanceActionAvailable = { whiteBalanceAction = it },
                    onWhiteBalancePresets = { presets ->
                        whiteBalancePresets = presets
                        if (selectedWhiteBalancePreset !in presets) {
                            val fallback = presets.firstOrNull() ?: WhiteBalancePreset.AUTO
                            selectedWhiteBalancePreset = fallback
                            if (fallback in presets) whiteBalanceAction?.invoke(fallback)
                        }
                    },
                    onRestoreWhiteBalance = { whiteBalanceAction?.invoke(selectedWhiteBalancePreset) },
                    onRestoreManualFocus = { if (manualFocusEnabled) manualFocusAction?.invoke(true, manualFocusDistance) },
                    onManualFocusSupport = { supported, maximum ->
                        manualFocusSupported = supported
                        minimumFocusDistance = maximum
                        manualFocusDistance = manualFocusDistance.coerceIn(0f, maximum)
                        if (!supported) manualFocusEnabled = false
                    },
                    onFocusPeakingPoints = { points -> focusPeakingPoints = points },
                    focusPeakingEnabled = focusPeakingEnabled,
                    zebraExposureEnabled = zebraExposureEnabled,
                    onZebraMarks = { marks -> zebraMarks = marks },
                    onExposureMeter = { histogram, warning ->
                        exposureHistogram = histogram
                        exposureWarning = warning
                    },
                    onManualExposureSupport = { supported, minimum, maximum ->
                        manualExposureSupported = supported
                        if (supported) {
                            manualIsoRange = minimum..maximum
                            manualIso = manualIso.coerceIn(minimum, maximum)
                        } else {
                            manualExposureEnabled = false
                        }
                    },
                    onTorchActionAvailable = { torchAction = it },
                    onTorchInfo = { torchEnabled = it },
                    onZoomInfo = { current, maximum -> zoomRatio = current; maxZoomRatio = maximum },
                    onExposureInfo = { current, minimum, maximum ->
                        exposureIndex = current; minExposureIndex = minimum; maxExposureIndex = maximum
                    },
                    qualityExecutor = qualityExecutor,
                    lensFacing = lensFacing,
                    onPhotoQualitySummary = { photoQualitySummary = it },
                    onPhotoQualityLines = { photoQualityLines = it },
                    onCameraCapabilitySummary = { cameraCapabilitySummary = it },
                    onSceneLabels = { labels ->
                        val classification = SceneClassifier.classify(labels)
                        if (classification == null) {
                            sceneRecognitionSummary = if (labels.isEmpty()) "장면을 분석하고 있습니다." else "장면을 확신 있게 분류하지 못했어요. 피사체를 더 크게 비춰보세요."
                            recognizedShootingIntent = null
                        } else {
                            val percent = (classification.confidence * 100).toInt()
                            val matched = classification.matchedLabels.take(2).joinToString(" · ")
                            sceneRecognitionSummary = "${classification.intent.label} 장면 후보 · 신뢰도 ${percent}% · $matched"
                            recognizedShootingIntent = classification.intent
                        }
                    },
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

@OptIn(ExperimentalCamera2Interop::class)
@Composable
private fun CameraPreview(
    onCaptureActionAvailable: (((Int, Int) -> Unit)?) -> Unit,
    onCaptureStatus: (String) -> Unit,
    onZoomActionAvailable: (((Float) -> Unit)?) -> Unit,
    onExposureActionAvailable: (((Int) -> Unit)?) -> Unit,
    onManualExposureActionAvailable: (((Boolean, Int, Long) -> Unit)?) -> Unit,
    onManualFocusActionAvailable: (((Boolean, Float) -> Unit)?) -> Unit,
    onRestoreManualFocus: () -> Unit,
    onWhiteBalanceActionAvailable: (((WhiteBalancePreset) -> Unit)?) -> Unit,
    onWhiteBalancePresets: (List<WhiteBalancePreset>) -> Unit,
    onRestoreWhiteBalance: () -> Unit,
    onManualFocusSupport: (Boolean, Float) -> Unit,
    onExposureMeter: (List<Float>, String) -> Unit,
    focusPeakingEnabled: Boolean,
    onFocusPeakingPoints: (List<FocusPeakingPoint>) -> Unit,
    zebraExposureEnabled: Boolean,
    onZebraMarks: (List<ZebraMark>) -> Unit,
    onManualExposureSupport: (Boolean, Int, Int) -> Unit,
    onTorchActionAvailable: (((Boolean) -> Unit)?) -> Unit,
    onTorchInfo: (Boolean) -> Unit,
    onZoomInfo: (Float, Float) -> Unit,
    onExposureInfo: (Int, Int, Int) -> Unit,
    qualityExecutor: java.util.concurrent.ExecutorService,
    lensFacing: Int,
    onPhotoQualitySummary: (String) -> Unit,
    onPhotoQualityLines: (List<String>) -> Unit,
    onCameraCapabilitySummary: (String) -> Unit,
    onSceneLabels: (List<SceneLabelInput>) -> Unit,
    onFaceDetected: (Double, Double, Double) -> Unit,
    onFaceLost: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFocusPeakingEnabled by rememberUpdatedState(focusPeakingEnabled)
    val currentZebraExposureEnabled by rememberUpdatedState(zebraExposureEnabled)
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    val detector = remember(lensFacing) {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .enableTracking()
                .build()
        )
    }
    val imageLabeler = remember(lensFacing) { ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS) }

    DisposableEffect(lifecycleOwner, cameraProviderFuture, previewView, detector, imageLabeler, lensFacing) {
        var imageCapture: ImageCapture? = null
        var imageAnalysis: ImageAnalysis? = null
        var boundProvider: ProcessCameraProvider? = null
        var isCapturing = false
        var isDisposed = false
        val timerHandler = Handler(Looper.getMainLooper())
        var countdownRunnable: Runnable? = null
        val analysisBusy = AtomicBoolean(false)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var lastSceneLabelTimestamp = 0L
        var lastExposureMeterTimestamp = 0L
        var lastFocusPeakingTimestamp = 0L
        var lastZebraTimestamp = 0L
        val listener = Runnable {
            if (isDisposed) return@Runnable
            try {
                val provider = cameraProviderFuture.get()
                boundProvider = provider
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis = analysis
                analysis.setAnalyzer(analysisExecutor) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null || !analysisBusy.compareAndSet(false, true)) {
                        proxy.close()
                    } else {
                        try {
                            val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                            val meterNow = SystemClock.elapsedRealtime()
                            if (meterNow - lastExposureMeterTimestamp >= 350L) {
                                lastExposureMeterTimestamp = meterNow
                                val plane = proxy.planes.firstOrNull()
                                if (plane != null) {
                                    val yBuffer = plane.buffer.duplicate()
                                    val crop = proxy.cropRect
                                    val samples = ArrayList<Int>(24 * 14)
                                    for (row in 0 until 14) {
                                        val y = crop.top + ((row + 0.5) * crop.height() / 14).toInt()
                                        for (column in 0 until 24) {
                                            val x = crop.left + ((column + 0.5) * crop.width() / 24).toInt()
                                            val index = yBuffer.position() + y * plane.rowStride + x * plane.pixelStride
                                            if (index in 0 until yBuffer.limit()) samples.add(yBuffer.get(index).toInt() and 0xFF)
                                        }
                                    }
                                    if (samples.isNotEmpty()) {
                                        val meter = ExposureMeterAnalyzer.analyze(samples)
                                        mainExecutor.execute { if (!isDisposed) onExposureMeter(meter.histogram.map { it.toFloat() }, meter.warning) }
                                    }
                                }
                            }
                            if (currentFocusPeakingEnabled && meterNow - lastFocusPeakingTimestamp >= 250L) {
                                lastFocusPeakingTimestamp = meterNow
                                val plane = proxy.planes.firstOrNull()
                                if (plane != null) {
                                    val yBuffer = plane.buffer.duplicate()
                                    val crop = proxy.cropRect
                                    val width = 40
                                    val height = 30
                                    val samples = ArrayList<Int>(width * height)
                                    for (row in 0 until height) {
                                        val y = crop.top + ((row + 0.5) * crop.height() / height).toInt()
                                        for (column in 0 until width) {
                                            val x = crop.left + ((column + 0.5) * crop.width() / width).toInt()
                                            val index = yBuffer.position() + y * plane.rowStride + x * plane.pixelStride
                                            if (index in 0 until yBuffer.limit()) samples.add(yBuffer.get(index).toInt() and 0xFF)
                                        }
                                    }
                                    if (samples.size == width * height) {
                                        val points = FocusPeakingAnalyzer.analyze(
                                            samples = samples,
                                            width = width,
                                            height = height,
                                            threshold = 48,
                                            rotationDegrees = proxy.imageInfo.rotationDegrees,
                                            mirrorHorizontally = lensFacing == CameraSelector.LENS_FACING_FRONT,
                                            maxPoints = 420
                                        )
                                        mainExecutor.execute { if (!isDisposed && currentFocusPeakingEnabled) onFocusPeakingPoints(points) }
                                    }
                                }
                            }

                            if (currentZebraExposureEnabled && meterNow - lastZebraTimestamp >= 300L) {
                                lastZebraTimestamp = meterNow
                                val plane = proxy.planes.firstOrNull()
                                if (plane != null) {
                                    val yBuffer = plane.buffer.duplicate()
                                    val crop = proxy.cropRect
                                    val width = 32
                                    val height = 24
                                    val samples = ArrayList<Int>(width * height)
                                    for (row in 0 until height) {
                                        val y = crop.top + ((row + 0.5) * crop.height() / height).toInt()
                                        for (column in 0 until width) {
                                            val x = crop.left + ((column + 0.5) * crop.width() / width).toInt()
                                            val index = yBuffer.position() + y * plane.rowStride + x * plane.pixelStride
                                            if (index in 0 until yBuffer.limit()) samples.add(yBuffer.get(index).toInt() and 0xFF)
                                        }
                                    }
                                    if (samples.size == width * height) {
                                        val marks = ZebraExposureAnalyzer.analyze(
                                            samples = samples,
                                            width = width,
                                            height = height,
                                            threshold = 245,
                                            rotationDegrees = proxy.imageInfo.rotationDegrees,
                                            mirrorHorizontally = lensFacing == CameraSelector.LENS_FACING_FRONT,
                                            maxMarks = 360
                                        )
                                        mainExecutor.execute { if (!isDisposed && currentZebraExposureEnabled) onZebraMarks(marks) }
                                    }
                                }
                            }
                            val faceTask = detector.process(input)
                            val now = SystemClock.elapsedRealtime()
                            val shouldLabelScene = now - lastSceneLabelTimestamp >= 1_000L
                            val labelTask = if (shouldLabelScene) {
                                lastSceneLabelTimestamp = now
                                imageLabeler.process(input)
                            } else {
                                Tasks.forResult(emptyList<ImageLabel>())
                            }
                            Tasks.whenAllComplete(faceTask, labelTask).addOnCompleteListener(mainExecutor) {
                                if (!isDisposed) {
                                    if (faceTask.isSuccessful) {
                                        val face = faceTask.result.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height().toLong() }
                                        if (face == null) {
                                            onFaceLost()
                                        } else {
                                            val bounds = face.boundingBox
                                            val rotation = proxy.imageInfo.rotationDegrees
                                            val rotatedWidth = if (rotation == 90 || rotation == 270) mediaImage.height else mediaImage.width
                                            val rotatedHeight = if (rotation == 90 || rotation == 270) mediaImage.width else mediaImage.height
                                            val width = rotatedWidth.toDouble().coerceAtLeast(1.0)
                                            val height = rotatedHeight.toDouble().coerceAtLeast(1.0)
                                            val rawCenterX = (bounds.centerX() / width).coerceIn(0.0, 1.0)
                                            val centerX = if (lensFacing == CameraSelector.LENS_FACING_FRONT) 1.0 - rawCenterX else rawCenterX
                                            val centerY = (bounds.centerY() / height).coerceIn(0.0, 1.0)
                                            val area = (bounds.width().toDouble() * bounds.height().toDouble() / (width * height)).coerceIn(0.0, 1.0)
                                            onFaceDetected(centerX, centerY, area)
                                        }
                                    } else {
                                        Log.w(TAG, "On-device face detection failed", faceTask.exception)
                                    }
                                    if (shouldLabelScene) {
                                        if (labelTask.isSuccessful) {
                                            onSceneLabels(labelTask.result.map { SceneLabelInput(it.text, it.confidence.toDouble()) })
                                        } else {
                                            Log.w(TAG, "On-device scene labeling failed", labelTask.exception)
                                        }
                                    }
                                }
                                analysisBusy.set(false)
                                proxy.close()
                            }
                        } catch (error: Exception) {
                            analysisBusy.set(false)
                            proxy.close()
                            Log.w(TAG, "Camera frame analysis failed", error)
                        }
                    }
                }

                provider.unbindAll()
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                if (!provider.hasCamera(cameraSelector)) {
                    throw IllegalStateException(if (lensFacing == CameraSelector.LENS_FACING_FRONT) "전면 카메라를 찾을 수 없습니다." else "후면 카메라를 찾을 수 없습니다.")
                }
                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, capture, analysis
                )
                imageCapture = capture

                // Read Camera2 characteristics for the active rear camera. This is inspection only.
                val camera2Info = Camera2CameraInfo.from(boundCamera.cameraInfo)
                val isoRange = camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                val shutterRange = camera2Info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                val awbModes = camera2Info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
                    ?: intArrayOf(CameraCharacteristics.CONTROL_AWB_MODE_AUTO)
                val capabilities = camera2Info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?: intArrayOf()
                val manualSensorSupported = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
                val minimumFocus = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
                val camera2Control = runCatching { Camera2CameraControl.from(boundCamera.cameraControl) }.getOrNull()
                val supportedWhiteBalancePresets = awbModes.toList().mapNotNull { mode ->
                    when (mode) {
                        CameraCharacteristics.CONTROL_AWB_MODE_AUTO -> WhiteBalancePreset.AUTO
                        CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT -> WhiteBalancePreset.INCANDESCENT
                        CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT -> WhiteBalancePreset.FLUORESCENT
                        CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT -> WhiteBalancePreset.WARM_FLUORESCENT
                        CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT -> WhiteBalancePreset.DAYLIGHT
                        CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> WhiteBalancePreset.CLOUDY_DAYLIGHT
                        CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT -> WhiteBalancePreset.TWILIGHT
                        CameraCharacteristics.CONTROL_AWB_MODE_SHADE -> WhiteBalancePreset.SHADE
                        else -> null
                    }
                }.distinct()
                onWhiteBalanceActionAvailable { preset ->
                    if (preset !in supportedWhiteBalancePresets || camera2Control == null) {
                        onCaptureStatus("선택한 화이트밸런스 모드를 지원하지 않습니다.")
                    } else {
                        val options = CaptureRequestOptions.Builder()
                            .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, preset.toCamera2Mode())
                            .build()
                        val future = camera2Control.addCaptureRequestOptions(options)
                        future.addListener({
                            runCatching { future.get() }
                                .onSuccess { onCaptureStatus("화이트밸런스 적용 · ${preset.label}") }
                                .onFailure { error -> onCaptureStatus("화이트밸런스 적용 실패: ${error.localizedMessage ?: "카메라 오류"}") }
                        }, mainExecutor)
                    }
                }
                onWhiteBalancePresets(supportedWhiteBalancePresets)
                val manualFocusAvailable = manualSensorSupported && minimumFocus.isFinite() && minimumFocus > 0f && camera2Control != null
                onManualFocusSupport(manualFocusAvailable, if (manualFocusAvailable) minimumFocus else 0f)
                val manualExposureAvailable = manualSensorSupported &&
                    isoRange != null && isoRange.lower > 0 &&
                    shutterRange != null && shutterRange.lower > 0 &&
                    camera2Control != null
                onManualExposureSupport(manualExposureAvailable, isoRange?.lower ?: 100, isoRange?.upper ?: 800)
                onManualFocusActionAvailable { enabled, requestedDistance ->
                    if (!manualFocusAvailable || camera2Control == null) {
                        onCaptureStatus("이 렌즈는 수동 초점을 지원하지 않습니다.")
                    } else {
                        val options = CaptureRequestOptions.Builder()
                            .setCaptureRequestOption(
                                CaptureRequest.CONTROL_AF_MODE,
                                if (enabled) CaptureRequest.CONTROL_AF_MODE_OFF else CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            .apply {
                                if (enabled) setCaptureRequestOption(
                                    CaptureRequest.LENS_FOCUS_DISTANCE,
                                    ManualFocusOptions.normalizeDiopters(requestedDistance, minimumFocus)
                                )
                            }
                            .build()
                        val future = camera2Control.addCaptureRequestOptions(options)
                        future.addListener({
                            runCatching { future.get() }
                                .onSuccess { onCaptureStatus(if (enabled) "수동 초점 적용 · ${"%.2f".format(requestedDistance)} D" else "자동 초점으로 전환했습니다.") }
                                .onFailure { error -> onCaptureStatus("초점 설정 실패: ${error.localizedMessage ?: "카메라 오류"}") }
                        }, mainExecutor)
                    }
                }
                onManualExposureActionAvailable { enabled, requestedIso, requestedShutterNs ->
                    if (!manualExposureAvailable || camera2Control == null) {
                        onCaptureStatus("이 카메라는 수동 ISO·셔터를 지원하지 않습니다.")
                    } else if (!enabled) {
                        val future = camera2Control.clearCaptureRequestOptions()
                        future.addListener({
                            runCatching { future.get() }
                                .onSuccess {
                                    onRestoreManualFocus()
                                    onRestoreWhiteBalance()
                                    onCaptureStatus("자동 노출 모드로 전환했습니다.")
                                }
                                .onFailure { error -> onCaptureStatus("자동 노출 전환 실패: ${error.localizedMessage ?: "카메라 오류"}") }
                        }, mainExecutor)
                    } else {
                        val safeIso = ManualExposureOptions.normalizeIso(requestedIso, isoRange!!.lower, isoRange.upper)
                        val safeShutterNs = ManualExposureOptions.normalizeExposureTime(requestedShutterNs, shutterRange!!.lower, shutterRange.upper)
                        val options = CaptureRequestOptions.Builder()
                            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                            .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, safeIso)
                            .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, safeShutterNs)
                            .build()
                        val future = camera2Control.addCaptureRequestOptions(options)
                        future.addListener({
                            runCatching { future.get() }
                                .onSuccess { onCaptureStatus("수동 노출 적용 · ISO $safeIso · ${"%.1f".format(safeShutterNs / 1_000_000.0)} ms") }
                                .onFailure { error -> onCaptureStatus("수동 노출 적용 실패: ${error.localizedMessage ?: "카메라 오류"}") }
                        }, mainExecutor)
                    }
                }
                val awbNames = awbModes.toList().mapNotNull { mode ->
                    when (mode) {
                        CameraCharacteristics.CONTROL_AWB_MODE_AUTO -> "자동"
                        CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT -> "백열등"
                        CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT -> "형광등"
                        CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT -> "온백색 형광등"
                        CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT -> "주광"
                        CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "흐림"
                        CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT -> "황혼"
                        CameraCharacteristics.CONTROL_AWB_MODE_SHADE -> "그늘"
                        else -> null
                    }
                }.distinct()
                val isoText = isoRange?.let { "ISO ${it.lower}–${it.upper}" } ?: "ISO 범위 미제공"
                val shutterText = shutterRange?.let {
                    "셔터 ${"%.2f".format(it.lower / 1_000_000.0)}–${"%.2f".format(it.upper / 1_000_000.0)} ms"
                } ?: "셔터 범위 미제공"
                val manualText = if (manualSensorSupported && isoRange != null && shutterRange != null) {
                    "수동 ISO·셔터 지원"
                } else {
                    "수동 ISO·셔터 미지원 또는 제한"
                }
                onCameraCapabilitySummary(
                    (if (lensFacing == CameraSelector.LENS_FACING_FRONT) "전면 카메라 · " else "후면 카메라 · ") +
                        (if (boundCamera.cameraInfo.hasFlashUnit()) "플래시 있음" else "플래시 없음") + "\n" +
                        manualText + " · " + isoText + " · " + shutterText + "\n" +
                        "화이트밸런스: " + awbNames.joinToString(" · ").ifBlank { "지원 모드 확인 불가" }
                )

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
                onTorchInfo(false)
                onTorchActionAvailable { enabled ->
                    if (boundCamera.cameraInfo.hasFlashUnit()) {
                        boundCamera.cameraControl.enableTorch(enabled)
                            .addListener({ onTorchInfo(enabled) }, mainExecutor)
                    }
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
                onCaptureStatus((if (lensFacing == CameraSelector.LENS_FACING_FRONT) "전면" else "후면") + " 카메라 연결됨 · 얼굴/장면 분석 및 터치 초점 준비")
                fun captureNow(remainingShots: Int, totalShots: Int) {
                    val activeCapture = imageCapture
                    if (activeCapture == null) {
                        isCapturing = false
                        onCaptureStatus("카메라가 아직 준비되지 않았습니다.")
                        return
                    }
                    activeCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                    val completedShots = totalShots - remainingShots
                    onCaptureStatus(if (totalShots > 1) "연속 촬영 중 · ${completedShots + 1}/$totalShots" else "사진 촬영 중…")
                    saveCapture(activeCapture, context, mainExecutor,
                        onSaved = { uri ->
                            if (remainingShots > 1) {
                                onCaptureStatus("연속 촬영 저장 중 · ${completedShots + 1}/$totalShots")
                                captureNow(remainingShots - 1, totalShots)
                            } else {
                                isCapturing = false
                                onCaptureStatus("저장 완료 · 사진 품질 분석 중…")
                                onPhotoQualitySummary("저장된 사진을 기기 안에서 분석하고 있습니다.")
                                onPhotoQualityLines(emptyList())
                                analyzeSavedPhoto(uri, context, qualityExecutor, mainExecutor,
                                    onResult = { result ->
                                        onPhotoQualitySummary("종합 ${result.score}점 · 노출 ${result.exposureScore}점 · 명암 ${result.contrastScore}점 · 디테일 ${result.detailScore}점")
                                        onPhotoQualityLines(result.suggestions)
                                        onCaptureStatus(if (totalShots > 1) "${totalShots}장 저장 및 마지막 사진 분석 완료" else "저장 및 사진 품질 분석 완료 · 갤러리에서도 확인할 수 있습니다.")
                                    },
                                    onFailure = { message ->
                                        onPhotoQualitySummary("사진은 저장됐지만 분석하지 못했습니다.")
                                        onPhotoQualityLines(listOf(message))
                                        onCaptureStatus("사진 저장 완료 · 품질 분석은 실패했습니다.")
                                    }
                                )
                                Log.i(TAG, "Photo saved: $uri")
                            }
                        },
                        onFailure = { message ->
                            isCapturing = false
                            onCaptureStatus("촬영 실패: $message")
                            Log.e(TAG, "Capture failed: $message")
                        }
                    )
                }
                onCaptureActionAvailable { delaySeconds, requestedBurstCount ->
                    if (isCapturing) {
                        onCaptureStatus("카운트다운 또는 촬영이 이미 진행 중입니다.")
                    } else {
                        isCapturing = true
                        val safeDelay = CaptureTimerOptions.normalize(delaySeconds)
                        val burstCount = CaptureBurstOptions.normalize(requestedBurstCount)
                        if (safeDelay == 0) {
                            captureNow(burstCount, burstCount)
                        } else {
                            var remaining = safeDelay
                            lateinit var tick: Runnable
                            tick = Runnable {
                                if (isDisposed) return@Runnable
                                if (remaining <= 0) {
                                    countdownRunnable = null
                                    captureNow(burstCount, burstCount)
                                } else {
                                    onCaptureStatus("${remaining}초 후 ${burstCount}장 촬영")
                                    remaining -= 1
                                    timerHandler.postDelayed(tick, 1000L)
                                }
                            }
                            countdownRunnable = tick
                            timerHandler.post(tick)
                        }
                    }
                }
            } catch (error: Exception) {
                onCaptureActionAvailable(null)
                onZoomActionAvailable(null)
                onExposureActionAvailable(null)
                onManualExposureActionAvailable(null)
                onManualFocusActionAvailable(null)
                onWhiteBalanceActionAvailable(null)
                onWhiteBalancePresets(emptyList())
                onFocusPeakingPoints(emptyList())
                onZebraMarks(emptyList())
                onManualFocusSupport(false, 0f)
                onManualExposureSupport(false, 100, 800)
                onTorchActionAvailable(null)
                onCaptureStatus("카메라 연결 실패: ${error.localizedMessage ?: "알 수 없는 오류"}")
                Log.e(TAG, "Camera binding failed", error)
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)
        onDispose {
            isDisposed = true
            countdownRunnable?.let(timerHandler::removeCallbacks)
            countdownRunnable = null
            isCapturing = false
            onCaptureActionAvailable(null)
            onZoomActionAvailable(null)
            onExposureActionAvailable(null)
            onManualExposureActionAvailable(null)
            onManualFocusActionAvailable(null)
            onWhiteBalanceActionAvailable(null)
            onWhiteBalancePresets(emptyList())
            onFocusPeakingPoints(emptyList())
            onZebraMarks(emptyList())
            onManualFocusSupport(false, 0f)
            onManualExposureSupport(false, 100, 800)
            onTorchActionAvailable(null)
            previewView.setOnTouchListener(null)
            imageAnalysis?.clearAnalyzer()
            imageAnalysis = null
            imageCapture = null
            runCatching { boundProvider?.unbindAll() }
            boundProvider = null
            analysisExecutor.shutdown()
            detector.close()
            imageLabeler.close()
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
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
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

private fun WhiteBalancePreset.toCamera2Mode(): Int = when (this) {
    WhiteBalancePreset.AUTO -> CameraCharacteristics.CONTROL_AWB_MODE_AUTO
    WhiteBalancePreset.INCANDESCENT -> CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT
    WhiteBalancePreset.FLUORESCENT -> CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT
    WhiteBalancePreset.WARM_FLUORESCENT -> CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT
    WhiteBalancePreset.DAYLIGHT -> CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT
    WhiteBalancePreset.CLOUDY_DAYLIGHT -> CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
    WhiteBalancePreset.TWILIGHT -> CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT
    WhiteBalancePreset.SHADE -> CameraCharacteristics.CONTROL_AWB_MODE_SHADE
}
