package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.network.models.BlueprintGenerationSpecs
import com.example.network.models.CapturedMediaType
import com.example.network.models.WalkthroughTelemetry
import com.example.ui.BlueprintViewModel
import com.example.ui.audio.VoiceCommandAction
import com.example.ui.audio.VoiceCommandHUD
import com.example.ui.audio.rememberVoiceCommandManager
import com.example.ui.components.GeminiCircularProgressIndicator
import com.example.ui.theme.AccentGreen
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorSetupScreen(
    viewModel: BlueprintViewModel,
    onBlueprintGenerated: () -> Unit,
    onBack: () -> Unit,
    onOpenLiveCamera: (CameraCaptureMode) -> Unit = {},
    onOpenMediaGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val isProcessing by viewModel.isProcessing.collectAsState()
    val processStatus by viewModel.processStatus.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Local Rules, 1: AI Sketch, 2: Video Scanner

    // --- Tab 0: Rule-Based Form Parameters ---
    var title by remember { mutableStateOf("My Custom Bungalow") }
    var bedrooms by remember { mutableStateOf(3) }
    var bathrooms by remember { mutableStateOf(2) }
    var targetArea by remember { mutableStateOf(110f) }
    var finishesQuality by remember { mutableStateOf("Standard") }
    var roofType by remember { mutableStateOf("Mabati (Iron Sheets)") }
    var soilType by remember { mutableStateOf("Red Soil") }
    var selectedCounty by remember { mutableStateOf("Nairobi") }

    // --- Tab 1: Sketch Photo Setup ---
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extraNotes by remember { mutableStateOf("") }

    // Hand-drawn sketch sample assets
    val sampleSketches = remember {
        listOf(
            SampleSketchInfo("Simple 1-Bedroom Layout", "sketch_1bed", createSimpleSketch(1)),
            SampleSketchInfo("Modern 2-Bedroom Layout", "sketch_2bed", createSimpleSketch(2)),
            SampleSketchInfo("Large 3-Bedroom Layout", "sketch_3bed", createSimpleSketch(3))
        )
    }

    // Camera launcher for capturing sketch photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
        }
    }

    // Gallery launcher for selecting sketch photo
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Tab 2: Walkthrough Video Capture ---
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
    }

    var isScanningWalkthrough by remember { mutableStateOf(false) }
    var walkthroughProgress by remember { mutableStateOf(0f) }
    var scanningRoomName by remember { mutableStateOf("Living Room") }

    LaunchedEffect(isScanningWalkthrough) {
        if (isScanningWalkthrough) {
            // Step through walkthrough scanning simulation
            var count = 0
            while (count < 100) {
                kotlinx.coroutines.delay(120)
                count += 4
                walkthroughProgress = count / 100f
                scanningRoomName = when {
                    count < 30 -> "Living Room & Entrance"
                    count < 60 -> "Main Corridor & Kitchen"
                    count < 85 -> "Master Suite Bedroom"
                    else -> "Common Toilet & Bath"
                }
            }
            isScanningWalkthrough = false

            // Generate sample keyframes from room zones for multimodal analysis
            val sampleKeyframes = listOf(
                createSimpleSketch(bedrooms),
                createSimpleSketch(bedrooms.coerceAtLeast(2))
            )

            val telemetry = WalkthroughTelemetry(
                durationSeconds = 18,
                estimatedPacedDistanceMeters = 14.2f,
                detectedRoomsCount = bedrooms + 2,
                deviceHeadingDegrees = 45f
            )

            val specs = BlueprintGenerationSpecs(
                targetAreaSqm = targetArea,
                preferredBedrooms = bedrooms,
                preferredBathrooms = bathrooms,
                county = selectedCounty,
                roofType = roofType,
                finishesQuality = finishesQuality,
                soilType = soilType
            )

            // Submit walkthrough telemetry and video frames to Gemini Floor Plan Analysis Service
            viewModel.analyzeWalkthroughVideoFrames(
                frames = sampleKeyframes,
                telemetry = telemetry,
                county = selectedCounty,
                extraNotes = "Captured from real-time continuous video walkthrough scan",
                specs = specs,
                onComplete = {
                    onBlueprintGenerated()
                }
            )
        }
    }

    // Hands-Free Voice Command Controller
    val voiceManager = rememberVoiceCommandManager { action ->
        when (action) {
            VoiceCommandAction.START_SCAN -> {
                if (selectedTab == 2) {
                    isScanningWalkthrough = true
                } else {
                    viewModel.generateLocalRuleBasedPlan(
                        numBedrooms = bedrooms,
                        numBathrooms = bathrooms,
                        finishesQuality = finishesQuality,
                        roofType = roofType,
                        soilType = soilType,
                        targetAreaSqm = targetArea,
                        title = title,
                        county = selectedCounty
                    )
                    onBlueprintGenerated()
                }
            }
            VoiceCommandAction.CALCULATE_COSTS, VoiceCommandAction.SAVE_PROJECT, VoiceCommandAction.VIEW_3D, VoiceCommandAction.VIEW_2D -> {
                viewModel.generateLocalRuleBasedPlan(
                    numBedrooms = bedrooms,
                    numBathrooms = bathrooms,
                    finishesQuality = finishesQuality,
                    roofType = roofType,
                    soilType = soilType,
                    targetAreaSqm = targetArea,
                    title = title,
                    county = selectedCounty
                )
                onBlueprintGenerated()
            }
            VoiceCommandAction.SHOW_HELP -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Design Layout", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            VoiceCommandHUD(
                manager = voiceManager,
                onExecuteAction = { action ->
                    when (action) {
                        VoiceCommandAction.START_SCAN -> {
                            if (selectedTab == 2) {
                                isScanningWalkthrough = true
                            } else {
                                viewModel.generateLocalRuleBasedPlan(
                                    numBedrooms = bedrooms,
                                    numBathrooms = bathrooms,
                                    finishesQuality = finishesQuality,
                                    roofType = roofType,
                                    soilType = soilType,
                                    targetAreaSqm = targetArea,
                                    title = title,
                                    county = selectedCounty
                                )
                                onBlueprintGenerated()
                            }
                        }
                        VoiceCommandAction.CALCULATE_COSTS, VoiceCommandAction.SAVE_PROJECT, VoiceCommandAction.VIEW_3D, VoiceCommandAction.VIEW_2D -> {
                            viewModel.generateLocalRuleBasedPlan(
                                numBedrooms = bedrooms,
                                numBathrooms = bathrooms,
                                finishesQuality = finishesQuality,
                                roofType = roofType,
                                soilType = soilType,
                                targetAreaSqm = targetArea,
                                title = title,
                                county = selectedCounty
                            )
                            onBlueprintGenerated()
                        }
                        VoiceCommandAction.SHOW_HELP -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mode Select Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Local Parameters", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Rules", modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_local_rules")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Sketch Photo (AI)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "AI Sketch", modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_ai_sketch")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Walkthrough Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.CompassCalibration, contentDescription = "Scanner", modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_walkthrough_scanner")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Contents
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> {
                            // Local Rules Form
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Rule-Based Generative Setup",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Applies strict architectural alignment blocks so bedrooms, kitchens, and toilets fit cohesively without overlapping, following physical planning acts.",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Project / Owner Title") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_project_title"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Regional County Zoning Preset", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedCounty = when (selectedCounty) {
                                                    "Nairobi" -> "Kiambu"
                                                    "Kiambu" -> "Mombasa"
                                                    "Mombasa" -> "Machakos"
                                                    "Machakos" -> "Nakuru"
                                                    "Nakuru" -> "Kisumu"
                                                    else -> "Nairobi"
                                                }
                                            }
                                            .padding(12.dp)
                                            .testTag("county_preset_selector"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(selectedCounty, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            val currentPreset = com.example.data.CountyZoningPresets.getPreset(selectedCounty)
                                            Text(
                                                "Max Coverage: ${currentPreset.maxCoveragePct}% • Min Setback: ${currentPreset.minSetbackMeters}m",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                currentPreset.description,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp,
                                                lineHeight = 12.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select County", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Bedroom and Bathroom Selectors
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Bedrooms: $bedrooms", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = bedrooms.toFloat(),
                                            onValueChange = { bedrooms = it.toInt() },
                                            valueRange = 1f..4f,
                                            steps = 2,
                                            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary, thumbColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.testTag("slider_bedrooms")
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Bathrooms: $bathrooms", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = bathrooms.toFloat(),
                                            onValueChange = { bathrooms = it.toInt() },
                                            valueRange = 1f..3f,
                                            steps = 1,
                                            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary, thumbColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.testTag("slider_bathrooms")
                                        )
                                    }
                                }

                                // Target Area
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Target Floor Area: ${targetArea.toInt()} sqm",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Slider(
                                        value = targetArea,
                                        onValueChange = { targetArea = it },
                                        valueRange = 40f..200f,
                                        colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary, thumbColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.testTag("slider_target_area")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("40 sqm (Starter)", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                                        Text("120 sqm (Standard)", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                                        Text("200 sqm (Premium)", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                                    }
                                }

                                // dropdown inputs for soil, roof, finishes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Soil Quality", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    soilType = when (soilType) {
                                                        "Red Soil" -> "Clay (Black Cotton)"
                                                        "Clay (Black Cotton)" -> "Sandy Rock"
                                                        else -> "Red Soil"
                                                    }
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(soilType, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Roof Structure", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    roofType = when (roofType) {
                                                        "Mabati (Iron Sheets)" -> "Concrete Slab"
                                                        "Concrete Slab" -> "Decra Tiles"
                                                        else -> "Mabati (Iron Sheets)"
                                                    }
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(roofType, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Finishes Specification", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .clickable {
                                                finishesQuality = when (finishesQuality) {
                                                    "Budget" -> "Standard"
                                                    "Standard" -> "Premium"
                                                    else -> "Budget"
                                                }
                                            }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(finishesQuality, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.generateLocalRuleBasedPlan(
                                            numBedrooms = bedrooms,
                                            numBathrooms = bathrooms,
                                            finishesQuality = finishesQuality,
                                            roofType = roofType,
                                            soilType = soilType,
                                            targetAreaSqm = targetArea,
                                            title = title,
                                            county = selectedCounty
                                        )
                                        onBlueprintGenerated()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("generate_local_plan_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Generate Local Floor Plan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        1 -> {
                            // AI Sketch Photo
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Multimodal AI Vision Setup",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Snap a photo of your hand-drawn layout sketch or load a reference schematic plan. Gemini Vision AI analyzes boundaries, wall orientations, and compiles the room dimensions directly.",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Photo Actions
                                Button(
                                    onClick = { onOpenLiveCamera(CameraCaptureMode.SKETCH) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_camerax_viewfinder_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "CameraX", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open CameraX Smart Viewfinder (AI)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { cameraLauncher.launch(null) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("snap_sketch_photo_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Quick Photo", fontSize = 11.sp)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("upload_sketch_gallery_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Photo, contentDescription = "Gallery", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Gallery Pick", fontSize = 11.sp)
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = onOpenMediaGallery,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_captured_media_gallery_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF00E5FF)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Collections, contentDescription = "Captured Media", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Captured Media Gallery & Batch Select", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // Display selected image
                                selectedBitmap?.let { bmp ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Selected Sketch",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { selectedBitmap = null },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                }

                                // Sample sketches quick picker
                                Column {
                                    Text(
                                        text = "Or choose a quick reference sample sketch:",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(sampleSketches) { s ->
                                            Column(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .background(
                                                        if (selectedBitmap == s.bitmap) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        width = if (selectedBitmap == s.bitmap) 1.5.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedBitmap = s.bitmap }
                                                    .padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Image(
                                                    bitmap = s.bitmap.asImageBitmap(),
                                                    contentDescription = s.title,
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = s.title,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = extraNotes,
                                    onValueChange = { extraNotes = it },
                                    label = { Text("Extra AI Instructions (e.g. 3 bedroom, modern roof)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_ai_notes"),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                analysisError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        selectedBitmap?.let {
                                            viewModel.analyzeSketchWithGemini(
                                                bitmap = it,
                                                extraNotes = extraNotes,
                                                county = selectedCounty,
                                                onComplete = {
                                                    onBlueprintGenerated()
                                                }
                                            )
                                        }
                                    },
                                    enabled = selectedBitmap != null,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("analyze_sketch_photo_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Analyze & Generate with Gemini AI", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        2 -> {
                            // Video Scanner Tab
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!cameraPermissionGranted) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = "Camera Needed", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(64.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Walkthrough Scan requires Camera Access",
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "This simulates walk-through telemetry tracking of physical room walls using on-device ML edge outlines.",
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Text("Grant Camera Permission")
                                        }
                                    }
                                } else {
                                    // Custom camera layout with overlay telemetry vectors
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Camera Preview View Integration
                                        CameraPreview()

                                        // Overlaid Architectural Telemetry Graphics (Sci-fi vectors)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                                                        radius = 800f
                                                    )
                                                )
                                        )

                                        // Telemetry indicators
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Top overlay: active telemetry configs
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(AccentGreen, CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("EDGE DETECTION ACTIVE", color = AccentGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "LATENCY: 12ms",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }

                                            // Center Crosshairs and contour detection markers
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    // Crosshair lines
                                                    Box(
                                                        modifier = Modifier
                                                            .size(60.dp)
                                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    
                                                    if (isScanningWalkthrough) {
                                                        Text("SCANNING: $scanningRoomName", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        LinearProgressIndicator(
                                                            progress = { walkthroughProgress },
                                                            color = Color.Yellow,
                                                            trackColor = MaterialTheme.colorScheme.outline,
                                                            modifier = Modifier.width(150.dp)
                                                        )
                                                    } else {
                                                        Text("ALIGN DEVICE WITH WALL INTERSECTIONS", color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                        Text("Simulated Depth estimation: 3.82 meters", color = MaterialTheme.colorScheme.outline, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            }

                                            // Bottom Controls
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onOpenLiveCamera(CameraCaptureMode.VIDEO_WALKTHROUGH) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    ),
                                                    shape = RoundedCornerShape(30.dp),
                                                    modifier = Modifier.testTag("open_camerax_walkthrough_button")
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Videocam, contentDescription = "CameraX Video")
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Launch CameraX 360° AI Video Scanner", fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                if (!isScanningWalkthrough) {
                                                    OutlinedButton(
                                                        onClick = { isScanningWalkthrough = true },
                                                        shape = RoundedCornerShape(30.dp),
                                                        modifier = Modifier.testTag("start_walkthrough_scan_button")
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = "Scan")
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("Quick Simulation Scan", fontWeight = FontWeight.Medium)
                                                        }
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = { isScanningWalkthrough = false },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                        shape = RoundedCornerShape(30.dp),
                                                        modifier = Modifier.testTag("cancel_walkthrough_scan_button")
                                                    ) {
                                                        Text("Cancel Simulation", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Processing overlay with circular progress indicator
            if (isProcessing) {
                ProfessionalProgressOverlay(
                    processStatus = processStatus,
                    mediaType = if (selectedTab == 2) CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES else CapturedMediaType.SKETCH_IMAGE
                )
            }
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Displays the viewfinder or a tech-styled fallback if we are in an emulator without camera access
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        // tech lines overlay decoration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
    }
}

// Dummy bitmap generators to represent reference sketches
fun createSimpleSketch(bedrooms: Int): Bitmap {
    val size = 200
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()
    
    // Background
    paint.color = android.graphics.Color.WHITE
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    
    // Border
    paint.color = android.graphics.Color.BLUE
    paint.strokeWidth = 4f
    paint.style = android.graphics.Paint.Style.STROKE
    canvas.drawRect(10f, 10f, size.toFloat() - 10f, size.toFloat() - 10f, paint)

    // Partition lines
    canvas.drawLine(size * 0.5f, 10f, size * 0.5f, size - 10f, paint)
    canvas.drawLine(10f, size * 0.6f, size * 0.5f, size * 0.6f, paint)

    // text label simple
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    paint.textSize = 12f
    canvas.drawText("${bedrooms} Bed sketch", 20f, 40f, paint)
    canvas.drawText("Living", 20f, 100f, paint)
    canvas.drawText("Kitchen", 20f, 160f, paint)
    canvas.drawText("Bed", 110f, 100f, paint)

    return bitmap
}

data class SampleSketchInfo(
    val title: String,
    val key: String,
    val bitmap: Bitmap
)

@Composable
fun ProfessionalProgressOverlay(
    processStatus: String,
    mediaType: CapturedMediaType = CapturedMediaType.SKETCH_IMAGE,
    modifier: Modifier = Modifier
) {
    val isStage2Active = remember(processStatus) {
        processStatus.contains("Analyzing design", ignoreCase = true) || 
        processStatus.contains("Calculating", ignoreCase = true) ||
        processStatus.contains("Constructing", ignoreCase = true) ||
        processStatus.contains("Synthesizing", ignoreCase = true)
    }

    val isStage3Active = remember(processStatus) {
        processStatus.contains("Calculating", ignoreCase = true) ||
        processStatus.contains("Constructing", ignoreCase = true)
    }

    // Smoothly animated progress targets
    var overallProgressTarget by remember { mutableFloatStateOf(0.15f) }

    LaunchedEffect(isStage2Active, isStage3Active) {
        if (isStage3Active) {
            var current = 0.70f
            while (current < 0.95f) {
                kotlinx.coroutines.delay(100)
                current += 0.03f
                overallProgressTarget = current.coerceAtMost(0.96f)
            }
        } else if (isStage2Active) {
            var current = 0.35f
            while (current < 0.70f) {
                kotlinx.coroutines.delay(100)
                current += 0.04f
                overallProgressTarget = current.coerceAtMost(0.70f)
            }
        } else {
            var current = 0.10f
            while (current < 0.35f) {
                kotlinx.coroutines.delay(80)
                current += 0.03f
                overallProgressTarget = current.coerceAtMost(0.35f)
            }
        }
    }

    val overallProgress by animateFloatAsState(
        targetValue = overallProgressTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "overallProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A).copy(alpha = 0.88f)) // Premium deep navy background
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
            border = BorderStroke(1.5.dp, Color(0xFF00B4D8)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(8.dp)
                .testTag("progress_card")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Surface(
                    color = Color(0xFF0D1B2A),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AccentGreen, CircleShape)
                        )
                        Text(
                            text = "GEMINI 2.5 VISION ENGINE",
                            color = Color(0xFF00B4D8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Synthesizing Architectural Floor Plan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Extracting room geometries & estimating Kenyan NCA costings",
                    fontSize = 11.sp,
                    color = Color(0xFF78909C),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Prominent Circular Progress Indicator with radar sweep & percentage
                GeminiCircularProgressIndicator(
                    progress = overallProgress,
                    statusText = processStatus,
                    mediaType = mediaType,
                    size = 140.dp,
                    strokeWidth = 6.dp,
                    primaryColor = Color(0xFF00E5FF),
                    secondaryColor = Color(0xFF0077B6)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stage Progression Checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1B2A), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mediaType == CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES) "Stage 1: Video Frames & Telemetry Scan" else "Stage 1: Sketch & Wall Detection",
                            fontSize = 11.sp,
                            color = if (isStage2Active) Color.White else Color(0xFF00B4D8),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isStage2Active) "Completed" else "Processing...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStage2Active) Color(0xFF4CAF50) else Color(0xFF00B4D8),
                            modifier = Modifier.testTag("stage_1_status")
                        )
                    }

                    HorizontalDivider(color = Color(0xFF415A77).copy(alpha = 0.3f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stage 2: Spatial Alignment & Costings",
                            fontSize = 11.sp,
                            color = if (isStage2Active) Color.White else Color(0xFF78909C),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (!isStage2Active) "Waiting..." else if (isStage3Active) "Completed" else "Synthesizing...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStage3Active) Color(0xFF4CAF50) else if (isStage2Active) Color(0xFF00B4D8) else Color(0xFF78909C),
                            modifier = Modifier.testTag("stage_2_status")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time engine status message with mini circular spinner
                Surface(
                    color = Color(0xFF0D1B2A),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00B4D8),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = processStatus.ifBlank { "Processing architectural vectors..." },
                            color = Color(0xFF90E0EF),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            modifier = Modifier.testTag("process_status_text")
                        )
                    }
                }
            }
        }
    }
}
