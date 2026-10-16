package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RoomItem
import kotlin.math.roundToInt

/**
 * A standalone, scrollable, zoomable canvas composable for displaying 2D floor plans.
 * Supports pinch-to-zoom, pan drag scrolling, room tap selection, interactive CAD dimension lines,
 * door/window symbols, and floating view controls.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun ZoomableFloorPlanCanvas(
    rooms: List<RoomItem>,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    selectedRoomIndex: Int? = null,
    onRoomTap: ((Int) -> Unit)? = null,
    showControls: Boolean = true,
    showGrid: Boolean = true
) {
    var scale by remember { mutableStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Transformable state for pinch zoom & pan drag scrolling
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.4f, 5.0f)
        offset += offsetChange
    }

    val textMeasurer = rememberTextMeasurer()

    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("zoomable_floor_plan_canvas_container")
    ) {
        // Core Canvas with gesture transformable layer
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformableState)
                .pointerInput(rooms, scale, offset) {
                    detectTapGestures { tapOffset ->
                        if (onRoomTap == null || rooms.isEmpty()) return@detectTapGestures

                        // Reverse canvas transformations to map screen tap -> canvas world coordinates
                        val drawWidth = size.width.toFloat()
                        val drawHeight = size.height.toFloat()
                        val centerX = drawWidth / 2f
                        val centerY = drawHeight / 2f

                        val maxRoomX = rooms.maxOfOrNull { it.x + it.width } ?: 10f
                        val maxRoomY = rooms.maxOfOrNull { it.y + it.height } ?: 10f
                        val scaleFactor = (drawWidth * 0.7f / maxRoomX)
                            .coerceAtMost(drawHeight * 0.7f / maxRoomY)
                            .coerceIn(30f, 80f)

                        val startX = centerX - (maxRoomX * scaleFactor) / 2f
                        val startY = centerY - (maxRoomY * scaleFactor) / 2f

                        val worldX = (tapOffset.x - offset.x - centerX) / scale + centerX
                        val worldY = (tapOffset.y - offset.y - centerY) / scale + centerY

                        // Check which room bounds contain world position
                        rooms.forEachIndexed { index, room ->
                            val rx = startX + room.x * scaleFactor
                            val ry = startY + room.y * scaleFactor
                            val rw = room.width * scaleFactor
                            val rh = room.height * scaleFactor

                            if (worldX in rx..(rx + rw) && worldY in ry..(ry + rh)) {
                                onRoomTap(index)
                                return@detectTapGestures
                            }
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .testTag("zoomable_floor_plan_canvas")
        ) {
            val drawWidth = size.width
            val drawHeight = size.height
            val centerX = drawWidth / 2f
            val centerY = drawHeight / 2f

            // 1. Draw Architectural Grid Overlay
            if (showGrid) {
                val gridSize = 40f
                var xVal = 0f
                while (xVal < drawWidth) {
                    drawLine(
                        color = onBackgroundColor.copy(alpha = 0.08f),
                        start = Offset(xVal, 0f),
                        end = Offset(xVal, drawHeight),
                        strokeWidth = 1f
                    )
                    xVal += gridSize
                }

                var yVal = 0f
                while (yVal < drawHeight) {
                    drawLine(
                        color = onBackgroundColor.copy(alpha = 0.08f),
                        start = Offset(0f, yVal),
                        end = Offset(drawWidth, yVal),
                        strokeWidth = 1f
                    )
                    yVal += gridSize
                }
            }

            // 2. Scale factor calculation (meters -> pixels)
            val maxRoomX = rooms.maxOfOrNull { it.x + it.width } ?: 10f
            val maxRoomY = rooms.maxOfOrNull { it.y + it.height } ?: 10f

            val scaleFactor = (drawWidth * 0.7f / maxRoomX)
                .coerceAtMost(drawHeight * 0.7f / maxRoomY)
                .coerceIn(30f, 80f)

            val startX = centerX - (maxRoomX * scaleFactor) / 2f
            val startY = centerY - (maxRoomY * scaleFactor) / 2f

            // 3. Render 2D Floor Plan Rooms
            rooms.forEachIndexed { index, room ->
                val rx = startX + room.x * scaleFactor
                val ry = startY + room.y * scaleFactor
                val rw = room.width * scaleFactor
                val rh = room.height * scaleFactor

                val baseColor = try {
                    Color(android.graphics.Color.parseColor(room.color))
                } catch (e: Exception) {
                    Color(0xFF2196F3)
                }

                // Fill translucent background
                drawRect(
                    color = baseColor.copy(alpha = 0.15f),
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh)
                )

                // Thick outer structural CAD walls
                drawRect(
                    color = surfaceVariantColor,
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh),
                    style = Stroke(width = 6f)
                )

                drawRect(
                    color = onSurfaceColor,
                    topLeft = Offset(rx + 2f, ry + 2f),
                    size = Size(rw - 4f, rh - 4f),
                    style = Stroke(width = 1.5f)
                )

                // Selected Room visual indicator
                if (isEditMode && selectedRoomIndex == index) {
                    drawRect(
                        color = Color(0xFF00B4D8),
                        topLeft = Offset(rx - 3f, ry - 3f),
                        size = Size(rw + 6f, rh + 6f),
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                        )
                    )
                }

                // 4. CAD Architectural Details: Door Arc & Window Cutout
                val doorSize = 1.0f * scaleFactor
                if (rw > doorSize + 10f) {
                    drawArc(
                        color = onSurfaceColor,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset(rx, ry + rh - doorSize),
                        size = Size(doorSize, doorSize),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                        )
                    )
                }

                if (rw > 2.5f * scaleFactor) {
                    val winW = rw * 0.4f
                    val winX = rx + (rw - winW) / 2f
                    drawLine(
                        color = primaryColor,
                        start = Offset(winX, ry),
                        end = Offset(winX + winW, ry),
                        strokeWidth = 4f
                    )
                    drawLine(
                        color = onSurfaceColor,
                        start = Offset(winX, ry - 3f),
                        end = Offset(winX + winW, ry - 3f),
                        strokeWidth = 1f
                    )
                }

                // 5. Room text overlay
                val cleanLabel = room.name
                val sizeLabel = "${(room.width * 10).roundToInt() / 10f}m × ${(room.height * 10).roundToInt() / 10f}m"

                val labelStyle = TextStyle(
                    color = onSurfaceColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val sizeStyle = TextStyle(
                    color = primaryColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                val labelLayout = textMeasurer.measure(AnnotatedString(cleanLabel), style = labelStyle)
                val sizeLayout = textMeasurer.measure(AnnotatedString(sizeLabel), style = sizeStyle)

                val lX = rx + (rw - labelLayout.size.width) / 2f
                val lY = ry + (rh - labelLayout.size.height - sizeLayout.size.height) / 2f

                drawText(textLayoutResult = labelLayout, topLeft = Offset(lX, lY))
                drawText(
                    textLayoutResult = sizeLayout,
                    topLeft = Offset(rx + (rw - sizeLayout.size.width) / 2f, lY + labelLayout.size.height + 2f)
                )
            }

            // 6. Outer Footprint Measurement Dimension Lines
            val outerMargin = 40f
            // Top Width Dimension
            drawLine(
                color = onBackgroundColor.copy(alpha = 0.4f),
                start = Offset(startX, startY - outerMargin),
                end = Offset(startX + maxRoomX * scaleFactor, startY - outerMargin),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            val totalWidthLabel = "${(maxRoomX * 10).roundToInt() / 10f} m"
            val widthLayout = textMeasurer.measure(
                text = AnnotatedString(totalWidthLabel),
                style = TextStyle(color = onSurfaceColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            drawText(
                textLayoutResult = widthLayout,
                topLeft = Offset(centerX - widthLayout.size.width / 2f, startY - outerMargin - widthLayout.size.height - 5f)
            )

            // Left Height Dimension
            drawLine(
                color = onBackgroundColor.copy(alpha = 0.4f),
                start = Offset(startX - outerMargin, startY),
                end = Offset(startX - outerMargin, startY + maxRoomY * scaleFactor),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            val totalHeightLabel = "${(maxRoomY * 10).roundToInt() / 10f} m"
            val heightLayout = textMeasurer.measure(
                text = AnnotatedString(totalHeightLabel),
                style = TextStyle(color = onSurfaceColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            )
            drawText(
                textLayoutResult = heightLayout,
                topLeft = Offset(startX - outerMargin - heightLayout.size.width - 8f, centerY - heightLayout.size.height / 2f)
            )
        }

        // Floating On-Screen View Controls (Zoom In, Zoom Out, Reset, Scale indicator)
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Current Zoom percentage chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "${(scale * 100).roundToInt()}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Zoom In
                SmallFloatingActionButton(
                    onClick = { scale = (scale * 1.25f).coerceAtMost(5.0f) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("canvas_zoom_in")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = { scale = (scale / 1.25f).coerceAtLeast(0.4f) },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("canvas_zoom_out")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }

                // Reset Zoom & Pan
                SmallFloatingActionButton(
                    onClick = {
                        scale = 1.0f
                        offset = Offset.Zero
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.testTag("canvas_zoom_reset")
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset Canvas View")
                }
            }
        }
    }
}
