package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.ui.components.ZoomableFloorPlanCanvas
import com.example.ui.theme.AccentGreen
import org.json.JSONArray

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BlueprintCanvasScreen(
    roomsJson: String,
    title: String,
    areaSqm: Float,
    onSaveRooms: (List<RoomItem>) -> Unit = {}
) {
    // Parse rooms safely initially
    val initialRooms = remember(roomsJson) {
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
            // fallback standard
            list.add(RoomItem("Living Room", 5f, 4f, 0f, 0f, "#2196F3"))
            list.add(RoomItem("Bedroom", 4f, 4f, 5f, 0f, "#9C27B0"))
        }
        list
    }

    // Interactive customizer states
    var isEditMode by remember { mutableStateOf(false) }
    var editableRooms by remember(roomsJson) { mutableStateOf(initialRooms) }
    var selectedRoomIndex by remember { mutableStateOf<Int?>(null) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 4f)
        offset += offsetChange
    }

    val textMeasurer = rememberTextMeasurer()

    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    // Standard preset colors for room customizer picker
    val colorPresets = listOf(
        "#2196F3" to "Blue",
        "#4CAF50" to "Green",
        "#FF9800" to "Orange",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple",
        "#F44336" to "Red",
        "#00BCD4" to "Cyan"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scrollable, Zoomable Floor Plan Canvas Component
        ZoomableFloorPlanCanvas(
            rooms = editableRooms,
            isEditMode = isEditMode,
            selectedRoomIndex = selectedRoomIndex,
            onRoomTap = { index ->
                if (isEditMode) {
                    selectedRoomIndex = index
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Left top corner: Legend / Technical details
        Card(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                val totalAreaCalculated = editableRooms.sumOf { (it.width * it.height).toDouble() }.toFloat()
                Text(
                    text = "Total Footprint Area: ${totalAreaCalculated.roundTo(1)} sqm",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Scale: 1:${(100 / scale).toInt()} (Interactive)",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Right top corner: Toggle Edit Mode button & Architectural Compass
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Edit Mode Trigger
            Button(
                onClick = { 
                    isEditMode = !isEditMode 
                    if (!isEditMode) {
                        selectedRoomIndex = null
                    } else if (editableRooms.isNotEmpty()) {
                        selectedRoomIndex = 0
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditMode) AccentGreen else MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("toggle_floor_plan_edit_mode")
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit, 
                    contentDescription = "Edit Mode",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEditMode) "Previewing" else "Customize Layout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = {},
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(30.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = "North Indicator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Bottom Editor Panel or Zoom Controllers
        if (isEditMode) {
            // Room-by-Room Customizer Sliders and Tools
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .heightIn(max = 320.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Room-by-Room Customizer",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Add Room trigger
                            TextButton(
                                onClick = { showAddRoomDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.testTag("editor_add_room_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Save Trigger
                            Button(
                                onClick = {
                                    onSaveRooms(editableRooms)
                                    isEditMode = false
                                    selectedRoomIndex = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                                modifier = Modifier.testTag("editor_save_layout_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal selection of rooms
                    Text(
                        text = "Select room to customize:",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(editableRooms) { index, room ->
                            val isSelected = selectedRoomIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRoomIndex = index },
                                label = { Text(room.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("editor_room_chip_$index")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selected Room sliders & rename
                    selectedRoomIndex?.let { index ->
                        if (index < editableRooms.size) {
                            val activeRoom = editableRooms[index]

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Name Edit
                                    OutlinedTextField(
                                        value = activeRoom.name,
                                        onValueChange = { newName ->
                                            editableRooms = editableRooms.toMutableList().apply {
                                                this[index] = activeRoom.copy(name = newName)
                                            }
                                        },
                                        label = { Text("Room Label", fontSize = 10.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("editor_room_name_input"),
                                        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    )

                                    // Delete Room Button
                                    IconButton(
                                        onClick = {
                                            editableRooms = editableRooms.toMutableList().apply {
                                                removeAt(index)
                                            }
                                            selectedRoomIndex = if (editableRooms.isNotEmpty()) 0 else null
                                        },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape)
                                            .testTag("editor_delete_room_button")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Room", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                // Dimension controllers: Width and Height sliders side by side
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Width: ${activeRoom.width.roundTo(1)}m",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Slider(
                                            value = activeRoom.width,
                                            onValueChange = { newVal ->
                                                editableRooms = editableRooms.toMutableList().apply {
                                                    this[index] = activeRoom.copy(width = newVal)
                                                }
                                            },
                                            valueRange = 1.5f..10.0f,
                                            modifier = Modifier.testTag("editor_slider_room_width")
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Height: ${activeRoom.height.roundTo(1)}m",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Slider(
                                            value = activeRoom.height,
                                            onValueChange = { newVal ->
                                                editableRooms = editableRooms.toMutableList().apply {
                                                    this[index] = activeRoom.copy(height = newVal)
                                                }
                                            },
                                            valueRange = 1.5f..10.0f,
                                            modifier = Modifier.testTag("editor_slider_room_height")
                                        )
                                    }
                                }

                                // Layout Offset controllers: X and Y sliders side by side
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Align X: ${activeRoom.x.roundTo(1)}m",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Slider(
                                            value = activeRoom.x,
                                            onValueChange = { newVal ->
                                                editableRooms = editableRooms.toMutableList().apply {
                                                    this[index] = activeRoom.copy(x = newVal)
                                                }
                                            },
                                            valueRange = 0f..15.0f,
                                            modifier = Modifier.testTag("editor_slider_room_x")
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Align Y: ${activeRoom.y.roundTo(1)}m",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Slider(
                                            value = activeRoom.y,
                                            onValueChange = { newVal ->
                                                editableRooms = editableRooms.toMutableList().apply {
                                                    this[index] = activeRoom.copy(y = newVal)
                                                }
                                            },
                                            valueRange = 0f..15.0f,
                                            modifier = Modifier.testTag("editor_slider_room_y")
                                        )
                                    }
                                }

                                // Color Preset Dots
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Theme:", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    colorPresets.forEach { (hex, name) ->
                                        val isChosen = activeRoom.color.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                                .clickable {
                                                    editableRooms = editableRooms.toMutableList().apply {
                                                        this[index] = activeRoom.copy(color = hex)
                                                    }
                                                }
                                                .testTag("editor_color_dot_$name"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChosen) {
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Select a room above to begin customizing", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // Bottom standard interactive UI: Zoom Controllers
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scale = (scale - 0.2f).coerceAtLeast(0.5f) },
                    modifier = Modifier.testTag("zoom_out_button")
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { scale = (scale + 0.2f).coerceAtMost(4f) },
                    modifier = Modifier.testTag("zoom_in_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.testTag("reset_zoom_button")
                ) {
                    Icon(imageVector = Icons.Default.ZoomOutMap, contentDescription = "Reset Zoom", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Modal dialog to add a new room
    if (showAddRoomDialog) {
        var newRoomName by remember { mutableStateOf("New Room") }
        var newRoomWidth by remember { mutableStateOf("4.0") }
        var newRoomHeight by remember { mutableStateOf("3.5") }
        var newRoomColor by remember { mutableStateOf("#2196F3") }

        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = { Text("Add Custom Room Unit", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("Room Label (e.g. Study, Garage)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_room_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newRoomWidth,
                            onValueChange = { newRoomWidth = it },
                            label = { Text("Width (m)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_room_width_input")
                        )
                        OutlinedTextField(
                            value = newRoomHeight,
                            onValueChange = { newRoomHeight = it },
                            label = { Text("Height (m)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_room_height_input")
                        )
                    }

                    // Color choice dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color Spec:", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
                        colorPresets.forEach { (hex, name) ->
                            val isChosen = newRoomColor == hex
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .clickable { newRoomColor = hex }
                                    .testTag("add_room_color_dot_$name"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChosen) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = newRoomWidth.toFloatOrNull() ?: 3.0f
                        val h = newRoomHeight.toFloatOrNull() ?: 3.0f
                        // Find a good free coordinate space by reading max x
                        val nextX = editableRooms.maxOfOrNull { it.x + it.width } ?: 0f
                        val newRoom = RoomItem(
                            name = newRoomName.ifBlank { "Utility Room" },
                            width = w,
                            height = h,
                            x = nextX,
                            y = 0f,
                            color = newRoomColor
                        )
                        editableRooms = editableRooms.toMutableList().apply {
                            add(newRoom)
                        }
                        selectedRoomIndex = editableRooms.size - 1
                        showAddRoomDialog = false
                    },
                    modifier = Modifier.testTag("add_room_confirm_button")
                ) {
                    Text("Add to Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun Float.roundTo(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10f }
    return (this * multiplier).toInt() / multiplier
}
