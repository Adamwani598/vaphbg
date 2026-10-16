package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val pageCount = 3

    val titleText = when (currentPage) {
        0 -> "AI Sketch to Blueprint"
        1 -> "NCA Construction Costings"
        else -> "County Compliance & Permits"
    }

    val descText = when (currentPage) {
        0 -> "Scan or upload hand-drawn drafts. NyumbaPlan uses Gemini Vision AI to instantly map rooms, calculate surface area, and draft clean CAD-style architectural floor plans."
        1 -> "Calculate comprehensive technical cost estimations instantly. Get material lists, excavation, timbering, and finish cost ranges conforming to National Construction Authority indices."
        else -> "Self-assess your layout boundary setbacks, daylight ratios, and plot coverages. Generate certified physical planning reports ready for formal engineer stamping and county submission."
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0D1B2A), // Premium Navy Deep Background
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Page Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = index == currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "indicatorWidth"
                        )
                        val color = if (isSelected) Color(0xFF00B4D8) else Color(0xFF1B263B)

                        Box(
                            modifier = Modifier
                                .size(width = width, height = 8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Right side: Action Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage < pageCount - 1) {
                        TextButton(
                            onClick = onOnboardingComplete,
                            modifier = Modifier
                                .testTag("onboarding_skip_button")
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = "Skip",
                                color = Color(0xFF78909C),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentPage < pageCount - 1) {
                                currentPage++
                            } else {
                                onOnboardingComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00B4D8),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("onboarding_next_button")
                            .minimumInteractiveComponentSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (currentPage == pageCount - 1) "Get Started" else "Next",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (currentPage == pageCount - 1) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                                contentDescription = "Next Icon",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Canvas Illustration Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = currentPage,
                    animationSpec = tween(500),
                    label = "CanvasTransition"
                ) { page ->
                    when (page) {
                        0 -> OnboardingSketchCanvas()
                        1 -> OnboardingCostingCanvas()
                        2 -> OnboardingComplianceCanvas()
                    }
                }
            }

            // Text Onboarding Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = titleText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.testTag("onboarding_title")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = descText,
                    fontSize = 14.sp,
                    color = Color(0xFF78909C), // Slate muted/light text
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .testTag("onboarding_desc")
                )
            }
        }
    }
}

@Composable
fun OnboardingSketchCanvas() {
    // Pulse animation for scan bar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scanYOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanBar"
    )

    Canvas(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.85f)
    ) {
        val w = size.width
        val h = size.height

        // Background Slate
        drawRoundRect(
            color = Color(0xFF1B263B),
            size = size,
            cornerRadius = CornerRadius(24f, 24f)
        )

        // Draw CAD style blue Grid background
        val gridSpacing = 24f
        val gridPaintColor = Color(0xFF00B4D8).copy(alpha = 0.06f)
        var x = 0f
        while (x < w) {
            drawLine(color = gridPaintColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
            x += gridSpacing
        }
        var y = 0f
        while (y < h) {
            drawLine(color = gridPaintColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
            y += gridSpacing
        }

        // Draw hand-drawn sketch (Left to center)
        val sketchPath = Path().apply {
            moveTo(w * 0.15f, h * 0.35f)
            lineTo(w * 0.55f, h * 0.35f)
            lineTo(w * 0.55f, h * 0.75f)
            lineTo(w * 0.15f, h * 0.75f)
            close()

            // inner partitions
            moveTo(w * 0.35f, h * 0.35f)
            lineTo(w * 0.35f, h * 0.75f)
            moveTo(w * 0.35f, h * 0.55f)
            lineTo(w * 0.55f, h * 0.55f)
        }

        // Left half shows hand sketch (rough, sketchy dashed grey lines)
        drawPath(
            path = sketchPath,
            color = Color(0xFF78909C),
            style = Stroke(
                width = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            )
        )

        // Right half shows digital blueprints (precise solid cyan lines with double-walls)
        val digitalPathOuter = Path().apply {
            moveTo(w * 0.55f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.75f)
            lineTo(w * 0.55f, h * 0.75f)
        }
        drawPath(
            path = digitalPathOuter,
            color = Color(0xFF00B4D8),
            style = Stroke(width = 6f)
        )
        // Precise double lines
        val digitalPathInner = Path().apply {
            moveTo(w * 0.55f, h * 0.37f)
            lineTo(w * 0.83f, h * 0.37f)
            lineTo(w * 0.83f, h * 0.73f)
            lineTo(w * 0.55f, h * 0.73f)
        }
        drawPath(
            path = digitalPathInner,
            color = Color.White,
            style = Stroke(width = 1.5f)
        )

        // Partition lines on digitized side
        drawLine(
            color = Color(0xFF00B4D8),
            start = Offset(w * 0.55f, h * 0.55f),
            end = Offset(w * 0.85f, h * 0.55f),
            strokeWidth = 4f
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.55f, h * 0.55f),
            end = Offset(w * 0.85f, h * 0.55f),
            strokeWidth = 1f
        )

        // Label Room Names
        // "KITCHEN"
        drawRoundRect(
            color = Color(0xFF0D1B2A).copy(alpha = 0.8f),
            topLeft = Offset(w * 0.62f, h * 0.42f),
            size = Size(w * 0.16f, h * 0.08f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // "LIVING"
        drawRoundRect(
            color = Color(0xFF0D1B2A).copy(alpha = 0.8f),
            topLeft = Offset(w * 0.62f, h * 0.61f),
            size = Size(w * 0.16f, h * 0.08f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Draw animated laser scan bar
        val scanY = h * scanYOffset
        val laserBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00B4D8).copy(alpha = 0.05f),
                Color(0xFF00B4D8).copy(alpha = 0.4f),
                Color(0xFF00B4D8).copy(alpha = 0.05f)
            ),
            start = Offset(0f, scanY - 20f),
            end = Offset(0f, scanY + 20f)
        )
        drawRect(
            brush = laserBrush,
            topLeft = Offset(0f, scanY - 20f),
            size = Size(w, 40f)
        )
        drawLine(
            color = Color(0xFF00B4D8),
            start = Offset(w * 0.05f, scanY),
            end = Offset(w * 0.95f, scanY),
            strokeWidth = 2.5f
        )
    }
}

@Composable
fun OnboardingCostingCanvas() {
    // Upward bar expansion animation
    val infiniteTransition = rememberInfiniteTransition(label = "charts")
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barScale"
    )

    Canvas(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.85f)
    ) {
        val w = size.width
        val h = size.height

        // Background Slate
        drawRoundRect(
            color = Color(0xFF1B263B),
            size = size,
            cornerRadius = CornerRadius(24f, 24f)
        )

        // Draw dashboard circular gauges/indicators in background
        drawArc(
            color = Color(0xFF0D1B2A),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.3f, w * 0.3f),
            style = Stroke(width = 8f)
        )
        drawArc(
            color = Color(0xFF00B4D8),
            startAngle = 180f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.3f, w * 0.3f),
            style = Stroke(width = 8f)
        )

        // Small circular progress on the right
        drawArc(
            color = Color(0xFF0D1B2A),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(w * 0.65f, h * 0.18f),
            size = Size(w * 0.15f, w * 0.15f),
            style = Stroke(width = 6f)
        )
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(w * 0.65f, h * 0.18f),
            size = Size(w * 0.15f, w * 0.15f),
            style = Stroke(width = 6f)
        )

        // Draw Bar Graph at the bottom half
        val barW = w * 0.14f
        val barGap = w * 0.05f
        val startX = w * 0.14f
        val baseY = h * 0.85f

        val barHeights = listOf(
            h * 0.22f, // Foundations
            h * 0.40f, // Walling
            h * 0.32f, // Roofing
            h * 0.15f  // Finishes
        )
        val barColors = listOf(
            Color(0xFFE9C46A), // Sand Gold
            Color(0xFFE76F51), // Terracotta block
            Color(0xFF264653), // Deep slate roofing
            Color(0xFF00B4D8)  // Finishes Teal
        )

        for (i in barHeights.indices) {
            val barX = startX + i * (barW + barGap)
            val currentBarMaxH = barHeights[i]
            val animatedH = currentBarMaxH * heightScale

            // Draw shadow bar
            drawRoundRect(
                color = Color(0xFF0D1B2A),
                topLeft = Offset(barX, baseY - currentBarMaxH),
                size = Size(barW, currentBarMaxH),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Draw growing active bar
            drawRoundRect(
                color = barColors[i],
                topLeft = Offset(barX, baseY - animatedH),
                size = Size(barW, animatedH),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }

        // Base ground line
        drawLine(
            color = Color(0xFF0D1B2A),
            start = Offset(w * 0.08f, baseY),
            end = Offset(w * 0.92f, baseY),
            strokeWidth = 3f
        )
    }
}

@Composable
fun OnboardingComplianceCanvas() {
    // Pulse animation for certified seal
    val infiniteTransition = rememberInfiniteTransition(label = "compliance")
    val pulseSizeMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseSeal"
    )

    Canvas(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.85f)
    ) {
        val w = size.width
        val h = size.height

        // Background Slate
        drawRoundRect(
            color = Color(0xFF1B263B),
            size = size,
            cornerRadius = CornerRadius(24f, 24f)
        )

        // Draw Plot Boundaries layout (Dashed line for parcel borders)
        drawRoundRect(
            color = Color(0xFF0D1B2A),
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        )

        // Draw House layout on the parcel
        val houseX = w * 0.28f
        val houseY = h * 0.32f
        val houseW = w * 0.55f
        val houseH = h * 0.48f

        drawRoundRect(
            color = Color(0xFF0D1B2A),
            topLeft = Offset(houseX, houseY),
            size = Size(houseW, houseH),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Dimension indicators & spacing measurements
        // Front setbacks (Left space: 6.0m minimum)
        val setbackLineY = h * 0.56f
        drawLine(
            color = Color(0xFFE76F51),
            start = Offset(w * 0.12f, setbackLineY),
            end = Offset(houseX, setbackLineY),
            strokeWidth = 2f
        )
        // Setback end markers
        drawLine(
            color = Color(0xFFE76F51),
            start = Offset(w * 0.12f, setbackLineY - 8f),
            end = Offset(w * 0.12f, setbackLineY + 8f),
            strokeWidth = 2f
        )
        drawLine(
            color = Color(0xFFE76F51),
            start = Offset(houseX, setbackLineY - 8f),
            end = Offset(houseX, setbackLineY + 8f),
            strokeWidth = 2f
        )

        // Draw a glowing Green Certified Badge (Seal of approval) in top-right
        val sealX = w * 0.28f
        val sealY = h * 0.28f
        val baseSealRadius = w * 0.14f
        val animatedSealRadius = baseSealRadius * pulseSizeMultiplier

        // Outer glow
        drawCircle(
            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
            radius = animatedSealRadius * 1.3f,
            center = Offset(sealX, sealY)
        )
        drawCircle(
            color = Color(0xFF2E7D32),
            radius = animatedSealRadius,
            center = Offset(sealX, sealY)
        )
        drawCircle(
            color = Color.White,
            radius = animatedSealRadius * 0.85f,
            center = Offset(sealX, sealY),
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
            )
        )

        // Draw white checkmark inside seal
        val checkPath = Path().apply {
            moveTo(sealX - animatedSealRadius * 0.35f, sealY - animatedSealRadius * 0.05f)
            lineTo(sealX - animatedSealRadius * 0.08f, sealY + animatedSealRadius * 0.22f)
            lineTo(sealX + animatedSealRadius * 0.38f, sealY - animatedSealRadius * 0.28f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = 4.5f)
        )
    }
}
