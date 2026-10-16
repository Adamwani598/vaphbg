package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RoomItem
import com.example.ui.audio.AudioNarrationPlayerBar
import com.example.ui.audio.rememberAudioNarrator
import com.example.ui.theme.AccentGreen
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

data class Point3D(val x: Float, val y: Float, val z: Float)

data class Polygon3D(
    val points: List<Point3D>,
    val baseColor: Color,
    val label: String? = null,
    val isWall: Boolean = false,
    val isWindowOrDoor: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun ThreeDModelingScreen(
    roomsJson: String,
    title: String = "3D Architectural Model",
    totalAreaSqm: Float = 100f,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Parse rooms from JSON
    val rooms = remember(roomsJson) {
        val list = mutableListOf<RoomItem>()
        try {
            val arr = JSONArray(roomsJson)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    RoomItem(
                        name = o.getString("name"),
                        width = o.getDouble("width").toFloat(),
                        height = o.getDouble("height").toFloat(),
                        x = o.getDouble("x").toFloat(),
                        y = o.getDouble("y").toFloat(),
                        color = o.optString("color", "#2196F3")
                    )
                )
            }
        } catch (e: Exception) {
            list.add(RoomItem("Living & Dining", 6f, 5f, 0f, 0f, "#2196F3"))
            list.add(RoomItem("Kitchen", 4f, 4f, 0f, 5f, "#4CAF50"))
            list.add(RoomItem("Master Bedroom", 5f, 5f, 6f, 0f, "#9C27B0"))
            list.add(RoomItem("Bathroom", 3f, 4f, 6f, 5f, "#FF9800"))
        }
        list
    }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
            maximumFractionDigits = 0
        }
    }

    // Text-To-Speech Audio Walkthrough Narrator
    val audioNarrator = rememberAudioNarrator()

    // Calculated bounds
    val maxX = remember(rooms) { rooms.maxOfOrNull { it.x + it.width } ?: 10f }
    val maxZ = remember(rooms) { rooms.maxOfOrNull { it.y + it.height } ?: 10f }

    // Interactive 3D Camera Controls
    var yawDegrees by remember { mutableFloatStateOf(35f) }
    var pitchDegrees by remember { mutableFloatStateOf(28f) }
    var cameraZoom by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Multi-Storey & Cutaway Controls (supports 1 to 10 storeys)
    val detectedInitialStoreys = remember(title) {
        val regex = Regex("""(\d+)[ -]Storey""", RegexOption.IGNORE_CASE)
        val match = regex.find(title)
        val parsed = match?.groupValues?.get(1)?.toIntOrNull()
        when {
            parsed != null && parsed in 1..10 -> parsed
            title.contains("10-Storey", ignoreCase = true) -> 10
            title.contains("Maisonette", ignoreCase = true) -> 2
            else -> 1
        }
    }
    var buildingStoreys by remember { mutableIntStateOf(detectedInitialStoreys) }
    var cutawayWallHeightRatio by remember { mutableFloatStateOf(1.0f) } // 0.2 (low cross-section) to 1.0 (full wall)
    var wallHeightMeters by remember { mutableFloatStateOf(2.9f) }

    // Finishes & Materials
    var wallFinish by remember { mutableStateOf("Nairobi Stone") } // Nairobi Stone, Smooth Plaster, Brick, Coral Stone, Timber
    var floorFinish by remember { mutableStateOf("Porcelain Tile") } // Porcelain Tile, Parquet Hardwood, Terrazzo
    var roofStyle by remember { mutableStateOf("Hip Roof") } // Hip Roof, Gable Roof, Flat Slab, Modern Butterfly
    var roofColorHex by remember { mutableStateOf("#1A365D") } // Cobalt Blue, Charcoal, Clay Red, Forest Green

    // Lighting & Atmosphere
    var sunTimeHour by remember { mutableFloatStateOf(12f) } // 7am to 19pm
    var isNightAtmosphere by remember { mutableStateOf(false) }

    // Visibility Layers
    var showRoof by remember { mutableStateOf(true) }
    var showExteriorWalls by remember { mutableStateOf(true) }
    var showInteriorPartitions by remember { mutableStateOf(true) }
    var show3DFurniture by remember { mutableStateOf(true) }
    var showWindowsAndDoors by remember { mutableStateOf(true) }
    var showTreesAndLandscape by remember { mutableStateOf(true) }
    var showDimensions3D by remember { mutableStateOf(true) }
    var showGrassTerrain by remember { mutableStateOf(true) }
    var autoRotate by remember { mutableStateOf(false) }

    // Room Selection for Highlighting
    var selectedRoomName by remember { mutableStateOf<String?>(null) }
    var activeWalkthroughRoomIndex by remember { mutableStateOf<Int?>(null) }

    val textMeasurer = rememberTextMeasurer()

    // Auto-orbit animation loop
    LaunchedEffect(autoRotate) {
        if (autoRotate) {
            while (autoRotate) {
                yawDegrees = (yawDegrees + 0.8f) % 360f
                kotlinx.coroutines.delay(16)
            }
        }
    }

    val totalVolumeM3 = remember(rooms, buildingStoreys, wallHeightMeters) {
        (totalAreaSqm * wallHeightMeters * buildingStoreys).roundToInt()
    }

    val fullTourScript = remember(rooms, title, totalAreaSqm, wallHeightMeters, roofStyle, buildingStoreys) {
        val roomDesc = rooms.joinToString(". ") { r ->
            "${r.name}, measuring ${r.width} meters wide by ${r.height} meters deep with ${(r.width * r.height).roundToInt()} square meters area"
        }
        "Welcome to the 3D Architectural Audio Tour for $title. This $buildingStoreys storey structure encompasses $totalAreaSqm square meters gross floor space. The room distribution comprises: $roomDesc. Framed with $wallHeightMeters meter ceiling clearance, $wallFinish masonry, and a $roofStyle structural cover."
    }

    val isDarkMode = MaterialTheme.colorScheme.background.red < 0.3f || isNightAtmosphere

    val mainContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isNightAtmosphere) Color(0xFF070B14) else if (isDarkMode) Color(0xFF0D1B2A) else Color(0xFFF8FAFC))
        ) {
            // VIEWPORT: Interactive 3D Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.35f)
                    .background(if (isNightAtmosphere) Color(0xFF090D1A) else if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            yawDegrees = (yawDegrees + dragAmount.x * 0.4f) % 360f
                            pitchDegrees = (pitchDegrees - dragAmount.y * 0.3f).coerceIn(5f, 85f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            cameraZoom = (cameraZoom * zoom).coerceIn(0.4f, 3.5f)
                            panOffsetX += pan.x
                            panOffsetY += pan.y
                        }
                    }
            ) {
                // 3D Rendering Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("3d_canvas_viewport")
                ) {
                    draw3DScene(
                        rooms = rooms,
                        maxX = maxX,
                        maxZ = maxZ,
                        yawDeg = yawDegrees,
                        pitchDeg = pitchDegrees,
                        zoom = cameraZoom,
                        panX = panOffsetX,
                        panY = panOffsetY,
                        wallHeight = wallHeightMeters,
                        cutawayRatio = cutawayWallHeightRatio,
                        buildingStoreys = buildingStoreys,
                        roofStyle = roofStyle,
                        wallFinish = wallFinish,
                        floorFinish = floorFinish,
                        roofColor = parseHexColor(roofColorHex),
                        sunHour = sunTimeHour,
                        isNight = isNightAtmosphere,
                        showRoof = showRoof,
                        showExteriorWalls = showExteriorWalls,
                        showInteriorWalls = showInteriorPartitions,
                        showFurniture = show3DFurniture,
                        showWindowsDoors = showWindowsAndDoors,
                        showLandscape = showTreesAndLandscape,
                        showDimensions = showDimensions3D,
                        showTerrain = showGrassTerrain,
                        selectedRoom = selectedRoomName,
                        textMeasurer = textMeasurer,
                        isDarkMode = isDarkMode
                    )
                }

                // Top Overlay: Camera Presets & Fast Actions Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(16.dp),
                    color = (if (isDarkMode) Color(0xFF1E293B) else Color.White).copy(alpha = 0.90f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Camera Angle Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ViewPresetChip("Isometric", yaw = 35f, pitch = 28f) { y, p ->
                                yawDegrees = y
                                pitchDegrees = p
                                activeWalkthroughRoomIndex = null
                            }
                            ViewPresetChip("Facade", yaw = 0f, pitch = 12f) { y, p ->
                                yawDegrees = y
                                pitchDegrees = p
                                activeWalkthroughRoomIndex = null
                            }
                            ViewPresetChip("Top Plan", yaw = 0f, pitch = 85f) { y, p ->
                                yawDegrees = y
                                pitchDegrees = p
                                activeWalkthroughRoomIndex = null
                            }
                            ViewPresetChip("Rear", yaw = 180f, pitch = 20f) { y, p ->
                                yawDegrees = y
                                pitchDegrees = p
                                activeWalkthroughRoomIndex = null
                            }
                        }

                        // Right Action Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Night Atmosphere toggle
                            IconButton(
                                onClick = { isNightAtmosphere = !isNightAtmosphere },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("3d_night_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isNightAtmosphere) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                                    contentDescription = "Atmosphere",
                                    tint = if (isNightAtmosphere) Color(0xFFFFD166) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Walkthrough Next Room
                            IconButton(
                                onClick = {
                                    if (rooms.isNotEmpty()) {
                                        val nextIdx = ((activeWalkthroughRoomIndex ?: -1) + 1) % rooms.size
                                        activeWalkthroughRoomIndex = nextIdx
                                        val r = rooms[nextIdx]
                                        selectedRoomName = r.name
                                        pitchDegrees = 22f
                                        yawDegrees = (nextIdx * 55f) % 360f
                                        cameraZoom = 1.7f
                                        audioNarrator.speak("Entering ${r.name}. Dimensions are ${r.width} meters by ${r.height} meters, with ${(r.width * r.height).roundToInt()} square meters area.")
                                    }
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("3d_next_room_walkthrough_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = "Walkthrough Next Room",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // 360 Auto-Rotate
                            IconButton(
                                onClick = { autoRotate = !autoRotate },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("3d_auto_rotate_button")
                            ) {
                                Icon(
                                    imageVector = if (autoRotate) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Auto Rotate 360°",
                                    tint = if (autoRotate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // Reset View
                            IconButton(
                                onClick = {
                                    yawDegrees = 35f
                                    pitchDegrees = 28f
                                    cameraZoom = 1.0f
                                    panOffsetX = 0f
                                    panOffsetY = 0f
                                    selectedRoomName = null
                                    activeWalkthroughRoomIndex = null
                                    autoRotate = false
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("3d_reset_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CenterFocusStrong,
                                    contentDescription = "Reset Camera",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Selected Room Floating Pill Overlay
                selectedRoomName?.let { roomName ->
                    val matchedRoom = rooms.find { it.name == roomName }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = roomName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            matchedRoom?.let { r ->
                                val roomArea = r.width * r.height
                                Text(
                                    text = "• ${r.width}m × ${r.height}m (${roomArea.roundToInt()} m²)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            IconButton(
                                onClick = { selectedRoomName = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Deselect",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Compact Audio Briefing Voiceover Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (audioNarrator.isSpeaking) Icons.Default.GraphicEq else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "3D Architectural Audio Tour",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (audioNarrator.isSpeaking) "Narrating layout & spatial dimensions..." else "Tap to hear spoken architectural walkthrough",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (audioNarrator.isSpeaking) {
                                audioNarrator.stop()
                            } else {
                                audioNarrator.speak(fullTourScript)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("3d_audio_tour_play_toggle")
                    ) {
                        Icon(
                            imageVector = if (audioNarrator.isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (audioNarrator.isSpeaking) "Stop Narration" else "Play Audio Tour",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ARCHITECTURAL CONTROLS DRAWER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                var selectedControlTab by remember { mutableIntStateOf(0) }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Control Tabs Header
                    TabRow(
                        selectedTabIndex = selectedControlTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedControlTab == 0,
                            onClick = { selectedControlTab = 0 },
                            text = { Text("Structure", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.HomeWork, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_3d_structure")
                        )
                        Tab(
                            selected = selectedControlTab == 1,
                            onClick = { selectedControlTab = 1 },
                            text = { Text("Finishes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_3d_finishes")
                        )
                        Tab(
                            selected = selectedControlTab == 2,
                            onClick = { selectedControlTab = 2 },
                            text = { Text("Cutaway & Levels", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_3d_cutaway_levels")
                        )
                        Tab(
                            selected = selectedControlTab == 3,
                            onClick = { selectedControlTab = 3 },
                            text = { Text("Layers", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_3d_layers")
                        )
                        Tab(
                            selected = selectedControlTab == 4,
                            onClick = { selectedControlTab = 4 },
                            text = { Text("Lighting", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_3d_sun_light")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (selectedControlTab) {
                            0 -> {
                                // 3D Structure Controls
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Wall Extrusion Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Wall Height (Clearance):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${(wallHeightMeters * 10).roundToInt() / 10f} m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = wallHeightMeters,
                                        onValueChange = { wallHeightMeters = it },
                                        valueRange = 2.4f..4.2f,
                                        steps = 8,
                                        modifier = Modifier.testTag("3d_wall_height_slider")
                                    )

                                    HorizontalDivider()

                                    // Roof Style Selection
                                    Text("Roof Architectural Style:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("Hip Roof", "Gable Roof", "Flat Slab", "Modern Butterfly")) { style ->
                                            FilterChip(
                                                selected = roofStyle == style,
                                                onClick = { roofStyle = style },
                                                label = { Text(style, fontSize = 11.sp) },
                                                leadingIcon = if (roofStyle == style) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                                } else null
                                            )
                                        }
                                    }

                                    // Interactive Room Quick Focus Chips
                                    Text("Focus Room in 3D Viewport:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(rooms) { room ->
                                            FilterChip(
                                                selected = selectedRoomName == room.name,
                                                onClick = {
                                                    selectedRoomName = if (selectedRoomName == room.name) null else room.name
                                                    if (selectedRoomName != null) {
                                                        cameraZoom = 1.4f
                                                    }
                                                },
                                                label = { Text(room.name, fontSize = 10.sp) },
                                                leadingIcon = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(parseHexColor(room.color))
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                // Finishes & Materials
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Exterior Wall Masonry & Cladding:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("Nairobi Stone", "Smooth Plaster", "Brick", "Coral Stone", "Timber")) { finish ->
                                            FilterChip(
                                                selected = wallFinish == finish,
                                                onClick = { wallFinish = finish },
                                                label = { Text(finish, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    HorizontalDivider()

                                    Text("Interior Floor Finish:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("Porcelain Tile", "Parquet Hardwood", "Terrazzo")) { floor ->
                                            FilterChip(
                                                selected = floorFinish == floor,
                                                onClick = { floorFinish = floor },
                                                label = { Text(floor, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    HorizontalDivider()

                                    Text("Roof Cover Color:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    val roofColors = listOf(
                                        "#1A365D" to "Cobalt Blue",
                                        "#2D3748" to "Charcoal Grey",
                                        "#9B2C2C" to "Clay Red",
                                        "#2F855A" to "Forest Green",
                                        "#718096" to "Galvanized Silver"
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        roofColors.forEach { (hex, label) ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(parseHexColor(hex))
                                                        .border(
                                                            width = if (roofColorHex == hex) 3.dp else 1.dp,
                                                            color = if (roofColorHex == hex) MaterialTheme.colorScheme.primary else Color.Gray,
                                                            shape = CircleShape
                                                        )
                                                        .clickable { roofColorHex = hex },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (roofColorHex == hex) {
                                                        Icon(Icons.Default.Check, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Text(label.split(" ")[0], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // Cutaway X-Ray & Multi-Storey Controls
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Multi-Storey Switcher (1 to 10 Storeys)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Building Storeys (1 to 10 Storeys):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$buildingStoreys ${if (buildingStoreys == 1) "Storey" else "Storeys"}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            1 to "1F",
                                            2 to "2F",
                                            4 to "4F",
                                            6 to "6F",
                                            8 to "8F",
                                            10 to "10F"
                                        ).forEach { (storey, label) ->
                                            val isSel = buildingStoreys == storey
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { buildingStoreys = storey },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }

                                    Slider(
                                        value = buildingStoreys.toFloat(),
                                        onValueChange = { buildingStoreys = it.roundToInt().coerceIn(1, 10) },
                                        valueRange = 1f..10f,
                                        steps = 8
                                    )

                                    HorizontalDivider()

                                    // Live Horizontal Cutaway Slider
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Text("Interior Cutaway Slice Height:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            text = if (cutawayWallHeightRatio >= 0.99f) "Full Height (100%)" else "${(cutawayWallHeightRatio * 100).roundToInt()}% Cutaway",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Slider(
                                        value = cutawayWallHeightRatio,
                                        onValueChange = {
                                            cutawayWallHeightRatio = it
                                            if (it < 0.8f) showRoof = false // auto-hide roof in cutaway mode
                                        },
                                        valueRange = 0.15f..1.0f,
                                        steps = 10,
                                        modifier = Modifier.testTag("3d_cutaway_slider")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(onClick = { cutawayWallHeightRatio = 0.3f; showRoof = false }) {
                                            Text("Plan Cross-Section", fontSize = 11.sp)
                                        }
                                        TextButton(onClick = { cutawayWallHeightRatio = 1.0f; showRoof = true }) {
                                            Text("Restore Full 3D", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // Layer Visibility Toggles (Clean 2-Column Grid)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("Roof Truss", Icons.Default.Roofing, showRoof) { showRoof = it }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("Exterior Walls", Icons.Default.Domain, showExteriorWalls) { showExteriorWalls = it }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("Partitions", Icons.Default.MeetingRoom, showInteriorPartitions) { showInteriorPartitions = it }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("3D Furniture", Icons.Default.Chair, show3DFurniture) { show3DFurniture = it }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("Doors & Windows", Icons.Default.Window, showWindowsAndDoors) { showWindowsAndDoors = it }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("Trees & Yard", Icons.Default.Park, showTreesAndLandscape) { showTreesAndLandscape = it }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("Dimension Grid", Icons.Default.GridOn, showDimensions3D) { showDimensions3D = it }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerToggleCard("Grass Terrain", Icons.Default.Landscape, showGrassTerrain) { showGrassTerrain = it }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                // Sun & Solar Lighting Controls
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Solar Shading Time:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = when {
                                                sunTimeHour < 10f -> "${sunTimeHour.roundToInt()}:00 AM (Morning)"
                                                sunTimeHour < 14f -> "${sunTimeHour.roundToInt()}:00 PM (Midday Sun)"
                                                sunTimeHour < 17f -> "${sunTimeHour.roundToInt()}:00 PM (Afternoon)"
                                                else -> "${sunTimeHour.roundToInt()}:00 PM (Golden Hour Sunset)"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Slider(
                                        value = sunTimeHour,
                                        onValueChange = { sunTimeHour = it },
                                        valueRange = 7f..18f,
                                        steps = 11,
                                        modifier = Modifier.testTag("3d_sun_time_slider")
                                    )

                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "Simulates real-time solar path & shadow projections across east/west window openings.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
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

    if (onBack != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "3D Studio • Area: ${totalAreaSqm.roundToInt()} m² • Vol: $totalVolumeM3 m³ • ${if (buildingStoreys > 1) "Maisonette" else "Bungalow"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("3d_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                mainContent()
            }
        }
    } else {
        mainContent()
    }
}

@Composable
private fun LayerToggleCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (checked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.7f)
            )
        }
    }
}

@Composable
private fun ViewPresetChip(
    label: String,
    yaw: Float,
    pitch: Float,
    onSelect: (Float, Float) -> Unit
) {
    Surface(
        onClick = { onSelect(yaw, pitch) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 2.dp
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

// --- 3D ENGINE MATHEMATICS & DRAWING LOGIC ---

private fun DrawScope.draw3DScene(
    rooms: List<RoomItem>,
    maxX: Float,
    maxZ: Float,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    wallHeight: Float,
    cutawayRatio: Float,
    buildingStoreys: Int,
    roofStyle: String,
    wallFinish: String,
    floorFinish: String,
    roofColor: Color,
    sunHour: Float,
    isNight: Boolean,
    showRoof: Boolean,
    showExteriorWalls: Boolean,
    showInteriorWalls: Boolean,
    showFurniture: Boolean,
    showWindowsDoors: Boolean,
    showLandscape: Boolean,
    showDimensions: Boolean,
    showTerrain: Boolean,
    selectedRoom: String?,
    textMeasurer: TextMeasurer,
    isDarkMode: Boolean
) {
    val centerX = size.width / 2f + panX
    val centerY = size.height / 2f + panY + 30f

    val effectiveWallHeight = wallHeight * cutawayRatio

    val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
    val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

    val scaleFactor = min(size.width / (maxX * 1.85f), size.height / (maxZ * 1.85f)) * zoom * 1.15f

    val originOffsetX = maxX / 2f
    val originOffsetZ = maxZ / 2f

    // 3D Projection Pipeline Function
    fun project(p: Point3D): Offset {
        val relX = p.x - originOffsetX
        val relY = p.y
        val relZ = p.z - originOffsetZ

        // Yaw rotation around Y axis
        val x1 = relX * cos(yawRad) + relZ * sin(yawRad)
        val z1 = -relX * sin(yawRad) + relZ * cos(yawRad)

        // Pitch rotation around X axis
        val y2 = relY * cos(pitchRad) - z1 * sin(pitchRad)
        val z2 = relY * sin(pitchRad) + z1 * cos(pitchRad)

        val screenX = centerX + x1 * scaleFactor
        val screenY = centerY - y2 * scaleFactor

        return Offset(screenX, screenY)
    }

    // 1. Draw 3D Ground Terrain & Paved Driveway
    if (showTerrain) {
        val padMargin = 3.5f
        val groundPoints = listOf(
            Point3D(-padMargin, 0f, -padMargin),
            Point3D(maxX + padMargin, 0f, -padMargin),
            Point3D(maxX + padMargin, 0f, maxZ + padMargin),
            Point3D(-padMargin, 0f, maxZ + padMargin)
        ).map { project(it) }

        val groundPath = Path().apply {
            moveTo(groundPoints[0].x, groundPoints[0].y)
            for (i in 1 until groundPoints.size) {
                lineTo(groundPoints[i].x, groundPoints[i].y)
            }
            close()
        }

        drawPath(
            path = groundPath,
            color = if (isNight) Color(0xFF0F1A15) else if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        )
        drawPath(
            path = groundPath,
            color = if (isNight) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFF4CAF50).copy(alpha = 0.35f),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        )

        // Driveway / Walkway paving on front
        val drivewayPts = listOf(
            Point3D(0f, 0.02f, maxZ),
            Point3D(3.5f, 0.02f, maxZ),
            Point3D(3.5f, 0.02f, maxZ + padMargin),
            Point3D(0f, 0.02f, maxZ + padMargin)
        ).map { project(it) }

        val drivewayPath = Path().apply {
            moveTo(drivewayPts[0].x, drivewayPts[0].y)
            for (i in 1 until drivewayPts.size) {
                lineTo(drivewayPts[i].x, drivewayPts[i].y)
            }
            close()
        }
        drawPath(path = drivewayPath, color = Color(0xFF94A3B8).copy(alpha = 0.5f))
    }

    // 2. Solar Shadow Projection on Ground
    if (!isNight) {
        val sunAngleRad = Math.toRadians(((sunHour - 7f) / 11f * 130f).toDouble()).toFloat()
        val shadowLength = (effectiveWallHeight * buildingStoreys) * 0.75f
        val shadowDX = cos(sunAngleRad) * shadowLength
        val shadowDZ = sin(sunAngleRad) * shadowLength

        val shadowPoints = listOf(
            Point3D(0f + shadowDX, 0f, 0f + shadowDZ),
            Point3D(maxX + shadowDX, 0f, 0f + shadowDZ),
            Point3D(maxX + shadowDX, 0f, maxZ + shadowDZ),
            Point3D(0f + shadowDX, 0f, maxZ + shadowDZ)
        ).map { project(it) }

        val shadowPath = Path().apply {
            moveTo(shadowPoints[0].x, shadowPoints[0].y)
            for (i in 1 until shadowPoints.size) {
                lineTo(shadowPoints[i].x, shadowPoints[i].y)
            }
            close()
        }
        drawPath(path = shadowPath, color = Color.Black.copy(alpha = 0.14f))
    }

    // Collect all 3D Polygons for depth-sorting
    val polygons = mutableListOf<Polygon3D>()

    // Wall finish base colors
    val wallBaseColor = when (wallFinish) {
        "Smooth Plaster" -> Color(0xFFF1F5F9)
        "Brick" -> Color(0xFFB91C1C)
        "Coral Stone" -> Color(0xFFE2D9CC)
        "Timber" -> Color(0xFF854D0E)
        else -> Color(0xFF64748B) // Nairobi Stone
    }

    // Floor finish base colors
    val floorBaseColor = when (floorFinish) {
        "Parquet Hardwood" -> Color(0xFF9A3412)
        "Terrazzo" -> Color(0xFF475569)
        else -> Color(0xFFCBD5E1) // Porcelain Tile
    }

    // Render for each Storey
    for (floorLevel in 0 until buildingStoreys) {
        val baseElevY = floorLevel * (wallHeight + 0.25f)
        val currentWallH = effectiveWallHeight

        // Slabs and rooms for this floor
        rooms.forEach { room ->
            val isSelected = selectedRoom == room.name
            val roomColor = parseHexColor(room.color)

            val x0 = room.x
            val x1 = room.x + room.width
            val z0 = room.y
            val z1 = room.y + room.height

            // Floor Slab Polygon
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(x0, baseElevY + 0.05f, z0),
                        Point3D(x1, baseElevY + 0.05f, z0),
                        Point3D(x1, baseElevY + 0.05f, z1),
                        Point3D(x0, baseElevY + 0.05f, z1)
                    ),
                    baseColor = if (isSelected) Color(0xFF00B4D8) else roomColor.copy(alpha = 0.65f),
                    label = if (floorLevel == 0) room.name else "Upper ${room.name}"
                )
            )

            // Walls (Exterior & Partitions)
            if (showExteriorWalls || showInteriorWalls) {
                // Front Wall (Z = z1)
                polygons.add(
                    Polygon3D(
                        points = listOf(
                            Point3D(x0, baseElevY, z1),
                            Point3D(x1, baseElevY, z1),
                            Point3D(x1, baseElevY + currentWallH, z1),
                            Point3D(x0, baseElevY + currentWallH, z1)
                        ),
                        baseColor = wallBaseColor.copy(alpha = 0.88f),
                        isWall = true
                    )
                )

                // Right Wall (X = x1)
                polygons.add(
                    Polygon3D(
                        points = listOf(
                            Point3D(x1, baseElevY, z0),
                            Point3D(x1, baseElevY, z1),
                            Point3D(x1, baseElevY + currentWallH, z1),
                            Point3D(x1, baseElevY + currentWallH, z0)
                        ),
                        baseColor = wallBaseColor.copy(alpha = 0.72f),
                        isWall = true
                    )
                )

                // Back Wall (Z = z0)
                polygons.add(
                    Polygon3D(
                        points = listOf(
                            Point3D(x0, baseElevY, z0),
                            Point3D(x1, baseElevY, z0),
                            Point3D(x1, baseElevY + currentWallH, z0),
                            Point3D(x0, baseElevY + currentWallH, z0)
                        ),
                        baseColor = wallBaseColor.copy(alpha = 0.62f),
                        isWall = true
                    )
                )

                // Left Wall (X = x0)
                polygons.add(
                    Polygon3D(
                        points = listOf(
                            Point3D(x0, baseElevY, z0),
                            Point3D(x0, baseElevY, z1),
                            Point3D(x0, baseElevY + currentWallH, z1),
                            Point3D(x0, baseElevY + currentWallH, z0)
                        ),
                        baseColor = wallBaseColor.copy(alpha = 0.78f),
                        isWall = true
                    )
                )
            }

            // Window Openings Cutouts / Glass Panes
            if (showWindowsDoors && currentWallH > 1.2f) {
                val winW = min(room.width * 0.45f, 1.8f)
                val winMidX = x0 + room.width / 2f
                val winBottomY = baseElevY + 0.9f
                val winTopY = min(baseElevY + currentWallH - 0.2f, baseElevY + 2.1f)

                if (winTopY > winBottomY) {
                    // Front Window Glass Pane
                    polygons.add(
                        Polygon3D(
                            points = listOf(
                                Point3D(winMidX - winW / 2, winBottomY, z1 + 0.02f),
                                Point3D(winMidX + winW / 2, winBottomY, z1 + 0.02f),
                                Point3D(winMidX + winW / 2, winTopY, z1 + 0.02f),
                                Point3D(winMidX - winW / 2, winTopY, z1 + 0.02f)
                            ),
                            baseColor = if (isNight) Color(0xFFFFE082).copy(alpha = 0.85f) else Color(0xFF67E8F9).copy(alpha = 0.75f),
                            isWindowOrDoor = true
                        )
                    )
                }
            }

            // 3D Furniture Items
            if (showFurniture && currentWallH > 0.4f) {
                val cx = x0 + room.width / 2f
                val cz = z0 + room.height / 2f
                val lowerName = room.name.lowercase()

                if (lowerName.contains("bed")) {
                    // Bed Furniture Box with Pillow
                    val bedW = min(room.width * 0.5f, 2.0f)
                    val bedL = min(room.height * 0.6f, 2.2f)
                    polygons.add(
                        Polygon3D(
                            points = listOf(
                                Point3D(cx - bedW / 2, baseElevY + 0.5f, cz - bedL / 2),
                                Point3D(cx + bedW / 2, baseElevY + 0.5f, cz - bedL / 2),
                                Point3D(cx + bedW / 2, baseElevY + 0.5f, cz + bedL / 2),
                                Point3D(cx - bedW / 2, baseElevY + 0.5f, cz + bedL / 2)
                            ),
                            baseColor = Color(0xFFE2E8F0)
                        )
                    )
                } else if (lowerName.contains("living") || lowerName.contains("lounge")) {
                    // Sofa Sectional in Lounge
                    val sofaW = min(room.width * 0.6f, 2.8f)
                    val sofaD = 1.0f
                    polygons.add(
                        Polygon3D(
                            points = listOf(
                                Point3D(cx - sofaW / 2, baseElevY + 0.45f, cz - sofaD / 2),
                                Point3D(cx + sofaW / 2, baseElevY + 0.45f, cz - sofaD / 2),
                                Point3D(cx + sofaW / 2, baseElevY + 0.45f, cz + sofaD / 2),
                                Point3D(cx - sofaW / 2, baseElevY + 0.45f, cz + sofaD / 2)
                            ),
                            baseColor = Color(0xFF3B82F6)
                        )
                    )
                } else if (lowerName.contains("dining")) {
                    // Dining Table
                    val tblW = 1.8f
                    val tblD = 1.2f
                    polygons.add(
                        Polygon3D(
                            points = listOf(
                                Point3D(cx - tblW / 2, baseElevY + 0.6f, cz - tblD / 2),
                                Point3D(cx + tblW / 2, baseElevY + 0.6f, cz - tblD / 2),
                                Point3D(cx + tblW / 2, baseElevY + 0.6f, cz + tblD / 2),
                                Point3D(cx - tblW / 2, baseElevY + 0.6f, cz + tblD / 2)
                            ),
                            baseColor = Color(0xFFD97706)
                        )
                    )
                } else if (lowerName.contains("kitchen")) {
                    // Kitchen Island Counter
                    val cntW = min(room.width * 0.7f, 2.4f)
                    val cntD = 0.8f
                    polygons.add(
                        Polygon3D(
                            points = listOf(
                                Point3D(cx - cntW / 2, baseElevY + 0.85f, cz - cntD / 2),
                                Point3D(cx + cntW / 2, baseElevY + 0.85f, cz - cntD / 2),
                                Point3D(cx + cntW / 2, baseElevY + 0.85f, cz + cntD / 2),
                                Point3D(cx - cntW / 2, baseElevY + 0.85f, cz + cntD / 2)
                            ),
                            baseColor = Color(0xFF10B981)
                        )
                    )
                }
            }
        }
    }

    // 3D Roof Extrusion
    if (showRoof && cutawayRatio >= 0.85f) {
        val totalBuildingHeight = buildingStoreys * (wallHeight + 0.25f)
        val rh = totalBuildingHeight + when (roofStyle) {
            "Gable Roof" -> 2.4f
            "Flat Slab" -> 0.35f
            "Modern Butterfly" -> 1.9f
            else -> 2.2f // Hip Roof
        }

        val rxMid = maxX / 2f
        val rzMid = maxZ / 2f

        if (roofStyle == "Hip Roof" || roofStyle == "Gable Roof") {
            // Front Roof Pitch
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(-0.35f, totalBuildingHeight, maxZ + 0.35f),
                        Point3D(maxX + 0.35f, totalBuildingHeight, maxZ + 0.35f),
                        Point3D(rxMid, rh, rzMid)
                    ),
                    baseColor = roofColor.copy(alpha = 0.95f)
                )
            )
            // Right Roof Pitch
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(maxX + 0.35f, totalBuildingHeight, -0.35f),
                        Point3D(maxX + 0.35f, totalBuildingHeight, maxZ + 0.35f),
                        Point3D(rxMid, rh, rzMid)
                    ),
                    baseColor = roofColor.copy(alpha = 0.82f)
                )
            )
            // Left Roof Pitch
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(-0.35f, totalBuildingHeight, -0.35f),
                        Point3D(-0.35f, totalBuildingHeight, maxZ + 0.35f),
                        Point3D(rxMid, rh, rzMid)
                    ),
                    baseColor = roofColor.copy(alpha = 0.88f)
                )
            )
            // Back Roof Pitch
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(-0.35f, totalBuildingHeight, -0.35f),
                        Point3D(maxX + 0.35f, totalBuildingHeight, -0.35f),
                        Point3D(rxMid, rh, rzMid)
                    ),
                    baseColor = roofColor.copy(alpha = 0.70f)
                )
            )
        } else if (roofStyle == "Flat Slab") {
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(-0.25f, rh, -0.25f),
                        Point3D(maxX + 0.25f, rh, -0.25f),
                        Point3D(maxX + 0.25f, rh, maxZ + 0.25f),
                        Point3D(-0.25f, rh, maxZ + 0.25f)
                    ),
                    baseColor = roofColor
                )
            )
        }
    }

    // 3D Landscape Trees around plot
    if (showLandscape) {
        val treeLocations = listOf(
            Point3D(-2.0f, 0f, 2.0f),
            Point3D(maxX + 2.0f, 0f, 3.0f),
            Point3D(-2.0f, 0f, maxZ - 1.0f)
        )
        treeLocations.forEach { loc ->
            // Tree Trunk
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(loc.x - 0.2f, 0f, loc.z),
                        Point3D(loc.x + 0.2f, 0f, loc.z),
                        Point3D(loc.x + 0.2f, 2.2f, loc.z),
                        Point3D(loc.x - 0.2f, 2.2f, loc.z)
                    ),
                    baseColor = Color(0xFF78350F)
                )
            )
            // Tree Canopy Cone/Pyramid
            polygons.add(
                Polygon3D(
                    points = listOf(
                        Point3D(loc.x - 1.2f, 2.0f, loc.z - 1.2f),
                        Point3D(loc.x + 1.2f, 2.0f, loc.z - 1.2f),
                        Point3D(loc.x + 1.2f, 2.0f, loc.z + 1.2f),
                        Point3D(loc.x - 1.2f, 2.0f, loc.z + 1.2f)
                    ),
                    baseColor = Color(0xFF15803D)
                )
            )
        }
    }

    // Depth Sorting for 3D Polygons (Painter's Algorithm)
    val sortedPolygons = polygons.sortedBy { poly ->
        val avgZ = poly.points.map { it.z }.average().toFloat()
        val avgY = poly.points.map { it.y }.average().toFloat()
        avgZ + avgY * 0.08f
    }

    // Render sorted 3D faces
    sortedPolygons.forEach { poly ->
        val screenPts = poly.points.map { project(it) }
        val path = Path().apply {
            moveTo(screenPts[0].x, screenPts[0].y)
            for (i in 1 until screenPts.size) {
                lineTo(screenPts[i].x, screenPts[i].y)
            }
            close()
        }

        drawPath(path = path, color = poly.baseColor)
        drawPath(
            path = path,
            color = if (poly.isWindowOrDoor) Color(0xFF38BDF8) else Color.Black.copy(alpha = 0.28f),
            style = Stroke(width = if (poly.isWindowOrDoor) 1.8f else 1.2f)
        )

        // Draw Room Labels in 3D
        poly.label?.let { label ->
            val centerPt = screenPts.fold(Offset.Zero) { acc, pt -> acc + pt } / screenPts.size.toFloat()
            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(label),
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(centerPt.x - textLayoutResult.size.width / 2f, centerPt.y - textLayoutResult.size.height / 2f)
            )
        }
    }

    // 3D Dimension Overlays
    if (showDimensions) {
        val dimPt1 = project(Point3D(0f, 0f, maxZ + 1.2f))
        val dimPt2 = project(Point3D(maxX, 0f, maxZ + 1.2f))
        drawLine(color = Color(0xFF00B4D8), start = dimPt1, end = dimPt2, strokeWidth = 3f)

        val dimText = textMeasurer.measure(
            text = AnnotatedString("${maxX.roundToInt()}m Width"),
            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B4D8))
        )
        drawText(dimText, topLeft = Offset((dimPt1.x + dimPt2.x) / 2f, (dimPt1.y + dimPt2.y) / 2f + 4f))
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.replace("#", "")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        Color(0xFF2196F3)
    }
}
