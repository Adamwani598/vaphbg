package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.network.models.CapturedMediaType
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-precision Circular Progress Indicator with architectural radar scan rings,
 * pulsing glowing halo, and animated progress percentages for Gemini Vision AI processing.
 */
@Composable
fun GeminiCircularProgressIndicator(
    progress: Float? = null, // null for indeterminate, 0f..1f for determinate
    statusText: String = "Processing with Gemini AI...",
    mediaType: CapturedMediaType = CapturedMediaType.SKETCH_IMAGE,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 6.dp,
    primaryColor: Color = Color(0xFF00E5FF),
    secondaryColor: Color = Color(0xFF0077B6)
) {
    // Pulse animation for outer glowing halo
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransitions")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Radar rotation
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
    )

    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconRotation"
    )

    val displayProgress = progress?.coerceIn(0f, 1f)

    Column(
        modifier = modifier.testTag("gemini_circular_progress_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // Background pulsing glow halo
            Box(
                modifier = Modifier
                    .size(size * 0.92f)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = pulseAlpha * 0.35f),
                                secondaryColor.copy(alpha = pulseAlpha * 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Architectural CAD Dial Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(radarRotation)
            ) {
                val centerOffset = Offset(this.size.width / 2, this.size.height / 2)
                val radius = (this.size.minDimension / 2) - 8.dp.toPx()

                // Radar tick marks along perimeter
                val totalTicks = 24
                for (i in 0 until totalTicks) {
                    val angleDeg = (i * (360f / totalTicks)) * (Math.PI / 180f).toFloat()
                    val tickLen = if (i % 6 == 0) 8.dp.toPx() else 4.dp.toPx()
                    val startX = centerOffset.x + (radius - tickLen) * cos(angleDeg)
                    val startY = centerOffset.y + (radius - tickLen) * sin(angleDeg)
                    val endX = centerOffset.x + radius * cos(angleDeg)
                    val endY = centerOffset.y + radius * sin(angleDeg)

                    val tickAlpha = if (i % 6 == 0) 0.8f else 0.35f
                    drawLine(
                        color = primaryColor.copy(alpha = tickAlpha),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i % 6 == 0) 2.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Sweeping radar arc beam
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.05f),
                            primaryColor.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.8f)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 100f,
                    useCenter = true,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }

            // Material 3 CircularProgressIndicator track and progress arc
            if (displayProgress != null) {
                // Determinate Circular Progress Indicator
                CircularProgressIndicator(
                    progress = { displayProgress },
                    modifier = Modifier
                        .size(size - 16.dp)
                        .testTag("gemini_circular_progress_determinate"),
                    color = primaryColor,
                    trackColor = SpaceNavy.copy(alpha = 0.7f),
                    strokeWidth = strokeWidth,
                    strokeCap = StrokeCap.Round
                )
            } else {
                // Indeterminate Circular Progress Indicator
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(size - 16.dp)
                        .testTag("gemini_circular_progress_indeterminate"),
                    color = primaryColor,
                    trackColor = SpaceNavy.copy(alpha = 0.7f),
                    strokeWidth = strokeWidth,
                    strokeCap = StrokeCap.Round
                )
            }

            // Central Core Content (Percentage or Animated Gemini / Media Icon)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .size(size * 0.55f)
                    .background(SpaceNavy, CircleShape)
                    .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                    .padding(4.dp)
            ) {
                if (displayProgress != null) {
                    Text(
                        text = "${(displayProgress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "GEMINI AI",
                        color = primaryColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                } else {
                    Icon(
                        imageVector = when (mediaType) {
                            CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES -> Icons.Default.Videocam
                            CapturedMediaType.MULTI_ANGLE_PHOTOS -> Icons.Default.PhotoCamera
                            CapturedMediaType.PHYSICAL_PHOTO -> Icons.Default.CameraAlt
                            CapturedMediaType.SKETCH_IMAGE -> Icons.Default.AutoAwesome
                            CapturedMediaType.MIXED_MEDIA -> Icons.Default.Layers
                        },
                        contentDescription = "Processing Media",
                        tint = primaryColor,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(iconRotation)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AI SCAN",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen modal overlay displaying the animated Circular Progress Indicator,
 * multi-step architectural synthesis stages, and real-time process logs.
 */
@Composable
fun GeminiProcessingOverlayModal(
    processStatus: String,
    mediaType: CapturedMediaType = CapturedMediaType.SKETCH_IMAGE,
    onCancel: (() -> Unit)? = null
) {
    // Dynamic simulated multi-stage progress
    val isStage2Active = remember(processStatus) {
        processStatus.contains("Analyzing", ignoreCase = true) ||
        processStatus.contains("Vision", ignoreCase = true) ||
        processStatus.contains("Synthesizing", ignoreCase = true) ||
        processStatus.contains("Calculating", ignoreCase = true) ||
        processStatus.contains("Constructing", ignoreCase = true)
    }

    val isStage3Active = remember(processStatus) {
        processStatus.contains("Calculating", ignoreCase = true) ||
        processStatus.contains("Constructing", ignoreCase = true) ||
        processStatus.contains("blueprint", ignoreCase = true)
    }

    var calculatedProgress by remember { mutableFloatStateOf(0.15f) }

    LaunchedEffect(isStage2Active, isStage3Active) {
        if (isStage3Active) {
            // Stage 3 progress
            var p = 0.70f
            while (p < 0.94f) {
                delay(120)
                p += 0.03f
                calculatedProgress = p.coerceAtMost(0.96f)
            }
        } else if (isStage2Active) {
            // Stage 2 progress
            var p = 0.35f
            while (p < 0.70f) {
                delay(100)
                p += 0.04f
                calculatedProgress = p.coerceAtMost(0.70f)
            }
        } else {
            // Stage 1 progress
            var p = 0.10f
            while (p < 0.35f) {
                delay(80)
                p += 0.03f
                calculatedProgress = p.coerceAtMost(0.35f)
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = calculatedProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "animatedOverallProgress"
    )

    Dialog(
        onDismissRequest = { /* Prevent dismiss during active AI processing */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060D17).copy(alpha = 0.92f))
                .padding(20.dp)
                .testTag("gemini_processing_modal_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateBlue),
                border = BorderStroke(1.5.dp, BlueprintCyan.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("gemini_processing_card")
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badge
                    Surface(
                        color = SpaceNavy,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BlueprintCyan.copy(alpha = 0.5f))
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
                                text = when (mediaType) {
                                    CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES -> "360° VIDEO TELEMETRY SCAN"
                                    CapturedMediaType.MULTI_ANGLE_PHOTOS -> "MULTI-ANGLE STEREO VISION"
                                    CapturedMediaType.PHYSICAL_PHOTO -> "SITE ROOM PHOTO RECON"
                                    CapturedMediaType.SKETCH_IMAGE -> "GEMINI 2.5 VISION AI ENGINE"
                                    CapturedMediaType.MIXED_MEDIA -> "MULTIMODAL COMPOSITE RECON"
                                },
                                color = BlueprintCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Synthesizing Floor Plan",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Analyzing spatial geometry, room boundaries & Kenyan building bylaws",
                        color = MutedSlate,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dedicated Circular Progress Indicator
                    GeminiCircularProgressIndicator(
                        progress = animatedProgress,
                        statusText = processStatus,
                        mediaType = mediaType,
                        size = 150.dp,
                        strokeWidth = 7.dp,
                        primaryColor = Color(0xFF00E5FF),
                        secondaryColor = Color(0xFF0077B6)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step by step progression
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SpaceNavy, RoundedCornerShape(12.dp))
                            .border(1.dp, OutlineBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProcessingStepRow(
                            stepNumber = 1,
                            title = "Media Tensor Encoding",
                            subtitle = "High-contrast contour & wall detection",
                            isCompleted = isStage2Active,
                            isCurrent = !isStage2Active
                        )

                        HorizontalDivider(color = OutlineBlue.copy(alpha = 0.3f), thickness = 0.5.dp)

                        ProcessingStepRow(
                            stepNumber = 2,
                            title = "Gemini 2.5 Vision Spatial Mapping",
                            subtitle = "Extracting room partitions & meter dimensions",
                            isCompleted = isStage3Active,
                            isCurrent = isStage2Active && !isStage3Active
                        )

                        HorizontalDivider(color = OutlineBlue.copy(alpha = 0.3f), thickness = 0.5.dp)

                        ProcessingStepRow(
                            stepNumber = 3,
                            title = "2D CAD Coordinate & Cost Schedule",
                            subtitle = "Generating Room polygons & NCA bill of quantities",
                            isCompleted = false,
                            isCurrent = isStage3Active
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Real-time status ticker with miniature circular spinner
                    Surface(
                        color = SpaceNavy.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BlueprintCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = BlueprintCyan,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(14.dp)
                                    .testTag("gemini_modal_mini_spinner")
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = processStatus.ifBlank { "Analyzing drawing features with Gemini..." },
                                color = LightSlate,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.testTag("gemini_modal_status_ticker")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingStepRow(
    stepNumber: Int,
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Step Icon/Circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when {
                        isCompleted -> AccentGreen
                        isCurrent -> BlueprintCyan
                        else -> SpaceNavy
                    },
                    CircleShape
                )
                .border(
                    1.dp,
                    when {
                        isCompleted -> AccentGreen
                        isCurrent -> BlueprintCyan
                        else -> OutlineBlue
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = SpaceNavy,
                    modifier = Modifier.size(14.dp)
                )
            } else if (isCurrent) {
                CircularProgressIndicator(
                    color = SpaceNavy,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = "$stepNumber",
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isCompleted -> Color.White
                    isCurrent -> BlueprintCyan
                    else -> MutedSlate
                }
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isCurrent) LightSlate.copy(alpha = 0.8f) else MutedSlate.copy(alpha = 0.7f),
                lineHeight = 12.sp
            )
        }

        if (isCurrent) {
            Text(
                text = "ACTIVE",
                color = BlueprintCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Compact Inline Card with Circular Progress Indicator for bottom bars, review sheets, or forms.
 */
@Composable
fun GeminiInlineProgressCard(
    processStatus: String,
    modifier: Modifier = Modifier,
    mediaType: CapturedMediaType = CapturedMediaType.SKETCH_IMAGE
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateBlue),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_inline_progress_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Circular progress indicator
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gemini_inline_circular_indicator"),
                    color = Color(0xFF00E5FF),
                    trackColor = SpaceNavy,
                    strokeWidth = 3.5.dp,
                    strokeCap = StrokeCap.Round
                )
                Icon(
                    imageVector = when (mediaType) {
                        CapturedMediaType.WALKTHROUGH_VIDEO_FRAMES -> Icons.Default.Videocam
                        CapturedMediaType.MULTI_ANGLE_PHOTOS -> Icons.Default.PhotoCamera
                        CapturedMediaType.PHYSICAL_PHOTO -> Icons.Default.CameraAlt
                        CapturedMediaType.SKETCH_IMAGE -> Icons.Default.AutoAwesome
                        CapturedMediaType.MIXED_MEDIA -> Icons.Default.Layers
                    },
                    contentDescription = "Gemini Processing",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Gemini AI Vision Analysis",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AccentGreen, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = processStatus.ifBlank { "Processing sketch tensors & layout polygons..." },
                    color = Color(0xFF90E0EF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
    }
}
