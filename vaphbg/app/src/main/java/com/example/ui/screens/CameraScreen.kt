package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.network.models.BlueprintGenerationSpecs
import com.example.network.models.CapturedMediaItem
import com.example.network.models.CapturedMediaType
import com.example.network.models.WalkthroughTelemetry
import com.example.ui.BlueprintViewModel
import com.example.ui.components.GeminiInlineProgressCard
import com.example.ui.components.GeminiProcessingOverlayModal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class CameraCaptureMode(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val mediaType: CapturedMediaType
) {
    SKETCH(
        title = "Sketch / Blueprint",
        subtitle = "Align drawing inside frame",
        icon = Icons.Default.Description,
        mediaType = CapturedMediaType.SKETCH_IMAGE
    ),
    MULTI_ANGLE_PHOTO(
        title = "Room Photo / Multi-Angle",
        subtitle = "Capture 1 to 4 perspective angles",
        icon = Icons.Default.CameraAlt,
        mediaType = CapturedMediaType.MULTI_ANGLE_PHOTOS
    ),
    VIDEO_WALKTHROUGH(
        title = "Video Walkthrough",
        subtitle = "Continuous 360° spatial scan",
        icon = Icons.Default.Videocam,
        mediaType = CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: BlueprintViewModel,
    initialMode: CameraCaptureMode = CameraCaptureMode.SKETCH,
    onBlueprintGenerated: () -> Unit,
    onOpenMediaGallery: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // --- Permissions State ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!hasCameraPermission) {
            Toast.makeText(context, "Camera permission is required for floor plan capture", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionDeniedContent(
            onRequestPermission = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            },
            onBack = onBack
        )
        return
    }

    // --- Camera Operational State ---
    var selectedMode by remember { mutableStateOf(initialMode) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var showGridGuidelines by remember { mutableStateOf(true) }
    var showSpiritLevel by remember { mutableStateOf(true) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(5f) }

    // Camera instances
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    // Multi-photo captures
    val capturedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var capturedVideoFile by remember { mutableStateOf<File?>(null) }

    // Video Recording Timer & Keyframe Extraction
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    val extractedVideoKeyframes = remember { mutableStateListOf<Bitmap>() }

    // Review / Analysis Confirmation Dialog
    var showReviewModal by remember { mutableStateOf(false) }
    var analysisNotes by remember { mutableStateOf("") }
    var selectedCounty by remember { mutableStateOf("Nairobi") }
    var preferredBedrooms by remember { mutableIntStateOf(3) }
    var preferredBathrooms by remember { mutableIntStateOf(2) }
    var targetAreaSqm by remember { mutableFloatStateOf(95f) }

    val isProcessing by viewModel.isProcessing.collectAsState()
    val processStatus by viewModel.processStatus.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()

    // Camera Executor
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Video Recording Timer Loop
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            recordingDurationSeconds = 0
            while (isRecordingVideo) {
                delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // --- CameraX Viewfinder ---
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_viewfinder"),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageCaptureUseCase = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setFlashMode(flashMode)
                        .build()
                    imageCapture = imageCaptureUseCase

                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build()
                    val videoCaptureUseCase = VideoCapture.withOutput(recorder)
                    videoCapture = videoCaptureUseCase

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCaptureUseCase,
                            videoCaptureUseCase
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo

                        camera.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                            minZoomRatio = state.minZoomRatio
                            maxZoomRatio = state.maxZoomRatio
                            zoomRatio = state.zoomRatio
                        }
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Failed to initialize camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                // Update flash or torch mode
                cameraControl?.enableTorch(isTorchEnabled)
                imageCapture?.flashMode = flashMode
            }
        )

        // --- Architectural Alignment Overlay & Grid ---
        if (showGridGuidelines) {
            CameraArchitecturalGrid(
                mode = selectedMode,
                modifier = Modifier.fillMaxSize()
            )
        }

        // --- Spirit Level Indicator (Overhead & Wall leveling) ---
        if (showSpiritLevel) {
            SpiritLevelIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(140.dp)
            )
        }

        // --- Top Control Bar ---
        TopCameraAppBar(
            selectedMode = selectedMode,
            flashMode = flashMode,
            isTorchEnabled = isTorchEnabled,
            showGrid = showGridGuidelines,
            showLevel = showSpiritLevel,
            onToggleFlash = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            },
            onToggleTorch = {
                isTorchEnabled = !isTorchEnabled
                cameraControl?.enableTorch(isTorchEnabled)
            },
            onToggleGrid = { showGridGuidelines = !showGridGuidelines },
            onToggleLevel = { showSpiritLevel = !showSpiritLevel },
            onSwitchLens = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            },
            onBack = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        )

        // --- Video Recording Status Pill ---
        if (isRecordingVideo) {
            RecordingStatusPill(
                durationSeconds = recordingDurationSeconds,
                extractedFrames = extractedVideoKeyframes.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 70.dp)
            )
        }

        // --- Bottom Camera Controls & Mode Selector ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC050811), Color(0xF0050811))
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Controls Pill
            ZoomControlPill(
                currentZoom = zoomRatio,
                minZoom = minZoomRatio,
                maxZoom = maxZoomRatio,
                onZoomChange = { target ->
                    cameraControl?.setZoomRatio(target)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-Photo Captured Strip (if any)
            if (capturedBitmaps.isNotEmpty()) {
                CapturedGalleryStrip(
                    bitmaps = capturedBitmaps,
                    onRemove = { idx ->
                        if (idx in capturedBitmaps.indices) {
                            capturedBitmaps.removeAt(idx)
                        }
                    },
                    onOpenReview = { showReviewModal = true }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Mode Selector Pill Tabs
            CameraModeSelectorRow(
                currentMode = selectedMode,
                onSelectMode = { newMode ->
                    if (!isRecordingVideo) {
                        selectedMode = newMode
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Shutter / Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Left Action: Gallery / Review trigger
                IconButton(
                    onClick = {
                        if (capturedBitmaps.isNotEmpty() || capturedVideoFile != null) {
                            showReviewModal = true
                        } else {
                            onOpenMediaGallery()
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(1.5.dp, Color(0xFF00E5FF), CircleShape)
                        .testTag("camera_gallery_preview_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (capturedBitmaps.isNotEmpty()) {
                                Badge(containerColor = Color(0xFF00E5FF), contentColor = Color.Black) {
                                    Text("${capturedBitmaps.size}")
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Collections,
                            contentDescription = "Review captures",
                            tint = Color.White
                        )
                    }
                }

                // Center: Shutter Button
                CameraShutterButton(
                    mode = selectedMode,
                    isRecording = isRecordingVideo,
                    onClick = {
                        when (selectedMode) {
                            CameraCaptureMode.SKETCH -> {
                                captureSinglePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    executor = cameraExecutor,
                                    onSuccess = { bmp ->
                                        capturedBitmaps.clear()
                                        capturedBitmaps.add(bmp)
                                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                        viewModel.addCapturedMedia(
                                            CapturedMediaItem(
                                                title = "Hand-Drawn Sketch ($timeStr)",
                                                mediaType = CapturedMediaType.SKETCH_IMAGE,
                                                bitmap = bmp,
                                                notes = "High-contrast architectural sketch scan."
                                            )
                                        )
                                        showReviewModal = true
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Capture error: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            CameraCaptureMode.MULTI_ANGLE_PHOTO -> {
                                captureSinglePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    executor = cameraExecutor,
                                    onSuccess = { bmp ->
                                        capturedBitmaps.add(bmp)
                                        val angleNum = capturedBitmaps.size
                                        Toast.makeText(context, "Captured angle #$angleNum", Toast.LENGTH_SHORT).show()
                                        if (capturedBitmaps.size >= 4) {
                                            val angles = capturedBitmaps.toList()
                                            viewModel.addCapturedMedia(
                                                CapturedMediaItem(
                                                    title = "Room Photo Angles (${angles.size} Views)",
                                                    mediaType = CapturedMediaType.MULTI_ANGLE_PHOTOS,
                                                    bitmap = angles.first(),
                                                    secondaryBitmaps = angles.drop(1),
                                                    notes = "Multi-angle physical room photos."
                                                )
                                            )
                                            showReviewModal = true
                                        }
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, "Capture error: $err", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            CameraCaptureMode.VIDEO_WALKTHROUGH -> {
                                if (isRecordingVideo) {
                                    // Stop Video Recording
                                    activeRecording?.stop()
                                    activeRecording = null
                                    isRecordingVideo = false
                                    val frames = if (extractedVideoKeyframes.isNotEmpty()) {
                                        extractedVideoKeyframes.toList()
                                    } else if (capturedBitmaps.isNotEmpty()) {
                                        capturedBitmaps.toList()
                                    } else emptyList()

                                    if (frames.isNotEmpty()) {
                                        viewModel.addCapturedMedia(
                                            CapturedMediaItem(
                                                title = "360° Walkthrough Video (${recordingDurationSeconds}s)",
                                                mediaType = CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES,
                                                bitmap = frames.first(),
                                                secondaryBitmaps = frames.drop(1),
                                                videoDurationSeconds = recordingDurationSeconds,
                                                telemetry = WalkthroughTelemetry(
                                                    durationSeconds = recordingDurationSeconds.coerceAtLeast(4),
                                                    estimatedPacedDistanceMeters = (recordingDurationSeconds * 0.9f).coerceAtLeast(6f),
                                                    detectedRoomsCount = preferredBedrooms + 2,
                                                    deviceHeadingDegrees = 0f
                                                ),
                                                notes = "Spatial video walkthrough frame scans."
                                            )
                                        )
                                    }
                                    showReviewModal = true
                                } else {
                                    // Start Video Recording
                                    val videoFile = File(
                                        context.cacheDir,
                                        "walkthrough_scan_${System.currentTimeMillis()}.mp4"
                                    )
                                    capturedVideoFile = videoFile

                                    val outputOptions = FileOutputOptions.Builder(videoFile).build()

                                    val recording = videoCapture?.output
                                        ?.prepareRecording(context, outputOptions)
                                        ?.apply {
                                            if (hasAudioPermission) {
                                                withAudioEnabled()
                                            }
                                        }
                                        ?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                            when (recordEvent) {
                                                is VideoRecordEvent.Start -> {
                                                    isRecordingVideo = true
                                                }
                                                is VideoRecordEvent.Finalize -> {
                                                    isRecordingVideo = false
                                                    if (recordEvent.hasError()) {
                                                        Toast.makeText(context, "Video recording error: ${recordEvent.error}", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        // Automatically extract sample keyframes for analysis
                                                        if (capturedBitmaps.isNotEmpty()) {
                                                            extractedVideoKeyframes.addAll(capturedBitmaps)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    activeRecording = recording

                                    // Periodic snapshot buffer for keyframe analysis during video
                                    coroutineScope.launch {
                                        while (isRecordingVideo) {
                                            delay(2500)
                                            captureSinglePhoto(
                                                context = context,
                                                imageCapture = imageCapture,
                                                executor = cameraExecutor,
                                                onSuccess = { frameBmp ->
                                                    extractedVideoKeyframes.add(frameBmp)
                                                },
                                                onError = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )

                // Right Action: Switch Camera Lens
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f), CircleShape)
                        .testTag("camera_switch_lens_button")
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera Lens",
                        tint = Color.White
                    )
                }
            }
        }

        // --- Post Capture Review & Analysis BottomSheet / Dialog ---
        if (showReviewModal) {
            CameraReviewAndAnalysisDialog(
                mode = selectedMode,
                bitmaps = if (selectedMode == CameraCaptureMode.VIDEO_WALKTHROUGH && extractedVideoKeyframes.isNotEmpty()) {
                    extractedVideoKeyframes
                } else {
                    capturedBitmaps
                },
                videoDurationSec = recordingDurationSeconds,
                isProcessing = isProcessing,
                processStatus = processStatus,
                analysisError = analysisError,
                analysisNotes = analysisNotes,
                selectedCounty = selectedCounty,
                preferredBedrooms = preferredBedrooms,
                preferredBathrooms = preferredBathrooms,
                targetAreaSqm = targetAreaSqm,
                onNotesChange = { analysisNotes = it },
                onCountyChange = { selectedCounty = it },
                onBedroomsChange = { preferredBedrooms = it },
                onBathroomsChange = { preferredBathrooms = it },
                onAreaChange = { targetAreaSqm = it },
                onDismiss = { showReviewModal = false },
                onRetake = {
                    showReviewModal = false
                    capturedBitmaps.clear()
                    extractedVideoKeyframes.clear()
                    capturedVideoFile = null
                    recordingDurationSeconds = 0
                },
                onOpenMediaGallery = {
                    showReviewModal = false
                    onOpenMediaGallery()
                },
                onAnalyze = {
                    val specs = BlueprintGenerationSpecs(
                        targetAreaSqm = targetAreaSqm,
                        preferredBedrooms = preferredBedrooms,
                        preferredBathrooms = preferredBathrooms,
                        county = selectedCounty,
                        finishesQuality = "Standard",
                        roofType = "Mabati (Iron Sheets)",
                        soilType = "Red Soil"
                    )

                    when (selectedMode) {
                        CameraCaptureMode.SKETCH -> {
                            val bmp = capturedBitmaps.firstOrNull()
                            if (bmp != null) {
                                viewModel.analyzeCapturedMedia(
                                    bitmap = bmp,
                                    mediaType = CapturedMediaType.SKETCH_IMAGE,
                                    extraNotes = analysisNotes,
                                    county = selectedCounty,
                                    specs = specs
                                ) {
                                    showReviewModal = false
                                    onBlueprintGenerated()
                                }
                            }
                        }
                        CameraCaptureMode.MULTI_ANGLE_PHOTO -> {
                            if (capturedBitmaps.isNotEmpty()) {
                                viewModel.analyzeMultiAnglePhotos(
                                    bitmaps = capturedBitmaps.toList(),
                                    extraNotes = analysisNotes,
                                    county = selectedCounty,
                                    specs = specs
                                ) {
                                    showReviewModal = false
                                    onBlueprintGenerated()
                                }
                            }
                        }
                        CameraCaptureMode.VIDEO_WALKTHROUGH -> {
                            val frames = if (extractedVideoKeyframes.isNotEmpty()) {
                                extractedVideoKeyframes.toList()
                            } else if (capturedBitmaps.isNotEmpty()) {
                                capturedBitmaps.toList()
                            } else {
                                emptyList()
                            }

                            val telemetry = WalkthroughTelemetry(
                                durationSeconds = recordingDurationSeconds.coerceAtLeast(5),
                                estimatedPacedDistanceMeters = (recordingDurationSeconds * 0.9f).coerceAtLeast(8f),
                                detectedRoomsCount = preferredBedrooms + 2,
                                deviceHeadingDegrees = 0f
                            )

                            if (frames.isNotEmpty()) {
                                viewModel.analyzeWalkthroughVideoFrames(
                                    frames = frames,
                                    telemetry = telemetry,
                                    county = selectedCounty,
                                    extraNotes = analysisNotes,
                                    specs = specs
                                ) {
                                    showReviewModal = false
                                    onBlueprintGenerated()
                                }
                            } else {
                                Toast.makeText(context, "No video frames available to analyze.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }

        // Fullscreen Gemini Circular Progress Overlay Modal
        if (isProcessing) {
            GeminiProcessingOverlayModal(
                processStatus = processStatus,
                mediaType = selectedMode.mediaType
            )
        }
    }
}

// --- Top App Bar for Camera ---
@Composable
private fun TopCameraAppBar(
    selectedMode: CameraCaptureMode,
    flashMode: Int,
    isTorchEnabled: Boolean,
    showGrid: Boolean,
    showLevel: Boolean,
    onToggleFlash: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleLevel: () -> Unit,
    onSwitchLens: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0x800B132B), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .testTag("camera_back_button")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Mode Title Pill
        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.8f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    selectedMode.icon,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = selectedMode.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Quick Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Flash Mode Toggle
            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("camera_flash_toggle")
            ) {
                val (flashIcon, tint) = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to Color(0xFFFFD54F)
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to Color(0xFF00E5FF)
                    else -> Icons.Default.FlashOff to Color.White.copy(alpha = 0.6f)
                }
                Icon(flashIcon, contentDescription = "Flash", tint = tint, modifier = Modifier.size(20.dp))
            }

            // Torch Toggle (useful for video walkthroughs in dark rooms)
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("camera_torch_toggle")
            ) {
                Icon(
                    Icons.Default.Highlight,
                    contentDescription = "Torch Light",
                    tint = if (isTorchEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Grid Toggle
            IconButton(
                onClick = onToggleGrid,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("camera_grid_toggle")
            ) {
                Icon(
                    Icons.Default.GridOn,
                    contentDescription = "Grid Overlay",
                    tint = if (showGrid) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Spirit Level Toggle
            IconButton(
                onClick = onToggleLevel,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("camera_level_toggle")
            ) {
                Icon(
                    Icons.Default.CompassCalibration,
                    contentDescription = "Spirit Level",
                    tint = if (showLevel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- Architectural Grid & Frame Reticle ---
@Composable
private fun CameraArchitecturalGrid(
    mode: CameraCaptureMode,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 3x3 Architectural Grid Lines
        val gridColor = Color(0x3300E5FF)
        val stroke = 1.2.dp.toPx()

        // Vertical lines
        drawLine(gridColor, Offset(w / 3, 0f), Offset(w / 3, h), stroke)
        drawLine(gridColor, Offset(2 * w / 3, 0f), Offset(2 * w / 3, h), stroke)

        // Horizontal lines
        drawLine(gridColor, Offset(0f, h / 3), Offset(w, h / 3), stroke)
        drawLine(gridColor, Offset(0f, 2 * h / 3), Offset(w, 2 * h / 3), stroke)

        // Mode specific framing reticles
        when (mode) {
            CameraCaptureMode.SKETCH -> {
                // Large Document Framing Box
                val marginX = w * 0.08f
                val marginY = h * 0.16f
                val boxW = w - (2 * marginX)
                val boxH = h - (2 * marginY)

                val cornerLen = 28.dp.toPx()
                val cornerStroke = 3.dp.toPx()
                val accent = Color(0xFF00E5FF)

                // Top Left
                drawLine(accent, Offset(marginX, marginY), Offset(marginX + cornerLen, marginY), cornerStroke)
                drawLine(accent, Offset(marginX, marginY), Offset(marginX, marginY + cornerLen), cornerStroke)

                // Top Right
                drawLine(accent, Offset(marginX + boxW, marginY), Offset(marginX + boxW - cornerLen, marginY), cornerStroke)
                drawLine(accent, Offset(marginX + boxW, marginY), Offset(marginX + boxW, marginY + cornerLen), cornerStroke)

                // Bottom Left
                drawLine(accent, Offset(marginX, marginY + boxH), Offset(marginX + cornerLen, marginY + boxH), cornerStroke)
                drawLine(accent, Offset(marginX, marginY + boxH), Offset(marginX, marginY + boxH - cornerLen), cornerStroke)

                // Bottom Right
                drawLine(accent, Offset(marginX + boxW, marginY + boxH), Offset(marginX + boxW - cornerLen, marginY + boxH), cornerStroke)
                drawLine(accent, Offset(marginX + boxW, marginY + boxH), Offset(marginX + boxW, marginY + boxH - cornerLen), cornerStroke)
            }
            CameraCaptureMode.MULTI_ANGLE_PHOTO, CameraCaptureMode.VIDEO_WALKTHROUGH -> {
                // Center Crosshair for perspective alignment
                val cx = w / 2
                val cy = h / 2
                val crossLen = 16.dp.toPx()
                val crossColor = Color(0x6600E5FF)
                drawLine(crossColor, Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), 2.dp.toPx())
                drawLine(crossColor, Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), 2.dp.toPx())
            }
        }
    }
}

// --- Spirit Level Bubble Indicator ---
@Composable
private fun SpiritLevelIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LevelPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.minDimension / 2 * 0.75f

        // Outer Ring
        drawCircle(
            color = Color(0x3300E5FF),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Inner Center Circle
        drawCircle(
            color = Color(0x5500E5FF),
            radius = radius * 0.35f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Level Target Center Dot
        drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
            radius = 4.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}

// --- Recording Status Pill ---
@Composable
private fun RecordingStatusPill(
    durationSeconds: Int,
    extractedFrames: Int,
    modifier: Modifier = Modifier
) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val pulseTransition = rememberInfiniteTransition(label = "RecPulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotAlpha"
    )

    Surface(
        modifier = modifier,
        color = Color(0xE6E11D48),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.White.copy(alpha = dotAlpha), CircleShape)
            )
            Text(
                text = "REC $timeFormatted",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            VerticalDivider(
                modifier = Modifier.height(14.dp),
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = "$extractedFrames frames buffered",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp
            )
        }
    }
}

// --- Zoom Controls Pill ---
@Composable
private fun ZoomControlPill(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val zoomOptions = listOf(0.5f, 1f, 2f)
    Row(
        modifier = modifier
            .background(Color(0x801E293B), RoundedCornerShape(16.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        zoomOptions.forEach { z ->
            val isSelected = kotlin.math.abs(currentZoom - z) < 0.2f
            Surface(
                color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                shape = CircleShape,
                modifier = Modifier
                    .clickable {
                        val clamped = z.coerceIn(minZoom, maxZoom)
                        onZoomChange(clamped)
                    }
            ) {
                Text(
                    text = "${z}x",
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// --- Mode Selector Row ---
@Composable
private fun CameraModeSelectorRow(
    currentMode: CameraCaptureMode,
    onSelectMode: (CameraCaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraCaptureMode.values().forEach { mode ->
            val isSelected = mode == currentMode
            Surface(
                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0x331E293B),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0x4D64748B)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .clickable { onSelectMode(mode) }
                    .testTag("camera_mode_${mode.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        mode.icon,
                        contentDescription = mode.title,
                        tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = mode.title,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// --- Camera Shutter Button ---
@Composable
private fun CameraShutterButton(
    mode: CameraCaptureMode,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = mode == CameraCaptureMode.VIDEO_WALKTHROUGH

    Box(
        modifier = modifier
            .size(80.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(6.dp)
            .clickable { onClick() }
            .testTag("camera_shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            // Video Shutter: Red Circle or Red Square when recording
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                    .background(Color(0xFFEF4444))
            )
        } else {
            // Photo Shutter: Clean White / Cyan Circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF))
            )
        }
    }
}

// --- Captured Gallery Strip ---
@Composable
private fun CapturedGalleryStrip(
    bitmaps: List<Bitmap>,
    onRemove: (Int) -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bitmaps.forEachIndexed { index, bmp ->
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                    .clickable { onOpenReview() }
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Angle ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Angle Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .background(Color(0xCC000000), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove photo",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// --- Review and Analysis Bottom Sheet / Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraReviewAndAnalysisDialog(
    mode: CameraCaptureMode,
    bitmaps: List<Bitmap>,
    videoDurationSec: Int,
    isProcessing: Boolean,
    processStatus: String,
    analysisError: String?,
    analysisNotes: String,
    selectedCounty: String,
    preferredBedrooms: Int,
    preferredBathrooms: Int,
    targetAreaSqm: Float,
    onNotesChange: (String) -> Unit,
    onCountyChange: (String) -> Unit,
    onBedroomsChange: (Int) -> Unit,
    onBathroomsChange: (Int) -> Unit,
    onAreaChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onRetake: () -> Unit,
    onOpenMediaGallery: () -> Unit = {},
    onAnalyze: () -> Unit
) {
    val counties = listOf("Nairobi", "Kiambu", "Machakos", "Nakuru", "Mombasa", "Kisumu", "Kajiado", "Uasin Gishu")

    ModalBottomSheet(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = Color(0xFF0F172A),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF475569)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Captured Floor Plan Media",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${mode.title} • ${bitmaps.size} key angle(s)",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Gemini 2.5 Vision",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Image / Keyframe Carousel
            if (bitmaps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    bitmaps.forEachIndexed { i, bmp ->
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Angle $i",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "Angle #${i + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Architectural Quality Checklist
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Visual Parsing Checklist",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ChecklistItem(text = "Clear wall boundaries & door openings visible")
                    ChecklistItem(text = "Room function annotations or dimensions framed")
                    ChecklistItem(text = "Perspective alignment calibrated with spirit level")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Extra Prompt / Constraints Input
            OutlinedTextField(
                value = analysisNotes,
                onValueChange = onNotesChange,
                label = { Text("Special Architect Directives (Optional)") },
                placeholder = { Text("e.g. Include ensuite for master bed, open-plan kitchen...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("camera_analysis_notes_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF475569),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Parameters: County & Bedrooms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // County Selector Dropdown
                var countyMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { countyMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Text(text = "County: $selectedCounty", fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = countyMenuExpanded,
                        onDismissRequest = { countyMenuExpanded = false }
                    ) {
                        counties.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    onCountyChange(c)
                                    countyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Target Bedrooms Counter
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Beds: $preferredBedrooms", color = Color.White, fontSize = 12.sp)
                        Row {
                            IconButton(
                                onClick = { if (preferredBedrooms > 1) onBedroomsChange(preferredBedrooms - 1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = { if (preferredBedrooms < 8) onBedroomsChange(preferredBedrooms + 1) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Processing Indicator / Error
            if (isProcessing) {
                GeminiInlineProgressCard(
                    processStatus = processStatus,
                    mediaType = mode.mediaType,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (analysisError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Analysis Notice: $analysisError",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Secondary Gallery Switcher
            OutlinedButton(
                onClick = onOpenMediaGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("camera_modal_open_gallery_button"),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select & Batch Analyze in Media Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("camera_retake_button"),
                    border = BorderStroke(1.dp, Color(0xFF64748B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake")
                }

                Button(
                    onClick = onAnalyze,
                    enabled = !isProcessing && (bitmaps.isNotEmpty() || videoDurationSec > 0),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("camera_analyze_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Blueprint", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp
        )
    }
}

// --- Camera Permission Denied Content ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraPermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Permission Required", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF090D16)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, Color(0xFF00E5FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Access is Essential",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NyumbaPlan uses CameraX to capture hand-drawn sketches, physical room perspectives, and video walkthrough scans for instant Gemini 2.5 Vision floor plan analysis.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("grant_camera_permission_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Helper to capture high-res Bitmap from CameraX ImageCapture ---
private fun captureSinglePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: ExecutorService,
    onSuccess: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    if (imageCapture == null) {
        onError("Camera is not initialized")
        return
    }

    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val buffer: ByteBuffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    // Correct rotation
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val finalBitmap = if (rotationDegrees != 0 && rawBitmap != null) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }

                    imageProxy.close()

                    ContextCompat.getMainExecutor(context).execute {
                        if (finalBitmap != null) {
                            onSuccess(finalBitmap)
                        } else {
                            onError("Failed to decode image bitmap")
                        }
                    }
                } catch (e: Exception) {
                    imageProxy.close()
                    ContextCompat.getMainExecutor(context).execute {
                        onError(e.localizedMessage ?: "Unknown capture error")
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                ContextCompat.getMainExecutor(context).execute {
                    onError(exception.localizedMessage ?: "Camera capture failed")
                }
            }
        }
    )
}
