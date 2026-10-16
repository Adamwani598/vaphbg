package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.network.models.BlueprintGenerationSpecs
import com.example.network.models.CapturedMediaItem
import com.example.network.models.CapturedMediaType
import com.example.ui.BlueprintViewModel
import com.example.ui.components.GeminiInlineProgressCard
import com.example.ui.components.GeminiProcessingOverlayModal
import java.text.SimpleDateFormat
import java.util.*

enum class GalleryFilterTab(val label: String) {
    ALL("All Media"),
    SKETCHES("Sketches"),
    PHOTOS("Photos & Multi-Angle"),
    VIDEOS("360° Videos"),
    SELECTED("Selected Only")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    viewModel: BlueprintViewModel,
    onOpenLiveCamera: (CameraCaptureMode) -> Unit,
    onBlueprintGenerated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mediaList by viewModel.capturedMediaList.collectAsState()
    val selectedIds by viewModel.selectedMediaIds.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processStatus by viewModel.processStatus.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()

    var selectedTab by remember { mutableStateOf(GalleryFilterTab.ALL) }
    var inspectingMediaItem by remember { mutableStateOf<CapturedMediaItem?>(null) }
    var showSpecsSheet by remember { mutableStateOf(false) }

    // Analysis parameters
    var analysisNotes by remember { mutableStateOf("") }
    var selectedCounty by remember { mutableStateOf("Nairobi") }
    var targetAreaSqm by remember { mutableFloatStateOf(110f) }
    var preferredBedrooms by remember { mutableIntStateOf(3) }
    var preferredBathrooms by remember { mutableIntStateOf(2) }

    // Filter media items
    val filteredMedia = remember(mediaList, selectedTab, selectedIds) {
        when (selectedTab) {
            GalleryFilterTab.ALL -> mediaList
            GalleryFilterTab.SKETCHES -> mediaList.filter { it.mediaType == CapturedMediaType.SKETCH_IMAGE }
            GalleryFilterTab.PHOTOS -> mediaList.filter {
                it.mediaType == CapturedMediaType.PHYSICAL_PHOTO || it.mediaType == CapturedMediaType.MULTI_ANGLE_PHOTOS
            }
            GalleryFilterTab.VIDEOS -> mediaList.filter { it.mediaType == CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES }
            GalleryFilterTab.SELECTED -> mediaList.filter { it.id in selectedIds }
        }
    }

    val selectedCount = selectedIds.size
    val allSelected = mediaList.isNotEmpty() && selectedIds.size == mediaList.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Captured Media Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$selectedCount of ${mediaList.size} item(s) selected for Gemini AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedCount > 0) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("gallery_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Select All / Deselect Toggle
                    TextButton(
                        onClick = {
                            if (allSelected) {
                                viewModel.clearMediaSelection()
                            } else {
                                viewModel.selectAllMedia()
                            }
                        },
                        modifier = Modifier.testTag("gallery_select_all_button")
                    ) {
                        Text(
                            text = if (allSelected) "Deselect All" else "Select All",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Camera Button
                    IconButton(
                        onClick = { onOpenLiveCamera(CameraCaptureMode.SKETCH) },
                        modifier = Modifier.testTag("gallery_open_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Open CameraX Viewfinder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            GeminiAnalysisDispatcherBottomBar(
                selectedCount = selectedCount,
                isProcessing = isProcessing,
                processStatus = processStatus,
                analysisError = analysisError,
                showSpecsSheet = showSpecsSheet,
                selectedCounty = selectedCounty,
                targetAreaSqm = targetAreaSqm,
                preferredBedrooms = preferredBedrooms,
                preferredBathrooms = preferredBathrooms,
                analysisNotes = analysisNotes,
                onToggleSpecs = { showSpecsSheet = !showSpecsSheet },
                onCountyChange = { selectedCounty = it },
                onAreaChange = { targetAreaSqm = it },
                onBedroomsChange = { preferredBedrooms = it },
                onBathroomsChange = { preferredBathrooms = it },
                onNotesChange = { analysisNotes = it },
                onDispatchAnalysis = {
                    if (selectedCount == 0) {
                        Toast.makeText(context, "Select at least one media item first.", Toast.LENGTH_SHORT).show()
                        return@GeminiAnalysisDispatcherBottomBar
                    }
                    val specs = BlueprintGenerationSpecs(
                        targetAreaSqm = targetAreaSqm,
                        preferredBedrooms = preferredBedrooms,
                        preferredBathrooms = preferredBathrooms,
                        county = selectedCounty,
                        finishesQuality = "Standard",
                        roofType = "Mabati (Iron Sheets)",
                        soilType = "Red Soil",
                        extraInstructions = analysisNotes
                    )
                    viewModel.analyzeSelectedMediaItems(
                        extraNotes = analysisNotes,
                        county = selectedCounty,
                        specs = specs
                    ) {
                        onBlueprintGenerated()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Pills Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GalleryFilterTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val badgeCount = when (tab) {
                        GalleryFilterTab.ALL -> mediaList.size
                        GalleryFilterTab.SKETCHES -> mediaList.count { it.mediaType == CapturedMediaType.SKETCH_IMAGE }
                        GalleryFilterTab.PHOTOS -> mediaList.count {
                            it.mediaType == CapturedMediaType.PHYSICAL_PHOTO || it.mediaType == CapturedMediaType.MULTI_ANGLE_PHOTOS
                        }
                        GalleryFilterTab.VIDEOS -> mediaList.count { it.mediaType == CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES }
                        GalleryFilterTab.SELECTED -> selectedIds.size
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = "${tab.label} ($badgeCount)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Media Grid Content
            if (filteredMedia.isEmpty()) {
                EmptyGalleryState(
                    selectedTab = selectedTab,
                    onOpenLiveCamera = { onOpenLiveCamera(CameraCaptureMode.SKETCH) },
                    onLoadDemoMedia = { viewModel.loadDemoSampleMedia() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("captured_media_grid")
                ) {
                    items(filteredMedia, key = { it.id }) { item ->
                        val isSelected = item.id in selectedIds
                        MediaThumbnailCard(
                            item = item,
                            isSelected = isSelected,
                            onToggleSelect = { viewModel.toggleMediaSelection(item.id) },
                            onInspect = { inspectingMediaItem = item },
                            onDelete = { viewModel.removeCapturedMedia(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Detail Inspection Modal
    inspectingMediaItem?.let { item ->
        val isSelected = item.id in selectedIds
        MediaInspectionDialog(
            item = item,
            isSelected = isSelected,
            onToggleSelect = { viewModel.toggleMediaSelection(item.id) },
            onDelete = {
                viewModel.removeCapturedMedia(item.id)
                inspectingMediaItem = null
            },
            onAnalyzeSingle = {
                inspectingMediaItem = null
                val specs = BlueprintGenerationSpecs(
                    targetAreaSqm = targetAreaSqm,
                    preferredBedrooms = preferredBedrooms,
                    preferredBathrooms = preferredBathrooms,
                    county = selectedCounty,
                    extraInstructions = analysisNotes
                )
                when (item.mediaType) {
                    CapturedMediaType.SKETCH_IMAGE, CapturedMediaType.PHYSICAL_PHOTO -> {
                        item.bitmap?.let { bmp ->
                            viewModel.analyzeCapturedMedia(
                                bitmap = bmp,
                                mediaType = item.mediaType,
                                extraNotes = analysisNotes.ifBlank { item.notes },
                                county = selectedCounty,
                                specs = specs
                            ) {
                                onBlueprintGenerated()
                            }
                        }
                    }
                    CapturedMediaType.MULTI_ANGLE_PHOTOS -> {
                        viewModel.analyzeMultiAnglePhotos(
                            bitmaps = item.allBitmaps,
                            extraNotes = analysisNotes.ifBlank { item.notes },
                            county = selectedCounty,
                            specs = specs
                        ) {
                            onBlueprintGenerated()
                        }
                    }
                    CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES -> {
                        viewModel.analyzeWalkthroughVideoFrames(
                            frames = item.allBitmaps,
                            telemetry = item.telemetry,
                            county = selectedCounty,
                            extraNotes = analysisNotes.ifBlank { item.notes },
                            specs = specs
                        ) {
                            onBlueprintGenerated()
                        }
                    }
                    else -> {}
                }
            },
            onDismiss = { inspectingMediaItem = null }
        )
    }

    // Fullscreen Gemini Circular Progress Overlay Modal
    if (isProcessing) {
        GeminiProcessingOverlayModal(
            processStatus = processStatus,
            mediaType = when (selectedTab) {
                GalleryFilterTab.VIDEOS -> CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES
                GalleryFilterTab.PHOTOS -> CapturedMediaType.MULTI_ANGLE_PHOTOS
                else -> CapturedMediaType.SKETCH_IMAGE
            }
        )
    }
}

@Composable
fun MediaThumbnailCard(
    item: CapturedMediaItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val borderWidth = if (isSelected) 2.5.dp else 1.dp
    val elevation = if (isSelected) 6.dp else 1.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
            .testTag("media_card_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail Image Box with Badges & Checkbox
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(Color(0xFF0A0F1D))
            ) {
                if (item.bitmap != null) {
                    Image(
                        bitmap = item.bitmap.asImageBitmap(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.mediaType) {
                                CapturedMediaType.SKETCH_IMAGE -> Icons.Default.Description
                                CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES -> Icons.Default.Videocam
                                else -> Icons.Default.PhotoLibrary
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Selection Checkbox Badge (Top Right)
                Surface(
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0x99000000),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, if (isSelected) Color.White else Color(0x66FFFFFF)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .testTag("checkbox_${item.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Media Type Pill (Top Left)
                Surface(
                    color = Color(0xCC0B132B),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val (typeIcon, typeColor) = when (item.mediaType) {
                            CapturedMediaType.SKETCH_IMAGE -> Icons.Default.Description to Color(0xFF00E5FF)
                            CapturedMediaType.PHYSICAL_PHOTO -> Icons.Default.CameraAlt to Color(0xFF4CAF50)
                            CapturedMediaType.MULTI_ANGLE_PHOTOS -> Icons.Default.Collections to Color(0xFFFFB300)
                            CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES -> Icons.Default.Videocam to Color(0xFFFF5252)
                            else -> Icons.Default.Image to Color.White
                        }
                        Icon(
                            typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = when (item.mediaType) {
                                CapturedMediaType.SKETCH_IMAGE -> "SKETCH"
                                CapturedMediaType.PHYSICAL_PHOTO -> "PHOTO"
                                CapturedMediaType.MULTI_ANGLE_PHOTOS -> "${item.totalFramesCount} ANGLES"
                                CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES -> "${item.videoDurationSeconds}s VIDEO"
                                else -> "MEDIA"
                            },
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Frame Count & Resolution Pill (Bottom Left)
                Surface(
                    color = Color(0xAA000000),
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "${item.qualityScore}% AI clarity",
                        color = Color(0xFF64FFDA),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Quick Inspect Action Overlay Button (Bottom Right)
                IconButton(
                    onClick = onInspect,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color(0xCC1E293B), CircleShape)
                        .testTag("inspect_media_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Inspect Full Media",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Card Text & Actions Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateFormatted = remember(item.timestamp) {
                        SimpleDateFormat("HH:mm • dd MMM", Locale.getDefault()).format(Date(item.timestamp))
                    }
                    Text(
                        text = dateFormatted,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("delete_media_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete media item",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- Detail Inspection Modal Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInspectionDialog(
    item: CapturedMediaItem,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
    onAnalyzeSingle: () -> Unit,
    onDismiss: () -> Unit
) {
    var activePreviewBitmapIndex by remember { mutableIntStateOf(0) }
    val allBitmaps = item.allBitmaps
    val currentBitmap = allBitmaps.getOrNull(activePreviewBitmapIndex) ?: item.bitmap

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.mediaType.label} • ${item.resolutionLabel}",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Zoomable / High-Res View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF050914))
                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = "Full Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("No preview bitmap available", color = Color.Gray)
                    }

                    // If multi-angle/video, show active frame index pill
                    if (allBitmaps.size > 1) {
                        Surface(
                            color = Color(0xCC0B132B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = "Frame ${activePreviewBitmapIndex + 1} of ${allBitmaps.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // If multiple perspective frames / keyframes, show horizontal selector strip
                if (allBitmaps.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allBitmaps.forEachIndexed { idx, bmp ->
                            val isSelectedFrame = idx == activePreviewBitmapIndex
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelectedFrame) 2.dp else 1.dp,
                                        color = if (isSelectedFrame) Color(0xFF00E5FF) else Color(0x4464748B),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { activePreviewBitmapIndex = idx }
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Frame $idx",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Details Card
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "AI Clarity: ${item.qualityScore}%",
                                color = Color(0xFF64FFDA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Est. Rooms: ${item.estimatedRoomCount}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        if (item.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notes: ${item.notes}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Selection Button
                    OutlinedButton(
                        onClick = onToggleSelect,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF64748B)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("inspect_toggle_select_button")
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSelected) "Selected" else "Select",
                            color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Direct Analyze This Single Item
                    Button(
                        onClick = onAnalyzeSingle,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("inspect_analyze_single_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze with AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Bottom Bar: Gemini AI Analysis Dispatcher ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAnalysisDispatcherBottomBar(
    selectedCount: Int,
    isProcessing: Boolean,
    processStatus: String,
    analysisError: String?,
    showSpecsSheet: Boolean,
    selectedCounty: String,
    targetAreaSqm: Float,
    preferredBedrooms: Int,
    preferredBathrooms: Int,
    analysisNotes: String,
    onToggleSpecs: () -> Unit,
    onCountyChange: (String) -> Unit,
    onAreaChange: (Float) -> Unit,
    onBedroomsChange: (Int) -> Unit,
    onBathroomsChange: (Int) -> Unit,
    onNotesChange: (String) -> Unit,
    onDispatchAnalysis: () -> Unit
) {
    val counties = listOf("Nairobi", "Kiambu", "Machakos", "Nakuru", "Mombasa", "Kisumu", "Kajiado", "Uasin Gishu")

    Surface(
        color = Color(0xFF0B132B),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Processing or Error State
            if (isProcessing) {
                GeminiInlineProgressCard(
                    processStatus = processStatus,
                    mediaType = CapturedMediaType.MIXED_MEDIA,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            } else if (analysisError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = analysisError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Expandable Specs Customization Card
            AnimatedVisibility(visible = showSpecsSheet) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kenyan Context & Target Parameters",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Close",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onToggleSpecs() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // County Chips
                    Text("Target County:", color = Color.White, fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        counties.forEach { county ->
                            val isSelected = selectedCounty == county
                            Surface(
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0x3300E5FF),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { onCountyChange(county) }
                            ) {
                                Text(
                                    text = county,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Beds & Baths Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bedrooms
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bedrooms: $preferredBedrooms", color = Color.White, fontSize = 11.sp)
                            Slider(
                                value = preferredBedrooms.toFloat(),
                                onValueChange = { onBedroomsChange(it.toInt()) },
                                valueRange = 1f..6f,
                                steps = 4,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF)
                                )
                            )
                        }

                        // Bathrooms
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bathrooms: $preferredBathrooms", color = Color.White, fontSize = 11.sp)
                            Slider(
                                value = preferredBathrooms.toFloat(),
                                onValueChange = { onBathroomsChange(it.toInt()) },
                                valueRange = 1f..5f,
                                steps = 3,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF)
                                )
                            )
                        }
                    }

                    // Prompt Notes input
                    OutlinedTextField(
                        value = analysisNotes,
                        onValueChange = onNotesChange,
                        placeholder = {
                            Text("Extra prompt instructions (e.g., 'Open-plan kitchen, include pantry & ensuite porch')", fontSize = 11.sp, color = Color(0xFF64748B))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gallery_analysis_notes_input"),
                        maxLines = 2,
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Specs toggle button
                OutlinedIconButton(
                    onClick = onToggleSpecs,
                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("toggle_specs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure Parameters",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Main Dispatch Button
                Button(
                    onClick = onDispatchAnalysis,
                    enabled = selectedCount > 0 && !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("send_to_gemini_analysis_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedCount == 0) {
                                "Select Media to Analyze"
                            } else {
                                "Send $selectedCount Item(s) to Gemini AI"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// --- Empty Gallery State ---
@Composable
fun EmptyGalleryState(
    selectedTab: GalleryFilterTab,
    onOpenLiveCamera: () -> Unit,
    onLoadDemoMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No media in ${selectedTab.label}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Capture floor plan sketches, site photos, or 360° video scans using CameraX to analyze with Gemini AI.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onLoadDemoMedia,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("load_demo_media_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Load Sample Media", fontSize = 12.sp)
            }

            Button(
                onClick = onOpenLiveCamera,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_state_camera_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open CameraX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
