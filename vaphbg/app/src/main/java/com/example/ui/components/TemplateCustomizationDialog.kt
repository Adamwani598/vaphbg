package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BlueprintEngine
import com.example.data.BuildingTemplate
import com.example.data.CountyZoningPresets
import com.example.data.RoomItem
import com.example.ui.theme.AccentGreen
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateCustomizationDialog(
    template: BuildingTemplate,
    onDismiss: () -> Unit,
    onConfirmCustomization: (
        customTitle: String,
        customAreaSqm: Float,
        county: String,
        finishesQuality: String,
        roofType: String,
        includedModules: List<String>,
        storeys: Int
    ) -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
            maximumFractionDigits = 0
        }
    }

    var projectTitle by remember { mutableStateOf(template.title) }
    var selectedAreaSqm by remember { mutableFloatStateOf(template.baseAreaSqm) }
    var selectedCounty by remember { mutableStateOf("Nairobi") }
    var selectedFinishes by remember { mutableStateOf("Standard") }
    var selectedRoof by remember { mutableStateOf(template.recommendedRoof) }
    val selectedAddonModules = remember { mutableStateListOf<String>() }

    // Multi-storey configuration: supports up to 10 storeys
    var selectedStoreys by remember {
        mutableIntStateOf(if (template.isMultiStorey) template.defaultStoreys else 1)
    }
    var previewLevelIndex by remember { mutableIntStateOf(0) } // 0: Ground Floor, 1: Typical Floor, 2: Penthouse

    val countiesList = remember {
        listOf("Nairobi", "Kiambu", "Mombasa", "Nakuru", "Kisumu", "Machakos", "Uasin Gishu", "Kajiado", "Kilifi", "Nyeri")
    }

    val finishesOptions = remember {
        listOf(
            "Budget" to "KES 35,000 / m²",
            "Standard" to "KES 48,000 / m²",
            "Premium" to "KES 65,000 / m²"
        )
    }

    val roofingOptions = remember {
        listOf(
            "Hip Roof Mabati",
            "Decra Tiles",
            "Flat Concrete Slab",
            "Asphalt Shingles",
            "Parapet Hidden Roof"
        )
    }

    val addonOptions = remember(template.isMultiStorey) {
        if (template.isMultiStorey) {
            listOf(
                "Dual High-Speed Lifts / Elevators" to "+16 m²",
                "Backup Industrial Generator" to "+12 m²",
                "Basement Parking Ramp" to "+45 m²",
                "Solar PV Rooftop Array" to "+20 m²",
                "Wet Riser Firefighting Core" to "+8 m²",
                "Rooftop Sky Lounge & Helipad" to "+35 m²"
            )
        } else {
            listOf(
                "DSQ (Servants Quarter)" to "+14 m²",
                "Covered Carport" to "+18 m²",
                "Solar Inverter Room" to "+6 m²",
                "Water Tower Base" to "+4 m²"
            )
        }
    }

    // Dynamic cost calculation based on customized footprint area, number of storeys and finishes
    val dynamicTotalArea = remember(selectedAreaSqm, selectedAddonModules.size, selectedStoreys) {
        var extra = 0f
        if (selectedAddonModules.contains("DSQ (Servants Quarter)")) extra += 14f
        if (selectedAddonModules.contains("Covered Carport)")) extra += 18f
        if (selectedAddonModules.contains("Solar Inverter Room")) extra += 6f
        if (selectedAddonModules.contains("Water Tower Base")) extra += 4f
        if (selectedAddonModules.contains("Dual High-Speed Lifts / Elevators")) extra += 16f
        if (selectedAddonModules.contains("Backup Industrial Generator")) extra += 12f
        if (selectedAddonModules.contains("Basement Parking Ramp")) extra += 45f
        if (selectedAddonModules.contains("Solar PV Rooftop Array")) extra += 20f
        if (selectedAddonModules.contains("Wet Riser Firefighting Core")) extra += 8f
        if (selectedAddonModules.contains("Rooftop Sky Lounge & Helipad")) extra += 35f

        if (selectedStoreys > 1) {
            (selectedAreaSqm * selectedStoreys) + extra
        } else {
            selectedAreaSqm + extra
        }
    }

    val highRiseMultiplier = remember(selectedStoreys) {
        when {
            selectedStoreys >= 7 -> 1.15
            selectedStoreys >= 5 -> 1.08
            selectedStoreys >= 3 -> 1.04
            else -> 1.00
        }
    }

    val dynamicCostBreakdown = remember(dynamicTotalArea, selectedFinishes, selectedRoof, selectedStoreys) {
        val base = BlueprintEngine.calculateCostBreakdown(
            areaSqm = dynamicTotalArea,
            finishesQuality = selectedFinishes,
            roofType = selectedRoof,
            soilType = "Red Soil"
        )
        base.copy(
            totalCostMin = base.totalCostMin * highRiseMultiplier,
            totalCostMax = base.totalCostMax * highRiseMultiplier
        )
    }

    val estimatedDurationMonths = remember(dynamicTotalArea, selectedStoreys) {
        when {
            selectedStoreys >= 8 -> "18 - 28 Months (High-Rise Tower)"
            selectedStoreys >= 5 -> "14 - 20 Months (Multi-Storey)"
            selectedStoreys >= 3 -> "10 - 15 Months (Mid-Rise)"
            dynamicTotalArea < 80f -> "3 - 5 Months"
            dynamicTotalArea < 150f -> "5 - 7 Months"
            dynamicTotalArea < 220f -> "7 - 10 Months"
            else -> "10 - 14 Months"
        }
    }

    // Active floor preview rooms based on level tab selection
    val activeRoomsForPreview = remember(template, previewLevelIndex) {
        when (previewLevelIndex) {
            1 -> if (template.typicalFloorRooms.isNotEmpty()) template.typicalFloorRooms else template.defaultRooms
            2 -> if (template.penthouseRooms.isNotEmpty()) template.penthouseRooms else template.defaultRooms
            else -> template.defaultRooms
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .testTag("template_customization_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Customize Building Template",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${template.category} • ${template.badge}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_template_customization_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Main Scrollable Customizer Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Preview Card & Highlights Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Title, Subtitle, and Plot Badge (Full width layout to prevent squishing)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = template.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = template.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AspectRatio,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = template.recommendedPlot,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            // Interactive Mini Plan Thumbnail Map with Floor Level Switcher
                            if (template.isMultiStorey || template.maxStoreys > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val levels = buildList {
                                        add(0 to "Ground")
                                        if (selectedStoreys > 1) {
                                            add(1 to "Typical (L1–${selectedStoreys - 1})")
                                        }
                                        if (selectedStoreys >= 3 && template.penthouseRooms.isNotEmpty()) {
                                            add(2 to "Penthouse (L$selectedStoreys)")
                                        }
                                    }
                                    levels.forEach { (idx, label) ->
                                        val isSel = previewLevelIndex == idx
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { previewLevelIndex = idx },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = if (isSel) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(6.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val rooms = activeRoomsForPreview
                                    val maxX = rooms.maxOfOrNull { it.x + it.width } ?: 12f
                                    val maxY = rooms.maxOfOrNull { it.y + it.height } ?: 10f

                                    val scale = min(size.width / (maxX + 1f), size.height / (maxY + 1f))
                                    val offX = (size.width - maxX * scale) / 2f
                                    val offY = (size.height - maxY * scale) / 2f

                                    rooms.forEach { room ->
                                        val rx = offX + room.x * scale
                                        val ry = offY + room.y * scale
                                        val rw = room.width * scale
                                        val rh = room.height * scale

                                        drawRect(
                                            color = parseHexColor(room.color).copy(alpha = 0.55f),
                                            topLeft = Offset(rx, ry),
                                            size = Size(rw, rh)
                                        )
                                        drawRect(
                                            color = Color.White.copy(alpha = 0.8f),
                                            topLeft = Offset(rx, ry),
                                            size = Size(rw, rh),
                                            style = Stroke(width = 1.2f)
                                        )
                                    }
                                }
                            }

                            // Highlights bullet points
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                template.highlights.take(4).forEach { hl ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = hl,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. Custom Project Name
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        label = { Text("Project / Client Title") },
                        placeholder = { Text("e.g. My 10-Storey Commercial Plaza") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_project_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // 2. Multi-Storey Configuration Section (Up to 10 Storeys)
                    if (template.isMultiStorey || template.maxStoreys > 1) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Apartment,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Building Height & Storeys:",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Configurable from 1 to ${template.maxStoreys} storeys",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Stepper controls
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (selectedStoreys > template.minStoreys) {
                                                    selectedStoreys--
                                                    if (previewLevelIndex > 1 && selectedStoreys < 3) previewLevelIndex = 0
                                                }
                                            },
                                            enabled = selectedStoreys > template.minStoreys,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease Storeys")
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$selectedStoreys ${if (selectedStoreys == 1) "Storey" else "Storeys"}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (selectedStoreys < template.maxStoreys) selectedStoreys++
                                            },
                                            enabled = selectedStoreys < template.maxStoreys,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase Storeys")
                                        }
                                    }
                                }

                                // Interactive Storey Slider (1 to 10)
                                Slider(
                                    value = selectedStoreys.toFloat(),
                                    onValueChange = {
                                        selectedStoreys = it.roundToInt().coerceIn(template.minStoreys, template.maxStoreys)
                                        if (previewLevelIndex > 1 && selectedStoreys < 3) previewLevelIndex = 0
                                    },
                                    valueRange = template.minStoreys.toFloat()..template.maxStoreys.toFloat(),
                                    steps = template.maxStoreys - template.minStoreys - 1,
                                    modifier = Modifier.testTag("template_storeys_slider")
                                )

                                // Quick Storey Selector Chips (e.g., 2, 4, 6, 8, 10 Storeys)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(1, 2, 4, 6, 8, 10).filter { it <= template.maxStoreys }.forEach { storey ->
                                        val isSel = selectedStoreys == storey
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedStoreys = storey
                                                    if (previewLevelIndex > 1 && selectedStoreys < 3) previewLevelIndex = 0
                                                },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${storey}F",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Building Elevation Stacked Diagram
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Elevation Profile (Level Distribution):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    // Visual tower blocks stacked from top to bottom
                                    if (selectedStoreys >= 3) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE91E63).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Level $selectedStoreys: Rooftop Penthouse & Sky Lounge", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC2185B))
                                            Text("${selectedAreaSqm.roundToInt()} m²", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (selectedStoreys > 1) {
                                        val typicalCount = if (selectedStoreys >= 3) selectedStoreys - 2 else 1
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2196F3).copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Levels 1 to ${if (selectedStoreys >= 3) selectedStoreys - 1 else 1}: Typical Floors ($typicalCount levels)", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1976D2))
                                            Text("${(selectedAreaSqm * typicalCount).roundToInt()} m²", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Ground Floor: Entrance Lobby, Commercial & Lift Core", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF388E3C))
                                        Text("${selectedAreaSqm.roundToInt()} m²", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // NCA High-Rise Structural Regulations Checklist
                                Surface(
                                    color = if (selectedStoreys >= 5) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            if (selectedStoreys >= 5) Icons.Default.Shield else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (selectedStoreys >= 5) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = when {
                                                    selectedStoreys >= 7 -> "NCA High-Rise Tower (7-10 Storeys)"
                                                    selectedStoreys >= 5 -> "NCA Multi-Storey Code (5-6 Storeys)"
                                                    selectedStoreys >= 3 -> "Mid-Rise Concrete Frame (3-4 Storeys)"
                                                    else -> "Low-Rise Category (1-2 Storeys)"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = when {
                                                    selectedStoreys >= 7 -> "Mandatory dual passenger lifts, pressurized escape stairwells, wet riser pump, and Class A structural sign-off."
                                                    selectedStoreys >= 5 -> "Mandatory lift installation, dual fire exits, and reinforced concrete shear core."
                                                    selectedStoreys >= 3 -> "Reinforced concrete columns and suspended slabs required."
                                                    else -> "Standard foundation strip & masonry construction."
                                                },
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Target Floor Footprint Area Slider
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SquareFoot,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (selectedStoreys > 1) "Footprint per Floor:" else "Gross Floor Area:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (selectedStoreys > 1) {
                                            Text(
                                                text = "Total Gross: ${dynamicTotalArea.roundToInt()} m² across $selectedStoreys floors",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${selectedAreaSqm.roundToInt()} m² / floor",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            val minArea = max(45f, template.baseAreaSqm * 0.7f)
                            val maxArea = template.baseAreaSqm * 1.6f

                            Slider(
                                value = selectedAreaSqm,
                                onValueChange = { selectedAreaSqm = it },
                                valueRange = minArea..maxArea,
                                modifier = Modifier.testTag("template_area_slider")
                            )

                            // Area preset quick chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    (minArea).roundToInt() to "Compact",
                                    (template.baseAreaSqm).roundToInt() to "Original",
                                    (maxArea).roundToInt() to "Spacious"
                                ).forEach { (sqm, label) ->
                                    FilterChip(
                                        selected = (selectedAreaSqm.roundToInt() == sqm),
                                        onClick = { selectedAreaSqm = sqm.toFloat() },
                                        label = { Text("$sqm m² ($label)", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Location / Kenyan County Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Site Location (Kenyan County):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(countiesList) { county ->
                                FilterChip(
                                    selected = selectedCounty == county,
                                    onClick = { selectedCounty = county },
                                    label = { Text(county, fontSize = 11.sp) },
                                    leadingIcon = if (selectedCounty == county) {
                                        { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // 4. Finishes Quality Grade
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Finishes & Fittings Quality Grade:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            finishesOptions.forEach { (grade, rateDesc) ->
                                val isSelected = selectedFinishes.equals(grade, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedFinishes = grade },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = grade,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = rateDesc,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Roofing Architectural Style
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Roofing System & Cover:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(roofingOptions) { roof ->
                                FilterChip(
                                    selected = selectedRoof == roof,
                                    onClick = { selectedRoof = roof },
                                    label = { Text(roof, fontSize = 11.sp) },
                                    leadingIcon = if (selectedRoof == roof) {
                                        { Icon(Icons.Default.Roofing, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // 6. Optional Add-on Modules & Wings
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Optional Modular Add-on Wings:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        addonOptions.forEach { (addon, areaTag) ->
                            val isChecked = selectedAddonModules.contains(addon)
                            Surface(
                                onClick = {
                                    if (isChecked) selectedAddonModules.remove(addon)
                                    else selectedAddonModules.add(addon)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = if (isChecked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedAddonModules.add(addon)
                                                else selectedAddonModules.remove(addon)
                                            }
                                        )
                                        Text(
                                            text = addon,
                                            fontSize = 12.sp,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = areaTag,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Live Dynamic Cost Estimation & Timeline Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MonetizationOn,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Estimated NCA Construction Cost:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Surface(
                                    color = AccentGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = estimatedDurationMonths,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${currencyFormat.format(dynamicCostBreakdown.totalCostMin)} – ${currencyFormat.format(dynamicCostBreakdown.totalCostMax)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Adjusted Area: ${dynamicTotalArea.roundToInt()} m²",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "County Zoning: $selectedCounty OK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Bottom Action Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onConfirmCustomization(
                                    projectTitle,
                                    selectedAreaSqm,
                                    selectedCounty,
                                    selectedFinishes,
                                    selectedRoof,
                                    selectedAddonModules.toList(),
                                    selectedStoreys
                                )
                            },
                            modifier = Modifier
                                .weight(2f)
                                .testTag("apply_template_customization_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate & Open Plan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
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
